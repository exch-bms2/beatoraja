package bms.player.beatoraja.select;

/**
 * 選曲時の譜面難易度フィルター
 *
 * @author exch
 */
public enum DifficultyFilter {
	ALL("ALL", 0, 0),
	BEGINNER("BEGINNER", 1, 1),
	NORMAL("NORMAL", 2, 2),
	HYPER("HYPER", 3, 3),
	ANOTHER("ANOTHER", 4, 4),
	INSANE("INSANE", 5, 5);

	private final String displayName;
	private final int skinNumber;
	private final int difficulty;

	DifficultyFilter(String displayName, int skinNumber, int difficulty) {
		this.displayName = displayName;
		this.skinNumber = skinNumber;
		this.difficulty = difficulty;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getSkinNumber() {
		return skinNumber;
	}

	public boolean matches(int difficulty) {
		return this.difficulty == 0 || this.difficulty == difficulty;
	}
}
