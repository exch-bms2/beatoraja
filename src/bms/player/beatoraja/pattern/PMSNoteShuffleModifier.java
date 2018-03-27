package bms.player.beatoraja.pattern;

import java.util.List;

import bms.model.BMSModel;
import bms.player.beatoraja.PlayerConfig;

public class PMSNoteShuffleModifier extends PatternModifier {
	private static final PlayerConfig config = playerConfig;
	/**
	 * タイムライン毎にノーツをランダムに入れ替える
	 */
	public static final int S_RANDOM = 0;
	/**
	 * 初期の並べ替えをベースに、螺旋状に並べ替える
	 */
	public static final int SPIRAL = 1;
	/**
	 * ノーツをスクラッチレーンに集約する
	 */
	public static final int ALL_SCR = 2;
	/**
	 * S-RANDOMに縦連が極力来ないように配置する
	 */
	public static final int H_RANDOM = 3;
	/**
	 * スクラッチレーンを含めたS-RANDOM
	 */
	public static final int S_RANDOM_EX = 4;

	private int type;

	/**
	 * 次のTimeLine増加分(SPIRAL用)
	 */
	private int inc;

	/**
	 * 連打しきい値(ms)(H-RANDOM用)
	 */
	private int hranThreshold = 125;

	public PMSNoteShuffleModifier(int type) {
		super(type >= ALL_SCR ? 1 : 0);
		this.type = type;
	}

	@Override
	public List<PatternModifyLog> modify(BMSModel model) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
