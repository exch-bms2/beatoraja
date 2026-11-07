package bms.player.beatoraja.input;

import bms.player.beatoraja.input.controller.JInputControllerCompatibilityTest;
import bms.player.beatoraja.input.controller.JInputControllerManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayerConfig;

/** No hardware enumeration: verify the actual polling owner and controller shutdown handshake. */
public final class InputShutdownTest {
    public static void main(String[] args) throws Exception {
        cadenceAndJoin(); neverStartedAndStartFailure(); failureAndSelfClose();
        blockedClose(false); blockedClose(true); backendOrder();
        System.out.println("InputShutdownTest passed (legacy cadence, stop/join/release, failures, self-close and backend ordering)");
    }

    private static void cadenceAndJoin() throws Exception {
        var worker = new AtomicReference<Thread>();
        var ticks = new AtomicInteger();
        var polls = new CountDownLatch(3);
        var released = new AtomicInteger();
        var caller = Thread.currentThread();
        long[] times = {0, 1, 1, 2, 3};
        var loop = new InputPollingThread(polls::countDown, () -> {
            check(Thread.currentThread() == caller && !worker.get().isAlive(), "Normal release occurs after join, on caller");
            released.incrementAndGet();
        }, recording(worker), () -> times[Math.min(times.length - 1, ticks.getAndIncrement())], 2000);
        loop.start(); loop.start(); await(polls); loop.close(); loop.close();
        check(released.get() == 1 && !worker.get().isAlive(), "One thread, one release");
    }

    private static void neverStartedAndStartFailure() {
        var released = new AtomicInteger();
        var loop = new InputPollingThread(() -> { throw new AssertionError(); }, released::incrementAndGet);
        loop.close(); loop.start(); loop.close();
        check(released.get() == 1, "Close before start is terminal and releases once");
        var primary = new IllegalStateException("Thread creation failed");
        for (boolean startFailure : new boolean[] {false, true}) {
            var count = new AtomicInteger();
            var failed = new InputPollingThread(() -> { }, count::incrementAndGet, task -> {
                if (!startFailure) throw primary;
                return new Thread(task) { @Override public synchronized void start() { throw primary; } };
            }, () -> 1, 100);
            try { failed.start(); throw new AssertionError(); } catch (IllegalStateException e) { check(e == primary, "Preserve start failure"); }
            failed.close(); check(count.get() == 1, "Thread startup failure releases once");
        }
    }

    private static void failureAndSelfClose() throws Exception {
        for (boolean selfClose : new boolean[] {false, true}) {
            var worker = new AtomicReference<Thread>();
            var loopRef = new AtomicReference<InputPollingThread>();
            var active = new AtomicBoolean();
            var released = new AtomicInteger();
            var loop = new InputPollingThread(() -> {
                active.set(true);
                try {
                    if (selfClose) loopRef.get().close();
                    else throw new IllegalStateException("Expected poll failure");
                } finally { active.set(false); }
            }, () -> { check(!active.get(), "Never release inside poll"); released.incrementAndGet(); },
                    recording(worker), () -> 1, 100);
            loopRef.set(loop); loop.start(); join(worker.get()); loop.close();
            check(released.get() == 1, "Poll failure/self-close cleans up once");
        }
    }

    private static void blockedClose(boolean interrupted) throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var active = new AtomicBoolean(); var disposed = new AtomicInteger();
        var worker = new AtomicReference<Thread>();
        var loop = new InputPollingThread(() -> {
            active.set(true); entered.countDown();
            try { awaitIgnoringInterrupt(release); } finally { active.set(false); }
        }, () -> { check(!active.get(), "Cannot close native device during poll"); disposed.incrementAndGet(); },
                recording(worker), () -> 1, 25);
        loop.start();
        try {
            await(entered);
            if (interrupted) Thread.currentThread().interrupt();
            loop.close();
            check(Thread.currentThread().isInterrupted() == interrupted, "Preserve caller interruption");
            Thread.interrupted();
            check(disposed.get() == 0 && worker.get().isAlive(), "Timeout does not force-close active device");
            loop.close(); check(disposed.get() == 0, "Repeated close cannot bypass deferred cleanup");
        } finally { Thread.interrupted(); release.countDown(); join(worker.get()); loop.close(); }
        check(disposed.get() == 1, "Deferred release after native poll returns");
    }

    private static void backendOrder() throws Exception {
        Runnable oldRunner = BMSPlayerInputProcessor.controllerPollRunner;
        var oldInput = com.badlogic.gdx.Gdx.input;
        com.badlogic.gdx.Gdx.input = (com.badlogic.gdx.Input) java.lang.reflect.Proxy.newProxyInstance(
                InputShutdownTest.class.getClassLoader(), new Class<?>[] {com.badlogic.gdx.Input.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? false
                        : method.getReturnType() == int.class ? 0 : null);
        var nativePoll = new CountDownLatch(1);
        var unblock = new CountDownLatch(1);
        var active = new AtomicBoolean(); var closed = new AtomicInteger();
        var manager = JInputControllerCompatibilityTest.createManager(new net.java.games.input.Controller[0], () -> {
            active.set(true); nativePoll.countDown();
            try { awaitIgnoringInterrupt(unblock); } finally { active.set(false); }
        }, () -> { check(!active.get(), "Controller release cannot overlap native polling"); closed.incrementAndGet(); }, Runnable::run);
        var runner = JInputControllerManager.class.getDeclaredField("pollRunner"); runner.setAccessible(true);
        BMSPlayerInputProcessor.controllerPollRunner = (Runnable) runner.get(manager);
        BMSPlayerInputProcessor input = null;
        Thread backend = null;
        try {
            var config = new Config();
            input = KeyboardQueueTestSupport.input(config, new PlayerConfig(), manager.getControllers());
            input.startPolling(); await(nativePoll);
            // The backend calls the manager first. Its shutdown hook must join outside manager.poll's monitor.
            backend = new Thread(manager::dispose, "Backend disposal"); backend.start();
            Thread.sleep(20);
            check(closed.get() == 0, "No controller disposal before poll completion");
            unblock.countDown(); join(backend);
            input.dispose(); input.dispose(); manager.dispose();
            check(closed.get() == 1 && BMSPlayerInputProcessor.controllerPollRunner == null,
                    "Backend-first shutdown detaches only its own runner and releases once");
            input.poll(); // terminal, even if someone retains the old input processor.
        } finally {
            unblock.countDown(); if (backend != null) join(backend);
            if (input != null) input.dispose(); manager.dispose();
            BMSPlayerInputProcessor.controllerPollRunner = oldRunner;
            com.badlogic.gdx.Gdx.input = oldInput;
        }
    }

    private static ThreadFactory recording(AtomicReference<Thread> worker) {
        return task -> { var t = new Thread(task, "Test input polling"); worker.set(t); return t; };
    }

    /** Test-only access to the shared polling lifecycle from controller fixtures. */
    public static final class TestPolling implements AutoCloseable {
        private final InputPollingThread thread;

        public TestPolling(Runnable poll, Runnable release) {
            thread = new InputPollingThread(poll, release);
        }

        public TestPolling(Runnable poll, Runnable release, ThreadFactory factory,
                java.util.function.LongSupplier clock, long timeout) {
            thread = new InputPollingThread(poll, release, factory, clock, timeout);
        }

        public void start() { thread.start(); }
        public boolean stopAndJoin() { return thread.stopAndJoin(); }
        @Override public void close() { thread.close(); }
    }
    private static void await(CountDownLatch latch) throws InterruptedException { check(latch.await(5, TimeUnit.SECONDS), "Latch timed out"); }
    private static void awaitIgnoringInterrupt(CountDownLatch latch) {
        boolean interrupted = false;
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try {
            while (true) {
                try { check(latch.await(Math.max(0, end - System.nanoTime()), TimeUnit.NANOSECONDS), "Unblock timed out"); return; }
                catch (InterruptedException e) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static void join(Thread thread) throws InterruptedException { thread.join(5000); check(!thread.isAlive(), "Thread did not exit"); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
