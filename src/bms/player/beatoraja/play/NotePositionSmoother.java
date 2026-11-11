package bms.player.beatoraja.play;

/** A bounded phase correction for note geometry, never a gameplay clock. */
final class NotePositionSmoother {
    static final long MAX_OFFSET_MICROS = 2_000;
    private static final int STABLE_INTERVALS = 8;
    private static final double MAX_INTERVAL_ERROR = 0.15;
    private static final double MAX_AVERAGE_ERROR = 0.03;
    private long previousTime;
    private long clockOrigin;
    private double framePeriod;
    private double offset;
    private final long[] intervals = new long[STABLE_INTERVALS];
    private int cappedFps;
    private long cappedOrigin;
    private long previousFrameTime;
    private int intervalCount;
    private int intervalIndex;
    private long intervalSum;

    void reset() {
        cappedFps = 0;
        clearIntervals();
    }

    long sample(long chartTime, long origin, int refreshRate) {
        if (cappedFps != 0) reset();
        if (refreshRate <= 0) {
            reset();
            return 0;
        }
        return samplePhase(chartTime, origin, 1_000_000.0 / refreshRate);
    }

    long sampleCapped(long chartTime, long origin, int fps, long frameTime) {
        if (fps <= 0) {
            reset();
            return 0;
        }
        if (fps != cappedFps || origin != cappedOrigin) {
            clearIntervals();
            cappedFps = fps;
            cappedOrigin = origin;
            previousFrameTime = frameTime;
            return 0;
        }
        final double period = 1_000_000.0 / fps;
        final long interval = frameTime - previousFrameTime;
        // Several draws can share a render timestamp; only count distinct frames.
        if (interval == 0) return Math.round(offset);
        previousFrameTime = frameTime;
        if (Math.abs(interval - period) > period * MAX_INTERVAL_ERROR) {
            clearIntervals();
            return 0;
        }
        if (intervalCount == STABLE_INTERVALS) intervalSum -= intervals[intervalIndex];
        else intervalCount++;
        intervals[intervalIndex] = interval;
        intervalSum += interval;
        intervalIndex = (intervalIndex + 1) % STABLE_INTERVALS;
        if (intervalCount < STABLE_INTERVALS
                || Math.abs(intervalSum - period * STABLE_INTERVALS) > period * STABLE_INTERVALS * MAX_AVERAGE_ERROR) {
            resetPhase();
            return 0;
        }
        if (chartTime == previousTime) resetPhase(); // A stale skin prepare is not a fresh position sample.
        return samplePhase(chartTime, origin, period);
    }

    private void clearIntervals() {
        intervalCount = 0;
        intervalIndex = 0;
        intervalSum = 0;
        resetPhase();
    }

    private void resetPhase() {
        framePeriod = 0;
        offset = 0;
    }

    private long samplePhase(long chartTime, long origin, double period) {
        if (chartTime == previousTime && period == framePeriod && origin == clockOrigin) {
            return Math.round(offset);
        }
        final long delta = chartTime - previousTime;
        previousTime = chartTime;
        if (period != framePeriod || origin != clockOrigin || delta < period * 0.5 || delta > period * 1.5) {
            framePeriod = period;
            clockOrigin = origin;
            offset = 0;
            return 0;
        }
        final double limit = Math.min(MAX_OFFSET_MICROS, period * 0.25);
        offset = Math.max(-limit, Math.min(limit, offset * 0.9 + period - delta));
        return Math.round(offset);
    }

    static double positionOffset(long time, long offset, long scrollStart, long scrollEnd, double sectionPixels) {
        if (offset == 0 || time <= scrollStart || time >= scrollEnd) return 0;
        // Fade the correction out at either edge; never extrapolate through a STOP or tempo/scroll change.
        final long limit = Math.min(time - scrollStart, scrollEnd - time) / 2;
        final long shift = Math.max(-limit, Math.min(limit, offset));
        return -sectionPixels * shift / (scrollEnd - scrollStart);
    }
}
