package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.ReplayData;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.utils.Json;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Compare SDL3's legacy logical channel adapter against the unchanged SDL2 producer and real BM. */
public final class SdlGamepadCompatibilityTest {
    public static void main(String[] args) throws Exception {
        logicalIds(); digitalAndReplay(); parity(); focusAndDisconnect();
        System.out.println("SdlGamepadCompatibilityTest passed (all Jamepad IDs, exhaustive int16 normalization, raw-profile isolation, SDL parity, POV/START/SELECT, duration, JKOC/analog v1/v2, time/replays/focus)");
    }
    private static void logicalIds() {
        check(ControllerButton.values().length == SdlGamepadCompatibility.BUTTONS.length, "Pin legacy button count");
        for (var b : ControllerButton.values()) check(SdlGamepadCompatibility.legacyButton(SdlGamepadCompatibility.BUTTONS[b.ordinal()]) == b.ordinal(), "Legacy button round trip " + b);
        check(ControllerButton.DPAD_LEFT.ordinal() == 13 && ControllerButton.DPAD_UP.ordinal() == 11
                && ControllerButton.DPAD_RIGHT.ordinal() == 14 && ControllerButton.DPAD_DOWN.ordinal() == 12, "Pin D-pad IDs independently of adapter");
        check(ControllerAxis.TRIGGERLEFT.ordinal() == 4 && ControllerAxis.TRIGGERRIGHT.ordinal() == 5, "Legacy six-axis order");
        check(SdlGamepadCompatibility.legacyButton(org.lwjgl.sdl.SDLGamepad.SDL_GAMEPAD_BUTTON_MISC2) == -1,
                "New SDL3 buttons cannot become legacy buttons or HAT aliases");
        for (int v = Short.MIN_VALUE; v <= Short.MAX_VALUE; v++)
            check(SdlGamepadCompatibility.normalized((short) v) == Math.max(-1, Math.min(1, v / 32767f)), "Exact Jamepad/clamp formula");
        check(SdlGamepadCompatibility.normalized((short) -1) == -1 / 32767f, "Do not borrow raw -32768 divisor");
    }
    private static void digitalAndReplay() throws Exception {
        try (var f = new Pair(new int[]{0, 28, 29, 15, 20}, 0)) {
            f.settings.setStart(6); f.settings.setSelect(4);
            f.settings.setSdlRawButtonMap(new int[]{3}); f.settings.setSdlRawAxisMap(new int[]{3});
            f.settings.setSdlRawAxisInverted(new boolean[]{true}); f.actual.setConfig(f.settings);
            f.source.press(0, true); f.source.press(0, false); f.actualPoll(1_234_567);
            var batch = f.drainActual(); check(batch.size == 2 && batch.keys[0] == 0 && batch.pressed[0] && !batch.pressed[1]
                    && batch.times[0] == 1_234_567 && batch.times[1] == 1_234_567, "Native tap retains time/order; ignores raw conversion profile");
            f.source.press(11, true); f.source.press(13, true); f.actualPoll(2_000_000);
            check(f.actualDevice.getButton(11) && f.actualDevice.getButton(13) && f.actualDevice.getButton(28) && f.actualDevice.getButton(29), "D-pad original IDs and diagonal aliases coexist");
            f.source.press(6, true); f.source.press(4, true); f.actualPoll(2_001_000);
            check(f.actualInput.startPressed() && f.actualInput.isSelectPressed(), "START/SELECT unchanged");
            f.source.press(6, false); f.source.press(4, false); f.actualPoll(2_002_000);
            check(!f.actualInput.startPressed() && !f.actualInput.isSelectPressed(), "START/SELECT release unchanged");
            f.drainActual();
            var replay = new ReplayData(); replay.controllerEventKeys = f.actualInput.getControllerReplayKeys(); replay.keylog = f.actualInput.getKeyInputLog();
            replay.shrink(); var json = new Json(); replay = json.fromJson(ReplayData.class, json.toJson(replay));
            check(replay.validate(), "Existing compressed replay format");
            f.actualInput.beginInputJudge(null, replay.controllerEventKeys, true);
            for (var event : replay.keylog) f.actualInput.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            check(f.drainActual().size == replay.keylog.length, "New replay preserves all logical edges");
            f.actualInput.beginInputJudge(null, null, true); f.actualInput.setKeyState(0, true, 3_000_000);
            check(f.drainActual().size == 0, "Old replay retains last-state path");
        }
    }
    private static void parity() throws Exception {
        int[] keys = new int[25]; for (int i = 0; i < 21; i++) keys[i] = i;
        for (int i = 21; i < 25; i++) keys[i] = 28 + i - 21;
        try (var f = new Pair(keys, 0)) {
            for (int i = 0; i < 21; i++) {
                f.source.press(i, true); f.compare(1_000_000 + i * 2000);
                f.source.press(i, false); f.compare(1_001_000 + i * 2000);
            }
        }
        try (var f = new Pair(new int[]{0}, 10)) {
            f.source.press(0, true); f.compare(1000); f.source.press(0, false); f.compare(2000); f.compare(10999); f.compare(11000);
        }
        for (int algorithm = -1; algorithm < 2; algorithm++) try (var f = new Pair(new int[]{32, 33, 38, 39, 40, 42}, 0)) {
            f.settings.setJKOC(algorithm < 0); f.settings.setAnalogScratch(algorithm >= 0);
            f.settings.setAnalogScratchMode(Math.max(0, algorithm)); f.settings.setAnalogScratchThreshold(3);
            f.actual.setConfig(f.settings); f.expected.setConfig(f.settings);
            float[] trace = {0, .01f, .03f, .04f, .04f, .04f, -.01f, -.04f, .99f, -.99f, -.98f, -.96f, 0, .95f, -.95f, 0};
            for (int i = 0; i < trace.length; i++) {
                f.source.axes[0] = trace[i]; f.source.axes[3] = -trace[i];
                f.source.axes[4] = Math.max(0, trace[i]); f.source.axes[5] = Math.max(0, -trace[i]);
                f.compare(1_000_000 + i * 1000L);
            }
        }
    }
    private static void focusAndDisconnect() throws Exception {
        try (var f = new Pair(new int[]{0}, 0)) {
            f.source.press(0, true); f.actualPoll(1000); f.drainActual();
            f.source.connected = false; f.actualPoll(2000); check(!f.actualInput.getKeyState(0), "Disconnect releases state");
            f.source.connected = true; f.source.buttons[0] = false; f.actualPoll(3000); f.drainActual();
            f.actual.setWindowActive(false); f.source.press(0, true); f.source.press(0, false);
            f.actual.setWindowActive(true); f.actual.poll(4000); f.drainActual();
            f.actualPoll(5000); check(f.drainActual().size == 0, "Late native edges across focus boundary are discarded");
            f.source.press(0, true); f.actualPoll(6000); check(f.actualInput.getKeyState(0), "Fresh press after baseline");
        }
    }
    static final class Source implements SdlRawSource {
        final boolean[] buttons = new boolean[21]; final float[] axes = new float[6];
        final ArrayDeque<Event> events = new ArrayDeque<>(); boolean connected = true;
        public Device[] devices() { return new Device[]{new Device(7, "Mapped test", 21, 6, 0)}; }
        void press(int button, boolean down) { buttons[button] = down; var e = new Event(); e.device = 7; e.type = Event.BUTTON; e.index = button; e.value = down ? 1 : 0; events.add(e); }
        public void update() { }
        public boolean next(Event target) { var e = events.poll(); if (e == null) return false; target.device = e.device; target.type = e.type; target.index = e.index; target.value = e.value; return true; }
        public boolean connected(int slot) { return connected; }
        public boolean button(int slot, int button) { return buttons[button]; }
        public float axis(int slot, int axis) { return axes[axis]; }
        public int hat(int slot) { return 0; }
        public void close() { }
    }
    private static final class Pair implements AutoCloseable {
        final Source source = new Source();
        final SdlRawControllerManager manager = new SdlRawControllerManager(source, r -> { throw new AssertionError("No listeners or deferred close expected"); }, ControllerBackend.SDL_ASYNC);
        final Controller actualDevice = manager.getControllers().first();
        final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        final SdlControllerManager reference;
        final BMSPlayerInputProcessor actualInput, expectedInput;
        final BMControllerInputProcessor actual, expected;
        final ControllerConfig settings;
        Pair(int[] keys, int duration) throws Exception {
            var delegate = (Controller) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Controller.class}, (p, m, args) -> switch (m.getName()) {
                case "getName", "getUniqueId" -> "Mapped test";
                case "getAxisCount" -> 6; case "getMaxButtonIndex" -> 20; case "getMinButtonIndex" -> 0;
                case "getButton" -> source.buttons[(int) args[0]]; case "getAxis" -> source.axes[(int) args[0]];
                case "isConnected" -> source.connected; case "getMapping" -> SdlGamepadCompatibility.MAPPING;
                default -> null;
            });
            reference = new SdlControllerManager(new Controller[]{delegate}, () -> { }, () -> { }, Runnable::run);
            actualInput = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            expectedInput = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            settings = new ControllerConfig(keys, -1, -1); settings.setDuration(duration);
            actual = bind(actualInput, actualDevice); expected = bind(expectedInput, reference.getControllers().first());
        }
        private BMControllerInputProcessor bind(BMSPlayerInputProcessor input, Controller device) throws Exception {
            var bm = new BMControllerInputProcessor(input, "Mapped test", device, settings);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{bm});
            bm.setEnable(true); bm.clear(); input.beginInputJudge(null, null, false); return bm;
        }
        void actualPoll(long time) throws Exception { worker.submit(() -> { manager.poll(); actual.poll(time); }).get(3, TimeUnit.SECONDS); }
        JudgeEventQueue.Batch drainActual() { var batch = new JudgeEventQueue.Batch(); actualInput.getJudgeEventQueue().drain(batch); return batch; }
        void compare(long time) throws Exception {
            actualPoll(time); reference.poll(); expected.poll(time);
            var a = drainActual(); var b = new JudgeEventQueue.Batch(); expectedInput.getJudgeEventQueue().drain(b);
            check(a.size == b.size, "SDL parity event count at " + time);
            for (int i = 0; i < a.size; i++) check(a.keys[i] == b.keys[i] && a.pressed[i] == b.pressed[i] && a.times[i] == b.times[i], "SDL parity key/state/time/order");
            for (int i = 0; i < settings.getKeyAssign().length; i++) check(actualInput.getKeyState(i) == expectedInput.getKeyState(i), "SDL parity held state");
        }
        public void close() throws Exception { worker.shutdown(); check(worker.awaitTermination(3, TimeUnit.SECONDS), "Worker stopped"); manager.dispose(); reference.dispose(); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
