package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicReference;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.lwjgl.glfw.GLFW;

/** Synthetic positions through the real render/input boundary; never physical latency measurements. */
public final class Lwjgl3MouseScratchQueueSmokeTest {
    public static void main(String[] args) {
        System.setProperty("beatoraja.inputDiagnostics", "true");
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja mouse scratch queue test");
        config.setWindowedMode(320, 240);
        config.setHdpiMode(HdpiMode.Logical);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                var window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
                long handle = window.getWindowHandle();
                if (++frames == 2) {
                    var focus = GLFW.glfwSetWindowFocusCallback(handle, null);
                    GLFW.glfwSetWindowFocusCallback(handle, focus);
                    focus.invoke(handle, true);
                }
                if (frames >= 10 && window.isFocused()) {
                    try {
                        for (int mode : new int[]{0, 1}) checkPipeline((PollingLwjgl3Input) Gdx.input, handle, mode);
                    } catch (Exception e) { throw new AssertionError(e); }
                    Gdx.app.exit();
                }
                if (frames > 120) throw new AssertionError("Synthetic focus notification not dispatched");
            }
        }, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3MouseScratchQueueSmokeTest passed (position FIFO, mapping, same-poll reversal, resets)");
    }

    private static void checkPipeline(PollingLwjgl3Input source, long handle, int mode) throws Exception {
        int cx = Gdx.graphics.getWidth() / 2, cy = Gdx.graphics.getHeight() / 2;
        var input = KeyboardQueueTestSupport.input(MouseMotionQueueTest.config(mode));
        var keyboard = input.getKeyBoardInputProcesseor();
        var scratch = keyboard.getMouseScratchInput();
        // Warm up the center without interpreting the pre-existing desktop cursor as gameplay motion.
        source.update();
        source.resetMouseScratchDeltas();
        source.update();
        input.beginInputJudge(null, null, false);
        cursor(handle, cx + 20, cy);
        source.update();
        cursor(handle, cx - 20, cy);
        source.update();
        poll(scratch, 1_000_000);
        var batch = new JudgeEventQueue.Batch();
        input.getJudgeEventQueue().drain(batch);
        check(batch.size == 3 && batch.keys[0] == 0 && batch.pressed[0]
                && batch.keys[1] == 0 && !batch.pressed[1] && batch.keys[2] == 1 && batch.pressed[2],
                "Opposite sampled motions survive net zero and reach mapped judgment keys");
        for (int i = 0; i < batch.size; i++) check(batch.times[i] == 1_000_000, "Original poll microtime");
        check(!input.getKeyState(0) && input.getKeyState(1), "Final UI state retains last direction");
        poll(scratch, 1_000_000);
        check(input.getJudgeEventQueue().available() == 0, "No repeated motion on a faster input poll");
        check(input.getMouseScratchReplayKeys().length == 4 && input.getKeyboardReplayKeys().length == 0,
                "Mouse replay metadata is separate from keyboard metadata");
        var observed = source.getDiagnostics().mouseScratch().snapshot();
        check(observed.counts().get(MouseScratchDiagnostics.Counter.FIFO_RECORDS) >= 2
                && observed.x().deliveredPath() >= 40, "Diagnostics record delivered path, not just zero net");

        cursor(handle, cx + 20, cy); source.update();
        input.setStartTime(100);
        input.beginInputJudge(null, null, false);
        poll(scratch, 0);
        check(input.getJudgeEventQueue().available() == 0, "Clock reset discards queued old-scene movement");
        cursor(handle, cx + 20, cy); source.update();
        source.windowHandleChanged(handle);
        poll(scratch, 1000);
        check(input.getJudgeEventQueue().available() == 0, "Window recreation/rebinding discards old movement");
        cursor(handle, cx, cy);
        cursor(handle, cx + 20, cy);
        cursor(handle, cx, cy);
        source.update();
        poll(scratch, 2000);
        check(input.getJudgeEventQueue().available() == 0, "Callback-only round trip is deliberately not captured");
        source.setMouseScratchEnabled(false);
    }

    private static void cursor(long handle, double x, double y) {
        var callback = GLFW.glfwSetCursorPosCallback(handle, null);
        GLFW.glfwSetCursorPosCallback(handle, callback);
        callback.invoke(handle, x, y);
    }

    private static void poll(MouseScratchInput scratch, long time) throws Exception {
        var error = new AtomicReference<Throwable>();
        Thread thread = new Thread(() -> { try { scratch.poll(time); } catch (Throwable e) { error.set(e); } });
        thread.start(); thread.join(3000);
        check(!thread.isAlive() && error.get() == null, "Worker failed: " + error.get());
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
