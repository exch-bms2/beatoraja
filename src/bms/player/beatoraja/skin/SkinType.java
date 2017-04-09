package bms.player.beatoraja.skin;

public enum SkinType {
	PLAY_7KEYS(0, "7KEYS", 7, false),
	PLAY_5KEYS(1, "5KEYS", 5, false),
	PLAY_14KEYS(2, "14KEYS", 14, false),
	PLAY_10KEYS(3, "10KEYS", 10, false),
	PLAY_9KEYS(4, "9KEYS", 9, false),
	MUSIC_SELECT(5, "MUSIC SELECT"),
	DECIDE(6, "DECIDE"),
	RESULT(7, "RESULT"),
	KEY_CONFIG(8, "KEY CONFIG"),
	SKIN_SELECT(9, "SKIN SELECT"),
	SOUND_SET(10, "SOUND SET"),
	THEME(11, "THEME"),
	PLAY_7KEYS_BATTLE(12, "7KEYS BATTLE", 7, true),
	PLAY_5KEYS_BATTLE(13, "5KEYS BATTLE", 5, true),
	PLAY_9KEYS_BATTLE(14, "9KEYS BATTLE", 9, true),
	COURSE_RESULT(15, "COURSE RESULT"),
	PLAY_24KEYS(16, "24KEYS", 24, false);

	private final int id;
	private final String name;
	private final boolean play;
	private final int keys;
	private final boolean battle;

	private SkinType(int id, String name) {
		this.id = id;
		this.name = name;
		this.play = false;
		this.keys = 0;
		this.battle = false;
	}

	private SkinType(int id, String name, int keys, boolean battle) {
		this.id = id;
		this.name = name;
		this.play = true;
		this.keys = keys;
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

	public int getKeys() {
		return keys;
	}

	public boolean isBattle() {
		return battle;
	}
}
