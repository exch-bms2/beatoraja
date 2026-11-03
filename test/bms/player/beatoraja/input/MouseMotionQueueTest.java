package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayModeConfig.MouseScratchConfig;
import bms.player.beatoraja.ReplayData;

/** Real scratch converters and judgment/replay streams, using sampled positions rather than hardware. */
public final class MouseMotionQueueTest {
    public static void main(String[] args) throws Exception {
        testQueue();
        testConcurrentQueue();
        for (int mode : new int[]{MouseScratchConfig.MOUSE_SCRATCH_VER_1, MouseScratchConfig.MOUSE_SCRATCH_VER_2}) {
            testReversalAndReplay(mode);
            testStopsAndAxisIndependence(mode);
            testOrdinaryParity(mode);
        }
        testReplayVersions();
        testResetAndOverflow();
        System.out.println("MouseMotionQueueTest passed (position FIFO, v1/v2, timing, lifecycle and replay)");
    }

    private static void testQueue() {
        var q = new MouseMotionQueue(3);
        var batch = new MouseMotionQueue.Batch();
        q.add(0, 0);
        q.add(10, 7);
        q.add(-10, -7);
        q.drain(batch);
        check(batch.size == 2 && batch.dx[0] == 10 && batch.dy[0] == 7
                && batch.dx[1] == -10 && batch.dy[1] == -7, "Retain order and paired axes, do not sum reversals");
        check(q.isCurrent(batch), "Batch valid before reset");
        q.add(3, 4);
        check(batch.size == 2 && batch.dx[0] == 10, "Frozen batch unaffected by new publication");
        q.clear();
        check(!q.isCurrent(batch), "Clock/configuration reset invalidates detached batches");
        q.drain(batch);
        check(batch.size == 0, "No stale motion after clear");
        for (int i = 0; i < 4; i++) q.add(1, 1);
        q.drain(batch);
        check(q.isOverflowed() && batch.size == 0 && !q.isCurrent(batch), "Overflow fails closed");
        q.clear();
        check(q.isOverflowed(), "Reset cannot repair a stream after overflow");
    }

    private static void testConcurrentQueue() throws Exception {
        var q = new MouseMotionQueue();
        var error = new AtomicReference<Throwable>();
        Thread producer = new Thread(() -> { for (int i = 1; i <= 2000; i++) q.add(i, -i); });
        Thread consumer = new Thread(() -> {
            try {
                int next = 1;
                var batch = new MouseMotionQueue.Batch();
                long deadline = System.nanoTime() + 3_000_000_000L;
                while (next <= 2000 && System.nanoTime() < deadline) {
                    q.drain(batch);
                    for (int i = 0; i < batch.size; i++, next++) check(batch.dx[i] == next && batch.dy[i] == -next,
                            "Concurrent motion order and X/Y pairing");
                    if (batch.size == 0) Thread.yield();
                }
                check(next == 2001, "Every record delivered exactly once");
            } catch (Throwable e) { error.set(e); }
        });
        producer.start(); consumer.start();
        producer.join(4000); consumer.join(4000);
        check(!producer.isAlive() && !consumer.isAlive() && error.get() == null, "Concurrent queue failure: " + error.get());
    }

    static KeyboardConfig config(int mode) {
        var config = new KeyboardConfig(Mode.BEAT_7K, false);
        var mouse = config.getMouseScratchConfig();
        mouse.setMouseScratchEnabled(true);
        mouse.setKeyAssign(new int[]{0, 1, 2, 3});
        mouse.setMouseScratchMode(mode);
        mouse.setMouseScratchDistance(12);
        mouse.setMouseScratchTimeThreshold(150);
        return config;
    }

    private static void testReversalAndReplay(int mode) throws Exception {
        try (var f = new Fixture(mode)) {
            f.motion.add(20, 20);
            f.motion.add(-20, -20);
            f.poll(1_000_000);
            var batch = f.drain();
            check(batch.size == 6, "Each axis preserves press, release and reverse press in one poll");
            int[] keys = {0, 2, 0, 1, 2, 3};
            boolean[] down = {true, true, false, true, false, true};
            for (int i = 0; i < batch.size; i++) check(batch.keys[i] == keys[i] && batch.pressed[i] == down[i]
                    && batch.times[i] == 1_000_000, "Preserve assignments, order and original poll microtime");
            check(!f.input.getKeyState(0) && f.input.getKeyState(1), "Latest state reflects last motion, not zero sum");
            f.poll(1_000_000);
            check(f.drain().size == 0, "No duplicate movement or elapsed time in empty same-time poll");
            var replay = new ReplayData();
            replay.keyboardEventKeys = f.input.getKeyboardReplayKeys();
            replay.controllerEventKeys = f.input.getControllerReplayKeys();
            replay.mouseScratchEventKeys = f.input.getMouseScratchReplayKeys();
            replay.keylog = f.input.getKeyInputLog();
            replay.shrink();
            var json = new Json();
            var copy = json.fromJson(ReplayData.class, json.toJson(replay));
            check(copy.validate() && Arrays.equals(copy.mouseScratchEventKeys, new int[]{0,1,2,3}), "Compressed replay metadata round trip");
            f.input.beginInputJudge(copy.keyboardEventKeys, copy.controllerEventKeys, copy.mouseScratchEventKeys, true);
            for (var event : copy.keylog) f.input.setKeyState(event.getKeycode(), event.isPressed(), event.getTime());
            var restored = f.drain();
            check(restored.size == batch.size, "New replay retains all mouse edges");
            for (int i = 0; i < batch.size; i++) check(restored.keys[i] == batch.keys[i]
                    && restored.pressed[i] == batch.pressed[i] && restored.times[i] == batch.times[i], "Exact replay order/times");
        }
    }

    private static void testStopsAndAxisIndependence(int mode) throws Exception {
        try (var f = new Fixture(mode)) {
            f.motion.add(20, 20); f.poll(1_000_000); f.drain();
            f.motion.add(20, 0); f.motion.add(20, 0);
            f.poll(1_150_000);
            check(f.input.getKeyState(0), "Moving X still active");
            boolean v1 = mode == MouseScratchConfig.MOUSE_SCRATCH_VER_1;
            check(f.input.getKeyState(2) == v1, "Idle Y updated once, retaining v1/v2 stop boundary");
            var boundary = f.drain();
            check(boundary.size == (v1 ? 0 : 1), "No extra idle-axis update per queued X record");
            f.poll(1_151_000);
            check(!f.input.getKeyState(2), "Idle Y eventually stops without new render samples");
        }
        try (var f = new Fixture(mode)) {
            f.motion.add(20, 20); f.poll(1_000_000); f.drain();
            f.motion.add(20, 0); f.motion.add(0, 20);
            f.poll(1_200_000);
            check(f.drain().size == 0 && f.input.getKeyState(0) && f.input.getKeyState(2),
                    "Do not synthesize timeout release before an axis's later motion in the same poll");
        }
    }

    private static void testOrdinaryParity(int mode) throws Exception {
        check(ordinaryTrace(mode, true).equals(ordinaryTrace(mode, false)), "Single-sample states/times/analog values match legacy center-difference path");
        for (int sampler : new int[]{1,2})
            check(ordinaryTrace(mode,true,sampler).equals(ordinaryTrace(mode,false)),
                    "Wayland captured/uncaptured endpoints preserve algorithm, microsecond event times and replay trace");
        try (var f = new Fixture(mode)) {
            f.config.getMouseScratchConfig().setKeyAssign(new int[]{2, 2, 0, 1});
            f.input.getKeyBoardInputProcesseor().setConfig(f.config);
            f.input.beginInputJudge(null, null, false);
            f.motion.add(0, 20); f.poll(1_000_000);
            var events = f.drain();
            check(events.size == 1 && events.keys[0] == 0, "Duplicate axis mapping retains first assignment");
        }
    }

    private static java.util.List<String> ordinaryTrace(int mode, boolean queued) throws Exception {
        return ordinaryTrace(mode,queued,0);
    }

    private static java.util.List<String> ordinaryTrace(int mode, boolean queued, int sampler) throws Exception {
        var previousInput = Gdx.input;
        var previousGraphics = Gdx.graphics;
        var result = new java.util.ArrayList<String>();
        try (var f = new Fixture(mode)) {
            int[] position = {160, 120};
            var source = new MouseScratchPosition();
            source.sample(sampler==1,false,160,120,320,240);
            var legacy = (com.badlogic.gdx.Input) java.lang.reflect.Proxy.newProxyInstance(MouseMotionQueueTest.class.getClassLoader(),
                    new Class<?>[]{com.badlogic.gdx.Input.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getX" -> position[0];
                        case "getY" -> position[1];
                        case "setCursorPosition" -> { position[0] = (int) args[0]; position[1] = (int) args[1]; yield null; }
                        default -> method.getReturnType() == boolean.class ? false : null;
                    });
            Gdx.graphics = (com.badlogic.gdx.Graphics) java.lang.reflect.Proxy.newProxyInstance(MouseMotionQueueTest.class.getClassLoader(),
                    new Class<?>[]{com.badlogic.gdx.Graphics.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth" -> 320;
                        case "getHeight" -> 240;
                        default -> null;
                    });
            var analog = BMSPlayerInputProcessor.class.getDeclaredField("currentAnalogValue");
            analog.setAccessible(true);
            KeyboardQueueTestSupport.set(f.input, BMSPlayerInputProcessor.class, "analogScroll", true);
            long time = 1_000_000;
            for (int[] movement : new int[][]{{0,0},{8,8},{8,8},{0,0},{-5,0},{-15,-20},{0,0},{20,20},{0,0},{0,0}}) {
                if (queued) {
                    Gdx.input = f.polling;
                    if (sampler==0) f.motion.add(movement[0],movement[1]);
                    else {
                        position[0] = (sampler==1 ? 160 : position[0])+movement[0];
                        position[1] = (sampler==1 ? 120 : position[1])+movement[1];
                        source.sample(sampler==1,true,position[0],position[1],320,240);
                        if (source.delivered) f.motion.add(source.dx,source.dy);
                    }
                }
                else { Gdx.input = legacy; position[0] += movement[0]; position[1] += movement[1]; }
                f.scratch.poll(time);
                result.add(f.input.getKeyState(0) + ":" + f.input.getKeyState(1) + ":" + f.input.getKeyState(2)
                        + ":" + f.input.getKeyState(3) + ":" + Arrays.toString(Arrays.copyOf((float[]) analog.get(f.input), 4)));
                time += 100_000;
            }
            for (var event : f.input.getKeyInputLog()) result.add(event.getKeycode() + ":" + event.getTime() + ":" + event.isPressed());
        } finally { Gdx.input = previousInput; Gdx.graphics = previousGraphics; }
        return result;
    }

    private static void testReplayVersions() throws Exception {
        try (var f = new Fixture(0)) {
            f.input.beginInputJudge(null, null, true);
            f.input.setKeyState(0, true, 1000); f.input.setKeyState(0, false, 1000);
            check(f.drain().size == 0, "Legacy replay remains last-state based");
            f.input.beginInputJudge(new int[]{0}, new int[]{1}, true);
            f.input.setKeyState(2, true, 2000); f.input.setKeyState(2, false, 2000);
            check(f.drain().size == 0 && !f.input.getJudgeEventQueue().isQueued(2), "Keyboard/controller-era replay keeps mouse on legacy path");
            f.input.beginInputJudge(new int[]{0}, new int[]{1}, new int[]{2}, true);
            for (int i = 0; i < 3; i++) f.input.setKeyState(i, true, 3000);
            check(f.drain().size == 3, "Mixed devices share one ordered replay FIFO");
            var invalid = new ReplayData();
            invalid.mouseScratchEventKeys = new int[257];
            check(!invalid.validate(), "Bound new replay metadata");
        }
    }

    private static void testResetAndOverflow() throws Exception {
        var previousApp = Gdx.app;
        try (var f = new Fixture(0)) {
            f.motion.add(20, 0); f.motion.add(-20, 0);
            f.input.setStartTime(10);
            f.poll(0);
            check(f.drain().size == 0, "Clock reset discards previous scene's pending movement");
            f.motion.add(20, 0);
            f.scratch.setConfig(f.config);
            f.poll(1000);
            check(f.drain().size == 0, "Configuration reset discards pending movement");
            int[] exits = {0};
            Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(MouseMotionQueueTest.class.getClassLoader(),
                    new Class<?>[]{com.badlogic.gdx.Application.class}, (proxy, method, args) -> {
                        if (method.getName().equals("exit")) exits[0]++; return null;
                    });
            for (int i = 0; i <= MouseMotionQueue.CAPACITY; i++) f.motion.add(1, 0);
            f.poll(2000); f.poll(3000);
            check(exits[0] == 1 && f.drain().size == 0, "Overflow stops once without partial gameplay events");
        } finally { Gdx.app = previousApp; }
    }

    private static final class Fixture implements AutoCloseable {
        final com.badlogic.gdx.Input previous = Gdx.input;
        final PollingLwjgl3Input polling;
        final PollingLwjgl3Input.MouseDeltas motion = new PollingLwjgl3Input.MouseDeltas();
        final BMSPlayerInputProcessor input;
        final KeyboardConfig config;
        final MouseScratchInput scratch;
        Fixture(int mode) throws Exception {
            polling = KeyboardQueueTestSupport.allocate(PollingLwjgl3Input.class);
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "scratch", motion);
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "keys", new java.util.concurrent.atomic.AtomicIntegerArray(256));
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "keyboardEvents", new KeyboardEventQueue());
            Gdx.input = polling;
            config = config(mode);
            input = KeyboardQueueTestSupport.input(config);
            scratch = input.getKeyBoardInputProcesseor().getMouseScratchInput();
            input.beginInputJudge(null, null, false);
        }
        void poll(long time) { Gdx.input = polling; scratch.poll(time); }
        JudgeEventQueue.Batch drain() {
            var batch = new JudgeEventQueue.Batch(); input.getJudgeEventQueue().drain(batch); return batch;
        }
        @Override public void close() { Gdx.input = previous; }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
