package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Per-key FIFOs, committed at the same boundary as the old polling snapshot. */
public final class KeyboardEventQueue {
    private static final int KEYS = 256;
    private final int capacity;
    private final boolean[] events;
    private final long[] callbackTimes;
    private final long[] publicationTimes;
    private final long[] lastCallback = new long[KEYS];
    private final long[] lastPublication = new long[KEYS];
    private final int[] head = new int[KEYS];
    private final int[] size = new int[KEYS];
    private final int[] committed = new int[KEYS];
    private final boolean[] enabled = new boolean[KEYS];
    private final boolean[] enqueuedState = new boolean[KEYS];
    private boolean focused, overflow;

    public KeyboardEventQueue() { this(128); }
    KeyboardEventQueue(boolean timings) { this(128, timings); }

    KeyboardEventQueue(int capacity) { this(capacity, false); }
    KeyboardEventQueue(int capacity, boolean timings) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        events = new boolean[KEYS * capacity];
        callbackTimes = timings ? new long[events.length] : null;
        publicationTimes = timings ? new long[events.length] : null;
    }

    public synchronized void configure(int[] assignments, AtomicIntegerArray snapshot) {
        Arrays.fill(enabled, false);
        for (int key : assignments) if (key >= 0 && key < KEYS) enabled[key] = true;
        reset(snapshot);
    }

    public synchronized void offer(int key, boolean pressed) {
        offer(key, pressed, 0);
    }

    synchronized void offer(int key, boolean pressed, long callbackTime) {
        if (overflow || key < 0 || key >= KEYS || !enabled[key]) return;
        if (size[key] == capacity) {
            overflow = true;
            return;
        }
        int slot = key * capacity + (head[key] + size[key]) % capacity;
        events[slot] = pressed;
        enqueuedState[key] = pressed;
        if (callbackTimes != null) callbackTimes[slot] = callbackTime;
        size[key]++;
    }

    public synchronized void publish(boolean focused, AtomicIntegerArray snapshot) {
        if (this.focused != focused || !focused) reset(snapshot);
        this.focused = focused;
        for (int key = 0; key < KEYS; key++) if (enabled[key] && enqueuedState[key] != (snapshot.get(key) != 0))
            offer(key, snapshot.get(key) != 0);
        if (publicationTimes != null) {
            long now = System.nanoTime();
            for (int key = 0; key < KEYS; key++) for (int n = committed[key]; n < size[key]; n++)
                publicationTimes[key * capacity + (head[key] + n) % capacity] = now;
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
            remove(key);
            return value;
        }
        return -1;
    }

    public synchronized void reset(AtomicIntegerArray snapshot) {
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
}
