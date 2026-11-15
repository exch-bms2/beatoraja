package bms.player.beatoraja.play;

/** Timing and single-song gauge parameters for the experimental LunaticRave2 rule. */
final class LunaticRave2Rules {
    private static final int BAD_MS = 200;
    private static final int EMPTY_POOR_MS = 1_000;
    private static final int[][] HIT_WINDOWS_MS = {{8, 24, 40}, {15, 30, 60}, {18, 40, 100}, {21, 60, 120}};
    private static final int[] DAMAGE_BREAKS = {20, 30, 45, 60, 125, 250, 500, 1_000};
    private static final int[] DAMAGE_AT_START = {10, 7, 6, 5, 4, 3, 2};
    private static final int[] DAMAGE_DROP = {2, 1, 1, 1, 1, 1, 1};

    private final int[] hitWindows;
    private final double total;

    LunaticRave2Rules(BMSPlayerRule.ChartParameters chart, int notes) {
        int rank = chart.rank();
        hitWindows = HIT_WINDOWS_MS[rank >= 0 && rank < HIT_WINDOWS_MS.length ? rank : 2];
        total = chart.total() >= 1 ? (int) chart.total() : 0.8 * (200 + notes / 5.0
                + Math.max(0, Math.min(200, notes - 400)) / 5.0);
    }

    boolean isLate(long timeMs, long noteUs, boolean longNoteHead) {
        int lateWindowMs = longNoteHead ? hitWindows[2] : BAD_MS;
        return timeMs * 1_000 - noteUs > lateWindowMs * 1_000L;
    }

    long missAtMs(long noteUs, boolean longNoteHead) {
        return noteUs / 1_000 + (longNoteHead ? hitWindows[2] : BAD_MS) + 1;
    }

    int judge(long earlyMs) {
        long gap = Math.abs(earlyMs);
        for (int i = 0; i < hitWindows.length; i++) {
            if (gap <= hitWindows[i]) return i;
        }
        if (gap <= BAD_MS) return 3;
        return earlyMs < EMPTY_POOR_MS ? 5 : -1;
    }

    static boolean canChainBad(long earlyMs) {
        return Math.abs(earlyMs) <= BAD_MS;
    }

    boolean isEarlyLongNoteRelease(long endMs, long releaseMs) {
        return endMs - releaseMs > hitWindows[2];
    }

    boolean isMinePress(long timeMs, long noteUs) {
        return noteUs - timeMs * 1_000 < hitWindows[1] * 1_000L;
    }

    double[] gaugeChanges(int type, int notes) {
        double recovery = total / Math.max(notes, 1);
        return switch (type) {
            case GrooveGauge.NORMAL -> new double[]{recovery, recovery, recovery / 2, -4, -6, -2};
            case GrooveGauge.EASY -> new double[]{recovery * 1.2, recovery * 1.2, recovery * 0.6, -3.2, -4.8, -1.6};
            case GrooveGauge.HARD -> {
                double damage = Math.max(noteDamage(notes), 10.0 / Math.max(1, (int) ((total - 80) / 16)));
                yield new double[]{0.1, 0.1, 0.05, -6 * damage, -10 * damage, -2 * damage};
            }
            default -> null;
        };
    }

    private static double noteDamage(int notes) {
        if (notes < DAMAGE_BREAKS[0]) return 10;
        for (int i = 0; i < DAMAGE_AT_START.length; i++) {
            if (notes < DAMAGE_BREAKS[i + 1]) {
                return DAMAGE_AT_START[i] - DAMAGE_DROP[i] * (notes - DAMAGE_BREAKS[i])
                        / (double) (DAMAGE_BREAKS[i + 1] - DAMAGE_BREAKS[i]);
            }
        }
        return 1;
    }
}
