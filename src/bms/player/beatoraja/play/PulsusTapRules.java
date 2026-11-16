package bms.player.beatoraja.play;

/** Pulsus's default BMS tap timing tables, expressed in microseconds. */
record PulsusTapRules(int perfectGreatUs, int greatUs, int goodUs) {
	static final long LATE_LIMIT_US = 200_000;
	private static final long EARLY_LIMIT_US = 1_000_000;
	private static final PulsusTapRules[] RANKS = {
			new PulsusTapRules(8_000, 24_000, 40_000),
			new PulsusTapRules(15_000, 30_000, 60_000),
			new PulsusTapRules(18_000, 40_000, 100_000),
			new PulsusTapRules(21_000, 60_000, 120_000)
	};

	static PulsusTapRules forRank(Integer rank) {
		return RANKS[rank != null && rank >= 0 && rank < RANKS.length ? rank : 2];
	}

	static boolean isCandidate(long noteMinusInputUs) {
		return noteMinusInputUs >= -LATE_LIMIT_US && noteMinusInputUs < EARLY_LIMIT_US;
	}

	int judge(long noteMinusInputUs) {
		long distance = Math.abs(noteMinusInputUs);
		if (distance <= perfectGreatUs) return 0;
		if (distance <= greatUs) return 1;
		if (distance <= goodUs) return 2;
		return distance <= LATE_LIMIT_US ? 3 : 5;
	}
}
