package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.MineNote;
import bms.model.Mode;
import bms.player.beatoraja.BMSPlayerMode;

import java.util.Objects;

import static bms.player.beatoraja.play.BMSPlayerRule.Beatoraja_7;
import static bms.player.beatoraja.play.BMSPlayerRule.LunaticRave2;
import static bms.player.beatoraja.play.AbstractGrooveGauge.*;

/** OpenLR2-derived timing and gauge expectations against both production rules. */
public final class LunaticRave2MineComparisonTest {
    private static final int KEY = 0;
    private static final long FIRST = 2_000_000;
    private static final long SECOND = 3_000_000;

    public static void main(String[] args) throws Exception {
        var lr2 = fixture(LunaticRave2, HARD, 30);
        var standard = fixture(Beatoraja_7, HARD, 30);
        lr2.input.setKeyState(KEY, true, 1_500_000);
        standard.input.setKeyState(KEY, true, 1_500_000);
        checkpoint("before", lr2, standard, FIRST - 1, 30, 30, 0);
        checkpoint("exact crossing", lr2, standard, FIRST, 30, 20, 0);
        checkpoint("first millisecond past crossing", lr2, standard, FIRST + 1_000, 20, 20, 1);
        checkpoint("held over second mine", lr2, standard, SECOND + 1_000, 10, 10, 2);
        checkpoint("no duplicate damage", lr2, standard, SECOND + 2_000, 10, 10, 2);

        for (var rule : new BMSPlayerRule[]{LunaticRave2, Beatoraja_7}) {
            boolean experimental = rule == LunaticRave2;
            assertTap(rule, FIRST - 40_000, FIRST - 39_000, false);
            assertTap(rule, FIRST - 39_000, FIRST - 38_000, experimental);
            assertTap(rule, FIRST - 100_000, FIRST, false);
            assertTap(rule, FIRST - 100_000, FIRST + 1_000, false);
            assertTap(rule, FIRST - 100_000, FIRST + 2_000, experimental);
            var avoided = fixture(rule, HARD, 30);
            avoided.judge.update(FIRST + 2_000);
            avoided.input.setKeyState(KEY, true, FIRST + 3_000);
            avoided.judge.update(FIRST + 3_000);
            expect(avoided, 30, 0, "press after an already avoided mine");
        }

        for (int type : new int[]{NORMAL, EASY, HARD}) {
            for (float initial : new float[]{5, 11, 12}) {
                var gauge = fixture(LunaticRave2, type, initial);
                gauge.input.setKeyState(KEY, true, FIRST);
                gauge.judge.update(FIRST);
                float expected = Math.max(type == HARD ? 0 : 2, initial - 10);
                expect(gauge, expected, 1, "mine clamp");
                check(gauge.gauge.isFailed() == (type == HARD && expected < 2), "mine failure threshold");
            }
        }
        var scratch = fixture(LunaticRave2, HARD, 30);
        scratch.input.setKeyState(7, true, SECOND - 100_000);
        scratch.input.setKeyState(7, false, SECOND);
        scratch.input.setKeyState(8, true, SECOND);
        scratch.judge.update(SECOND + 1_000);
        expect(scratch, 20, 1, "scratch handoff is one hit");

        var combo = fixture(LunaticRave2, HARD, 30);
        combo.input.setKeyState(KEY, true, FIRST);
        combo.judge.update(FIRST + 1_000);
        check(combo.judge.getCombo() == 1 && combo.judge.getPastNotes() == 1, "mine keeps combo and scorable-note count");
        combo.input.setKeyState(KEY, false, SECOND - 100_000);
        combo.input.setKeyState(KEY, true, 4_000_000);
        combo.judge.update(4_001_000);
        check(combo.judge.getCombo() == 2 && combo.judge.getPastNotes() == 2, "normal judgment resumes after mines");
        System.out.println("LunaticRave2MineComparisonTest passed");
    }

    private static void checkpoint(String label, KeyboardJudgeCompatibilityTest.Fixture lr2,
                                   KeyboardJudgeCompatibilityTest.Fixture standard, long time,
                                   float lr2Hp, float standardHp, int poor) {
        lr2.judge.update(time);
        standard.judge.update(time);
        expect(lr2, lr2Hp, poor, label);
        expect(standard, standardHp, 0, label);
        System.out.printf("%s: OpenLR2=%s/%d LunaticRave2=%s/%d beatoraja=%s/%d%n", label, lr2Hp, poor,
                lr2.gauge.getValue(), lr2.judge.getJudgeCount(5), standard.gauge.getValue(), standard.judge.getJudgeCount(5));
    }

    private static void assertTap(BMSPlayerRule rule, long press, long release, boolean hit) throws Exception {
        var fixture = fixture(rule, HARD, 30);
        fixture.input.setKeyState(KEY, true, press);
        fixture.input.setKeyState(KEY, false, release);
        fixture.judge.update(FIRST + 10_000);
        expect(fixture, hit ? 20 : 30, hit ? 1 : 0, rule + " press/release " + press + "/" + release);
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(BMSPlayerRule rule, int type, float hp) throws Exception {
        try (var stream = Objects.requireNonNull(LunaticRave2MineComparisonTest.class
                .getResourceAsStream("/play/lunatic-rave2/mines.bms"))) {
            var model = new BMSDecoder().decode(stream.readAllBytes(), "mines.bms", false, new int[0]);
            check(model != null && model.getMode() == Mode.BEAT_7K && model.getTotalNotes() == 3, "7KEY mine fixture");
            var notes = model.getLanes()[KEY].getNotes();
            check(notes[0] instanceof MineNote mine && mine.getDamage() == 10 && mine.getMicroTime() == FIRST,
                    "0A is ten points of damage");
            var chart = BMSPlayerRule.ChartParameters.fromBms(model);
            check(BMSPlayerRule.select(rule, model, Mode.BEAT_7K, BMSPlayerMode.PLAY, false, type, chart) == rule,
                    "mine chart is eligible");
            BMSPlayerRule.validate(model);
            var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, rule, chart, type);
            fixture.input.setKeyState(1, true, 1_000_000);
            fixture.judge.update(1_000_000);
            check(fixture.judge.getCombo() == 1 && fixture.judge.getPastNotes() == 1, "initial combo");
            fixture.gauge.setValue(type, hp);
            return fixture;
        }
    }

    private static void expect(KeyboardJudgeCompatibilityTest.Fixture fixture, float hp, int poor, String label) {
        check(fixture.gauge.getValue() == hp && fixture.judge.getJudgeCount(5) == poor
                        && fixture.judge.getJudgeCount(4) == 0 && fixture.judge.getPastNotes() == 1,
                label + ": expected HP/POOR " + hp + "/" + poor + ", got "
                        + fixture.gauge.getValue() + "/" + fixture.judge.getJudgeCount(5));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
