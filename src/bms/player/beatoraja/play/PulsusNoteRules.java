package bms.player.beatoraja.play;

/** Pulsus's default BMS timing tables for presses and releases, expressed in microseconds. */
record PulsusNoteRules(int perfectGreatUs, int greatUs, int goodUs) {
	static final long LATE_LIMIT_US = 200_000;
	static final long EARLY_LIMIT_US = 1_000_000;
	private static final PulsusNoteRules[] RANKS = {
			new PulsusNoteRules(8_000, 24_000, 40_000),
			new PulsusNoteRules(15_000, 30_000, 60_000),
			new PulsusNoteRules(18_000, 40_000, 100_000),
			new PulsusNoteRules(21_000, 60_000, 120_000)
	};

	static PulsusNoteRules forRank(Integer rank) {
		return RANKS[rank != null && rank >= 0 && rank < RANKS.length ? rank : 2];
	}

	int judge(long noteMinusInputUs, boolean release) {
		long distance = Math.abs(noteMinusInputUs);
		if (distance <= perfectGreatUs) return 0;
		if (distance <= greatUs) return 1;
		if (distance <= goodUs) return 2;
		if (distance <= LATE_LIMIT_US) return 3;
		return release ? 4 : 5;
	}
}
