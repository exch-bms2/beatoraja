package bms.player.beatoraja.skin;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;

/** GL texture lifecycle fixture that needs neither native image decoding nor a window. */
public final class MockTextureEnvironment implements AutoCloseable {
    private final GL20 previousGL = Gdx.gl;
    private final GL20 previousGL20 = Gdx.gl20;
    private final Graphics previousGraphics = Gdx.graphics;
    private final Map<Integer, Integer> deletions = new HashMap<>();
    private int nextHandle = 1;

    public MockTextureEnvironment() {
        GL20 gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(),
                new Class<?>[] { GL20.class }, (proxy, method, args) -> {
                    if (method.getName().equals("glGenTexture")) return nextHandle++;
                    if (method.getName().equals("glDeleteTexture")) {
                        deletions.merge((Integer) args[0], 1, Integer::sum);
                    }
                    return method.getReturnType() == int.class ? 0 : null;
                });
        Gdx.gl = gl;
        Gdx.gl20 = gl;
        Gdx.graphics = graphicsWithoutExtensions();
    }

    static Graphics graphicsWithoutExtensions() {
        return (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[] {Graphics.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("supportsExtension")) return false;
                    throw new AssertionError("Unexpected graphics method: " + method.getName());
                });
    }

    public Texture createTexture() {
        return new Texture(new GLOnlyTextureData(16, 16, 0,
                GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE));
    }

    public void assertDeletedOnce(int handle) {
        if (deletions.getOrDefault(handle, 0) != 1) {
            throw new AssertionError("Texture must be deleted once: " + handle);
        }
    }

    @Override
    public void close() {
        Gdx.gl = previousGL;
        Gdx.gl20 = previousGL20;
        Gdx.graphics = previousGraphics;
    }
}
