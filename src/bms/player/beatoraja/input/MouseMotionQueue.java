package bms.player.beatoraja.input;

/** Bounded FIFO of sampled X/Y position differences, not cursor callbacks. */
public class MouseMotionQueue {
    static final int CAPACITY = 4096;
    private final int[] dx, dy;
    private int head, size;
    private long generation;
    private boolean overflow;

    public MouseMotionQueue() { this(CAPACITY); }
    MouseMotionQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        dx = new int[capacity];
        dy = new int[capacity];
    }

    public synchronized void add(int x, int y) {
        if (overflow || (x == 0 && y == 0)) return;
        if (size == dx.length) { overflow = true; return; }
        int slot = (head + size) % dx.length;
        dx[slot] = x;
        dy[slot] = y;
        size++;
    }

    /** Freeze a finite prefix; no producer lock is held during scratch conversion. */
    public synchronized void drain(Batch batch) {
        batch.generation = generation;
        batch.size = overflow ? 0 : Math.min(size, batch.dx.length);
        for (int i = 0; i < batch.size; i++) {
            batch.dx[i] = dx[head];
            batch.dy[i] = dy[head];
            head = (head + 1) % dx.length;
            size--;
        }
    }

    /** Legacy aggregate probe. Gameplay uses drain(Batch), never this summing method. */
    public synchronized long drain() {
        int x = 0, y = 0;
        if (!overflow) for (int i = 0; i < size; i++) {
            int slot = (head + i) % dx.length;
            x += dx[slot];
            y += dy[slot];
        }
        head = size = 0;
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    public synchronized void clear() { head = size = 0; generation++; }
    public synchronized boolean isCurrent(Batch batch) { return !overflow && batch.generation == generation; }
    public synchronized boolean isOverflowed() { return overflow; }

    public static final class Batch {
        public final int[] dx = new int[CAPACITY], dy = new int[CAPACITY];
        public int size;
        private long generation;
    }
}
