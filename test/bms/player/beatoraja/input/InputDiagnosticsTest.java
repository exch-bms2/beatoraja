package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic timestamps: demonstrates losses without changing the production input path. */
public final class InputDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        testDisabled();
        testShortPressAndTimings();
        testFrameBudgets();
        testUnreadAndFocus();
        testMouseAndScratch();
        testGameSlots();
        testConcurrentObservations();
        System.out.println("InputDiagnosticsTest passed (synthetic timestamps, not hardware latency measurements)");
    }

    private static void testDisabled() {
        String old = System.getProperty("beatoraja.inputDiagnostics");
        try {
            System.clearProperty("beatoraja.inputDiagnostics");
            check(InputDiagnostics.createIfEnabled() == null, "No collector allocated by default");
        } finally {
            if (old == null) System.clearProperty("beatoraja.inputDiagnostics");
            else System.setProperty("beatoraja.inputDiagnostics", old);
        }
    }

    private static void testShortPressAndTimings() {
        var d = new InputDiagnostics(false);
        var keys = new AtomicIntegerArray(256);
        var buttons = new AtomicIntegerArray(5);
        d.publish(keys, buttons, true, 0);
        d.read(29, false, 0);
        d.edge(29, true, 1_000_000);
        d.edge(29, false, 2_000_000);
        d.publish(keys, buttons, true, 16_000_000);
        check(d.snapshot().keyCollapsed() == 1, "A complete press/release between snapshots is invisible to polling");
        check(d.snapshot().keyUnread() == 0, "Do not count the same lost press at both stages");

        d.edge(29, true, 18_000_000);
        keys.set(29, 1);
        d.publish(keys, buttons, true, 32_000_000);
        d.read(29, true, 33_000_000);
        d.keyboardDecision(29, true, 33_500_000);
        d.poll(33_000_000);
        d.poll(34_000_000);
        var snapshot = d.snapshot();
        check(snapshot.timings().get("callbackToSnapshot").maxUs() == 14_000, "Callback to snapshot delay");
        check(snapshot.timings().get("snapshotToRead").maxUs() == 1000, "Snapshot to polling read delay");
        check(snapshot.timings().get("callbackToRead").maxUs() == 15_000, "Callback to read delay");
        check(snapshot.timings().get("callbackToKeyboardForward").maxUs() == 15_500, "Forwarding delay, not hardware/judgment delay");
        check(snapshot.timings().get("updateGap").meanUs() == 16_000, "Frame/snapshot intervals");
        check(snapshot.timings().get("pollGap").maxUs() == 1000, "Polling intervals");
        check(snapshot.timings().get("callbackToRead").p95UpperUs() == 16_000, "Histogram percentile upper bound");
        check(snapshot.keyboardForwarded() == 1, "Forwarding count");
        d.repeat();
        d.unknownKey();
        d.edge(-1, true, 0);
        d.edge(999, true, 0);
        check(d.snapshot().keyEdges() == 3 && d.snapshot().repeats() == 1 && d.snapshot().unknownKeys() == 1,
                "Ignore repeats and invalid codes when counting real state transitions");
        d.close();
        long updates = d.snapshot().updates();
        d.publish(keys, buttons, true, 99_000_000);
        check(d.snapshot().updates() == updates, "Closed collector ignores new observations");
        d.close();
    }

    private static void testUnreadAndFocus() {
        var d = new InputDiagnostics(false);
        var keys = new AtomicIntegerArray(256);
        var buttons = new AtomicIntegerArray(5);
        d.publish(keys, buttons, true, 0);
        d.read(31, false, 0);
        d.edge(31, true, 1000);
        keys.set(31, 1);
        d.publish(keys, buttons, true, 2000);
        d.edge(31, false, 3000);
        keys.set(31, 0);
        d.publish(keys, buttons, true, 4000);
        check(d.snapshot().keyUnread() == 1, "Published press disappears before the consumer observes it");
        check(d.snapshot().keyCollapsed() == 0, "This loss is after publication, not before");
        d.edge(31, true, 5000);
        d.publish(keys, buttons, false, 6000);
        check(d.snapshot().focusSuppressed() == 1, "Separate focus suppression from coalescing");
        d.publish(keys, buttons, true, 7000);
        check(d.snapshot().keyCollapsed() == 0, "Focus regain does not fabricate a lost input");
        d.keyboardDecision(31, false, 8000);
        d.keyboardDecision(31, false, 9000);
        check(d.snapshot().debounceDeferrals() == 2, "Debounce deferrals count attempts, not lost physical presses");
        d.close();
    }

    private static void testMouseAndScratch() {
        var d = new InputDiagnostics(false);
        var keys = new AtomicIntegerArray(256);
        var buttons = new AtomicIntegerArray(5);
        d.publish(keys, buttons, true, 0);
        d.read(256, false, 0);
        d.edge(256, true, 100);
        d.edge(256, false, 200);
        d.publish(keys, buttons, true, 1000);
        check(d.snapshot().buttonCollapsed() == 1, "Short mouse state is coalesced; UI callbacks are a separate path");
        d.cursorCallback(1100);
        d.publish(keys, buttons, true, 1900);
        check(d.snapshot().timings().get("cursorToSnapshot").meanUs() == 0.8,
                "Latest cursor callback to snapshot timing");
        d.scratchPublished(10, 0, 2000);
        d.scratchPublished(-10, 0, 3000);
        d.scratchConsumed(5000);
        check(d.snapshot().scratchCancelled() == 1, "Opposite batches can cancel before scratch consumption");
        check(d.snapshot().timings().get("scratchWait").maxUs() == 3, "Wait measured from oldest pending batch");
        d.scratchConsumed(6000);
        check(d.snapshot().timings().get("scratchWait").count() == 1, "Do not repeat consumed motion");
        d.scratchPublished(3, 2, 7000);
        d.resetScratch();
        d.scratchConsumed(8000);
        check(d.snapshot().timings().get("scratchWait").count() == 1, "Configuration reset clears pending timing");
        d.close();
    }

    private static void testConcurrentObservations() throws Exception {
        var d = new InputDiagnostics(false);
        var keys = new AtomicIntegerArray(256);
        var buttons = new AtomicIntegerArray(5);
        var clock = new AtomicLong();
        var failure = new AtomicReference<Throwable>();
        Thread publisher = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    synchronized (d) {
                        d.edge(29, (i & 1) == 0, clock.incrementAndGet());
                        keys.set(29, (i & 1) == 0 ? 1 : 0);
                        d.publish(keys, buttons, true, clock.incrementAndGet());
                    }
                }
            } catch (Throwable e) { failure.set(e); }
        });
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 2000; i++) synchronized (d) {
                    d.read(29, keys.get(29) != 0, clock.incrementAndGet());
                    d.poll(clock.incrementAndGet());
                }
            } catch (Throwable e) { failure.set(e); }
        });
        publisher.start();
        reader.start();
        publisher.join(5000);
        reader.join(5000);
        check(!publisher.isAlive() && !reader.isAlive() && failure.get() == null, "No deadlock or concurrent corruption");
        check(d.snapshot().keyEdges() == 1000 && d.snapshot().polls() == 2000, "Aggregate counts remain exact");
        d.close();
    }

    private static void testGameSlots() {
        var d = new InputDiagnostics(false);
        d.gameWrite(1, false, 1000);
        d.gameWrite(1, true, 2000);
        d.gameRead(1, 5000);
        d.gameRead(1, 6000);
        check(d.snapshot().gameSlotReplaced() == 1 && d.snapshot().gameWrites() == 2,
                "Count pending-slot replacements independently from snapshot losses");
        check(d.snapshot().timings().get("writeToGameRead").count() == 1
                && d.snapshot().timings().get("writeToGameRead").maxUs() == 3, "Sample first read of the latest write only");
        d.gameReset(1);
        d.gameRead(1, 7000);
        d.gameWrite(2, false, 8000);
        d.clearGameSlots();
        d.gameRead(2, 9000);
        check(d.snapshot().timings().get("writeToGameRead").count() == 1, "Reset does not fabricate pending reads");
        d.close();
    }

    private static void testFrameBudgets() {
        for (long frame : new long[] {4_166_667, 8_333_333, 16_666_667, 100_000_000}) {
            var d = new InputDiagnostics(false);
            var keys = new AtomicIntegerArray(256);
            var buttons = new AtomicIntegerArray(5);
            d.publish(keys, buttons, true, 0);
            d.edge(29, true, 1_000_000);
            d.edge(29, false, 2_000_000);
            d.publish(keys, buttons, true, frame);
            check(d.snapshot().keyCollapsed() == 1, "Short pulse can disappear at 240/120/60 FPS or a stall");
            check(d.snapshot().timings().get("updateGap").maxUs() == frame / 1000, "Frame interval is measurable");
            d.close();
        }
        var d = new InputDiagnostics(false);
        d.poll(0);
        d.poll(100_001);
        check(d.snapshot().timings().get("pollGap").p95UpperUs() == 250, "Do not round down percentile bounds");
        d.close();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
