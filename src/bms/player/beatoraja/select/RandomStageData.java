package bms.player.beatoraja;

/**
 * ランダムコースのステージデータ
 */
public class RandomStageData {

	public static final RandomStageData[] EMPTY = new RandomStageData[0];

	/**
	 * ステージタイトル
	 */
	private String title;
	/**
	 * DBに対するSQL
	 */
	private String sql;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}
}
