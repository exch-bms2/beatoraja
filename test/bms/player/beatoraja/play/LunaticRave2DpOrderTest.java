package bms.player.beatoraja.play;

import bms.model.BMSDecoder;
import bms.model.Mode;

import java.util.Objects;
import java.nio.charset.StandardCharsets;

import static bms.player.beatoraja.play.AbstractJudgeManager.PlayEnd.*;
import static bms.player.beatoraja.play.BMSPlayerRule.*;
import static bms.player.beatoraja.play.AbstractGrooveGauge.*;
import static bms.player.beatoraja.PlayerConfig.*;

public final class LunaticRave2DpOrderTest {
    private static final int LEFT = 0;
    private static final int SCRATCH = 7;
    private static final int RIGHT = 9;
    private static final long TICK = 2_126_000;

    public static void main(String[] args) throws Exception {
        checkSongEndTransition();
        checkGaugeAutoShift();
        checkSecondaryDpOrder(Mode.BEAT_10K, 5, 7);
        checkSecondaryDpOrder(Mode.BEAT_14K, SCRATCH, RIGHT);
        for (boolean reverse : new boolean[]{false, true}) {
            var f = fixture(LunaticRave2, NORMAL, 100);
            holdScratch(f);
            f.input.setKeyState(reverse ? RIGHT : LEFT, true, TICK + 100);
            f.input.setKeyState(reverse ? LEFT : RIGHT, true, TICK + 900);
            f.judge.update(TICK + 999);
            // Left scratch mine (-10), left BAD (-4), right PG (+30, capped).
            check(f.judge instanceof LunaticRave2JudgeManager && f.gauge instanceof LunaticRave2GrooveGauge
                    && f.gauge.getValue() == 100 && f.judge.getJudgeCount(0) == 1
                    && f.judge.getJudgeCount(3) == 1 && f.judge.getJudgeCount(5) == 1
                    && f.judge.getCombo() == 1, "left-to-right lane order independent of FIFO order");
        }
        var standard = fixture(Beatoraja_7, NORMAL, 100);
        holdScratch(standard);
        standard.input.setKeyState(RIGHT, true, TICK + 100);
        standard.input.setKeyState(LEFT, true, TICK + 900);
        standard.judge.update(TICK + 999);
        check(standard.judge instanceof JudgeManager && standard.gauge instanceof GrooveGauge
                && standard.gauge.getValue() == 97 && standard.judge.getCombo() == 0
                && standard.judge.getJudgeCount(5) == 0 && standard.judge.playEnd(2_127, 2_126) == UNSYNCHRONIZED,
                "default keeps FIFO, its timing windows and mine counting");

        var sameTick = fixture(LunaticRave2, HARD, 11);
        holdScratch(sameTick);
        sameTick.input.setKeyState(RIGHT, true, TICK + 700);
        sameTick.judge.update(TICK + 999);
        check(sameTick.gauge.getValue() == 0 && sameTick.judge.getJudgeCount(0) == 1,
                "right PG in the same lane pass counts but cannot revive HARD");
        check(sameTick.judge.playEnd(2_127, 2_126) == FAILED,
                "failure wins when song completion is also due");

        var nextTick = fixture(LunaticRave2, HARD, 11);
        holdScratch(nextTick);
        nextTick.input.setKeyState(RIGHT, true, TICK + 1_000);
        nextTick.judge.update(TICK + 1_000);
        check(nextTick.gauge.getValue() == 1 && nextTick.judge.getJudgeCount(0) == 0
                        && BMSPlayer.updateGaugeAutoShift(nextTick.gauge, nextTick.playerConfig, true),
                "a later tick cannot score after failure, even in the same FIFO drain");

        var exactFloor = fixture(LunaticRave2, HARD, 12);
        holdScratch(exactFloor);
        check(exactFloor.judge.playEnd(2_127, 2_126) == PLAYING,
                "render waits for judgment before successful completion");
        exactFloor.input.setKeyState(RIGHT, true, TICK + 700);
        exactFloor.judge.update(TICK + 999);
        check(exactFloor.judge.playEnd(2_127, 2_126) == PLAYING,
                "the last millisecond alone is not past the normal song end");
        exactFloor.judge.update(TICK + 1_000);
        check(Math.abs(exactFloor.gauge.getValue() - 2.1f) < 0.0001f
                && exactFloor.judge.playEnd(2_127, 2_126) == FINISHED,
                "exactly 2% is alive and can recover");
        System.out.println("LunaticRave2DpOrderTest passed");
    }

    private static void checkSecondaryDpOrder(Mode mode, int scratch, int right) throws Exception {
        for (boolean reverse : new boolean[]{false, true}) {
            var primary = fixture(mode, BMSPlayerRule.getBMSPlayerRule(mode), NORMAL, 100);
            var secondary = Objects.requireNonNull(SecondaryJudging.create(primary.judge.main, primary.model,
                    primary.resource, TICK + 20_000));
            secondary.gauge().setValue(NORMAL, 100);
            primary.input.setKeyState(scratch, true, 2_050_000);
            primary.judge.update(2_050_000, secondary);
            primary.input.setKeyState(reverse ? right : LEFT, true, TICK + 100);
            primary.input.setKeyState(reverse ? LEFT : right, true, TICK + 900);
            primary.judge.update(TICK + 999, secondary);
            var result = secondary.finish();
            var counts = result.judgeCounts();
            check(result.gaugeType() == NORMAL && result.gaugeValue() == 100 && counts.get(0) + counts.get(1) == 1
                            && counts.get(6) + counts.get(7) == 1 && counts.get(10) == 1
                            && result.passed() == 2 && result.maxCombo() == 1,
                    mode + " secondary orders left mine and BAD before right PG on the shared gauge: " + result);
        }

        var primary = fixture(mode, BMSPlayerRule.getBMSPlayerRule(mode), HARD, 100);
        var secondary = Objects.requireNonNull(SecondaryJudging.create(primary.judge.main, primary.model,
                primary.resource, TICK + 20_000));
        secondary.gauge().setValue(HARD, 11);
        primary.input.setKeyState(scratch, true, 2_050_000);
        primary.judge.update(2_050_000, secondary);
        primary.input.setKeyState(right, true, TICK + 1_000);
        primary.judge.update(TICK + 10_000, secondary);
        var result = secondary.finish();
        check(result.outcome() == SecondaryJudging.Outcome.FAILED
                        && result.gaugeValue() == 1 && result.exScore() == 0
                        && result.gaugeHistory().timesUs().peek() == TICK
                        && !primary.gauge.isFailed() && primary.judge.getJudgeCount(0) == 1,
                mode + " secondary closure ends at the fatal tick without blocking later primary input");
    }

    private static void checkGaugeAutoShift() throws Exception {
        for (int option : new int[]{GAUGEAUTOSHIFT_CONTINUE, GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE,
                GAUGEAUTOSHIFT_SELECT_TO_UNDER, GAUGEAUTOSHIFT_BESTCLEAR}) {
            var f = fixture(LunaticRave2, HARD, 11);
            f.playerConfig.setGauge(HARD);
            f.playerConfig.setGaugeAutoShift(option);
            f.gauge.setValue(NORMAL, 100);
            holdScratch(f);
            f.judge.update(TICK + 999);
            check(f.gauge.getValue() == 1 && f.gauge.isFailed()
                            && f.judge.playEnd(2_127, 2_126) == PLAYING,
                    "HARD dies without closing the DP judge in auto shift mode " + option);
            check(!BMSPlayer.updateGaugeAutoShift(f.gauge, f.playerConfig, false),
                    "auto shift keeps playing in mode " + option);
            int expected = switch (option) {
                case GAUGEAUTOSHIFT_CONTINUE -> HARD;
                case GAUGEAUTOSHIFT_BESTCLEAR -> HAZARD;
                default -> NORMAL;
            };
            check(f.gauge.getType() == expected, "expected gauge selection in mode " + option);
            f.input.setKeyState(RIGHT, true, TICK + 1_000);
            f.judge.update(TICK + 1_000);
            check(f.judge.getJudgeCount(0) == 1 && f.judge.playEnd(2_127, 2_126) == FINISHED,
                    "the next millisecond is judged and the song can finish in mode " + option);
        }
    }

    private static void checkSongEndTransition() {
        int playing = BMSPlayer.STATE_PLAY;
        int closed = BMSPlayer.STATE_FAILED;
        long end = 10_000;
        check(!BMSPlayer.shouldFinishSong(playing, UNSYNCHRONIZED, end, end),
                "render-clock rules do not finish at the exact boundary");
        check(BMSPlayer.shouldFinishSong(playing, UNSYNCHRONIZED, end + 1, end),
                "render-clock rules finish after the boundary if still playing");
        check(!BMSPlayer.shouldFinishSong(closed, UNSYNCHRONIZED, end + 1, end),
                "beatoraja and LR2 SP cannot overwrite a same-frame closure");
        check(!BMSPlayer.shouldFinishSong(playing, PLAYING, end + 1, end)
                        && !BMSPlayer.shouldFinishSong(playing, FAILED, end + 1, end)
                        && BMSPlayer.shouldFinishSong(playing, FINISHED, end + 1, end),
                "LR2 DP waits for judgment and never finishes on failure");
    }

    private static void holdScratch(KeyboardJudgeCompatibilityTest.Fixture f) {
        f.input.setKeyState(SCRATCH, true, 2_050_000);
        f.judge.update(2_050_000);
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(BMSPlayerRule rule, int gauge, float hp) throws Exception {
        return fixture(Mode.BEAT_14K, rule, gauge, hp);
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(Mode mode, BMSPlayerRule rule, int gauge, float hp) throws Exception {
        try (var stream = Objects.requireNonNull(LunaticRave2DpOrderTest.class
                .getResourceAsStream("/play/lunatic-rave2/dp-order.bms"))) {
            var source = stream.readAllBytes();
            if (mode == Mode.BEAT_10K) {
                source = new String(source, StandardCharsets.US_ASCII)
                        .replace("#00219:", "#00215:").replace("#00229:", "#00225:")
                        .getBytes(StandardCharsets.US_ASCII);
            }
            var model = new BMSDecoder().decode(source, "dp-order.bms", false, new int[0]);
            check(model != null && model.getMode() == mode && model.getTotalNotes() == 4, mode + " fixture");
            var params = BMSPlayerRule.ChartParameters.fromBms(model);
            BMSPlayerRule.validate(model);
            var f = new KeyboardJudgeCompatibilityTest.Fixture(model, true, rule, params, gauge);
            f.gauge.setValue(gauge, hp);
            return f;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
