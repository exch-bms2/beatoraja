package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.player.beatoraja.BMSPlayerMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static bms.player.beatoraja.play.BMSPlayerRule.Beatoraja_7;
import static bms.player.beatoraja.play.BMSPlayerRule.LunaticRave2;
import static bms.player.beatoraja.play.AbstractGrooveGauge.HARD;

/** OpenLR2 expectations are source-derived, not captured from a running LR2. */
public final class LunaticRave2LongNoteComparisonTest {
    private static final int KEY = 0;
    private static final int SCRATCH = 7;
    private static final int REVERSE = 8;
    private record Edge(int key, boolean down, long us) {}
    private record Result(int pg, int gr, int gd, int bad, int miss, int empty) {}

    private static final Result PG = new Result(1, 0, 0, 0, 0, 0);
    private static final Result GR = new Result(0, 1, 0, 0, 0, 0);
    private static final Result GD = new Result(0, 0, 1, 0, 0, 0);
    private static final Result BAD = new Result(0, 0, 0, 1, 0, 0);
    private static final Result MISS = new Result(0, 0, 0, 0, 1, 0);
    private static final Result PG_WITH_EMPTY = new Result(1, 0, 0, 0, 0, 1);

    public static void main(String[] args) throws Exception {
        var failures = new ArrayList<String>();
        compare(failures, "LN head PG edge", 0, PG, GR, down(KEY, 1_982_000));
        compare(failures, "LN head outside PG", 0, GR, GR, down(KEY, 1_981_000));
        compare(failures, "LN early head BAD edge", 0, BAD, new Result(0, 0, 0, 0, 1, 1), down(KEY, 1_800_000));
        compare(failures, "LN head late GOOD edge", 0, GD, GD, down(KEY, 2_100_000));
        compare(failures, "LN late input becomes miss", 0, MISS, GD, down(KEY, 2_101_000));
        compare(failures, "LN missed without input", 0, MISS, MISS);
        compare(failures, "LN early empty then hit", 0, PG_WITH_EMPTY, PG,
                down(KEY, 1_001_000), up(KEY, 1_002_000), down(KEY, 2_000_000));
        compare(failures, "LN release at GOOD boundary", 0, PG, GR,
                down(KEY, 2_000_000), up(KEY, 2_900_000));
        compare(failures, "LN release outside GOOD boundary", 0, BAD, GR,
                down(KEY, 2_000_000), up(KEY, 2_899_999));
        compare(failures, "LN released at exact end", 0, GR, GR,
                down(KEY, 1_980_000), up(KEY, 3_000_000));
        compare(failures, "LN early release cannot be rescued", 0, BAD, BAD,
                down(KEY, 2_000_000), up(KEY, 2_500_000), down(KEY, 2_501_000));
        compare(failures, "scratch held to end without reverse", 7, PG, PG, down(SCRATCH, 4_000_000));
        compare(failures, "scratch reversed while both directions overlap", 7, PG, BAD,
                down(SCRATCH, 4_000_000), down(REVERSE, 4_400_000), up(SCRATCH, 4_401_000));
        compare(failures, "scratch same-timestamp reverse (release first)", 7, PG, PG,
                down(SCRATCH, 4_000_000), up(SCRATCH, 4_400_000), down(REVERSE, 4_400_000));
        compare(failures, "scratch same-timestamp reverse (press first)", 7, PG, BAD,
                down(SCRATCH, 4_000_000), down(REVERSE, 4_400_000), up(SCRATCH, 4_400_000));
        compare(failures, "scratch reversal with a real gap", 7, BAD, BAD,
                down(SCRATCH, 4_000_000), up(SCRATCH, 4_400_000), down(REVERSE, 4_401_000));
        compare(failures, "scratch reverse direction released at boundary", 7, PG, GR,
                down(REVERSE, 4_000_000), up(REVERSE, 4_900_000));
        checkHoldAndGauge();
        checkRankDependentHeadDeadline();
        if (!failures.isEmpty()) throw new AssertionError(String.join(System.lineSeparator(), failures));
        System.out.println("LunaticRave2LongNoteComparisonTest passed");
    }

    private static Edge down(int key, long us) { return new Edge(key, true, us); }
    private static Edge up(int key, long us) { return new Edge(key, false, us); }

    private static void compare(List<String> failures, String name, int lane, Result openLr2, Result standard,
                                Edge... edges) throws Exception {
        long now = lane == 0 ? 3_400_000 : 5_400_000;
        Result actualLr2 = play(LunaticRave2, lane, now, edges);
        Result actualStandard = play(Beatoraja_7, lane, now, edges);
        System.out.printf("%s: OpenLR2=%s LunaticRave2=%s beatoraja=%s%n", name, openLr2, actualLr2, actualStandard);
        if (!openLr2.equals(actualLr2)) failures.add(name + ": LunaticRave2 expected " + openLr2 + ", got " + actualLr2);
        if (!standard.equals(actualStandard)) failures.add(name + ": beatoraja expected " + standard + ", got " + actualStandard);
    }

    private static Result play(BMSPlayerRule rule, int lane, long now, Edge[] edges) throws Exception {
        var fixture = fixture(rule);
        // Resolve the unrelated key LN before checking scratch so the fixture exercises both lanes.
        if (lane == 7) {
            fixture.input.setKeyState(KEY, true, 2_000_000);
            fixture.judge.update(3_100_000);
        }
        for (var edge : edges) fixture.input.setKeyState(edge.key(), edge.down(), edge.us());
        fixture.judge.update(now);
        check(fixture.judge.getProcessingLongNote(lane) == null, "hold ended");
        check(fixture.judge.getPastNotes() == (lane == 7 ? 2 : 1), "each LN counts once");
        var judge = fixture.judge;
        return new Result(judge.getJudgeCount(0) - (lane == 7 ? 1 : 0), judge.getJudgeCount(1), judge.getJudgeCount(2),
                judge.getJudgeCount(3), judge.getJudgeCount(4), judge.getJudgeCount(5));
    }

    private static void checkHoldAndGauge() throws Exception {
        var fixture = fixture(LunaticRave2);
        fixture.gauge.setValue(HARD, 80);
        fixture.input.setKeyState(KEY, true, 1_982_000);
        fixture.judge.update(3_000_999);
        check(fixture.judge.getProcessingLongNote(KEY) != null && fixture.judge.getPastNotes() == 0
                && fixture.gauge.getValue() == 80, "exact end millisecond does not score or change gauge");
        fixture.judge.update(3_001_000);
        check(fixture.judge.getProcessingLongNote(KEY) == null && fixture.judge.getPastNotes() == 1
                && fixture.judge.getJudgeCount(0, true) == 1
                && Math.abs(fixture.gauge.getValue() - 80.1) < 0.0001, "stored head judgment scores exactly once");
        fixture.input.setKeyState(KEY, false, 3_002_000);
        fixture.judge.update(3_100_000);
        check(fixture.judge.getPastNotes() == 1, "release after automatic end does not score again");
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(BMSPlayerRule rule) throws Exception {
        return fixture(rule, 2);
    }

    private static void checkRankDependentHeadDeadline() throws Exception {
        int[] goodMs = {40, 60, 100, 120, 100};
        for (int rank = 0; rank < goodMs.length; rank++) {
            long boundary = 2_000_000 + goodMs[rank] * 1_000L;
            String label = "RANK " + rank;
            var idle = fixture(LunaticRave2, rank);
            idle.judge.update(boundary - 1_000);
            check(idle.judge.getPastNotes() == 0, label + ": before boundary without input");
            idle.judge.update(boundary + 999);
            check(idle.judge.getPastNotes() == 0, label + ": at boundary without input");
            idle.judge.update(boundary + 1_000);
            check(idle.judge.getJudgeCount(4) == 1, label + ": miss after boundary without input");

            for (long offset : new long[]{-1_000, 0, 1_000}) {
                var pressed = fixture(LunaticRave2, rank);
                pressed.input.setKeyState(KEY, true, boundary + offset);
                pressed.judge.update(boundary + offset);
                check((pressed.judge.getProcessingLongNote(KEY) != null) == (offset <= 0),
                        label + ": head accepted only through the GOOD boundary");
                pressed.judge.update(3_001_000);
                check(pressed.judge.getPastNotes() == 1
                        && pressed.judge.getJudgeCount(offset <= 0 ? 2 : 4) == 1,
                        label + ": expected GOOD or MISS at boundary offset " + offset);
            }
        }
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(BMSPlayerRule rule, int rank) throws Exception {
        var model = load(rank);
        var chart = BMSPlayerRule.ChartParameters.fromBms(model);
        check(BMSPlayerRule.select(rule, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart) == rule,
                "eligible ordinary LN and long scratch chart");
        BMSPlayerRule.validate(model);
        return new KeyboardJudgeCompatibilityTest.Fixture(model, true, rule, chart, HARD);
    }

    private static BMSModel load(int rank) throws IOException {
        try (var stream = Objects.requireNonNull(LunaticRave2LongNoteComparisonTest.class
                .getResourceAsStream("/play/lunatic-rave2/long-notes.bms"))) {
            String chart = new String(stream.readAllBytes(), StandardCharsets.US_ASCII)
                    .replace("#RANK 2", "#RANK " + rank);
            var model = new BMSDecoder().decode(chart.getBytes(StandardCharsets.US_ASCII), "long-notes.bms", false, new int[0]);
            check(model != null && model.getMode() == Mode.BEAT_7K && model.getTotalNotes() == 3, "7KEY LN fixture");
            check(Objects.equals(model.getJudgeType().bmsRank(), rank), "fixture RANK");
            var lanes = model.getLanes();
            checkPair(lanes[0].getNotes(), 2_000_000, 3_000_000);
            checkPair(lanes[7].getNotes(), 4_000_000, 5_000_000);
            return model;
        }
    }

    private static void checkPair(Note[] notes, long start, long end) {
        check(notes.length == 2 && notes[0] instanceof LongNote head && head.getPair() == notes[1]
                && head.getMicroTime() == start && notes[1].getMicroTime() == end, "expected LN pair");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
