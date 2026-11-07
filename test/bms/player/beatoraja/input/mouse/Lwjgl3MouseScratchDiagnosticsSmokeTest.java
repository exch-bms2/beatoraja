package bms.player.beatoraja.input.mouse;

import bms.player.beatoraja.input.PollingLwjgl3Input;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.lwjgl.glfw.GLFW;

/** Real callback wrapping and accumulation with synthetic cursor motion, never hardware timing. */
public final class Lwjgl3MouseScratchDiagnosticsSmokeTest {
    public static void main(String[] args) {
        System.setProperty("beatoraja.inputDiagnostics", "true");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja mouse scratch diagnostics test");
        config.setWindowedMode(320, 240);
        config.setHdpiMode(HdpiMode.Logical);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        var app = new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                var window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
                long handle = window.getWindowHandle();
                if (++frames == 2) {
                    var callback = GLFW.glfwSetWindowFocusCallback(handle, null);
                    GLFW.glfwSetWindowFocusCallback(handle, callback);
                    callback.invoke(handle, true);
                }
                if (frames >= 10 && window.isFocused()) {
                    runChecks((PollingLwjgl3Input) Gdx.input, handle);
                    Gdx.app.exit();
                }
                if (frames > 120) throw new AssertionError("Synthetic focus notification not dispatched");
            }
        };
        new Lwjgl3Application(app, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3MouseScratchDiagnosticsSmokeTest passed (synthetic callbacks, real GLFW accumulation)");
    }

    private static void runChecks(PollingLwjgl3Input input, long handle) {
        var diagnostics = input.getDiagnostics().mouseScratch();
        int cx = Gdx.graphics.getWidth() / 2, cy = Gdx.graphics.getHeight() / 2;
        input.setMouseScratchEnabled(true);
        input.update();
        readOnWorker(input);
        input.update();
        cursor(handle, cx, cy); // Explicit warp candidate before the measured synthetic round trip.
        double cancelled = diagnostics.snapshot().x().callbackCancellation();
        cursor(handle, cx + 10, cy);
        cursor(handle, cx, cy);
        input.update();
        check(readOnWorker(input) == 0, "Final-position round trip remains invisible to gameplay");
        check(diagnostics.snapshot().x().callbackCancellation() == cancelled + 20, "Observe intra-sample cancellation");

        cursor(handle, cx, cy);
        cursor(handle, cx + 10, cy + 7);
        input.update();
        cursor(handle, cx, cy);
        cursor(handle, cx - 6, cy - 3);
        input.update();
        long delta = readOnWorker(input);
        check((int) (delta >> 32) == 4 && (int) delta == 4, "Retain unchanged two-axis movement sums");
        check(readOnWorker(input) == 0, "No repeated movement at worker polling frequency");
        check(diagnostics.snapshot().x().betweenSampleCancellation() >= 12
                && diagnostics.snapshot().y().betweenSampleCancellation() >= 6, "Observe partial inter-sample cancellation");

        var geometry = diagnostics.snapshot().geometry();
        check(geometry.inputWidth() == Gdx.graphics.getWidth()
                && geometry.bufferWidth() == Gdx.graphics.getBackBufferWidth()
                && geometry.bufferPerInputX() > 0, "Record real logical/framebuffer dimensions without rescaling input");
        input.windowHandleChanged(handle);
        cursor(handle, cx + 5, cy);
        check(input.getX() == cx + 5, "Reinstalled callback still forwards original coordinates");
        input.update();
        check(readOnWorker(input) == 0, "Window rebinding re-primes the center instead of interpreting stale coordinates");
        cursor(handle, cx + 5, cy);
        input.update();
        check((int) (readOnWorker(input) >> 32) == 5, "Stable callback after rebinding retains ordinary movement");

        cursor(handle, cx, cy);
        cursor(handle, cx + 8, cy);
        input.update();
        long discarded = diagnostics.snapshot().counts().get(MouseScratchDiagnostics.Counter.DISCARDED_BATCHES);
        input.setMouseScratchEnabled(false);
        check(readOnWorker(input) == 0, "Disable retains existing accumulator-clear behavior");
        check(diagnostics.snapshot().counts().get(MouseScratchDiagnostics.Counter.DISCARDED_BATCHES) == discarded + 1,
                "Measure reset discard separately from cancellation");
        check(diagnostics.snapshot().counts().get(MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES) == 0,
                "Observer matches the real accumulator");
    }

    private static void cursor(long handle, double x, double y) {
        var callback = GLFW.glfwSetCursorPosCallback(handle, null);
        GLFW.glfwSetCursorPosCallback(handle, callback);
        check(callback != null, "Cursor observer installed");
        callback.invoke(handle, x, y);
    }

    private static long readOnWorker(PollingLwjgl3Input input) {
        var value = new AtomicLong();
        var failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try { value.set(input.consumeMouseScratchDelta()); } catch (Throwable e) { failure.set(e); }
        });
        worker.start();
        try { worker.join(3000); } catch (InterruptedException e) { throw new AssertionError(e); }
        check(!worker.isAlive() && failure.get() == null, "Worker consume failed: " + failure.get());
        return value.get();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
