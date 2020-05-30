package bms.player.beatoraja.song;

/**
 * 楽曲データベースへのアクセスインターフェイス
 * 
 * @author exch
 */
public interface SongDatabaseAccessor {

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @return 検索結果
	 */
	public SongData[] getSongDatas(String key, String value);

	/**
	 * MD5/SHA256で指定した楽曲をまとめて取得する
	 * 
	 * @param hashes
	 *            楽曲のハッシュ
	 * @return
	 */
	public SongData[] getSongDatas(String[] hashes);

	/**
	 * スコアデータベース、スコアログデータベース、譜面情報データベースを跨いでSQLで問い合わせを行う
	 * 
	 * @param sql
	 *            SQL
	 * @param score
	 *            スコアデータベースのパス
	 * @param scorelog
	 *            スコアログデータベースのパス
	 * @param info
	 *            譜面情報データベースのパス
	 * @return
	 */
	public SongData[] getSongDatas(String sql, String score, String scorelog, String info);

	public void setSongDatas(SongData[] songs);

	public SongData[] getSongDatasByText(String text);

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @return 検索結果
	 */
	public FolderData[] getFolderDatas(String key, String value);

	/**
	 * データベースを更新する
	 * 
	 * @param updatepath
	 *            更新するフォルダのパス。全更新する場合はnull
	 * @param updateAll
	 *            更新の必要がないものも更新するかどうか
	 */
	public void updateSongDatas(String updatepath, String[] bmsroot, boolean updateAll, SongInformationAccessor info);

}
