package bms.player.beatoraja.ir;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.input.BMSPlayerInputDevice;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.JudgeAlgorithm;

/**
 * IR用スコアデータ
 * 
 * @author exch
 */
public class IRScoreData {

	/**
	 * 譜面のハッシュ値
	 */
	public final String sha256;
	/**
	 * LN TYPE(0: LN, 1: CN, 2: HCN)
	 */
	public final int lntype;
	/**
	 * プレイヤーID
	 */
	public final String id;
	/**
	 * プレイヤー名。自身のスコアの場合は空白
	 */
	public final String player;
	/**
	 * クリアタイプ
	 */
	public final ClearType clear;
	/**
	 * スコア最終取得日時(unixtime, 秒単位)
	 */
	public final long date;
	/**
	 * 総PGREATノート数
	 */
	public final int epg;
	public final int lpg;
	/**
	 * 総GREATノート数
	 */
	public final int egr;
	public final int lgr;
	/**
	 * 総GOODノート数
	 */
	public final int egd;
	public final int lgd;
	/**
	 * 総BADノート数
	 */
	public final int ebd;
	public final int lbd;
	/**
	 * 総POORノート数
	 */
	public final int epr;
	public final int lpr;
	/**
	 * 総MISSノート数
	 */
	public final int ems;
	public final int lms;
	
	public final long avgjudge;
	/**
	 * 最大コンボ数
	 */
	public final int maxcombo;
	/**
	 * 総ノート数
	 */
	public final int notes;
	/**
	 * 処理済ノート数
	 */
	public final int passnotes;
	/**
	 * 最小ミスカウント
	 */
	public final int minbp;
	/**
	 * 更新時のオプション
	 */
	public final int option;
	
	public final long seed;
	/**
	 * アシストオプション
	 */
	public final int assist;
	/**
	 * プレイゲージ
	 */
	public final int gauge;
	/**
	 * 入力デバイス
	 */
	public final BMSPlayerInputDevice.Type deviceType;
	/**
	 * 判定アルゴリズム
	 */
	public final JudgeAlgorithm judgeAlgorithm;
	/**
	 * ルール
	 */
	public final BMSPlayerRule rule;
	
	public final String skin;
	
	public IRScoreData(ScoreData score) {
		this.sha256 = score.getSha256();
		this.lntype = score.getMode();
		this.id = score.getID();
		this.player = score.getPlayer();
		this.clear = ClearType.getClearTypeByID(score.getClear());
		this.date = score.getDate();
		this.epg = score.getEpg();
		this.lpg = score.getLpg();
		this.egr = score.getEgr();
		this.lgr = score.getLgr();
		this.egd = score.getEgd();
		this.lgd = score.getLgd();
		this.ebd = score.getEbd();
		this.lbd = score.getLbd();
		this.epr = score.getEpr();
		this.lpr = score.getLpr();
		this.ems = score.getEms();
		this.lms = score.getLms();
		this.avgjudge = score.getAvgjudge();
		this.maxcombo = score.getCombo();
		this.notes = score.getNotes();
		this.passnotes = score.getPassnotes();
		this.minbp = score.getMinbp();
		this.option = score.getOption();
		this.seed = score.getSeed();
		this.assist = score.getAssist();
		this.gauge = score.getGauge();
		this.deviceType = score.getDeviceType();
		this.judgeAlgorithm = score.getJudgeAlgorithm();
		this.rule = score.getRule();
		this.skin = score.getSkin();
	}
	
	public int getExscore() {
		return (epg + lpg) * 2 + egr + lgr;
	}
}
