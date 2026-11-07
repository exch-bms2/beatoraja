package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayModeConfig.MidiConfig;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.config.KeyConfiguration;
import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.utils.Json;
import org.lwjgl.glfw.GLFW;

/** Real libGDX callbacks with injected Windows scancodes, NOT physical Windows keyboard validation. */
public final class Lwjgl3WindowsKeyboardTest {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("beatoraja Windows JIS keyboard test");
        config.setWindowedMode(320, 240);
        config.disableAudio(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            @Override public void render() {
                if (++frames != 10) return;
                try { test((PollingLwjgl3Input) Gdx.input); }
                catch (Exception e) { throw new AssertionError(e); }
                Gdx.app.exit();
            }
        }, config) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) {
                // Exercise the Windows policy without lying about the host OS to native loaders.
                return new PollingLwjgl3Input(window, null, true, true);
            }
        };
        System.out.println("Lwjgl3WindowsKeyboardTest passed (synthetic Windows scancodes through real GLFW/libGDX, key config/state/FIFO/duration/focus/replay)");
    }

    private static void test(PollingLwjgl3Input source) throws Exception {
        long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
        focus(handle, true);
        var config = new KeyboardConfig(Mode.BEAT_7K, true);
        Arrays.fill(config.getKeyAssign(), -1);
        config.setDuration(0);
        config.getMouseScratchConfig().setMouseScratchEnabled(false);
        var input = KeyboardQueueTestSupport.input(config);
        var keyboard = input.getKeyBoardInputProcesseor();
        source.setInputProcessor(keyboard);
        source.update();
        var screen = KeyboardQueueTestSupport.allocate(KeyConfiguration.class);
        KeyboardQueueTestSupport.set(screen, KeyConfiguration.class, "keyboard", keyboard);
        KeyboardQueueTestSupport.set(screen, KeyConfiguration.class, "keyboardConfig", config);
        KeyboardQueueTestSupport.set(screen, KeyConfiguration.class, "controllerConfigs", new ControllerConfig[0]);
        KeyboardQueueTestSupport.set(screen, KeyConfiguration.class, "midiconfig", new MidiConfig(Mode.BEAT_7K, false));
        var assign = KeyConfiguration.class.getDeclaredMethod("setKeyboardKeyAssign", int.class);
        assign.setAccessible(true);

        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_PRESS);
        check(source.isKeyPressed(240) && !source.isKeyPressed(73) && !source.isKeyPressed(Keys.UNKNOWN),
                "Render-thread held state uses Ro, not Unknown or Yen");
        check(source.isKeyJustPressed(240) && source.isKeyJustPressed(Keys.ANY_KEY), "libGDX just-pressed state");
        source.update();
        check(keyboard.getLastPressedKey() == 240, "Key config receives Ro through original InputProcessor");
        assign.invoke(screen, 0);
        check(config.getKeyAssign()[0] == 240 && screen.getKeyAssign(0).equals("JIS Ro"), "Ro assignment and screen name");
        check(read(source, 240), "Published Ro state agrees on input thread");

        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_PRESS);
        source.update();
        check(keyboard.getLastPressedKey() == 73 && source.isKeyPressed(73) && source.isKeyPressed(240),
                "Both physical keys can be held independently");
        assign.invoke(screen, 1);
        check(config.getKeyAssign()[1] == 73 && screen.getKeyAssign(1).equals("Yen/\\"), "Yen retains old Backslash ID");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_RELEASE);
        check(!source.isKeyPressed(240) && source.isKeyPressed(73), "Ro release cannot release Yen");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_RELEASE);
        source.update();
        check(!read(source, 240) && !read(source, 73), "Both published releases are visible");
        check(!source.isKeyPressed(Keys.ANY_KEY), "Balanced libGDX press count");
        config.getKeyAssign()[2] = Keys.Z;
        keyboard.setConfig(config);
        keyboard.clear();
        input.beginKeyboardJudge(null, false);
        poll(keyboard, 500);
        tap(handle, 0x73);
        tap(handle, 0x7d);
        edge(handle, GLFW.GLFW_KEY_Z, 0x73, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_Z, 0x73, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 123_456);
        var batch = new KeyboardJudgeQueue.Batch();
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 6, "Two short JIS taps and an existing assignment survive the FIFO");
        for (int i = 0; i < batch.size; i++) check(batch.keys[i] == i / 2
                && batch.pressed[i] == (i % 2 == 0) && batch.times[i] == 123_456,
                "Existing per-key ordering, duration=0 and poll-time timestamps");
        var replay = new ReplayData();
        replay.keyboardEventKeys = input.getKeyboardReplayKeys();
        replay.keylog = input.getKeyInputLog();
        replay.shrink();
        var json = new Json();
        var restored = json.fromJson(ReplayData.class, json.toJson(replay));
        check(restored.validate(), "Existing compressed replay format");
        input.beginKeyboardJudge(restored.keyboardEventKeys, true);
        for (var event : restored.keylog) input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
        var playback = new KeyboardJudgeQueue.Batch();
        input.getKeyboardJudgeQueue().drain(playback);
        check(playback.size == batch.size, "New replay keeps all fallback-key edges");
        for (int i = 0; i < batch.size; i++) check(playback.keys[i] == batch.keys[i]
                && playback.pressed[i] == batch.pressed[i] && playback.times[i] == batch.times[i], "Replay parity");
        input.beginKeyboardJudge(null, true);
        input.setKeyState(0, true, 200_000);
        input.getKeyboardJudgeQueue().drain(playback);
        check(playback.size == 0 && input.getKeyState(0), "Metadata-free replay retains legacy path");
        input.resetAllKeyState();
        config.setDuration(10);
        keyboard.setConfig(config);
        keyboard.clear();
        input.beginKeyboardJudge(null, false);
        tap(handle, 0x73);
        source.update();
        poll(keyboard, 300_000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 1 && batch.pressed[0], "Duration defers short-tap release");
        poll(keyboard, 310_000);
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 1 && !batch.pressed[0] && batch.times[0] == 310_000, "Duration acceptance time unchanged");

        config.setDuration(0);
        keyboard.setConfig(config);
        keyboard.clear();
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_PRESS);
        source.update();
        focus(handle, false);
        source.update();
        check(!read(source, 240) && !source.isKeyPressed(240), "Focus loss releases owner and published alias state");
        focus(handle, true);
        source.update();
        check(!read(source, 240), "Held aliases are suppressed on focus return");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_RELEASE);
        source.update();
        check(!source.isKeyPressed(Keys.ANY_KEY), "Late physical release cannot decrement the count twice");
        tap(handle, 0x73);
        source.update();
        check(!read(source, 240), "Fresh tap ends released after focus recovery");

        Arrays.fill(config.getKeyAssign(), -1);
        config.setStart(240);
        config.setSelect(73);
        keyboard.setConfig(config);
        keyboard.clear();
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_PRESS);
        source.update();
        poll(keyboard, 400_000);
        check(input.startPressed() && input.isSelectPressed(), "START/SELECT read the same published aliases");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 401_000);
        check(!input.startPressed() && input.isSelectPressed(), "Independent START release");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_RELEASE);
        source.update();
        poll(keyboard, 402_000);
        check(!input.startPressed() && !input.isSelectPressed(), "No stuck START/SELECT aliases");

        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_PRESS);
        source.update();
        var iconify = GLFW.glfwSetWindowIconifyCallback(handle, null);
        GLFW.glfwSetWindowIconifyCallback(handle, iconify);
        iconify.invoke(handle, true);
        check(!source.isKeyPressed(73), "Minimization also releases aliases without a focus callback");
        iconify.invoke(handle, false);
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_RELEASE);
        source.update();
        check(!source.isKeyPressed(Keys.ANY_KEY), "Minimize/restore and physical release stay balanced");

        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_PRESS);
        source.update();
        source.windowHandleChanged(handle);
        source.update();
        check(!source.isKeyPressed(240) && !read(source, 240), "Callback replacement releases old alias state");
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_RELEASE);
        source.update();
        check(!source.isKeyPressed(Keys.ANY_KEY), "Old physical release after replacement is ignored");

        for (boolean fallbackFirst : new boolean[]{false, true}) {
            int first = fallbackFirst ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_BACKSLASH;
            int second = fallbackFirst ? GLFW.GLFW_KEY_BACKSLASH : GLFW.GLFW_KEY_UNKNOWN;
            int firstScan = fallbackFirst ? 0x7d : 0x2b;
            int secondScan = fallbackFirst ? 0x2b : 0x7d;
            edge(handle, first, firstScan, GLFW.GLFW_PRESS);
            edge(handle, second, secondScan, GLFW.GLFW_PRESS);
            edge(handle, first, firstScan, GLFW.GLFW_RELEASE);
            check(source.isKeyPressed(73), "Releasing one Backslash/Yen source preserves the other");
            source.update();
            check(read(source, 73), "Published alias uses OR too");
            edge(handle, second, secondScan, GLFW.GLFW_RELEASE);
            source.update();
            check(!read(source, 73) && !source.isKeyPressed(Keys.ANY_KEY), "Both alias source orders stay balanced");
        }

        // Keys and characters remain separate; aliases must never synthesize text.
        var text = new StringBuilder();
        source.setInputProcessor(new InputAdapter() {
            @Override public boolean keyTyped(char ch) { text.append(ch); return true; }
        });
        tap(handle, 0x73);
        tap(handle, 0x7d);
        source.update();
        check(text.isEmpty(), "Scancode aliases do not fabricate a second character stream");
        var callback = GLFW.glfwSetCharCallback(handle, null);
        GLFW.glfwSetCharCallback(handle, callback);
        callback.invoke(handle, '\\');
        source.update();
        check(text.toString().equals("\\"), "Original GLFW character/IME path unchanged");
        if (source.getDiagnostics() != null) check(source.getDiagnostics().snapshot().unknownKeys() == 0,
                "Resolved JIS keys are not counted as unsupported input");
    }

    private static void tap(long handle, int scan) {
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, scan, GLFW.GLFW_PRESS);
        for (int i = 0; i < 140; i++) edge(handle, GLFW.GLFW_KEY_UNKNOWN, scan, GLFW.GLFW_PRESS);
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, scan, GLFW.GLFW_REPEAT);
        edge(handle, GLFW.GLFW_KEY_UNKNOWN, scan, GLFW.GLFW_RELEASE);
    }

    private static void edge(long handle, int key, int scan, int action) {
        var callback = GLFW.glfwSetKeyCallback(handle, null);
        GLFW.glfwSetKeyCallback(handle, callback);
        callback.invoke(handle, key, scan, action, 0);
    }

    private static void focus(long handle, boolean focused) {
        var callback = GLFW.glfwSetWindowFocusCallback(handle, null);
        GLFW.glfwSetWindowFocusCallback(handle, callback);
        callback.invoke(handle, focused);
    }

    private static boolean read(PollingLwjgl3Input source, int key) throws Exception {
        var pressed = new AtomicReference<Boolean>();
        worker(() -> pressed.set(source.isKeyPressed(key)));
        return pressed.get();
    }

    private static void poll(KeyBoardInputProcesseor keyboard, long time) throws Exception { worker(() -> keyboard.poll(time)); }

    private static void worker(Runnable action) throws Exception {
        var failure = new AtomicReference<Throwable>();
        Thread thread = new Thread(() -> { try { action.run(); } catch (Throwable e) { failure.set(e); } });
        thread.start();
        thread.join(3000);
        check(!thread.isAlive() && failure.get() == null, "Input worker completed: " + failure.get());
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
