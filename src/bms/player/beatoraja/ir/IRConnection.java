package bms.player.beatoraja.ir;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.TableData;

/**
 * IR接続用インターフェイス
 * 
 * @author exch
 */
public interface IRConnection {

	/**
	 * IRに新規ユーザー登録する。
	 * 
	 * @param id
	 *            ユーザーID
	 * @param pass
	 *            パスワード
	 * @param name
	 *            ユーザー名
	 * @return
	 */
	public IRResponse<Object> register(String id, String pass, String name);

	/**
	 * IRにログインする。起動時に呼び出される
	 * 
	 * @param id
	 *            ユーザーID
	 * @param pass
	 *            パスワード
	 */
	public IRResponse<Object> login(String id, String pass);

	/**
	 * ライバルデータを収録する
	 * 
	 * @return ライバルデータ
	 */
	public IRResponse<PlayerInformation[]> getRivals();

	/**
	 * IRに設定されている表データを収録する
	 * 
	 * @return IRで取得可能な表データ
	 */
	public IRResponse<TableData[]> getTableDatas();

	/**
	 * スコアデータを取得する
	 * 
	 * @param id
	 *            ユーザーID。譜面に登録されているスコアデータを全取得する場合はnullを入れる
	 * @param model
	 *            スコアデータを取得する譜面。ユーザーIDのスコアデータを全取得する場合はnullを入れる
	 * @return
	 */
	public IRResponse<IRScoreData[]> getPlayData(String id, IRChartData model);

	public IRResponse<IRScoreData[]> getCoursePlayData(String id, CourseData course, int lnmode);

	/**
	 * スコアデータを送信する
	 * 
	 * @param model
	 *            楽曲データ
	 * @param score
	 *            スコア
	 * @return 送信結果
	 */
	public IRResponse<Object> sendPlayData(IRChartData model, IRScoreData score);

	/**
	 * コーススコアデータを送信する
	 * 
	 * @param course
	 *            コースデータ
	 * @param lnmode
	 *            LNモード
	 * @param score
	 *            スコア
	 * @return 送信結果
	 */
	public IRResponse<Object> sendCoursePlayData(CourseData course, int lnmode, IRScoreData score);

	/**
	 * 楽曲のURLを取得する
	 * 
	 * @param song
	 *            譜面データ
	 * @return 楽曲URL。存在しない場合はnull
	 */
	public String getSongURL(IRChartData chart);

	/**
	 * コースのURLを取得する
	 * 
	 * @param course
	 *            コースデータ
	 * @return コースURL。存在しない場合はnull
	 */
	public String getCourseURL(CourseData course);

	/**
	 * プレイヤーURLを取得する
	 * 
	 * @param id
	 *            ユーザーID
	 * @return
	 */
	public String getPlayerURL(String id);

}
