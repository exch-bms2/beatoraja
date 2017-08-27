package bms.player.beatoraja.play;

import bms.model.*;

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
			return t2.getState() == 0 && t1.getTime() < ptime + judgetable[2][0] && t2.getTime() <= ptime + judgetable[0][1];
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
	 * @param pmsjudge PMS判定
	 * @return 判定対象ノーツ
	 */
	public Note getNote(Lane lanemodel, long ptime, int[][] judgetable, boolean pmsjudge) {
		Note note = null;
		int judge = 0;
		for (Note judgenote = lanemodel.getNote();judgenote != null;judgenote = lanemodel.getNote()) {
			final int dtime = (int) (judgenote.getTime() - ptime);
			if (dtime >= judgetable[4][1]) {
				break;
			}
			if (dtime >= judgetable[4][0]) {
				if (!(judgenote instanceof MineNote) && !(judgenote instanceof LongNote
						&& ((LongNote) judgenote).isEnd())) {
					if (note == null || note.getState() != 0 || compare(note, judgenote, ptime, judgetable)) {
						if (!(pmsjudge && (judgenote.getState() != 0
								|| (judgenote.getState() == 0 && judgenote.getPlayTime() != 0 && dtime >= judgetable[2][1])))) {
							note = judgenote;
							if (judgenote.getState() != 0) {
								judge = 5;
							} else {
								for (judge = 0; judge < judgetable.length && !(dtime >= judgetable[judge][0] && dtime <= judgetable[judge][1]); judge++) {
								}
							}
						}
					}
				}
			}
		}
		this.judge = judge == 4 ? 5 : judge;
		return note;
	}

	public int getJudge() {
		return judge;
	}

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