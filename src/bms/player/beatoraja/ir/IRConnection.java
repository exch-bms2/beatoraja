package bms.player.beatoraja.ir;

/**
 * IR接続用インターフェイス
 * 
 * @author exch
 */
public interface IRConnection {

	/**
	 * IRに新規ユーザー登録する。
	 * 
	 * @param accout
	 *            アカウント情報
	 * @return
	 */
	public IRResponse<IRPlayerData> register(IRAccount account);

	/**
	 * IRにログインする。起動時に呼び出される
	 * 
	 * @param accout
	 *            アカウント情報
	 * @return
	 */
	public IRResponse<IRPlayerData> login(IRAccount account);

	/**
	 * ライバルデータを収録する
	 * 
	 * @return ライバルデータ
	 */
	public IRResponse<IRPlayerData[]> getRivals();

	/**
	 * IRに設定されている表データを収録する
	 * 
	 * @return IRで取得可能な表データ
	 */
	public IRResponse<IRTableData[]> getTableDatas();

	/**
	 * スコアデータを取得する
	 * 
	 * @param id
	 *            ユーザー。譜面に登録されているスコアデータを全取得する場合はnullを入れる
	 * @param model
	 *            スコアデータを取得する譜面。ユーザーIDのスコアデータを全取得する場合はnullを入れる
	 * @return
	 */
	public IRResponse<IRScoreData[]> getPlayData(IRPlayerData player, IRChartData chart);

	public IRResponse<IRScoreData[]> getCoursePlayData(IRPlayerData player, IRCourseData course);

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
	public IRResponse<Object> sendCoursePlayData(IRCourseData course, IRScoreData score);

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
	public String getCourseURL(IRCourseData course);

	/**
	 * プレイヤーURLを取得する
	 * 
	 * @param id
	 *            ユーザーID
	 * @return
	 */
	public String getPlayerURL(IRPlayerData player);

	/**
	 * IRが提供するbeatorajaのバージョン情報を取得する。
	 * <p>
	 * 未対応のIR実装はデフォルトで失敗レスポンスを返す。
	 * </p>
	 *
	 * @param currentVersion
	 *            現在のbeatorajaバージョン
	 * @return バージョン情報
	 */
	public default IRResponse<IRVersionInfo> getVersionInfo(String currentVersion) {
		return new SimpleIRResponse<>(false, "Not supported", null);
	}

	/**
	 * IRが起動拒否対象として扱うBMSのSHA256一覧を取得する。
	 * <p>
	 * 未対応のIR実装はデフォルトで失敗レスポンスを返す。
	 * </p>
	 *
	 * @return 起動拒否対象のSHA256一覧
	 */
	public default IRResponse<String[]> getIllegalSongs() {
		return new SimpleIRResponse<>(false, "Not supported", new String[0]);
	}

}
