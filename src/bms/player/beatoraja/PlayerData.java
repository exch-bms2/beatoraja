package bms.player.beatoraja;

public class PlayerData {

	/**
	 * プレイヤーデータ取得日時(unixtime, 1日刻み)
	 */
	private long date = 0;	
	/**
	 * 総プレイ回数
	 */
	private long playcount = 0;
	/**
	 * 総クリア回数
	 */
	private long clear = 0;
	/**
	 * 総PGREATノート数
	 */
	private long epg = 0;
	private long lpg = 0;
	/**
	 * 総GREATノート数
	 */
	private long egr = 0;
	private long lgr = 0;
	/**
	 * 総GOODノート数
	 */
	private long egd = 0;
	private long lgd = 0;
	/**
	 * 総BADノート数
	 */
	private long ebd = 0;
	private long lbd = 0;
	/**
	 * 総POORノート数
	 */
	private long epr = 0;
	private long lpr = 0;
	/**
	 * 総MISSノート数
	 */
	private long ems = 0;
	private long lms = 0;
	/**
	 * 総プレイ時間
	 */
	private long playtime = 0;
	private long maxcombo = 0;
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
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
	public long getEpg() {
		return epg;
	}
	public void setEpg(long epg) {
		this.epg = epg;
	}
	public long getLpg() {
		return lpg;
	}
	public void setLpg(long lpg) {
		this.lpg = lpg;
	}
	public long getEgr() {
		return egr;
	}
	public void setEgr(long egr) {
		this.egr = egr;
	}
	public long getLgr() {
		return lgr;
	}
	public void setLgr(long lgr) {
		this.lgr = lgr;
	}
	public long getEgd() {
		return egd;
	}
	public void setEgd(long egd) {
		this.egd = egd;
	}
	public long getLgd() {
		return lgd;
	}
	public void setLgd(long lgd) {
		this.lgd = lgd;
	}
	public long getEbd() {
		return ebd;
	}
	public void setEbd(long ebd) {
		this.ebd = ebd;
	}
	public long getLbd() {
		return lbd;
	}
	public void setLbd(long lbd) {
		this.lbd = lbd;
	}
	public long getEpr() {
		return epr;
	}
	public void setEpr(long epr) {
		this.epr = epr;
	}
	public long getLpr() {
		return lpr;
	}
	public void setLpr(long lpr) {
		this.lpr = lpr;
	}
	public long getEms() {
		return ems;
	}
	public void setEms(long ems) {
		this.ems = ems;
	}
	public long getLms() {
		return lms;
	}
	public void setLms(long lms) {
		this.lms = lms;
	}
	public long getPlaytime() {
		return playtime;
	}
	public void setPlaytime(long playtime) {
		this.playtime = playtime;
	}
	public long getMaxcombo() {
		return maxcombo;
	}
	public void setMaxcombo(long maxcombo) {
		this.maxcombo = maxcombo;
	}

}
