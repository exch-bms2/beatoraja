package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.InputDiagnostics;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Opt-in observations of Java-visible controller events; never changes gameplay input. */
final class ControllerInputDiagnostics implements AutoCloseable {
    private final float[] values, before;
    private final int[] changes;
    private final int buttons, axes;
    private final String id;
    private final InputDiagnostics.Histogram pollGap = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram pollDuration = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram bmGap = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram snapshotToBm = new InputDiagnostics.Histogram();
    private final InputDiagnostics.Histogram bmDuration = new InputDiagnostics.Histogram();
    private long polls, failures, buttonEdges, povEdges, axisEvents;
    private long buttonCollapsed, povCollapsed, axisCollapsed, gatedSamples, acceptedEdges, forwarded;
    private long gameWrites, gameSlotReplaced;
    private long buttonSnapshotMismatches, povSnapshotMismatches, axisSnapshotMismatches;
    private long snapshotCorrections, snapshotDeferred, jinputEvents, jinputMaxEvents, jinputSaturatedPolls;
    private long lastPoll, lastBm, publishedAt;
    private boolean havePoll, haveBm, havePublication, closed;
    private final ScheduledExecutorService reporter;

    ControllerInputDiagnostics(String id, int buttons, int axes, boolean report) {
        this.id = id;
        this.buttons = buttons;
        this.axes = axes;
        values = new float[buttons + axes + 1];
        before = new float[values.length];
        changes = new int[values.length];
        if (report) {
            reporter = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Controller input diagnostics");
                thread.setDaemon(true);
                return thread;
            });
            long interval = Math.max(1000, Math.min(60000,
                    Long.getLong("beatoraja.inputDiagnostics.intervalMillis", 5000L)));
            reporter.scheduleWithFixedDelay(this::log, interval, interval, TimeUnit.MILLISECONDS);
        } else reporter = null;
    }

    synchronized void seed(int channel, float value) { values[channel] = value; }

    synchronized void beginPoll(long now) {
        if (closed) return;
        if (havePoll) pollGap.add(now - lastPoll);
        havePoll = true;
        lastPoll = now;
        System.arraycopy(values, 0, before, 0, values.length);
        Arrays.fill(changes, 0);
    }

    synchronized void event(int channel, float value) {
        if (closed || Float.compare(values[channel], value) == 0) return;
        values[channel] = value;
        changes[channel]++;
        if (channel < buttons) buttonEdges++;
        else if (channel < buttons + axes) axisEvents++;
        else povEdges++;
        if (traceRemaining > 0) trace(channel < buttons ? "button" : channel < buttons + axes ? "axis" : "pov",
                channel < buttons ? channel : channel < buttons + axes ? channel - buttons : channel - buttons - axes,
                value, "backend-channel");
    }

    private int traceRemaining;
    private int logicalTraceRemaining;

    synchronized void setAssignmentTrace(boolean enabled) {
        traceRemaining = enabled ? 256 : 0;
        logicalTraceRemaining = enabled ? 32 : 0;
    }

    synchronized void traceLogicalPress(int button) {
        if (closed || logicalTraceRemaining == 0) return;
        logicalTraceRemaining--;
        Logger.getGlobal().info("KeyConfig controller input: device=" + id + ", stage=logical, index=" + button);
    }

    synchronized void trace(String kind, int index, float value, String stage) {
        if (closed || traceRemaining == 0) return;
        Logger.getGlobal().info("KeyConfig controller input: device=" + id + ", stage=" + stage
                + ", kind=" + kind + ", index=" + index + ", value=" + value);
        if (--traceRemaining == 0) Logger.getGlobal().info("KeyConfig controller input trace limit reached: device=" + id);
    }

    String traceId() { return id; }

    synchronized void endPoll(long now, boolean success) {
        if (closed) return;
        polls++;
        pollDuration.add(now - lastPoll);
        if (!success) { failures++; return; }
        publishedAt = now;
        havePublication = true;
        for (int i = 0; i < changes.length; i++) {
            long collapsed = Math.max(0, changes[i] - (Float.compare(before[i], values[i]) == 0 ? 0 : 1));
            if (i < buttons) buttonCollapsed += collapsed;
            else if (i < buttons + axes) axisCollapsed += collapsed;
            else povCollapsed += collapsed;
        }
    }

    synchronized void beginBm(long now) {
        if (closed) return;
        if (haveBm) bmGap.add(now - lastBm);
        haveBm = true;
        lastBm = now;
        if (havePublication) snapshotToBm.add(now - publishedAt);
    }

    synchronized void endBm(long now, int gated, int accepted, int sent) {
        if (closed) return;
        bmDuration.add(now - lastBm);
        gatedSamples += gated;
        acceptedEdges += accepted;
        forwarded += sent;
    }

    synchronized void gameWrite(boolean pending) {
        if (closed) return;
        gameWrites++;
        if (pending) gameSlotReplaced++;
    }

    synchronized void jinputQueue(int events, int confirmedCapacity) {
        if (closed) return;
        jinputEvents += events;
        jinputMaxEvents = Math.max(jinputMaxEvents, events);
        // A full Java queue is a saturation candidate, not a measurement of native loss.
        if (confirmedCapacity > 0 && events >= confirmedCapacity) jinputSaturatedPolls++;
    }

    synchronized void snapshotMismatch(int channel, float value, boolean deferred) {
        if (closed) return;
        if (channel < buttons) buttonSnapshotMismatches++;
        else if (channel < buttons + axes) axisSnapshotMismatches++;
        else povSnapshotMismatches++;
        if (deferred) snapshotDeferred++;
        else {
            snapshotCorrections++;
            // Corrections are not observed edges and must not inflate collapsed-event counts.
            values[channel] = value;
        }
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(polls, failures, buttonEdges, povEdges, axisEvents, buttonCollapsed,
                povCollapsed, axisCollapsed, gatedSamples, acceptedEdges, forwarded, gameWrites, gameSlotReplaced,
                buttonSnapshotMismatches, povSnapshotMismatches, axisSnapshotMismatches,
                snapshotCorrections, snapshotDeferred, jinputEvents, jinputMaxEvents, jinputSaturatedPolls,
                Map.of("pollGap", pollGap.snapshot(), "pollDuration", pollDuration.snapshot(),
                        "bmGap", bmGap.snapshot(), "snapshotToBm", snapshotToBm.snapshot(),
                        "bmDuration", bmDuration.snapshot()));
    }

    record Snapshot(long polls, long failures, long buttonEdges, long povEdges, long axisEvents,
            long buttonCollapsed, long povCollapsed, long axisCollapsed, long gatedSamples,
            long acceptedEdges, long forwarded, long gameWrites, long gameSlotReplaced,
            long buttonSnapshotMismatches, long povSnapshotMismatches, long axisSnapshotMismatches,
            long snapshotCorrections, long snapshotDeferred, long jinputEvents, long jinputMaxEvents,
            long jinputSaturatedPolls,
            Map<String, InputDiagnostics.Timing> timings) { }

    private void log() {
        Logger.getGlobal().info("Controller input diagnostics [cumulative] device=" + id
                + ", hardwareLatencyMeasured=false, nativeQueueOverflowMeasured=false, " + snapshot());
    }

    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        if (reporter != null) {
            reporter.shutdownNow();
            log();
        }
    }
}
