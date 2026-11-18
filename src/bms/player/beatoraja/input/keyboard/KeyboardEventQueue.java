package bms.player.beatoraja.input.keyboard;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Per-key FIFOs, committed at the same boundary as the old polling snapshot. */
public final class KeyboardEventQueue {
    private static final int KEYS = 256;
    private final int capacity;
    private final boolean[] events;
    private final long[] callbackTimes;
    private final long[] publicationTimes;
    private final KeyboardTimingTrace.Edge[] traces, lastTraces;
    private final long[] lastCallback = new long[KEYS];
    private final long[] lastPublication = new long[KEYS];
    private final int[] head = new int[KEYS];
    private final int[] size = new int[KEYS];
    private final int[] committed = new int[KEYS];
    private final boolean[] enabled = new boolean[KEYS];
    private final boolean[] enqueuedState = new boolean[KEYS];
    private boolean focused, overflow;

    public KeyboardEventQueue() { this(128); }
    public KeyboardEventQueue(boolean timings) { this(128, timings); }
    public KeyboardEventQueue(boolean timings, boolean trace) { this(128, timings, trace); }

    KeyboardEventQueue(int capacity) { this(capacity, false); }
    KeyboardEventQueue(int capacity, boolean timings) {
        this(capacity, timings, false);
    }

    KeyboardEventQueue(int capacity, boolean timings, boolean trace) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        events = new boolean[KEYS * capacity];
        callbackTimes = timings ? new long[events.length] : null;
        publicationTimes = timings ? new long[events.length] : null;
        traces = trace ? new KeyboardTimingTrace.Edge[events.length] : null;
        lastTraces = trace ? new KeyboardTimingTrace.Edge[KEYS] : null;
    }

    public synchronized void configure(int[] assignments, AtomicIntegerArray snapshot) {
        Arrays.fill(enabled, false);
        for (int key : assignments) if (key >= 0 && key < KEYS) enabled[key] = true;
        reset(snapshot);
    }

    public void offer(int key, boolean pressed) {
        offer(key, pressed, 0);
    }

    public void offer(int key, boolean pressed, long callbackTime) {
        offer(key, pressed, callbackTime, null);
    }

    public synchronized void offer(int key, boolean pressed, long callbackTime, KeyboardTimingTrace.Edge trace) {
        if (overflow || key < 0 || key >= KEYS || !enabled[key]) return;
        if (size[key] == capacity) {
            overflow = true;
            return;
        }
        int slot = key * capacity + (head[key] + size[key]) % capacity;
        events[slot] = pressed;
        enqueuedState[key] = pressed;
        if (callbackTimes != null) callbackTimes[slot] = callbackTime;
        if (traces != null) traces[slot] = trace;
        size[key]++;
    }

    public synchronized void publish(boolean focused, AtomicIntegerArray snapshot) {
        if (this.focused != focused || !focused) reset(snapshot);
        this.focused = focused;
        for (int key = 0; key < KEYS; key++) if (enabled[key] && enqueuedState[key] != (snapshot.get(key) != 0))
            offer(key, snapshot.get(key) != 0);
        if (publicationTimes != null || traces != null) {
            long now = System.nanoTime();
            for (int key = 0; key < KEYS; key++) for (int n = committed[key]; n < size[key]; n++) {
                int slot = key * capacity + (head[key] + n) % capacity;
                if (publicationTimes != null) publicationTimes[slot] = now;
                if (traces != null && traces[slot] != null) traces[slot].publishedNanos = now;
            }
        }
        System.arraycopy(size, 0, committed, 0, KEYS);
    }

    /** -1 means no committed event; 0/1 are release/press. No allocation on the polling path. */
    public synchronized int peek(int key) {
        return overflow || key < 0 || key >= KEYS || committed[key] == 0 ? -1
                : events[key * capacity + head[key]] ? 1 : 0;
    }

    public synchronized void remove(int key) {
        if (key < 0 || key >= KEYS || committed[key] == 0) return;
        if (traces != null) traces[key * capacity + head[key]] = null;
        head[key] = (head[key] + 1) % capacity;
        committed[key]--;
        size[key]--;
    }

    /** Atomically remove one eligible edge. -2 is duration-deferred, -1 is empty. */
    public synchronized int pollReady(int key, boolean current, long now, long lastTime, int duration) {
        int value;
        while ((value = peek(key)) != -1) {
            if ((value != 0) == current) { remove(key); continue; }
            if (now < lastTime + duration * 1000L) return -2;
            if (callbackTimes != null) {
                lastCallback[key] = callbackTimes[key * capacity + head[key]];
                lastPublication[key] = publicationTimes[key * capacity + head[key]];
            }
            if (lastTraces != null) lastTraces[key] = traces[key * capacity + head[key]];
            remove(key);
            return value;
        }
        return -1;
    }

    public synchronized void reset(AtomicIntegerArray snapshot) {
        if (traces != null) {
            Arrays.fill(traces, null);
            Arrays.fill(lastTraces, null);
        }
        Arrays.fill(head, 0);
        Arrays.fill(size, 0);
        Arrays.fill(committed, 0);
        for (int key = 0; key < KEYS; key++) if (enabled[key]) {
            events[key * capacity] = snapshot.get(key) != 0;
            enqueuedState[key] = snapshot.get(key) != 0;
            if (callbackTimes != null) callbackTimes[key * capacity] = publicationTimes[key * capacity] = 0;
            size[key] = committed[key] = 1;
        }
    }

    public synchronized boolean isOverflowed() { return overflow; }
    synchronized long callbackTime(int key) { return lastCallback[key]; }
    synchronized long publicationTime(int key) { return lastPublication[key]; }
    KeyboardTimingTrace.Edge lastTrace(int key) {
        if (lastTraces == null) return null;
        synchronized (this) { return lastTraces[key]; }
    }
}
