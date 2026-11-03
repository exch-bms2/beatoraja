package bms.player.beatoraja.audio;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALAudioDevice;
import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;

/** No audible output: synthetic clocks and fake libGDX devices. */
public final class AudioDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        String enabled = System.getProperty("beatoraja.audioDiagnostics");
        String interval = System.getProperty("beatoraja.audioDiagnostics.intervalMillis");
        Audio previousAudio = Gdx.audio;
        try {
            System.clearProperty("beatoraja.audioDiagnostics");
            check(AudioDiagnostics.createIfEnabled("test", new AudioConfig(), 48000, 2, 512) == null,
                    "Disabled diagnostics allocate no collector");
            System.setProperty("beatoraja.audioDiagnostics.intervalMillis", "1");
            check(AudioDiagnostics.intervalMillis() == 1000, "Minimum interval");
            System.setProperty("beatoraja.audioDiagnostics.intervalMillis", "999999");
            check(AudioDiagnostics.intervalMillis() == 60000, "Maximum interval");
            System.setProperty("beatoraja.audioDiagnostics.intervalMillis", "bad");
            check(AudioDiagnostics.intervalMillis() == 5000, "Invalid interval fallback");
            accounting();
            concurrencyAndShutdown();
            // This accessor requires no OpenAL context; it reports frames, not milliseconds.
            var device = new OpenALAudioDevice(null, 44100, false, 4096, 9);
            check(device.getLatency() == 9216, "Stereo 16-bit bytes -> frames, nine buffers");
            near(AudioDiagnostics.latencyMillis(device.getLatency(), 44100), 208.9795918367);
            check(Double.isNaN(AudioDiagnostics.latencyMillis(10, 0)), "Unknown rate is not zero latency");
            for (boolean on : new boolean[] {false, true}) {
                System.setProperty("beatoraja.audioDiagnostics", Boolean.toString(on));
                soundCompatibility(on);
                for (int rate : new int[] {0, 44100, 48000}) audioDeviceCompatibility(on, rate);
            }
        } finally {
            Gdx.audio = previousAudio;
            restore("beatoraja.audioDiagnostics", enabled);
            restore("beatoraja.audioDiagnostics.intervalMillis", interval);
        }
        System.out.println("AudioDiagnosticsTest passed");
    }

    private static void accounting() {
        var d = new AudioDiagnostics("test", 48000, 480, 5000);
        d.stream(0, 2_000_000, 10_000_000, 480, AudioDiagnostics.RETURNED, 1, 1);
        d.stream(30_000_000, 42_000_000, 45_000_000, 480, AudioDiagnostics.FAILED, .5f, 1);
        d.stream(45_000_000, 46_000_000, 50_000_000, 480, AudioDiagnostics.CANCELLED, 1, .75f);
        var before = d.snapshot();
        check(before.writes() == 3 && before.returned() == 1 && before.failed() == 1 && before.cancelled() == 1,
                "Separate write outcomes");
        check(before.returnedFrames() == 480, "Only returned writes count as submitted frames");
        near(before.submittedSeconds(), .01);
        near(before.producerElapsedSeconds(), .05);
        near(before.submissionRatio(), .2);
        check(before.mixOverBudget() == 1 && before.producerGapOverBudget() == 1, "Producer budget observations");
        near(before.write().meanUs(), 5000);
        check(before.write().maxUs() == 8000 && before.write().p95UpperUs() == 8192, "Timing histogram");
        check(before.lastPitch() == 1 && before.lastStretch() == .75f, "Intentional speed controls recorded");
        d.sound(0, 1900, 1, false, 1);
        d.sound(0, 1000, -1, false, 1);
        d.sound(0, 1000, -1, true, 1);
        var s = d.snapshot();
        check(s.playAttempts() == 3 && s.playRejected() == 1 && s.playExceptions() == 1, "Sound rejection != exception");
        check(s.play().p95UpperUs() == 2, "Sub-microsecond part rounded upwards for histogram bounds");
        check(before.playAttempts() == 0, "Snapshots immutable");
        d.close();
        d.stream(0, 0, 1, 480, AudioDiagnostics.RETURNED, 1, 1);
        check(d.snapshot().writes() == 3, "Ignore observations after close");
    }

    private static void concurrencyAndShutdown() throws Exception {
        var d = new AudioDiagnostics("test", 48000, 480, 60000);
        Thread a = new Thread(() -> { for (int i = 0; i < 1000; i++) d.sound(0, 1000, 1, false, 1); });
        Thread b = new Thread(() -> { for (int i = 0; i < 1000; i++) d.sound(0, 2000, 1, false, 1); });
        a.start(); b.start(); a.join(); b.join();
        check(d.snapshot().playAttempts() == 2000, "Concurrent callers retain all observations");
        d.start(); d.start();
        Thread reporter = (Thread) field(d, "reporter");
        check(reporter.isDaemon(), "Reporter never keeps application alive");
        d.close(); d.close(); reporter.join(2000);
        check(!reporter.isAlive(), "Reporter ends without waiting for reporting interval");
        var closed = new AudioDiagnostics("closed", 0, 0, 5000);
        closed.close(); closed.start();
        check(field(closed, "reporter") == null, "No reporter after close");
    }

    private static void soundCompatibility(boolean enabled) throws Exception {
        var calls = new AtomicInteger();
        var failure = new IllegalStateException("test rejection");
        Sound sound = (Sound) Proxy.newProxyInstance(Sound.class.getClassLoader(), new Class<?>[] {Sound.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("play")) {
                        int call = calls.incrementAndGet();
                        near((float) values[0], .7f);
                        near((float) values[1], .75f);
                        near((float) values[2], 0);
                        if (call == 3) throw failure;
                        return call == 2 ? -1L : 42L;
                    }
                    return null;
                });
        var config = new Config();
        config.setAudioConfig(new AudioConfig());
        var driver = new GdxSoundDriver(config);
        try {
            driver.setGlobalPitch(.5f);
            driver.play(sound, 1, .7f, 1.5f);
            driver.play(sound, 1, .7f, 1.5f);
            try { driver.play(sound, 1, .7f, 1.5f); throw new AssertionError("Exception must propagate"); }
            catch (IllegalStateException e) { check(e == failure, "Preserve original exception"); }
            var diagnostics = (AudioDiagnostics) field(driver, "diagnostics");
            check((diagnostics != null) == enabled, "Opt-in collector only");
            if (enabled) {
                var s = diagnostics.snapshot();
                check(s.playAttempts() == 3 && s.playRejected() == 1 && s.playExceptions() == 1, "Driver sound observations");
            }
        } finally { driver.dispose(); }
        check(calls.get() == 3, "No extra play calls");
    }

    private static void audioDeviceCompatibility(boolean enabled, int rate) throws Exception {
        var writes = new AtomicInteger();
        var failure = new AtomicReference<String>();
        AudioDevice device = (AudioDevice) Proxy.newProxyInstance(AudioDevice.class.getClassLoader(),
                new Class<?>[] {AudioDevice.class}, (proxy, method, values) -> {
                    return switch (method.getName()) {
                        case "isMono" -> false;
                        case "getLatency" -> 9216;
                        case "writeSamples" -> {
                            if (!(values[0] instanceof short[] pcm) || pcm.length != 2048
                                    || (int) values[1] != 0 || (int) values[2] != 2048)
                                failure.set("Stereo write must remain 1024 frames / 2048 interleaved short samples");
                            writes.incrementAndGet();
                            LockSupport.parkNanos(1_000_000);
                            yield null;
                        }
                        default -> null;
                    };
                });
        Gdx.audio = (Audio) Proxy.newProxyInstance(Audio.class.getClassLoader(), new Class<?>[] {Audio.class},
                (proxy, method, values) -> {
                    check(method.getName().equals("newAudioDevice"), "No additional backend calls");
                    check((int) values[0] == (rate == 0 ? 44100 : rate) && !(boolean) values[1], "Rate/stereo unchanged");
                    return device;
                });
        var config = new Config();
        config.setAudioConfig(new AudioConfig());
        config.getAudioConfig().setSampleRate(rate);
        config.getAudioConfig().setDeviceBufferSize(384);
        var driver = new GdxAudioDeviceDriver(config);
        Thread mixer = (Thread) field(driver, "mixer");
        try {
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (writes.get() < 3 && System.nanoTime() < deadline) Thread.sleep(1);
            check(writes.get() >= 3 && failure.get() == null, "Existing PCM write format: " + failure.get());
            var diagnostics = (AudioDiagnostics) field(driver, "diagnostics");
            check((diagnostics != null) == enabled, "AudioDevice opt-in");
            if (enabled) check(diagnostics.snapshot().returnedFrames() >= 2048, "Frames, not interleaved sample count");
        } finally { driver.dispose(); mixer.join(2000); }
        check(!mixer.isAlive(), "Test mixer terminates");
    }

    private static Object field(Object instance, String name) throws Exception {
        var f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(instance);
    }
    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name); else System.setProperty(name, value);
    }
    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < .000001, actual + " != " + expected);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
