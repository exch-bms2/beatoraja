package bms.player.beatoraja.play;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import bms.model.*;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;
import bms.player.beatoraja.play.KeyboardJudgeCompatibilityTest.Fixture;

import static bms.player.beatoraja.PlayerConfig.*;
import static bms.player.beatoraja.play.AbstractGrooveGauge.*;
import static bms.player.beatoraja.play.BMSPlayerRule.*;
import static bms.player.beatoraja.play.SecondaryJudging.Outcome.*;

public final class SecondaryJudgingTest {
    private static final long END = 6_500_000;
    private static final int KEY = 0;

    public static void main(String[] args) throws Exception {
        compare(true, GAUGEAUTOSHIFT_NONE);
        compare(false, GAUGEAUTOSHIFT_NONE); // Legacy input, with no queued keys.
        compare(true, GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE);
        compare(true, GAUGEAUTOSHIFT_CONTINUE);
        stopWhenPrimaryCloses();
        checkExitDuringSongTail();
        checkFullComboBelowClearBorder();
        checkStartGaugeMapping();
        checkAutoShiftOrder();
        System.out.println("SecondaryJudgingTest passed (primary parity, LR2 parity, FIFO/legacy, LN, mine, closure and auto shift)");
    }

    private static void checkExitDuringSongTail() throws Exception {
        for (boolean primaryFinished : new boolean[]{true, false}) {
            var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_NONE, 100);
            var secondary = makeSecondary(primary);
            playThroughFinalNote(primary, secondary);
            check(secondary.isRunning(), "post-song margin has not elapsed");
            var result = secondary.finish(primaryFinished);
            check(result.passed() == result.notes() && result.maxCombo() == result.notes(), "all notes form a full combo");
            check(result.outcome() == (primaryFinished ? FINISHED : INTERRUPTED)
                            && result.clear() == (primaryFinished ? ClearType.FullCombo : ClearType.Failed)
                            && result.toLocalAttempt(primary.model).outcome()
                                    == (primaryFinished ? ScoreOutcome.FINISHED : ScoreOutcome.INTERRUPTED),
                    "skipping only the tail of a completed play preserves the LR2 lamp and record");
            checkGaugeHistory(result, 1_100_000);
        }

        var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_NONE, 100);
        var secondary = makeSecondary(primary);
        drive(primary, secondary, new Tick(200_000, true)); // The LN end is not yet judged.
        check(secondary.finish(true).outcome() == INTERRUPTED, "primary completion cannot promote pending LR2 notes");
    }

    private static void checkFullComboBelowClearBorder() throws Exception {
        for (var expected : new SecondaryJudging.Outcome[]{FINISHED, INTERRUPTED, FAILED}) {
            var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_NONE, 100);
            var secondary = makeSecondary(primary);
            playThroughFinalNote(primary, secondary);
            secondary.gauge().setType(expected == FAILED ? HARD : NORMAL);
            secondary.gauge().setValue(expected == FAILED ? HARD : NORMAL, expected == FAILED ? 1 : 79);
            if (expected != INTERRUPTED) drive(primary, secondary, new Tick(expected == FAILED ? 1_200_000 : END, null));
            var result = secondary.finish(expected != INTERRUPTED);
            check(result.passed() == result.notes() && result.maxCombo() == result.notes()
                            && result.outcome() == expected
                            && result.clear() == (expected == FINISHED ? ClearType.FullCombo : ClearType.Failed),
                    "only a completed LR2 full combo overrides a below-border gauge");
            check(primary.judge.getCompletedComboClear() == null, "beatoraja has no LR2 clear override");
        }

        var standalone = fixture(LunaticRave2, true, GAUGEAUTOSHIFT_NONE, 100);
        playThroughFinalNote(standalone, null);
        standalone.gauge.setType(NORMAL);
        standalone.gauge.setValue(NORMAL, 79);
        check(standalone.judge.getCompletedComboClear() == ClearType.FullCombo,
                "standalone LR2 uses the same full-combo override");

        var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_NONE, 100);
        var secondary = makeSecondary(primary);
        drive(primary, secondary, new Tick(250_000, true));
        drive(primary, secondary, new Tick(401_000, false));
        secondary.gauge().setType(NORMAL);
        secondary.gauge().setValue(NORMAL, 79);
        drive(primary, secondary, new Tick(END, null));
        var result = secondary.finish();
        check(result.outcome() == FINISHED && result.maxCombo() < result.notes() && result.clear() == ClearType.Failed,
                "a completed run without a full combo still needs to meet the clear border");
    }

    private static void playThroughFinalNote(Fixture primary, SecondaryJudging secondary) {
        for (var tick : List.of(new Tick(250_000, true), new Tick(401_000, false),
                new Tick(600_000, true), new Tick(700_000, false), new Tick(1_100_000, true))) {
            drive(primary, secondary, tick);
        }
    }

    private static void checkStartGaugeMapping() throws Exception {
        for (int[] pair : new int[][]{{EXHARD, HARD}, {ASSISTEASY, EASY}, {HAZARD, HAZARD}}) {
            for (int option : new int[]{GAUGEAUTOSHIFT_NONE, GAUGEAUTOSHIFT_SELECT_TO_UNDER}) {
                var primary = fixture(Beatoraja_7, true, option, 100);
                primary.playerConfig.setGauge(pair[0]);
                primary.gauge.setType(pair[0]);
                var secondary = makeSecondary(primary);
                check(secondary.gauge().getType() == pair[1], "mapped LR2 start gauge");
                drive(primary, secondary, new Tick(200_000, true));
                drive(primary, secondary, new Tick(401_000, false));
                var result = secondary.finish();
                check(result.gaugeType() == pair[1] && result.passed() == 1
                                && primary.gauge.getType() == pair[0] && primary.playerConfig.getGauge() == pair[0],
                        "mapped start and auto shift ceiling apply only to LR2");
            }
        }
    }

    private static void checkAutoShiftOrder() throws Exception {
        var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_BESTCLEAR, 100);
        var gauge = makeSecondary(primary).gauge();
        gauge.setValue(NORMAL, 90);
        gauge.setValue(EASY, 90);
        checkShift(primary, gauge, HAZARD);
        gauge.setValue(HAZARD, 0);
        checkShift(primary, gauge, HARD); // EXHARD is still at 100.
        gauge.setValue(HARD, 1);
        checkShift(primary, gauge, NORMAL);
        gauge.setValue(NORMAL, 20);
        checkShift(primary, gauge, EASY);
        gauge.setValue(EASY, 20);
        gauge.setValue(ASSISTEASY, 100);
        checkShift(primary, gauge, EASY); // Do not fall back to ASSIST EASY.

        primary.gauge.setValue(HAZARD, 0);
        checkShift(primary, primary.gauge, EXHARD);
    }

    private static void checkShift(Fixture primary, AbstractGrooveGauge gauge, int expected) {
        check(!BMSPlayer.updateGaugeAutoShift(gauge, primary.playerConfig, gauge.isFailed())
                        && gauge.getType() == expected, "auto shift should select gauge type " + expected);
    }

    private static void compare(boolean queued, int autoShift) throws Exception {
        var baseline = fixture(Beatoraja_7, queued, autoShift, 11);
        var primary = fixture(Beatoraja_7, queued, autoShift, 11);
        var lr2 = fixture(LunaticRave2, queued, autoShift, 11);
        // A held mine takes HARD below 2%: alive in beatoraja, dead in LR2.
        var secondary = makeSecondary(primary, 11);
        boolean lr2Running = true;
        for (var tick : ticks()) {
            drive(baseline, null, tick);
            drive(primary, secondary, tick);
            check(snapshot(baseline).equals(snapshot(primary))
                            && Arrays.equals(baseline.judge.getGhost(), primary.judge.getGhost())
                            && baseline.audioEvents.equals(primary.audioEvents),
                    "primary differs at " + tick.time);
            check(baseline.judge.main.getState() == primary.judge.main.getState(), "secondary altered primary state");
            if (lr2Running) {
                drive(lr2, null, tick);
                lr2Running = !BMSPlayer.updateGaugeAutoShift(lr2.gauge, lr2.playerConfig, lr2.gauge.isFailed());
            }
        }
        var result = secondary.finish();
        check(result.outcome() == (autoShift == GAUGEAUTOSHIFT_NONE ? FAILED : FINISHED),
                "secondary outcome for auto shift mode " + autoShift + ": " + result);
        check(snapshot(lr2).equals(new Score(result.passed(), result.exScore(), result.maxCombo(),
                        result.judgeCounts(), result.gaugeType(), result.gaugeValue())),
                "LR2 standalone differs for auto shift mode " + autoShift + ": " + result);
        check(primary.judge.getPastNotes() == 3, "primary continued through the later note");
        check(result.passed() == (autoShift == GAUGEAUTOSHIFT_NONE ? 2 : 3), "secondary scoring cutoff");
        check(result.judgeCounts().get(10) == 1, "LR2 mine counts as POOR once");
        check(result.gaugeType() == (autoShift == GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE ? NORMAL : HARD),
                "independent secondary gauge selection");
        checkGaugeHistory(result, autoShift == GAUGEAUTOSHIFT_NONE ? 801_000 : END);
        var local = result.toLocalAttempt(primary.model);
        check(local.exscore() == result.exScore() && local.mode() == 0 && local.notes() == 3
                        && local.judgeCounts().get(10) == 1,
                "the local record uses the secondary judgments and played chart identity");
    }

    private static void stopWhenPrimaryCloses() throws Exception {
        var primary = fixture(Beatoraja_7, true, GAUGEAUTOSHIFT_NONE, 9);
        var secondary = makeSecondary(primary);
        for (var tick : ticks()) {
            drive(primary, secondary, tick);
            if (primary.gauge.isFailed()) break;
        }
        var result = secondary.finish();
        check(primary.gauge.isFailed() && result.outcome() == INTERRUPTED
                && result.gaugeValue() > 2 && result.passed() == 2
                && result.toLocalAttempt(primary.model).outcome().name().equals("INTERRUPTED"),
                "primary-only closure interrupts the secondary");
        checkGaugeHistory(result, 801_000);
    }

    private static void checkGaugeHistory(SecondaryJudging.Result result, long cutoff) {
        var history = result.gaugeHistory();
        check(history.durationUs() == END && history.timesUs().first() == 0 && history.timesUs().peek() == cutoff,
                "history preserves the full time axis and stops at the exact judgment cutoff");
        for (int type = 0; type < history.values().length; type++) {
            check(history.values()[type].size == history.timesUs().size
                            && history.values()[type].peek() == history.gauge().getValue(type),
                    "each gauge type includes the final sample, without a fabricated tail: " + type);
        }
    }

    private static SecondaryJudging makeSecondary(Fixture f) {
        return Objects.requireNonNull(SecondaryJudging.create(f.judge.main, f.model, f.resource, END));
    }

    private static SecondaryJudging makeSecondary(Fixture f, float hp) {
        var secondary = makeSecondary(f);
        secondary.gauge().setValue(HARD, hp);
        return secondary;
    }

    private static Fixture fixture(BMSPlayerRule rule, boolean queued, int autoShift, float hp) throws Exception {
        var model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        model.setSHA256("1".repeat(64));
        model.setTotal(120);
        var start = new LongNote(0);
        start.setType(LongNote.TYPE_LONGNOTE);
        var end = new LongNote(0);
        model.setAllTimeLine(new TimeLine[]{
                line(0.1, 200_000, start), line(0.2, 400_000, end),
                line(0.3, 600_000, new NormalNote(0)), line(0.4, 800_000, new MineNote(0, 10)),
                line(0.5, 1_100_000, new NormalNote(0))
        });
        start.setPair(end);
        var f = new Fixture(model, queued, rule, new BMSPlayerRule.ChartParameters(2, 120), HARD);
        f.playerConfig.setGaugeAutoShift(autoShift);
        f.playerConfig.setGauge(HARD);
        f.gauge.setValue(HARD, hp);
        return f;
    }

    private static TimeLine line(double section, long time, Note note) {
        var line = new TimeLine(section, time, Mode.BEAT_7K.key);
        line.setNote(0, note);
        return line;
    }

    private record Tick(long time, Boolean press) {}

    private static List<Tick> ticks() {
        return List.of(new Tick(200_000, true), new Tick(401_000, false), new Tick(600_000, true),
                new Tick(801_000, null), new Tick(900_000, false), new Tick(1_100_000, true), new Tick(END, null));
    }

    private static void drive(Fixture f, SecondaryJudging secondary, Tick tick) {
        if (tick.press != null) f.input.setKeyState(KEY, tick.press, tick.time);
        f.judge.update(tick.time, secondary);
    }

    private record Score(int passed, int exScore, int maxCombo, List<Integer> counts, int type, float value) {}

    private static Score snapshot(Fixture f) {
        var counts = new ArrayList<Integer>(12);
        for (int i = 0; i < 6; i++) {
            counts.add(f.judge.getJudgeCount(i, true));
            counts.add(f.judge.getJudgeCount(i, false));
        }
        return new Score(f.judge.getPastNotes(), f.judge.getScoreData().getExscore(), f.judge.getScoreData().getCombo(),
                counts, f.gauge.getType(), f.gauge.getValue());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
