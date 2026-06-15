package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.play.JudgeProperty.JudgeWindow;
import bms.player.beatoraja.play.JudgeProperty.NoteType;

/**
 * 判定アルゴリズム
 * 
 * @author exch
 */
public enum JudgeAlgorithm {

	/**
	 * 判定アルゴリズム:コンボ最優先
	 */
	Combo ((t1, t2, ptime, window, type) -> 
	// 下ノーツをGOOD以上で拾えないなら上ノーツを拾いに行く
		t2.getState() == 0 && t1.getMicroTime() < ptime + window.getTime(type, 2, false) && t2.getMicroTime() <= ptime + window.getTime(type, 2, true)
	),
	/**
	 * 判定アルゴリズム:判定時間差最優先
	 */
	Duration ((t1, t2, ptime, window, type) ->
	// より時間が近いノーツを拾いに行く
		Math.abs(t1.getMicroTime() - ptime) > Math.abs(t2.getMicroTime() - ptime) && t2.getState() == 0
	),
	/**
	 * 判定アルゴリズム:最下ノーツ優先
	 */
	Lowest  ((t1, t2, ptime, window, type) -> false),
	/**
	 * 判定アルゴリズム:スコア最優先
	 */
	Score ((t1, t2, ptime, window, type) ->
		t2.getState() == 0 && t1.getMicroTime() < ptime + window.getTime(type, 1, false)  && t2.getMicroTime() <= ptime + window.getTime(type, 1, true)
	)
	;
	
	public final JudgeFunction function;

	public static final JudgeAlgorithm[] defaultAlgorithm = {Combo, Duration, Lowest};

	private JudgeAlgorithm(JudgeFunction function) {
		this.function = function;
	}

	public static int getIndex(String algorithm) {
		for(int i = 0;i < defaultAlgorithm.length;i++) {
			if(defaultAlgorithm[i].name().equals(algorithm)) {
				return i;
			}
		}
		return -1;
	}
	
	public interface JudgeFunction {
		/**
		 * ２つのノーツを比較する
		 * @param t1 ノーツ1
		 * @param t2 ノーツ2
		 * @param ptime キー操作の時間
		 * @param window 判定ウィンドウ
		 * @param type ノートタイプ
		 * @return ノーツ2が選ばれた場合はtrue, ノーツ1が選ばれた場合はfalse
		 */
		public abstract boolean compare(Note t1, Note t2, long ptime, JudgeWindow window, NoteType type);
		
	}
}