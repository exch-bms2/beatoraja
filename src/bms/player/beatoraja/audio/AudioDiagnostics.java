package bms.player.beatoraja.audio;

import java.util.logging.Logger;
import bms.player.beatoraja.AudioConfig;

/** Opt-in application-side observations. No audio data, native error consumption or playback control. */
final class AudioDiagnostics implements AutoCloseable {
    static final int RETURNED = 0, FAILED = 1, CANCELLED = 2;
    private final String description;
    private final int sampleRate, framesPerWrite;
    private final long intervalMillis;
    private final Timing mix = new Timing(), write = new Timing(), producerGap = new Timing(), play = new Timing();
    private long attempts, completed, failed, cancelled, frames, firstStart, lastEnd;
    private boolean haveWrite;
    private long mixOverBudget, producerGapOverBudget, playAttempts, playRejected, playExceptions;
    private float lastPitch = 1, lastStretch = 1;
    private volatile boolean closed;
    private Thread reporter;

    static AudioDiagnostics createIfEnabled(String backend, AudioConfig config, int rate, int channels, int framesPerWrite) {
        if (!Boolean.getBoolean("beatoraja.audioDiagnostics")) return null;
        return new AudioDiagnostics(backend + ", configuredRate=" + config.getSampleRate()
                + ", configuredBuffer=" + config.getDeviceBufferSize() + ", sources=" + config.getDeviceSimultaneousSources()
                + ", pcmRate=" + rate + ", channels=" + channels + ", framesPerWrite=" + framesPerWrite
                + ", freqOption=" + config.getFreqOption() + ", fastForward=" + config.getFastForward(),
                rate, framesPerWrite, intervalMillis());
    }

    static long intervalMillis() {
        return Math.max(1000, Math.min(60000, Long.getLong("beatoraja.audioDiagnostics.intervalMillis", 5000L)));
    }

    static double latencyMillis(int frames, int rate) {
        return rate > 0 && frames >= 0 ? frames * 1000.0 / rate : Double.NaN;
    }

    /** Call only on the existing OpenAL context owner at driver construction, never the reporting thread. */
    static void openALInfo() {
        try {
            long context = org.lwjgl.openal.ALC10.alcGetCurrentContext();
            if (context == 0) {
                Logger.getGlobal().info("Audio diagnostics OpenAL : currentContext=false");
                return;
            }
            long device = org.lwjgl.openal.ALC10.alcGetContextsDevice(context);
            Logger.getGlobal().info("Audio diagnostics OpenAL : backend=" + com.badlogic.gdx.Gdx.audio.getClass().getName()
                    + ", vendor=" + org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_VENDOR)
                    + ", renderer=" + org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_RENDERER)
                    + ", version=" + org.lwjgl.openal.AL10.alGetString(org.lwjgl.openal.AL10.AL_VERSION)
                    + ", contextRate=" + org.lwjgl.openal.ALC10.alcGetInteger(device, org.lwjgl.openal.ALC10.ALC_FREQUENCY)
                    + ", contextRateIsHardwareRate=false");
        } catch (RuntimeException | LinkageError e) {
            Logger.getGlobal().warning("Audio diagnostics OpenAL info unavailable : " + e);
        }
    }

    AudioDiagnostics(String description, int rate, int framesPerWrite, long intervalMillis) {
        this.description = description;
        this.sampleRate = rate;
        this.framesPerWrite = framesPerWrite;
        this.intervalMillis = intervalMillis;
    }

    synchronized void start() {
        if (reporter != null || closed) return;
        reporter = new Thread(() -> {
            try {
                Logger.getGlobal().info("Audio diagnostics enabled [hardwareLatencyMeasured=false, underrunsMeasured=false,"
                        + " PCMNotLogged=true] " + description);
                while (!closed) {
                    Thread.sleep(intervalMillis);
                    if (!closed) report();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally { report(); }
        }, "Audio diagnostics");
        reporter.setDaemon(true);
        reporter.start();
    }

    synchronized void stream(long started, long writeStarted, long ended, int submittedFrames, int outcome,
            float pitch, float stretch) {
        if (closed) return;
        long budget = sampleRate > 0 ? (long) framesPerWrite * 1_000_000_000L / sampleRate : 0;
        if (!haveWrite) { firstStart = started; haveWrite = true; }
        else {
            long gap = Math.max(0, started - lastEnd);
            producerGap.add(gap);
            if (budget > 0 && gap > budget) producerGapOverBudget++;
        }
        long mixing = Math.max(0, writeStarted - started);
        mix.add(mixing);
        if (budget > 0 && mixing > budget) mixOverBudget++;
        write.add(Math.max(0, ended - writeStarted));
        lastEnd = ended;
        attempts++;
        if (outcome == RETURNED) { completed++; frames += submittedFrames; }
        else if (outcome == CANCELLED) cancelled++;
        else failed++;
        lastPitch = pitch; lastStretch = stretch;
    }

    synchronized void sound(long started, long ended, long id, boolean threw, float pitch) {
        if (closed) return;
        playAttempts++;
        if (threw) playExceptions++;
        else if (id == -1) playRejected++;
        lastPitch = pitch;
        play.add(Math.max(0, ended - started));
    }

    synchronized Snapshot snapshot() {
        double seconds = sampleRate > 0 ? frames / (double) sampleRate : 0;
        double elapsed = haveWrite ? Math.max(0, lastEnd - firstStart) / 1_000_000_000.0 : 0;
        return new Snapshot(attempts, completed, failed, cancelled, frames, seconds, elapsed,
                elapsed > 0 ? seconds / elapsed : 0, mixOverBudget, producerGapOverBudget,
                playAttempts, playRejected, playExceptions, lastPitch, lastStretch,
                mix.snapshot(), write.snapshot(), producerGap.snapshot(), play.snapshot());
    }

    private void report() {
        Snapshot snapshot = snapshot(); // Formatting and logging never hold the producer's monitor.
        Logger.getGlobal().info("Audio diagnostics [cumulative, writeReturnNotAudible=true, final=" + closed + "] "
                + description + " " + snapshot);
    }

    @Override public synchronized void close() {
        closed = true;
        if (reporter != null) reporter.interrupt();
    }

    record Snapshot(long writes, long returned, long failed, long cancelled, long returnedFrames,
            double submittedSeconds, double producerElapsedSeconds, double submissionRatio,
            long mixOverBudget, long producerGapOverBudget, long playAttempts, long playRejected,
            long playExceptions, float lastPitch, float lastStretch,
            Times mix, Times write, Times producerGap, Times play) { }
    record Times(long count, double meanUs, long maxUs, long p95UpperUs) { }

    private static final class Timing {
        private final long[] buckets = new long[32];
        private long count, total, max;
        void add(long nanos) {
            count++; total += nanos; max = Math.max(max, nanos);
            long us = nanos / 1000 + (nanos % 1000 == 0 ? 0 : 1);
            int bucket = us <= 1 ? 0 : Math.min(31, 64 - Long.numberOfLeadingZeros(us - 1));
            buckets[bucket]++;
        }
        Times snapshot() {
            long rank = (long) Math.ceil(count * 0.95), seen = 0, p95 = 0;
            if (count > 0) for (int i = 0; i < buckets.length; i++) {
                seen += buckets[i];
                if (seen >= rank) { p95 = i == 31 ? Long.MAX_VALUE : 1L << i; break; }
            }
            return new Times(count, count > 0 ? total / (double) count / 1000 : 0, max / 1000, p95);
        }
    }
}
