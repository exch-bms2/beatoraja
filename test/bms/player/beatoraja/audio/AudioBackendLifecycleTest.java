package bms.player.beatoraja.audio;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadFactory;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;
import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;

/** Failure injection only: no real audio device or user files. */
public final class AudioBackendLifecycleTest {
    public static void main(String[] args) throws Exception {
        for (boolean enabled : new boolean[] {false, true}) {
            String old = System.getProperty("beatoraja.audioDiagnostics");
            System.setProperty("beatoraja.audioDiagnostics", Boolean.toString(enabled));
            try {
                initializationFailures();
                writeFailure();
                blockedShutdown(false);
                blockedShutdown(true);
                openALCleanup();
            } finally {
                if (old == null) System.clearProperty("beatoraja.audioDiagnostics");
                else System.setProperty("beatoraja.audioDiagnostics", old);
            }
        }
        System.out.println("AudioBackendLifecycleTest passed (initialization, write failures, deferred shutdown, OpenAL cleanup)");
    }

    private static void initializationFailures() {
        expectFailure(() -> new GdxAudioDeviceDriver(config(), null, Thread::new));
        var failure = new IllegalStateException("Device creation failed");
        Audio audio = audio((proxy, method, values) -> { throw failure; });
        check(expectFailure(() -> new GdxAudioDeviceDriver(config(), audio, Thread::new)) == failure,
                "Preserve device creation failure");
        for (String stage : new String[] {"isMono", "getLatency", "threadFactory", "start"}) {
            var disposals = new AtomicInteger();
            var cleanup = new IllegalStateException("Cleanup failed");
            AudioDevice device = device((proxy, method, values) -> {
                if (method.getName().equals(stage)) throw failure;
                return switch (method.getName()) {
                    case "dispose" -> { disposals.incrementAndGet(); throw cleanup; }
                    case "isMono" -> false;
                    case "getLatency" -> 9216;
                    default -> null;
                };
            });
            ThreadFactory threads = task -> {
                if (stage.equals("threadFactory")) throw failure;
                return new Thread(task) {
                    @Override public synchronized void start() { throw failure; }
                };
            };
            var primary = expectFailure(() -> new GdxAudioDeviceDriver(config(), audioFor(device), threads));
            check(primary == failure && disposals.get() == 1, "Close acquired device exactly once at " + stage);
            check(java.util.Arrays.asList(primary.getSuppressed()).contains(cleanup), "Retain cleanup failure");
        }
    }

    private static void writeFailure() throws Exception {
        var writes = new AtomicInteger();
        var disposals = new AtomicInteger();
        var inWrite = new AtomicBoolean();
        AudioDevice device = device((proxy, method, values) -> switch (method.getName()) {
            case "isMono" -> false;
            case "getLatency" -> 9216;
            case "writeSamples" -> {
                inWrite.set(true);
                try { writes.incrementAndGet(); throw new IllegalStateException("Write failed"); }
                finally { inWrite.set(false); }
            }
            case "dispose" -> {
                check(!inWrite.get(), "Never dispose during write");
                disposals.incrementAndGet();
                throw new IllegalStateException("Dispose failed");
            }
            default -> null;
        });
        var worker = new AtomicReference<Thread>();
        var driver = new GdxAudioDeviceDriver(config(), audioFor(device), recording(worker));
        join(worker.get());
        driver.dispose(); driver.dispose();
        check(writes.get() == 1 && disposals.get() == 1, "No failed write retry or double disposal");
        var element = new AbstractAudioDriver.AudioElement<PCM>(new ShortPCM(2, 44100, 0, 4, new short[4]));
        driver.play(element, 1, false);
        check(element.id == -1, "Do not accept new playback after failure");
    }

    private static void blockedShutdown(boolean interrupted) throws Exception {
        var writing = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var disposals = new AtomicInteger();
        var writes = new AtomicInteger();
        var inWrite = new AtomicBoolean();
        AudioDevice device = device((proxy, method, values) -> switch (method.getName()) {
            case "isMono" -> false;
            case "getLatency" -> 9216;
            case "writeSamples" -> {
                writes.incrementAndGet(); inWrite.set(true); writing.countDown();
                try { awaitUninterruptibly(release); }
                finally { inWrite.set(false); }
                yield null;
            }
            case "dispose" -> {
                check(!inWrite.get(), "Deferred cleanup must wait for native write return");
                disposals.incrementAndGet(); yield null;
            }
            default -> null;
        });
        var worker = new AtomicReference<Thread>();
        var driver = new GdxAudioDeviceDriver(config(), audioFor(device), recording(worker));
        try {
            await(writing);
            if (interrupted) Thread.currentThread().interrupt();
            driver.dispose();
            check(Thread.currentThread().isInterrupted() == interrupted, "Preserve caller interruption");
            Thread.interrupted();
            check(disposals.get() == 0 && worker.get().isAlive(), "Timeout/interruption never frees a busy device");
            driver.dispose();
            check(disposals.get() == 0, "Repeated shutdown cannot bypass deferred cleanup");
        } finally {
            Thread.interrupted();
            release.countDown();
            join(worker.get());
            driver.dispose();
        }
        check(disposals.get() == 1 && writes.get() == 1, "Only the in-flight block, then exactly one cleanup");
    }

    @SuppressWarnings("unchecked")
    private static void openALCleanup() throws Exception {
        var disposed = new AtomicInteger();
        var plays = new AtomicInteger();
        Sound bad = sound((proxy, method, values) -> switch (method.getName()) {
            case "play" -> { plays.incrementAndGet(); yield 5L; }
            case "stop" -> throw new IllegalStateException("Stop failed");
            case "dispose" -> { disposed.incrementAndGet(); throw new IllegalStateException("Dispose failed"); }
            default -> null;
        });
        Sound good = sound((proxy, method, values) -> {
            if (method.getName().equals("dispose")) disposed.incrementAndGet();
            return null;
        });
        var driver = new GdxSoundDriver(config());
        var mapField = AbstractAudioDriver.class.getDeclaredField("soundmap");
        mapField.setAccessible(true);
        var map = (ObjectMap<String, AbstractAudioDriver.AudioElement<Sound>>) mapField.get(driver);
        map.put("bad", new AbstractAudioDriver.AudioElement<>(bad));
        map.put("good", new AbstractAudioDriver.AudioElement<>(good));
        driver.play(bad, 1, 1, 1);
        driver.dispose(); driver.dispose();
        driver.play(bad, 1, 1, 1);
        check(disposed.get() == 2 && map.size == 0, "One failing Sound cannot prevent remaining cleanup");
        check(plays.get() == 1, "No play after OpenAL driver disposal");
    }

    private static Config config() {
        var config = new Config();
        config.setAudioConfig(new AudioConfig());
        return config;
    }
    private static ThreadFactory recording(AtomicReference<Thread> worker) {
        return task -> { var t = new Thread(task, "Test audio mixer"); worker.set(t); return t; };
    }
    private static Audio audioFor(AudioDevice device) {
        return audio((proxy, method, values) -> {
            check(method.getName().equals("newAudioDevice"), "No extra backend calls");
            check((int) values[0] == 44100 && !(boolean) values[1], "Default rate/stereo unchanged");
            return device;
        });
    }
    private static Audio audio(java.lang.reflect.InvocationHandler handler) { return proxy(Audio.class, handler); }
    private static AudioDevice device(java.lang.reflect.InvocationHandler handler) { return proxy(AudioDevice.class, handler); }
    private static Sound sound(java.lang.reflect.InvocationHandler handler) { return proxy(Sound.class, handler); }
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
    private static Throwable expectFailure(Runnable task) {
        try { task.run(); } catch (IllegalStateException e) { return e; }
        throw new AssertionError("Expected failure");
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        check(latch.await(5, TimeUnit.SECONDS), "Latch timed out");
    }
    private static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try {
            while (true) {
                try {
                    check(latch.await(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS), "Release timed out");
                    return;
                } catch (InterruptedException e) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static void join(Thread worker) throws InterruptedException {
        worker.join(5000);
        check(!worker.isAlive(), "Mixer failed to terminate");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
