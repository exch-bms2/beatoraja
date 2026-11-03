package bms.player.beatoraja.input;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import org.lwjgl.glfw.GLFW;
import static bms.player.beatoraja.input.WindowInputLifecycleTest.check;

/** Real GLFW callback chaining with synthetic edges; no desktop-wide input injection. */
public final class Lwjgl3WindowInputLifecycleTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja focus lifecycle test");
        config.setWindowedMode(320, 240); config.disableAudio(true); config.setForegroundFPS(60);
        new Lwjgl3Application(bms.player.beatoraja.build.GlTestDiagnostics.wrap("input-lifecycle", new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                if (++frames != 10) return;
                var source = (PollingLwjgl3Input) Gdx.input;
                long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
                try {
                    for (int mode = 0; mode < 2; mode++) test(source, handle, mode);
                } catch (Exception e) { throw new AssertionError(e); }
                Gdx.app.exit();
            }
        }), config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3WindowInputLifecycleTest passed (focus/iconify callbacks, keyboard/controller/mouse FIFO and replay parity)");
    }

    private static void test(PollingLwjgl3Input source, long handle, int mode) throws Exception {
        focus(handle, true); iconify(handle, false); source.update();
        try (var f = new WindowInputLifecycleTest.Fixture(mode, true)) {
            source.update(); f.input.poll(); f.consume();
            edge(handle, GLFW.GLFW_PRESS); source.update(); f.input.poll();
            check(f.input.getKeyState(0), "Normal keyboard assignment works");
            // Do not drain the accepted press: focus loss must retain it followed by its logged release.
            focus(handle, false);
            check(!f.input.getKeyState(0), "Loss immediately releases held keyboard");
            var events = f.consume();
            check(events.size == 2 && events.pressed[0] && !events.pressed[1], "Accepted judgment FIFO keeps press/release order");
            var log = f.input.getKeyInputLog();
            check(log.length == 2 && log[0].getTime() == events.times[0] && log[1].getTime() == events.times[1],
                    "Live judgment and replay retain identical boundary times");
            f.fake.buttons[0] = true; f.input.poll();
            check(!f.input.getKeyState(1), "Controller cannot press while unfocused");
            focus(handle, true); source.update(); f.input.poll(); f.input.poll();
            check(!f.input.getKeyState(0) && !f.input.getKeyState(1), "Held keys/buttons cannot become resume presses");
            edge(handle, GLFW.GLFW_RELEASE); f.fake.buttons[0] = false;
            source.update(); f.input.poll(); f.consume();
            edge(handle, GLFW.GLFW_PRESS); source.update(); f.input.poll();
            check(f.input.getKeyState(0), "Fresh keyboard press works after resume");
            edge(handle, GLFW.GLFW_RELEASE); source.update(); f.input.poll(); f.consume();

            // Unpublished tap and sampled mouse movement must not survive even a same-frame loss/regain.
            edge(handle, GLFW.GLFW_PRESS); edge(handle, GLFW.GLFW_RELEASE);
            int cx = Gdx.graphics.getWidth() / 2, cy = Gdx.graphics.getHeight() / 2;
            cursor(handle, cx + 20, cy); source.update();
            focus(handle, false); focus(handle, true);
            source.update(); f.input.poll();
            check(f.input.getJudgeEventQueue().available() == 0, "No old keyboard/mouse FIFO after loss/regain between polls");

            cursor(handle, cx + 20, cy); source.update(); f.input.poll();
            check(f.input.getKeyState(2) || f.input.getKeyState(3), "Fresh scratch after baseline works");
            f.consume();
            cursor(handle, cx - 20, cy); source.update(); // leave opposite motion unconsumed
            iconify(handle, true);
            check(!source.isWindowInputActive() && !f.input.getKeyState(2) && !f.input.getKeyState(3),
                    "Minimization without focus loss releases scratch");
            f.consume(); f.input.poll();
            iconify(handle, false); source.update(); f.input.poll(); f.input.poll();
            check(f.input.getJudgeEventQueue().available() == 0 && !f.input.getKeyState(2) && !f.input.getKeyState(3),
                    "Restore rebases mouse and clears old scratch algorithm state");
            // Exercise the same boundaries with the actual worker and diagnostics locks active.
            f.input.startPolling();
            Thread.sleep(20);
            for (int i = 0; i < 20; i++) {
                focus(handle, false); source.update();
                focus(handle, true); source.update();
            }
            f.input.dispose();
            source.setMouseScratchEnabled(false);
        }
    }
    private static void edge(long handle, int action) {
        var callback = GLFW.glfwSetKeyCallback(handle, null); GLFW.glfwSetKeyCallback(handle, callback);
        callback.invoke(handle, GLFW.GLFW_KEY_Z, 0, action, 0);
    }
    private static void focus(long handle, boolean focused) {
        var callback = GLFW.glfwSetWindowFocusCallback(handle, null); GLFW.glfwSetWindowFocusCallback(handle, callback);
        callback.invoke(handle, focused);
    }
    private static void iconify(long handle, boolean minimized) {
        var callback = GLFW.glfwSetWindowIconifyCallback(handle, null); GLFW.glfwSetWindowIconifyCallback(handle, callback);
        callback.invoke(handle, minimized);
    }
    private static void cursor(long handle, double x, double y) {
        var callback = GLFW.glfwSetCursorPosCallback(handle, null); GLFW.glfwSetCursorPosCallback(handle, callback);
        callback.invoke(handle, x, y);
    }
}
