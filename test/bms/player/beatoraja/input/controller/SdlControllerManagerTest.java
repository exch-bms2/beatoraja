package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.ReplayData;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.utils.Json;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic SDL producer frames through the real common FIFO/BM/replay consumers. */
public final class SdlControllerManagerTest {
    public static void main(String[] args) throws Exception {
        shortTapReplay(); durationAndPov(); scratchAndFocus(); scratchParity(); fullInputLifecycle(); lifecycle(); overflow();
        focusRegainDuringDeviceRead();
        System.out.println("SdlControllerManagerTest passed (shared FIFO/time/duration, POV OR, JKOC, analog v1/v2, focus/disconnect, replay versions, thread ownership and shutdown)");
    }

    private static void focusRegainDuringDeviceRead() throws Exception {
        var reading = new CountDownLatch(1);
        var inputLocked = new CountDownLatch(1);
        var owner = new FutureTask<Void>(() -> {
            try (var f = new Fixture(new int[]{0, 1}, 0)) {
                f.fake.buttons[0] = true; f.sample(); f.controller.poll(1000); f.drain();
                check(f.input.getKeyState(0), "Held Jamepad input before focus loss");
                var worker = new FutureTask<Void>(() -> {
                    SdlHotplugTest.await(reading);
                    synchronized (f.input.getKeyBoardInputProcesseor()) {
                        inputLocked.countDown();
                        f.controller.poll(2000);
                    }
                    return null;
                });
                SdlHotplugTest.startDaemon(worker, "test-jamepad-input");
                f.fake.buttons[1] = true;
                f.fake.onAxis = () -> {
                    f.fake.onAxis = () -> { };
                    reading.countDown();
                    SdlHotplugTest.await(inputLocked);
                    synchronized (f.input.getKeyBoardInputProcesseor()) {
                        f.controller.setWindowActive(false);
                        f.controller.setWindowActive(true);
                    }
                };
                f.sample();
                worker.get(5, TimeUnit.SECONDS);
                var device = (QueuedController) f.device;
                check(!device.getButton(1) && !device.windowBaselineReady(), "Discard interrupted Jamepad sample");
                f.controller.poll(3000);
                f.sample(); f.controller.poll(4000);
                check(device.getButton(1) && f.drain().size == 0, "Baseline does not forward held buttons");
                f.fake.buttons[1] = false; f.sample(); f.controller.poll(5000); f.drain();
                f.fake.buttons[1] = true; f.sample(); f.controller.poll(6000);
                var batch = f.drain();
                check(batch.size == 1 && batch.keys[0] == 1 && batch.pressed[0] && batch.times[0] == 6000,
                        "Fresh Jamepad press keeps its binding and poll timestamp");
            }
            return null;
        });
        SdlHotplugTest.startDaemon(owner, "test-jamepad-owner");
        owner.get(10, TimeUnit.SECONDS);
    }

    private static void shortTapReplay() throws Exception {
        try (var f = new Fixture(new int[]{0, 1}, 0)) {
            f.fake.buttons[0] = true; f.sample(); f.fake.buttons[1] = true; f.sample();
            f.fake.buttons[0] = false; f.sample(); f.fake.buttons[1] = false; f.sample();
            f.controller.poll(1000000);
            var events = f.drain(); check(events.size == 4, "Both producer-frame edges survive before one judge poll");
            for (int i = 0; i < 4; i++) check(events.times[i] == 1000000 && events.keys[i] == i % 2 && events.pressed[i] == (i < 2), "Keep poll timestamp/order");
            check(Arrays.equals(f.input.getControllerReplayKeys(), new int[]{0, 1}), "SDL uses common controller replay mask");
            var replay = new ReplayData(); replay.controllerEventKeys = f.input.getControllerReplayKeys(); replay.keylog = f.input.getKeyInputLog();
            replay.shrink(); replay = new Json().fromJson(ReplayData.class, new Json().toJson(replay)); check(replay.validate(), "Replay round trip");
            f.input.beginInputJudge(null, replay.controllerEventKeys, true);
            for (var event : replay.keylog) f.input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            check(f.drain().size == 4, "New replay keeps SDL FIFO transitions");
            f.input.beginInputJudge(null, null, true); f.input.setKeyState(0, true, 2000); f.input.setKeyState(0, false, 2000);
            check(f.drain().size == 0, "Legacy replay remains last-state based");
            f.input.beginInputJudge(new int[]{1}, null, true); f.input.setKeyState(0, true, 3000);
            check(f.drain().size == 0, "Keyboard-only replay does not reinterpret controller input");
        }
    }

    private static void durationAndPov() throws Exception {
        try (var f = new Fixture(new int[]{0, 28}, 10)) {
            f.fake.buttons[0] = true; f.sample(); f.fake.buttons[0] = false; f.sample();
            f.controller.poll(1000); var press = f.drain(); check(press.size == 1 && press.pressed[0], "Duration keeps press");
            f.controller.poll(10999); check(f.drain().size == 0, "Release deferred");
            f.controller.poll(11000); var release = f.drain(); check(release.size == 1 && !release.pressed[0] && release.times[0] == 11000, "Release at existing duration boundary");
            f.config.setDuration(0); f.controller.setConfig(f.config);
            f.fake.buttons[27] = true; f.fake.buttons[28] = true; f.sample(); f.controller.poll(12000); check(f.drain().size == 1, "Physical button/HAT OR");
            f.fake.buttons[27] = false; f.sample(); f.controller.poll(13000); check(f.drain().size == 0, "One source still holds HAT");
            f.fake.buttons[28] = false; f.sample(); f.controller.poll(14000); check(f.drain().size == 1 && !f.input.getKeyState(1), "Final HAT source releases");
            f.fake.buttons[26] = true; f.fake.buttons[27] = true; f.sample();
            check(f.device.getButton(28) && f.device.getButton(29), "Diagonal POV aliases");
        }
    }

    private static void scratchAndFocus() throws Exception {
        for (int mode = -1; mode < 2; mode++) try (var f = new Fixture(new int[]{0, 32, 33, 34}, 0)) {
            f.config.setJKOC(mode < 0); f.config.setAnalogScratch(mode >= 0); f.config.setAnalogScratchMode(Math.max(0, mode));
            f.config.setAnalogScratchThreshold(1); f.controller.setConfig(f.config);
            f.fake.axes[0] = 0.95f; f.fake.axes[3] = -0.95f; f.sample(); f.controller.poll(1000);
            if (mode < 0) check(f.input.getKeyState(1) && f.input.getKeyState(2) && !f.input.getKeyState(3), "JKOC uses axis 0/3 OR only");
            f.drain();
            f.fake.buttons[0] = true; f.sample(); f.controller.poll(2000); f.drain();
            f.controller.setWindowActive(false); f.controller.poll(3000); f.drain();
            f.fake.buttons[0] = false; f.sample(); f.fake.buttons[0] = true; f.sample();
            f.controller.setWindowActive(true); f.controller.poll(4000); check(f.drain().size == 0, "Focus boundary discards pending FIFO and held controls");
            f.fake.buttons[0] = false; Arrays.fill(f.fake.axes, 0); f.sample(); f.controller.poll(5000); f.drain();
            f.fake.buttons[0] = true; f.sample(); f.controller.poll(6000); check(f.input.getKeyState(0), "Fresh press after focus baseline"); f.drain();
            f.fake.connected = false; f.sample(); f.controller.poll(7000); check(!f.input.getKeyState(0), "Disconnect releases held state without duration delay"); f.drain();
            check(f.manager.getCurrentController() == null, "Disconnected controller is not current");
            f.fake.connected = true; f.fake.buttons[0] = false; f.sample(); f.controller.poll(8000); f.drain();
            f.fake.buttons[0] = true; f.sample(); f.controller.poll(9000); check(f.input.getKeyState(0), "Reconnection snapshot baseline");
        }
        try (var f = new Fixture(new int[]{32, 33}, 0)) {
            f.fake.axes[0] = Float.NaN; f.fake.axes[1] = Float.POSITIVE_INFINITY; f.sample();
            check(f.device.getAxis(0) == 0 && f.device.getAxis(1) == 0 && f.device.getAxis(100) == 0, "Finite/missing axis protection");
        }
    }

    private static void lifecycle() throws Exception {
        var fake = new Fake(); var pending = new ArrayList<Runnable>(); var closed = new AtomicInteger();
        var manager = new SdlControllerManager(new Controller[]{fake.controller}, () -> { }, closed::incrementAndGet, pending::add);
        var device = manager.getControllers().first();
        manager.setPollingShutdown(() -> false); manager.dispose(); check(closed.get() == 0, "No native release before polling stops");
        manager.setPollingShutdown(() -> true);
        var active = manager;
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread reader = new Thread(() -> {
            try { device.getButton(0); device.getAxis(0); active.dispose(); }
            catch (Throwable failure) { error.set(failure); }
        }); reader.start(); reader.join(2000);
        check(!reader.isAlive() && error.get() == null && closed.get() == 0, "Input thread reads cache and defers SDL native release");
        for (var runnable : pending) runnable.run(); manager.dispose();
        check(closed.get() == 1, "Native release on owner thread exactly once");
        var failureClosed = new AtomicInteger();
        manager = new SdlControllerManager(new Controller[]{new Fake().controller}, () -> { throw new IllegalStateException("Expected SDL poll failure"); }, failureClosed::incrementAndGet, Runnable::run);
        manager.poll(); check(failureClosed.get() == 1, "Polling failure cleans up"); manager.dispose(); check(failureClosed.get() == 1, "Failure cleanup idempotent");
        var initClosed = new AtomicInteger();
        try { new SdlControllerManager(new Controller[]{null}, () -> { }, initClosed::incrementAndGet, Runnable::run); throw new AssertionError("Bad source accepted"); }
        catch (NullPointerException expected) { check(initClosed.get() == 1, "Partial adapter initialization releases source"); }
    }

    private static void fullInputLifecycle() throws Exception {
        var oldInput = com.badlogic.gdx.Gdx.input; Runnable oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
        com.badlogic.gdx.Gdx.input = (com.badlogic.gdx.Input) Proxy.newProxyInstance(SdlControllerManagerTest.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Input.class},
                (p, m, a) -> m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
        var fake = new Fake(); var closed = new AtomicInteger();
        var manager = new SdlControllerManager(new Controller[]{fake.controller}, () -> { }, closed::incrementAndGet, Runnable::run);
        BMSPlayerInputProcessor input = null;
        try {
            var runner = SdlControllerManager.class.getDeclaredField("pollRunner"); runner.setAccessible(true);
            BMSPlayerInputProcessor.controllerPollRunner = (Runnable) runner.get(manager);
            var config = new bms.player.beatoraja.Config(); config.setControllerBackend(ControllerBackend.SDL);
            var player = new bms.player.beatoraja.PlayerConfig(); player.selectControllerBackend(ControllerBackend.SDL);
            Arrays.fill(player.getMode7().getKeyboardConfig().getKeyAssign(), -1);
            var binding = player.getMode7().getController()[0]; binding.setName("Fake SDL"); binding.setDuration(0); binding.getKeyAssign()[0] = 0;
            input = KeyboardQueueTestSupport.input(config, player, manager.getControllers());
            input.setPlayConfig(player.getMode7()); input.setStartTime(1); input.beginInputJudge(null, null, false);
            check(input.getJudgeEventQueue().available() == 0, "Clean judgment baseline before starting input thread");
            check(Arrays.equals(input.getControllerReplayKeys(), new int[]{0}), "Full input processor selects SDL bank and common replay FIFO");
            fake.buttons[0] = true; manager.poll(); input.startPolling();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (input.getJudgeEventQueue().available() == 0 && System.nanoTime() < deadline) Thread.sleep(1);
            check(input.getJudgeEventQueue().available() == 1 && input.getKeyState(0), "Input polling thread consumes SDL producer FIFO without native calls");
            input.dispose(); check(closed.get() == 1, "Input stop/join owns SDL manager disposal");
        } finally {
            if (input != null) input.dispose(); manager.dispose();
            com.badlogic.gdx.Gdx.input = oldInput; BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
        }
    }

    private static void scratchParity() throws Exception {
        for (int mode = -1; mode < 2; mode++) try (var sdl = new Fixture(new int[]{32, 33, 38, 39}, 0)) {
            var fake = new JInputControllerCompatibilityTest.FakeDevice(0, 4);
            var jinput = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device}, () -> { }, () -> { }, Runnable::run);
            try {
                var input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
                var settings = new ControllerConfig(new int[]{32, 33, 38, 39}, -1, -1); settings.setDuration(0);
                settings.setJKOC(mode < 0); settings.setAnalogScratch(mode >= 0); settings.setAnalogScratchMode(Math.max(0, mode)); settings.setAnalogScratchThreshold(3);
                sdl.controller.setConfig(settings);
                var processor = new BMControllerInputProcessor(input, "raw parity", jinput.getControllers().first(), settings);
                KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{processor});
                processor.setEnable(true); processor.clear(); input.beginInputJudge(null, null, false);
                float[] positions = {0, .01f, .03f, .04f, .04f, .04f, -.01f, -.04f, .99f, -.99f, -.98f, -.96f, -.96f, -.96f, 0, .95f, -.95f, 0};
                for (int i = 0; i < positions.length; i++) {
                    fake.axes[0] = sdl.fake.axes[0] = positions[i];
                    fake.axes[3] = sdl.fake.axes[3] = -positions[i];
                    jinput.poll(); sdl.sample();
                    long time = 1000000 + i * 1000L; processor.poll(time); sdl.controller.poll(time);
                    var expected = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(expected); var actual = sdl.drain();
                    check(expected.size == actual.size, "Scratch event count matches JInput for mode " + mode);
                    for (int e = 0; e < actual.size; e++) check(actual.keys[e] == expected.keys[e] && actual.times[e] == expected.times[e]
                            && actual.pressed[e] == expected.pressed[e], "Scratch direction/wrap/stop and timing parity");
                }
            } finally { jinput.dispose(); }
        }
    }

    private static void overflow() throws Exception {
        var old = com.badlogic.gdx.Gdx.app; int[] exits = {0};
        try (var f = new Fixture(new int[]{0}, 0)) {
            com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application) Proxy.newProxyInstance(SdlControllerManagerTest.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Application.class},
                    (p, m, a) -> { if (m.getName().equals("exit")) exits[0]++; return null; });
            for (int i = 0; i < 130; i++) { f.fake.buttons[0] = i % 2 == 0; f.sample(); }
            check(((QueuedController) f.device).events().isOverflowed() && exits[0] == 1, "Overflow fails closed");
            f.controller.poll(1000); check(f.drain().size == 0, "No partial judgment after overflow");
        } finally { com.badlogic.gdx.Gdx.app = old; }
    }

    private static final class Fake {
        final boolean[] buttons = new boolean[32]; final float[] axes = new float[4]; boolean connected = true;
        Runnable onAxis = () -> { };
        final Thread owner = Thread.currentThread();
        final ControllerMapping mapping = new ControllerMapping(0, 1, 2, 3, 0, 1, 2, 3, 8, 9, 4, 5, 6, 7, 10, 11, 26, 24, 27, 25) { };
        final Controller controller = (Controller) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Controller.class}, (p, m, args) -> {
            if (Thread.currentThread() != owner) throw new AssertionError("Native source read off owner thread");
            return switch (m.getName()) {
                case "getName", "getUniqueId" -> "Fake SDL";
                case "getAxisCount" -> axes.length;
                case "getMaxButtonIndex" -> 31;
                case "getMinButtonIndex" -> 0;
                case "getButton" -> buttons[(int) args[0]];
                case "getAxis" -> { onAxis.run(); yield axes[(int) args[0]]; }
                case "isConnected" -> connected;
                case "getMapping" -> mapping;
                default -> null;
            };
        });
    }

    private static final class Fixture implements AutoCloseable {
        final Fake fake = new Fake(); final SdlControllerManager manager;
        final Controller device; final BMSPlayerInputProcessor input; final BMControllerInputProcessor controller; final ControllerConfig config;
        Fixture(int[] keys, int duration) throws Exception {
            manager = new SdlControllerManager(new Controller[]{fake.controller}, () -> { }, () -> { }, r -> { throw new AssertionError("No listener dispatch expected"); });
            device = manager.getControllers().first(); input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "lastAnalogValue", new float[256]);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "analogLastResetTime", new long[256]);
            config = new ControllerConfig(keys, -1, -1); config.setDuration(duration);
            controller = new BMControllerInputProcessor(input, "Fake SDL", device, config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{controller});
            controller.setEnable(true); controller.clear(); input.beginInputJudge(null, null, false);
        }
        void sample() { manager.poll(); }
        JudgeEventQueue.Batch drain() { var result = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(result); return result; }
        public void close() { manager.dispose(); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
