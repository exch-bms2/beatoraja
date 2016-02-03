package bms.player.lunaticrave2;

/**
 * プレイヤーデータ
 * 
 * @author ununique
 */
public class PlayerData {
	private static final long serialVersionUID = 1L;
	/**
	 * プレイヤーのLR2ID
	 */
	private long irid = 0;
	/**
	 * プレイヤーデータ取得日時(unixtime, 1日刻み)
	 */
	private long date = 0;
	
	private String logdate = "";
	/**
	 * 総プレイ回数
	 */
	private long playcount = 0;
	/**
	 * 総クリア回数
	 */
	private long clear = 0;
	/**
	 * 総Fail回数
	 */
	private long fail = 0;
	/**
	 * 総PGREATノート数
	 */
	private long perfect = 0;
	/**
	 * 総GREATノート数
	 */
	private long great = 0;
	/**
	 * 総GOODノート数
	 */
	private long good = 0;
	/**
	 * 総BADノート数
	 */
	private long bad = 0;
	/**
	 * 総POORノート数
	 */
	private long poor = 0;
	/**
	 * 総プレイ時間
	 */
	private long playtime = 0;
	/**
	 * ランキング更新数
	 */
	private long updateCount;
	/**
	 * 前回のプレイデータ
	 */
	private PlayerData prev;

	public PlayerData() {
	}

	public PlayerData(long irid, long playcount, long clear, long fail,
			long perfect, long great, long good, long bad, long poor,
			long playtime, long date) {
		setIrid(irid);
		setPlaycount(playcount);
		setClear(clear);
		setFail(fail);
		setPerfect(perfect);
		setGreat(great);
		setGood(good);
		setBad(bad);
		setPoor(bad);
		setPlaytime(playtime);
		setDate(date);
	}

	/**
	 * 前回のプレイデータを設定する
	 * 
	 * @param next
	 *            前回のプレイデータ
	 */
	public void setPrevPlayerData(PlayerData next) {
		this.prev = next;
		// プレイ回数が0の場合は前回のデータからコピーする
		if (playcount == 0) {
			playcount = next.playcount;
			clear = next.clear;
			fail = next.fail;
			perfect = next.perfect;
			great = next.great;
			good = next.good;
			bad = next.bad;
			poor = next.poor;
			playtime = next.playtime;
		}
	}

	public long getIrid() {
		return irid;
	}

	public void setIrid(long irid) {
		this.irid = irid;
	}

	public long getPlaycount() {
		return playcount;
	}

	public void setPlaycount(long playcount) {
		this.playcount = playcount;
	}

	public long getClear() {
		return clear;
	}

	public void setClear(long clear) {
		this.clear = clear;
	}

	public long getFail() {
		return fail;
	}

	public void setFail(long fail) {
		this.fail = fail;
	}

	public long getPerfect() {
		return perfect;
	}

	public void setPerfect(long perfect) {
		this.perfect = perfect;
	}

	public long getGreat() {
		return great;
	}

	public void setGreat(long great) {
		this.great = great;
	}

	public long getGood() {
		return good;
	}

	public void setGood(long good) {
		this.good = good;
	}

	public long getBad() {
		return bad;
	}

	public void setBad(long bad) {
		this.bad = bad;
	}

	public long getPoor() {
		return poor;
	}

	public void setPoor(long poor) {
		this.poor = poor;
	}

	public long getPlaytime() {
		return playtime;
	}

	public void setPlaytime(long playtime) {
		this.playtime = playtime;
	}

	public long getDate() {
		return date;
	}

	public long getDateMilli() {
		return date * 1000L;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public long getTotalnotes() {
		return getPerfect() + getGreat() + getGood() + getBad() + getPoor();
	}

	// デルタ系
	public long getPlaycountDelta() {
		return prev != null ? playcount - prev.getPlaycount() : 0;
	}

	public long getClearDelta() {
		return prev != null ? clear - prev.getClear() : 0;
	}

	public long getFailDelta() {
		return prev != null ? fail - prev.getFail() : 0;
	}

	public long getPerfectDelta() {
		return prev != null ? perfect - prev.getPerfect() : 0;
	}

	public long getGreatDelta() {
		return prev != null ? great - prev.getGreat() : 0;
	}

	public long getGoodDelta() {
		return prev != null ? good - prev.getGood() : 0;
	}

	public long getBadDelta() {
		return prev != null ? bad - prev.getBad() : 0;
	}

	public long getPoorDelta() {
		return prev != null ? poor - prev.getPoor() : 0;
	}

	public long getPlaytimeDelta() {
		return prev != null ? playtime - prev.getPlaytime() : 0;
	}

	public long getTotalnotesDelta() {
		return getPerfectDelta() + getGreatDelta() + getGoodDelta()
				+ getBadDelta() + getPoorDelta();
	}

	public long getUpdateCount() {
		return updateCount;
	}

	public void setUpdateCount(long updateCount) {
		this.updateCount = updateCount;
	}

	public double getUpdateRate() {
		double ret = 0.0;
		if (getPlaycountDelta() > 0) {
			ret = (double) getUpdateCount() / (double) getPlaycountDelta();
		}
		return ret;
	}

	public double getBPRate() {
		double ret = 0.0;
		if (getPlaycountDelta() > 0) {
			ret = ((double) getBadDelta() + getPoorDelta())
					/ (double) getTotalnotesDelta();
		}
		return ret;
	}

	public double getScoreRate() {
		double ret = 0.0;
		if (getPlaycountDelta() > 0) {
			ret = ((double) getPerfectDelta() * 2 + getGreatDelta())
					/ ((double) getTotalnotesDelta() * 2);
		}
		return ret;
	}

	public double getClearRate() {
		double ret = 0.0;
		if (getPlaycountDelta() > 0) {
			ret = (double) getClearDelta() / (double) getTotalnotesDelta();
		}
		return ret;
	}

	public double getNotesDensity() {
		double ret = 0.0;
		if (getPlaytimeDelta() > 0) {
			ret = (double) getTotalnotesDelta() / (double) getPlaytimeDelta();
		}
		return ret;
	}

	public String getLogdate() {
		return logdate;
	}

	public void setLogdate(String logdate) {
		this.logdate = logdate;
	}
}
