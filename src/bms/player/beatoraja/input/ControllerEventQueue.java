package bms.player.beatoraja.input;

import java.util.Arrays;

/** Per-button FIFOs after physical-button/first-POV OR; axes remain snapshots. */
final class ControllerEventQueue {
    private static final int BUTTONS = 32;
    private final int capacity;
    private final boolean[] events;
    private final long[] order;
    private final boolean[] enqueued = new boolean[BUTTONS];
    private final int[] head = new int[BUTTONS], size = new int[BUTTONS];
    private boolean active, overflow;
    private long sequence;

    ControllerEventQueue() { this(128); }
    ControllerEventQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        events = new boolean[BUTTONS * capacity];
        order = new long[events.length];
    }

    synchronized void reset(boolean active, boolean[] snapshot) {
        this.active = active;
        Arrays.fill(head, 0);
        Arrays.fill(size, 0);
        sequence = 0;
        for (int key = 0; key < BUTTONS; key++) {
            enqueued[key] = snapshot[key];
            if (active) {
                events[key * capacity] = snapshot[key];
                order[key * capacity] = sequence++;
                size[key] = 1;
            }
        }
    }

    synchronized void offer(int key, boolean down) {
        if (!active || overflow || key < 0 || key >= BUTTONS || enqueued[key] == down) return;
        if (size[key] == capacity) { overflow = true; return; }
        int slot = key * capacity + (head[key] + size[key]) % capacity;
        events[slot] = down;
        order[slot] = sequence++;
        enqueued[key] = down;
        size[key]++;
    }

    /** -1 empty, -2 duration-deferred, 0 release, 1 press. */
    synchronized int pollReady(int key, boolean current, long now, long lastTime, int duration) {
        if (!active || overflow) return -1;
        while (size[key] > 0) {
            boolean down = events[key * capacity + head[key]];
            if (down != current && now < lastTime + duration * 1000L) return -2;
            head[key] = (head[key] + 1) % capacity;
            size[key]--;
            if (down != current) return down ? 1 : 0;
        }
        return -1;
    }

    /** Earliest eligible event across buttons. A deferred button never blocks another button. */
    synchronized int pollNext(boolean[] current, long[] lastTime, long now, int duration) {
        if (!active || overflow) return -1;
        int next = -1;
        long first = Long.MAX_VALUE;
        for (int key = 0; key < BUTTONS; key++) {
            while (size[key] > 0 && events[key * capacity + head[key]] == current[key]) {
                head[key] = (head[key] + 1) % capacity;
                size[key]--;
            }
            if (size[key] == 0 || now < lastTime[key] + duration * 1000L) continue;
            long position = order[key * capacity + head[key]];
            if (position < first) { first = position; next = key; }
        }
        if (next == -1) return -1;
        return next * 2 + pollReady(next, current[next], now, lastTime[next], duration);
    }

    synchronized int deferredCount(boolean[] current, long[] lastTime, long now, int duration) {
        int count = 0;
        if (active && !overflow) for (int key = 0; key < BUTTONS; key++)
            if (size[key] > 0 && events[key * capacity + head[key]] != current[key]
                    && now < lastTime[key] + duration * 1000L) count++;
        return count;
    }

    synchronized boolean isOverflowed() { return overflow; }
}
