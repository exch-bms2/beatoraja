package bms.player.beatoraja;

import bms.player.beatoraja.input.mouse.MouseMotionQueue;
import bms.player.beatoraja.input.PollingLwjgl3Input;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.lwjgl.glfw.GLFW;

/** Real GLFW geometry, synthetic cursor samples; no charts, audio, IR or physical latency measurements. */
public final class Lwjgl3MouseScratchGeometrySmokeTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja mouse scratch geometry test");
        config.setWindowedMode(320, 240);
        config.setHdpiMode(HdpiMode.Logical);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            boolean checking;
            @Override public void render() {
                if (checking) return;
                var graphics = (Lwjgl3Graphics) Gdx.graphics;
                long handle = graphics.getWindow().getWindowHandle();
                if (++frames == 2) {
                    var focus = GLFW.glfwSetWindowFocusCallback(handle, null);
                    GLFW.glfwSetWindowFocusCallback(handle, focus);
                    focus.invoke(handle, true);
                }
                if (frames >= 10 && graphics.getWindow().isFocused()) {
                    checking = true;
                    checkGeometry((PollingLwjgl3Input) Gdx.input, graphics);
                    Gdx.app.exit();
                }
                if (frames > 120) throw new AssertionError("Window focus not acquired");
            }
        }, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3MouseScratchGeometrySmokeTest passed (resize, position, invalidation, preserved FIFO and ordinary movement)");
    }

    private static void checkGeometry(PollingLwjgl3Input input, Lwjgl3Graphics graphics) {
        long handle = graphics.getWindow().getWindowHandle();
        input.setMouseScratchEnabled(true);
        cursor(handle, 7, 9);
        input.update();
        expect(input, 0, 0, "Initial center is not movement");
        ordinary(input, handle);

        cursor(handle, graphics.getWidth() / 2 + 9, graphics.getHeight() / 2);
        input.update(); // Leave a genuine movement in the FIFO.
        int oldX = graphics.getWidth() / 2, oldY = graphics.getHeight() / 2;
        check(graphics.setWindowedMode(480, 360), "Resize request");
        cursor(handle, oldX, oldY);
        input.update();
        expect(input, 1, 9, "Resize rejects center shift but retains valid pre-resize FIFO");
        ordinary(input, handle);

        var window = graphics.getWindow();
        int wx = window.getPositionX(), wy = window.getPositionY();
        window.setPosition(wx + 17, wy + 11);
        if (window.getPositionX() != wx || window.getPositionY() != wy) {
            cursor(handle, graphics.getWidth() / 2 - 17, graphics.getHeight() / 2 - 11);
            input.update();
            expect(input, 0, 0, "Window move is not a scratch");
            ordinary(input, handle);
        } else System.out.println("Window manager ignored explicit positioning; move case skipped");

        if (Boolean.getBoolean("beatoraja.hidpiTest.fullscreen")) {
            var mode = WindowModeController.create(graphics, Resolution.SD);
            for (int i = 0; i < 2; i++) {
                oldX = graphics.getWidth() / 2; oldY = graphics.getHeight() / 2;
                check(mode.toggle(), "Mode switch");
                cursor(handle, oldX, oldY);
                input.update();
                expect(input, 0, 0, "Mode transition must not emit a center displacement");
                ordinary(input, handle);
            }
        }

        // Same-geometry round trips still need a boundary, even if no intermediate size was sampled.
        input.invalidateMouseScratchCenter();
        cursor(handle, 0, 0);
        input.update();
        expect(input, 0, 0, "Explicit transition boundary");
        ordinary(input, handle);
        input.setMouseScratchEnabled(false);
    }

    private static void ordinary(PollingLwjgl3Input input, long handle) {
        cursor(handle, Gdx.graphics.getWidth() / 2 + 12, Gdx.graphics.getHeight() / 2);
        input.update();
        expect(input, 1, 12, "First stable sample retains ordinary sensitivity");
    }
    private static void expect(PollingLwjgl3Input input, int size, int dx, String message) {
        var batch = new MouseMotionQueue.Batch();
        input.consumeMouseScratchDeltas(batch);
        check(batch.size == size && (size == 0 || batch.dx[0] == dx && batch.dy[0] == 0), message);
    }
    private static void cursor(long handle, double x, double y) {
        var callback = GLFW.glfwSetCursorPosCallback(handle, null);
        GLFW.glfwSetCursorPosCallback(handle, callback);
        callback.invoke(handle, x, y);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
