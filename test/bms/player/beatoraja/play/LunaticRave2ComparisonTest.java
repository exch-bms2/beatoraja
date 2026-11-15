package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.model.MineNote;
import bms.player.beatoraja.BMSPlayerMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static bms.player.beatoraja.play.AbstractGrooveGauge.*;

/** Fixed inputs against both production rules, with hand-derived OpenLR2 expectations. */
public final class LunaticRave2ComparisonTest {
    private static final String BOUNDARIES = "boundaries.bms";
    private static final String DENSE = "dense.bms";

    private enum Judge { PG, GR, GD, BAD, MISS, EMPTY }

    private record Tick(int ms, boolean press, String reference, String current) {}

    private record GaugeStep(Judge judge, double reference, double current) {}

    public static void main(String[] args) throws Exception {
        var failures = new ArrayList<String>();
        check(BMSPlayerRule.getBMSPlayerRule(Mode.BEAT_7K) == BMSPlayerRule.Beatoraja_7, "7KEY default rule");
        check(BMSPlayerRuleSet.LunaticRave2.ruleset[0] == BMSPlayerRule.LunaticRave2, "LunaticRave2 rule set");
        check(BMSPlayerRule.LunaticRave2.gauge == GaugeProperty.LR2, "existing gauge preset");
        checkRuleSelection();
        checkRankAndTotalParameters();
        checkDecodedRankCompatibility();

        // Individual inputs around the OpenLR2 windows, for a note at 2109.375 ms.
        for (var tick : List.of(
                hit(2090, "GR/1/1", "GR/1/1"), hit(2091, "PG/1/1", "GR/1/1"),
                hit(2127, "PG/1/1", "GR/1/1"), hit(2128, "GR/1/1", "GR/1/1"),
                hit(2068, "GD/1/1", "GR/1/1"), hit(2069, "GR/1/1", "GR/1/1"),
                hit(2149, "GR/1/1", "GR/1/1"), hit(2150, "GD/1/1", "GR/1/1"),
                hit(2008, "BAD/1/0", "GD/1/1"), hit(2009, "GD/1/1", "GD/1/1"),
                hit(2209, "GD/1/1", "GD/1/1"), hit(2210, "BAD/1/0", "GD/1/1"),
                hit(1908, "EMPTY/0/0", "EMPTY/0/0"), hit(1909, "BAD/1/0", "EMPTY/0/0"),
                hit(2309, "BAD/1/0", "BAD/1/0"), hit(2310, "MISS/1/0", "BAD/1/0"),
                hit(1944, "BAD/1/0", "EMPTY/0/0"), hit(1945, "BAD/1/0", "BAD/1/0"),
                hit(2094, "PG/1/1", "GR/1/1"), hit(2095, "PG/1/1", "PG/1/1"),
                hit(2124, "PG/1/1", "PG/1/1"), hit(2125, "PG/1/1", "GR/1/1"))) {
            compareInput(failures, "boundary", BOUNDARIES, tick);
        }
        compareInput(failures, "early-empty-and-recovery", BOUNDARIES,
                hit(1109, "-/0/0", "-/0/0"), hit(1110, "EMPTY/0/0", "-/0/0"),
                hit(1609, "EMPTY/0/0", "-/0/0"), hit(1610, "EMPTY/0/0", "EMPTY/0/0"),
                hit(2109, "PG/1/1", "PG/1/1"));
        compareInput(failures, "no-input-miss", BOUNDARIES,
                idle(2309, "-/0/0", "-/0/0"), idle(2310, "MISS/1/0", "-/0/0"),
                idle(2319, "-/1/0", "-/0/0"), idle(2320, "-/1/0", "MISS/1/0"));
        compareInput(failures, "repress-after-hit", BOUNDARIES,
                hit(2109, "PG/1/1", "PG/1/1"), hit(2150, "-/1/1", "EMPTY/1/1"));
        compareInput(failures, "repress-after-miss", BOUNDARIES,
                idle(2320, "MISS/1/0", "MISS/1/0"), hit(2321, "-/1/0", "-/1/0"));
        compareInput(failures, "dense-single-press", DENSE,
                hit(2125, "PG+BAD/3/1", "PG/2/1"), idle(2500, "-/3/1", "MISS/3/0"));
        compareInput(failures, "dense-good-does-not-chain", DENSE,
                hit(2100, "GD/1/1", "GD/1/1"), hit(2125, "PG/3/2", "PG/3/2"));

        compareGauge(failures, "NORMAL low", NORMAL, 31.5f, step(Judge.BAD, 27.5, 28.5));
        compareGauge(failures, "EASY low", EASY, 31.5f, step(Judge.BAD, 28.3, 30));
        compareGauge(failures, "NORMAL floor", NORMAL, 3, step(Judge.MISS, 2, 2), step(Judge.PG, 100, 100));
        compareGauge(failures, "EASY floor", EASY, 3, step(Judge.MISS, 2, 2), step(Judge.PG, 100, 100));
        compareGauge(failures, "HARD below 32", HARD, 31.5f, step(Judge.EMPTY, 19.5, 28));
        compareGauge(failures, "HARD at 32", HARD, 32, step(Judge.EMPTY, 12, 28.5));
        compareGauge(failures, "HARD at 2 after damage", HARD, 14,
                step(Judge.EMPTY, 2, 11.5), step(Judge.PG, 2.1, 11.65));
        compareGauge(failures, "HARD below 2 after damage", HARD, 13,
                step(Judge.EMPTY, 1, 10.5), step(Judge.PG, 0, 10.65));
        compareGauge(failures, "HARD same seed at 2", HARD, 2, step(Judge.PG, 2.1, 2.15));
        compareGauge(failures, "HARD same seed below 2", HARD, 1.99f, step(Judge.PG, 0, 2.14));
        compareGauge(failures, "HARD at zero after damage", HARD, 4,
                step(Judge.MISS, 0, 0), step(Judge.PG, 0, 0));
        checkIntegratedGauge();

        if (!failures.isEmpty()) throw new AssertionError(String.join(System.lineSeparator(), failures));
        System.out.println("LunaticRave2ComparisonTest passed");
    }

    private static Tick hit(int ms, String reference, String current) {
        return new Tick(ms, true, reference, current);
    }

    private static Tick idle(int ms, String reference, String current) {
        return new Tick(ms, false, reference, current);
    }

    private static GaugeStep step(Judge judge, double reference, double current) {
        return new GaugeStep(judge, reference, current);
    }

    private static void compareInput(List<String> failures, String name, String file, Tick... ticks) throws Exception {
        var model = load(file);
        var lr2Model = load(file);
        var chart = BMSPlayerRule.ChartParameters.fromBms(lr2Model);
        BMSPlayerRule.validate(model);
        BMSPlayerRule.validate(lr2Model);
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true);
        var lr2 = new KeyboardJudgeCompatibilityTest.Fixture(lr2Model, true, BMSPlayerRule.LunaticRave2, chart, NORMAL);
        var current = new InputRun(fixture, laneNotes(model, 0));
        var reference = new InputRun(lr2, laneNotes(lr2Model, 0));
        for (var tick : ticks) {
            String label = name + " @" + tick.ms() + " ms";
            String actual = current.advance(tick);
            String lr2Actual = reference.advance(tick);
            System.out.printf("%-39s OpenLR2=%-12s LunaticRave2=%-12s beatoraja=%s%n",
                    label, tick.reference(), lr2Actual, actual);
            if (!tick.current().equals(actual)) failures.add(label + ": beatoraja expected " + tick.current() + ", got " + actual);
            if (!tick.reference().equals(lr2Actual)) failures.add(label + ": LunaticRave2 expected " + tick.reference() + ", got " + lr2Actual);
        }
    }

    private record InputRun(KeyboardJudgeCompatibilityTest.Fixture fixture, List<Note> notes, int[] previous) {
        InputRun(KeyboardJudgeCompatibilityTest.Fixture fixture, List<Note> notes) {
            this(fixture, notes, new int[Judge.values().length]);
        }

        String advance(Tick tick) {
            long us = tick.ms() * 1_000L;
            if (tick.press()) {
                fixture.input.setKeyState(0, true, us);
                fixture.input.setKeyState(0, false, us);
            }
            fixture.judge.update(us);
            var judges = new ArrayList<String>();
            for (var judge : Judge.values()) {
                int index = judge.ordinal();
                int count = fixture.judge.getJudgeCount(index);
                for (int i = previous[index]; i < count; i++) judges.add(judge.name());
                previous[index] = count;
            }
            int consumed = 0;
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getState() != 0) consumed |= 1 << i;
            }
            return (judges.isEmpty() ? "-" : String.join("+", judges))
                    + "/" + consumed + "/" + fixture.judge.getCombo();
        }
    }

    private static void compareGauge(List<String> failures, String name, int type, float initial, GaugeStep... steps)
            throws Exception {
        var model = load(BOUNDARIES);
        var chart = BMSPlayerRule.ChartParameters.fromBms(model);
        BMSPlayerRule.validate(model);
        var gauge = new GrooveGauge(model, type, BMSPlayerRule.getBMSPlayerRule(model.getMode()).gauge);
        var lr2 = new KeyboardJudgeCompatibilityTest.Fixture(model, true, BMSPlayerRule.LunaticRave2, chart, type).gauge;
        gauge.setValue(type, initial);
        lr2.setValue(type, initial);
        for (var step : steps) {
            gauge.update(step.judge().ordinal());
            lr2.update(step.judge().ordinal());
            float actual = gauge.getValue();
            float lr2Actual = lr2.getValue();
            String label = name + " (" + initial + "%) / " + step.judge();
            System.out.printf(Locale.ROOT, "%-39s OpenLR2=%-8.2f LunaticRave2=%-8.2f beatoraja=%.2f%n",
                    label, step.reference(), lr2Actual, actual);
            if (!(Math.abs(step.current() - actual) < 0.0001)) {
                failures.add(label + ": expected " + step.current() + ", got " + actual);
            }
            if (!(Math.abs(step.reference() - lr2Actual) < 0.0001)) {
                failures.add(label + ": LunaticRave2 expected " + step.reference() + ", got " + lr2Actual);
            }
            check(lr2.isFailed() == (type == HARD && step.reference() < 2), label + ": LR2 failure threshold");
            check(gauge.isFailed() == (step.current() == 0), label + ": standard failure threshold");
        }
    }

    private static void checkRuleSelection() throws IOException {
        var model = load(BOUNDARIES);
        var chart = BMSPlayerRule.ChartParameters.fromBms(model);
        var lr2 = BMSPlayerRule.LunaticRave2;
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart) == lr2, "opt-in");
        check(BMSPlayerRule.select(BMSPlayerRule.Beatoraja_7, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart)
                == BMSPlayerRule.Beatoraja_7, "default");
        for (var mode : List.of(BMSPlayerMode.REPLAY_1, BMSPlayerMode.PRACTICE, BMSPlayerMode.AUTOPLAY)) {
            check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, mode, false, HARD, chart) != lr2, "other play modes");
        }
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, true, HARD, chart) != lr2, "course");
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, EXHARD, chart) != lr2, "other gauge");
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, null) != lr2, "not BMS");
        model.setMode(Mode.BEAT_14K);
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart) != lr2, "DP");
        model.setMode(Mode.BEAT_7K);
        model.getAllTimeLines()[0].setNote(0, new MineNote(0, 10));
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart) == lr2, "mine");
        model.getAllTimeLines()[0].setNote(0, new LongNote(0));
        check(BMSPlayerRule.select(lr2, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, HARD, chart) != lr2, "unpaired long note");
    }

    private static void checkRankAndTotalParameters() {
        int[] pgWindows = {8, 15, 18, 21, 18};
        for (int rank = 0; rank < pgWindows.length; rank++) {
            var rules = new LunaticRave2Rules(new BMSPlayerRule.ChartParameters(rank, 300), 500);
            int pg = pgWindows[rank];
            check(rules.judge(pg) == 0 && rules.judge(-pg - 1) == 1, "PG boundary for rank " + rank);
        }
        var missingTotal = new LunaticRave2Rules(new BMSPlayerRule.ChartParameters(2, 0), 500);
        check(Math.abs(missingTotal.gaugeChanges(NORMAL, 500)[0] - 0.512) < 0.0001, "undefined TOTAL");
        var lowTotal = new LunaticRave2Rules(new BMSPlayerRule.ChartParameters(2, 100.9), 1_000);
        check(lowTotal.gaugeChanges(HARD, 1_000)[Judge.MISS.ordinal()] == -100, "low TOTAL HARD damage");
        check(lowTotal.gaugeChanges(NORMAL, 1_000)[0] == 0.1, "fractional TOTAL is truncated");
    }

    private static void checkIntegratedGauge() throws Exception {
        var model = load(DENSE);
        var chart = BMSPlayerRule.ChartParameters.fromBms(model);
        BMSPlayerRule.validate(model);
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, BMSPlayerRule.LunaticRave2, chart, HARD);
        fixture.input.setKeyState(0, true, 2_125_999);
        fixture.input.setKeyState(0, false, 2_126_000);
        fixture.judge.update(2_500_000);
        check(fixture.judge.getScoreData().getRule() == BMSPlayerRule.LunaticRave2, "score rule marker");
        check(fixture.judge.getJudgeCount(Judge.BAD.ordinal()) == 1
                && fixture.judge.getJudgeCount(Judge.PG.ordinal()) == 1, "delayed poll preserves input time");
        check(Math.abs(fixture.gauge.getValue() - 40.1) < 0.0001, "BAD then PG reached the gauge in order");
    }

    private static void checkDecodedRankCompatibility() throws Exception {
        String rankFirst = "#RANK 0\n#DEFEXRANK 100\n";
        String rankLast = "#DEFEXRANK 100\n#RANK 0\n";
        var lr2 = BMSPlayerRule.LunaticRave2;
        var standard = BMSPlayerRule.Beatoraja_7;
        for (String headers : List.of(rankFirst, rankLast)) {
            checkDecodedRank(headers, lr2, 8, Judge.PG);
            checkDecodedRank(headers, lr2, 9, Judge.GR);
        }
        checkDecodedRank(rankFirst, standard, 8, Judge.PG);
        checkDecodedRank(rankLast, standard, 8, Judge.GR);
        for (String headers : List.of("", "#DEFEXRANK 100\n")) {
            checkDecodedRank(headers, lr2, 18, Judge.PG);
            checkDecodedRank(headers, lr2, 19, Judge.GR);
            checkDecodedRank(headers, standard, 18, Judge.GR);
        }
    }

    private static void checkDecodedRank(String headers, BMSPlayerRule rule, int lateMs, Judge expected) throws Exception {
        String bms = "#PLAYER 1\n#BPM 120\n#TOTAL 300\n#WAV01 silent.wav\n"
                + headers + "#00111:01\n#00219:01\n";
        var model = new BMSDecoder().decode(bms.getBytes(StandardCharsets.US_ASCII), "rank-compatibility.bms",
                false, new int[0]);
        check(model != null && model.getMode() == Mode.BEAT_7K, "rank compatibility fixture");
        var chart = BMSPlayerRule.ChartParameters.fromBms(model);
        BMSPlayerRule.validate(model);
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, rule, chart, NORMAL);
        long press = 2_000_000 + lateMs * 1_000L;
        fixture.input.setKeyState(0, true, press);
        fixture.judge.update(press);
        check(fixture.judge.getPastNotes() == 1 && fixture.judge.getJudgeCount(expected.ordinal()) == 1,
                rule + " expected " + expected + " at +" + lateMs + " ms for headers " + headers.replace('\n', ' '));
    }

    private static BMSModel load(String file) throws IOException {
        try (var input = Objects.requireNonNull(LunaticRave2ComparisonTest.class
                .getResourceAsStream("/play/lunatic-rave2/" + file), file)) {
            var model = new BMSDecoder().decode(input.readAllBytes(), file, false, new int[0]);
            check(model != null && model.getMode() == Mode.BEAT_7K, file + ": expected single 7KEY chart");
            check(model.getJudgerankType() == BMSModel.JudgeRankType.BMS_RANK && model.getJudgerank() == 2
                    && model.getTotal() == 300, file + ": RANK/TOTAL");
            var notes = laneNotes(model, 0);
            if (file.equals(BOUNDARIES)) {
                check(model.getTotalNotes() == 2 && notes.size() == 1 && notes.get(0).getMicroTime() == 2_109_375,
                        file + ": expected one target at 2109375 us");
            } else {
                check(model.getTotalNotes() == 3 && notes.size() == 2 && notes.get(0).getMicroTime() == 2_000_000
                        && notes.get(1).getMicroTime() == 2_125_000, file + ": expected targets 125 ms apart");
            }
            return model;
        }
    }

    private static List<Note> laneNotes(BMSModel model, int lane) {
        var notes = new ArrayList<Note>();
        for (var timeline : model.getAllTimeLines()) {
            if (timeline.getNote(lane) != null) notes.add(timeline.getNote(lane));
        }
        return notes;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
