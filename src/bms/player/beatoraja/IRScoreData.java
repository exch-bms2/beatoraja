package bms.player.beatoraja;

import bms.model.Mode;
import bms.player.beatoraja.input.BMSPlayerInputDevice;
import lombok.Data;

/**
 * スコアデータ
 * LR2のスコアデータを元に拡張している
 *
 * @author ununique
 */
@Data
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
	private int maxCombo = 0;
	
	private int notes = 0;
	
	private int minBP = Integer.MAX_VALUE;
	/**
	 * 各譜面オプションのクリア履歴
	 */
	private String trophy = "";
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
	private BMSPlayerInputDevice.Type deviceType;
	
	private int state;
	
	private String scorehash = "";

	private final Mode playmode;

	public IRScoreData() {
		this(Mode.BEAT_7K);
	}

	public IRScoreData(Mode playmode) {
		this.playmode = playmode;
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

	public int getExscore() {
		return (epg + lpg) * 2 + egr + lgr;
	}

	public static enum SongTrophy {
		
		EASY('g'),
		GROOVE('G'),
		HARD('h'),
		EXHARD('H'),
		NORMAL('n'),
		MIRROR('m'),
		RANDOM('r'),
		R_RANDOM('o'),
		S_RANDOM('s'),
		H_RANDOM('p'),
		SPIRAL('P'),
		ALL_SCR('a'),
		EX_RANDOM('R'),
		EX_S_RANDOM('S'),
		BATTLE('B'),
		BATTLE_ASSIST('b'),		
		;
		
		public final char character;
		
		private SongTrophy(char c) {
			character = c;
		}
		
		public static SongTrophy getTrophy(char c) {
			for(SongTrophy trophy : values()) {
				if(trophy.character == c) {
					return trophy;
				}
			}
			return null;
		}
	}
}
