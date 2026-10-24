package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Logger;

/** Opt-in, aggregate-only observations. Does not change input states or judgment timestamps. */
public final class InputDiagnostics implements AutoCloseable {
    static final int KEY_COUNT = 256;
    static final int CHANNELS = KEY_COUNT + 5;
    private final boolean[] raw = new boolean[CHANNELS];
    private final boolean[] published = new boolean[CHANNELS];
    private final boolean[] observed = new boolean[CHANNELS];
    private final boolean[] queried = new boolean[CHANNELS];
    private final boolean[] downObserved = new boolean[CHANNELS];
    private final long[] callbackAt = new long[CHANNELS];
    private final long[] publishedAt = new long[CHANNELS];
    private final long[] snapshotCallbackAt = new long[CHANNELS];
    private final long[] readCallbackAt = new long[CHANNELS];
    private final long[] pendingPresses = new long[CHANNELS];
    private final long[] gameWrittenAt = new long[KEY_COUNT];
    private final boolean[] gameRead = new boolean[KEY_COUNT];
    private final Histogram updateGap = new Histogram();
    private final Histogram pollGap = new Histogram();
    private final Histogram callbackToSnapshot = new Histogram();
    private final Histogram snapshotToRead = new Histogram();
    private final Histogram callbackToRead = new Histogram();
    private final Histogram callbackToKeyboardForward = new Histogram();
    private final Histogram scratchWait = new Histogram();
    private final Histogram cursorToSnapshot = new Histogram();
    private final Histogram writeToGameRead = new Histogram();
    private long updates, polls, keyEdges, buttonEdges, repeats, unknownKeys, cursorCallbacks;
    private long keyCollapsed, buttonCollapsed, keyUnread, buttonUnread, focusSuppressed;
    private long keyboardForwarded, debounceDeferrals, scratchBatches, scratchCancelled;
    private long gameWrites, gameSlotReplaced;
    private long lastUpdate, lastPoll, scratchSince, scratchX, scratchY;
    private long lastCursor;
    private boolean cursorPending;
    private boolean haveUpdate, havePoll, scratchPending, focused, closed;
    private final ScheduledExecutorService reporter;

    /** No arrays, callback hooks or worker are allocated unless this property is enabled. */
    static InputDiagnostics createIfEnabled() {
        if (!Boolean.getBoolean("beatoraja.inputDiagnostics")) return null;
        return new InputDiagnostics(true);
    }

    InputDiagnostics(boolean report) {
        Arrays.fill(callbackAt, Long.MIN_VALUE);
        Arrays.fill(publishedAt, Long.MIN_VALUE);
        Arrays.fill(snapshotCallbackAt, Long.MIN_VALUE);
        Arrays.fill(readCallbackAt, Long.MIN_VALUE);
        Arrays.fill(gameWrittenAt, Long.MIN_VALUE);
        if (report) {
            reporter = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "Input diagnostics reporter");
                thread.setDaemon(true);
                return thread;
            });
            long interval = Math.max(1000, Math.min(60000,
                    Long.getLong("beatoraja.inputDiagnostics.intervalMillis", 5000L)));
            Logger.getGlobal().info("Input diagnostics enabled: intervalMs=" + interval
                    + ", cumulative=true, hardwareLatencyMeasured=false, keyValuesLogged=false");
            reporter.scheduleWithFixedDelay(this::log, interval, interval, TimeUnit.MILLISECONDS);
        } else {
            reporter = null;
        }
    }

    synchronized void edge(int channel, boolean down, long now) {
        if (closed || channel < 0 || channel >= CHANNELS || raw[channel] == down) return;
        raw[channel] = down;
        callbackAt[channel] = now;
        if (down) pendingPresses[channel]++;
        if (channel < KEY_COUNT) keyEdges++;
        else buttonEdges++;
    }

    synchronized void repeat() { if (!closed) repeats++; }
    synchronized void unknownKey() { if (!closed) unknownKeys++; }
    synchronized void cursorCallback(long now) {
        if (closed) return;
        cursorCallbacks++;
        lastCursor = now;
        cursorPending = true;
    }

    synchronized void publish(AtomicIntegerArray keys, AtomicIntegerArray buttons, boolean focused, long now) {
        if (closed) return;
        if (haveUpdate) updateGap.add(now - lastUpdate);
        haveUpdate = true;
        lastUpdate = now;
        updates++;
        boolean previouslyFocused = this.focused;
        this.focused = focused;
        if (cursorPending && focused && previouslyFocused) cursorToSnapshot.add(now - lastCursor);
        cursorPending = false;
        for (int i = 0; i < CHANNELS; i++) {
            boolean down = i < KEY_COUNT ? keys.get(i) != 0 : buttons.get(i - KEY_COUNT) != 0;
            if (focused && previouslyFocused) {
                long collapsed = Math.max(0, pendingPresses[i] - (down && !published[i] ? 1 : 0));
                if (i < KEY_COUNT) keyCollapsed += collapsed;
                else buttonCollapsed += collapsed;
                if (published[i] && !down && queried[i] && !downObserved[i]) {
                    if (i < KEY_COUNT) keyUnread++;
                    else buttonUnread++;
                }
            } else {
                focusSuppressed += pendingPresses[i];
            }
            pendingPresses[i] = 0;
            if (published[i] != down) {
                publishedAt[i] = now;
                downObserved[i] = false;
                snapshotCallbackAt[i] = Long.MIN_VALUE;
                if (focused && previouslyFocused && raw[i] == down && callbackAt[i] != Long.MIN_VALUE) {
                    snapshotCallbackAt[i] = callbackAt[i];
                    callbackToSnapshot.add(now - callbackAt[i]);
                }
            }
            published[i] = down;
        }
    }

    synchronized void poll(long now) {
        if (closed) return;
        if (havePoll) pollGap.add(now - lastPoll);
        havePoll = true;
        lastPoll = now;
        polls++;
    }

    synchronized void read(int channel, boolean down, long now) {
        if (closed || channel < 0 || channel >= CHANNELS) return;
        queried[channel] = true;
        readCallbackAt[channel] = snapshotCallbackAt[channel];
        if (observed[channel] != down) {
            if (focused && publishedAt[channel] != Long.MIN_VALUE) {
                snapshotToRead.add(now - publishedAt[channel]);
                if (snapshotCallbackAt[channel] != Long.MIN_VALUE)
                    callbackToRead.add(now - snapshotCallbackAt[channel]);
            }
            if (down) downObserved[channel] = true;
        }
        observed[channel] = down;
    }

    synchronized void keyboardDecision(int key, boolean forwarded, long now) {
        if (closed) return;
        if (forwarded) {
            keyboardForwarded++;
            if (key >= 0 && key < KEY_COUNT && focused && readCallbackAt[key] != Long.MIN_VALUE)
                callbackToKeyboardForward.add(now - readCallbackAt[key]);
        } else {
            debounceDeferrals++;
        }
    }

    synchronized void keyboardEventRead(int key, long callbackTime, long publicationTime, long now) {
        readCallbackAt[key] = callbackTime == 0 ? Long.MIN_VALUE : callbackTime;
        if (callbackTime != 0) callbackToRead.add(now - callbackTime);
        if (publicationTime != 0) snapshotToRead.add(now - publicationTime);
    }

    synchronized void scratchPublished(int dx, int dy, long now) {
        if (closed || (dx == 0 && dy == 0)) return;
        if (!scratchPending) scratchSince = now;
        scratchPending = true;
        scratchX += dx;
        scratchY += dy;
        scratchBatches++;
    }

    synchronized void scratchConsumed(long now) {
        if (closed || !scratchPending) return;
        scratchWait.add(now - scratchSince);
        if (scratchX == 0 && scratchY == 0) scratchCancelled++;
        resetScratch();
    }

    synchronized void resetScratch() {
        scratchPending = false;
        scratchX = scratchY = 0;
    }

    synchronized void gameWrite(int key, boolean pending, long now) {
        if (closed || key < 0 || key >= KEY_COUNT) return;
        gameWrites++;
        if (pending && gameWrittenAt[key] != Long.MIN_VALUE) gameSlotReplaced++;
        gameWrittenAt[key] = now;
        gameRead[key] = false;
    }

    synchronized void gameRead(int key, long now) {
        if (closed || key < 0 || key >= KEY_COUNT || gameWrittenAt[key] == Long.MIN_VALUE || gameRead[key]) return;
        writeToGameRead.add(now - gameWrittenAt[key]);
        gameRead[key] = true;
    }

    synchronized void gameReset(int key) {
        if (key >= 0 && key < KEY_COUNT) gameWrittenAt[key] = Long.MIN_VALUE;
    }

    synchronized void clearGameSlots() { Arrays.fill(gameWrittenAt, Long.MIN_VALUE); }

    /** Cumulative snapshot; formatting/allocation happens only on the reporter or explicit inspection. */
    public synchronized Snapshot snapshot() {
        return new Snapshot(updates, polls, keyEdges, buttonEdges, repeats, unknownKeys, cursorCallbacks,
                keyCollapsed, buttonCollapsed, keyUnread, buttonUnread, focusSuppressed,
                keyboardForwarded, debounceDeferrals, scratchBatches, scratchCancelled,
                gameWrites, gameSlotReplaced,
                Map.of("updateGap", updateGap.snapshot(), "pollGap", pollGap.snapshot(),
                        "callbackToSnapshot", callbackToSnapshot.snapshot(), "snapshotToRead", snapshotToRead.snapshot(),
                        "callbackToRead", callbackToRead.snapshot(), "callbackToKeyboardForward", callbackToKeyboardForward.snapshot(),
                        "scratchWait", scratchWait.snapshot(), "writeToGameRead", writeToGameRead.snapshot(),
                        "cursorToSnapshot", cursorToSnapshot.snapshot()));
    }

    public record Snapshot(long updates, long polls, long keyEdges, long buttonEdges, long repeats,
            long unknownKeys, long cursorCallbacks, long keyCollapsed, long buttonCollapsed,
            long keyUnread, long buttonUnread, long focusSuppressed, long keyboardForwarded,
            long debounceDeferrals, long scratchBatches, long scratchCancelled, long gameWrites,
            long gameSlotReplaced, Map<String, Timing> timings) {}

    public record Timing(long count, double meanUs, long maxUs, long p95UpperUs) {}

    private void log() {
        Snapshot s = snapshot();
        Logger.getGlobal().info("Input diagnostics [cumulative] updates=" + s.updates + ", polls=" + s.polls
                + ", keyEdges=" + s.keyEdges + ", buttonEdges=" + s.buttonEdges + ", repeats=" + s.repeats
                + ", unknownKeys=" + s.unknownKeys + ", cursorCallbacks=" + s.cursorCallbacks
                + ", keyCollapsed=" + s.keyCollapsed + ", buttonCollapsed=" + s.buttonCollapsed
                + ", keyUnread=" + s.keyUnread + ", buttonUnread=" + s.buttonUnread
                + ", focusSuppressed=" + s.focusSuppressed + ", keyboardForwarded=" + s.keyboardForwarded
                + ", debounceDeferrals=" + s.debounceDeferrals + ", scratchBatches=" + s.scratchBatches
                + ", scratchCancelled=" + s.scratchCancelled + ", gameWrites=" + s.gameWrites
                + ", gameSlotReplaced=" + s.gameSlotReplaced);
        s.timings.forEach((name, timing) -> Logger.getGlobal().info("  " + name
                + " count=" + timing.count + ", meanUs=" + timing.meanUs + ", maxUs=" + timing.maxUs
                + ", p95UpperUs=" + timing.p95UpperUs));
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

    static final class Histogram {
        private static final long[] LIMIT_US = {100, 250, 500, 1000, 2000, 4000, 8000, 16000,
                32000, 64000, 128000, 256000, 1000000, Long.MAX_VALUE};
        private final long[] buckets = new long[LIMIT_US.length];
        private long count, sum, max;
        void add(long nanos) {
            if (nanos < 0) return;
            count++;
            sum += nanos;
            max = Math.max(max, nanos);
            int bucket = 0;
            while (LIMIT_US[bucket] != Long.MAX_VALUE && nanos > LIMIT_US[bucket] * 1000) bucket++;
            buckets[bucket]++;
        }
        Timing snapshot() {
            long accumulated = 0, p95 = 0;
            for (int i = 0; i < buckets.length && count != 0; i++) {
                accumulated += buckets[i];
                if (accumulated >= count - count / 20) {
                    p95 = LIMIT_US[i];
                    break;
                }
            }
            return new Timing(count, count == 0 ? 0 : sum / (1000.0 * count), max / 1000, p95);
        }
    }
}
