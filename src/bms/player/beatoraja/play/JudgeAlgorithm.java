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
		public boolean compare(Note t1, Note t2, long ptime, long[][] judgetable) {
			return t2.getState() == 0 && t1.getMicroTime() < ptime + judgetable[2][0] && t2.getMicroTime() <= ptime + judgetable[2][1];
		}
	},
	/**
	 * 判定アルゴリズム:判定時間差最優先
	 */
	Duration {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, long[][] judgetable) {
			return Math.abs(t1.getMicroTime() - ptime) > Math.abs(t2.getMicroTime() - ptime) && t2.getState() == 0;
		}
	},
	/**
	 * 判定アルゴリズム:最下ノーツ優先
	 */
	Lowest {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, long[][] judgetable) {
			return false;
		}
	},
	/**
	 * 判定アルゴリズム:スコア最優先
	 */
	Score {
		@Override
		public boolean compare(Note t1, Note t2, long ptime, long[][] judgetable) {
			return t2.getState() == 0 && t1.getMicroTime() < ptime + judgetable[1][0] && t2.getMicroTime() <= ptime + judgetable[1][1];
		}
	}
	;

	public static final JudgeAlgorithm[] defaultAlgorithm = {Combo, Duration, Lowest};

	/**
	 * ２つのノーツを比較する
	 * @param t1 ノーツ1
	 * @param t2 ノーツ2
	 * @param ptime キー操作の時間
	 * @param judgetable 判定テーブル
	 * @return ノーツ2が選ばれた場合はtrue, ノーツ1が選ばれた場合はfalse
	 */
	public abstract boolean compare(Note t1, Note t2, long ptime, long[][] judgetable);

	public static int getIndex(String algorithm) {
		for(int i = 0;i < values().length;i++) {
			if(values()[i].name().equals(algorithm)) {
				return i;
			}
		}
		return -1;
	}
}