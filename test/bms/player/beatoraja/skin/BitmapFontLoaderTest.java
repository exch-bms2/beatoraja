package bms.player.beatoraja.skin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.GdxNativesLoader;
import bms.player.beatoraja.PixmapResourcePool;
import bms.player.beatoraja.skin.BitmapFontLoader.BitmapFontDataCopy;
import bms.player.beatoraja.skin.FontRenderRetirement.FontResourceDisposer;

/** GL コンテキストなしで、利用者のフォント資産を変えずにファイル形式の境界を確かめる。 / File-format boundaries without a GL context or changes to user font assets. */
public final class BitmapFontLoaderTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("bitmap-font-loader-");
        try {
            Path font = folder.resolve("font.fnt");
            String bmp = "info face=\"Test\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=64 scaleH=64 pages=1 packed=0\n"
                    + "page id=0 file=\"page.png\"\nchars count=2\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=65 x=1 y=2 width=8 height=10 xoffset=2 yoffset=3 xadvance=11 page=0 chnl=0\n"
                    + "kernings count=1\nkerning first=65 second=65 amount=-1\n";
            Files.writeString(font, bmp, StandardCharsets.UTF_8);
            FileHandle handle = new FileHandle(font.toFile());
            BitmapFontLoader.ParsedFontFile plain = BitmapFontLoader.parseFontFile(handle);
            check(plain.supplementaryGlyphs.isEmpty(), "BMP-only file has no supplemental map");
            check(plain.fontData.getGlyph('A') != null && plain.fontData.getGlyph('A').getKerning('A') == -1,
                    "BMP glyph and kerning parsed");
            String expectedPagePath = folder.resolve("page.png").toString().replace('\\', '/');
            check(plain.fontData.imagePaths.length == 1 && plain.fontData.imagePaths[0].equals(expectedPagePath),
                    "BMP relative page path resolves beside source");

            String extended = bmp.replace("chars count=2", "chars count=3").replace("kernings count=1",
                    "char id=128512 x=3 y=4 width=12 height=14 xoffset=-2 yoffset=5 xadvance=15 page=1 chnl=0\n"
                    + "kernings count=2").replace("kerning first=65 second=65 amount=-1",
                    "kerning first=65 second=65 amount=-1\nkerning first=128512 second=65 amount=-2");
            Files.writeString(font, extended, StandardCharsets.UTF_8);
            BitmapFontLoader.ParsedFontFile parsed = BitmapFontLoader.parseFontFile(handle);
            BitmapFont.Glyph glyph = parsed.supplementaryGlyphs.get(128512);
            check(parsed.supplementaryGlyphs.size() == 1 && glyph != null, "supplementary glyph retained outside BMP reader");
            check(glyph.id == 128512 && glyph.page == 1 && glyph.xadvance == 15, "id, page and advance retained");
            check(glyph.yoffset == -19 && glyph.xoffset == -2, "bitmap coordinate convention retained");
            check(parsed.fontData.getGlyph('A') != null && parsed.fontData.getGlyph('A').getKerning('A') == -1,
                    "BMP kerning survives alongside supplementary kerning");
            check(parsed.fontData.imagePaths.length == 1 && parsed.fontData.imagePaths[0].equals(expectedPagePath),
                    "relative page paths still resolve beside source");
            check(Files.readString(font, StandardCharsets.UTF_8).equals(extended), "source .fnt remains untouched");

            BitmapFont.BitmapFontData original = new BitmapFont.BitmapFontData();
            BitmapFont.Glyph a = new BitmapFont.Glyph();
            a.id = 65; a.width = 8; a.height = 10; a.yoffset = -13; a.u = .2f; a.v2 = .8f;
            a.setKerning('B', -2);
            original.setGlyph('A', a); original.missingGlyph = a;
            original.breakChars = new char[] { '-' };
            BitmapFont.BitmapFontData copy = BitmapFontDataCopy.copyFontData(original);
            check(copy != original && copy.getGlyph('A') != a, "glyph data is not shared");
            copy.getGlyph('A').setKerning('B', -4); copy.breakChars[0] = '+';
            check(a.getKerning('B') == -2 && original.breakChars[0] == '-', "kerning and character arrays are independent");
            check(copy.getGlyph('A').u == .2f && copy.getGlyph('A').v2 == .8f, "original UV values preserved");
            check(copy.missingGlyph == null && original.missingGlyph == a, "fallback lookup leaves original missing-glyph policy untouched");
            check(SkinTextBitmap.SkinTextBitmapSource.getRepresentativeGlyphYOffset(copy) == 3, "public baseline entry point still delegates correctly");
            check(BitmapFontDataCopy.copyGlyph(null) == null && BitmapFontDataCopy.copyFontData(null) == null, "null-copy contract preserved");

            // ページ画像がない .fnt: 例外を出さず空の結果を返し、原因を一度記録する。
            // .fnt whose page image is missing: no exception, an empty result, and the cause logged once.
            var logged = new ArrayList<String>();
            var capture = new Handler() {
                @Override public void publish(LogRecord r) { logged.add(r.getMessage()); }
                @Override public void flush() { }
                @Override public void close() { }
            };
            Logger.getGlobal().addHandler(capture);
            BitmapFontCache.CacheableBitmapFont failed;
            try {
                failed = BitmapFontLoader.load(font, 0, false, false);
            } finally {
                Logger.getGlobal().removeHandler(capture);
            }
            check(failed != null && failed.fontData == null && failed.regions == null && failed.font == null,
                    "missing page yields an empty result without throwing");
            check(logged.stream().filter(m -> m.startsWith("Bitmap font load failed: ")).count() == 1,
                    "load failure is logged once");

            // 一覧版の解放も配列版と同じ規則: 全部試し、実行時例外は IllegalStateException にまとめる。
            // The list form follows the array rule: try everything, wrap runtime failures in one IllegalStateException.
            int[] attempts = new int[1];
            com.badlogic.gdx.utils.Disposable broken = () -> { attempts[0]++; throw new IllegalArgumentException("broken"); };
            com.badlogic.gdx.utils.Disposable broken2 = () -> { attempts[0]++; throw new IllegalArgumentException("broken2"); };
            IllegalStateException wrapped = null;
            try { FontResourceDisposer.disposeAll(List.of(broken, broken2, broken)); }
            catch (IllegalStateException e) { wrapped = e; }
            check(attempts[0] == 2, "each distinct resource is attempted once");
            check(wrapped != null && wrapped.getSuppressed().length == 2, "both failures are reported");

            testSinglePassParserEquivalence(folder);
            testSharedGlyphViewContracts();
            testMultiPageAndPreviewLoading(folder);
            System.out.println("BitmapFontLoaderTest PASS checks=" + checks);
        } finally {
            try (var files = Files.walk(folder)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }

    private static void testSinglePassParserEquivalence(Path folder) throws Exception {
        String baseFnt = "info face=\"Equiv\" size=32 bold=0 italic=0 padding=1,2,3,4 spacing=0,0\r\n"
                + "common lineHeight=38 base=29 scaleW=256 scaleH=128 pages=2 packed=0\r\n"
                + "page id=0 file=\"eq0.png\"\r\n"
                + "page id=1 file=\"eq1.png\"\r\n"
                + "chars count=6\r\n"
                + "char id=0 x=0 y=0 width=8 height=12 xoffset=1 yoffset=4 xadvance=10 page=0 chnl=0\r\n"
                + "char id=65 x=10 y=2 width=14 height=20 xoffset=1 yoffset=5 xadvance=16 page=0 chnl=0\r\n"
                + "char id=83 x=26 y=2 width=13 height=19 xoffset=0 yoffset=6 xadvance=15 page=1 chnl=0\r\n"
                + "char id=108 x=40 y=4 width=6 height=21 xoffset=1 yoffset=3 xadvance=8 page=0 chnl=0\r\n"
                + "char id=120 x=48 y=8 width=11 height=13 xoffset=1 yoffset=9 xadvance=13 page=0 chnl=0\r\n"
                + "char id=128512 x=60 y=2 width=20 height=20 xoffset=-1 yoffset=2 xadvance=22 page=1 chnl=0\r\n"
                + "kernings count=2\r\n"
                + "kerning first=65 second=83 amount=-2\r\n"
                + "kerning first=128512 second=65 amount=-3\r\n";
        Path standardPath = folder.resolve("equiv-standard.fnt");
        Files.writeString(standardPath, baseFnt, StandardCharsets.UTF_8);
        assertMatchesLibGdx(standardPath, 32f, 29, 256f, 128f, 6, 1);

        Path metricsPath = folder.resolve("equiv-metrics.fnt");
        String withMetrics = baseFnt + "metrics ascent=18.5 descent=-6.5 down=-38.0 capHeight=19.0 lineHeight=38.0 spaceXadvance=8.0 xHeight=11.0\r\n";
        Files.writeString(metricsPath, withMetrics, StandardCharsets.UTF_8);
        assertMatchesLibGdx(metricsPath, 32f, 29, 256f, 128f, 6, 1);

        Path fallbackAnchorPath = folder.resolve("equiv-anchor-fallback.fnt");
        String noAnchors = "info face=\"FallbackAnchor\" size=20 padding=0,0,0,0 spacing=0,0\n"
                + "common lineHeight=24 base=18 scaleW=128 scaleH=64 pages=1 packed=0\n"
                + "page id=0 file=\"eq0.png\"\n"
                + "chars count=2\n"
                + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=5 page=0 chnl=0\n"
                + "char id=66 x=4 y=5 width=10 height=14 xoffset=1 yoffset=7 xadvance=12 page=0 chnl=0\n"
                + "kernings count=0\n";
        Files.writeString(fallbackAnchorPath, noAnchors, StandardCharsets.UTF_8);
        assertMatchesLibGdx(fallbackAnchorPath, 20f, 18, 128f, 64f, 7, 0);
    }

    private static void assertMatchesLibGdx(Path fontPath, float expectedSize, int expectedBase,
            float expectedPageW, float expectedPageH, int expectedYOffset, int expectedSupplementaryCount) {
        FileHandle handle = new FileHandle(fontPath.toFile());
        BitmapFontData expected = new BitmapFontData(stripSupplementaryForLibGdx(handle), false);
        BitmapFontLoader.ParsedFontFile parsed = BitmapFontLoader.parseFontFile(handle);
        BitmapFontData actual = parsed.fontData;

        check(Objects.equals(expected.name, actual.name), "font name matches libGDX");
        check(Arrays.equals(expected.imagePaths, actual.imagePaths), "imagePaths match libGDX");
        check(expected.padTop == actual.padTop && expected.padRight == actual.padRight
                && expected.padBottom == actual.padBottom && expected.padLeft == actual.padLeft, "padding matches libGDX");
        check(expected.lineHeight == actual.lineHeight && expected.capHeight == actual.capHeight
                && expected.ascent == actual.ascent && expected.descent == actual.descent
                && expected.down == actual.down && expected.spaceXadvance == actual.spaceXadvance
                && expected.xHeight == actual.xHeight, "metrics match libGDX");
        check(glyphFieldsEqual(expected.missingGlyph, actual.missingGlyph), "missingGlyph matches libGDX");

        for (int ch = 0; ch <= Character.MAX_VALUE; ch++) {
            Glyph eg = expected.getGlyph((char) ch);
            Glyph ag = actual.getGlyph((char) ch);
            if (!glyphFieldsEqual(eg, ag)) {
                throw new AssertionError("Glyph mismatch for code point " + ch + " in " + fontPath);
            }
        }
        check(parsed.supplementaryGlyphs.size() == expectedSupplementaryCount, "supplementary count matches");
        if (expectedSupplementaryCount == 1) {
            Glyph sup = parsed.supplementaryGlyphs.get(128512);
            check(sup != null && sup.id == 128512 && sup.srcX == 60 && sup.srcY == 2
                    && sup.width == 20 && sup.height == 20 && sup.xoffset == -1
                    && sup.yoffset == -22 && sup.xadvance == 22 && sup.page == 1,
                    "supplementary glyph fields match");
        }
        check(parsed.headerMetadataValid && parsed.originalSize == expectedSize && parsed.base == expectedBase
                && parsed.pageWidth == expectedPageW && parsed.pageHeight == expectedPageH
                && parsed.glyphYOffset == expectedYOffset, "parsed header metadata and yoffset match");
    }

    private static FileHandle stripSupplementaryForLibGdx(FileHandle fontFile) {
        String[] lines = fontFile.readString(StandardCharsets.UTF_8.name()).split("\\r?\\n", -1);
        int charCount = 0;
        int kerningCount = 0;
        boolean hasSupplementary = false;
        for (String line : lines) {
            if (line.startsWith("char ")) {
                int cp = Integer.parseInt(extractAttr(line, "id="));
                if (cp > Character.MAX_VALUE) {
                    hasSupplementary = true;
                } else {
                    charCount++;
                }
            } else if (line.startsWith("kerning ")) {
                int first = Integer.parseInt(extractAttr(line, "first="));
                int second = Integer.parseInt(extractAttr(line, "second="));
                if (first > Character.MAX_VALUE || second > Character.MAX_VALUE) {
                    hasSupplementary = true;
                } else {
                    kerningCount++;
                }
            }
        }
        if (!hasSupplementary) {
            return fontFile;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("char ") && Integer.parseInt(extractAttr(line, "id=")) > Character.MAX_VALUE) {
                continue;
            }
            if (line.startsWith("kerning ")
                    && (Integer.parseInt(extractAttr(line, "first=")) > Character.MAX_VALUE
                    || Integer.parseInt(extractAttr(line, "second=")) > Character.MAX_VALUE)) {
                continue;
            }
            if (sb.length() > 0) sb.append('\n');
            if (line.startsWith("chars count=")) {
                sb.append("chars count=").append(charCount);
            } else if (line.startsWith("kernings count=")) {
                sb.append("kernings count=").append(kerningCount);
            } else {
                sb.append(line);
            }
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new FileHandle(fontFile.file()) {
            @Override
            public InputStream read() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public FileHandle parent() {
                return fontFile.parent();
            }
        };
    }

    private static String extractAttr(String line, String prefix) {
        int start = line.indexOf(prefix) + prefix.length();
        int end = start;
        if (end < line.length() && line.charAt(end) == '-') end++;
        while (end < line.length() && Character.isDigit(line.charAt(end))) end++;
        return line.substring(start, end);
    }

    private static boolean glyphFieldsEqual(Glyph a, Glyph b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.id != b.id || a.srcX != b.srcX || a.srcY != b.srcY || a.width != b.width || a.height != b.height
                || a.xoffset != b.xoffset || a.yoffset != b.yoffset || a.xadvance != b.xadvance || a.page != b.page) {
            return false;
        }
        if ((a.kerning == null) != (b.kerning == null)) return false;
        if (a.kerning != null) {
            for (int i = 0; i < a.kerning.length; i++) {
                if (!Arrays.equals(a.kerning[i], b.kerning[i])) return false;
            }
        }
        return true;
    }

    private static void testSharedGlyphViewContracts() {
        check(BitmapFontDataCopy.shareGlyphs(null) == null, "shareGlyphs(null) returns null");

        BitmapFontData original = new BitmapFontData();
        Glyph a = new Glyph();
        a.id = 'A';
        a.srcX = 1;
        a.srcY = 2;
        a.width = 8;
        a.height = 10;
        a.u = 0.1f;
        a.v = 0.9f;
        a.u2 = 0.3f;
        a.v2 = 0.5f;
        a.setKerning('B', -2);
        original.setGlyph('A', a);
        original.missingGlyph = a;
        original.breakChars = new char[] { '-' };

        BitmapFontData view1 = BitmapFontDataCopy.shareGlyphs(original);
        check(view1 != original && view1.getGlyph('A') == a, "shareGlyphs shares Glyph instances");
        check(view1.missingGlyph == null && original.missingGlyph == a,
                "shareGlyphs clears missingGlyph on view without altering source");

        view1.setScale(2f, 3f);
        view1.breakChars[0] = '+';
        check(original.scaleX == 1f && original.scaleY == 1f && original.breakChars[0] == '-',
                "shareGlyphs isolates scalar metrics and char arrays");

        BitmapFontData view2 = BitmapFontDataCopy.shareGlyphs(view1);
        Glyph replacement1 = new Glyph();
        replacement1.id = 'A';
        Glyph replacement2 = new Glyph();
        replacement2.id = 'A';

        view1.setGlyph('A', replacement1);
        check(original.getGlyph('A') == a && view1.getGlyph('A') == replacement1 && view2.getGlyph('A') == a,
                "intermediate view copy-on-write leaves original and downstream view intact");

        view2.setGlyph('A', replacement2);
        check(original.getGlyph('A') == a && view1.getGlyph('A') == replacement1 && view2.getGlyph('A') == replacement2,
                "downstream view copy-on-write leaves original and intermediate view intact");

        BitmapFontData puaData = new BitmapFontData();
        puaData.setGlyph((char) 0xDFFF, new Glyph());
        puaData.setGlyph((char) 0xE000, new Glyph());
        puaData.setGlyph((char) 0xE200, new Glyph());
        puaData.setGlyph((char) 0xF8FF, new Glyph());
        puaData.setGlyph((char) 0xF900, new Glyph());

        MissingGlyphMapper.PuaSlotAllocator allocator = new MissingGlyphMapper.PuaSlotAllocator();
        allocator.reserveNativeGlyphs(puaData);
        check(allocator.isReserved((char) 0xE000) && allocator.isReserved((char) 0xE200)
                && allocator.isReserved((char) 0xF8FF),
                "reserveNativeGlyphs reserves PUA boundaries and middle pages");
        check(!allocator.isReserved((char) 0xE001) && !allocator.isReserved((char) 0xF8FE),
                "reserveNativeGlyphs leaves unpopulated PUA slots free");
        Character allocated = allocator.allocateSlot(0x20000, java.util.Set.of(0x20000), null);
        check(allocated != null && allocated.charValue() == 0xE001,
                "first allocated supplementary PUA slot skips reserved 0xE000");
    }

    private static void testMultiPageAndPreviewLoading(Path folder) throws Exception {
        GdxNativesLoader.load();
        var previousFiles = Gdx.files;
        Gdx.files = new Lwjgl3Files();
        try (var gl = new MockTextureEnvironment()) {
            writePng(folder.resolve("p0.png"), 32, 16);
            writePng(folder.resolve("p1.png"), 16, 16);
            writePng(folder.resolve("p2.png"), 20, 16);
            writePng(folder.resolve("p3.png"), 24, 16);

            Path missingMetaFont = folder.resolve("missing-meta.fnt");
            Files.writeString(missingMetaFont,
                    "info face=\"NoMeta\" padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 pages=1 packed=0\n"
                    + "page id=0 file=\"p0.png\"\n"
                    + "chars count=2\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=83 x=1 y=2 width=8 height=10 xoffset=1 yoffset=4 xadvance=10 page=0 chnl=0\n",
                    StandardCharsets.UTF_8);
            var noMeta = BitmapFontLoader.load(missingMetaFont, 0, false, false);
            check(noMeta.fontData != null && noMeta.regions.size == 1, "metadata-fallback font loads");
            check(noMeta.originalSize == noMeta.fontData.lineHeight
                    && noMeta.base == (int) noMeta.fontData.ascent
                    && noMeta.pageWidth == 32f && noMeta.pageHeight == 16f
                    && noMeta.glyphYOffset == 4, "metadata fallback uses BitmapFontData and region0 metrics");
            noMeta.font.setOwnsTexture(true);
            int noMetaHandle = noMeta.regions.get(0).getTexture().getTextureObjectHandle();
            noMeta.font.dispose();
            gl.assertDeletedOnce(noMetaHandle);

            SkinLoader.getResource().dispose();
            Path fourPageFont = folder.resolve("four-page.fnt");
            Files.writeString(fourPageFont,
                    "info face=\"Four\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=32 scaleH=16 pages=4 packed=0\n"
                    + "page id=0 file=\"p0.png\"\n"
                    + "page id=1 file=\"p1.png\"\n"
                    + "page id=2 file=\"p2.png\"\n"
                    + "page id=3 file=\"p3.png\"\n"
                    + "chars count=5\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=0 chnl=0\n"
                    + "char id=66 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=1 chnl=0\n"
                    + "char id=67 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=2 chnl=0\n"
                    + "char id=68 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=3 chnl=0\n",
                    StandardCharsets.UTF_8);
            var fourLoaded = BitmapFontLoader.load(fourPageFont, 0, false, false);
            check(fourLoaded.font != null && fourLoaded.regions.size == 4, "4-page parallel font loads all pages");
            check(fourLoaded.regions.get(0).getRegionWidth() == 32
                    && fourLoaded.regions.get(1).getRegionWidth() == 16
                    && fourLoaded.regions.get(2).getRegionWidth() == 20
                    && fourLoaded.regions.get(3).getRegionWidth() == 24,
                    "4-page parallel regions preserve exact page order");
            for (char c : new char[] { 'A', 'B', 'C', 'D' }) {
                Glyph g = fourLoaded.fontData.getGlyph(c);
                check(g != null && g.u2 > g.u && g.v > g.v2, "UV bound for glyph " + c);
            }
            int[] fourHandles = handlesOf(fourLoaded);
            fourLoaded.font.setOwnsTexture(true);
            fourLoaded.font.dispose();
            for (int h : fourHandles) gl.assertDeletedOnce(h);

            SkinLoader.getResource().dispose();
            Path dupPageFont = folder.resolve("dup-page.fnt");
            Files.writeString(dupPageFont,
                    "info face=\"Dup\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=16 scaleH=16 pages=3 packed=0\n"
                    + "page id=0 file=\"p1.png\"\n"
                    + "page id=1 file=\"p2.png\"\n"
                    + "page id=2 file=\"p1.png\"\n"
                    + "chars count=2\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=2 chnl=0\n",
                    StandardCharsets.UTF_8);
            var dupLoaded = BitmapFontLoader.load(dupPageFont, 0, false, false);
            check(dupLoaded.regions.size == 3
                    && dupLoaded.regions.get(0).getTexture() != dupLoaded.regions.get(2).getTexture(),
                    "duplicate page paths receive distinct Texture instances");
            int[] dupHandles = handlesOf(dupLoaded);
            dupLoaded.font.setOwnsTexture(true);
            dupLoaded.font.dispose();
            for (int h : dupHandles) gl.assertDeletedOnce(h);

            // 既キャッシュ 2 ページの Texture 生成後に別ページ失敗で全破棄される経路を踏む
            SkinLoader.getResource().dispose();
            SkinLoader.getResource().getPixmap(folder.resolve("p0.png").toString());
            SkinLoader.getResource().getPixmap(folder.resolve("p1.png").toString());
            Path partialFourFont = folder.resolve("partial-four.fnt");
            Files.writeString(partialFourFont,
                    "info face=\"Partial4\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=32 scaleH=16 pages=4 packed=0\n"
                    + "page id=0 file=\"p0.png\"\n"
                    + "page id=1 file=\"p1.png\"\n"
                    + "page id=2 file=\"p2.png\"\n"
                    + "page id=3 file=\"missing-p3.png\"\n"
                    + "chars count=1\n"
                    + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=0 chnl=0\n",
                    StandardCharsets.UTF_8);
            assertPartialLoadCleansAllTextures(gl, partialFourFont, 2);

            Path partialTwoFont = folder.resolve("partial-two.fnt");
            Files.writeString(partialTwoFont,
                    "info face=\"Partial2\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=32 scaleH=16 pages=2 packed=0\n"
                    + "page id=0 file=\"p0.png\"\n"
                    + "page id=1 file=\"missing-p1.png\"\n"
                    + "chars count=1\n"
                    + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=0 chnl=0\n",
                    StandardCharsets.UTF_8);
            assertPartialLoadCleansAllTextures(gl, partialTwoFont, 1);

            SkinLoader.getResource().dispose();
            check(SkinLoader.getResource().size() == 0, "global PixmapResourcePool starts empty");
            @SuppressWarnings("unchecked")
            ThreadLocal<PixmapResourcePool> previewLocal = getPreviewResourcesThreadLocal();
            PixmapResourcePool previousPreview = previewLocal.get();
            PixmapResourcePool temporaryPreview = new PixmapResourcePool();
            previewLocal.set(temporaryPreview);
            try {
                var previewLoaded = BitmapFontLoader.load(fourPageFont, 0, true, false);
                check(previewLoaded.font != null && previewLoaded.regions.size == 4, "preview 4-page font loads");
                check(SkinLoader.getResource().size() == 0, "preview parallel load never touches global PixmapResourcePool");
                check(temporaryPreview.size() == 4, "preview parallel load populates the ThreadLocal temporary pool");
                try (var stream = Files.list(folder)) {
                    boolean anyCim = stream.anyMatch(p -> p.getFileName().toString().endsWith(".cim"));
                    check(!anyCim, "preview load skips .cim writes even when usecim=true");
                }
                int[] previewHandles = handlesOf(previewLoaded);
                previewLoaded.font.setOwnsTexture(true);
                previewLoaded.font.dispose();
                for (int h : previewHandles) gl.assertDeletedOnce(h);
            } finally {
                if (previousPreview == null) {
                    previewLocal.remove();
                } else {
                    previewLocal.set(previousPreview);
                }
                temporaryPreview.dispose();
            }

            testSkinTextBitmapSharedSource(gl, folder);
            testInFlightWorkerDrainsBeforeFailureReturn(folder);
        } finally {
            SkinLoader.getResource().dispose();
            Gdx.files = previousFiles;
        }
    }

    private static void testSkinTextBitmapSharedSource(MockTextureEnvironment gl, Path folder) {
        SkinLoader.getResource().dispose();
        Path sharedFont = folder.resolve("shared-source.fnt");
        try {
            Files.writeString(sharedFont,
                    "info face=\"Shared\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=32 scaleH=16 pages=2 packed=0\n"
                    + "page id=0 file=\"p0.png\"\n"
                    + "page id=1 file=\"p1.png\"\n"
                    + "chars count=3\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=0 chnl=0\n"
                    + "char id=83 x=2 y=2 width=7 height=10 xoffset=0 yoffset=2 xadvance=9 page=1 chnl=0\n",
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        SkinTextBitmap.SkinTextBitmapSource source = new SkinTextBitmap.SkinTextBitmapSource(sharedFont, false, false);
        SkinTextBitmap t1 = new SkinTextBitmap(source, 24f);
        SkinTextBitmap t2 = new SkinTextBitmap(source, 48f);
        try {
            BitmapFontCache.CacheableBitmapFont cached = BitmapFontCache.Get(sharedFont, 0);
            check(cached != null && source.getFontData() != null && source.getFontData() != cached.fontData,
                    "SkinTextBitmapSource uses an isolated BitmapFontData view from cache");
            check(t1.getFontData() != null && t2.getFontData() != null
                    && t1.getFontData() != t2.getFontData()
                    && t1.getFontData() != source.getFontData(),
                    "SkinTextBitmap instances own distinct BitmapFontData views");
            check(t1.getFontData().missingGlyph == null && t2.getFontData().missingGlyph == null,
                    "shared glyph view clears missingGlyph");

            Glyph gSource = source.getFontData().getGlyph('A');
            Glyph g1 = t1.getFontData().getGlyph('A');
            Glyph g2 = t2.getFontData().getGlyph('A');
            check(gSource != null && g1 == gSource && g2 == gSource,
                    "SkinTextBitmap instances share Glyph references with SkinTextBitmapSource");
            check(g1.u2 > g1.u && g1.v > g1.v2, "shared glyph retains bound UV coordinates");

            t1.getFontData().setScale(2f, 3f);
            check(t2.getFontData().scaleX == 1f && t2.getFontData().scaleY == 1f
                    && source.getFontData().scaleX == 1f && source.getFontData().scaleY == 1f,
                    "metric scale changes on one SkinTextBitmap do not leak to peers or source");

            float w1 = t1.getTextWidth("AS");
            float w2 = t2.getTextWidth("AS");
            check(w1 > 0f && w2 > w1, "layout measurement scales per SkinTextBitmap size");
            check(t1.createCachedTextLayout("AS") != null, "cached text layout builds on shared glyph view");
        } finally {
            int[] handles = new int[source.getRegions().size];
            for (int i = 0; i < handles.length; i++) {
                handles[i] = source.getRegions().get(i).getTexture().getTextureObjectHandle();
            }
            t1.dispose();
            t2.dispose();
            check(BitmapFontCache.Has(sharedFont, 0), "cache retains font until source disposes");
            source.dispose();
            check(!BitmapFontCache.Has(sharedFont, 0), "cache releases font after source disposes");
            for (int h : handles) {
                gl.assertDeletedOnce(h);
            }
        }

        Path fallbackFont = folder.resolve("shared-fallback.fnt");
        try {
            Files.writeString(fallbackFont,
                    "info face=\"Fallback\" size=24 padding=0,0,0,0 spacing=0,0\n"
                    + "common lineHeight=24 base=20 scaleW=20 scaleH=16 pages=1 packed=0\n"
                    + "page id=0 file=\"p2.png\"\n"
                    + "chars count=2\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=90 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=12 page=0 chnl=0\n",
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        SkinTextBitmap.SkinTextBitmapSource fallbackEnabledSource = new SkinTextBitmap.SkinTextBitmapSource(
                sharedFont,
                new SkinTextBitmap.SkinTextBitmapSource.FallbackFont[] {
                        new SkinTextBitmap.SkinTextBitmapSource.FallbackFont(
                                fallbackFont, SkinTextBitmap.SkinTextBitmapSource.TYPE_STANDARD)
                },
                false, false);
        SkinTextBitmap ft1 = new SkinTextBitmap(fallbackEnabledSource, 24f);
        SkinTextBitmap ft2 = new SkinTextBitmap(fallbackEnabledSource, 48f);
        try {
            check(ft1.getFontData().getGlyph('A') == ft2.getFontData().getGlyph('A')
                    && ft1.getFontData().getGlyph('A') == fallbackEnabledSource.getFontData().getGlyph('A'),
                    "primary glyphs stay shared when fallback .fnt is attached");
            check(ft1.getFontData().getGlyph('Z') == null,
                    "fallback .fnt glyph is not inserted into shared primary fontData");
            float widthA = ft1.getTextWidth("A");
            float widthAZ = ft1.getTextWidth("AZ");
            check(widthAZ > widthA, "fallback .fnt glyph 'Z' resolves and extends measured width");
            check(BitmapFontCache.Has(sharedFont, 0) && BitmapFontCache.Has(fallbackFont, 0),
                    "cache holds both primary and fallback fonts");
        } finally {
            int[] handles = new int[fallbackEnabledSource.getRegions().size];
            for (int i = 0; i < handles.length; i++) {
                handles[i] = fallbackEnabledSource.getRegions().get(i).getTexture().getTextureObjectHandle();
            }
            check(handles.length == 3, "primary (2) + fallback (1) regions tracked for disposal");
            ft1.dispose();
            ft2.dispose();
            fallbackEnabledSource.dispose();
            check(!BitmapFontCache.Has(sharedFont, 0) && !BitmapFontCache.Has(fallbackFont, 0),
                    "disposing fallback-enabled source releases both cached fonts");
            for (int h : handles) {
                gl.assertDeletedOnce(h);
            }
        }
    }

    private static void testInFlightWorkerDrainsBeforeFailureReturn(Path folder) throws Exception {
        Path slowFailFont = folder.resolve("slow-fail.fnt");
        Files.writeString(slowFailFont,
                "info face=\"SlowFail\" size=24 padding=0,0,0,0 spacing=0,0\n"
                + "common lineHeight=24 base=20 scaleW=32 scaleH=16 pages=2 packed=0\n"
                + "page id=0 file=\"slow-p0.png\"\n"
                + "page id=1 file=\"fail-p1.png\"\n"
                + "chars count=2\n"
                + "char id=65 x=1 y=1 width=8 height=10 xoffset=0 yoffset=2 xadvance=10 page=0 chnl=0\n"
                + "char id=128512 x=10 y=1 width=10 height=10 xoffset=0 yoffset=2 xadvance=12 page=0 chnl=0\n",
                StandardCharsets.UTF_8);

        CountDownLatch slowStarted = new CountDownLatch(1);
        AtomicBoolean slowFinished = new AtomicBoolean(false);
        PixmapResourcePool slowPool = new PixmapResourcePool() {
            @Override
            protected PixmapResource load(String path) {
                String fileName = Path.of(path).getFileName().toString();
                if ("slow-p0.png".equals(fileName)) {
                    slowStarted.countDown();
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("slow worker should not be interrupted", e);
                    }
                    Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
                    slowFinished.set(true);
                    return new PixmapResource(pixmap);
                }
                if ("fail-p1.png".equals(fileName)) {
                    try {
                        if (!slowStarted.await(5, TimeUnit.SECONDS)) {
                            throw new AssertionError("slow worker never started");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    return null;
                }
                return super.load(path);
            }
        };

        ThreadLocal<PixmapResourcePool> previewLocal = getPreviewResourcesThreadLocal();
        PixmapResourcePool previousPreview = previewLocal.get();
        previewLocal.set(slowPool);
        var capture = new Handler() {
            @Override public void publish(LogRecord r) { }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Logger.getGlobal().addHandler(capture);
        BitmapFontCache.CacheableBitmapFont failed;
        try {
            failed = BitmapFontLoader.load(slowFailFont, 0, false, false);
        } finally {
            Logger.getGlobal().removeHandler(capture);
            if (previousPreview == null) {
                previewLocal.remove();
            } else {
                previewLocal.set(previousPreview);
            }
        }
        try {
            check(slowFinished.get(), "in-flight slow worker completed before load() returned on failure");
            check(slowPool.size() == 1, "in-flight slow worker registered its Pixmap before pool disposal");
            check(failed != null && failed.fontData == null && failed.regions == null && failed.font == null,
                    "slow/fail font load returns empty result");
            check(failed.supplementaryGlyphs != null && failed.supplementaryGlyphs.isEmpty(),
                    "failed font load resets supplementaryGlyphs to empty");
        } finally {
            slowPool.dispose();
        }
    }

    private static void assertPartialLoadCleansAllTextures(MockTextureEnvironment gl, Path fontPath, int minExpectedTextures) {
        Texture beforeProbe = gl.createTexture();
        int beforeHandle = beforeProbe.getTextureObjectHandle();
        beforeProbe.dispose();
        gl.assertDeletedOnce(beforeHandle);

        var logged = new ArrayList<String>();
        var capture = new Handler() {
            @Override public void publish(LogRecord r) { logged.add(r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Logger.getGlobal().addHandler(capture);
        BitmapFontCache.CacheableBitmapFont failed;
        try {
            failed = BitmapFontLoader.load(fontPath, 0, false, false);
        } finally {
            Logger.getGlobal().removeHandler(capture);
        }

        Texture afterProbe = gl.createTexture();
        int afterHandle = afterProbe.getTextureObjectHandle();
        afterProbe.dispose();
        gl.assertDeletedOnce(afterHandle);

        int createdCount = afterHandle - beforeHandle - 1;
        check(createdCount >= minExpectedTextures,
                "expected at least " + minExpectedTextures + " textures created before failure, got " + createdCount);
        for (int h = beforeHandle + 1; h < afterHandle; h++) {
            gl.assertDeletedOnce(h);
        }
        check(failed.fontData == null && failed.regions == null && failed.font == null,
                "partial page failure returns empty result");
        check(logged.stream().filter(m -> m.startsWith("Bitmap font load failed: ")).count() == 1,
                "partial page failure logs once");
    }

    private static int[] handlesOf(BitmapFontCache.CacheableBitmapFont loaded) {
        int[] handles = new int[loaded.regions.size];
        for (int i = 0; i < loaded.regions.size; i++) {
            handles[i] = loaded.regions.get(i).getTexture().getTextureObjectHandle();
        }
        return handles;
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<PixmapResourcePool> getPreviewResourcesThreadLocal() throws ReflectiveOperationException {
        Field field = SkinLoader.class.getDeclaredField("previewResources");
        field.setAccessible(true);
        return (ThreadLocal<PixmapResourcePool>) field.get(null);
    }

    private static void writePng(Path path, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        try {
            PixmapIO.writePNG(new FileHandle(path.toFile()), pixmap);
        } finally {
            pixmap.dispose();
        }
    }
}
