package bms.player.beatoraja.input;

import java.util.Arrays;
import java.util.stream.IntStream;

/** Bounded keyboard-only judgment stream. Other devices retain their existing state path. */
public final class KeyboardJudgeQueue {
    private final int[] keys;
    private final boolean[] pressed;
    private final long[] times;
    private final boolean[] enabled = new boolean[256];
    private final boolean[] held = new boolean[256];
    private int head, size;
    private boolean active, overflow;

    public KeyboardJudgeQueue() { this(8192); }

    KeyboardJudgeQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        keys = new int[capacity];
        pressed = new boolean[capacity];
        times = new long[capacity];
    }

    public synchronized void begin(int[] assignments, boolean[] initial) {
        Arrays.fill(enabled, false);
        if (assignments != null)
            for (int key : assignments) if (key >= 0 && key < enabled.length) enabled[key] = true;
        Arrays.fill(held, false);
        System.arraycopy(initial, 0, held, 0, Math.min(initial.length, held.length));
        head = size = 0;
        overflow = false;
        active = true;
    }

    public synchronized boolean isQueued(int key) {
        return active && key >= 0 && key < enabled.length && enabled[key];
    }

    public synchronized void offer(int key, boolean down, long time) {
        if (!isQueued(key) || overflow) return;
        if (size == keys.length) { overflow = true; return; }
        int index = (head + size) % keys.length;
        keys[index] = key;
        pressed[index] = down;
        times[index] = time;
        size++;
    }

    /** Freeze a finite prefix so a busy producer cannot monopolize the judgment thread. */
    public synchronized int available() { return active ? size : 0; }

    public synchronized void drain(Batch batch) {
        batch.size = active && !overflow ? Math.min(size, batch.keys.length) : 0;
        for (int i = 0; i < batch.size; i++) {
            batch.keys[i] = keys[head];
            batch.pressed[i] = pressed[head];
            batch.times[i] = times[head];
            head = (head + 1) % keys.length;
            size--;
        }
    }

    public synchronized void apply(int key, boolean down) { held[key] = down; }

    public synchronized boolean held(int key) { return key >= 0 && key < held.length && held[key]; }
    public synchronized boolean isOverflowed() { return overflow; }
    public synchronized int[] configuredKeys() { return IntStream.range(0, enabled.length).filter(i -> enabled[i]).toArray(); }

    public synchronized void clear() {
        head = size = 0;
        Arrays.fill(held, false);
    }

    public synchronized void synchronizeHeld(boolean[] current) {
        clear();
        System.arraycopy(current, 0, held, 0, Math.min(current.length, held.length));
    }

    public synchronized void end() { active = false; clear(); }

    /** Reused by the single consumer, never allocated per event. */
    public static final class Batch {
        public final int[] keys = new int[8192];
        public final boolean[] pressed = new boolean[8192];
        public final long[] times = new long[8192];
        public int size;
    }
}
