package bms.player.beatoraja;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import bms.player.beatoraja.input.PollingLwjgl3Input;

/** Opt-in real window/GL/native test; never reads user config, charts or IR accounts. */
public final class Lwjgl3WindowSmokeTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja LWJGL3 smoke test");
        config.setWindowedMode(320, 240);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        var listener = new ApplicationAdapter() {
            SpriteBatch batch;
            Texture texture;
            int frames;
            @Override public void create() {
                var pixels = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
                try {
                    pixels.setColor(0, 1, 0, 1);
                    pixels.fill();
                    texture = new Texture(pixels);
                } finally {
                    pixels.dispose();
                }
                batch = new SpriteBatch();
                if (System.getProperty("os.name").startsWith("Mac")) {
                    try {
                        bms.player.beatoraja.external.DesktopActionsTest.macImageSmoke();
                    } catch (Exception e) {
                        throw new AssertionError("AWT-free screenshot clipboard from GLFW thread", e);
                    }
                }
                var input = (PollingLwjgl3Input) Gdx.input;
                check(input.getGdxKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_Z) == Input.Keys.Z, "Keyboard mapping");
                check(input.getGdxKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) == Input.Keys.NUMPAD_ENTER
                        && input.getGdxKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0) == Input.Keys.NUMPAD_0
                        && input.getGdxKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == Input.Keys.SHIFT_LEFT,
                        "Saved numpad/modifier key IDs");
                var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
                Thread worker = new Thread(() -> {
                    input.isKeyPressed(Input.Keys.Z);
                    input.isButtonPressed(Input.Buttons.LEFT);
                    input.consumeMouseScratchDelta();
                });
                worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
                worker.start();
                try { worker.join(3000); } catch (InterruptedException error) { throw new AssertionError(error); }
                check(!worker.isAlive() && failure.get() == null, "Input thread bridge");
            }
            @Override public void render() {
                Gdx.gl.glClearColor(0.2f, 0.4f, 0.6f, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.begin();
                batch.draw(texture, 0, 0, 64, 64);
                batch.end();
                if (frames == 10 || frames == 40) {
                    var pixel = BufferUtils.newByteBuffer(4);
                    Gdx.gl.glReadPixels(8, 8, 1, 1, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixel);
                    check((pixel.get(1) & 255) > 240 && (pixel.get(0) & 255) < 15, "Real texture pixels");
                    check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "No GL errors");
                }
                if (frames == 20) check(Gdx.graphics.setWindowedMode(400, 300), "Resize request");
                if (++frames == 60) {
                    check(Gdx.graphics.getWidth() == 400 && Gdx.graphics.getHeight() == 300, "Resized window dimensions");
                    Gdx.app.exit();
                }
            }
            @Override public void dispose() {
                texture.dispose();
                batch.dispose();
            }
        };
        new Lwjgl3Application(listener, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3WindowSmokeTest passed (real GLFW/OpenGL texture, resize, input bridge and shutdown)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
