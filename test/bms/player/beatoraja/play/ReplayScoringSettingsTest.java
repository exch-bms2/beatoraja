package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import com.badlogic.gdx.utils.Json;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

/** Changes the live profile between recording and playback, then compares real judge/gauge traces. */
public final class ReplayScoringSettingsTest {
    public static void main(String[] args) throws Exception {
        for (var rule : new BMSPlayerRule[]{BMSPlayerRule.Beatoraja_7, BMSPlayerRule.Beatoraja_9,
                BMSPlayerRule.LunaticRave2, BMSPlayerRule.Pulsus}) {
            for (int shift : new int[]{PlayerConfig.GAUGEAUTOSHIFT_NONE, PlayerConfig.GAUGEAUTOSHIFT_BESTCLEAR,
                    PlayerConfig.GAUGEAUTOSHIFT_SELECT_TO_UNDER}) verify(rule, shift);
        }
        verifyLegacy();
        System.out.println("ReplayScoringSettingsTest passed (JSON, custom key/scratch windows, PMS LN margin, native GAS traces, independent beatoraja and profile isolation)");
    }

    private static void verify(BMSPlayerRule rule, int shift) throws Exception {
        int gauge = rule == BMSPlayerRule.Beatoraja_7 ? AbstractGrooveGauge.EXHARD : AbstractGrooveGauge.NORMAL;
        var recorded = fixture(rule, gauge);
        var config = recorded.playerConfig;
        config.setCustomJudge(true);
        config.setKeyJudgeWindowRatePerfectGreat(50); config.setKeyJudgeWindowRateGreat(80); config.setKeyJudgeWindowRateGood(90);
        config.setScratchJudgeWindowRatePerfectGreat(40); config.setScratchJudgeWindowRateGreat(70); config.setScratchJudgeWindowRateGood(85);
        config.setLongnoteMarginRate(50); config.setGaugeAutoShift(shift); config.setBottomShiftableGauge(AbstractGrooveGauge.EASY);
        var info = prepare(recorded, null);
        var saved = new ReplayData(); saved.gauge = gauge; saved.scoringSettings = info.scoringSettings;
        saved.primaryScoringRuleId = rule == BMSPlayerRule.LunaticRave2 ? "lr2" : rule == BMSPlayerRule.Pulsus ? "pulsus" : "beatoraja";
        saved.requestedPrimaryScoringRuleId = rule == BMSPlayerRule.Beatoraja_7 ? "lr2" : saved.primaryScoringRuleId;
        saved.primaryScoringFallbackReason = rule == BMSPlayerRule.Beatoraja_7 ? "Unsupported gauge" : "";
        var json = new Json(); var restored = json.fromJson(ReplayData.class, json.toJson(saved));
        String settings = json.toJson(saved.scoringSettings);
        recorded.judge.init(recorded.model, recorded.resource);
        var expected = trace(recorded);

        var replayed = fixture(rule, gauge);
        replayed.resource.setPlayMode(BMSPlayerMode.REPLAY_1);
        var current = replayed.playerConfig;
        current.setCustomJudge(false); current.setKeyJudgeWindowRatePerfectGreat(100);
        current.setScratchJudgeWindowRatePerfectGreat(100); current.setLongnoteMarginRate(150);
        current.setGaugeAutoShift(shift == PlayerConfig.GAUGEAUTOSHIFT_NONE ? PlayerConfig.GAUGEAUTOSHIFT_BESTCLEAR : PlayerConfig.GAUGEAUTOSHIFT_NONE);
        current.setBottomShiftableGauge(AbstractGrooveGauge.HAZARD); current.setGauge(AbstractGrooveGauge.HAZARD);
        prepare(replayed, restored);
        check(json.toJson(ReplayData.ScoringSettings.capture(replayed.resource.getScoringConfig())).equals(settings),
                "Every recorded scoring setting is restored independently of current profile values");
        check(replayed.resource.getScoringConfig().getGauge() == gauge, "Auto-shift uses the replay's starting gauge as its selection limit");
        replayed.judge.init(replayed.model, replayed.resource);
        check(trace(replayed).equals(expected), "Primary and independent beatoraja judgments/scores/gauges match: " + rule + "/" + shift);
        check(!current.isCustomJudge() && current.getLongnoteMarginRate() == 150
                && current.getGauge() == AbstractGrooveGauge.HAZARD && current.getBottomShiftableGauge() == AbstractGrooveGauge.HAZARD,
                "Playback leaves current profile settings unchanged");
        var selected = PrimaryScoringSelection.resolveReplay(restored, replayed.model, replayed.resource);
        check(selected.effective().id().equals(restored.primaryScoringRuleId), "Restored scoring settings preserve the recorded primary or fallback rule");
    }

    private static KeyboardJudgeCompatibilityTest.Fixture fixture(BMSPlayerRule rule, int gauge) throws Exception {
        var model = new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 100\n#WAV01 a.wav\n"
                + (rule == BMSPlayerRule.Beatoraja_9 ? "#00159:0101\n" : "#00119:01\n#00116:0001\n") + "#00219:01\n")
                .getBytes(StandardCharsets.UTF_8), "replay-scoring.bme", false, new int[0]);
        if (rule == BMSPlayerRule.Beatoraja_9) model.setMode(Mode.POPN_9K);
        var parameters = BMSPlayerRule.ChartParameters.fromBms(model); BMSPlayerRule.validate(model);
        return new KeyboardJudgeCompatibilityTest.Fixture(model, true, rule, parameters, gauge);
    }

    private static ReplayData prepare(KeyboardJudgeCompatibilityTest.Fixture fixture, ReplayData saved) throws Exception {
        var info = new ReplayData();
        set(fixture.player, BMSPlayer.class, "playinfo", info); set(fixture.player, BMSPlayer.class, "replay", saved);
        var prepare = BMSPlayer.class.getDeclaredMethod("prepareScoringSettings", PlayerConfig.class); prepare.setAccessible(true);
        prepare.invoke(fixture.player, fixture.playerConfig);
        return info;
    }

    private static List<Number> trace(KeyboardJudgeCompatibilityTest.Fixture fixture) throws Exception {
        var independent = SecondaryJudging.createBeatoraja(fixture.player, fixture.model, fixture.resource, 5_000_000,
                fixture.resource.getScoringConfig().getGauge());
        var group = new SecondaryJudgingGroup(List.of(new SecondaryJudgingGroup.Entry(SecondaryScoringRule.BEATORAJA, independent)));
        var judgeField = SecondaryJudging.class.getDeclaredField("judge"); judgeField.setAccessible(true);
        var other = (AbstractJudgeManager) judgeField.get(independent);
        var values = new ArrayList<Number>();
        for (long time = 1_000_000; time <= 5_000_000; time += 10_000) {
            if (time == 2_010_000 || time == 4_010_000) fixture.input.setKeyState(6, true, time);
            if (time == 2_400_000 || time == 4_100_000) fixture.input.setKeyState(6, false, time);
            if (time == 2_510_000) fixture.input.setKeyState(7, true, time);
            if (time == 2_600_000) fixture.input.setKeyState(7, false, time);
            fixture.judge.update(time + 1, group);
            BMSPlayer.updateGaugeAutoShift(fixture.gauge, fixture.resource.getScoringConfig(), fixture.gauge.isFailed());
            for (var judge : new AbstractJudgeManager[]{fixture.judge, other}) {
                values.add(judge.getScoreData().getRuleScore());
                for (int j = 0; j < 6; j++) { values.add(judge.getJudgeCount(j, true)); values.add(judge.getJudgeCount(j, false)); }
            }
            values.add(fixture.gauge.getType()); values.add(independent.gauge().getType());
            for (int i = 0; i < fixture.gauge.getGaugeTypeLength(); i++) values.add(fixture.gauge.getValue(i));
            for (int i = 0; i < independent.gauge().getGaugeTypeLength(); i++) values.add(independent.gauge().getValue(i));
        }
        return values;
    }

    private static void verifyLegacy() throws Exception {
        var fixture = fixture(BMSPlayerRule.Beatoraja_7, AbstractGrooveGauge.NORMAL);
        fixture.resource.setPlayMode(BMSPlayerMode.REPLAY_1);
        fixture.playerConfig.setCustomJudge(true); fixture.playerConfig.setKeyJudgeWindowRatePerfectGreat(75);
        var legacy = new Json().fromJson(ReplayData.class, "{gauge:2}");
        prepare(fixture, legacy);
        check(legacy.scoringSettings == null && fixture.resource.getScoringConfig().getKeyJudgeWindowRatePerfectGreat() == 75,
                "Legacy replays keep their current-settings compatibility behavior");
        fixture.resource.setPlayMode(BMSPlayerMode.PRACTICE);
        prepare(fixture, null);
        check(fixture.resource.getScoringConfig() == fixture.playerConfig, "Practice continues to use live settings");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
