package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.InputShutdownTest.TestPolling;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Real input polling cadence without rendering; shutdown and failure exclude native use-after-free. */
public final class SdlRawAsyncControllerTest {
    private static ControllerBackend backend;
    public static void main(String[] args) throws Exception {
        for (var selected : new ControllerBackend[]{ControllerBackend.SDL_RAW_ASYNC, ControllerBackend.SDL_ASYNC}) {
            backend = selected;
            renderIndependent(); deferredShutdown(); failedPoll();
        }
        System.out.println("SdlRawAsyncControllerTest passed (no-render polling, shared runner, stop/join/owner release, deferred native call, backend-only failure)");
    }

    static Runnable runner(SdlRawControllerManager manager) throws Exception {
        var field = SdlRawControllerManager.class.getDeclaredField("pollRunner");
        field.setAccessible(true);
        return (Runnable) field.get(manager);
    }

    private static void renderIndependent() throws Exception {
        var fake = new Source();
        var posted = new ConcurrentLinkedQueue<Runnable>();
        var manager = new SdlRawControllerManager(fake, posted::add, backend);
        Runnable runner = runner(manager);
        check(ControllerBackendManager.ownerOf(runner) == manager, "Input runner participates in existing backend shutdown");
        var polling = new TestPolling(runner, () -> { });
        manager.setPollingShutdown(polling::stopAndJoin);
        try {
            polling.start();
            // No Gdx.app, render callback, SDL window or extra producer thread.
            check(fake.updated.await(3, TimeUnit.SECONDS), "Input polling progresses while rendering is absent");
            check(fake.worker.get() != null && fake.worker.get() != Thread.currentThread(), "Native acquisition runs on input worker");
            manager.dispose();
            check(fake.closed == 1, "Owner closes native source after polling joined");
            check(fake.worker.get().isAlive() == false, "No input worker remains after disposal");
            manager.dispose(); check(fake.closed == 1 && posted.isEmpty(), "Owner cleanup is idempotent");
        } finally { polling.close(); manager.dispose(); drain(posted); }
    }

    private static void deferredShutdown() throws Exception {
        var fake = new Source(); fake.block = true;
        var posted = new ConcurrentLinkedQueue<Runnable>();
        var manager = new SdlRawControllerManager(fake, posted::add, backend);
        var error = new AtomicReference<Throwable>();
        Runnable runner = runner(manager);
        var polling = new TestPolling(() -> {
            try { runner.run(); } catch (Throwable failure) { error.set(failure); throw failure; }
        }, manager::dispose, task -> new Thread(task, "Deferred SDL raw test"),
                () -> System.nanoTime() / 1_000_000, 20);
        manager.setPollingShutdown(polling::stopAndJoin);
        try {
            polling.start(); check(fake.entered.await(3, TimeUnit.SECONDS), "Worker entered blocking native read");
            manager.dispose(); check(fake.closed == 0, "Timed-out join must not free active native state");
            fake.release.countDown();
            fake.worker.get().join(3000);
            check(!fake.worker.get().isAlive() && error.get() == null, "Deferred native read completed without use-after-free");
            check(fake.closed == 0, "Input worker cannot destroy owner-thread SDL resources");
            drain(posted); manager.dispose(); check(fake.closed == 1, "Deferred cleanup reaches owner exactly once");
        } finally { fake.release.countDown(); polling.close(); manager.dispose(); drain(posted); }
    }

    private static void failedPoll() throws Exception {
        var fake = new Source();
        var posted = new ConcurrentLinkedQueue<Runnable>();
        var manager = new SdlRawControllerManager(fake, posted::add, backend);
        var stopped = new AtomicInteger(); manager.setPollingShutdown(() -> { stopped.incrementAndGet(); return true; });
        fake.fail = true;
        Runnable runner = runner(manager);
        var worker = new Thread(() -> { runner.run(); runner.run(); }, "Failed SDL raw test");
        worker.start(); worker.join(3000);
        check(!worker.isAlive() && fake.closed == 0, "Failure defers native release to owner");
        check(stopped.get() == 0 && fake.workerPolls.get() == 1, "Backend failure disables future reads without stopping keyboard/MIDI polling");
        check(!manager.getControllers().first().isConnected(), "Failure neutralizes cached controller state");
        drain(posted); manager.dispose(); check(fake.closed == 1, "Failure and later shutdown do not double-free");
    }

    private static final class Source extends SdlRawControllerTest.Fake {
        final CountDownLatch updated = new CountDownLatch(5), entered = new CountDownLatch(1), release = new CountDownLatch(1);
        final AtomicReference<Thread> worker = new AtomicReference<>();
        final AtomicInteger workerPolls = new AtomicInteger();
        volatile boolean block;
        Source() { super(true); }
        @Override public void update() {
            if (Thread.currentThread() != owner) {
                worker.compareAndSet(null, Thread.currentThread()); workerPolls.incrementAndGet();
                updated.countDown();
                if (block) {
                    entered.countDown();
                    boolean released = false;
                    while (!released) try { release.await(); released = true; } catch (InterruptedException expected) { }
                }
            }
            super.update();
        }
        @Override public void close() {
            check(worker.get() == null || !worker.get().isAlive(), "Native cleanup waits until input worker exited");
            super.close();
        }
    }
    private static void drain(ConcurrentLinkedQueue<Runnable> tasks) { Runnable task; while ((task = tasks.poll()) != null) task.run(); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
