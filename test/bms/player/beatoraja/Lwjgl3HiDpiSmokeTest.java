package bms.player.beatoraja;

import bms.player.beatoraja.input.mouse.MouseCoordinates;

import bms.player.beatoraja.input.PollingLwjgl3Input;
import bms.player.beatoraja.input.WindowDiagnostics;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.BufferUtils;
import org.lwjgl.glfw.GLFW;

/** Opt-in real GL verification. Fullscreen is separately opt-in because it changes the desktop. */
public final class Lwjgl3HiDpiSmokeTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja HiDPI verification");
        // Deliberately differ from the 320x240 skin coordinates at startup.
        config.setWindowedMode(480, 360);
        config.setHdpiMode(HdpiMode.Logical);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        if (Boolean.getBoolean("beatoraja.hidpiTest.startFullscreen"))
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        new Lwjgl3Application(new ApplicationAdapter() {
            SpriteBatch batch;
            Texture texture;
            SkinObjectRenderer renderer;
            WindowModeController windowMode;
            int frames, verified;
            int restoredWidth, restoredHeight;
            @Override public void create() {
                bms.player.beatoraja.play.SkinAdjustmentOverlayTest.verifyScene2d();
                batch = new SpriteBatch();
                check(Gdx.graphics.getWidth() != 320 || Gdx.graphics.getHeight() != 240,
                        "Exercise actual mismatched startup dimensions");
                check(!WindowDiagnostics.matchesSkinProjection(batch.getProjectionMatrix().val[0],
                        batch.getProjectionMatrix().val[5], 320, 240), "Default projection reproduces the mismatch");
                var camera = new OrthographicCamera();
                camera.setToOrtho(false, 320, 240);
                batch.setProjectionMatrix(MainController.skinProjection(320, 240));
                check(WindowDiagnostics.matchesSkinProjection(batch.getProjectionMatrix().val[0],
                        batch.getProjectionMatrix().val[5], 320, 240), "Configured initial projection");
                WindowDiagnostics.describeSkinProjection(batch, 320, 240);
                windowMode = WindowModeController.create((Lwjgl3Graphics) Gdx.graphics, Resolution.SD);
                if (Boolean.getBoolean("beatoraja.hidpiTest.startFullscreen")) {
                    check(Gdx.graphics.isFullscreen() && windowMode.toggle() && !Gdx.graphics.isFullscreen(),
                            "Fullscreen startup without saved window bounds");
                    restoredWidth = 640; restoredHeight = 480;
                }
                renderer = new SkinObjectRenderer(batch, camera);
                var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                try { pixmap.setColor(Color.WHITE); pixmap.fill(); texture = new Texture(pixmap); }
                finally { pixmap.dispose(); }
            }
            @Override public void render() {
                if (windowMode != null && windowMode.isSwitching()) return;
                if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) return;
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.begin();
                check(renderer.pushClip(80, 60, 160, 120), "Production clip push");
                batch.draw(texture, 0, 0, 320, 240);
                // Nested clipping exercises stack restoration as well as pixel conversion.
                check(renderer.pushClip(100, 80, 40, 40), "Nested clip push");
                renderer.popClip();
                var outer = com.badlogic.gdx.scenes.scene2d.utils.ScissorStack.peekScissors();
                check(Math.abs(outer.x - Gdx.graphics.getWidth() / 4f) <= 1
                        && Math.abs(outer.width - Gdx.graphics.getWidth() / 2f) <= 1,
                        "Outer clip restored after nested pop");
                batch.setColor(Color.GREEN);
                batch.draw(texture, 170, 90, 20, 20);
                batch.setColor(Color.WHITE);
                renderer.popClip();
                batch.end();
                if (frames % 20 == 10) verify();
                if (frames == 20) {
                    check(Gdx.graphics.setWindowedMode(480, 270), "Resize nonuniformly");
                    restoredWidth = restoredHeight = 0;
                }
                if (frames == 40) check(Gdx.graphics.setWindowedMode(400, 300), "Resize again");
                if (frames == 60 && Boolean.getBoolean("beatoraja.hidpiTest.fullscreen"))
                    check(windowMode.toggle() && Gdx.graphics.isFullscreen(), "Production fullscreen entry");
                if (frames == 80) {
                    if (Boolean.getBoolean("beatoraja.hidpiTest.fullscreen")) {
                        check(windowMode.toggle() && !Gdx.graphics.isFullscreen(), "Production window restoration");
                        restoredWidth = 400; restoredHeight = 300;
                    } else {
                        check(Gdx.graphics.setWindowedMode(320, 240), "Window resize");
                        restoredWidth = 320; restoredHeight = 240;
                    }
                }
                if (frames == 100 && Boolean.getBoolean("beatoraja.hidpiTest.fullscreen")) {
                    check(windowMode.toggle() && Gdx.graphics.isFullscreen(), "Repeated fullscreen entry");
                    restoredWidth = restoredHeight = 0;
                }
                if (frames == 120 && Boolean.getBoolean("beatoraja.hidpiTest.fullscreen")) {
                    check(windowMode.toggle() && !Gdx.graphics.isFullscreen(), "Repeated window restoration");
                    restoredWidth = 400; restoredHeight = 300;
                }
                if (++frames == 140) {
                    check(verified == 7, "All resize/restore stages verified");
                    Gdx.app.exit();
                }
            }
            private void verify() {
                int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
                check(pixel(bw / 2, bh / 2) > 240, "Clip center visible");
                check(pixel(bw / 8, bh / 2) < 15, "Clip outside black");
                check(pixel(bw / 2, bh / 8) < 15, "Clip vertical outside black");
                check(pixel(bw * 81 / 320, bh / 2) > 240, "Draw and clip agree just inside left edge");
                check(pixel(bw * 79 / 320, bh / 2) < 15, "Draw and clip agree just outside left edge");
                check(pixel(bw * 180 / 320, bh * 100 / 240) < 15,
                        "Drawing outside the former inner clip reaches restored outer clip");
                check(!Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST), "Scissor disabled after pop");
                check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "No GL errors");
                var graphics = (Lwjgl3Graphics) Gdx.graphics;
                if (restoredWidth > 0) check(graphics.getLogicalWidth() == restoredWidth
                        && graphics.getLogicalHeight() == restoredHeight,
                        "Restore previous logical size, not fullscreen or framebuffer dimensions");
                check(graphics.getWidth() == graphics.getLogicalWidth(), "Logical input mode");
                int cx = graphics.getWidth() / 2, cy = graphics.getHeight() / 2;
                long handle = graphics.getWindow().getWindowHandle();
                var callback = GLFW.glfwSetCursorPosCallback(handle, null);
                GLFW.glfwSetCursorPosCallback(handle, callback);
                callback.invoke(handle, cx, cy);
                check(Gdx.input.getX() == cx && Gdx.input.getY() == cy, "GLFW logical cursor coordinates");
                check(Math.abs(MouseCoordinates.skinX(cx, 320, graphics.getWidth()) - 160) <= 1,
                        "Click X agrees with rendered center");
                check(Math.abs(MouseCoordinates.skinY(cy, 240, graphics.getHeight()) - 120) <= 1,
                        "Click Y agrees with rendered center");
                int inside = (int) Math.ceil(graphics.getWidth() * 81.0 / 320);
                int outside = (int) Math.floor(graphics.getWidth() * 79.0 / 320);
                var clickBounds = new com.badlogic.gdx.math.Rectangle(80, 60, 160, 120);
                check(clickBounds.contains(MouseCoordinates.skinX(inside, 320, graphics.getWidth()),
                                MouseCoordinates.skinY(cy, 240, graphics.getHeight()))
                        && !clickBounds.contains(MouseCoordinates.skinX(outside, 320, graphics.getWidth()),
                                MouseCoordinates.skinY(cy, 240, graphics.getHeight())),
                        "Hit edges agree with visible clip edges after mismatched startup and resize");
                callback.invoke(handle, cx + 10, cy);
                check(Gdx.input.getX() - cx == 10, "Scratch input remains ten logical pixels, no DPI multiplier");
                verified++;
            }
            private int pixel(int x, int y) {
                var pixel = BufferUtils.newByteBuffer(4);
                Gdx.gl.glReadPixels(x, y, 1, 1, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixel);
                return pixel.get(0) & 255;
            }
            @Override public void dispose() {
                batch.dispose(); texture.dispose(); ShaderManager.dispose();
            }
        }, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3HiDpiSmokeTest passed (real resize/clip pixels, synthetic cursor; mixed-DPI monitor movement not automated)");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
