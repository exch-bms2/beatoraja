package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.play.JudgeProperty.MissCondition;

/**
 * 判定アルゴリズム
 * 
 * @author exch
 */
public enum JudgeAlgorithm {

	/**
	 * 判定アルゴリズム:コンボ最優先
	 */
	Combo {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return t2.getState() == 0 && t1.getTime() < ptime + judgetable[2][0] && t2.getTime() <= ptime + judgetable[2][1];
		}
	},
	/**
	 * 判定アルゴリズム:判定時間差最優先
	 */
	Duration {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return Math.abs(t1.getTime() - ptime) > Math.abs(t2.getTime() - ptime) && t2.getState() == 0;
		}
	},
	/**
	 * 判定アルゴリズム:最下ノーツ優先
	 */
	Lowest {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return false;
		}
	},
	/**
	 * 判定アルゴリズム:スコア最優先
	 */
	Score {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, int[][] judgetable) {
			return t2.getState() == 0 && t1.getTime() < ptime + judgetable[1][0] && t2.getTime() <= ptime + judgetable[1][1];
		}
	}
	;

	private int judge;

	/**
	 * 判定対象ノーツを取得する
	 *
	 * @param lanemodel レーン
	 * @param ptime 時間
	 * @param judgetable 判定時間テーブル
	 * @return 判定対象ノーツ
	 */
	public Note getNote(Lane lanemodel, long ptime, int[][] judgetable, long judgestart, long judgeend, MissCondition miss) {
		Note note = null;
		int judge = 0;
		for (Note judgenote = lanemodel.getNote();judgenote != null;judgenote = lanemodel.getNote()) {
			final long dtime = judgenote.getTime() - ptime;
			if (dtime >= judgeend) {
				break;
			}
			if (dtime >= judgestart) {
				if (!(judgenote instanceof MineNote) && !(judgenote instanceof LongNote
						&& ((LongNote) judgenote).isEnd())) {
					if (note == null || note.getState() != 0 || compare(note, judgenote, ptime, judgetable)) {
						if (!(miss == MissCondition.ONE && (judgenote.getState() != 0
								|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0 && (dtime > judgetable[2][1] || dtime < judgetable[2][0]))))) {
							if (judgenote.getState() != 0) {
								judge = (dtime >= judgetable[4][0] && dtime <= judgetable[4][1]) ? 5 : 6;
							} else {
								for (judge = 0; judge < judgetable.length && !(dtime >= judgetable[judge][0] && dtime <= judgetable[judge][1]); judge++) {
								}
								judge = (judge >= 4 ? judge + 1 : judge);
							}
							if(judge < 6 && (judge < 4 || note == null || Math.abs(note.getTime() - ptime) > Math.abs(judgenote.getTime() - ptime))) {
								note = judgenote;
							}
						}
					}
				}
			}
		}
		this.judge = judge;
		return note;
	}

	/**
	 * 判定対象ノーツの判定を取得する
	 * @return 判定
	 */
	public int getJudge() {
		return judge;
	}

	/**
	 * ２つのノーツを比較する
	 * @param t1 ノーツ1
	 * @param t2 ノーツ2
	 * @param ptime キー操作の時間
	 * @param judgetable 判定テーブル
	 * @return ノーツ2が選ばれた場合はtrue, ノーツ1が選ばれた場合はfalse
	 */
	public abstract boolean compare(Note t1, Note t2, long ptime, int[][] judgetable);
	
	public static int getIndex(JudgeAlgorithm algorithm) {
		for(int i = 0;i < JudgeAlgorithm.values().length;i++) {
			if(algorithm == JudgeAlgorithm.values()[i]) {
				return i;
			}
		}
		return -1;
	}
}