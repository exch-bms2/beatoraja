package bms.player.beatoraja.song;

/**
 * 楽曲データベースへのアクセスインターフェイス
 * 
 * @author exch
 */
public interface SongDatabaseAccessor {
	default SongData[] getAllSongDatas() { return getSongDatasByText(""); }

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
	default SongData[] getSongDatasForScoring(String sql, String score, String scorelog, String info, int lnmode) {
		return getSongDatas(sql, score, scorelog, info);
	}
	record ScoringSource(String rule, String score, String scorelog) {}
	default SongData[] getSongDatasForScoring(String sql, java.util.List<ScoringSource> sources,
			java.util.Map<String, String> rules, String info, int lnmode) {
		var results = new java.util.LinkedHashMap<String, SongData>();
		for (var source : sources) for (var song : getSongDatasForScoring(sql, source.score(), source.scorelog(), info, lnmode)) {
			if (source.rule().equals(rules.get(song.getSha256()))) results.put(song.getSha256() + song.getPath(), song);
		}
		return results.values().toArray(SongData[]::new);
	}

	public void setSongDatas(SongData[] songs);

	public default void setSongReviews(SongData[] songs) {
		setSongDatas(songs);
	}

	/** Updates the current player's review level without modifying song metadata. */
	default boolean setSongReviewLevel(SongData song, double level) {
		return false;
	}

	default SongReview getSongReview(String sha256) {
		return null;
	}

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
