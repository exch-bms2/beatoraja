package bms.player.beatoraja.video;

public final class EnduranceStatisticsTest {
    public static void main(String[] args) {
        var flat = EnduranceStatistics.summarize(new double[] {999, 5, 5, 5}, 1);
        check(flat.count() == 3 && flat.slope() == 0 && flat.p95() == 5, "Warmup excluded from flat baseline");
        var growth = EnduranceStatistics.summarize(new double[] {1000, 10, 12, 14, 16, 18, 20}, 1);
        check(Math.abs(growth.slope() - 2) < 1e-10 && growth.first() == 10 && growth.last() == 20, "Known growth slope");
        check(growth.earlyMean() == 14 && growth.lateMean() == 16, "Comparable first/last windows");
        check(EnduranceStatistics.summarize(new double[] {7}, 0).slope() == 0, "Single sample");
        for (double[] invalid : new double[][] {{}, {Double.NaN}, {Double.POSITIVE_INFINITY}}) {
            try { EnduranceStatistics.summarize(invalid, 0); throw new AssertionError(); }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("EnduranceStatisticsTest passed (warmup, growth, windows, percentiles and validation)");
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
