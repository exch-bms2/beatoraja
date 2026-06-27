package bms.player.beatoraja.song;

/**
 * ユーザーの楽曲評価情報
 * 
 * @author exch
 */
public class SongReview {

	/**
	 * 譜面のハッシュ値
	 */
	private String sha256;

	private String tag = "";

	private int favorite;

	private double levelreview;
	
	private String comment = "";

	public static final int FAVORITE_SONG = 1;
	public static final int FAVORITE_CHART = 2;
	public static final int INVISIBLE_SONG = 4;
	public static final int INVISIBLE_CHART = 8;

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public int getFavorite() {
		return favorite;
	}

	public void setFavorite(int favorite) {
		this.favorite = favorite;
	}

	public double getLevelreview() {
		return levelreview;
	}

	public void setLevelreview(double levelreview) {
		this.levelreview = levelreview;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
