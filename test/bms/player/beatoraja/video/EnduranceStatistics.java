package bms.player.beatoraja.video;

import java.util.Arrays;

/** Observational statistics, not a heap-leak classifier: no forced GC or universal memory threshold. */
final class EnduranceStatistics {
    record Summary(int count, double first, double last, double min, double max, double mean,
            double p95, double slope, double earlyMean, double lateMean) { }

    static Summary summarize(double[] values, int warmup) {
        if (warmup < 0 || warmup >= values.length) throw new IllegalArgumentException("warmup");
        double[] v = Arrays.copyOfRange(values, warmup, values.length);
        double sum = 0, weighted = 0;
        for (int i = 0; i < v.length; i++) {
            if (!Double.isFinite(v[i])) throw new IllegalArgumentException("Non-finite sample");
            sum += v[i]; weighted += i * v[i];
        }
        int n = v.length, window = Math.min(5, n);
        double x = (n - 1) / 2.0, mean = sum / n;
        double denominator = n * (double)(n * (double)n - 1) / 12;
        double slope = n == 1 ? 0 : (weighted - n * x * mean) / denominator;
        double early = 0, late = 0;
        for (int i = 0; i < window; i++) { early += v[i]; late += v[n - window + i]; }
        double first = v[0], last = v[n - 1];
        Arrays.sort(v);
        return new Summary(n, first, last, v[0], v[n - 1], mean, v[(int)Math.ceil(n * .95) - 1], slope,
                early / window, late / window);
    }
}
