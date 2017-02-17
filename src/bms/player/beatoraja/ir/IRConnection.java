package bms.player.beatoraja.ir;

import bms.model.BMSModel;
import bms.player.beatoraja.IRScoreData;

/**
 * IR接続用インターフェイス
 * 
 * @author exch
 */
public interface IRConnection {
	
	public static final String[] AVAILABLE = {};

	public void register(String id, String pass, String name);

	/**
	 * IRにログインする。起動時に呼び出される
	 * @param id ユーザーID
	 * @param pass パスワード
	 */
	public void login(String id, String pass);

	/**
	 * スコアデータを取得する
	 * @param id ユーザーID。譜面に登録されているスコアデータを全取得する場合はnullを入れる
	 * @param model スコアデータを取得する譜面。ユーザーIDのスコアデータを全取得する場合はnullを入れる
	 * @return
	 */
	public IRScoreData[] getPlayData(String id, BMSModel model);

	/**
	 * スコアデータを送信する
	 * @param model
	 * @param score
	 */
	public void sendPlayData(BMSModel model, IRScoreData score);
	
	public static IRConnection getIRConnection(String name) {
		return null;		
	}
}
