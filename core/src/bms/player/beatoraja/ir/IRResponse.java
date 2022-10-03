package bms.player.beatoraja.ir;

/**
 * IRからのレスポンスデータのインターフェイス
 * 
 * @author exch
 *
 * @param <T>
 */
public interface IRResponse<T> {
	
	/**
	 * IRへの操作が成功したかどうか。
	 * 
	 * @return 成功した場合はtrue
	 */
	public abstract boolean isSucceeded();
	
	/**
	 * IRからのメッセージを取得する
	 * 
	 * @return IRからのメッセージ
	 */
	public abstract String getMessage();
	
	/**
	 * IRからのデータを取得する
	 * 
	 * @return IRからのデータ
	 */
	public abstract T getData();
}
