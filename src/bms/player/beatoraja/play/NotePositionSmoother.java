package bms.player.beatoraja.play;

/** A bounded phase correction for note geometry, never a gameplay clock. */
final class NotePositionSmoother {
    static final long MAX_OFFSET_MICROS = 2_000;
    private long previousTime;
    private long clockOrigin;
    private double framePeriod;
    private double offset;

    void reset() {
        framePeriod = 0;
        offset = 0;
    }

    long sample(long chartTime, long origin, int refreshRate) {
        if (refreshRate <= 0) {
            reset();
            return 0;
        }
        final double period = 1_000_000.0 / refreshRate;
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
