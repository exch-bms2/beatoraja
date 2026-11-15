package bms.player.beatoraja.skin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
            String bmp = "info face=\"Test\" size=24\n"
                    + "common lineHeight=24 base=20 scaleW=64 scaleH=64 pages=1 packed=0\n"
                    + "page id=0 file=\"page.png\"\nchars count=2\n"
                    + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=6 page=0 chnl=0\n"
                    + "char id=65 x=1 y=2 width=8 height=10 xoffset=2 yoffset=3 xadvance=11 page=0 chnl=0\n"
                    + "kernings count=1\nkerning first=65 second=65 amount=-1\n";
            Files.writeString(font, bmp, StandardCharsets.UTF_8);
            FileHandle handle = new FileHandle(font.toFile());
            BitmapFontLoader.RemappedFontFile plain = BitmapFontLoader.createRemappedFontFile(handle);
            check(plain.fileHandle == handle, "BMP-only file keeps its original handle");
            check(plain.supplementaryGlyphs.isEmpty(), "BMP-only file has no supplemental map");
            String extended = bmp.replace("chars count=2", "chars count=3").replace("kernings count=1",
                    "char id=128512 x=3 y=4 width=12 height=14 xoffset=-2 yoffset=5 xadvance=15 page=1 chnl=0\n"
                    + "kernings count=2").replace("kerning first=65 second=65 amount=-1",
                    "kerning first=65 second=65 amount=-1\nkerning first=128512 second=65 amount=-2");
            Files.writeString(font, extended, StandardCharsets.UTF_8);
            BitmapFontLoader.RemappedFontFile parsed = BitmapFontLoader.createRemappedFontFile(handle);
            BitmapFont.Glyph glyph = parsed.supplementaryGlyphs.get(128512);
            check(parsed.supplementaryGlyphs.size() == 1 && glyph != null, "supplementary glyph retained outside BMP reader");
            check(glyph.id == 128512 && glyph.page == 1 && glyph.xadvance == 15, "id, page and advance retained");
            check(glyph.yoffset == -19 && glyph.xoffset == -2, "bitmap coordinate convention retained");
            String rewritten;
            try (var input = parsed.fileHandle.read()) {
                rewritten = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            check(rewritten.contains("chars count=2") && rewritten.contains("kernings count=1"), "filtered counts match BMP records");
            check(!rewritten.contains("128512") && rewritten.contains("kerning first=65 second=65 amount=-1"), "BMP kerning survives and supplemental kerning is filtered");
            check(parsed.fileHandle.parent().file().equals(folder.toFile()), "relative page paths still resolve beside source");
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
            var logged = new java.util.ArrayList<String>();
            var capture = new java.util.logging.Handler() {
                @Override public void publish(java.util.logging.LogRecord r) { logged.add(r.getMessage()); }
                @Override public void flush() { }
                @Override public void close() { }
            };
            java.util.logging.Logger.getGlobal().addHandler(capture);
            BitmapFontCache.CacheableBitmapFont failed;
            try {
                failed = BitmapFontLoader.load(font, 0, false, false);
            } finally {
                java.util.logging.Logger.getGlobal().removeHandler(capture);
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
            try { FontResourceDisposer.disposeAll(java.util.List.of(broken, broken2, broken)); }
            catch (IllegalStateException e) { wrapped = e; }
            check(attempts[0] == 2, "each distinct resource is attempted once");
            check(wrapped != null && wrapped.getSuppressed().length == 2, "both failures are reported");
            System.out.println("BitmapFontLoaderTest PASS checks=" + checks);
        } finally {
            try (var files = Files.walk(folder)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
}
