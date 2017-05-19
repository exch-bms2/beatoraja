package bms.player.beatoraja;

import bms.model.Mode;

/**
 * スコアデータ
 * LR2のスコアデータを元に拡張している
 *
 * @author ununique
 */
public class IRScoreData {
	/**
	 * 譜面のハッシュ値
	 */
	private String sha256 = "";
	
	private int mode = 0;
	
	private int clear = 0;

	/**
	 * スコア最終取得日時(unixtime, 秒単位)
	 */
	private long date = 0;	
	/**
	 * 総プレイ回数
	 */
	private int playcount = 0;
	/**
	 * 総クリア回数
	 */
	private int clearcount = 0;
	/**
	 * 総PGREATノート数
	 */
	private int epg = 0;
	private int lpg = 0;
	/**
	 * 総GREATノート数
	 */
	private int egr = 0;
	private int lgr = 0;
	/**
	 * 総GOODノート数
	 */
	private int egd = 0;
	private int lgd = 0;
	/**
	 * 総BADノート数
	 */
	private int ebd = 0;
	private int lbd = 0;
	/**
	 * 総POORノート数
	 */
	private int epr = 0;
	private int lpr = 0;
	/**
	 * 総MISSノート数
	 */
	private int ems = 0;
	private int lms = 0;
	private int maxcombo = 0;
	
	private int notes = 0;
	
	private int minbp = Integer.MAX_VALUE;
	/**
	 * 各譜面オプションのクリア履歴
	 */
	private int history;
	/**
	 * 更新時のRANDOM配列
	 */
	private int random;
	/**
	 * 更新時のオプション
	 */
	private int option;
	/**
	 * アシストオプション
	 */
	private int assist;
	/**
	 * プレイゲージ
	 */
	private int gauge;
	/**
	 * 入力デバイス
	 */
	private int device;
	
	private int state;
	
	private String scorehash = "";

	private final Mode playmode;

	public IRScoreData() {
		this(Mode.BEAT_7K);
	}

	public IRScoreData(Mode playmode) {
		this.playmode = playmode;
	}
	
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public int getPlaycount() {
		return playcount;
	}
	public void setPlaycount(int playcount) {
		this.playcount = playcount;
	}
	public int getClear() {
		return clear;
	}
	public void setClear(int clear) {
		this.clear = clear;
	}
	public int getEpg() {
		return epg;
	}
	public void setEpg(int epg) {
		this.epg = epg;
	}
	public int getLpg() {
		return lpg;
	}
	public void setLpg(int lpg) {
		this.lpg = lpg;
	}
	public int getEgr() {
		return egr;
	}
	public void setEgr(int egr) {
		this.egr = egr;
	}
	public int getLgr() {
		return lgr;
	}
	public void setLgr(int lgr) {
		this.lgr = lgr;
	}
	public int getEgd() {
		return egd;
	}
	public void setEgd(int egd) {
		this.egd = egd;
	}
	public int getLgd() {
		return lgd;
	}
	public void setLgd(int lgd) {
		this.lgd = lgd;
	}
	public int getEbd() {
		return ebd;
	}
	public void setEbd(int ebd) {
		this.ebd = ebd;
	}
	public int getLbd() {
		return lbd;
	}
	public void setLbd(int lbd) {
		this.lbd = lbd;
	}
	public int getEpr() {
		return epr;
	}
	public void setEpr(int epr) {
		this.epr = epr;
	}
	public int getLpr() {
		return lpr;
	}
	public void setLpr(int lpr) {
		this.lpr = lpr;
	}
	public int getEms() {
		return ems;
	}
	public void setEms(int ems) {
		this.ems = ems;
	}
	public int getLms() {
		return lms;
	}
	public void setLms(int lms) {
		this.lms = lms;
	}

	public int getJudgeCount(int judge) {
		return getJudgeCount(judge, true) + getJudgeCount(judge, false);
	}

	/**
	 * 指定の判定のカウント数を返す
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge, boolean fast) {
		switch (judge) {
			case 0:
				return fast ? epg : lpg;
			case 1:
				return fast ? egr : lgr;
			case 2:
				return fast ? egd : lgd;
			case 3:
				return fast ? ebd : lbd;
			case 4:
				return fast ? epr : lpr;
			case 5:
				return fast ? ems : lms;
		}
		return 0;
	}

	public void addJudgeCount(int judge, boolean fast, int count) {
		switch (judge) {
		case 0:
			if(fast) {
				epg += count;
			} else {
				lpg += count;
			}
			break;
		case 1:
			if(fast) {
				egr += count;
			} else {
				lgr += count;
			}
			break;
		case 2:
			if(fast) {
				egd += count;
			} else {
				lgd += count;
			}
			break;
		case 3:
			if(fast) {
				ebd += count;
			} else {
				lbd += count;
			}
			break;
		case 4:
			if(fast) {
				epr += count;
			} else {
				lpr += count;
			}
			break;
		case 5:
			if(fast) {
				ems += count;
			} else {
				lms += count;
			}
			break;
		}
	}

	public int getCombo() {
		return maxcombo;
	}
	public void setCombo(int maxcombo) {
		this.maxcombo = maxcombo;
	}
	public int getMode() {
		return mode;
	}
	public void setMode(int mode) {
		this.mode = mode;
	}
	public int getNotes() {
		return notes;
	}
	public void setNotes(int totalnotes) {
		this.notes = totalnotes;
	}
	public int getClearcount() {
		return clearcount;
	}
	public void setClearcount(int clearcount) {
		this.clearcount = clearcount;
	}
	public int getMinbp() {
		return minbp;
	}
	public void setMinbp(int minbp) {
		this.minbp = minbp;
	}
	public int getHistory() {
		return history;
	}
	public void setHistory(int history) {
		this.history = history;
	}
	public int getOption() {
		return option;
	}
	public void setOption(int option) {
		this.option = option;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public String getSha256() {
		return sha256;
	}
	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public int getExscore() {
		return (epg + lpg) * 2 + egr + lgr;
	}
	public int getRandom() {
		return random;
	}
	public void setRandom(int random) {
		this.random = random;
	}
	public String getScorehash() {
		return scorehash;
	}
	public void setScorehash(String scorehash) {
		this.scorehash = scorehash;
	}
	public int getAssist() {
		return assist;
	}
	public void setAssist(int assist) {
		this.assist = assist;
	}
	public int getGauge() {
		return gauge;
	}
	public void setGauge(int gauge) {
		this.gauge = gauge;
	}
	public int getDevice() {
		return device;
	}
	public void setDevice(int device) {
		this.device = device;
	}

	public Mode getPlaymode() {
		return playmode;
	}
}
