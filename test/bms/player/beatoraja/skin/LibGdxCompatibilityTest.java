package bms.player.beatoraja.skin;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.GdxNativesLoader;

/** Native glyph generation and cached-layout compatibility without a real GL window. */
public final class LibGdxCompatibilityTest {
    public static void main(String[] args) throws Exception {
        check(com.badlogic.gdx.Version.VERSION.equals("1.14.2"), "libGDX version");
        GdxNativesLoader.load();
        var oldFiles = Gdx.files;
        var oldGL = Gdx.gl;
        var oldGL20 = Gdx.gl20;
        var oldGraphics = Gdx.graphics;
        GL20 gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[] {GL20.class},
                (proxy, method, values) -> {
                    if (method.getReturnType() == int.class) return method.getName().equals("glGenTexture") ? 1 : 0;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
        Gdx.files = new Lwjgl3Files();
        Gdx.gl = gl;
        Gdx.gl20 = gl;
        Gdx.graphics = MockTextureEnvironment.graphicsWithoutExtensions();
        var text = new SkinTextFont("font/VL-Gothic-Regular.ttf", 0, 24, 1);
        BitmapFont font = null;
        try {
            text.prepareFont("AB\u3042");
            var field = SkinTextFont.class.getDeclaredField("font");
            field.setAccessible(true);
            font = (BitmapFont) field.get(text);
            check(font != null && font.getData().getGlyph('\u3042') != null, "FreeType native/Japanese glyph generation");

            var remap = SkinTextFont.class.getDeclaredMethod("remapMissingGlyphs", String.class);
            remap.setAccessible(true);
            String mapped = (String) remap.invoke(text, "A\uD83D\uDE00");
            check(mapped.length() == 2 && font.getData().getGlyph(mapped.charAt(1)) != null,
                    "Supplementary fallback glyph packing");

            for (Class<?> type : new Class<?>[] {SkinTextFont.class, SkinTextBitmap.class}) {
                var colorMethod = type.getDeclaredMethod("applyColor", GlyphLayout.class, Color.class);
                colorMethod.setAccessible(true);
                var layout = new GlyphLayout(font, "AB\nA");
                var runs = layout.runs.toArray(GlyphLayout.GlyphRun.class);
                int[] indices = new int[layout.colors.size / 2];
                for (int i = 0; i < indices.length; i++) indices[i] = layout.colors.get(i * 2);
                for (Color color : new Color[] {new Color(0.2f, 0.4f, 0.8f, 0.5f), Color.WHITE}) {
                    colorMethod.invoke(null, layout, color);
                    check(Arrays.equals(runs, layout.runs.toArray(GlyphLayout.GlyphRun.class)), "Recolor must not rebuild glyph runs");
                    for (int i = 0; i < indices.length; i++) {
                        check(layout.colors.get(i * 2) == indices[i], "Color glyph index must remain stable");
                        check(layout.colors.get(i * 2 + 1) == color.toIntBits(), "Layout ABGR color");
                    }
                    font.getCache().setText(layout, 0, 0);
                    check(font.getCache().getVertexCount(0) > 0, "Cached glyph vertices");
                    check(font.getCache().getVertices(0)[2] == NumberUtils.intToFloatColor(color.toIntBits()),
                            "Cached vertex color/alpha");
                }
                colorMethod.invoke(null, new GlyphLayout(), Color.WHITE);
            }
        } finally {
            if (font != null && font.getData() instanceof FreeTypeBitmapFontData data) data.dispose();
            text.dispose();
            Gdx.files = oldFiles;
            Gdx.gl = oldGL;
            Gdx.gl20 = oldGL20;
            Gdx.graphics = oldGraphics;
        }
        System.out.println("LibGdxCompatibilityTest passed (native FreeType, glyph packing, cached text colors; mock GL)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
