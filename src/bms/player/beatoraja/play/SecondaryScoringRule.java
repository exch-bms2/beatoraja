package bms.player.beatoraja.play;

/** Registry of implemented rules that may run alongside beatoraja scoring. */
public enum SecondaryScoringRule {
	LR2("lr2", "Lunatic Rave 2", BMSPlayerRule.LunaticRave2),
	PULSUS("pulsus", "Pulsus", BMSPlayerRule.Pulsus),
	RAINDROP("raindrop", "raindrop", BMSPlayerRule.Raindrop),
	BEMUSE("bemuse", "Bemuse", BMSPlayerRule.Bemuse),
	ANGOLMOIS("angolmois", "Angolmois (1 ms)", BMSPlayerRule.Angolmois),
	BEATORAJA("beatoraja", "beatoraja", BMSPlayerRule.Default);

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
	/** Gauges whose result graphs are calculated by this rule. */
	public boolean supportsGaugeGraph(int gauge) {
		return switch (this) {
			case LR2 -> gauge == AbstractGrooveGauge.EASY || gauge == AbstractGrooveGauge.NORMAL
					|| gauge == AbstractGrooveGauge.HARD || gauge == AbstractGrooveGauge.HAZARD;
			case PULSUS -> gauge == AbstractGrooveGauge.EASY || gauge == AbstractGrooveGauge.NORMAL || gauge == AbstractGrooveGauge.HARD;
			case ANGOLMOIS -> gauge == AbstractGrooveGauge.NORMAL;
			default -> gauge >= 0 && gauge < 9;
		};
	}
	@Override public String toString() { return displayName; }

	public static SecondaryScoringRule byId(String id) {
		for (var rule : values()) if (rule.id.equals(id)) return rule;
		return null;
	}
}
