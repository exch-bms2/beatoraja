package bms.player.beatoraja.skin;

import bms.model.Mode;

/**
 * スキンタイプ
 * 
 * @author exch
 */
public enum SkinType {
	
	PLAY_7KEYS(0, "7KEYS", Mode.BEAT_7K, false),
	PLAY_5KEYS(1, "5KEYS", Mode.BEAT_5K, false),
	PLAY_14KEYS(2, "14KEYS", Mode.BEAT_14K, false),
	PLAY_10KEYS(3, "10KEYS", Mode.BEAT_10K, false),
	PLAY_9KEYS(4, "9KEYS", Mode.POPN_9K, false),
	MUSIC_SELECT(5, "MUSIC SELECT"),
	DECIDE(6, "DECIDE"),
	RESULT(7, "RESULT"),
	KEY_CONFIG(8, "KEY CONFIG"),
	SKIN_SELECT(9, "SKIN SELECT"),
	SOUND_SET(10, "SOUND SET"),
	THEME(11, "THEME"),
	PLAY_7KEYS_BATTLE(12, "7KEYS BATTLE", Mode.BEAT_7K, true),
	PLAY_5KEYS_BATTLE(13, "5KEYS BATTLE", Mode.BEAT_5K, true),
	PLAY_9KEYS_BATTLE(14, "9KEYS BATTLE", Mode.POPN_9K, true),
	COURSE_RESULT(15, "COURSE RESULT"),
	PLAY_24KEYS(16, "24KEYS", Mode.KEYBOARD_24K, false),
	PLAY_24KEYS_DOUBLE(17, "24KEYS DOUBLE", Mode.KEYBOARD_24K_DOUBLE, false),
	PLAY_24KEYS_BATTLE(18, "24KEYS BATTLE", Mode.KEYBOARD_24K, true),
	;

	/**
	 * スキンタイプのID
	 */
	private final int id;
	/**
	 * 名称
	 */
	private final String name;
	/**
	 * プレイ可能なスキンかどうか
	 */
	private final boolean play;
	/**
	 * プレイモード
	 */
	private final Mode mode;
	/**
	 * バトルモードかどうか
	 */
	private final boolean battle;

	private SkinType(int id, String name) {
		this.id = id;
		this.name = name;
		this.play = false;
		this.mode = null;
		this.battle = false;
	}

	private SkinType(int id, String name, Mode mode, boolean battle) {
		this.id = id;
		this.name = name;
		this.play = true;
		this.mode = mode;
		this.battle = battle;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isPlay() {
		return play;
	}

	public Mode getMode() {
		return mode;
	}

	public boolean isBattle() {
		return battle;
	}
	
	public static SkinType getSkinTypeById(int id) {
		for(SkinType type : values()) {
			if(type.id == id) {
				return type;
			}
		}
		return null;
	}

	public static int getMaxSkinTypeID() {
		int max = -1;
		for(SkinType type : values()) {
			max = Math.max(max, type.id);
		}
		return max;
	}
}
