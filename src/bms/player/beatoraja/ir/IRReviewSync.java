package bms.player.beatoraja.ir;

import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.MainController.IRStatus;

import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/** Serial, best-effort upload of locally committed review levels. Never replays a request. */
public final class IRReviewSync implements AutoCloseable {
    private static final int MAX_PENDING = 128;
    private final IRConnection connection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ir-review-sync");
        thread.setDaemon(true);
        return thread;
    });
    private record ScoreKey(String sha256, int lnmode) {}

    private final LinkedHashMap<ScoreKey, IRReview> pending = new LinkedHashMap<>();
    private boolean draining, closed, unsupported;

    private IRReviewSync(IRConnection connection) {
        this.connection = connection;
    }

    /** Matches the configured Primary, not the first successful login. */
    public static IRReviewSync forPrimary(IRConfig[] configured, IRStatus[] loggedIn) {
        if (configured == null || configured.length == 0 || configured[0] == null
                || !configured[0].isSyncReviewLevel()) return null;
        for (IRStatus status : loggedIn) {
            if (status.config == configured[0]) return new IRReviewSync(status.connection);
        }
        return null;
    }

    public synchronized void offerLevel(String sha256, double level, int lnmode) {
        if (closed || unsupported) return;
        IRReview update;
        try {
            update = new IRReview(sha256, level, null, null, lnmode);
        } catch (IllegalArgumentException invalid) {
            Logger.getGlobal().warning("IR review level not queued: " + invalid.getMessage());
            return;
        }
        var key = new ScoreKey(update.sha256(), lnmode);
        if (pending.size() >= MAX_PENDING && !pending.containsKey(key)) {
            Logger.getGlobal().warning("IR review queue is full; review remains saved locally");
            return;
        }
        pending.put(key, update);
        if (!draining) {
            draining = true;
            executor.execute(this::drain);
        }
    }

    private void drain() {
        while (true) {
            IRReview update;
            synchronized (this) {
                if (closed || pending.isEmpty()) {
                    draining = false;
                    return;
                }
                var next = pending.values().iterator();
                update = next.next();
                next.remove();
            }
            try {
                IRResponse<Object> response = connection.setReview(update);
                if (response == IRReview.UNSUPPORTED) {
                    synchronized (this) {
                        unsupported = true;
                        pending.clear();
                        draining = false;
                    }
                    Logger.getGlobal().info("Primary IR does not support review updates; reviews remain local");
                    return;
                }
                if (response == null || !response.isSucceeded()) {
                    String reason = IRSubmissionResponse.isOutcomeUnknown(response)
                            ? "result unknown; not retrying" : "request failed";
                    Logger.getGlobal().warning("IR review sync: " + reason + "; review remains saved locally");
                }
            } catch (RuntimeException | LinkageError failure) {
                Logger.getGlobal().warning("IR review sync failed; review remains saved locally: "
                        + failure.getClass().getSimpleName());
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        pending.clear();
        executor.shutdownNow();
    }
}
