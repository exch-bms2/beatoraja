package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ScoringEligibilityTest {
    public static void main(String[] args) throws Exception {
        var config = new PlayerConfig(); config.setCustomJudge(true); config.setKeyJudgeWindowRatePerfectGreat(150);
        for (var rule : SecondaryScoringRule.values()) {
            var policy = ScoringEligibility.resolve(rule, config, true, 0, true);
            check(policy.updateScore() == (rule != SecondaryScoringRule.BEATORAJA), "Only the rule using expanded windows is ineligible: " + rule);
            check(policy.assist() == (rule == SecondaryScoringRule.BEATORAJA ? 2 : 0), "Assist labels follow the rule actually used");
            check(!ScoringEligibility.resolve(rule, config, false, 1, true).updateScore(), "Shared chart assists still block every rule");
            check(ScoringEligibility.resolve(rule, config, true, 0, false).updateScore(), "Practice omits custom expansion eligibility");
        }
        config.setKeyJudgeWindowRatePerfectGreat(100); config.setLongnoteMarginRate(150);
        check(!ScoringEligibility.resolve(SecondaryScoringRule.BEATORAJA, config, true, 0, true).updateScore()
                && ScoringEligibility.resolve(SecondaryScoringRule.PULSUS, config, true, 0, true).updateScore(), "LN expansion eligibility belongs to beatoraja");
        for (int margin : new int[]{50, 100, 150}) verifyLnJudgments(margin);
        System.out.println("ScoringEligibilityTest passed (rule-specific eligibility, assists and primary/independent PMS LN judgments and gauges)");
    }

    private static void verifyLnJudgments(int margin) throws Exception {
        var model = new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#00159:0101\n")
                .getBytes(StandardCharsets.UTF_8), "pms-ln.bme", false, new int[0]);
        model.setMode(Mode.POPN_9K);
        var parameters = BMSPlayerRule.ChartParameters.fromBms(model); BMSPlayerRule.validate(model);
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, BMSPlayerRule.Beatoraja_9, parameters, AbstractGrooveGauge.NORMAL);
        fixture.playerConfig.setCustomJudge(true); fixture.playerConfig.setLongnoteMarginRate(margin);
        fixture.judge.init(model, fixture.resource);
        var independent = SecondaryJudging.createBeatoraja(fixture.player, model, fixture.resource, 5_000_000, AbstractGrooveGauge.NORMAL);
        var group = new SecondaryJudgingGroup(List.of(new SecondaryJudgingGroup.Entry(SecondaryScoringRule.BEATORAJA, independent)));
        var field = SecondaryJudging.class.getDeclaredField("judge"); field.setAccessible(true);
        var other = (AbstractJudgeManager) field.get(independent);
        var release = JudgeManager.class.getDeclaredField("nreleasemargin"); release.setAccessible(true);
        check(release.getLong(other) == 200_000L * margin / 100 && release.getLong(other) == release.getLong(fixture.judge),
                "Independent beatoraja applies the same PMS LN margin: " + margin);
        for (long time = 1_000_000; time <= 4_000_000; time += 10_000) {
            if (time == 2_000_000) fixture.input.setKeyState(6, true, time);
            if (time == 2_400_000) fixture.input.setKeyState(6, false, time);
            fixture.judge.update(time + 1, group);
            for (int j = 0; j < 6; j++) for (boolean early : new boolean[]{false, true})
                check(fixture.judge.getJudgeCount(j, early) == other.getJudgeCount(j, early), "PMS judgments match throughout the LN release");
            check(fixture.judge.getScoreData().getRuleScore() == other.getScoreData().getRuleScore(), "PMS score matches");
            for (int g = 0; g < fixture.gauge.getGaugeTypeLength(); g++)
                check(fixture.gauge.getValue(g) == independent.gauge().getValue(g), "PMS gauges match during custom LN release");
        }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
