package bms.player.beatoraja.song;

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
	 * @param lr2path
	 * @return
	 */
	public SongData[] getSongDatas(String[] hashes);

	public SongData[] getSongDatasByText(String text, String lr2path);

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @param lr2path
	 *            LR2ルートパス
	 * @return 検索結果
	 */
	public FolderData[] getFolderDatas(String key, String value);

	/**
	 * データベースを更新する
	 * 
	 * @param rootdirs
	 *            楽曲のルートパス
	 * @param path
	 *            LR2のルートパス
	 * @param updateAll
	 *            更新の必要がないものも更新するかどうか
	 */
	public void updateSongDatas(String[] rootdirs, String path, boolean updateAll);

}
