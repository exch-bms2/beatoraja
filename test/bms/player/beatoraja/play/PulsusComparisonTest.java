package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.play.KeyboardJudgeCompatibilityTest.Fixture;
import bms.player.beatoraja.play.SecondaryJudgingGroup.Judgment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static bms.player.beatoraja.play.AbstractGrooveGauge.*;
import static bms.player.beatoraja.play.SecondaryJudgingGroup.Outcome.*;

/**
 * Fixed 7KEY inputs against the actual secondary run and source-derived Pulsus expectations.
 * Reference: GoaLitiuM/Pulsus@da7ad4409de8836a405afbe7200dd1cfc2920900,
 * Gameplay/BMS/BMSJudge.cs and Gameplay/EventPlayers/Judge.cs under Pulsus/.
 * Uses Pulsus's default LR2 timing tables without optional millisecond truncation.
 */
public final class PulsusComparisonTest {
    private static final int LANE = 6; // BMS channel 19 also makes the decoder detect 7KEY.
    private static final long START = 2_000_000;
    private static final long TAIL = 3_000_000;
    private static final long END = 5_000_000;
    private static final String TAP = "tap.bms";
    private static final String DENSE = "dense.bms";
    private static final String SPACED = "spaced.bms";
    private static final String LN = "long-note.bms";
    private static final String SHORT_LN = "mixed-short-ln.bms";
    private static final List<String> MISMATCHES = new ArrayList<>();

    private record Tick(long us, Boolean down) {}

    public static void main(String[] args) throws Exception {
        compareDense();
        compareRanks();
        compareMisses();
        compareLongNotes();
        compareGauges();
        if (!MISMATCHES.isEmpty()) throw new AssertionError(String.join(System.lineSeparator(), MISMATCHES));
        System.out.println("PulsusComparisonTest passed");
    }

    private static void compareDense() throws Exception {
        // 55 ms late for note 1 (GOOD), 7.5 ms early for note 2 (PG).
        // The second input can still hit note 1 (56 ms late) if the first input took note 2.
        var inputs = new Tick[]{press(2_055_000), release(2_055_500), press(2_056_000)};
        expectScore("dense / Combo, first press", "PG (note 2)", "PG/1/0/2/1",
                play(DENSE, 2, NORMAL, JudgeAlgorithm.Combo, inputs[0]));
        expectScore("dense / Duration, first press", "PG (note 2)", "PG/1/0/2/1",
                play(DENSE, 2, NORMAL, JudgeAlgorithm.Duration, inputs[0]));
        expectScore("dense / Combo, second press", "GD (note 1); 2 EX", "PG+GD/2/0/2/2",
                play(DENSE, 2, NORMAL, JudgeAlgorithm.Combo, inputs));
        // At the midpoint prefer the earlier candidate, then let the second note time out.
        var tie = play(DENSE, 2, NORMAL, JudgeAlgorithm.Duration, press(2_031_250), idle(2_263_000));
        expectScore("dense / midpoint tie", "GR (note 1) + MISS (note 2)", "GR+MISS/2/0/1/1", tie);
        check(tie.judgeCounts().get(3) == 1, "The equidistant GREAT must be late on the earlier note");
    }

    private static void compareRanks() throws Exception {
        // Bracket the boundary to avoid depending on binary floating-point rounding at exactly N ms.
        int[] pgUs = {8_000, 15_000, 18_000, 21_000};
        for (int rank = 0; rank < pgUs.length; rank++) {
            for (int side : new int[]{-1, 1}) {
                for (int extra : new int[]{-1, 1}) {
                    int offset = side * (pgUs[rank] + extra);
                    String reference = extra < 0 ? "PG" : "GR";
                    String expected = extra < 0 ? "PG/1/0/2/1" : "GR/1/0/1/1";
                    expectScore("RANK " + rank + " / offset " + offset + " us", reference, expected,
                            play(TAP, rank, NORMAL, JudgeAlgorithm.Combo, press(START + offset)));
                }
            }
        }
        expectScore("RANK 0 / 25 ms early", "GD", "GD/1/0/0/1",
                play(TAP, 0, NORMAL, JudgeAlgorithm.Combo, press(START - 25_000)));
        expectScore("RANK 3 / 110 ms late", "GD", "GD/1/0/0/1",
                play(TAP, 3, NORMAL, JudgeAlgorithm.Combo, press(START + 110_000)));
        for (int rank : new int[]{-1, 4}) {
            expectScore("missing or unsupported RANK " + rank, "RANK 2 fallback; PG", "PG/1/0/2/1",
                    play(TAP, rank, NORMAL, JudgeAlgorithm.Combo, press(START + 17_999)));
        }
    }

    private static void compareMisses() throws Exception {
        expectScore("early POOR", "early POOR; pending", "-/0/1/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, press(START - 201_000)));
        expectScore("early POOR followed by PG", "early POOR + PG; 2 EX", "PG/1/1/2/1",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo,
                        press(START - 201_000), release(START - 200_500), press(START)));
        expectScore("two early POORs leave the next note and combo intact", "PG x2; early POOR x2", "PG+PG/2/2/4/2",
                play(SPACED, 2, NORMAL, JudgeAlgorithm.Combo, press(START), release(START + 1_000),
                        press(TAIL - 400_000), release(TAIL - 399_000),
                        press(TAIL - 300_000), release(TAIL - 299_000), press(TAIL)));
        expectScore("outer early boundary", "no judge", "-/0/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, press(START - 1_000_000)));
        expectScore("just inside outer early boundary", "early POOR", "-/0/1/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, press(START - 999_999)));
        expectScore("idle just before late boundary", "pending", "-/0/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, idle(START + 199_999)));
        expectScore("idle at late boundary", "pending", "-/0/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, idle(START + 200_000)));
        expectScore("hit just inside late boundary", "BAD; consumed", "BAD/1/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, press(START + 199_999)));
        expectScore("idle beyond late boundary", "MISS; consumed", "MISS/1/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, idle(START + 201_000)));
        expectScore("press after PG", "no candidate", "PG/1/0/2/1",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo,
                        press(START), release(START + 1_000), press(START + 40_000)));
        expectScore("press after MISS", "no candidate", "MISS/1/0/0/0",
                play(TAP, 2, NORMAL, JudgeAlgorithm.Combo, idle(START + 201_000), press(START + 202_000)));
        expectScore("expired tap before still-reachable tap", "MISS (note 1) + BAD (note 2)", "BAD+MISS/2/0/0/0",
                play(DENSE, 2, NORMAL, JudgeAlgorithm.Combo, press(START + 200_001)));
    }

    private static void compareLongNotes() throws Exception {
        expectScore("LN head", "PG; 2 EX; tail pending", "PG/1/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START)));
        expectScore("LN independent rank windows", "GR head + PG tail; 3 EX", "PG+GR/2/0/3/2",
                play(LN, 0, NORMAL, JudgeAlgorithm.Combo, press(START - 10_000), release(TAIL)));
        expectScore("LN early release inside BAD", "PG + BAD; 2 EX", "PG+BAD/2/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), release(TAIL - 199_999)));
        expectScore("LN late release within GREAT", "PG + GR; 3 EX", "PG+GR/2/0/3/2",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), release(TAIL + 30_000)));
        var held = play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), release(TAIL));
        expectScore("LN head + tail", "PG x2; 4 EX; 2 scoring units", "PG+PG/2/0/4/2", held);
        check(held.notes() == 2 && held.exScore() == held.notes() * 2, "LN max EX is four");
        expectScore("LN early release", "PG + tail MISS; 2 EX", "PG+MISS/2/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), release(TAIL - 250_000)));
        expectScore("LN release more than 1 s before the tail", "PG + tail MISS; 2 EX", "PG+MISS/2/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START - 10_000), release(START - 5_000)));
        expectScore("LN failed tail is not judged again", "no additional judgments", "PG+MISS/2/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo,
                        press(START), release(TAIL - 250_000), press(TAIL - 100_000), release(TAIL), idle(TAIL + 201_000)));
        expectScore("held at tail deadline", "PG; tail still pending", "PG/1/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), idle(TAIL + 200_000)));
        var pending = new Run(LN, 2, NORMAL, JudgeAlgorithm.Combo);
        pending.tick(press(START));
        pending.tick(idle(TAIL + 1));
        check(pending.secondary.finish(true).outcome() == INTERRUPTED,
                "Primary completion cannot mark a pending Pulsus tail as finished");
        expectScore("held past tail deadline", "PG + tail MISS; 2 EX", "PG+MISS/2/0/2/1",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START), idle(TAIL + 200_001), release(TAIL + 201_000)));
        expectScore("LN missed head", "head MISS + immediate tail MISS", "MISS+MISS/2/0/0/0",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, idle(START + 200_001)));
        expectScore("LN release before the head is judged", "early POOR only", "-/0/1/0/0",
                play(LN, 2, NORMAL, JudgeAlgorithm.Combo, press(START - 300_000), release(START - 299_000)));

        expectScore("short LN missed head grades tail at the same time", "tap PG + head MISS + tail BAD", "PG+BAD+MISS/3/0/2/1",
                play(SHORT_LN, 2, NORMAL, JudgeAlgorithm.Combo,
                        press(START), release(START + 1_000), idle(2_262_501)));
        var mixed = play(SHORT_LN, 2, NORMAL, JudgeAlgorithm.Combo,
                press(2_055_000), release(2_095_000), press(2_096_000));
        expectScore("tap and LN head compete for the same press", "head PG + tail GR + tap GD", "PG+GR+GD/3/0/3/3", mixed);
        check(mixed.notes() == 3 && mixed.notes() * 2 == 6, "One tap plus an LN gives six max EX");

        var gaugeRun = new Run(SHORT_LN, 2, NORMAL, JudgeAlgorithm.Combo);
        check(gaugeRun.primary.model.getTotalNotes() == 2, "The original chart still counts the LN once");
        gaugeRun.tick(press(2_062_500));
        check(close(gaugeRun.secondary.gauge().getValue(), 70), "Head PG uses the original two-note gauge denominator");
        gaugeRun.tick(release(2_125_000));
        check(close(gaugeRun.secondary.gauge().getValue(), 100), "Tail PG uses the same gauge formula");
    }

    private static void compareGauges() throws Exception {
        for (int gauge : new int[]{NORMAL, EASY, HARD}) {
            var run = new Run(TAP, 2, gauge, JudgeAlgorithm.Combo);
            check(close(run.secondary.gauge().getValue(), gauge == HARD ? 100 : 20), "Initial gauge for " + gauge);
        }
        gauge("Default / BAD", NORMAL, 20, 18, press(START - 150_000));
        gauge("Easy / BAD", EASY, 20, 18.4, press(START - 150_000));
        gauge("Default / early POOR", NORMAL, 20, 18, press(START - 201_000));
        gauge("Easy / early POOR", EASY, 20, 18.4, press(START - 201_000));
        gauge("Default / MISS", NORMAL, 20, 14, idle(START + 201_000));
        gauge("Easy / MISS", EASY, 20, 15.2, idle(START + 201_000));
        gauge("Default / floor", NORMAL, 3, 2, idle(START + 201_000));
        gauge("Easy / floor", EASY, 3, 2, idle(START + 201_000));
        gauge("Default / GOOD recovery", NORMAL, 20, 70, press(START - 60_000));
        gauge("Easy / PG capped at 100%", EASY, 20, 100, press(START));
        gauge("Hard / PG", HARD, 90, 90.16, press(START));
        gauge("Hard / GREAT", HARD, 90, 90.16, press(START - 30_000));
        gauge("Hard / GOOD", HARD, 90, 90, press(START - 60_000));
        gauge("Hard / early POOR", HARD, 10, 5, press(START - 201_000));
        gauge("Hard / BAD at 2%", HARD, 7, 2, press(START - 150_000));
        gauge("Hard / BAD below 2%", HARD, 6.99f, 1.99, press(START - 150_000));
        gauge("Hard / MISS below 2%", HARD, 10.99f, 1.99, idle(START + 201_000));
        gauge("Hard / no closure at zero", HARD, 3, 0, idle(START + 201_000));

        var noTotal = new Run(SPACED, 2, NORMAL, JudgeAlgorithm.Combo, false);
        noTotal.tick(press(START - 60_000));
        check(close(noTotal.secondary.gauge().getValue(), 85), "Default TOTAL 260, two notes, half recovery for GOOD");

        for (int type : new int[]{NORMAL, EASY, HARD}) {
            double earlyPenalty = switch (type) { case NORMAL -> 2; case EASY -> 1.6; default -> 5; };
            double missedPenalty = switch (type) { case NORMAL -> 6; case EASY -> 4.8; default -> 9; };
            for (long early : new long[]{250_000, 1_005_000}) {
                var run = new Run(LN, 2, type, JudgeAlgorithm.Combo);
                run.tick(press(START - 10_000));
                double before = run.secondary.gauge().getValue();
                run.tick(release(TAIL - early));
                var result = run.secondary.finish();
                check(close(result.gaugeValue(), before - (early < 1_000_000 ? earlyPenalty : missedPenalty))
                                && result.judgeCounts().get(8) == 1 && result.passed() == 2,
                        "Pulsus LN release damage uses the timing without reclassifying the scored MISS");
            }
        }

        for (int type : new int[]{NORMAL, EASY, HARD}) {
            float border = type == HARD ? 2 : 80;
            float damage = type == HARD ? 5 : type == NORMAL ? 2 : 1.6f;
            checkFinalGauge(type, border + damage - 0.01f, null, false);
            // Seed the exact border separately; 81.6f - 1.6f is not exactly 80 in binary floating point.
            checkFinalGauge(type, border + damage + 0.01f, border, true);
        }

        compareHardRecovery(6.9f, 1.9, 2.06, ClearType.Hard);
        compareHardRecovery(3, 0, 0.16, ClearType.Failed);
    }

    private static void checkFinalGauge(int type, float seed, Float exactFinal, boolean clear) throws Exception {
        var run = new Run(TAP, 2, type, JudgeAlgorithm.Combo);
        run.secondary.gauge().setValue(type, seed);
        run.tick(press(START - 150_000));
        if (exactFinal != null) run.secondary.gauge().setValue(type, exactFinal);
        run.tick(idle(END));
        var result = run.secondary.finish(true);
        check(result.outcome() == FINISHED && (result.clear() != ClearType.Failed) == clear,
                "Final border for gauge " + type + ", seed " + seed + ": " + result.gaugeValue());
    }

    private static void compareHardRecovery(float seed, double low, double recovered, ClearType clear) throws Exception {
        var run = new Run(SPACED, 2, HARD, JudgeAlgorithm.Combo);
        run.secondary.gauge().setValue(HARD, seed);
        run.tick(press(START - 150_000));
        check(run.secondary.isRunning() && close(run.secondary.gauge().getValue(), low),
                "Crossing below 2%, including zero, does not stop HARD");
        run.tick(release(START - 149_000));
        run.tick(press(TAIL));
        check(close(run.secondary.gauge().getValue(), recovered), "HARD still recovers after reaching " + low);
        run.tick(idle(END));
        var result = run.secondary.finish(true);
        check(result.outcome() == FINISHED && result.clear() == clear && result.exScore() == 2 && result.passed() == 2,
                "HARD only uses the final value for clear; a prior drop does not discard any judgments");
    }

    private static void gauge(String label, int type, float seed, double reference, Tick tick) throws Exception {
        var run = new Run(TAP, 2, type, JudgeAlgorithm.Combo);
        run.secondary.gauge().setValue(type, seed);
        run.tick(tick);
        var actual = run.secondary.finish(false);
        System.out.printf(Locale.ROOT, "%s: Pulsus=%.2f%%, secondary=%.2f%%%n", label, reference, actual.gaugeValue());
        if (!close(actual.gaugeValue(), reference) || actual.outcome() != INTERRUPTED) {
            MISMATCHES.add(label + ": expected " + reference + "% without closure; got "
                    + actual.gaugeValue() + "%, " + actual.outcome());
        }
    }

    private static Judgment play(String file, int rank, int gauge, JudgeAlgorithm algorithm, Tick... ticks)
            throws Exception {
        var run = new Run(file, rank, gauge, algorithm);
        for (var tick : ticks) run.tick(tick);
        return run.secondary.finish(false);
    }

    // Secondary score: consuming judgments / consumed notes / early POOR / EX / max combo.
    private static void expectScore(String label, String reference, String expected, Judgment actual) {
        var consuming = new StringBuilder();
        int[] counts = new int[6];
        for (int i = 0; i < counts.length; i++) counts[i] = actual.judgeCounts().get(2 * i) + actual.judgeCounts().get(2 * i + 1);
        for (int i = 0; i < counts.length - 1; i++) {
            for (int j = 0; j < counts[i]; j++) {
                if (!consuming.isEmpty()) consuming.append('+');
                consuming.append(List.of("PG", "GR", "GD", "BAD", "MISS").get(i));
            }
        }
        String score = (consuming.isEmpty() ? "-" : consuming) + "/" + actual.passed()
                + "/" + counts[5] + "/" + actual.exScore() + "/" + actual.maxCombo();
        System.out.printf("%s: Pulsus=%s, secondary=%s%n", label, reference, score);
        if (!expected.equals(score)) MISMATCHES.add(label + ": expected secondary " + expected + ", got " + score);
    }

    private static final class Run {
        final Fixture primary;
        final SecondaryJudging secondary;
        final SecondaryJudgingGroup group;

        Run(String file, int rank, int gauge, JudgeAlgorithm algorithm) throws Exception {
            this(file, rank, gauge, algorithm, true);
        }

        Run(String file, int rank, int gauge, JudgeAlgorithm algorithm, boolean hasTotal) throws Exception {
            try (var input = Objects.requireNonNull(PulsusComparisonTest.class
                    .getResourceAsStream("/play/pulsus/" + file), file)) {
                var bms = new String(input.readAllBytes(), StandardCharsets.US_ASCII)
                        .replace("#RANK 2", rank == -1 ? "" : "#RANK " + rank);
                if (!hasTotal) bms = bms.replace("#TOTAL 100", "");
                var model = new BMSDecoder().decode(bms.getBytes(StandardCharsets.US_ASCII), file, false, new int[0]);
                check(model != null && model.getMode() == Mode.BEAT_7K
                        && Objects.equals(model.getJudgeType().bmsRank(), rank == -1 ? null : rank)
                        && (hasTotal ? model.getTotalType() == BMSModel.TotalType.BMS && model.getTotal() == 100
                                : model.getTotalType() != BMSModel.TotalType.BMS),
                        file + ": expected 7KEY with the requested RANK and TOTAL");
                var notes = Arrays.stream(model.getAllTimeLines()).map(line -> line.getNote(LANE)).filter(Objects::nonNull).toList();
                var times = notes.stream().map(Note::getMicroTime).toList();
                check(times.equals(switch (file) {
                    case TAP -> List.of(START);
                    case DENSE -> List.of(START, 2_062_500L);
                    case LN, SPACED -> List.of(START, TAIL);
                    case SHORT_LN -> List.of(START, 2_062_500L, 2_125_000L);
                    default -> throw new AssertionError(file);
                }) && (!file.equals(LN) || notes.get(0) instanceof LongNote head && head.getPair() == notes.get(1)),
                        file + ": unexpected notes " + times);
                if (file.equals(SHORT_LN)) {
                    check(notes.get(1) instanceof LongNote head && head.getPair() == notes.get(2), "Expected a short LN pair");
                }
                var chart = BMSPlayerRule.ChartParameters.fromBms(model);
                BMSPlayerRule.validate(model);
                primary = new Fixture(model, true, BMSPlayerRule.Beatoraja_7, chart, NORMAL);
                primary.playerConfig.setGauge(gauge);
                primary.playerConfig.getPlayConfig(Mode.BEAT_7K).getPlayconfig().setJudgetype(algorithm.name());
                secondary = Objects.requireNonNull(SecondaryJudging.createTimingProfile(
                        primary.player, model, primary.resource, END, SecondaryScoringRule.PULSUS));
                group = new SecondaryJudgingGroup(List.of(new SecondaryJudgingGroup.Entry(SecondaryScoringRule.PULSUS, secondary)));
            }
        }

        void tick(Tick tick) {
            if (tick.down() != null) primary.input.setKeyState(LANE, tick.down(), tick.us());
            primary.judge.update(tick.us(), group);
        }
    }

    private static Tick press(long us) { return new Tick(us, true); }
    private static Tick release(long us) { return new Tick(us, false); }
    private static Tick idle(long us) { return new Tick(us, null); }
    private static boolean close(double actual, double expected) { return Math.abs(actual - expected) < 0.0001; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
