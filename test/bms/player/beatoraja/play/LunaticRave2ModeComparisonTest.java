package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.LongNote;
import bms.model.MineNote;
import bms.model.Mode;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayDataAccessor;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static bms.player.beatoraja.play.BMSPlayerRule.LunaticRave2;
import static bms.player.beatoraja.play.AbstractGrooveGauge.HARD;
import static bms.player.beatoraja.play.AbstractGrooveGauge.NORMAL;

/** Exercises real chart channel mappings with OpenLR2-derived expectations. */
public final class LunaticRave2ModeComparisonTest {
    private record Chart(String file, Mode mode, int left, int right, int leftHold, int rightHold) {
        boolean hasScratch() { return mode != Mode.POPN_9K; }
        boolean isDp() { return mode.player == 2; }
    }

    private static final Chart[] CHARTS = {
            new Chart("5key.bms", Mode.BEAT_5K, 0, 4, 5, 5),
            new Chart("9key.pms", Mode.POPN_9K, 0, 8, 8, 8),
            new Chart("10key.bms", Mode.BEAT_10K, 0, 7, 5, 12),
            new Chart("14key.bms", Mode.BEAT_14K, 6, 15, 7, 16)
    };

    public static void main(String[] args) throws Exception {
        for (var chart : CHARTS) {
            var standard = BMSPlayerRule.getBMSPlayerRule(chart.mode());
            check(standard == switch (chart.mode()) {
                case BEAT_5K, BEAT_10K -> BMSPlayerRule.Beatoraja_5;
                case BEAT_14K -> BMSPlayerRule.Beatoraja_7;
                case POPN_9K -> BMSPlayerRule.Beatoraja_9;
                default -> throw new AssertionError(chart.mode());
            }, "mode-specific default");
            compareWindowsAndHolds(chart, LunaticRave2);
            compareWindowsAndHolds(chart, standard);
            compareSharedGauge(chart, LunaticRave2, 28);
            compareSharedGauge(chart, standard, 28);
            compareSharedGauge(chart, LunaticRave2, 27);
            compareSharedGauge(chart, standard, 27);
            if (chart.isDp()) checkDpGaugeRecoveryAndRightMine(chart);
        }
        checkSecondaryModesAndStorage();
        System.out.println("LunaticRave2ModeComparisonTest passed");
    }

    private static void checkSecondaryModesAndStorage() throws Exception {
        var directory = Files.createTempDirectory("lr2-mode-scores");
        try {
            Files.createDirectory(directory.resolve("test"));
            var config = new Config();
            config.setPlayerpath(directory.toString());
            config.setPlayername("test");
            var data = new PlayDataAccessor(config);
            for (var chart : CHARTS) {
                var rule = BMSPlayerRule.getBMSPlayerRule(chart.mode());
                for (boolean queued : new boolean[]{false, true}) {
                    var primary = fixture(chart, rule, NORMAL, queued);
                    var baseline = fixture(chart, rule, NORMAL, queued);
                    var standalone = fixture(chart, LunaticRave2, NORMAL, queued);
                    long end = 6_500_000;
                    var secondary = Objects.requireNonNull(SecondaryJudging.create(
                            primary.judge.main, primary.model, primary.resource, end), chart + " secondary eligible");
                    var group = SecondaryJudgingTest.group(secondary);
                    for (var f : List.of(primary, baseline, standalone)) {
                        boolean shadow = f == primary;
                        f.input.setKeyState(chart.left(), true, 1_982_000);
                        f.judge.update(1_982_000, shadow ? group : null);
                        f.input.setKeyState(chart.right(), true, 2_018_000);
                        f.judge.update(2_018_000, shadow ? group : null);
                        f.input.setKeyState(chart.left(), false, 2_100_000);
                        f.input.setKeyState(chart.right(), false, 2_100_000);
                        f.input.setKeyState(chart.leftHold(), true, 4_000_000);
                        if (chart.isDp()) f.input.setKeyState(chart.rightHold(), true, 4_000_000);
                        f.judge.update(5_001_000, shadow ? group : null);
                        f.input.setKeyState(chart.leftHold(), false, 5_200_000);
                        f.judge.update(6_001_000, shadow ? group : null);
                        f.judge.update(end, shadow ? group : null);
                    }
                    var result = secondary.finish();
                    check(result.outcome() == SecondaryJudgingGroup.Outcome.FINISHED
                                    && result.passed() == standalone.judge.getPastNotes()
                                    && result.gaugeValue() == standalone.gauge.getValue()
                                    && result.maxCombo() == standalone.judge.getScoreData().getCombo()
                                    && result.gaugeHistory().timesUs().peek() == end,
                            chart + " secondary timing, shared gauge and combo match standalone");
                    for (int i = 0; i < 12; i++) {
                        check(result.judgeCounts().get(i) == standalone.judge.getJudgeCount(i / 2, i % 2 == 0)
                                        && primary.judge.getJudgeCount(i / 2, i % 2 == 0)
                                                == baseline.judge.getJudgeCount(i / 2, i % 2 == 0),
                                chart + " judgment parity with queued=" + queued);
                    }
                    check(primary.audioEvents.equals(baseline.audioEvents)
                                    && primary.gauge.getValue() == baseline.gauge.getValue(),
                            chart + " secondary is silent and does not affect primary gauge");
                    if (queued) {
                        var attempt = result.toLocalAttempt(primary.model);
                        check(attempt.toScoreData(primary.model.getMode()).getPlaymode() == chart.mode(),
                                chart + " correct display mode");
                        var primaryScore = primary.judge.getScoreData();
                        primaryScore.setClear(ClearType.Normal.id);
                        data.writeScoreData(primaryScore, primary.model, 0, true);
                        data.writeLunaticRave2Attempt(attempt, true);
                        var reopened = new PlayDataAccessor(config);
                        var lr2Best = reopened.readLunaticRave2Best(attempt.sha256(), attempt.mode());
                        check(lr2Best != null && lr2Best.getRule() == LunaticRave2 && lr2Best.getExscore() == result.exScore()
                                        && lr2Best.getPlaycount() == 1
                                        && reopened.readScoreData(primary.model, 0).getExscore() == primaryScore.getExscore(),
                                chart + " separate bests survive reopening");
                    }
                }
            }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void compareWindowsAndHolds(Chart chart, BMSPlayerRule rule) throws Exception {
        var fixture = fixture(chart, rule, HARD);
        boolean pg = rule == LunaticRave2 || chart.mode() == Mode.POPN_9K;
        var judge = fixture.judge;
        var input = fixture.input;
        input.setKeyState(chart.left(), true, 1_982_000);
        judge.update(1_982_000);
        check(judge.getPastNotes() == 1 && judge.getJudgeCount(pg ? 0 : 1) == 1, chart + " left boundary");
        input.setKeyState(chart.right(), true, 2_018_000);
        judge.update(2_018_000);
        check(judge.getPastNotes() == 2 && judge.getJudgeCount(pg ? 0 : 1) == 2, chart + " right boundary");
        input.setKeyState(chart.left(), false, 2_100_000);
        input.setKeyState(chart.right(), false, 2_100_000);
        input.setKeyState(chart.leftHold(), true, 4_000_000);
        if (chart.isDp()) input.setKeyState(chart.rightHold() + 1, true, 4_000_000);
        judge.update(4_000_000);
        int[] lanes = new LaneProperty(chart.mode()).getKeyLaneAssign();
        int leftHoldLane = lanes[chart.leftHold()];
        int rightHoldLane = lanes[chart.rightHold()];
        check(judge.getProcessingLongNote(leftHoldLane) != null, chart + " left hold");
        if (chart.isDp()) check(judge.getProcessingLongNote(rightHoldLane) != null, chart + " right hold");
        if (chart.hasScratch()) {
            input.setKeyState(chart.leftHold() + 1, true, 4_400_000);
            input.setKeyState(chart.leftHold(), false, 4_400_000);
            judge.update(4_400_000);
            check((judge.getProcessingLongNote(leftHoldLane) != null) == (rule == LunaticRave2), chart + " scratch handoff " + rule);
            if (chart.isDp()) check(judge.getProcessingLongNote(rightHoldLane) != null, chart + " independent right scratch");
        }
        judge.update(5_001_000);
        int holds = chart.isDp() ? 2 : 1;
        int bad = chart.hasScratch() && rule != LunaticRave2 ? 1 : 0;
        check(judge.getPastNotes() == 2 + holds && judge.getJudgeCount(3) == bad
                && judge.getJudgeCount(0) == (pg ? 2 : 0) + holds - bad, chart + " hold results " + rule);
        System.out.printf("%s %s: PG=%d GR=%d BAD=%d%n", chart.file(), rule,
                judge.getJudgeCount(0), judge.getJudgeCount(1), judge.getJudgeCount(3));
    }

    private static void compareSharedGauge(Chart chart, BMSPlayerRule rule, int hp) throws Exception {
        var fixture = fixture(chart, rule, HARD);
        int leftMineKey = chart.left();
        int rightMineKey = chart.rightHold();
        fixture.gauge.setValue(HARD, hp);
        // Hit the first measure and complete the holds without leaving a pending miss.
        fixture.input.setKeyState(chart.left(), true, 2_000_000);
        fixture.input.setKeyState(chart.right(), true, 2_000_000);
        fixture.input.setKeyState(chart.left(), false, 2_100_000);
        fixture.input.setKeyState(chart.right(), false, 2_100_000);
        fixture.input.setKeyState(chart.leftHold(), true, 4_000_000);
        if (chart.isDp()) fixture.input.setKeyState(rightMineKey, true, 4_000_000);
        fixture.judge.update(5_001_000);
        fixture.input.setKeyState(chart.leftHold(), false, 5_200_000);
        if (chart.isDp()) fixture.input.setKeyState(rightMineKey, false, 5_200_000);
        fixture.judge.update(5_200_000);
        fixture.gauge.setValue(HARD, hp);
        fixture.input.setKeyState(leftMineKey, true, 5_950_000);
        fixture.input.setKeyState(rightMineKey, true, 5_950_000);
        fixture.judge.update(6_000_000);
        check(fixture.gauge.getValue() == (rule == LunaticRave2 ? hp : hp - 26), chart + " mine crossing " + rule);
        fixture.judge.update(6_001_000);
        check(fixture.gauge.getValue() == hp - 26, chart + " both mines affect the same gauge");
        check(fixture.gauge.isFailed() == (rule == LunaticRave2 && hp == 27), chart + " shared HARD death threshold");
        check(fixture.judge.getJudgeCount(5) == (rule == LunaticRave2 ? 2 : 0), chart + " mine counter");
        check(fixture.judge.getPastNotes() == (chart.isDp() ? 4 : 3), chart + " mines do not count as chart notes");
        System.out.printf("%s %s: HARD %d -> %.0f, failed=%s%n", chart.file(), rule, hp,
                fixture.gauge.getValue(), fixture.gauge.isFailed());
    }

    private static void checkDpGaugeRecoveryAndRightMine(Chart chart) throws Exception {
        var fixture = fixture(chart, LunaticRave2, NORMAL);
        check(fixture.judge.getScoreData().getNotes() == 6, chart + " both sides count toward TOTAL");
        fixture.input.setKeyState(chart.left(), true, 2_000_000);
        fixture.judge.update(2_000_000);
        check(fixture.gauge.getValue() == 40, chart + " left PG recovers 120/6");
        fixture.input.setKeyState(chart.right(), true, 2_001_000);
        fixture.judge.update(2_001_000);
        check(fixture.gauge.getValue() == 60, chart + " right PG recovers another 120/6");
        fixture.input.setKeyState(chart.left(), false, 2_100_000);
        fixture.input.setKeyState(chart.leftHold(), true, 4_000_000);
        fixture.input.setKeyState(chart.rightHold(), true, 4_000_000);
        fixture.judge.update(5_001_000);
        check(fixture.gauge.getValue() == 100, chart + " both long scratches recover the shared gauge");
        fixture.input.setKeyState(chart.leftHold(), false, 5_200_000);
        fixture.judge.update(6_001_000);
        check(fixture.gauge.getValue() == 84 && fixture.judge.getJudgeCount(5) == 1,
                chart + " right held scratch alone triggers the E6 mine");
        check(fixture.judge.getCombo() == 4 && fixture.judge.getPastNotes() == 4,
                chart + " both sides share combo and mines do not consume notes");
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(Chart chart, BMSPlayerRule rule, int gauge) throws Exception {
        return fixture(chart, rule, gauge, true);
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(Chart chart, BMSPlayerRule rule, int gauge, boolean queued) throws Exception {
        try (var stream = Objects.requireNonNull(LunaticRave2ModeComparisonTest.class
                .getResourceAsStream("/play/lunatic-rave2/" + chart.file()))) {
            var model = new BMSDecoder().decode(stream.readAllBytes(), chart.file(), chart.file().endsWith(".pms"), new int[0]);
            check(model != null && model.getMode() == chart.mode(), chart + " decoded mode");
            var lanes = new LaneProperty(chart.mode()).getKeyLaneAssign();
            var notes = model.getLanes();
            check(notes[lanes[chart.left()]].getNotes()[0].getMicroTime() == 2_000_000
                    && notes[lanes[chart.right()]].getNotes()[0].getMicroTime() == 2_000_000, chart + " channel mapping");
            check(Arrays.stream(notes[lanes[chart.leftHold()]].getNotes()).anyMatch(n -> n instanceof LongNote)
                    && Arrays.stream(notes[lanes[chart.rightHold()]].getNotes()).anyMatch(n -> n instanceof MineNote),
                    chart + " hold and mine mapping");
            var parameters = BMSPlayerRule.ChartParameters.fromBms(model);
            check(BMSPlayerRule.select(rule, model, chart.mode(), BMSPlayerMode.PLAY, false, gauge, parameters) == rule,
                    chart + " eligible chart");
            check(BMSPlayerRule.select(BMSPlayerRule.SINGLE_SONG_RULE, model, chart.mode(), BMSPlayerMode.PLAY,
                    false, gauge, parameters) == (BMSPlayerRule.SINGLE_SONG_RULE == LunaticRave2
                    ? LunaticRave2 : BMSPlayerRule.getBMSPlayerRule(chart.mode())), chart + " source switch");
            BMSPlayerRule.validate(model);
            return new KeyboardJudgeCompatibilityTest.Fixture(model, queued, rule, parameters, gauge);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
