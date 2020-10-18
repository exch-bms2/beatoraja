package bms.player.beatoraja.pattern;

public enum Random {
	IDENTITY(0, RandomUnit.LANE, false),
	MIRROR(1, RandomUnit.LANE, false),
	RANDOM(2, RandomUnit.LANE, false),
	R_RANDOM(3, RandomUnit.LANE, false),
	S_RANDOM(4, RandomUnit.NOTE, false),
	SPIRAL(5, RandomUnit.NOTE, false),
	H_RANDOM(6, RandomUnit.NOTE, false),
	ALL_SCR(7, RandomUnit.NOTE, true),
	RANDOM_EX(8, RandomUnit.LANE, true),
	S_RANDOM_EX(9, RandomUnit.NOTE, true),

    CROSS(10, RandomUnit.LANE, false),

    FLIP(20, RandomUnit.NOTE, true),
    BATTLE(21, RandomUnit.NOTE, true),
    ;

	/**
	 * PlayerConfigから渡されるid
	 */
	public final int id;

	public final RandomUnit unit;

	/**
	 * 変更レーンにスクラッチレーンを含むか
	 */
	public final boolean isScratchLaneModify;

	private Random(int id, RandomUnit unit, boolean s) {
		this.id = id;
		this.unit = unit;
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