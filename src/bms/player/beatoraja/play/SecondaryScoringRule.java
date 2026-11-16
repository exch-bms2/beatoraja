package bms.player.beatoraja.play;

/** Registry of implemented rules that may run alongside beatoraja scoring. */
public enum SecondaryScoringRule {
	LR2("lr2", "Lunatic Rave 2", BMSPlayerRule.LunaticRave2),
	PULSUS("pulsus", "Pulsus", BMSPlayerRule.Pulsus),
	RAINDROP("raindrop", "raindrop", BMSPlayerRule.Raindrop),
	BEMUSE("bemuse", "Bemuse", BMSPlayerRule.Bemuse);

	private final String id;
	private final String displayName;
	private final BMSPlayerRule scoreRule;

	SecondaryScoringRule(String id, String displayName, BMSPlayerRule scoreRule) {
		this.id = id;
		this.displayName = displayName;
		this.scoreRule = scoreRule;
	}

	public String id() { return id; }
	public String displayName() { return displayName; }
	public BMSPlayerRule scoreRule() { return scoreRule; }
	public String databaseSuffix() { return "_" + id; }

	public static SecondaryScoringRule byId(String id) {
		for (var rule : values()) if (rule.id.equals(id)) return rule;
		return null;
	}
}
