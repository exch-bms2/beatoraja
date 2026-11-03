package bms.player.beatoraja.input;

/** Geometry-only boundaries must not change sensitivity, FIFO ordering or judgment timestamps. */
public final class MouseScratchGeometryTest {
    record Context(long handle, long monitor, int w, int h, int bw, int bh, int x, int y,
            float sx, float sy, boolean focused, boolean minimized) {
        boolean sample(MouseScratchGeometry g) {
            return g.sample(handle, monitor, w, h, bw, bh, x, y, sx, sy, focused, minimized);
        }
    }
    private static final Context BASE = new Context(1, 0, 640, 480, 1280, 960, 20, 40, 2, 2, true, false);

    public static void main(String[] args) {
        Context[] changes = {
            new Context(1, 0, 800, 600, 1600, 1200, 20, 40, 2, 2, true, false),
            new Context(1, 0, 640, 480, 640, 480, 20, 40, 2, 2, true, false),
            new Context(1, 0, 640, 480, 1280, 960, -1900, 40, 2, 2, true, false),
            new Context(1, 0, 640, 480, 1280, 960, 20, 50, 2, 2, true, false),
            new Context(1, 0, 640, 480, 1280, 960, 20, 40, 1.25f, 2, true, false),
            new Context(1, 0, 640, 480, 1280, 960, 20, 40, 2, 1.5f, true, false),
            new Context(1, 7, 640, 480, 1280, 960, 20, 40, 2, 2, true, false),
            new Context(2, 0, 640, 480, 1280, 960, 20, 40, 2, 2, true, false)
        };
        for (Context change : changes) {
            var g = new MouseScratchGeometry();
            check(!BASE.sample(g) && BASE.sample(g), "Prime initial center once");
            check(!change.sample(g) && change.sample(g), "Suppress boundary, then accept stable samples: " + change);
            check(!BASE.sample(g) && BASE.sample(g), "Returning to original geometry is another boundary");
            g.invalidate(); g.invalidate();
            check(!BASE.sample(g) && BASE.sample(g), "Explicit before/after mode invalidations coalesce");
        }
        for (Context inactive : new Context[] {
                new Context(1, 0, 640, 480, 1280, 960, 20, 40, 2, 2, false, false),
                new Context(1, 0, 640, 480, 1280, 960, 20, 40, 2, 2, true, true),
                new Context(1, 0, 0, 0, 0, 0, 20, 40, 2, 2, true, false)}) {
            var g = new MouseScratchGeometry(); BASE.sample(g); BASE.sample(g);
            check(!inactive.sample(g) && !inactive.sample(g), "Never accept inactive window samples");
            check(!BASE.sample(g) && BASE.sample(g), "Re-prime focus/minimize/zero-size recovery");
        }
        testPendingMovement();
        testDiagnostics();
        System.out.println("MouseScratchGeometryTest passed (resize, position, DPI, mode, lifecycle and pending FIFO)");
    }

    private static void testPendingMovement() {
        var g = new MouseScratchGeometry(); var queue = new MouseMotionQueue();
        BASE.sample(g);
        if (BASE.sample(g)) queue.add(12, -4);
        var batch = new MouseMotionQueue.Batch();
        queue.drain(batch);
        check(batch.size == 1 && batch.dx[0] == 12 && batch.dy[0] == -4, "Ordinary logical movement unchanged");
        queue.add(6, 2);
        var resized = new Context(1, 0, 800, 600, 1600, 1200, 20, 40, 2, 2, true, false);
        if (resized.sample(g)) queue.add(-80, -60); // Old cursor center minus the new center.
        check(queue.isCurrent(batch), "Do not invalidate valid already-drained movement on geometry boundaries");
        if (resized.sample(g)) queue.add(-12, 3);
        queue.drain(batch);
        check(batch.size == 2 && batch.dx[0] == 6 && batch.dy[0] == 2
                && batch.dx[1] == -12 && batch.dy[1] == 3, "Retain valid pending movement and immediate stable reversal");
    }

    private static void testDiagnostics() {
        var d = new MouseScratchDiagnostics();
        d.sample(true, true, 325, 240, 640, 480, 640, 480, 1280, 960, 1);
        d.sample(true, true, 320, 240, 800, 600, 800, 600, 1600, 1200, 2, false);
        d.consumed(5, 0, 3);
        var s = d.snapshot();
        check(s.counts().get(MouseScratchDiagnostics.Counter.RECENTER_SAMPLES) == 1
                && s.counts().get(MouseScratchDiagnostics.Counter.CONSUME_MISMATCHES) == 0,
                "Diagnostics exclude synthetic center displacement without discarding valid pending data");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
