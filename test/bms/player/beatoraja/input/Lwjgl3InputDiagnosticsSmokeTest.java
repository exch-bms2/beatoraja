package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import org.lwjgl.glfw.GLFW;

/** Real GLFW callback ownership/forwarding, with synthetic edges rather than physical input timing. */
public final class Lwjgl3InputDiagnosticsSmokeTest {
    public static void main(String[] args) {
        System.setProperty("beatoraja.inputDiagnostics", "true");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja input diagnostics test");
        config.setWindowedMode(320, 240);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        var app = new ApplicationAdapter() {
            int frames, downs, ups;
            @Override public void create() {
                Gdx.input.setInputProcessor(new InputAdapter() {
                    @Override public boolean keyDown(int key) { if (key == Keys.F23) downs++; return true; }
                    @Override public boolean keyUp(int key) { if (key == Keys.F23) ups++; return true; }
                });
            }
            @Override public void render() {
                frames++;
                var input = (PollingLwjgl3Input) Gdx.input;
                var window = ((com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics) Gdx.graphics).getWindow();
                if (frames == 2) {
                    // macOS may not activate test JVMs automatically. Inject only the focus
                    // notification, not an OS key/mouse event, to make this callback test deterministic.
                    long handle = window.getWindowHandle();
                    var focus = GLFW.glfwSetWindowFocusCallback(handle, null);
                    GLFW.glfwSetWindowFocusCallback(handle, focus);
                    focus.invoke(handle, true);
                }
                if (frames >= 10 && window.isFocused()) {
                    runChecks(input, window.getWindowHandle());
                    Gdx.app.exit();
                }
                if (frames > 120) throw new AssertionError("Synthetic focus notification was not dispatched");
            }
            private void runChecks(PollingLwjgl3Input input, long handle) {
                var diagnostics = input.getDiagnostics();
                check(diagnostics != null, "Opt-in collector exists");
                readOnWorker(input, false);
                long collapsed = diagnostics.snapshot().keyCollapsed();
                key(handle, GLFW.GLFW_PRESS);
                key(handle, GLFW.GLFW_RELEASE);
                input.update();
                readOnWorker(input, false);
                check(diagnostics.snapshot().keyCollapsed() > collapsed, "Short callback pulse disappears from state snapshot");
                check(downs == 1 && ups == 1, "Original InputProcessor still receives both edges");

                key(handle, GLFW.GLFW_PRESS);
                key(handle, GLFW.GLFW_REPEAT);
                input.update();
                readOnWorker(input, true);
                key(handle, GLFW.GLFW_RELEASE);
                input.update();
                readOnWorker(input, false);
                check(diagnostics.snapshot().repeats() > 0, "Observe repeat without generating extra down edges");
                check(diagnostics.snapshot().timings().get("callbackToRead").count() >= 2,
                        "Measure callback/publication/worker-read pipeline");

                input.windowHandleChanged(handle);
                key(handle, GLFW.GLFW_PRESS);
                input.update();
                readOnWorker(input, true);
                key(handle, GLFW.GLFW_RELEASE);
                input.update();
                readOnWorker(input, false);
                check(downs == 3 && ups == 3, "Reinstalled callbacks forward once, without dangling native pointers");
                var button = GLFW.glfwSetMouseButtonCallback(handle, null);
                GLFW.glfwSetMouseButtonCallback(handle, button);
                long edges = diagnostics.snapshot().buttonEdges();
                button.invoke(handle, 0, GLFW.GLFW_PRESS, 0);
                button.invoke(handle, 0, GLFW.GLFW_RELEASE, 0);
                input.update();
                check(diagnostics.snapshot().buttonEdges() == edges + 2, "Mouse callbacks are observed and forwarded");
                var cursor = GLFW.glfwSetCursorPosCallback(handle, null);
                GLFW.glfwSetCursorPosCallback(handle, cursor);
                cursor.invoke(handle, 20, 30);
                input.update();
                check(input.getX() == 20 && input.getY() == 30, "Original cursor callback receives coordinates");
                check(diagnostics.snapshot().timings().get("cursorToSnapshot").count() > 0,
                        "Mouse motion publication timing");
                System.out.println("Input callback smoke checks passed (synthetic events; hardware latency not measured)");
            }
        };
        new Lwjgl3Application(app, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3InputDiagnosticsSmokeTest passed (callback forwarding, reinstallation and disposal)");
    }

    private static void key(long handle, int action) {
        var callback = GLFW.glfwSetKeyCallback(handle, null);
        GLFW.glfwSetKeyCallback(handle, callback);
        check(callback != null, "Key callback installed");
        callback.invoke(handle, GLFW.GLFW_KEY_F23, 0, action, 0);
    }

    private static void readOnWorker(PollingLwjgl3Input input, boolean expected) {
        var failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try {
                input.getDiagnostics().poll(System.nanoTime());
                check(input.isKeyPressed(Keys.F23) == expected, "Worker snapshot state");
            } catch (Throwable e) { failure.set(e); }
        });
        worker.start();
        try { worker.join(3000); } catch (InterruptedException e) { throw new AssertionError(e); }
        check(!worker.isAlive() && failure.get() == null, "Worker read completed: " + failure.get());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
