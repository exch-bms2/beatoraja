package bms.player.beatoraja.pattern;

public enum Random {
	IDENTITY(0, RandomUnit.LANE, 0, false),
	MIRROR(1, RandomUnit.LANE, 0, false),
	RANDOM(2, RandomUnit.LANE, 0, false),
	R_RANDOM(3, RandomUnit.LANE, 0, false),
	S_RANDOM(4, RandomUnit.NOTE, 0, false),
	SPIRAL(5, RandomUnit.NOTE, 1, false),
	H_RANDOM(6, RandomUnit.NOTE, 1, false),
	ALL_SCR(7, RandomUnit.NOTE, 1, true),
	RANDOM_EX(8, RandomUnit.LANE, 1, true),
	S_RANDOM_EX(9, RandomUnit.NOTE, 1, true);

	/**
	 * PlayerConfigから渡されるid
	 */
	public final int id;

	/**
	 * オプションのアシスト値
	 * 0: 制約なし
	 * 1: アシストランプ スコア保存なし
	 * 2: ランプ更新なし スコア保存なし
	 */
	public final int assist;

	public final RandomUnit unit;

	/**
	 * 変更レーンにスクラッチレーンを含むか
	 */
	public final boolean isScratchLaneModify;

	private Random(int id, RandomUnit unit, int assist, boolean s) {
		this.id = id;
		this.unit = unit;
		this.assist = assist;
		this.isScratchLaneModify = s;
	}

	public static Random getRandom(int id) {
		for(Random r : Random.values()) {
			if (r.id == id) {
				return r;
			}
		}
		return Random.IDENTITY;
	}
}

enum RandomUnit {
	LANE,
	NOTE
}