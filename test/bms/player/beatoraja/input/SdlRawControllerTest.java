package bms.player.beatoraja.input;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayModeConfig.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Synthetic raw-native events through the production mapping, common FIFO, BM and replay paths. */
public final class SdlRawControllerTest {
    private static boolean async;
    public static void main(String[] args) throws Exception {
        SdlHotplugTest.main(args);
        configuration();
        for (boolean acquisition : new boolean[]{false, true}) {
            async = acquisition;
            shortTapReplay(); durationPovMapping(); scratchParity(); lifecycle();
        }
        System.out.println("SdlRawControllerTest passed (bank migration, same-name inheritance, mapping/POV OR, FIFO/time/replay, JKOC/scratch v1/v2, lifecycle)");
    }
    private static void configuration() throws Exception {
        var json = new Json();
        var config = json.fromJson(Config.class, "{controllerBackend:SDL_RAW}");
        check(config.getControllerBackend() == ControllerBackend.SDL_RAW, "Config enum persistence");
        check(json.fromJson(Config.class, "{controllerBackend:SDL_RAW_ASYNC}").getControllerBackend()
                == ControllerBackend.SDL_RAW_ASYNC, "Async Config enum persistence");
        String manager = Controllers.preferredManager;
        try {
            ControllerBackend.SDL_RAW.configure(); check(Controllers.preferredManager.equals(SdlRawControllerManager.class.getName()), "Raw backend factory");
            ControllerBackend.SDL_RAW_ASYNC.configure(); check(Controllers.preferredManager.equals(SdlRawAsyncControllerManager.class.getName()), "Async raw backend factory");
        }
        finally { Controllers.preferredManager = manager; }
        var play = new PlayModeConfig(Mode.BEAT_7K);
        Arrays.fill(play.getKeyboardConfig().getKeyAssign(), -1);
        var original = play.getController()[0];
        original.setName("Raw device"); original.getKeyAssign()[0] = 16;
        original.setStart(8); original.setSelect(9); original.setDuration(7);
        original.setJKOC(true); original.setAnalogScratch(true); original.setAnalogScratchThreshold(17);
        play.selectControllerBackend(ControllerBackend.SDL_RAW, Mode.BEAT_7K);
        play.validate(Mode.BEAT_7K);
        var raw = play.getController()[0];
        check(raw != original && raw.getKeyAssign() != original.getKeyAssign() && raw.getName().equals("Raw device"), "Deep-copy named JInput bank");
        check(raw.getKeyAssign()[0] == 32 && raw.getStart() == 8 && raw.getSelect() == 9
                && raw.getDuration() == 7 && raw.getJKOC() && raw.isAnalogScratch() && raw.getAnalogScratchThreshold() == 17,
                "Legacy axis migration applied once to inherited bank; settings retained");
        raw.getKeyAssign()[0] = 2;
        raw.setSdlRawButtonMap(new int[]{3, 1, -1}); raw.setSdlRawAxisMap(new int[]{3, 0});
        raw.setSdlRawAxisInverted(new boolean[]{true});
        var shared = play.getController();
        play.selectControllerBackend(ControllerBackend.SDL_RAW_ASYNC, Mode.BEAT_7K);
        check(play.getController() == shared && play.getController()[0] == raw, "Acquisition modes share the exact binding bank");
        play.setController(shared.clone());
        play.selectControllerBackend(ControllerBackend.SDL_RAW, Mode.BEAT_7K);
        check(play.getController() != shared && play.getController()[0] == raw, "Async bank setter updates the synchronous bank too");
        var saved = json.toJson(play);
        var copy = json.fromJson(PlayModeConfig.class, saved);
        copy.selectControllerBackend(ControllerBackend.SDL_RAW, Mode.BEAT_7K); copy.validate(Mode.BEAT_7K);
        check(copy.getController()[0].getKeyAssign()[0] == 2 && copy.getController()[0].getSdlRawAxisMap()[0] == 3, "Raw bank/profile round trip");
        copy.selectControllerBackend(ControllerBackend.JINPUT, Mode.BEAT_7K);
        check(copy.getController()[0].getKeyAssign()[0] == 32 && copy.getController()[0].getSdlRawButtonMap() == null, "JInput bank not edited through raw bank");
        copy.selectControllerBackend(ControllerBackend.SDL, Mode.BEAT_7K);
        check(copy.getController()[0].getName().isEmpty() && copy.getController()[0].getStart() == -1, "Mapped SDL still has its own empty bank");
        var empty = new PlayModeConfig(Mode.BEAT_14K);
        empty.selectControllerBackend(ControllerBackend.SDL_RAW, Mode.BEAT_14K); empty.validate(Mode.BEAT_14K);
        check(empty.getController().length == 2 && empty.getController()[0].getStart() == -1, "Unconfigured devices never inherit arbitrary controls");
        var damaged = json.fromJson(PlayModeConfig.class, "{controller:null,sdlRawController:[null],version:1}");
        damaged.selectControllerBackend(ControllerBackend.SDL_RAW, Mode.BEAT_14K); damaged.validate(Mode.BEAT_14K);
        check(damaged.getController().length == 2, "Malformed raw bank repaired");
        var bad = new ControllerConfig(); bad.setSdlRawButtonMap(new int[]{Integer.MAX_VALUE, -3}); bad.setSdlRawAxisMap(new int[]{900});
        var mapping = new SdlRawMapping(bad, 32, 4);
        check(mapping.buttons[0] == -1 && mapping.buttons[1] == -1 && mapping.axes[0] == -1, "Invalid physical indices disabled");
        bad.setSdlRawButtonMap(new int[]{255}); bad.setSdlRawAxisMap(new int[]{255});
        mapping = new SdlRawMapping(bad, 256, 256);
        check(mapping.buttons[0] == 255 && mapping.axes[0] == 255 && mapping.buttonTargets[255] == 1,
                "High physical indices can map into the legacy namespace");
        var root = java.nio.file.Files.createTempDirectory("sdl-raw-player-");
        try {
            var player = new PlayerConfig(); player.setId("test");
            player.getMode7().getController()[0].setName("Raw device");
            Arrays.fill(player.getMode7().getKeyboardConfig().getKeyAssign(), -1);
            player.getMode7().getController()[0].getKeyAssign()[0] = 3;
            player.selectControllerBackend(ControllerBackend.SDL_RAW_ASYNC); player.validate();
            player.getMode7().getController()[0].getKeyAssign()[0] = 5;
            java.nio.file.Files.createDirectory(root.resolve("test")); PlayerConfig.write(root.toString(), player);
            var disk = PlayerConfig.readPlayerConfig(root.toString(), "test", ControllerBackend.SDL_RAW);
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 5
                    && disk.getMode14().getController().length == 2 && disk.getMode24double().getController().length == 2,
                    "Player disk read retains raw bank and mode-specific DP lengths");
            disk = PlayerConfig.readPlayerConfig(root.toString(), "test", ControllerBackend.SDL_RAW_ASYNC);
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 5, "Async disk read uses the shared bank, not another migration");
            disk = PlayerConfig.readPlayerConfig(root.toString(), "test");
            check(disk.getMode7().getController()[0].getKeyAssign()[0] == 3, "Existing disk API still reads unchanged JInput bank");
        } finally {
            try (var paths = java.nio.file.Files.walk(root)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(path);
            }
        }
    }
    private static void shortTapReplay() throws Exception {
        try (var f = new Fixture(new int[]{0, 1}, 0)) {
            f.fake.press(1, true); f.fake.press(0, true); f.fake.press(1, false); f.fake.press(0, false);
            f.poll(1_234_567);
            var batch = f.drain(); check(batch.size == 4, "Entire tap within one producer update survives");
            for (int i = 0; i < 4; i++) check(batch.keys[i] == (i % 2 == 0 ? 1 : 0)
                    && batch.pressed[i] == (i < 2) && batch.times[i] == 1_234_567, "Native event FIFO order and existing poll timestamp");
            var replay = new ReplayData(); replay.controllerEventKeys = f.input.getControllerReplayKeys(); replay.keylog = f.input.getKeyInputLog();
            replay.shrink(); var json = new Json(); replay = json.fromJson(ReplayData.class, json.toJson(replay));
            check(replay.validate(), "Existing compressed replay");
            f.input.beginInputJudge(null, replay.controllerEventKeys, true);
            for (var e : replay.keylog) f.input.setKeyState(e.getKeycode(), e.isPressed(), e.getTime());
            var restored = f.drain(); check(restored.size == batch.size, "New replay event count");
            for (int i = 0; i < batch.size; i++) check(restored.keys[i] == batch.keys[i] && restored.times[i] == batch.times[i]
                    && restored.pressed[i] == batch.pressed[i], "Replay parity");
            f.input.beginInputJudge(null, null, true); f.input.setKeyState(0, true, 2000);
            check(f.drain().size == 0, "Metadata-free replay remains legacy");
        }
    }
    private static void durationPovMapping() throws Exception {
        try (var f = new Fixture(new int[]{0, 28, 29}, 10)) {
            f.fake.press(0, true); f.fake.press(0, false); f.poll(1000);
            check(f.drain().size == 1 && f.input.getKeyState(0), "Duration holds short-tap release");
            f.controller.poll(11000); var release = f.drain();
            check(release.size == 1 && !release.pressed[0] && release.times[0] == 11000, "Duration acceptance time unchanged");
            f.config.setDuration(0); f.controller.setConfig(f.config);
            f.fake.press(28, true); f.fake.pov(8); f.fake.press(28, false); f.fake.pov(0); f.poll(20000);
            check(f.drain().size == 2, "Physical 28 / first POV are ORed without phantom edges");
            f.fake.pov(9); f.fake.pov(0); f.poll(21000);
            check(f.drain().size == 4, "POV diagonal preserves both short taps");
            int[] map = new int[32]; for (int i = 0; i < map.length; i++) map[i] = i;
            map[0] = 3; map[3] = 0; f.config.setSdlRawButtonMap(map);
            f.config.setSdlRawAxisMap(new int[]{3, 1, 2, 0}); f.config.setSdlRawAxisInverted(new boolean[]{true});
            f.controller.setConfig(f.config);
            f.fake.press(3, true); f.fake.axes[3] = -.75f; f.poll(22000);
            check(f.input.getKeyState(0) && f.raw.getButton(0) && !f.raw.getButton(3) && f.raw.getAxis(0) == .75f,
                    "Precomputed button/axis order and sign conversion");
            f.fake.connected = false; f.poll(23000);
            check(!f.input.getKeyState(0), "Disconnect releases state without old duration delay");
            f.drain(); f.fake.connected = true; f.fake.buttons[3] = false; f.poll(24000);
            f.fake.press(3, true); f.poll(25000); check(f.input.getKeyState(0), "Reconnect snapshot then fresh edge");
            f.controller.setWindowActive(false); f.fake.press(3, false); f.fake.press(3, true); f.produce();
            f.controller.setWindowActive(true); f.controller.poll(26000); f.drain();
            f.fake.press(3, false); f.poll(27000); f.drain(); f.fake.press(3, true); f.poll(28000);
            check(f.input.getKeyState(0), "Focus return clears old FIFO; fresh input still works");
            f.fake.press(3, false); f.poll(29000); f.drain();
            f.controller.setWindowActive(false);
            f.fake.press(3, true); f.fake.press(3, false);
            // The input thread can resume before the owner thread drains SDL's queue.
            f.controller.setWindowActive(true); f.controller.poll(30000); f.drain();
            f.poll(31000); check(f.drain().size == 0 && !f.input.getKeyState(0), "Undrained native tap before regain is discarded");
            f.fake.press(3, true); f.poll(32000);
            check(f.drain().size == 1 && f.input.getKeyState(0), "Fresh input after producer baseline survives");
        }
    }
    private static void scratchParity() throws Exception {
        for (int mode = -1; mode < 2; mode++) try (var f = new Fixture(new int[]{32, 33, 38, 39}, 0)) {
            var fake = new JInputControllerCompatibilityTest.FakeDevice(0, 4);
            var jinput = new JInputControllerManager(new net.java.games.input.Controller[]{fake.device}, () -> { }, () -> { }, Runnable::run);
            try {
                var target = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
                var settings = new ControllerConfig(new int[]{32, 33, 38, 39}, -1, -1); settings.setDuration(0);
                settings.setJKOC(mode < 0); settings.setAnalogScratch(mode >= 0); settings.setAnalogScratchMode(Math.max(0, mode)); settings.setAnalogScratchThreshold(3);
                f.controller.setConfig(settings);
                var expectedInput = new BMControllerInputProcessor(target, "reference", jinput.getControllers().first(), settings);
                KeyboardQueueTestSupport.set(target, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{expectedInput});
                expectedInput.setEnable(true); expectedInput.clear(); target.beginInputJudge(null, null, false);
                float[] positions = {0, .01f, .03f, .04f, .04f, .04f, -.01f, -.04f, .99f, -.99f, -.98f, -.96f, 0, .95f, -.95f, 0};
                for (int i = 0; i < positions.length; i++) {
                    fake.axes[0] = f.fake.axes[0] = positions[i]; fake.axes[3] = f.fake.axes[3] = -positions[i];
                    jinput.poll(); long time = 1_000_000 + i * 1000L;
                    expectedInput.poll(time); f.poll(time);
                    var expected = new JudgeEventQueue.Batch(); target.getJudgeEventQueue().drain(expected);
                    var actual = f.drain(); check(expected.size == actual.size, "JKOC/scratch v1/v2 event count parity");
                    for (int e = 0; e < actual.size; e++) check(actual.keys[e] == expected.keys[e] && actual.times[e] == expected.times[e]
                            && actual.pressed[e] == expected.pressed[e], "Direction/wrap/stop/timing parity");
                }
            } finally { jinput.dispose(); }
        }
    }
    private static void lifecycle() throws Exception {
        var fake = new Fake(); var pending = new ArrayList<Runnable>();
        var manager = new SdlRawControllerManager(fake, pending::add);
        manager.setPollingShutdown(() -> false); manager.dispose(); check(fake.closed == 0, "No native close before polling stops");
        manager.setPollingShutdown(() -> true);
        var active = manager;
        var failure = new AtomicReference<Throwable>();
        Thread t = new Thread(() -> { try { active.getControllers().first().getButton(0); active.dispose(); } catch (Throwable e) { failure.set(e); } });
        t.start(); t.join(3000); check(!t.isAlive() && failure.get() == null && fake.closed == 0, "Reader only accesses cache; release deferred to owner");
        for (var runnable : pending) runnable.run(); manager.dispose(); check(fake.closed == 1, "Close exactly once on owner");
        fake = new Fake(); manager = new SdlRawControllerManager(fake, Runnable::run);
        fake.fail = true; manager.poll(); manager.dispose(); check(fake.closed == 1, "Poll failure closes source once");
        var old = Gdx.app; int[] exits = {0};
        try (var f = new Fixture(new int[]{0}, 0)) {
            Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(SdlRawControllerTest.class.getClassLoader(),
                    new Class<?>[]{com.badlogic.gdx.Application.class}, (p, m, a) -> { if (m.getName().equals("exit")) exits[0]++; return null; });
            for (int i = 0; i < 140; i++) f.fake.press(0, i % 2 == 0);
            f.produce(); check(exits[0] == 1 && f.raw.events().isOverflowed(), "Common FIFO overflow fails closed");
        } finally { Gdx.app = old; }
    }
    static class Fake implements SdlRawSource {
        final Thread owner = Thread.currentThread();
        final boolean async;
        Fake() { this(false); }
        Fake(boolean async) { this.async = async; }
        final boolean[] buttons = new boolean[32]; final float[] axes = new float[4];
        final ArrayDeque<Event> pending = new ArrayDeque<>();
        int hat, closed; boolean connected = true, fail;
        public Device[] devices() { return new Device[]{new Device(5, "Raw device", 32, 4, 1)}; }
        public void update() { check(async || Thread.currentThread() == owner, "Native calls stay on producer thread"); if (fail) throw new IllegalStateException("Expected raw poll failure"); }
        void press(int button, boolean down) { buttons[button] = down; add(Event.BUTTON, button, down ? 1 : 0); }
        void pov(int value) { hat = value; add(Event.HAT, 0, value); }
        private void add(int type, int index, float value) { var e = new Event(); e.device = 5; e.type = type; e.index = index; e.value = value; pending.add(e); }
        public boolean next(Event target) { var e = pending.pollFirst(); if (e == null) return false; target.device = e.device; target.type = e.type; target.index = e.index; target.value = e.value; return true; }
        public boolean connected(int slot) { return connected; }
        public boolean button(int slot, int button) { return buttons[button]; }
        public float axis(int slot, int axis) { return axes[axis]; }
        public int hat(int slot) { return hat; }
        public void close() { check(Thread.currentThread() == owner, "Native close stays on owner"); closed++; }
    }
    private static final class Fixture implements AutoCloseable {
        final Fake fake = new Fake(async);
        final SdlRawControllerManager manager = new SdlRawControllerManager(fake, Runnable::run, async);
        final java.util.concurrent.ExecutorService worker = async ? java.util.concurrent.Executors.newSingleThreadExecutor() : null;
        final SdlRawControllerManager.RawController raw = (SdlRawControllerManager.RawController) manager.getControllers().first();
        final ControllerConfig config;
        final BMSPlayerInputProcessor input;
        final BMControllerInputProcessor controller;
        Fixture(int[] keys, int duration) throws Exception {
            input = KeyboardQueueTestSupport.input(new KeyboardConfig(Mode.BEAT_7K, false));
            config = new ControllerConfig(keys, -1, -1); config.setDuration(duration);
            controller = new BMControllerInputProcessor(input, "Raw device", raw, config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "bminput", new BMControllerInputProcessor[]{controller});
            controller.setEnable(true); controller.clear(); input.beginInputJudge(null, null, false);
        }
        void runInput(Runnable task) throws Exception {
            if (worker == null) task.run();
            else worker.submit(task).get(3, java.util.concurrent.TimeUnit.SECONDS);
        }
        void produce() throws Exception { runInput(manager::poll); }
        void poll(long time) throws Exception { runInput(() -> { manager.poll(); controller.poll(time); }); }
        JudgeEventQueue.Batch drain() { var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch); return batch; }
        public void close() throws Exception {
            if (worker != null) {
                worker.shutdown();
                check(worker.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS), "Async fixture worker stopped before native release");
            }
            manager.dispose();
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
