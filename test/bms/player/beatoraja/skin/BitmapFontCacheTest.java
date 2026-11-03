package bms.player.beatoraja.skin;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.utils.Array;

/** Regression tests for shared bitmap-font texture ownership without a real GL context. */
public final class BitmapFontCacheTest {
    private static final Map<Integer, Integer> deletions = new HashMap<>();
    private static int nextTexture = 1;

    public static void main(String[] args) {
        GL20 previousGL = Gdx.gl;
        GL20 previousGL20 = Gdx.gl20;
        var previousGraphics = Gdx.graphics;
        GL20 gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(),
                new Class<?>[] { GL20.class }, (proxy, method, arguments) -> {
                    if (method.getName().equals("glGenTexture")) {
                        return nextTexture++;
                    }
                    if (method.getName().equals("glDeleteTexture")) {
                        deletions.merge((Integer) arguments[0], 1, Integer::sum);
                    }
                    return method.getReturnType() == int.class ? 0 : null;
                });
        Gdx.gl = gl;
        Gdx.gl20 = gl;
        Gdx.graphics = MockTextureEnvironment.graphicsWithoutExtensions();
        try {
            for (int cycle = 0; cycle < 16; cycle++) {
                assertSharedFontReleased(0);
                assertSharedFontReleased(1);
            }
            System.out.println("BitmapFontCacheTest passed");
        } finally {
            Gdx.gl = previousGL;
            Gdx.gl20 = previousGL20;
            Gdx.graphics = previousGraphics;
        }
    }

    private static void assertSharedFontReleased(int type) {
        Path path = Path.of("bitmap-font-cache-test.fnt");
        var cached = BitmapFontCache.acquire(path, type, BitmapFontCacheTest::createFont);
        var shared = BitmapFontCache.acquire(path, type, () -> {
            throw new AssertionError("Shared font must not be reloaded");
        });
        check(cached == shared, "Cache must share the font");
        check(cached.font.ownsTexture(), "Cache must own the font textures");

        BitmapFont wrapper = new BitmapFont(cached.fontData, cached.regions, false);
        check(!wrapper.ownsTexture(), "Text wrappers must not own shared textures");
        wrapper.dispose();
        assertDeletions(cached, 0);
        BitmapFontCache.release(path, type);
        check(BitmapFontCache.Has(path, type), "Another source still references the font");
        assertDeletions(cached, 0);
        BitmapFontCache.release(path, type);
        check(!BitmapFontCache.Has(path, type), "Last release must remove the font");
        assertDeletions(cached, 1);
        BitmapFontCache.release(path, type);
        assertDeletions(cached, 1);
        check(BitmapFontCache.getStatistics().entryCount() == 0, "Cache must return to empty");
    }

    private static BitmapFontCache.CacheableBitmapFont createFont() {
        var result = new BitmapFontCache.CacheableBitmapFont();
        result.fontData = new BitmapFont.BitmapFontData();
        result.regions = new Array<>();
        for (int page = 0; page < 2; page++) {
            Texture texture = new Texture(new GLOnlyTextureData(16, 16, 0,
                    GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE));
            result.regions.add(new TextureRegion(texture));
        }
        result.font = new BitmapFont(result.fontData, result.regions, true);
        check(!result.font.ownsTexture(), "Supplied regions are non-owned by default");
        return result;
    }

    private static void assertDeletions(BitmapFontCache.CacheableBitmapFont font, int expected) {
        for (TextureRegion region : font.regions) {
            // Texture.dispose resets the handle, so use the original creation order below.
            int handle = region.getTexture().getTextureObjectHandle();
            if (expected == 0) {
                check(deletions.getOrDefault(handle, 0) == 0, "Shared texture deleted too early");
            } else {
                check(handle == 0, "Released texture must have its handle cleared");
            }
        }
        if (expected == 1) {
            check(deletions.size() == nextTexture - 1, "All created textures must be deleted");
            check(deletions.values().stream().allMatch(count -> count == 1),
                    "Each texture must be deleted exactly once");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
