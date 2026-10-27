package bms.player.beatoraja.input;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns the legacy polling cadence and a bounded shutdown, without changing input event clocks. */
final class InputPollingThread implements AutoCloseable {
    private final Runnable poll, release;
    private final ThreadFactory threads;
    private final LongSupplier milliseconds;
    private final long joinMillis;
    private final AtomicBoolean released = new AtomicBoolean();
    private volatile boolean stopping;
    private Thread thread;
    private boolean finished, deferred;

    InputPollingThread(Runnable poll, Runnable release) {
        this(poll, release, task -> new Thread(task, "Input polling"), () -> System.nanoTime() / 1_000_000, 2000);
    }

    InputPollingThread(Runnable poll, Runnable release, ThreadFactory threads, LongSupplier milliseconds, long joinMillis) {
        this.poll = poll; this.release = release; this.threads = threads;
        this.milliseconds = milliseconds; this.joinMillis = Math.max(1, joinMillis);
    }

    synchronized void start() {
        if (thread != null || stopping) return;
        try {
            thread = threads.newThread(this::run);
            thread.start();
        } catch (RuntimeException | Error e) {
            stopping = true; finished = true;
            releaseOnce();
            throw e;
        }
    }

    private void run() {
        boolean failed = false;
        try {
            long time = 0;
            while (!stopping) {
                final long now = milliseconds.getAsLong();
                if (time != now) {
                    time = now;
                    poll.run();
                } else {
                    try { Thread.sleep(0, 500000); }
                    catch (InterruptedException e) { if (stopping) break; }
                }
            }
        } catch (Throwable e) {
            failed = true;
            Logger.getGlobal().log(Level.WARNING, "Input polling failed; stopping input", e);
        } finally {
            boolean cleanup;
            synchronized (this) { stopping = true; finished = true; cleanup = deferred || failed; }
            if (cleanup) releaseOnce();
        }
    }

    /** Never called while holding the keyboard or controller polling monitors. */
    boolean stopAndJoin() {
        Thread worker;
        synchronized (this) {
            stopping = true;
            worker = thread;
            if (worker == null) return true;
            worker.interrupt();
        }
        if (worker != Thread.currentThread()) {
            try { worker.join(joinMillis); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        synchronized (this) {
            if (!worker.isAlive() || finished) return true;
            deferred = true;
        }
        Logger.getGlobal().warning("Input polling is still stopping; device cleanup deferred until poll returns");
        return false;
    }

    @Override public void close() { if (stopAndJoin()) releaseOnce(); }

    private void releaseOnce() {
        if (!released.compareAndSet(false, true)) return;
        try { release.run(); }
        catch (Throwable e) { Logger.getGlobal().log(Level.WARNING, "Input device cleanup failed", e); }
    }
}
