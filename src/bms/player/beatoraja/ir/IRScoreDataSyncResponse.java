package bms.player.beatoraja.ir;

/**
 * Score synchronization response with a server-issued revision cursor.
 */
public final class IRScoreDataSyncResponse implements IRResponse<IRScoreData[]> {

	private final boolean succeeded;
	private final String message;
	private final IRScoreData[] data;
	private final long scoreRevision;
	private final boolean hasMore;

	public IRScoreDataSyncResponse(boolean succeeded, String message, IRScoreData[] data, long scoreRevision, boolean hasMore) {
		this.succeeded = succeeded;
		this.message = message != null ? message : "";
		this.data = data != null ? data : new IRScoreData[0];
		this.scoreRevision = scoreRevision;
		this.hasMore = hasMore;
	}

	@Override
	public boolean isSucceeded() {
		return succeeded;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public IRScoreData[] getData() {
		return data;
	}

	public long getScoreRevision() {
		return scoreRevision;
	}

	public boolean hasMore() {
		return hasMore;
	}
}
