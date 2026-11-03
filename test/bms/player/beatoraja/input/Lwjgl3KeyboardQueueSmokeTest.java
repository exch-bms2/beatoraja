package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.*;
import org.lwjgl.glfw.GLFW;

/** Native callback-to-keyboard-to-judgment-queue integration; no physical OS input injection. */
public final class Lwjgl3KeyboardQueueSmokeTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja keyboard queue test");
        config.setWindowedMode(320, 240);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                frames++;
                var input = (PollingLwjgl3Input) Gdx.input;
                var window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
                if (frames == 2) {
                    var focus = GLFW.glfwSetWindowFocusCallback(window.getWindowHandle(), null);
                    GLFW.glfwSetWindowFocusCallback(window.getWindowHandle(), focus);
                    focus.invoke(window.getWindowHandle(), true);
                }
                if (frames == 10) {
                    try { test(input, window.getWindowHandle()); }
                    catch (Exception e) { throw new AssertionError(e); }
                    Gdx.app.exit();
                }
            }
        }, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) { return new PollingLwjgl3Input(window); }
        };
        System.out.println("Lwjgl3KeyboardQueueSmokeTest passed (synthetic callbacks, duration, assignments, text input, replay log)");
    }

    private static void test(PollingLwjgl3Input source, long handle) throws Exception {
        KeyboardConfig config = config(Keys.Z, 0);
        var input = KeyboardQueueTestSupport.input(config);
        var keyboard = input.getKeyBoardInputProcesseor();
        Gdx.input.setInputProcessor(keyboard);
        source.update();
        poll(keyboard, 500);
        input.beginKeyboardJudge(null, false);
        edge(handle, GLFW.GLFW_KEY_Z, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_Z, GLFW.GLFW_REPEAT);
        edge(handle, GLFW.GLFW_KEY_Z, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 123456);
        var batch = new KeyboardJudgeQueue.Batch();
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 2 && batch.pressed[0] && !batch.pressed[1], "Retain short tap without auto-repeat");
        check(batch.times[0] == 123456 && batch.times[1] == 123456, "Use poll time without synthetic offsets");
        check(!input.getKeyState(0), "Live held state ends released");
        var log = input.getKeyInputLog();
        check(log.length == 2 && log[0].getTime() == 123456 && log[1].getTime() == 123456,
                "Replay log records both edges in the same order/time");

        input.resetAllKeyState();
        keyboard.setConfig(config(Keys.UP, 10));
        keyboard.clear();
        input.beginKeyboardJudge(null, false);
        edge(handle, GLFW.GLFW_KEY_UP, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_UP, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 200000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 1 && batch.pressed[0] && input.getKeyState(0), "Duration keeps release pending");
        keyboard.isKeyPressed(Keys.UP);
        poll(keyboard, 209999);
        check(input.getKeyboardJudgeQueue().available() == 0, "Control-key consumption cannot bypass game duration");
        poll(keyboard, 210000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 1 && !batch.pressed[0] && batch.times[0] == 210000, "Deferred release uses its eligible poll time");

        input.resetAllKeyState();
        keyboard.setConfig(config(Keys.X, 0));
        keyboard.clear();
        input.beginKeyboardJudge(null, false);
        keyboard.setTextInputMode(true);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 300000);
        keyboard.setTextInputMode(false);
        poll(keyboard, 301000);
        check(input.getKeyboardJudgeQueue().available() == 0, "Text input cannot become delayed gameplay input");
        edge(handle, GLFW.GLFW_KEY_Z, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_Z, GLFW.GLFW_RELEASE);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 302000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 2 && batch.keys[0] == 0 && batch.keys[1] == 0, "Only current saved assignment is used");

        input.setStartTime(2);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_X, GLFW.GLFW_RELEASE);
        source.update();
        keyboard.poll(999999999, 1);
        check(input.getKeyboardJudgeQueue().available() == 0, "Poll captured before a clock reset cannot publish stale timestamps");
        poll(keyboard, 400000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 2 && batch.times[0] == 400000, "Clock reset leaves new events for the next valid poll");
        var mouseConfig = config(Keys.X, 0);
        mouseConfig.getMouseScratchConfig().setMouseScratchEnabled(true);
        mouseConfig.getMouseScratchConfig().getKeyAssign()[0] = 0;
        keyboard.setConfig(mouseConfig);
        check(keyboard.getQueuedGameKeys().length == 0 && keyboard.getQueuedMouseScratchGameKeys().length == 1,
                "Mouse-scratch-owned logical keys use mouse metadata, not keyboard metadata");
    }

    private static KeyboardConfig config(int key, int duration) {
        var config = new KeyboardConfig(Mode.BEAT_7K, true);
        Arrays.fill(config.getKeyAssign(), -1);
        config.getKeyAssign()[0] = key;
        config.setDuration(duration);
        config.getMouseScratchConfig().setMouseScratchEnabled(false);
        return config;
    }

    private static void edge(long handle, int key, int action) {
        var callback = GLFW.glfwSetKeyCallback(handle, null);
        GLFW.glfwSetKeyCallback(handle, callback);
        callback.invoke(handle, key, 0, action, 0);
    }

    private static void poll(KeyBoardInputProcesseor keyboard, long time) throws Exception {
        var failure = new AtomicReference<Throwable>();
        Thread worker = new Thread(() -> {
            try { keyboard.poll(time); } catch (Throwable e) { failure.set(e); }
        });
        worker.start();
        worker.join(3000);
        check(!worker.isAlive() && failure.get() == null, "Input worker completed: " + failure.get());
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
