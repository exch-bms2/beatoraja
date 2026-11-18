package bms.player.beatoraja.skin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import bms.player.beatoraja.skin.SkinTextFont.FontScaleSnapshot;
import bms.player.beatoraja.skin.FontSession.FallbackAtlas;

/**
 * GL・コンテキストは作らない。CPU のネイティブ書体と制御したフレーム境界で、実際の所有権コードを動かす。
 * No GL/context creation. Exercises actual ownership code with CPU native faces and controlled frame boundaries.
 */
public final class FontLifetimeTest {
    private static final List<Runnable> pending = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        GdxNativesLoader.load();
        Gdx.files = new Lwjgl3Files();
        Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(), new Class<?>[]{Application.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("postRunnable")) { pending.add((Runnable) values[0]); return null; }
                    throw new UnsupportedOperationException(method.getName());
                });
        check(Gdx.gl == null, "CPU test must never create GL");
        if (args.length > 0 && args[0].equals("--transient-only")) {
            transientBorrowers("font/VL-Gothic-Regular.ttf");
            System.out.println("Transient borrower PASS checks=" + checks);
            return;
        }
        scaleRestore();
        sharedBorrowers();
        sessionTransaction(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        rotationBounded(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        transientBorrowers(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        coverageLookup();
        fallbackBaselineAlignment();
        variationSelectorsAreZeroWidth();
        activeAtlasRenewal(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        faceIndexLeases();
        sharedGlobalChoice();
        generatorLeases();
        samePathReplacement();
        shutdownWithoutApp();
        shutdownReleasesGlobalSource();
        drain();
        check(pending.isEmpty(), "no pending cleanup");
        check(Gdx.gl == null, "GL stayed absent");
        System.out.println("FontLifetimeTest PASS checks=" + checks + " (GL NOT_RUN)");
    }

    /**
     * TTC の書体番号ごとに別の生成器を共有し、同じ番号は同じ生成器を返す。返し終えたら共有の場から消える。
     * Each TTC face index gets its own pooled generator, the same index is shared, and released faces leave the pool.
     */
    private static void faceIndexLeases() throws Exception {
        SystemFallbackGlyphs.resetGlobalSource();
        drain();
        byte[] font = Files.readAllBytes(Path.of("font/VL-Gothic-Regular.ttf"));
        Path ttc = Files.createDirectories(Path.of("build/font-lifetime")).resolve("two-faces.ttc");
        Files.write(ttc, collectionOfTwo(font));
        String path = ttc.toString().replace('\\', '/');
        int before = FontGeneratorPool.activeCount();
        var face0 = FontGeneratorPool.acquire(path, 0);
        var face1 = FontGeneratorPool.acquire(path, 1);
        var again1 = FontGeneratorPool.acquire(path, 1);
        check(face0.generator() != face1.generator() && face1.generator() == again1.generator()
                && FontGeneratorPool.activeCount() == before + 2, "each TTC face is its own pooled generator; the same face is shared");
        check(face1.coverage().mayContain('A') && face1.coverage().mayContain(0x3042) && !face1.coverage().mayContain(0xac00),
                "the second face's coverage is read from its own directory");
        var weighted = FontGeneratorPool.acquire(path, 0, FallbackFontPolicy.GLOBAL_WEIGHT);
        check(weighted.generator() == face0.generator() && FontGeneratorPool.activeCount() == before + 2,
                "a static font asked for a weight shares the generator of the plain lease");
        weighted.dispose();
        face0.dispose();
        face1.dispose();
        again1.dispose();
        drain();
        check(FontGeneratorPool.activeCount() == before, "released faces leave the pool");
    }

    /** 一つの書体を二つの書体として持つ TTC。TTC では表の位置が絶対位置なのでずらす。 / A TTC holding one face twice; table offsets are absolute in a TTC, so they shift. */
    private static byte[] collectionOfTwo(byte[] font) {
        int header = 12 + 2 * Integer.BYTES;
        java.nio.ByteBuffer out = java.nio.ByteBuffer.allocate(header + font.length);
        out.putInt(0x74746366).putInt(0x00010000).putInt(2).putInt(header).putInt(header);
        out.put(font);
        int tables = java.nio.ByteBuffer.wrap(font).getShort(4) & 0xffff;
        for (int i = 0; i < tables; i++) {
            int at = header + 12 + 16 * i + 8;
            out.putInt(at, out.getInt(at) + header);
        }
        return out.array();
    }

    /**
     * 共通代替フォントの並びと文字ごとの選択は共通供給元が一度だけ作り、スキンの供給元はそれを参照するだけで作り直さない。
     * The shared global source builds the global order and per-character choice once; skin sources only refer to it.
     */
    private static void sharedGlobalChoice() {
        SystemFallbackGlyphs.resetGlobalSource();
        drain();
        SkinFontSource global = SystemFallbackGlyphs.globalSource();
        check(global != null && global.isAvailable(), "the global source loads the font folder");
        SkinFontSource first = new SkinFontSource((String) null, new String[0]);
        SkinFontSource second = new SkinFontSource("skin/missing.ttf", new String[0]);
        FreeTypeFontGenerator main = global.getGenerator();
        check(first.getGenerator() == main && second.getGenerator() == main
                && first.getFallbackCandidates('A').equals(global.getFallbackCandidates('A')),
                "skin sources use the global choice built once by the shared source");
        first.dispose();
        second.dispose();
        drain();
        check(global.isAvailable() && global.getGenerator() == main, "disposing skin sources leaves the global choice alone");
    }

    private static void scaleRestore() {
        BitmapFontData data = new BitmapFontData();
        data.scaleX = 1.37f; data.scaleY = .73f; data.lineHeight = 29.123f; data.spaceXadvance = 5.321f;
        data.xHeight = 13.147f; data.capHeight = 17.99f; data.ascent = -.123f; data.descent = -5.121f;
        data.down = -31.43f; data.padTop = .123f; data.padLeft = .312f; data.padBottom = .456f; data.padRight = .789f;
        int[] before = bits(data);
        for (int i = 0; i < 10000; i++) {
            try (FontScaleSnapshot ignored = new FontScaleSnapshot(data)) { data.setScale(.33337f, 4.77771f); }
        }
        check(java.util.Arrays.equals(before, bits(data)), "all scale metrics restore bit-exactly after 10000 uses");
        try {
            try (FontScaleSnapshot ignored = new FontScaleSnapshot(data)) {
                data.setScale(7.333f, .1117f);
                throw new IllegalStateException("draw failure");
            }
        } catch (IllegalStateException expected) { }
        check(java.util.Arrays.equals(before, bits(data)), "exception restores every scale metric");
    }

    private static int[] bits(BitmapFontData d) {
        float[] values = {d.scaleX,d.scaleY,d.lineHeight,d.spaceXadvance,d.xHeight,d.capHeight,d.ascent,d.descent,d.down,
                d.padTop,d.padLeft,d.padBottom,d.padRight};
        int[] result = new int[values.length];
        for (int i=0;i<values.length;i++) result[i]=Float.floatToRawIntBits(values[i]);
        return result;
    }

    private static void sharedBorrowers() {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        FallbackAtlas a = FallbackAtlas.acquire(p);
        FallbackAtlas b = FallbackAtlas.acquire(p);
        check(a == b && a.borrowerCount() == 2, "two borrowers share one fallback page owner");
        String firstName = a.nextName();
        check(!firstName.equals(b.nextName()), "PUA-identical borrowers cannot overwrite atlas names");
        a.release();
        check(b.isLive() && b.borrowerCount() == 1 && pending.isEmpty(), "A release preserves B, no dispose");
        FallbackAtlas againA = FallbackAtlas.acquire(p);
        check(againA != b && b.isLive() && againA.isLive(), "departed sharing group stays live but rejects new borrowers");
        b.release();
        againA.release();
        check(!a.isLive() && FontRenderRetirement.pendingCount() == 2 && pending.size() == 1,
                "both final leases retire through one scheduled boundary drain");
        FallbackAtlas next = FallbackAtlas.acquire(p);
        check(next != a && next.generation != a.generation, "retiring owner cannot be reacquired");
        FallbackAtlas.invalidateAll();
        check(!next.isLive(), "context loss stales existing generation");
        FallbackAtlas recovered = FallbackAtlas.acquire(p);
        check(recovered.generation != next.generation && recovered.isLive(), "context recovery creates fresh generation");
        next.release(); recovered.release();
        drain();
        int[] count = {0};
        FontRenderRetirement.retire(() -> count[0]++);
        Runnable cleanup = pending.remove(0);
        check(count[0] == 0, "native disposal not synchronous with last release");
        cleanup.run(); cleanup.run();
        check(count[0] == 1, "cleanup runnable disposes exactly once even if called twice");
    }

    private static final class CountingFont extends BitmapFont {
        int disposals;
        CountingFont() { super(new BitmapFontData(), Array.with(new TextureRegion()), false); }
        @Override public void dispose() { disposals++; super.dispose(); }
    }

    private static final class ControlledGenerator extends FreeTypeFontGenerator {
        boolean fail;
        CountingFont last;
        ControlledGenerator(String path) { super(new FileHandle(path)); }
        @Override public BitmapFont generateFont(FreeTypeFontParameter p) {
            if (fail) throw new GdxRuntimeException("injected preparation failure");
            last = new CountingFont();
            return last;
        }
    }

    private static void sessionTransaction(String path) {
        ControlledGenerator generator = new ControlledGenerator(path);
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        FontSession old = FontSession.open(generator, p);
        CountingFont oldFont = generator.last;
        PixmapPacker oldPacker = p.packer;
        generator.fail = true;
        try { FontSession.open(generator, p, old); throw new AssertionError("failure expected"); }
        catch (GdxRuntimeException expected) { }
        check(old.isLive() && old.font() == oldFont && p.packer == oldPacker && oldFont.disposals == 0,
                "partial generation failure retains READY font/packer");
        generator.fail = false;
        FontSession next = FontSession.open(generator, p, old);
        check(next.atlas()!=old.atlas(), "replacement excludes its own previous fallback atlas");
        PixmapPacker nextPacker = p.packer;
        check(next.generation() != old.generation(), "session replacement changes cache generation");
        BitmapFont.Glyph a = new BitmapFont.Glyph(); a.id=0xe000;
        old.font().getData().setGlyph(0xe000, a);
        BitmapFont.Glyph b = new BitmapFont.Glyph(); b.id=0xe000;
        next.font().getData().setGlyph(0xe000, b);
        check(old.font().getData()!=next.font().getData() && old.font().getData().getGlyph((char)0xe000)==a,
                "same PUA slot uses separate borrower glyph tables");
        old.dispose(); old.dispose();
        check(!old.isLive() && next.isLive() && p.packer==nextPacker && oldFont.disposals==0,
                "old release stales immediately, preserves replacement parameter, defers native cleanup");
        drain();
        check(oldFont.disposals==1, "old font disposed once at boundary");
        CountingFont newFont=generator.last;
        next.dispose(); next.dispose(); drain();
        check(newFont.disposals==1 && p.packer==null, "last borrower cleanup exactly once");
        generator.dispose();
    }

    private static void rotationBounded(String path) {
        int originalPageSize=FreeTypeFontGenerator.getMaxTextureSize();
        FreeTypeFontGenerator.setMaxTextureSize(64);
        ControlledGenerator generator=new ControlledGenerator(path);
        FreeTypeFontParameter p=new FreeTypeFontParameter();p.size=48;
        FontSession current=FontSession.open(generator,p);
        FreeTypeFontParameter anchorParameter=new FreeTypeFontParameter();anchorParameter.size=48;
        FontSession anchor=FontSession.open(generator,anchorParameter);
        FallbackAtlas anchorAtlas=anchor.atlas();
        check(anchorAtlas==current.atlas(),"two borrowers initially share one actual packer");
        com.badlogic.gdx.graphics.Pixmap pixels=new com.badlogic.gdx.graphics.Pixmap(38,48,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        anchorAtlas.packer.pack(anchorAtlas.nextName(),pixels);
        int peakOwners=0;
        List<CountingFont> replacedFonts=new ArrayList<>();
        for(int i=0;i<48;i++) {
            FallbackAtlas oldAtlas=current.atlas();
            CountingFont oldFont=(CountingFont)current.font();
            FontSession candidate=FontSession.open(generator,p,current);
            check(candidate.atlas()!=oldAtlas,"rotation "+i+" gets a fresh owner before old release");
            peakOwners=Math.max(peakOwners,FallbackAtlas.liveOwnerCount());
            candidate.atlas().packer.pack(candidate.atlas().nextName(),pixels);
            current.dispose();current=candidate;replacedFonts.add(oldFont);
            drain();
            check(FallbackAtlas.liveOwnerCount()<=2 && current.atlas().packer.getPages().size==1,
                    "rotation "+i+" bounds live owners/pages after boundary");
            check(anchor.isLive() && anchorAtlas.packer.getPages().size==1,
                    "rotation "+i+" preserves the other borrower's old pages");
        }
        check(peakOwners<=3,"two borrowers plus one replacement bound transient owners to three");
        check(replacedFonts.stream().allMatch(f->f.disposals==1),"all replaced font wrappers disposed exactly once");
        long readyGeneration=current.atlas().generation;
        PixmapPacker readyPacker=p.packer;
        int readyOwners=FallbackAtlas.liveOwnerCount();
        generator.fail=true;
        try { FontSession.open(generator,p,current);throw new AssertionError("injected failure expected"); }
        catch(GdxRuntimeException expected) { }
        check(current.isLive() && anchor.isLive() && current.atlas().generation==readyGeneration
                && p.packer==readyPacker && FallbackAtlas.liveOwnerCount()==readyOwners,
                "failed replacement preserves both READY leases, generation and resource count");
        FallbackAtlas.invalidateAll();
        check(!anchor.isLive() && !current.isLive(),"context invalidation includes displaced still-leased owner");
        current.dispose();anchor.dispose();pixels.dispose();drain();generator.dispose();
        FreeTypeFontGenerator.setMaxTextureSize(originalPageSize);
        check(FallbackAtlas.liveOwnerCount()==0 && FontRenderRetirement.pendingCount()==0,
                "all rotated owners and retirement resources released at final boundary");
    }

    private static void transientBorrowers(String path) {
        int originalPageSize = FreeTypeFontGenerator.getMaxTextureSize();
        FreeTypeFontGenerator.setMaxTextureSize(64);
        FreeTypeFontParameter p = new FreeTypeFontParameter(); p.size = 48;
        FallbackAtlas anchor = FallbackAtlas.acquire(p);
        com.badlogic.gdx.graphics.Pixmap pixels = new com.badlogic.gdx.graphics.Pixmap(38, 48,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        try {
            anchor.packer.pack(anchor.nextName(), pixels);
            for (int i = 0; i < 48; i++) {
                FallbackAtlas temporary = FallbackAtlas.acquire(p);
                temporary.packer.pack(temporary.nextName(), pixels);
                temporary.release(); drain();
                check(anchor.isLive() && anchor.borrowerCount() == 1,
                        "temporary release " + i + " preserves the long-lived borrower");
                check(anchor.packer.getPages().size <= 2 && FallbackAtlas.liveOwnerCount() == 1,
                        "temporary release " + i + " does not accumulate old pages");
            }
        } finally {
            anchor.release(); pixels.dispose(); drain();
            FreeTypeFontGenerator.setMaxTextureSize(originalPageSize);
        }
        check(FallbackAtlas.liveOwnerCount() == 0, "temporary sharing groups all released");
    }

    private static void generatorLeases() {
        // 共通代替フォントの供給元はプログラムの間ずっと共有の場に残るので、項目数を数える前に手放す。
        // The global fallback source stays pooled for the whole program; release it before counting entries.
        SystemFallbackGlyphs.resetGlobalSource(); drain();
        FontGeneratorPool.Lease a=FontGeneratorPool.acquire("font/VL-Gothic-Regular.ttf");
        FontGeneratorPool.Lease b=FontGeneratorPool.acquire("font\\VL-Gothic-Regular.ttf");
        check(a.generator()==b.generator() && a.coverage()==b.coverage(), "same snapshot shares native face and coverage");
        try { FontGeneratorPool.clear(); throw new AssertionError("live clear must fail"); }
        catch (IllegalStateException expected) { }
        a.dispose(); a.dispose();
        check(b.generator()!=null && FontGeneratorPool.activeCount()==1, "duplicate lease release preserves borrower");
        b.dispose();
        check(FontGeneratorPool.activeCount()==0 && pending.size()==1, "last native face retirement is deferred");
        FontGeneratorPool.clear(); drain();
    }

    /**
     * 別のフォントから作った代替グリフは、生成済みフォントの基準線(基本ジェネレーターの ascender)にそろう。
     * A fallback glyph from another font lands on the generated font's baseline (the base generator's ascender).
     */
    private static void fallbackBaselineAlignment() {
        SkinFontSource source = new SkinFontSource("font/VL-Gothic-Regular.ttf",
                new String[] { "font/NotoEmoji-VariableFont_wght.ttf" });
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 32;
        int baseBottom = source.getGenerator().generateGlyphAndBitmap('4', parameter.size, false).glyph.yoffset;
        int[] raw = { Integer.MIN_VALUE }, aligned = { Integer.MIN_VALUE };
        new GlyphProvider(source, parameter, (cp, cause) -> { throw new AssertionError(cause); })
                .resolve('4', true, (raster, cp) -> { raw[0] = raster.glyph.yoffset; return raster.glyph; });
        GlyphProvider.Result result = new GlyphProvider(source, parameter, (cp, cause) -> { throw new AssertionError(cause); }, true)
                .resolve('4', true, (raster, cp) -> { aligned[0] = raster.glyph.yoffset; return raster.glyph; });
        System.out.println("baseline: base=" + baseBottom + " fallback raw=" + raw[0] + " aligned=" + aligned[0]);
        check(raw[0] != Integer.MIN_VALUE && Math.abs(raw[0] - baseBottom) > 1,
                "the bundled second font really has another baseline before alignment");
        check(result.glyph != null && Math.abs(aligned[0] - baseBottom) <= 1,
                "fallback glyph from another font is aligned to the generated font's baseline");
        source.dispose();
        drain();
    }

    /**
     * 画面フォントの代替データでは、異体字セレクター(U+FE00–FE0F、U+E0100–E01EF)が幅も欠落記号も足さない。GL なしで字形列を比べる。
     * In screen-font fallback data, variation selectors (U+FE00–FE0F, U+E0100–E01EF) add neither advance nor a marker.
     * Compares glyph runs without GL.
     */
    private static void variationSelectorsAreZeroWidth() {
        BitmapFontData base = new BitmapFontData();
        for (char c : new char[] { 'A', 'B' }) {
            BitmapFont.Glyph glyph = new BitmapFont.Glyph();
            glyph.id = c;
            glyph.width = 10;
            glyph.height = 10;
            glyph.xadvance = 12;
            base.setGlyph(c, glyph);
        }
        SystemFontFactory.OnDemandFallbackFontData data = new SystemFontFactory.OnDemandFallbackFontData(base, null, null);
        float pair = runWidth(data, "AB");
        check(runWidth(data, "A\uFE00B") == pair && runWidth(data, "A\uFE0FB") == pair
                && runWidth(data, "A\uDB40\uDD00B") == pair && selectorSlotsAreEmpty(data, "A\uFE00\uFE0F\uDB40\uDD00"),
                "variation selectors (U+FE00-FE0F, U+E0100-E01EF) are zero-width");
    }

    /** 先頭以外の字形がすべて幅・大きさ 0(欠落記号ではない)。 / Every glyph after the first has zero advance and size (no marker). */
    private static boolean selectorSlotsAreEmpty(BitmapFontData data, String text) {
        GlyphLayout.GlyphRun run = new GlyphLayout.GlyphRun();
        data.getGlyphs(run, text, 0, text.length(), null);
        for (int i = 1; i < run.glyphs.size; i++) {
            BitmapFont.Glyph glyph = run.glyphs.get(i);
            if (glyph.xadvance != 0 || glyph.width != 0) return false;
        }
        return run.glyphs.size == text.length();
    }

    private static float runWidth(BitmapFontData data, String text) {
        GlyphLayout.GlyphRun run = new GlyphLayout.GlyphRun();
        data.getGlyphs(run, text, 0, text.length(), null);
        float width = 0;
        for (int i = 0; i < run.xAdvances.size; i++) width += run.xAdvances.get(i);
        if (run.glyphs.size != text.length()) throw new AssertionError("one glyph slot per UTF-16 unit: " + text);
        return width;
    }

    /**
     * 長く使う画面フォントの代替ページは上限に達したら新しい組へ移り、ページ数は実行時間に比例して増えない。
     * Long-lived screen-font fallback pages move to a fresh atlas at the cap, so the page count does not grow with run time.
     */
    private static void activeAtlasRenewal(String path) {
        int originalPageSize = FreeTypeFontGenerator.getMaxTextureSize();
        FreeTypeFontGenerator.setMaxTextureSize(64);
        ControlledGenerator generator = new ControlledGenerator(path);
        FreeTypeFontParameter p = new FreeTypeFontParameter(); p.size = 47;
        com.badlogic.gdx.graphics.Pixmap pixels = new com.badlogic.gdx.graphics.Pixmap(38, 48,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        FontSession session = FontSession.open(generator, p);
        int renewals = 0, maxPages = 0, maxOwners = 0;
        try {
            for (int i = 0; i < 2000; i++) {
                FallbackAtlas atlas = session.atlas();
                atlas.packer.pack(atlas.nextName(), pixels); // 一字形 = 一ページ / one glyph per page
                atlas.notePacked();
                maxPages = Math.max(maxPages, atlas.pageCount());
                if (atlas.isFull()) {
                    FallbackAtlas probe = FallbackAtlas.acquire(p);
                    check(probe != atlas, "a full atlas is not handed to new borrowers");
                    probe.release();
                    session.renewFallbackAtlas();
                    renewals++;
                    drain();
                    check(session.atlas() != atlas && session.atlas().isLive() && !session.atlas().isFull(),
                            "renewal " + renewals + " moves to a fresh atlas");
                }
                maxOwners = Math.max(maxOwners, FallbackAtlas.liveOwnerCount());
            }
            check(renewals > 100 && maxPages <= FallbackAtlas.MAX_ACTIVE_PAGES,
                    "2000 distinct glyphs never hold more than the active page cap (" + maxPages + " pages, " + renewals + " renewals)");
            check(maxOwners <= 2, "old atlases are released after each renewal (owners " + maxOwners + ")");
        } finally {
            session.dispose(); pixels.dispose(); drain(); generator.dispose();
            FreeTypeFontGenerator.setMaxTextureSize(originalPageSize);
        }
        check(FallbackAtlas.liveOwnerCount() == 0 && FontRenderRetirement.pendingCount() == 0,
                "renewed atlases are all released at the end");
    }

    private static void coverageLookup() {
        int[] reads = { 0 }, attempts = { 0 };
        FileHandle file = new FileHandle("font/VL-Gothic-Regular.ttf") {
            @Override public java.io.InputStream read() { reads[0]++; return super.read(); }
        };
        SkinFontSource source = new SkinFontSource(file, new String[0]);
        check(source.isAvailable(), "source opens native face from pinned bytes");
        GlyphProvider provider = new GlyphProvider(source, new FreeTypeFontParameter(),
                (cp, cause) -> { throw new AssertionError(cause); });
        GlyphProvider.Result absent = provider.resolvePrepared(0x10ffff, false, generator -> {
            attempts[0]++;
            return (raster, cp) -> raster.glyph;
        });
        check(absent.kind == GlyphProvider.Kind.MISSING && attempts[0] == 0,
                "known absence skips native raster and pack preparation");
        GlyphProvider.Result present = provider.resolvePrepared('A', false, generator -> {
            attempts[0]++;
            return (raster, cp) -> raster.glyph;
        });
        check(present.kind == GlyphProvider.Kind.FALLBACK_RASTER && attempts[0] == 1,
                "coverage member still performs actual raster lookup");
        check(reads[0] == 1, "source coverage and native face consume one file snapshot");
        source.dispose();
    }

    private static void samePathReplacement() throws Exception {
        SystemFallbackGlyphs.resetGlobalSource(); drain();
        Path directory=Path.of("build/lp04-fixture"); Files.createDirectories(directory);
        Path fixture=directory.resolve("replace.ttf");
        byte[] original=Files.readAllBytes(Path.of("font/VL-Gothic-Regular.ttf"));
        Files.write(fixture,original);
        FontGeneratorPool.Lease before=FontGeneratorPool.acquire(fixture.toString());
        String originalFingerprint=before.coverage().fingerprint();
        byte[] changed=java.util.Arrays.copyOf(original,original.length+1);
        Files.write(fixture,changed);
        FontGeneratorPool.Lease after=FontGeneratorPool.acquire(fixture.toString());
        check(before.generator()!=after.generator() && !before.coverage().fingerprint().equals(after.coverage().fingerprint()),
                "same path replaced bytes never reuse old face/coverage");
        before.dispose();
        check(after.generator()!=null && FontGeneratorPool.activeCount()==1, "old fingerprint release cannot retire new entry");
        Files.write(fixture,original);
        FontGeneratorPool.Lease restored=FontGeneratorPool.acquire(fixture.toString());
        check(restored.generator()!=after.generator() && restored.coverage().fingerprint().equals(originalFingerprint),
                "A-B-A opens restored bytes after original lease retired");
        after.dispose(); restored.dispose(); drain();
    }

    private static void drain() {
        while (!pending.isEmpty()) {
            List<Runnable> frame = new ArrayList<>(pending); pending.clear();
            for (Runnable task : frame) task.run();
        }
    }

    private static void shutdownWithoutApp() {
        Application application=Gdx.app;
        Gdx.app=null;
        int[] count={0};
        FontRenderRetirement.retire(() -> count[0]++);
        check(count[0]==0 && pending.isEmpty(), "no-app retirement retains ownership without throwing");
        FontRenderRetirement.drainAfterBatch(null);
        FontRenderRetirement.drainAfterBatch(null);
        check(count[0]==1, "explicit CPU/shutdown boundary drains exactly once without another frame");
        FontRenderRetirement.retire(() -> { throw new IllegalStateException("injected cleanup failure"); });
        FontRenderRetirement.retire(() -> count[0]++);
        try { FontRenderRetirement.drainAfterBatch(null); throw new AssertionError("cleanup failure expected"); }
        catch (IllegalStateException expected) { check(expected.getSuppressed().length==1,"cleanup failure propagated"); }
        check(count[0]==2,"cleanup failure cannot prevent subsequent retirement");
        Gdx.app=application;
    }

    /**
     * 終了処理は共通代替フォントの貸与をすべて返して解放を実行し、その後は作り直さない。
     * Shutdown returns every global lease, drains the cleanup, and does not rebuild the global source afterwards.
     */
    private static void shutdownReleasesGlobalSource() {
        SystemFallbackGlyphs.resetGlobalSource(); drain();
        SkinFontSource global = SystemFallbackGlyphs.globalSource();
        check(global != null && global.getGenerator() != null && FontGeneratorPool.activeCount() > 0,
                "the global source holds pooled faces before shutdown");
        FontRenderRetirement.drainAtShutdown(null);
        check(FontGeneratorPool.activeCount() == 0 && FontRenderRetirement.pendingCount() == 0,
                "shutdown returns every global lease and drains its cleanup");
        FontGeneratorPool.clear();
        check(SystemFallbackGlyphs.globalSource() == null, "the global source is not rebuilt after shutdown");
        SystemFallbackGlyphs.resetGlobalSource();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("OK " + message);
    }
}
