package bms.player.beatoraja;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;

/** Synthetic clocks only: no FPS sleeps, GL context or player data. */
public final class FramePacingDiagnosticsTest {
    private static final FramePacingDiagnostics.Context ACTIVE = context("PLAY", true, false);
    public static void main(String[] args) throws Exception {
        String old = System.getProperty("beatoraja.frameDiagnostics");
        String interval = System.getProperty("beatoraja.frameDiagnostics.intervalMillis");
        try {
            System.clearProperty("beatoraja.frameDiagnostics");
            check(FramePacingDiagnostics.createIfEnabled(new Config()) == null, "Disabled collector is absent");
            var listener = new ApplicationAdapter() { };
            check(FramePacingDiagnostics.wrap(listener, null, () -> { throw new AssertionError(); }) == listener,
                    "Disabled mode returns original listener; no clock or state probes");
            System.setProperty("beatoraja.frameDiagnostics.intervalMillis", "invalid");
            check(FramePacingDiagnostics.intervalMillis() == 5000, "Default invalid interval");
            System.setProperty("beatoraja.frameDiagnostics.intervalMillis", "1");
            check(FramePacingDiagnostics.intervalMillis() == 1000, "Minimum interval");
            System.setProperty("beatoraja.frameDiagnostics.intervalMillis", "999999");
            check(FramePacingDiagnostics.intervalMillis() == 60000, "Maximum interval");
            accounting(); lifecycle(); concurrency();
        } finally { restore("beatoraja.frameDiagnostics", old); restore("beatoraja.frameDiagnostics.intervalMillis", interval); }
        System.out.println("FramePacingDiagnosticsTest passed (synthetic intervals, jitter, boundaries, input age and listener compatibility)");
    }
    private static void accounting() {
        var d = diagnostics();
        d.frame(0, ms(2), ACTIVE, false);
        d.frame(ms(10), ms(13), ACTIVE, false);
        d.frame(ms(30), ms(34), ACTIVE, true);
        var s = d.snapshot();
        check(s.frames() == 3 && s.failures() == 1, "Count all renders, including failure");
        near(s.focused().interval().meanMs(), 15); near(s.focused().interval().stddevMs(), 5);
        near(s.focused().interval().p95UpperMs(), 20); near(s.focused().render().meanMs(), 3);
        near(s.focused().outsideRender().meanMs(), 12.5); near(s.focused().intervalDelta().maxMs(), 10);
        near(FramePacingDiagnostics.nominalNanos(true, 240, 60), 1e9 / 60);
        near(FramePacingDiagnostics.nominalNanos(true, 30, 144), 1e9 / 30);
        near(FramePacingDiagnostics.nominalNanos(false, 0, 60), 0);
        d.boundary();
        d.frame(ms(100), ms(102), ACTIVE, false);
        check(d.snapshot().focused().interval().count() == 2, "Resize/pause boundary not pooled into steady intervals");
        d.frame(ms(120), ms(122), context("PLAY", false, false), false);
        d.frame(ms(140), ms(142), context("PLAY", false, false), false);
        d.frame(ms(200), ms(202), context("PLAY", false, true), false);
        d.frame(ms(270), ms(272), context("PLAY", false, true), false);
        check(d.snapshot().unfocused().interval().count() == 1 && d.snapshot().minimized().interval().count() == 1,
                "Separate focus/minimized groups");
        d.inputUpdated(0, ms(1)); d.workerPoll(ms(3));
        d.inputUpdated(ms(9), ms(11)); d.workerPoll(ms(15));
        s = d.snapshot();
        near(s.inputUpdateGap().meanMs(), 10); near(s.inputUpdateTime().meanMs(), 1.5);
        near(s.workerPollGap().meanMs(), 12); near(s.publicationAgeAtPoll().meanMs(), 3);
        var before = d.snapshot();
        for (int i = 1; i <= 40; i++) d.frame(ms(1000 + i * 100), ms(1002 + i * 100), ACTIVE, false);
        s = d.snapshot();
        check(s.recentLongFrames().size() == 16 && s.evictedLongFrames() == s.longFrames() - 16, "Long-frame ring bounded");
        check(s.recentLongFrames().get(15).sequence() == s.longFrames(), "Keep newest long frames in order");
        check(before.frames() != s.frames(), "Snapshot immutable");
        try { s.recentLongFrames().clear(); throw new AssertionError(); } catch (UnsupportedOperationException expected) { }
        d.close(); d.frame(ms(10000), ms(10001), ACTIVE, false);
        check(d.snapshot().frames() == s.frames(), "Ignore post-close observations");
    }
    private static void lifecycle() throws Exception {
        var d = diagnostics();
        var calls = new ArrayList<String>();
        var clock = new AtomicLong();
        var failure = new IllegalStateException("render failed");
        var listener = new ApplicationAdapter() {
            @Override public void create() { calls.add("create"); }
            @Override public void resize(int w, int h) { calls.add(w + "x" + h); }
            @Override public void render() { calls.add("render"); clock.addAndGet(ms(4)); throw failure; }
            @Override public void pause() { calls.add("pause"); }
            @Override public void resume() { calls.add("resume"); }
            @Override public void dispose() { calls.add("dispose"); }
        };
        ApplicationListener wrapped = d.wrapWithClock(listener, clock::get, () -> ACTIVE);
        wrapped.create(); wrapped.resize(320, 200);
        try { wrapped.render(); throw new AssertionError(); } catch (IllegalStateException e) { check(e == failure, "Same render exception"); }
        wrapped.pause(); wrapped.resume(); wrapped.dispose(); d.close();
        check(calls.equals(List.of("create", "320x200", "render", "pause", "resume", "dispose")), "Forward callbacks once in order");
        near(d.snapshot().focused().render().meanMs(), 4);
        var field = FramePacingDiagnostics.class.getDeclaredField("reporter"); field.setAccessible(true);
        Thread reporter = (Thread) field.get(d); reporter.join(2000);
        check(reporter.isDaemon() && !reporter.isAlive(), "Reporter ends without interval wait");
        var failed = diagnostics();
        var broken = failed.wrapWithClock(new ApplicationAdapter() {
            @Override public void create() { throw failure; }
        }, clock::get, () -> ACTIVE);
        try { broken.create(); throw new AssertionError(); } catch (IllegalStateException e) { check(e == failure, "Same init exception"); }
        failed.start(); check(field.get(failed) == null, "No reporter after failed initialization");
        var disposeFailure = diagnostics();
        var brokenDispose = disposeFailure.wrapWithClock(new ApplicationAdapter() {
            @Override public void dispose() { throw failure; }
        }, clock::get, () -> ACTIVE);
        try { brokenDispose.dispose(); throw new AssertionError(); } catch (IllegalStateException e) { check(e == failure, "Same dispose exception"); }
        disposeFailure.start(); check(field.get(disposeFailure) == null, "Close despite delegate disposal failure");
    }
    private static void concurrency() throws Exception {
        var d = diagnostics();
        Thread worker = new Thread(() -> { for (int i = 0; i < 1000; i++) d.workerPoll(i); });
        worker.start();
        for (int i = 0; i < 1000; i++) { d.frame(ms(i * 10), ms(i * 10 + 1), ACTIVE, false); d.inputUpdated(i, i + 1); }
        worker.join();
        check(d.snapshot().frames() == 1000 && d.snapshot().workerPollGap().count() == 999, "Concurrent observations retained");
        d.close();
    }
    private static FramePacingDiagnostics diagnostics() { return new FramePacingDiagnostics(true, 240, 60000, ms(50)); }
    private static FramePacingDiagnostics.Context context(String state, boolean focus, boolean minimized) {
        return new FramePacingDiagnostics.Context(state, focus, minimized, false, 1280, 720, 60);
    }
    private static long ms(long n) { return n * 1_000_000L; }
    private static void restore(String key, String value) { if (value == null) System.clearProperty(key); else System.setProperty(key, value); }
    private static void near(double actual, double expected) { check(Math.abs(actual - expected) < .000001, actual + " != " + expected); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
