package bms.player.beatoraja;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;

/** Opt-in CPU-side observations; never changes swap interval, FPS, input clocks or audio pacing. */
public final class FramePacingDiagnostics implements AutoCloseable {
    private final boolean requestedVsync;
    private final int configuredFps;
    private final long intervalMillis, longFrameNanos;
    private final Group[] groups = {new Group(), new Group(), new Group()};
    private final Timing inputUpdateGap = new Timing(), inputUpdateTime = new Timing();
    private final Timing workerPollGap = new Timing(), publicationAgeAtPoll = new Timing();
    private final Event[] recent = new Event[16];
    private long frames, failures, longFrames, boundaries, transitions;
    private long previousStart, previousEnd, previousInterval, previousEpoch, lastUpdate, lastPoll;
    private boolean haveFrame, haveInterval, haveUpdate, havePoll;
    private Context previousContext, context;
    private int refreshRate;
    private long refreshAt;
    private boolean haveRefresh;
    private volatile boolean closed;
    private Thread reporter;

    public static FramePacingDiagnostics createIfEnabled(Config config) {
        return Boolean.getBoolean("beatoraja.frameDiagnostics")
                ? new FramePacingDiagnostics(config.isVsync(), config.getMaxFramePerSecond(),
                        intervalMillis(), Math.max(5, Math.min(1000,
                                Long.getLong("beatoraja.frameDiagnostics.longFrameMillis", 50L))) * 1_000_000L) : null;
    }

    static long intervalMillis() {
        return Math.max(1000, Math.min(60000, Long.getLong("beatoraja.frameDiagnostics.intervalMillis", 5000L)));
    }

    FramePacingDiagnostics(boolean vsync, int fps, long intervalMillis, long longFrameNanos) {
        requestedVsync = vsync; configuredFps = fps;
        this.intervalMillis = intervalMillis; this.longFrameNanos = longFrameNanos;
        for (int i = 0; i < recent.length; i++) recent[i] = new Event();
    }

    public static ApplicationListener wrap(ApplicationListener listener, FramePacingDiagnostics diagnostics,
            Supplier<String> state) {
        return diagnostics == null ? listener : diagnostics.wrapWithClock(listener, System::nanoTime,
                () -> diagnostics.observeWindow(state.get()));
    }

    ApplicationListener wrapWithClock(ApplicationListener listener, LongSupplier clock, Supplier<Context> environment) {
        return new ApplicationListener() {
            @Override public void create() {
                try { listener.create(); start(); }
                catch (RuntimeException | Error e) { close(); throw e; }
            }
            @Override public void render() {
                Context current = environment.get();
                long started = clock.getAsLong();
                boolean failed = true;
                try { listener.render(); failed = false; }
                finally { frame(started, clock.getAsLong(), current, failed); }
            }
            @Override public void resize(int w, int h) { boundary(); listener.resize(w, h); }
            @Override public void pause() { boundary(); listener.pause(); }
            @Override public void resume() { boundary(); listener.resume(); }
            @Override public void dispose() { try { listener.dispose(); } finally { close(); } }
        };
    }

    private Context observeWindow(String state) {
        if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics))
            return updateContext(state, false, false, false, 0, 0, 0);
        var window = graphics.getWindow();
        long now = System.nanoTime();
        // Display-mode queries may allocate; do not query them on every high-FPS frame.
        if (!haveRefresh || now - refreshAt >= 1_000_000_000L) {
            refreshRate = GlfwWindowSupport.refreshRate(graphics);
            refreshAt = now; haveRefresh = true;
        }
        return updateContext(state, window.isFocused(),
                GLFW.glfwGetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_ICONIFIED) != 0,
                graphics.isFullscreen(), graphics.getWidth(), graphics.getHeight(), refreshRate);
    }

    private synchronized Context updateContext(String state, boolean focus, boolean minimized,
            boolean fullscreen, int width, int height, int hz) {
        if (context == null || !context.state().equals(state) || context.focused() != focus
                || context.minimized() != minimized || context.fullscreen() != fullscreen
                || context.width() != width || context.height() != height || context.refreshHz() != hz)
            context = new Context(state, focus, minimized, fullscreen, width, height, hz);
        return context;
    }

    synchronized void boundary() { boundaries++; }

    synchronized void frame(long started, long ended, Context current, boolean failed) {
        if (closed) return;
        long duration = Math.max(0, ended - started);
        long gap = haveFrame ? Math.max(0, started - previousStart) : 0;
        long outside = haveFrame ? Math.max(0, started - previousEnd) : 0;
        Group group = groups[current.minimized() ? 2 : current.focused() ? 0 : 1];
        group.render.add(duration);
        if (haveFrame && current.equals(previousContext) && boundaries == previousEpoch) {
            group.interval.add(gap); group.outsideRender.add(outside);
            if (haveInterval) group.intervalDelta.add(Math.abs(gap - previousInterval));
            previousInterval = gap; haveInterval = true;
            double nominal = nominalNanos(requestedVsync, configuredFps, current.refreshHz());
            if (nominal > 0 && gap > nominal * 1.5) group.overNominal++;
        } else {
            if (haveFrame) transitions++;
            haveInterval = false;
        }
        if (duration >= longFrameNanos || (haveFrame && gap >= longFrameNanos)) {
            Event event = recent[(int) (longFrames % recent.length)];
            event.sequence = ++longFrames; event.start = started; event.end = ended;
            event.interval = gap; event.render = duration; event.outside = outside; event.context = current;
        }
        frames++; if (failed) failures++;
        previousStart = started; previousEnd = ended; previousContext = current; previousEpoch = boundaries;
        haveFrame = true;
    }

    /** Called after the existing owner-thread input publication, without changing its event timestamps. */
    public synchronized void inputUpdated(long started, long ended) {
        if (closed) return;
        inputUpdateTime.add(Math.max(0, ended - started));
        if (haveUpdate) inputUpdateGap.add(Math.max(0, ended - lastUpdate));
        lastUpdate = ended; haveUpdate = true;
    }

    /** Called on the existing game input polling thread; no GLFW or input mutation. */
    public synchronized void workerPoll(long now) {
        if (closed) return;
        if (havePoll) workerPollGap.add(Math.max(0, now - lastPoll));
        if (haveUpdate) publicationAgeAtPoll.add(Math.max(0, now - lastUpdate));
        lastPoll = now; havePoll = true;
    }

    static double nominalNanos(boolean vsync, int fps, int hz) {
        return Math.max(fps > 0 ? 1e9 / fps : 0, vsync && hz > 0 ? 1e9 / hz : 0);
    }

    synchronized Snapshot snapshot() {
        var events = new ArrayList<LongFrame>();
        for (long i = Math.max(0, longFrames - recent.length); i < longFrames; i++) {
            Event e = recent[(int) (i % recent.length)];
            events.add(new LongFrame(e.sequence, e.start, e.end, e.interval / 1e6,
                    e.render / 1e6, e.outside / 1e6, e.context));
        }
        return new Snapshot(frames, failures, longFrames, Math.max(0, longFrames - recent.length), boundaries, transitions,
                previousContext, groups[0].snapshot(), groups[1].snapshot(), groups[2].snapshot(),
                inputUpdateGap.snapshot(), inputUpdateTime.snapshot(), workerPollGap.snapshot(), publicationAgeAtPoll.snapshot(),
                List.copyOf(events));
    }

    synchronized void start() {
        if (reporter != null || closed) return;
        reporter = new Thread(() -> {
            try {
                Logger.getGlobal().info("Frame pacing diagnostics enabled : requestedVsync=" + requestedVsync
                        + ", configuredFps=" + configuredFps + ", idleFps=" + Math.max(1, configuredFps)
                        + ", longFrameMs=" + longFrameNanos / 1e6
                        + ", GPUTimeMeasured=false, swapWaitMeasured=false, hardwareLatencyMeasured=false");
                while (!closed) { Thread.sleep(intervalMillis); if (!closed) report(); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { report(); }
        }, "Frame pacing diagnostics");
        reporter.setDaemon(true); reporter.start();
    }

    private void report() {
        Snapshot s = snapshot();
        Logger.getGlobal().info("Frame pacing diagnostics [cumulative, final=" + closed + ", sampleNanos="
                + System.nanoTime() + ", requestedVsync=" + requestedVsync + ", configuredFps=" + configuredFps + "] " + s);
    }

    @Override public void close() {
        Thread thread;
        synchronized (this) { closed = true; thread = reporter; if (thread != null) thread.interrupt(); }
        // Give the reporter a bounded chance to emit the final sample before System.exit.
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public record Context(String state, boolean focused, boolean minimized, boolean fullscreen, int width, int height, int refreshHz) { }
    record LongFrame(long sequence, long startNanos, long endNanos, double intervalMs, double renderMs, double outsideRenderMs, Context context) { }
    record Times(long count, double meanMs, double stddevMs, double maxMs, double p50UpperMs, double p95UpperMs, double p99UpperMs) { }
    record GroupSnapshot(Times interval, Times render, Times outsideRender, Times intervalDelta, long overNominal) { }
    record Snapshot(long frames, long failures, long longFrames, long evictedLongFrames, long boundaries, long transitions,
            Context lastContext, GroupSnapshot focused, GroupSnapshot unfocused, GroupSnapshot minimized,
            Times inputUpdateGap, Times inputUpdateTime, Times workerPollGap, Times publicationAgeAtPoll, List<LongFrame> recentLongFrames) { }
    private static final class Event { long sequence, start, end, interval, render, outside; Context context; }
    private static final class Group {
        final Timing interval = new Timing(), render = new Timing(), outsideRender = new Timing(), intervalDelta = new Timing();
        long overNominal;
        GroupSnapshot snapshot() { return new GroupSnapshot(interval.snapshot(), render.snapshot(), outsideRender.snapshot(), intervalDelta.snapshot(), overNominal); }
    }
    private static final class Timing {
        private static final long BIN_NANOS = 125_000;
        final long[] buckets = new long[1025];
        long count, max;
        double mean, m2;
        void add(long nanos) {
            count++; max = Math.max(max, nanos);
            double delta = nanos - mean; mean += delta / count; m2 += delta * (nanos - mean);
            long bucket = nanos / BIN_NANOS + (nanos % BIN_NANOS == 0 ? 0 : 1);
            buckets[(int) Math.min(buckets.length - 1, bucket)]++;
        }
        double percentile(double p) {
            if (count == 0) return 0;
            long seen = 0, rank = (long) Math.ceil(count * p);
            for (int i = 0; i < buckets.length; i++) {
                seen += buckets[i];
                if (seen >= rank) return i == buckets.length - 1 ? max / 1e6 : i * BIN_NANOS / 1e6;
            }
            return 0;
        }
        Times snapshot() { return new Times(count, mean / 1e6, count > 0 ? Math.sqrt(Math.max(0, m2 / count)) / 1e6 : 0,
                max / 1e6, percentile(.5), percentile(.95), percentile(.99)); }
    }
}
