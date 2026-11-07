package bms.player.beatoraja.input;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

import net.java.games.input.AbstractComponent;
import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.Rumbler;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.ReplayData;
import com.badlogic.gdx.utils.Json;

/** Real JInput queue/filtering with a synthetic native source, no hardware or window. */
public final class JInputQueueRecoveryTest {
    public static void main(String[] args) throws Exception {
        String previous = System.getProperty("beatoraja.inputDiagnostics");
        System.setProperty("beatoraja.inputDiagnostics", "true");
        try {
            testBurstAndReplay();
            testOverflowRecovery(false);
            testOverflowRecovery(true);
            testSnapshotRace();
            testMissingStateAndPovOr();
            testAxisRecovery();
            testRelativeAxisCompatibility();
            testRecoveryDuration();
            testWithoutDiagnostics();
        } finally {
            if (previous == null) System.clearProperty("beatoraja.inputDiagnostics");
            else System.setProperty("beatoraja.inputDiagnostics", previous);
        }
        System.out.println("JInputQueueRecoveryTest passed (real JInput queues, synthetic native source)");
    }

    private static void testBurstAndReplay() throws Exception {
        try (var f = new Fixture(false)) {
            check(f.device.requestedCapacity == 1024, "Resize native and Java queues before polling");
            var input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            var config = new ControllerConfig(new int[]{6, 5, 4, 3, 2, 1, 0}, 8, 9);
            config.setDuration(0);
            var bm = new BMControllerInputProcessor(input, "burst", f.raw, config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{bm});
            bm.setEnable(true);
            bm.clear();
            input.beginInputJudge(null, null, false);
            for (int i = 0; i < 7; i++) f.device.edge(f.device.buttons[i], 1);
            f.device.axisBurst(128);
            for (int i = 0; i < 7; i++) f.device.edge(f.device.buttons[i], 0);
            f.manager.poll();
            bm.poll(1_234_567);
            var batch = new JudgeEventQueue.Batch();
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 14, "All seven press/release pairs survive an axis burst larger than 32");
            for (int i = 0; i < batch.size; i++) {
                check(batch.keys[i] == 6 - i % 7 && batch.pressed[i] == (i < 7), "Assignment and FIFO order");
                check(batch.times[i] == 1_234_567, "Existing caller-supplied microsecond judgment timestamp");
            }
            var replay = new ReplayData();
            replay.controllerEventKeys = input.getControllerReplayKeys();
            replay.keylog = input.getKeyInputLog();
            replay.shrink();
            var json = new Json();
            var restored = json.fromJson(ReplayData.class, json.toJson(replay));
            check(restored.validate() && Arrays.equals(restored.controllerEventKeys, replay.controllerEventKeys),
                    "Existing replay metadata/encoding");
            input.beginInputJudge(restored.keyboardEventKeys, restored.controllerEventKeys, true);
            for (var event : restored.keylog) input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            var playback = new JudgeEventQueue.Batch();
            input.getJudgeEventQueue().drain(playback);
            check(playback.size == batch.size, "Replay retains every burst edge");
            for (int i = 0; i < batch.size; i++) check(playback.keys[i] == batch.keys[i]
                    && playback.pressed[i] == batch.pressed[i] && playback.times[i] == batch.times[i], "Replay parity");
            check(f.raw.diagnostics.snapshot().snapshotCorrections() == 0, "Intact burst needs no repair");
        }
    }

    private static void testOverflowRecovery(boolean resizeFailure) {
        try (var f = new Fixture(resizeFailure)) {
            f.device.edge(f.device.buttons[6], 1);
            f.device.axisBurst(resizeFailure ? 64 : 1200);
            f.device.edge(f.device.buttons[6], 0);
            f.manager.poll();
            check(f.raw.getButton(6), "Real AbstractController queue dropped release after filling");
            check(f.edge(6, false) == 1, "Retain observed press despite snapshot disagreement");
            check(f.edge(6, true) == -1, "No same-poll phantom release from potentially older snapshot");
            var first = f.raw.diagnostics.snapshot();
            check(first.buttonSnapshotMismatches() == 1 && first.snapshotDeferred() == 1,
                    "Diagnose mismatch without undoing observed press");
            check(first.jinputMaxEvents() == (resizeFailure ? 32 : 1024), "Actual JInput queue saturation reproduced");
            check(first.jinputSaturatedPolls() == (resizeFailure ? 0 : 1), "No capacity claim after failed native resize");
            f.manager.poll();
            check(!f.raw.getButton(6) && f.edge(6, true) == 0, "Recover dropped release on next event-free poll");
            check(f.raw.diagnostics.snapshot().snapshotCorrections() == 1, "Single corrective edge");
            f.manager.poll();
            check(f.edge(6, false) == -1, "No duplicate correction or repeated stuck state");
            f.device.buttons[2].value = 1; // Entire press absent from native event stream.
            f.manager.poll();
            check(f.raw.getButton(2) && f.edge(2, false) == 1, "Recover missing press from current snapshot");
        }
    }

    private static void testSnapshotRace() {
        try (var f = new Fixture(false)) {
            f.device.edge(f.device.buttons[0], 1);
            f.device.edge(f.device.pov, Component.POV.UP);
            // Model DirectInput's GetDeviceState preceding GetDeviceData.
            f.device.buttons[0].value = 0;
            f.device.pov.value = Component.POV.OFF;
            f.manager.poll();
            check(f.raw.getButton(0) && f.raw.getButton(29), "Newer button/POV events beat older snapshot");
            check(f.edge(0, false) == 1 && f.edge(29, false) == 1, "Presses remain in FIFO");
            check(f.edge(0, true) == -1 && f.edge(29, true) == -1, "No phantom releases");
            f.device.buttons[0].value = 1;
            f.device.pov.value = Component.POV.UP;
            f.manager.poll();
            check(f.raw.diagnostics.snapshot().snapshotCorrections() == 0, "Snapshot catches up without synthetic edges");
            f.device.edge(f.device.buttons[0], 0);
            f.device.edge(f.device.pov, Component.POV.OFF);
            f.device.buttons[0].value = 1;
            f.device.pov.value = Component.POV.UP;
            f.manager.poll();
            check(!f.raw.getButton(0) && !f.raw.getButton(29), "Newer releases beat older held snapshot");
            check(f.edge(0, true) == 0 && f.edge(29, true) == 0, "Releases retained");
            f.device.buttons[0].value = 0;
            f.device.pov.value = Component.POV.OFF;
            f.manager.poll();
            check(f.edge(0, false) == -1 && f.edge(29, false) == -1, "No phantom re-presses");
        }
    }

    private static void testMissingStateAndPovOr() {
        try (var f = new Fixture(false)) {
            f.device.edge(f.device.buttons[28], 1);
            f.device.edge(f.device.pov, Component.POV.LEFT);
            f.manager.poll();
            check(f.edge(28, false) == 1, "Physical button and first POV are ORed");
            f.device.buttons[28].value = 0;
            f.manager.poll();
            check(f.raw.getButton(28) && f.edge(28, true) == -1, "One corrected source cannot release other held source");
            f.device.pov.value = Component.POV.OFF;
            f.manager.poll();
            check(!f.raw.getButton(28) && f.edge(28, true) == 0, "Missing POV release recovered");
            f.device.edge(f.device.buttons[28], 0);
            f.device.edge(f.device.pov, Component.POV.OFF);
            f.manager.poll();
            check(f.edge(28, false) == -1, "Late duplicate native release does not duplicate correction");
            f.device.buttons[1].value = 1;
            f.manager.poll();
            f.raw.resetEvents(false);
            f.device.buttons[1].value = 0;
            f.manager.poll();
            check(!f.raw.getButton(1) && f.edge(1, true) == -1, "Disabled FIFO does not accumulate corrective events");
        }
    }

    private static void testAxisRecovery() {
        try (var f = new Fixture(false)) {
            f.device.axes[0].value = 2;
            f.device.axes[3].value = -1;
            f.manager.poll();
            check(f.raw.getAxis(0) == 1 && f.raw.getAxis(3) == -1, "Recover missing axes including JKOC fourth axis");
            f.device.axes[0].value = 1;
            f.device.axes[3].value = Float.NaN;
            f.manager.poll();
            check(f.raw.getAxis(0) == .5f && f.raw.getAxis(3) == 0, "Keep observed-maximum normalization and finite guard");
            f.device.axes[0].value = .01f;
            f.device.axes[0].deadZone = .02f;
            f.manager.poll();
            check(f.raw.getAxis(0) == 0, "Component dead zone retained");
            check(f.raw.diagnostics.snapshot().axisSnapshotMismatches() == 5, "Axis corrections counted separately");
        }
    }

    private static void testRecoveryDuration() throws Exception {
        try (var f = new Fixture(false)) {
            var input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            var config = new ControllerConfig(new int[]{0}, -1, -1);
            config.setDuration(10);
            var bm = new BMControllerInputProcessor(input, "recovery", f.raw, config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{bm});
            bm.setEnable(true);
            bm.clear();
            input.beginInputJudge(null, null, false);
            f.device.edge(f.device.buttons[0], 1);
            f.manager.poll();
            bm.poll(1_000_000);
            var batch = new JudgeEventQueue.Batch();
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 1 && batch.pressed[0], "Initial press accepted");
            f.device.buttons[0].value = 0;
            f.manager.poll();
            bm.poll(1_001_000);
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 0 && input.getKeyState(0), "Corrective release respects existing duration");
            bm.poll(1_010_000);
            input.getJudgeEventQueue().drain(batch);
            check(batch.size == 1 && !batch.pressed[0] && batch.times[0] == 1_010_000,
                    "Correction uses acceptance poll timestamp, not native event time");
        }
    }

    private static void testRelativeAxisCompatibility() {
        try (var f = new Fixture(false)) {
            f.device.axes[0].relative = true;
            f.device.edge(f.device.axes[0], 1);
            f.manager.poll();
            check(f.raw.getAxis(0) == 1, "Relative axis still uses event-driven accumulated delta");
            f.manager.poll();
            check(f.raw.getAxis(0) == 1, "Do not reinterpret JInput's relative-delta reset as a missing event");
            f.device.edge(f.device.axes[0], -1);
            f.manager.poll();
            check(f.raw.getAxis(0) == -1, "Relative direction reversal unchanged");
        }
    }

    private static void testWithoutDiagnostics() {
        System.clearProperty("beatoraja.inputDiagnostics");
        try (var f = new Fixture(false)) {
            check(f.raw.diagnostics == null, "Diagnostics remain opt-in");
            f.device.edge(f.device.buttons[1], 1);
            f.manager.poll();
            check(f.edge(1, false) == 1, "Press without diagnostics");
            f.device.buttons[1].value = 0;
            f.manager.poll();
            check(!f.raw.getButton(1) && f.edge(1, true) == 0, "Recovery also runs without diagnostics");
        } finally {
            System.setProperty("beatoraja.inputDiagnostics", "true");
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Device device = new Device();
        final JInputControllerManager manager;
        final JInputControllerManager.RawController raw;

        Fixture(boolean resizeFailure) {
            device.resizeFailure = resizeFailure;
            manager = new JInputControllerManager(new Controller[]{device}, () -> { }, () -> { }, Runnable::run);
            raw = (JInputControllerManager.RawController) manager.getControllers().first();
            raw.resetEvents(true);
        }

        int edge(int key, boolean current) { return raw.events().pollReady(key, current, 1_234_567, Long.MIN_VALUE, 0); }
        @Override public void close() { manager.dispose(); }
    }

    private static final class Part extends AbstractComponent {
        float value, deadZone;
        boolean relative;
        Part(Component.Identifier identifier) { super(identifier.getName(), identifier); }
        @Override public boolean isRelative() { return relative; }
        @Override public boolean isAnalog() { return getIdentifier() instanceof Component.Identifier.Axis; }
        @Override public float getDeadZone() { return deadZone; }
        @Override protected float poll() { return value; }
    }

    private static final class Device extends AbstractController {
        final Part[] buttons = new Part[32], axes = new Part[4];
        final Part pov;
        final ArrayDeque<Event> nativeEvents = new ArrayDeque<>();
        boolean resizeFailure;
        int requestedCapacity;

        Device() { this(parts()); }

        private Device(Component[] parts) {
            super("Synthetic native controller", parts, new Controller[0], new Rumbler[0]);
            for (int i = 0; i < buttons.length; i++) buttons[i] = (Part) parts[i];
            for (int i = 0; i < axes.length; i++) axes[i] = (Part) parts[32 + i];
            pov = (Part) parts[36];
        }

        private static Component[] parts() {
            var parts = new Component[37];
            for (int i = 0; i < 32; i++) parts[i] = new Part(Component.Identifier.Button._0);
            for (int i = 32; i < 36; i++) parts[i] = new Part(Component.Identifier.Axis.X);
            parts[36] = new Part(Component.Identifier.Axis.POV);
            return parts;
        }

        @Override public Type getType() { return Type.STICK; }
        @Override protected void setDeviceEventQueueSize(int size) throws IOException {
            requestedCapacity = size;
            if (resizeFailure) throw new IOException("Expected synthetic native resize failure");
        }

        void edge(Part part, float value) {
            part.value = value;
            var event = new Event();
            event.set(part, value, 999_999_999); // Must not replace beatoraja judgment timestamp.
            nativeEvents.addLast(event);
        }

        void axisBurst(int count) {
            for (int i = 0; i < count; i++) edge(axes[0], i % 2 == 0 ? 1 : -1);
        }

        @Override protected boolean getNextDeviceEvent(Event target) {
            var event = nativeEvents.pollFirst();
            if (event == null) return false;
            target.set(event.getComponent(), event.getValue(), event.getNanos());
            return true;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
