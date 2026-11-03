package bms.player.beatoraja.input;

import java.util.Arrays;

import com.badlogic.gdx.utils.Json;
import net.java.games.input.Component;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.input.JInputControllerCompatibilityTest.FakeDevice;

/** Synthetic native events through the real raw adapter, BM translation, replay log and judge stream. */
public final class ControllerEventQueueTest {
    public static void main(String[] args) throws Exception {
        testFifoAndDuration();
        testOverflowAndReset();
        testRawShortTapAndReplay();
        testPovOr();
        testDurationAndLifecycle();
        testAxes();
        testReplayVersions();
        testClockResetSerialization();
        testControlsAndDuplicateAssignments();
        testAdapterOverflow();
        testManagerDisposalRelease();
        System.out.println("ControllerEventQueueTest passed (synthetic controller events)");
    }

    private static void testFifoAndDuration() {
        var q = new ControllerEventQueue(8);
        var states = new boolean[32];
        var times = new long[32];
        Arrays.fill(times, Long.MIN_VALUE);
        q.reset(true, states);
        q.offer(1, true);
        q.offer(0, true);
        q.offer(1, false);
        q.offer(0, false);
        int[] expected = {3, 1, 2, 0};
        for (int event : expected) {
            int actual = q.pollNext(states, times, 1000, 0);
            check(actual == event, "JInput event order across buttons retained");
            states[event / 2] = event % 2 != 0;
            times[event / 2] = 1000;
        }
        check(q.pollNext(states, times, 1000, 0) == -1, "No duplicate events");
        q.offer(0, true);
        q.offer(1, true);
        times[0] = 1000;
        times[1] = Long.MIN_VALUE;
        check(q.pollNext(states, times, 2000, 10) == 3, "Deferred key does not block another key");
        states[1] = true;
        times[1] = 2000;
        check(q.deferredCount(states, times, 2000, 10) == 1, "Count pending duration sample");
        check(q.pollNext(states, times, 11000, 10) == 1, "Retain edge until duration boundary");
    }

    private static void testOverflowAndReset() {
        var q = new ControllerEventQueue(2);
        var states = new boolean[32];
        var times = new long[32];
        Arrays.fill(times, Long.MIN_VALUE);
        q.reset(false, states);
        q.offer(0, true);
        check(q.pollNext(states, times, 0, 0) == -1, "Disabled capture is inert");
        q.reset(true, states);
        q.offer(0, true);
        q.reset(true, states);
        check(q.pollNext(states, times, 0, 0) == -1, "Configuration/clock reset drops pending old edges");
        q.offer(0, true);
        q.offer(0, false);
        q.offer(0, true);
        check(q.isOverflowed() && q.pollNext(states, times, 0, 0) == -1, "Overflow fails closed");
        q.reset(true, states);
        check(q.isOverflowed(), "Reset cannot silently recover a corrupted stream");
        var logical = new JudgeEventQueue(1);
        logical.begin(new int[]{0}, states);
        logical.offer(0, true, 0);
        logical.offer(0, false, 0);
        var batch = new JudgeEventQueue.Batch();
        logical.drain(batch);
        check(logical.isOverflowed() && batch.size == 0, "Shared judgment overflow fails closed");
    }

    private static void testRawShortTapAndReplay() throws Exception {
        try (var f = new Fixture(2, 0, new int[]{1, 0}, 0)) {
            f.fake.update(f.fake.buttonComponents[1], 1);
            f.fake.update(f.fake.buttonComponents[0], 1);
            f.fake.update(f.fake.buttonComponents[1], 0);
            f.fake.update(f.fake.buttonComponents[0], 0);
            f.poll(1_000_000);
            var events = f.drain();
            check(events.size == 4, "Press/release survives a single native poll");
            for (int i = 0; i < 4; i++) {
                check(events.keys[i] == i % 2 && events.pressed[i] == (i < 2), "Saved assignments and event order");
                check(events.times[i] == 1_000_000, "Keep original poll timestamp without synthetic offsets");
            }
            check(!f.input.getKeyState(0) && !f.input.getKeyState(1), "UI sees final snapshot");
            var replay = new ReplayData();
            replay.keyboardEventKeys = f.input.getKeyboardReplayKeys();
            replay.controllerEventKeys = f.input.getControllerReplayKeys();
            replay.keylog = f.input.getKeyInputLog();
            replay.shrink();
            var copy = new Json().fromJson(ReplayData.class, new Json().toJson(replay));
            check(copy.validate() && Arrays.equals(copy.controllerEventKeys, new int[]{0, 1}), "Replay mask round trip");
            f.input.beginInputJudge(copy.keyboardEventKeys, copy.controllerEventKeys, true);
            for (var event : copy.keylog) f.input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            var recorded = f.drain();
            check(recorded.size == events.size, "New replay uses the shared event stream");
            for (int i = 0; i < events.size; i++) check(recorded.keys[i] == events.keys[i]
                    && recorded.pressed[i] == events.pressed[i] && recorded.times[i] == events.times[i],
                    "Replay preserves exact order and microsecond timestamps");
        }
    }

    private static void testPovOr() throws Exception {
        try (var f = new Fixture(32, 0, new int[]{28, 29}, 0)) {
            f.fake.update(f.fake.buttonComponents[28], 1);
            f.fake.update(f.fake.pov, Component.POV.LEFT);
            f.fake.update(f.fake.buttonComponents[28], 0);
            f.fake.update(f.fake.pov, Component.POV.OFF);
            f.poll(1_000_000);
            check(f.drain().size == 2, "OR prevents phantom releases/presses when POV and physical button overlap");
            f.fake.update(f.fake.pov, Component.POV.UP_LEFT);
            f.fake.update(f.fake.pov, Component.POV.OFF);
            f.poll(1_001_000);
            var events = f.drain();
            check(events.size == 4 && events.keys[0] == 0 && events.keys[1] == 1
                    && events.pressed[0] && events.pressed[1] && !events.pressed[2] && !events.pressed[3],
                    "Diagonal POV short pulse produces both legacy HAT directions");
        }
    }

    private static void testDurationAndLifecycle() throws Exception {
        try (var f = new Fixture(2, 0, new int[]{0, 1}, 10)) {
            f.fake.update(f.fake.buttonComponents[0], 1);
            f.fake.update(f.fake.buttonComponents[0], 0);
            f.fake.buttons[1] = true;
            f.poll(1_000_000);
            check(f.input.getKeyState(0) && f.input.getKeyState(1) && f.drain().size == 2,
                    "Short release deferred without blocking another button");
            f.poll(1_009_999);
            check(f.drain().size == 0, "No early release before duration");
            f.poll(1_010_000);
            var release = f.drain();
            check(release.size == 1 && !release.pressed[0] && release.times[0] == 1_010_000,
                    "Deferred edge uses accepting poll timestamp");
            f.fake.pollSuccess = false;
            f.poll(1_010_001);
            check(!f.input.getKeyState(1), "Disconnect releases held input without waiting for old duration");
            f.drain();
            f.fake.buttons[1] = false;
            f.fake.pollSuccess = true;
            f.poll(1_011_000);
            check(f.drain().size == 0, "Reconnect does not replay stale history");
            f.fake.update(f.fake.buttonComponents[0], 1);
            f.fake.update(f.fake.buttonComponents[0], 0);
            f.manager.poll();
            f.input.setStartTime(100);
            f.input.beginInputJudge(null, null, false);
            f.controller.poll(0);
            check(f.drain().size == 0, "Clock origin reset discards pending native-adapter edges");
            f.fake.update(f.fake.buttonComponents[0], 1);
            f.fake.update(f.fake.buttonComponents[0], 0);
            f.manager.poll();
            f.config = new ControllerConfig(new int[]{1, -1}, -1, -1);
            f.config.setDuration(0);
            f.controller.setConfig(f.config);
            f.controller.poll(1000);
            check(f.drain().size == 0, "Remapping discards pending edges");
            f.controller.setEnable(false);
            f.fake.buttons[0] = true;
            f.manager.poll();
            f.fake.buttons[0] = false;
            f.manager.poll();
            f.controller.setEnable(true);
            f.controller.poll(2000);
            check(f.drain().size == 0, "Disabled-period taps are not replayed on enable");
        }
    }

    private static void testAxes() throws Exception {
        try (var f = new Fixture(0, 4, new int[]{32, 33}, 0)) {
            f.fake.update(f.fake.axisComponents[0], 1);
            f.fake.update(f.fake.axisComponents[0], -1);
            f.fake.update(f.fake.axisComponents[0], 0);
            f.poll(1_000_000);
            check(f.drain().size == 0, "Same-poll axis reversal is deliberately not restored");
            f.fake.axes[0] = 1;
            f.poll(1_001_000);
            check(f.drain().size == 1, "Digital axis result reaches judge queue");
            f.fake.axes[0] = -1;
            f.poll(1_002_000);
            var reverse = f.drain();
            check(reverse.size == 2 && !reverse.pressed[0] && reverse.pressed[1], "Snapshot axis direction changes retained");
        }
        try (var f = new Fixture(0, 4, new int[]{32, 33}, 0)) {
            f.config.setJKOC(true);
            f.controller.setConfig(f.config);
            f.fake.axes[3] = 1;
            f.poll(1_000_000);
            check(f.drain().size == 1 && f.input.getKeyState(0), "JKOC fourth-axis fallback reaches queue");
            f.fake.axes[0] = 1;
            f.fake.axes[3] = 0;
            f.poll(1_001_000);
            check(f.drain().size == 0, "JKOC axis OR does not create redundant events");
        }
    }

    private static void testReplayVersions() throws Exception {
        var input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
        input.beginInputJudge(null, null, true);
        input.setKeyState(0, true, 1000);
        input.setKeyState(0, false, 1000);
        check(input.getJudgeEventQueue().available() == 0, "Legacy replay keeps last-state semantics");
        input.beginInputJudge(new int[]{0}, null, true);
        input.setKeyState(0, true, 2000);
        input.setKeyState(1, true, 2000);
        check(input.getJudgeEventQueue().available() == 1 && !input.getJudgeEventQueue().isQueued(1),
                "Keyboard-only replay does not reinterpret old controller input");
        input.beginInputJudge(new int[]{0}, new int[]{1}, true);
        input.setKeyState(1, false, 3000);
        input.setKeyState(0, false, 3000);
        var batch = new JudgeEventQueue.Batch();
        input.getJudgeEventQueue().drain(batch);
        check(batch.size == 2 && batch.keys[0] == 1 && batch.keys[1] == 0,
                "Mixed-device replay retains one ordered stream");
        var invalid = new ReplayData();
        invalid.controllerEventKeys = new int[257];
        check(!invalid.validate(), "Bound replay masks");
        invalid.controllerEventKeys = new int[]{-1, 0, 0, 999};
        invalid.keylog = new KeyInputLog[]{new KeyInputLog(1000, 0, true)};
        check(invalid.validate() && Arrays.equals(invalid.controllerEventKeys, new int[]{0}),
                "Replay masks discard invalid IDs and duplicates");
    }

    private static void testAdapterOverflow() throws Exception {
        var previous = com.badlogic.gdx.Gdx.app;
        int[] exits = {0};
        try (var f = new Fixture(1, 0, new int[]{0}, 0)) {
            com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(
                    ControllerEventQueueTest.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Application.class},
                    (proxy, method, args) -> { if (method.getName().equals("exit")) exits[0]++; return null; });
            for (int i = 0; i < 130; i++) f.fake.update(f.fake.buttonComponents[0], i % 2 == 0 ? 1 : 0);
            f.poll(1_000_000);
            check(exits[0] == 1 && f.drain().size == 0, "Adapter overflow requests exit without partial gameplay events");
            f.poll(1_001_000);
            check(exits[0] == 1, "Report/request overflow stop only once");
        } finally { com.badlogic.gdx.Gdx.app = previous; }
    }

    private static void testManagerDisposalRelease() throws Exception {
        try (var f = new Fixture(1, 0, new int[]{0}, 10)) {
            f.fake.buttons[0] = true;
            f.poll(1_000_000);
            check(f.input.getKeyState(0), "Held before manager shutdown");
            f.drain();
            f.manager.dispose();
            f.controller.poll(1_000_001);
            var batch = f.drain();
            check(!f.input.getKeyState(0) && batch.size == 1 && !batch.pressed[0],
                    "Manager shutdown/poll exception releases held input even inside duration");
        }
    }

    private static void testControlsAndDuplicateAssignments() throws Exception {
        try (var f = new Fixture(2, 0, new int[]{0, 0}, 0)) {
            f.config = new ControllerConfig(new int[]{0, 0}, 0, 1);
            f.config.setDuration(0);
            f.controller.setConfig(f.config);
            f.fake.buttons[0] = f.fake.buttons[1] = true;
            f.poll(1_000_000);
            var batch = f.drain();
            check(batch.size == 1 && batch.keys[0] == 0 && !f.input.getKeyState(1),
                    "Duplicate physical assignment retains first logical mapping");
            check(!f.input.startPressed() && f.input.isSelectPressed(), "Play mapping takes priority over START; SELECT unchanged");
            f.fake.buttons[0] = f.fake.buttons[1] = false;
            f.poll(1_001_000);
            check(!f.input.isSelectPressed() && !f.input.startPressed(), "Control releases retained");
        }
    }

    private static void testClockResetSerialization() throws Exception {
        var previousInput = com.badlogic.gdx.Gdx.input;
        var previousRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var resetStarted = new java.util.concurrent.CountDownLatch(1);
        var resetDone = new java.util.concurrent.CountDownLatch(1);
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        try (var f = new Fixture(1, 0, new int[]{0}, 0)) {
            com.badlogic.gdx.Gdx.input = (com.badlogic.gdx.Input) java.lang.reflect.Proxy.newProxyInstance(
                    ControllerEventQueueTest.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Input.class},
                    (proxy, method, args) -> method.getReturnType() == boolean.class ? false
                            : method.getReturnType() == int.class ? 0 : null);
            BMSPlayerInputProcessor.controllerPollRunner = () -> {
                entered.countDown();
                try { check(release.await(3, java.util.concurrent.TimeUnit.SECONDS), "Release blocked poll"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                f.fake.buttons[0] = true;
                f.manager.poll();
            };
            Thread poll = new Thread(() -> {
                try { f.input.poll(); } catch (Throwable e) { error.set(e); }
            });
            Thread reset = new Thread(() -> {
                resetStarted.countDown();
                try { f.input.setStartTime(100); } catch (Throwable e) { error.set(e); }
                finally { resetDone.countDown(); }
            });
            poll.start();
            try {
                check(entered.await(3, java.util.concurrent.TimeUnit.SECONDS), "Controller poll entered");
                reset.start();
                check(resetStarted.await(3, java.util.concurrent.TimeUnit.SECONDS), "Clock reset started");
                check(!resetDone.await(25, java.util.concurrent.TimeUnit.MILLISECONDS), "Clock reset waits for in-flight controller poll");
            } finally {
                release.countDown();
                poll.join(3000);
                if (reset.getState() != Thread.State.NEW) reset.join(3000);
            }
            check(!poll.isAlive() && !reset.isAlive() && error.get() == null, "No deadlock or polling failure: " + error.get());
            check(f.input.getStartTime() == 100 && f.input.getKeyInputLog().length == 0
                    && f.drain().size == 0, "No old-clock events remain after reset");
        } finally {
            release.countDown();
            com.badlogic.gdx.Gdx.input = previousInput;
            BMSPlayerInputProcessor.controllerPollRunner = previousRunner;
        }
    }

    private static final class Fixture implements AutoCloseable {
        final FakeDevice fake;
        final JInputControllerManager manager;
        final BMSPlayerInputProcessor input;
        final BMControllerInputProcessor controller;
        ControllerConfig config;
        Fixture(int buttons, int axes, int[] mapping, int duration) throws Exception {
            fake = new FakeDevice(buttons, axes);
            manager = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device},
                    () -> { }, () -> { }, Runnable::run);
            input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            config = new ControllerConfig(mapping, -1, -1);
            config.setDuration(duration);
            controller = new BMControllerInputProcessor(input, "test", manager.getControllers().first(), config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{controller});
            controller.setEnable(true);
            controller.clear();
            input.beginInputJudge(null, null, false);
        }
        void poll(long time) { manager.poll(); controller.poll(time); }
        JudgeEventQueue.Batch drain() {
            var batch = new JudgeEventQueue.Batch();
            input.getJudgeEventQueue().drain(batch);
            return batch;
        }
        @Override public void close() { manager.dispose(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
