package bms.player.beatoraja.skin;

public class TimeSmoother {
    float averageDeltaSmoother = 0.03f;
    final float maxAverageDeltaSmoother = 0.97f;

    private float targetTime = 0;  // Output value
    private float averageDelta = 0;  // Moving average of intervals between `smooth` function call. This approximates `1/fps`.
    private long prevTime = 0;

    public void reset() {
        prevTime = 0;
        averageDelta = 0;
        targetTime = 0;
    }

    /**
     * Smooths out [newTime] to more evenly-spaced values.
     * @param newTime Raw input time
     * @return Smoothed out time.
     */
    public long smoothTime(long newTime) {
        averageDelta = averageDelta * averageDeltaSmoother + (newTime - prevTime) * (1 - averageDeltaSmoother);

        if (averageDelta > 0.001) {
            averageDeltaSmoother = Math.min(averageDeltaSmoother + 0.1f, maxAverageDeltaSmoother);
            // Detect frame skip and advance targetTime
            // Note: this might be due to display HZ change, but this will be averaged out in `averageDelta` as
            // the time passes by, and `targetTime` will follow up.

            // newTime = [true display time] - α
            // we expect α to be in range of [0, averageDelta)
            // where targetTime tries to estimate [true display time] - α. (There is no way to expect α as I know of)
            //
            // let targetTime = [true display time] - β
            // abs(α - β) < averageDelta (must) -> if (a - b) > averageDelta → miss

            // targetTime ~= [previous display time] - β
            // newTime - targetTime = ([true display time] - α) - ([previous display time] - β)
            //                      = averageDelta + (β - α) <= averageDelta + averageDelta
            // if this inequality misses, we assume that the frame skip occurred
            if (newTime - targetTime >= averageDelta * 2.02f) {
                // Allow additional 0.02f to prevent false positive
                long frameSkips = (long) (Math.floor((newTime - targetTime) / averageDelta) - 1);
                targetTime += averageDelta * frameSkips;
            }
        }
        prevTime = newTime;

        // Some beatoraja code just spams a bunch of `time=0` before starting.
        if (averageDelta == 0) {
            return newTime;
        }

        targetTime = targetTime + averageDelta;
        float timeDiff = targetTime - newTime;

        if (timeDiff < -averageDelta) {
            targetTime += Math.min(averageDelta, (-timeDiff - averageDelta) / 2 + 0.05f * averageDelta);
        } else if (timeDiff < averageDelta) {
            targetTime -= timeDiff * 0.05f;
            // Do nothing
        } else {
            targetTime -= Math.min(averageDelta * 0.95f, (timeDiff - averageDelta) / 2 + 0.05f * averageDelta);
        }
        return (long) Math.round(targetTime);
    }
}
