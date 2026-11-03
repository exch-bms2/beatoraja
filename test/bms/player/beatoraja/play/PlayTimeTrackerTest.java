package bms.player.beatoraja.play;

import bms.player.beatoraja.play.BMSPlayer.PlayTimeTracker;

/** Real elapsed time must exclude chart offsets, ending margin, and result transitions. */
public final class PlayTimeTrackerTest {
    public static void main(String[] args) {
        PlayTimeTracker tracker = new PlayTimeTracker();
        equal(-1, tracker.getPlaytime());
        tracker.stop(1000000, 0);
        equal(-1, tracker.getPlaytime());

        tracker.start(10000000, 0, 2000000);
        tracker.update(11000000, 1000000);
        equal(1000, tracker.getPlaytime());
        tracker.update(12100000, 2100000);
        equal(2000, tracker.getPlaytime());
        tracker.update(17000000, 7000000);
        tracker.stop(22000000, 12000000);
        equal(2000, tracker.getPlaytime());

        tracker.start(30000000, 0, 2000000);
        tracker.update(30500000, 500000);
        tracker.stop(30750000, 750000);
        equal(750, tracker.getPlaytime());
        tracker.stop(32000000, 2000000);
        equal(750, tracker.getPlaytime());

        tracker.start(0, 0, 2000000);
        tracker.update(1250000, 2500000);
        equal(1000, tracker.getPlaytime());
        tracker.start(0, 0, 2000000);
        tracker.update(5000000, 2500000);
        equal(4000, tracker.getPlaytime());

        tracker.start(0, 0, 3000000);
        tracker.update(1000000, 1000000);
        tracker.update(2000000, 3000000);
        equal(2000, tracker.getPlaytime());

        tracker.start(0, 1000000, 3000000);
        tracker.update(2000000, 3000000);
        equal(2000, tracker.getPlaytime());
        tracker.start(0, 0, 3000000);
        tracker.update(1000000, 1000000);
        tracker.stop(1250999, 1250999);
        equal(1250, tracker.getPlaytime());

        tracker.start(0, 0, 0);
        tracker.stop(5000000, 5000000);
        equal(0, tracker.getPlaytime());
        System.out.println("PlayTimeTrackerTest passed");
    }

    private static void equal(long expected, long actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but was " + actual);
    }
}
