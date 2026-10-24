package bms.player.beatoraja.input;

import java.util.EnumMap;
import java.util.Map;

/** Aggregate observations only. Coordinates and motion never feed back into gameplay. */
final class MouseScratchDiagnostics {
    enum Counter {
        SAMPLES, NONZERO_SAMPLES, CONSUMES, EMPTY_CONSUMES, CANCELLED_CONSUMES,
        RESETS, DISCARDED_BATCHES, BOUNDARY_CALLBACKS, BOUNDARY_SAMPLES,
        WARP_REQUESTS, WARP_TARGET_MATCHES, BASELINE_CALLBACKS, FRACTIONAL_CALLBACKS, INVALID_CALLBACKS,
        GEOMETRY_CHANGES, FOCUS_CHANGES, ENABLE_CHANGES, WRAPPED_CONSUMES, CONSUME_MISMATCHES,
        EDGE_SAMPLES, OUTSIDE_SAMPLES,
        ALGORITHM_POLLS, ALGORITHM_EDGES, FORWARD_ATTEMPTS, FIFO_RECORDS, RECENTER_SAMPLES
    }
    private final long[] counts = new long[Counter.values().length];
    private final Axis x = new Axis(), y = new Axis();
    private final InputDiagnostics.Histogram sampleGap = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram consumeGap = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram oldestWait = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram newestWait = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram callbackToSample = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram algorithmDuration = new InputDiagnostics.Histogram();
    private boolean haveContext, enabled, focused, baseline, warpPending, haveSample, haveConsume, closed;
    private int logicalWidth, logicalHeight, inputWidth, inputHeight, bufferWidth, bufferHeight;
    private int mode, distance, threshold;
    private double cursorX, cursorY, warpX, warpY;
    private long frameCallbacks, lastCallback, lastSample, lastConsume, pendingBatches, peakPendingBatches;
    private long firstPending, lastPending;

    private void count(Counter counter) { counts[counter.ordinal()]++; }

    synchronized void configure(int mode, int distance, int threshold) {
        if (closed) return;
        this.mode = mode;
        this.distance = distance;
        this.threshold = threshold;
    }

    synchronized void cursor(double nextX, double nextY, long now) {
        if (closed || !enabled || !focused) return;
        if (!Double.isFinite(nextX) || !Double.isFinite(nextY)) { count(Counter.INVALID_CALLBACKS); return; }
        if (warpPending && nextX == warpX && nextY == warpY) {
            count(Counter.WARP_TARGET_MATCHES);
            warpPending = false;
            baseline = true;
            cursorX = nextX;
            cursorY = nextY;
            return;
        }
        if (nextX != (int) nextX || nextY != (int) nextY) count(Counter.FRACTIONAL_CALLBACKS);
        if (baseline) {
            x.callback(nextX - cursorX, (long) (int) nextX - (int) cursorX);
            y.callback(nextY - cursorY, (long) (int) nextY - (int) cursorY);
        } else count(Counter.BASELINE_CALLBACKS);
        baseline = true;
        cursorX = nextX;
        cursorY = nextY;
        frameCallbacks++;
        lastCallback = now;
    }

    synchronized void sample(boolean enabled, boolean focused, int px, int py,
            int lw, int lh, int iw, int ih, int bw, int bh, long now) {
        sample(enabled, focused, px, py, lw, lh, iw, ih, bw, bh, now, true);
    }

    synchronized void sample(boolean enabled, boolean focused, int px, int py,
            int lw, int lh, int iw, int ih, int bw, int bh, long now, boolean deliver) {
        if (closed) return;
        boolean geometryChanged = haveContext && (logicalWidth != lw || logicalHeight != lh
                || inputWidth != iw || inputHeight != ih || bufferWidth != bw || bufferHeight != bh);
        boolean boundary = !haveContext || geometryChanged || this.enabled != enabled || this.focused != focused;
        if (geometryChanged) count(Counter.GEOMETRY_CHANGES);
        if (haveContext && this.focused != focused) count(Counter.FOCUS_CHANGES);
        if (haveContext && this.enabled != enabled) count(Counter.ENABLE_CHANGES);
        if (boundary) {
            counts[Counter.BOUNDARY_CALLBACKS.ordinal()] += frameCallbacks;
            baseline = warpPending = false;
            haveSample = false;
        } else {
            x.finishFrame();
            y.finishFrame();
            if (frameCallbacks > 0) callbackToSample.add(now - lastCallback);
        }
        frameCallbacks = 0;
        x.clearFrame();
        y.clearFrame();
        haveContext = true;
        this.enabled = enabled;
        this.focused = focused;
        logicalWidth = lw;
        logicalHeight = lh;
        inputWidth = iw;
        inputHeight = ih;
        bufferWidth = bw;
        bufferHeight = bh;
        if (!enabled || !focused) return;
        count(Counter.SAMPLES);
        if (haveSample) sampleGap.add(now - lastSample);
        haveSample = true;
        lastSample = now;
        if (!deliver) {
            count(Counter.RECENTER_SAMPLES);
            baseline = warpPending = false;
            return;
        }
        int dx = px - iw / 2, dy = py - ih / 2;
        if (px <= 0 || py <= 0 || px >= iw - 1 || py >= ih - 1) count(Counter.EDGE_SAMPLES);
        if (px < 0 || py < 0 || px >= iw || py >= ih) count(Counter.OUTSIDE_SAMPLES);
        x.sample(dx);
        y.sample(dy);
        if (dx != 0 || dy != 0) {
            count(Counter.NONZERO_SAMPLES);
            if (boundary) count(Counter.BOUNDARY_SAMPLES);
            if (pendingBatches == 0) firstPending = now;
            lastPending = now;
            pendingBatches++;
            peakPendingBatches = Math.max(peakPendingBatches, pendingBatches);
        }
    }

    /** GLFW's later target-matching callback is only a warp candidate, not proven synthetic motion. */
    synchronized void warp(int centerX, int centerY) {
        if (closed) return;
        count(Counter.WARP_REQUESTS);
        if (inputWidth <= 0 || inputHeight <= 0) { baseline = false; return; }
        warpX = (int) (centerX * (double) logicalWidth / inputWidth);
        warpY = (int) (centerY * (double) logicalHeight / inputHeight);
        cursorX = warpX;
        cursorY = warpY;
        baseline = warpPending = true;
    }

    synchronized void consumed(int dx, int dy, long now) {
        if (closed) return;
        count(Counter.CONSUMES);
        if (haveConsume) consumeGap.add(now - lastConsume);
        haveConsume = true;
        lastConsume = now;
        if (pendingBatches == 0) count(Counter.EMPTY_CONSUMES);
        else {
            oldestWait.add(now - firstPending);
            newestWait.add(now - lastPending);
            if (x.pendingNet == 0 && y.pendingNet == 0) count(Counter.CANCELLED_CONSUMES);
        }
        if (x.pendingNet != (int) x.pendingNet || y.pendingNet != (int) y.pendingNet) count(Counter.WRAPPED_CONSUMES);
        if (dx != (int) x.pendingNet || dy != (int) y.pendingNet) count(Counter.CONSUME_MISMATCHES);
        x.consume(dx);
        y.consume(dy);
        pendingBatches = 0;
    }

    synchronized void applied(int dx, int dy, int beforeX, int beforeY, int afterX, int afterY, int domain) {
        if (closed || domain <= 0) return;
        x.applied(dx, beforeX, afterX, domain);
        y.applied(dy, beforeY, afterY, domain);
    }

    synchronized void consumedQueue(MouseMotionQueue.Batch batch, long now) {
        if (closed) return;
        int sumX = 0, sumY = 0;
        for (int i = 0; i < batch.size; i++) {
            sumX += batch.dx[i];
            sumY += batch.dy[i];
            x.deliveredPath += Math.abs((long) batch.dx[i]);
            y.deliveredPath += Math.abs((long) batch.dy[i]);
        }
        counts[Counter.FIFO_RECORDS.ordinal()] += batch.size;
        consumed(sumX, sumY, now);
    }

    synchronized void algorithm(long started, long ended, int edges, int forwards) {
        if (closed) return;
        count(Counter.ALGORITHM_POLLS);
        counts[Counter.ALGORITHM_EDGES.ordinal()] += edges;
        counts[Counter.FORWARD_ATTEMPTS.ordinal()] += forwards;
        algorithmDuration.add(ended - started);
    }

    synchronized void reset() {
        if (closed) return;
        count(Counter.RESETS);
        counts[Counter.DISCARDED_BATCHES.ordinal()] += pendingBatches;
        x.discardedPath += x.pendingPath;
        y.discardedPath += y.pendingPath;
        x.clearPending();
        y.clearPending();
        pendingBatches = 0;
        resetCallbacks();
        haveSample = haveConsume = false;
    }

    synchronized void resetCallbacks() {
        if (closed) return;
        counts[Counter.BOUNDARY_CALLBACKS.ordinal()] += frameCallbacks;
        frameCallbacks = 0;
        x.clearFrame();
        y.clearFrame();
        baseline = warpPending = false;
    }

    synchronized Snapshot snapshot() {
        var counters = new EnumMap<Counter, Long>(Counter.class);
        for (var counter : Counter.values()) counters.put(counter, counts[counter.ordinal()]);
        return new Snapshot(Map.copyOf(counters), x.snapshot(), y.snapshot(), pendingBatches, peakPendingBatches,
                new Geometry(logicalWidth, logicalHeight, inputWidth, inputHeight, bufferWidth, bufferHeight,
                        inputWidth > 0 ? bufferWidth / (double) inputWidth : 0,
                        inputHeight > 0 ? bufferHeight / (double) inputHeight : 0), mode, distance, threshold,
                Map.of("sampleGap", sampleGap.snapshot(), "consumeGap", consumeGap.snapshot(),
                        "oldestSampleWait", oldestWait.snapshot(), "newestSampleWait", newestWait.snapshot(),
                        "callbackToSample", callbackToSample.snapshot(), "algorithmDuration", algorithmDuration.snapshot()));
    }

    synchronized void close() { closed = true; }

    record Geometry(int logicalWidth, int logicalHeight, int inputWidth, int inputHeight,
            int bufferWidth, int bufferHeight, double bufferPerInputX, double bufferPerInputY) { }
    record Snapshot(Map<Counter, Long> counts, AxisSnapshot x, AxisSnapshot y, long pendingBatches,
            long peakPendingBatches, Geometry geometry, int mode, int distance, int threshold,
            Map<String, InputDiagnostics.Timing> timings) { }
    record AxisSnapshot(double callbackPath, double callbackCancellation, long callbackReversals,
            long integerZeroSteps, double boundaryPath, long sampledPath, long consumedPath,
            long betweenSampleCancellation, long discardedPath, long pendingNet, long pendingPath,
            long aliasReads, long additionOverflowCandidates, long deliveredPath) { }

    private static final class Axis {
        double callbackPath, callbackCancellation, framePath, frameNet, boundaryPath;
        long callbackReversals, frameReversals, integerZeroSteps, sampledPath, consumedPath;
        long betweenSampleCancellation, discardedPath, pendingNet, pendingPath, aliasReads, additionOverflowCandidates;
        long deliveredPath;
        int direction;
        void callback(double delta, long integerDelta) {
            framePath += Math.abs(delta);
            frameNet += delta;
            if (delta != 0 && integerDelta == 0) integerZeroSteps++;
            int sign = delta > 0 ? 1 : delta < 0 ? -1 : 0;
            if (sign != 0 && direction != 0 && sign != direction) frameReversals++;
            if (sign != 0) direction = sign;
        }
        void finishFrame() {
            callbackPath += framePath;
            callbackCancellation += Math.max(0, framePath - Math.abs(frameNet));
            callbackReversals += frameReversals;
            framePath = 0;
        }
        void clearFrame() {
            boundaryPath += framePath;
            framePath = frameNet = 0;
            frameReversals = 0;
            direction = 0;
        }
        void sample(int delta) {
            sampledPath += Math.abs((long) delta);
            pendingPath += Math.abs((long) delta);
            pendingNet += delta;
        }
        void consume(int delta) {
            consumedPath += Math.abs((long) delta);
            betweenSampleCancellation += Math.max(0, pendingPath - Math.abs(pendingNet));
            clearPending();
        }
        void clearPending() { pendingPath = pendingNet = 0; }
        void applied(int delta, int before, int after, int domain) {
            int diff = after - before;
            if (diff >= domain / 2) diff -= domain;
            else if (diff < -domain / 2) diff += domain;
            if (diff != delta) aliasReads++;
            long sum = (long) before + delta;
            if (sum != (int) sum) additionOverflowCandidates++;
        }
        AxisSnapshot snapshot() {
            return new AxisSnapshot(callbackPath, callbackCancellation, callbackReversals, integerZeroSteps,
                    boundaryPath, sampledPath, consumedPath, betweenSampleCancellation, discardedPath,
                    pendingNet, pendingPath, aliasReads, additionOverflowCandidates, deliveredPath);
        }
    }
}
