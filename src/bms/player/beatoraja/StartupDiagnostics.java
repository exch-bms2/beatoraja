package bms.player.beatoraja;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/** Opt-in timings with fixed labels; no request, response or account data. */
public final class StartupDiagnostics {
    static final String PROPERTY = "beatoraja.startupDiagnostics";
    private static final String TOKEN = "BEATORAJA_STARTUP_TRACE";
    private static final String EPOCH = "BEATORAJA_STARTUP_EPOCH_MS";
    private static final Launch inherited = readLaunch(System.getenv(TOKEN), System.getenv(EPOCH));
    private static volatile Launch launch;
    private static boolean firstFrame, inputReady;

    public enum Step {
        START, GAME_CHILD_START, GAME_ENTRY, SELECT_FIRST_FRAME, SELECT_INPUT_READY,
        SYSTEM_FONT, INPUT_DEVICES, SELECT_BAR_INIT, SELECT_BAR_BUILD, SELECT_SKIN_LOAD,
        IR_METADATA, IR_METADATA_WORKER_START, IR_METADATA_RESPONSE_WAIT, IR_METADATA_EXIT_WAIT,
        IR_METADATA_CLEANUP, IR_CHART_CHECK, IR_CHART_DB_LOOKUP, IR_LOGIN, IR_REQUEST_LOCK_WAIT,
        IR_WORKER_START, IR_RESPONSE_WAIT, IR_WORKER_EXIT_WAIT
    }

    public enum Outcome { OK, FAILED, TIMEOUT, INTERRUPTED, SKIPPED }

    record Launch(UUID id, long epochMillis) { }

    private StartupDiagnostics() { }

    static void start() {
        if (!enabled()) return;
        launch = new Launch(UUID.randomUUID(), System.currentTimeMillis());
        milestone(Step.START);
    }

    static void configureGameEnvironment(Map<String, String> environment) {
        if (!enabled()) return;
        if (launch == null) start();
        environment.put(TOKEN, launch.id().toString());
        environment.put(EPOCH, Long.toString(launch.epochMillis()));
    }

    static void gameEntry() {
        if (enabled()) milestone(Step.GAME_ENTRY);
    }

    static Launch readLaunch(String id, String epoch) {
        if (id == null || epoch == null) return null;
        try { return new Launch(UUID.fromString(id), Long.parseLong(epoch)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    static void selectionFrame(boolean unlocked) {
        if ((firstFrame && (inputReady || !unlocked)) || !enabled()) return;
        if (!firstFrame) {
            firstFrame = true;
            milestone(Step.SELECT_FIRST_FRAME);
        }
        if (unlocked && !inputReady) {
            inputReady = true;
            milestone(Step.SELECT_INPUT_READY);
        }
    }

    static boolean enabled() { return Boolean.getBoolean(PROPERTY); }

    private static void milestone(Step step) { log(step, null, -1, -1); }

    public static Span measure(Step step) { return measure(step, -1); }
    public static Span measure(Step step, boolean include) { return include ? measure(step) : Span.DISABLED; }
    public static Span measure(Step step, long workerPid) {
        return enabled() ? new Span(step, workerPid) : Span.DISABLED;
    }

    private static void log(Step step, Outcome outcome, long durationMicros, long workerPid) {
        Launch current = launch != null ? launch : inherited;
        Logger.getGlobal().info("Startup diagnostics: step=" + step
                + " pid=" + ProcessHandle.current().pid()
                + (current == null ? "" : " trace=" + current.id()
                    + " sinceStartMs=" + (System.currentTimeMillis() - current.epochMillis()))
                + (outcome == null ? "" : " outcome=" + outcome + " durationUs=" + durationMicros)
                + (workerPid < 0 ? "" : " workerPid=" + workerPid));
    }

    public static final class Span implements AutoCloseable {
        private static final Span DISABLED = new Span(null, -1);
        private final Step step;
        private final long startNanos;
        private long workerPid;
        private Outcome outcome = Outcome.FAILED;

        private Span(Step step, long workerPid) {
            this.step = step;
            this.workerPid = workerPid;
            startNanos = step == null ? 0 : System.nanoTime();
        }

        public void worker(long pid) { if (step != null) workerPid = pid; }
        public void outcome(Outcome result) { if (step != null) outcome = result; }
        @Override public void close() {
            if (step != null) log(step, outcome, (System.nanoTime() - startNanos) / 1000, workerPid);
        }
    }
}
