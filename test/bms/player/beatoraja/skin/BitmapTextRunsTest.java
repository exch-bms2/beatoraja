package bms.player.beatoraja.skin;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.GlyphLayout.GlyphRun;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import bms.player.beatoraja.skin.SkinTextBitmap.BitmapTextRuns;
import bms.player.beatoraja.skin.SystemFallbackGlyphs.SystemFallbackAtlas;

/**
 * H234 の合成ビットマップ計測値と既存の所有権検査の書き方で、CPU 上の配置契約を確かめる。
 * CPU layout contracts using the H234 synthetic bitmap metrics and the existing ownership probe style.
 */
public final class BitmapTextRunsTest {
    private static int checks;

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }

    private static Glyph glyph(int codePoint, int advance) {
        Glyph g = new Glyph();
        g.id = codePoint;
        g.width = advance - 2;
        g.height = 24;
        g.xadvance = advance;
        g.xoffset = 1;
        g.yoffset = -24;
        g.u = .1f; g.v = .2f; g.u2 = .3f; g.v2 = .4f;
        return g;
    }

    private static BitmapFontData data() {
        BitmapFontData data = new BitmapFontData();
        data.lineHeight = 32; data.capHeight = 24; data.ascent = 2;
        data.descent = -6; data.down = -32; data.spaceXadvance = 10;
        data.setGlyph('A', glyph('A', 22));
        data.setGlyph('H', glyph('H', 22));
        data.setGlyph('?', glyph('?', 17));
        Glyph space = glyph(' ', 10); space.width = space.height = 0;
        data.setGlyph(' ', space);
        return data;
    }

    private static List<String> positions(GlyphLayout layout) {
        List<String> result = new ArrayList<>();
        for (GlyphRun run : layout.runs) {
            float x = run.x;
            for (int i = 0; i < run.glyphs.size; i++) {
                x += run.xAdvances.get(i);
                result.add(run.glyphs.get(i).id + ":" + x + ":" + run.y);
            }
        }
        return result;
    }

    private static void runPositions() {
        BitmapFontData original = data(), fallback = data();
        original.getGlyph('A').setKerning('H', -3);
        original.getGlyph('A').setKerning('\uBC24', -9);
        fallback.setGlyph('\uBC24', glyph('\uBC24', 30));
        fallback.getGlyph('\uBC24').setKerning('H', -8);
        BitmapFont measure = BitmapTextRuns.measurementFont(original, fallback);
        for (String text : new String[] {"AA", "AH", "A\uBC24H", "\uBC24A\uBC24", "A\uBC24 A\nH\uBC24"}) {
            for (boolean wrap : new boolean[] {false, true}) {
                for (int align : new int[] {Align.left, Align.center, Align.right}) {
                    GlyphLayout layout = new GlyphLayout();
                    layout.setText(measure, text, Color.WHITE, 54, align, wrap);
                    BitmapTextRuns split = new BitmapTextRuns(layout, original);
                    List<String> actual = new ArrayList<>();
                    for (BitmapTextRuns.Run run : split.runs) {
                        actual.addAll(positions(run.layout));
                        for (GlyphRun line : run.layout.runs) for (Glyph g : line.glyphs) {
                            check(run.fallback == (original.getGlyph((char)g.id) != g), "source ownership");
                        }
                    }
                    check(positions(layout).equals(actual), "shared positions " + text + " wrap=" + wrap + " align=" + align);
                }
            }
        }
        GlyphLayout mixed = new GlyphLayout(measure, "A\uBC24H");
        check(mixed.runs.first().xAdvances.get(1) == 22, "no kerning from original into fallback");
        check(mixed.runs.first().xAdvances.get(2) == 30, "no kerning from fallback into original");
        GlyphLayout nativePair = new GlyphLayout(measure, "AH");
        check(nativePair.runs.first().xAdvances.get(1) == 19, "native kerning retained");
        check(original.getGlyph('\uBC24') == null, "measurement does not insert fallback glyphs");
        check(measure.getData().glyphs[0] == null, "measurement owns no composite glyph table");
        original.getGlyph('H').page = 1;
        GlyphLayout preloaded = new GlyphLayout(measure, "AH");
        BitmapTextRuns pages = new BitmapTextRuns(preloaded,
                g -> original.getGlyph((char)g.id) != g || g.page == 1);
        check(pages.runs.size() == 2 && !pages.runs.get(0).fallback && pages.runs.get(1).fallback,
                "preloaded RGBA page stays a fallback run even when its glyph is in original data");
        check(pages.runs.get(1).layout.runs.first().glyphs.first() == original.getGlyph('H'),
                "preloaded RGBA glyph and UV are reused without replacement");
    }

    private static SkinTextBitmap text(BitmapFontData original, SkinFontSource fallback) {
        Array<TextureRegion> regions = new Array<>(); regions.add(new TextureRegion());
        SkinTextBitmap text = new SkinTextBitmap(original, regions, fallback, 32, 1, null);
        text.region.set(0, 0, 200, 32);
        text.setOverflow(SkinText.OVERFLOW_OVERFLOW);
        return text;
    }

    private static String metrics(BitmapFontData data) {
        return data.scaleX + ":" + data.scaleY + ":" + data.lineHeight + ":" + data.capHeight + ":"
                + data.ascent + ":" + data.descent + ":" + data.padLeft + ":" + data.padRight + ":"
                + data.padTop + ":" + data.padBottom + ":" + data.spaceXadvance + ":" + data.xHeight + ":" + data.down;
    }

    private static void caches() {
        SkinTextBitmap nativeText = text(data(), null);
        nativeText.prepareFont("AH"); nativeText.getTextWidth("AH"); nativeText.createCachedTextLayout("AH");
        check(nativeText.getGlyphProvider() == null && nativeText.getGlyphRevision() == 0,
                "original-only text never creates a raster provider");
        nativeText.dispose();
        SkinFontSource unavailable = new SkinFontSource("", new String[0]);
        unavailable.dispose(); // FreeType 書体の読み込みや GL なしで CPU 上の「含まれない」判定を確かめる / CPU negative lookup without loading a FreeType face or touching GL.
        BitmapFontData original = data(); original.setScale(2.25f, 1.375f);
        SkinTextBitmap a = text(original, unavailable), b = text(original, unavailable);
        String before = metrics(a.getFontData());
        Glyph nativeA = a.getFontData().getGlyph('A');
        float u = nativeA.u;
        a.registerFallbackGlyph('\uBC24', glyph('\uBC24', 30));
        check(a.getFontData().getGlyph('\uBC24') == null, "registration never inserts into original draw data");
        check(a.getFont().getData() == a.getFontData(), "original draw font remains original");
        check(a.getRegions().size == a.getBasePageCount(), "original page table unchanged");
        SkinText.CachedTextLayout mixed = a.createCachedTextLayout("A\uBC24H");
        SkinText.CachedTextLayout nativeOnly = a.createCachedTextLayout("AH");
        check(a.isCachedTextLayoutValid(mixed), "mixed cache valid");
        check(!b.isCachedTextLayoutValid(mixed), "cross-owner cache rejected");
        a.registerFallbackGlyph('Z', glyph('Z', 27));
        check(a.isCachedTextLayoutValid(mixed), "unrelated glyph does not invalidate cached run plan");
        a.registerFallbackGlyph('\uBC24', glyph('\uBC24', 31));
        check(!a.isCachedTextLayoutValid(mixed), "positive dependency replacement invalidates");
        check(a.isCachedTextLayoutValid(nativeOnly), "native cache survives unrelated fallback replacement");
        SkinText.CachedTextLayout negative = a.createCachedTextLayout("\u0416");
        check(a.isCachedTextLayoutValid(negative), "negative result cached");
        a.registerFallbackGlyph('\u0416', glyph('\u0416', 24));
        check(!a.isCachedTextLayoutValid(negative), "negative to positive invalidates");
        mixed = a.createCachedTextLayout("A\uBC24H");
        a.invalidateFallbackGlyphs();
        check(!a.isCachedTextLayoutValid(mixed), "generation invalidates fallback plan");
        check(a.isCachedTextLayoutValid(nativeOnly), "native-only plan survives fallback generation");
        negative = a.createCachedTextLayout("\uBC24");
        a.registerFallbackGlyph('\uBC24', glyph('\uBC24', 32));
        check(!a.isCachedTextLayoutValid(negative), "positive to negative to positive re-evaluated");
        int supplementary = 0x20000;
        char slot = a.getPuaSlotAllocator().allocateSlot(supplementary, java.util.Set.of(supplementary), null);
        a.registerFallbackGlyph(slot, glyph(slot, 29));
        SkinText.CachedTextLayout mapped = a.createCachedTextLayout(new String(Character.toChars(supplementary)));
        check(a.isCachedTextLayoutValid(mapped), "supplementary mapped layout valid");
        a.prepareFont(String.valueOf(slot));
        check(!a.isCachedTextLayoutValid(mapped), "literal PUA collision invalidates supplementary dependency");
        check(a.getFontData().getGlyph(slot) == null, "PUA glyph never inserted into original data");
        for (int i = 0; i < 50; i++) {
            a.getTextWidth("AH"); a.prepareFont("AH"); a.createCachedTextLayout("AH");
        }
        check(before.equals(metrics(a.getFontData())), "prepare/measure/cache preserve exact non-unit scale and metrics");
        check(nativeA.u == u && a.getFontData().getGlyph('A') == nativeA, "native glyph and UV identity unchanged");
        a.setAlign(99);
        boolean thrown = false;
        try { a.createCachedTextLayout("AH"); } catch (ArrayIndexOutOfBoundsException expected) { thrown = true; }
        check(thrown && before.equals(metrics(a.getFontData())), "layout exception preserves original scale");
        a.setAlign(0);
        check(a.createCachedTextLayout("AH") != null, "layout recovers after exception");
        a.setOverflow(SkinText.OVERFLOW_SHRINK); a.region.width = 0;
        check(a.createCachedTextLayout("AH") != null && before.equals(metrics(a.getFontData())), "zero-width shrink restores metrics");
        a.region.width = 50;
        check(a.createCachedTextLayout("AH") != null, "shrink recovers after zero width");
        a.region.width = 200; a.setOverflow(SkinText.OVERFLOW_OVERFLOW);
        mixed = a.createCachedTextLayout("A\uBC24H");
        a.setOutlineWidth(4f); // SDF 32px の既定縁取り(2)と異なる値 / differs from the SDF 32px default outline (2)
        check(!a.isCachedTextLayoutValid(mixed), "parameter change invalidates fallback plan");
        a.dispose(); b.dispose();
        check(!a.isCachedTextLayoutValid(nativeOnly), "disposed owner rejects cache");
        check(original.getGlyph('\uBC24') == null, "borrowed source remains unchanged");
    }

    /**
     * 画像文字の通常描画は、文字が同じでも描画高さが変わればラン幅を作り直す(空白幅 = 0.35 × 高さ)。
     * The normal image-text path rebuilds run widths when the region height changes even if the text does not
     * (space width = 0.35 × height).
     */
    private static void imageRunsFollowHeight() {
        SkinTextImage.SkinTextImageSource source = new SkinTextImage.SkinTextImageSource(false);
        source.setSize(20);
        SkinTextImage text = new SkinTextImage(source);
        text.region.set(0, 0, 400, 10);
        text.setText("  ");
        text.draw(null, 0, 0);
        float before = imageTextWidth(text);
        text.region.height = 30;
        text.draw(null, 0, 0);
        float after = imageTextWidth(text);
        check(Math.abs(before - 2 * 0.35f * 10) < 1e-3f && Math.abs(after - 2 * 0.35f * 30) < 1e-3f,
                "image text run widths follow an animated height without a text change");
    }

    private static float imageTextWidth(SkinTextImage text) {
        try {
            var field = SkinTextImage.class.getDeclaredField("textwidth");
            field.setAccessible(true);
            return field.getFloat(text);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * ビットマップ文字の共有代替組は、別々の文字を長く準備し続けても使用中のページが上限を超えない。
     * The shared fallback pair of bitmap text stays within the active page cap while many distinct characters are prepared.
     */
    private static void bitmapActiveAtlasBounded(String fontPath) {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        var previousFiles = com.badlogic.gdx.Gdx.files;
        com.badlogic.gdx.Gdx.files = new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files();
        int originalPageSize = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.getMaxTextureSize();
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize(64);
        SkinFontSource shared = new SkinFontSource(new com.badlogic.gdx.files.FileHandle(fontPath), new String[0]);
        SkinTextBitmap a = text(data(), shared);
        int maxPages = 0, prepared = 0;
        try {
            for (int cp = 0x4E00; cp < 0x4E00 + 600; cp++) {
                a.prepareFont(new String(Character.toChars(cp)));
                maxPages = Math.max(maxPages, a.getFallbackAtlasPageCount());
                prepared++;
                FontRenderRetirement.drainAfterBatch(null);
            }
            check(maxPages <= SystemFallbackAtlas.MAX_ACTIVE_PAGES,
                    prepared + " distinct characters keep the bitmap fallback pair within the cap (" + maxPages + " pages)");
        } finally {
            a.dispose(); shared.dispose();
            FontRenderRetirement.drainAfterBatch(null);
            com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize(originalPageSize);
            com.badlogic.gdx.Gdx.files = previousFiles;
        }
        check(SystemFallbackAtlas.fullCount() == 0, "full pairs are released once their borrowers leave");
    }

    /**
     * 代替の形式の分け方: .fnt の文字は .fnt の代替を先に、TTF/OTF の代替を後に使い、TTF の文字は .fnt の代替を使わない。
     * Fallback formats: .fnt text uses .fnt fallbacks before TTF/OTF ones, and TTF text never uses .fnt fallbacks.
     */
    private static void fallbackFormatSplit() {
        check(SkinTextBitmap.isBitmapFontPath("skin/font/fallback.fnt") && SkinTextBitmap.isBitmapFontPath("A.FNT")
                && !SkinTextBitmap.isBitmapFontPath("skin/font/fallback.ttf") && !SkinTextBitmap.isBitmapFontPath("x.otf")
                && !SkinTextBitmap.isBitmapFontPath(null), "fallback entries are split by format (.fnt vs TTF/OTF)");
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--review-width")) {
            nominalWidthRegression(); return;
        }
        if (args.length > 0 && args[0].equals("--review-marker")) {
            markerReplacementRegression(); return;
        }
        if (args.length > 0 && args[0].equals("--review-atlas")) {
            bitmapResetRegression("font/VL-Gothic-Regular.ttf"); return;
        }
        runPositions(); caches();
        nominalWidthRegression(); markerReplacementRegression();
        sharedBitmapBorrowers(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        bitmapResetRegression(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        sharedGlobalSource();
        idleGlobalAtlases();
        imageRunsFollowHeight();
        bitmapActiveAtlasBounded(args.length == 0 ? "font/VL-Gothic-Regular.ttf" : args[0]);
        fallbackFormatSplit();
        System.out.println("CHECKS=" + checks + " FAILURES=0 SCOPE=CPU_LAYOUT_NO_GL");
    }

    private static void nominalWidthRegression() {
        SkinTextBitmap text = text(data(), null);
        try {
            text.getFontData().setScale(1.37f, .73f);
            String before = metrics(text.getFontData());
            text.setOverflow(SkinText.OVERFLOW_OVERFLOW);
            float nominal = text.getTextWidth("AHAHAH");
            text.region.width = nominal / 3;
            text.setOverflow(SkinText.OVERFLOW_SHRINK);
            float narrow = text.getTextWidth("AHAHAH");
            System.out.println("F2 nominal=" + nominal + " region=" + text.region.width + " measured=" + narrow);
            check(narrow == nominal, "F2 shrink width query preserves nominal advance");
            check(text.createCachedTextLayout("AHAHAH") != null, "F2 draw/cache shrink still builds");
            text.region.width = 0;
            check(text.getTextWidth("AHAHAH") == nominal, "F2 zero-width region preserves nominal advance");
            check(before.equals(metrics(text.getFontData())), "F2 width query preserves exact non-unit scale and metrics");
        } finally { text.dispose(); }
    }

    private static void markerReplacementRegression() {
        SkinFontSource unavailable = new SkinFontSource("", new String[0]);
        unavailable.dispose();
        BitmapFontData original = data(); original.setGlyph('?', null);
        SkinTextBitmap text = text(original, unavailable);
        try {
            text.registerFallbackGlyph('?', glyph('?', 17));
            float oldMarker = text.getTextWidth("?");
            float oldMissing = text.getTextWidth("\u0416");
            check(oldMissing == oldMarker, "F3 negative glyph initially uses first marker metrics");
            SkinText.CachedTextLayout negative = text.createCachedTextLayout("\u0416");
            text.registerFallbackGlyph('?', glyph('?', 37));
            check(!text.isCachedTextLayoutValid(negative), "F3 marker version invalidates negative cached plan");
            text.createCachedTextLayout("\u0416");
            float newMarker = text.getTextWidth("?");
            float newMissing = text.getTextWidth("\u0416");
            System.out.println("F3 old=" + oldMissing + " newMarker=" + newMarker + " rebuiltMissing=" + newMissing);
            check(newMissing == newMarker && newMissing != oldMissing,
                    "F3 rebuilding negative glyph consumes replacement marker metrics");
            check(text.getFontData().getGlyph('?') == null && text.getFontData().getGlyph('\u0416') == null,
                    "F3 marker refresh does not mutate original glyph data");
        } finally { text.dispose(); }
    }

    /**
     * スキンの代替指定がない文字は、プログラム全体で一つの共通代替供給元を借り、部品を解放しても供給元は残る。
     * Text without a skin fallback borrows the one program-wide global source, which survives text disposal.
     */
    private static void sharedGlobalSource() {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        var previousFiles = com.badlogic.gdx.Gdx.files;
        com.badlogic.gdx.Gdx.files = new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files();
        SkinTextBitmap a = text(data(), null), b = text(data(), null);
        try {
            SkinFontSource global = SystemFallbackGlyphs.globalSource();
            check(global != null && global.isAvailable(), "global fallback source exists when font/ holds fonts");
            a.prepareFont("\u3042"); b.prepareFont("\u3042");
            check(a.getFontSource() == global && b.getFontSource() == global, "texts without a skin fallback borrow the one global source");
            check(a.getGlyphProvider() != null && b.getGlyphProvider() != null, "global source gives each text a raster provider");
            a.dispose();
            check(global.isAvailable() && SystemFallbackGlyphs.globalSource() == global, "disposing a text keeps the shared global source");
            long before = b.getGlyphRevision();
            b.prepareFont("\u3044");
            check(b.getGlyphRevision() > before, "remaining text still resolves through the global source");
        } finally {
            a.dispose(); b.dispose();
            FontRenderRetirement.drainAfterBatch(null);
            com.badlogic.gdx.Gdx.files = previousFiles;
        }
    }

    private static void sharedBitmapBorrowers(String fontPath) {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        check(com.badlogic.gdx.Gdx.gl == null, "bitmap borrower test creates no GL context");
        var previousFiles = com.badlogic.gdx.Gdx.files;
        com.badlogic.gdx.Gdx.files = new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files();
        SkinFontSource shared = new SkinFontSource(new com.badlogic.gdx.files.FileHandle(fontPath), new String[0]);
        SkinTextBitmap a = text(data(), shared), b = text(data(), shared);
        try {
            a.prepareFont("B"); b.prepareFont("C");
            SystemFallbackAtlas probe = SystemFallbackAtlas.acquire(a.getFallbackParameter(), shared, false);
            check(probe.borrowerCount() == 3, "two bitmap texts actually lease the same fallback atlas");
            check(probe.glyphCount() == 2, "each character is rasterized once per style and source");
            b.prepareFont("B");
            check(probe.glyphCount() == 2, "a second text reuses the first text's raster");
            probe.release();
            SkinText.CachedTextLayout mixed = b.createCachedTextLayout("C");
            SkinText.CachedTextLayout original = b.createCachedTextLayout("AH");
            int pending = FontRenderRetirement.pendingCount();
            a.dispose();
            check(probe.borrowerCount() == 1 && probe.isLive() && FontRenderRetirement.pendingCount() == pending,
                    "bitmap A release preserves bitmap B pages without retirement");
            check(b.isCachedTextLayoutValid(mixed), "remaining bitmap borrower's cache stays live");
            FontSession.invalidateSharedAtlases();
            check(!b.isCachedTextLayoutValid(mixed), "context invalidation rejects stale bitmap fallback layout");
            check(b.isCachedTextLayoutValid(original), "context invalidation preserves original-only bitmap layout");
            b.prepareFont("C");
            SystemFallbackAtlas replacement = SystemFallbackAtlas.acquire(b.getFallbackParameter(), shared, false);
            check(replacement != probe && replacement.isLive() && replacement.borrowerCount() == 2,
                    "bitmap borrower reacquires a fresh atlas after context loss");
            replacement.release();
            check(b.getFontData().getGlyph('C') == null && b.getRegions().size == b.getBasePageCount(),
                    "shared atlas leaves bitmap original glyph and page tables untouched");
        } finally {
            a.dispose(); b.dispose(); shared.dispose();
            FontRenderRetirement.drainAfterBatch(null);
            com.badlogic.gdx.Gdx.files = previousFiles;
        }
        check(com.badlogic.gdx.Gdx.gl == null, "bitmap borrower cleanup stays CPU-only");
    }

    /**
     * 代替を作り直しても、同じスタイル・供給元の共有ラスタを使い回し、詰め直さない(履歴が増え続けない)。
     * Rebuilding fallbacks reuses the shared raster of the same style and source without repacking, so history does not grow.
     */
    private static void bitmapResetRegression(String fontPath) {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        var previousFiles = com.badlogic.gdx.Gdx.files;
        com.badlogic.gdx.Gdx.files = new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files();
        SkinFontSource shared = new SkinFontSource(new com.badlogic.gdx.files.FileHandle(fontPath), new String[0]);
        SkinTextBitmap a = text(data(), shared), b = text(data(), shared);
        try {
            a.prepareFont("B"); b.prepareFont("C");
            SystemFallbackAtlas held = SystemFallbackAtlas.acquire(a.getFallbackParameter(), shared, false);
            int pages = held.packer.getPages().size;
            held.release();
            SkinText.CachedTextLayout survivor = b.createCachedTextLayout("C");
            for (int i = 0; i < 20; i++) {
                a.invalidateFallbackGlyphs(); a.prepareFont("B");
                SystemFallbackAtlas current = SystemFallbackAtlas.acquire(a.getFallbackParameter(), shared, false);
                try {
                    check(current == held && current.glyphCount() == 2 && current.packer.getPages().size == pages,
                            "F1 bitmap reset reuses the shared raster without repacking");
                } finally { current.release(); }
                FontRenderRetirement.drainAfterBatch(null);
                check(held.isLive() && held.borrowerCount() == 2 && b.isCachedTextLayoutValid(survivor),
                        "F1 long-lived bitmap borrower survives reset and retirement boundary");
            }
        } finally {
            a.dispose(); b.dispose(); shared.dispose();
            FontRenderRetirement.drainAfterBatch(null);
            com.badlogic.gdx.Gdx.files = previousFiles;
        }
        check(com.badlogic.gdx.Gdx.gl == null, "F1 repeated reset test stays CPU-only");
    }

    /**
     * 共通代替フォントの組は使われなくなっても残し、上限を超えたら古い順に捨てる。使用中は捨てない。GL 文脈の喪失ではすべて捨てる。
     * Global fallback pairs stay when unused and are dropped oldest first past the cap; pairs in use are never dropped.
     * Losing the GL context drops them all.
     */
    private static void idleGlobalAtlases() {
        SkinFontSource global = SystemFallbackGlyphs.globalSource();
        check(global != null, "global fallback source exists for the idle policy check");
        var styles = new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter[SystemFallbackAtlas.MAX_IDLE + 2];
        for (int i = 0; i < styles.length; i++) {
            styles[i] = new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter();
            styles[i].size = 101 + i;
        }
        SystemFallbackAtlas held = SystemFallbackAtlas.acquire(styles[0], global, true);
        SystemFallbackAtlas first = SystemFallbackAtlas.acquire(styles[1], global, true);
        first.release();
        check(first.isLive() && SystemFallbackAtlas.acquire(styles[1], global, true) == first, "an unused global style atlas is kept for reuse");
        first.release();
        for (int i = 2; i < styles.length; i++) {
            SystemFallbackAtlas.acquire(styles[i], global, true).release();
        }
        check(SystemFallbackAtlas.idleCount() <= SystemFallbackAtlas.MAX_IDLE, "unused global atlases stay within the cap");
        check(!first.isLive(), "the oldest unused global atlas is dropped first");
        check(held.isLive() && held.borrowerCount() == 1, "a global atlas in use is never dropped");
        held.release();
        FontSession.invalidateSharedAtlases();
        check(SystemFallbackAtlas.idleCount() == 0 && !held.isLive(), "context loss drops every global atlas");
        FontRenderRetirement.drainAfterBatch(null);
    }
}
