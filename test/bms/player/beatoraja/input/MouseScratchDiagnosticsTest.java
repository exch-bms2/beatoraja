package bms.player.beatoraja.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.badlogic.gdx.Gdx;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayModeConfig.MouseScratchConfig;

/** Synthetic motion/times; no native input, display, user files or audio. */
public final class MouseScratchDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        testCallbackCancellationAndWarp();
        testBetweenSampleCancellation();
        testGeometryAndBoundaries();
        testOverflowAliasingAndReset();
        testAlgorithmParity();
        System.out.println("MouseScratchDiagnosticsTest passed (synthetic motion, v1/v2 diagnostic ON/OFF parity)");
    }

    private static void sample(MouseScratchDiagnostics d, int dx, int dy, long time) {
        d.sample(true, true, 160 + dx, 120 + dy, 320, 240, 320, 240, 640, 480, time);
        d.warp(160, 120);
    }

    private static long count(MouseScratchDiagnostics d, MouseScratchDiagnostics.Counter c) {
        return d.snapshot().counts().get(c);
    }

    private static void testCallbackCancellationAndWarp() {
        var d = new MouseScratchDiagnostics();
        sample(d, 0, 0, 0);
        d.cursor(160, 120, 100);
        d.cursor(170, 120, 1000);
        d.cursor(160, 120, 2000);
        sample(d, 0, 0, 1_000_000);
        var s = d.snapshot();
        check(count(d, MouseScratchDiagnostics.Counter.WARP_TARGET_MATCHES) == 1, "Separate target-matching warp candidate");
        check(s.x().callbackPath() == 20 && s.x().callbackCancellation() == 20
                && s.x().callbackReversals() == 1, "Observe a round trip invisible to final-position sampling");
        check(s.x().sampledPath() == 0, "Callback observation does not inject motion");
        d.cursor(160, 120, 1_000_100);
        d.cursor(160.25, 120, 1_001_000);
        d.cursor(160.5, 120, 1_002_000);
        d.cursor(160.75, 120, 1_003_000);
        sample(d, 0, 0, 2_000_000);
        check(d.snapshot().x().integerZeroSteps() == 3 && count(d, MouseScratchDiagnostics.Counter.FRACTIONAL_CALLBACKS) == 3,
                "Sub-logical-unit integer truncation candidates");
        check(d.snapshot().timings().get("callbackToSample").maxUs() == 998, "Arrival-to-final-sample timing");
        d.cursor(Double.NaN, 120, 3_000_000);
        check(count(d, MouseScratchDiagnostics.Counter.INVALID_CALLBACKS) == 1, "Invalid diagnostic coordinates are ignored");
    }

    private static void testBetweenSampleCancellation() {
        var d = new MouseScratchDiagnostics();
        var actual = new PollingLwjgl3Input.MouseDeltas();
        sample(d, 10, 0, 0); actual.add(10, 0);
        sample(d, -6, 0, 1_000_000); actual.add(-6, 0);
        long delta = actual.drain();
        d.consumed((int) (delta >> 32), (int) delta, 2_000_000);
        check(delta >> 32 == 4 && d.snapshot().x().betweenSampleCancellation() == 12, "Partial cancellation at accumulation stage");
        check(d.snapshot().timings().get("oldestSampleWait").maxUs() == 2000
                && d.snapshot().timings().get("newestSampleWait").maxUs() == 1000, "Oldest/newest nonzero waits");
        sample(d, 10, 5, 3_000_000); actual.add(10, 5);
        sample(d, -10, -5, 4_000_000); actual.add(-10, -5);
        delta = actual.drain();
        d.consumed((int) (delta >> 32), (int) delta, 5_000_000);
        check(delta == 0 && count(d, MouseScratchDiagnostics.Counter.CANCELLED_CONSUMES) == 1, "Full cancellation distinct from empty reads");
        check(actual.drain() == 0, "Actual motion is consumed once, never repeated at poll rate");
        d.consumed(0, 0, 6_000_000);
        check(count(d, MouseScratchDiagnostics.Counter.EMPTY_CONSUMES) == 1
                && count(d, MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES) == 0, "Empty polls are not lost motion");
    }

    private static void testGeometryAndBoundaries() {
        var d = new MouseScratchDiagnostics();
        sample(d, 10, 0, 0);
        check(d.snapshot().geometry().bufferPerInputX() == 2, "Record framebuffer/logical-input ratio");
        d.cursor(175, 120, 1000);
        d.sample(true, true, 160, 120, 640, 480, 640, 480, 640, 480, 1_000_000);
        check(count(d, MouseScratchDiagnostics.Counter.GEOMETRY_CHANGES) == 1
                && count(d, MouseScratchDiagnostics.Counter.BOUNDARY_SAMPLES) == 2,
                "Resize/re-enable center offsets are boundary candidates, not fixed automatically");
        check(d.snapshot().x().boundaryPath() == 15 && d.snapshot().x().callbackCancellation() == 0,
                "Do not count callback movement across geometry boundary as cancellation");
        d.sample(true, false, 0, 0, 640, 480, 640, 480, 640, 480, 2_000_000);
        check(d.snapshot().pendingBatches() == 2, "Focus loss does not silently clear the real accumulator");
        d.consumed(-150, -120, 3_000_000);
        check(count(d, MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES) == 0, "Old pending samples remain measurable after focus loss");
        d.sample(true, true, -1, 0, 640, 480, 640, 480, 640, 480, 4_000_000);
        check(count(d, MouseScratchDiagnostics.Counter.EDGE_SAMPLES) == 1
                && count(d, MouseScratchDiagnostics.Counter.OUTSIDE_SAMPLES) == 1, "Window-edge/clipping candidates");
    }

    private static void testOverflowAliasingAndReset() {
        var d = new MouseScratchDiagnostics();
        var actual = new PollingLwjgl3Input.MouseDeltas();
        // Width two gives center one; the largest delta stays representable before accumulation.
        for (int i = 0; i < 2; i++) {
            d.sample(true, true, Integer.MAX_VALUE, 1, 2, 2, 2, 2, 2, 2, i);
            actual.add(Integer.MAX_VALUE - 1, 0);
        }
        long delta = actual.drain();
        d.consumed((int) (delta >> 32), (int) delta, 10);
        check(count(d, MouseScratchDiagnostics.Counter.WRAPPED_CONSUMES) == 1
                && count(d, MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES) == 0, "Detect int accumulator wrap without changing it");
        d.applied(12000, 0, 0, 0, 12000, 0, 19200);
        check(d.snapshot().x().aliasReads() == 1, "Large movement aliases to shortest direction in the existing domain");
        d.applied(Integer.MAX_VALUE, 0, 100, 0, 0, 0, 19200);
        check(d.snapshot().x().additionOverflowCandidates() == 1, "Position addition overflow candidate");
        sample(d, 10, 5, 1000);
        d.reset();
        check(count(d, MouseScratchDiagnostics.Counter.DISCARDED_BATCHES) == 1
                && d.snapshot().x().discardedPath() == 10 && d.snapshot().y().discardedPath() == 5,
                "Configuration reset accounts for pending movement separately");
        d.close();
        var closed = d.snapshot();
        sample(d, 99, 99, 2000);
        d.cursor(1, 1, 2000);
        d.consumed(99, 99, 2000);
        d.reset();
        check(d.snapshot().equals(closed), "Disposed collector is inert");
    }

    private static void testAlgorithmParity() throws Exception {
        for (int mode : new int[]{MouseScratchConfig.MOUSE_SCRATCH_VER_1, MouseScratchConfig.MOUSE_SCRATCH_VER_2}) {
            var off = runAlgorithm(mode, false, 0);
            check(!off.isEmpty(), "Fixture exercises scratch and reverse transitions");
            check(off.equals(runAlgorithm(mode, true, 0)), "Diagnostic hooks preserve v1/v2 states, analog values, assignments and timestamps");
            check(off.equals(runAlgorithm(mode, true, 1000)), "Keyboard duration remains unused by mouse scratch, as before");
        }
    }

    private static List<String> runAlgorithm(int mode, boolean enabled, int keyboardDuration) throws Exception {
        var previous = Gdx.input;
        InputDiagnostics diagnostics = enabled ? new InputDiagnostics(false) : null;
        try {
            var polling = KeyboardQueueTestSupport.allocate(PollingLwjgl3Input.class);
            var actual = new PollingLwjgl3Input.MouseDeltas();
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "scratch", actual);
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "diagnostics", diagnostics);
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "keys", new AtomicIntegerArray(256));
            KeyboardQueueTestSupport.set(polling, PollingLwjgl3Input.class, "keyboardEvents", new KeyboardEventQueue());
            Gdx.input = polling;
            var config = new KeyboardConfig(Mode.BEAT_7K, false);
            config.setDuration(keyboardDuration);
            var input = KeyboardQueueTestSupport.input(config);
            KeyboardQueueTestSupport.set(input, BMSPlayerInputProcessor.class, "analogScroll", true);
            var mouse = config.getMouseScratchConfig();
            mouse.setMouseScratchEnabled(true);
            mouse.setKeyAssign(new int[]{2, 0, 3, 1});
            mouse.setMouseScratchMode(mode);
            mouse.setMouseScratchDistance(12);
            mouse.setMouseScratchTimeThreshold(150);
            var scratch = new MouseScratchInput(input, input.getKeyBoardInputProcesseor(), config);
            var analogField = BMSPlayerInputProcessor.class.getDeclaredField("currentAnalogValue");
            analogField.setAccessible(true);
            List<String> result = new ArrayList<>();
            int[][] motions = {{0,0}, {8,8}, {8,8}, {0,0}, {-5,-5}, {-15,-15}, {0,0}, {800,-800}, {-800,800}, {0,0}, {0,0}};
            long time = 1_000_000;
            for (int i = 0; i < motions.length; i++) {
                int dx = motions[i][0], dy = motions[i][1];
                if (diagnostics != null) sample(diagnostics.mouseScratch(), dx, dy, System.nanoTime());
                actual.add(dx, dy);
                scratch.poll(time);
                result.add(input.getKeyState(0) + ":" + input.getKeyState(1) + ":" + input.getKeyState(2) + ":"
                        + input.getKeyState(3) + ":" + scratch.getLastMouseScratch() + ":"
                        + Arrays.toString(Arrays.copyOf((float[]) analogField.get(input), 4)));
                time += i >= 8 ? 200_000 : 1000;
            }
            for (var event : input.getKeyInputLog()) result.add(event.getKeycode() + ":" + event.getTime() + ":" + event.isPressed());
            if (diagnostics != null) check(count(diagnostics.mouseScratch(), MouseScratchDiagnostics.Counter.ALGORITHM_POLLS) == motions.length
                    && diagnostics.mouseScratch().snapshot().x().aliasReads() > 0, "Real algorithm and large-domain observation hooks run");
            check(input.getKeyInputLog().length > 0, "Fixture generates real mapped play events");
            return result;
        } finally {
            Gdx.input = previous;
            if (diagnostics != null) diagnostics.close();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
