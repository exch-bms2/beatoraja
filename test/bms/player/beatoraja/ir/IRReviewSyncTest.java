package bms.player.beatoraja.ir;

import bms.player.beatoraja.IRConfig;
import bms.player.beatoraja.MainController.IRStatus;
import com.badlogic.gdx.utils.Json;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class IRReviewSyncTest {
    public static void main(String[] args) throws Exception {
        var json = IsolatedIRConnection.createJson();
        var primary = json.fromJson(IRConfig.class, "{irname:primary}");
        var secondary = new IRConfig();
        secondary.setIrname("secondary");
        secondary.setSyncReviewLevel(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new LinkedBlockingQueue<IRReview>();
        Thread caller = Thread.currentThread();
        String a = "a".repeat(64);
        String b = "b".repeat(64);
        var connection = new TestIRConnectionBase() {
            @Override public IRResponse<Object> setReview(IRReview review) {
                check(Thread.currentThread() != caller, "Review never runs on the game thread");
                calls.add(review);
                if (entered.getCount() != 0) {
                    entered.countDown();
                    try {
                        check(release.await(5, TimeUnit.SECONDS), "Fixture release timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return IRSubmissionResponse.unknown("fixture timeout");
                }
                return new SimpleIRResponse<>(true, "", null);
            }
        };
        var wrong = new IRStatus(secondary, new TestIRConnectionBase() {
            @Override public IRResponse<Object> setReview(IRReview review) {
                throw new AssertionError("Never upload to a secondary IR");
            }
        }, null);
        var right = new IRStatus(primary, connection, null);
        var configured = new IRConfig[]{primary, secondary};
        check(!primary.isSyncReviewLevel() && IRReviewSync.forPrimary(configured, new IRStatus[]{right}) == null,
                "Older configurations default to opt-out");
        primary.setSyncReviewLevel(true);
        check(new Json().fromJson(IRConfig.class, json.toJson(primary)).isSyncReviewLevel(), "Opt-in is persisted");
        check(IRReviewSync.forPrimary(configured, new IRStatus[]{wrong}) == null,
                "A failed Primary login never falls back");
        var sync = IRReviewSync.forPrimary(configured, new IRStatus[]{wrong, right});
        try (sync) {
            sync.offerLevel(a.toUpperCase(java.util.Locale.ROOT), 1.0, 0);
            check(entered.await(5, TimeUnit.SECONDS), "First review entered the sender");
            check(take(calls).equals(new IRReview(a, 1.0, null, null, 0)), "Only level and score mode are sent");
            sync.offerLevel(a, 2.0, 0);
            sync.offerLevel(b, 4.5, 0);
            sync.offerLevel(a, 6.0, 2);
            sync.offerLevel(a, 0.0, 0);
            check(calls.isEmpty(), "No parallel review request");
            release.countDown();
            check(take(calls).equals(new IRReview(a, 0.0, null, null, 0)), "Only the latest pending value per score mode is sent");
            check(take(calls).equals(new IRReview(b, 4.5, null, null, 0)), "Other pending charts are retained");
            check(take(calls).equals(new IRReview(a, 6.0, null, null, 2)), "Other modes of the same chart are retained");
        }
        sync.offerLevel(a, 10, 0);
        check(calls.isEmpty(), "Closed synchronizer cannot accept edits");
        System.out.println("IRReviewSyncTest passed (opt-in, configured Primary only, async ordering, latest pending value, no retry)");
    }

    private static IRReview take(LinkedBlockingQueue<IRReview> calls) throws InterruptedException {
        IRReview review = calls.poll(5, TimeUnit.SECONDS);
        check(review != null, "Expected an IR review request");
        return review;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
