package bms.player.beatoraja.ir;

/** Worker-side fixture that records only the received review payload. */
public final class ReviewRecoveryIRConnection extends RecoveryIRConnection {
    public static final String NAME = "Recovery fixture review";

    @Override
    public IRResponse<Object> setReview(IRReview review) {
        return submission("review:" + review.sha256() + ":" + review.level() + ":" + review.comment() + ":" + review.favorite()
                + ":" + review.lnmode());
    }
}
