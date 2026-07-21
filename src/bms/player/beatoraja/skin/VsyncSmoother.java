// Copyright (C) 2025 Park Hyunwoo
//
// This file is part of beatoraja.
//
// beatoraja is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// beatoraja is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with beatoraja.  If not, see <https://www.gnu.org/licenses/>.

package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;

public class VsyncSmoother {
    private float targetTime = 0;  // Output value

    public void reset() {
        targetTime = 0;
    }

    /**
     * Smooths out [newTime] to more evenly-spaced values.
     *
     * @param newTime Raw input time in milliseconds
     * @return Smoothed out time.
     */
    public long smoothTime(long newTime) {
        float timeInterval = 1000.0f / Gdx.graphics.getDisplayMode().refreshRate;

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
        if (newTime - targetTime >= timeInterval * 2.02f) {
            // Allow additional 0.02f to prevent false positive
            long frameSkips = (long) (Math.floor((newTime - targetTime) / timeInterval) - 1);
            targetTime += timeInterval * frameSkips;
        }

        // Some beatoraja code just spams a bunch of `time=0` before starting.
        if (timeInterval == 0) {
            return newTime;
        }

        float timeDiff = targetTime - newTime - timeInterval;
        float targetTimeDiff = timeInterval;
        if (timeDiff < -timeInterval) {
            targetTimeDiff += Math.min(timeInterval, (-timeDiff - timeInterval) / 2 + 0.05f * timeInterval);
        } else if (timeDiff < timeInterval) {
            targetTimeDiff -= timeDiff * 0.05f;
            // Do nothing
        } else {
            targetTimeDiff -= Math.min(timeInterval, (timeDiff - timeInterval) / 2 + 0.05f * timeInterval);
        }
        targetTime += targetTimeDiff;
        return (long) Math.round(targetTime);
    }
}
