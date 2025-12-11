package bms.player.beatoraja.pattern;

import bms.model.Mode;

public enum Random {
	IDENTITY(RandomUnit.NONE, false),
	MIRROR(RandomUnit.LANE, false),
	RANDOM(RandomUnit.LANE, false),
	ROTATE(RandomUnit.LANE, false),
	S_RANDOM(RandomUnit.NOTE, false),
	SPIRAL(RandomUnit.NOTE, false),
	H_RANDOM(RandomUnit.NOTE, false),
	ALL_SCR(RandomUnit.NOTE, true),
	MIRROR_EX(RandomUnit.LANE, true),
	RANDOM_EX(RandomUnit.LANE, true),
	ROTATE_EX(RandomUnit.LANE, true),
	S_RANDOM_EX(RandomUnit.NOTE, true),

    CROSS(RandomUnit.LANE, false),

    CONVERGE(RandomUnit.NOTE, true),
    S_RANDOM_NO_THRESHOLD(RandomUnit.NOTE, false),
    RANDOM_PLAYABLE(RandomUnit.LANE, true),
    S_RANDOM_PLAYABLE(RandomUnit.NOTE, true),

    FLIP(RandomUnit.PLAYER, true),
    BATTLE(RandomUnit.PLAYER, true),
    ;

	public final RandomUnit unit;
	
	public static final Random[] OPTION_GENERAL = 
		{IDENTITY, MIRROR, RANDOM, ROTATE, S_RANDOM, SPIRAL, H_RANDOM, ALL_SCR, RANDOM_EX, S_RANDOM_EX};
	public static final Random[] OPTION_PMS = 
		{IDENTITY, MIRROR, RANDOM, ROTATE, S_RANDOM_NO_THRESHOLD, SPIRAL, H_RANDOM, CONVERGE, RANDOM_PLAYABLE, S_RANDOM_PLAYABLE};

	public static final Random[] OPTION_DOUBLE = {IDENTITY, FLIP};
	public static final Random[] OPTION_SINGLE = {IDENTITY, BATTLE};

	/**
	 * 変更レーンにスクラッチレーンを含むか
	 */
	public final boolean isScratchLaneModify;

	private Random(RandomUnit unit, boolean s) {
		this.unit = unit;
		this.isScratchLaneModify = s;
	}

	public static Random getRandom(int id, Mode mode) {
		final Random[] randoms = switch(mode) {
			case POPN_5K, POPN_9K -> OPTION_PMS;
			default -> OPTION_GENERAL;
		};
		return id >= 0 && id < randoms.length ? randoms[id] : IDENTITY;
	}	
}

enum RandomUnit {
	NONE, LANE, NOTE, PLAYER;
}