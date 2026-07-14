package bms.player.beatoraja.ir;

/**
 * workerから戻ってきた結果を既存の{@link IRResponse}として扱うための単純な値実装。
 *
 * @param <T> レスポンスデータの型
 */
final class SimpleIRResponse<T> implements IRResponse<T> {

	private final boolean succeeded;
	private final String message;
	private final T data;

	SimpleIRResponse(boolean succeeded, String message, T data) {
		this.succeeded = succeeded;
		this.message = message != null ? message : "";
		this.data = data;
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
	public T getData() {
		return data;
	}
}
