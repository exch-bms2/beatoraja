package bms.player.beatoraja.ir;

/**
 * Failed submission whose delivery cannot be established. The legacy IRResponse
 * ABI remains unchanged; callers must not automatically retry this subtype.
 * Even an IR JAR's negative response may represent an HTTP timeout after commit.
 */
public final class IRSubmissionResponse implements IRResponse<Object> {
    private final String message;
    private IRSubmissionResponse(String message) { this.message = message; }
    public static IRSubmissionResponse unknown(String message) { return new IRSubmissionResponse(message); }
    public static boolean isOutcomeUnknown(IRResponse<?> response) { return response instanceof IRSubmissionResponse; }
    @Override public boolean isSucceeded() { return false; }
    @Override public String getMessage() { return message; }
    @Override public Object getData() { return null; }
}
