package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import com.badlogic.gdx.utils.Json;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static bms.player.beatoraja.play.AbstractGrooveGauge.*;

public final class PrimaryScoringTest {
    private static BMSModel model() {
        return new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 tap.wav\n#00119:01\n#00219:01\n")
                .getBytes(StandardCharsets.UTF_8), "primary.bme", false, new int[0]);
    }
    public static void main(String[] args) throws Exception {
        var config = new PlayerConfig();
        check(config.getPrimaryScoringRule(Mode.BEAT_7K) == SecondaryScoringRule.BEATORAJA, "Old profile default");
        config.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
        config.setPrimaryScoringRule(Mode.BEAT_14K, SecondaryScoringRule.PULSUS);
        var json = new Json();
        var restored = json.fromJson(PlayerConfig.class, json.toJson(config));
        restored.validate();
        check(restored.getPrimaryScoringRule(Mode.BEAT_7K) == SecondaryScoringRule.LR2
                && restored.getPrimaryScoringRule(Mode.BEAT_14K) == SecondaryScoringRule.PULSUS, "Mode-specific profile round trip");
        config.setSecondaryScoringRuleIds(new String[]{"beatoraja", "lr2", "lr2"});
        check(Arrays.equals(config.getSecondaryScoringRuleIds(), new String[]{"lr2"}), "beatoraja is implicit");
        var oldNative = new ScoreData(); oldNative.setNativeScore(500); oldNative.setEpg(10);
        var newNative = new ScoreData(); newNative.setNativeScore(600); newNative.setEpg(5);
        check(bms.player.beatoraja.result.AbstractResult.ReplayAutoSaveConstraint.ANYONE_UPDATE.isQualified(oldNative, newNative),
                "Any-update replay saving honors native-only improvement");
        for (var rule : SecondaryScoringRule.values()) {
            var chart = model();
            var parameters = BMSPlayerRule.ChartParameters.fromBms(chart);
            BMSPlayerRule.validate(chart);
            var f = new KeyboardJudgeCompatibilityTest.Fixture(chart, true, BMSPlayerRule.Beatoraja_7, parameters, NORMAL);
            var selected = PrimaryScoringSelection.resolve(rule, chart, f.resource, NORMAL);
            boolean timingOnly = rule == SecondaryScoringRule.RAINDROP || rule == SecondaryScoringRule.BEMUSE;
            check(selected.effective() == (timingOnly ? SecondaryScoringRule.BEATORAJA : rule), "Eligible primary " + rule);
            f = new KeyboardJudgeCompatibilityTest.Fixture(chart, true, selected.isStandard()
                    ? BMSPlayerRule.Beatoraja_7 : selected.effective().scoreRule(), parameters, NORMAL);
            f.resource.setPrimaryScoringSelection(selected);
            check(bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1011).get(f.player).equals(selected.effective().displayName())
                    && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty("display_scoring_rule").get(f.player).equals(selected.effective().displayName())
                    && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1013).get(f.player).equals(selected.requested().displayName()),
                    "Play rule strings use the frozen selection, including fallback: " + rule);
            var hitError = new bms.player.beatoraja.skin.SkinHitErrorVisualizer(301, 150, 1, 1, 1, 0,
                    "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", .5f, 100, 0, 0);
            hitError.setDestination(0, 0, 0, 301, 100, 0, 255, 255, 255, 255, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0);
            hitError.prepare(0, f.player);
            var areaField = hitError.getClass().getDeclaredField("judgeArea"); areaField.setAccessible(true);
            check(Arrays.equals((int[]) areaField.get(hitError), f.judge.getTimingJudgeArea()), "Hit-error colors use active judge windows: " + rule);
            f.playerConfig.setSecondaryScoringRuleIds(new String[]{rule.id(), "lr2"});
            var group = SecondaryJudgingGroup.create(f.player, chart, f.resource, 6_000_000);
            f.input.setKeyState(6, true, 2_017_000);
            f.judge.update(2_018_001, group);
            check(chart.getAllTimeLines()[1].getNote(6).getState() != 0, "Primary consumes actual notes: " + rule);
            if (rule == SecondaryScoringRule.LR2) check(f.judge.getJudgeCount(0) == 1, "LR2 real primary window");
            check(f.judge.getTimingData().timings().length == 1, "Primary timing captures actual judgments: " + rule);
            if (rule == SecondaryScoringRule.LR2 || rule == SecondaryScoringRule.PULSUS)
                check(f.judge.getTimingJudgeArea()[1] == 18, "Native RANK 2 timing background: " + rule);
            if (rule == SecondaryScoringRule.ANGOLMOIS)
                check(f.judge.getTimingJudgeArea()[1] == 14, "Angolmois uses its scaled timing coordinate");
            if (rule == SecondaryScoringRule.ANGOLMOIS) check(f.judge.getScoreData().getNativeScore() > 0, "Angolmois live native score");
            if (!selected.isStandard()) {
                var results = group.finish(false);
                var timing = f.resource.getScoringTiming("beatoraja");
                check(timing != null && timing.timings().length == 1 && timing.timings()[0] == -17
                        && timing.judgeArea()[1] != f.judge.getTimingJudgeArea()[1], "Independent beatoraja timing snapshot: " + rule);
                check(results.stream().noneMatch(r -> r.rule() == rule), "No duplicate primary run");
                var standard = results.stream().filter(r -> r.rule() == SecondaryScoringRule.BEATORAJA).findFirst().orElseThrow().judgment();
                check(standard.metadata().avgjudge() == 508500 && standard.metadata().totalDuration() == 1017000,
                        "Independent beatoraja statistics include its own hit and unplayed note: " + rule);
                check(standard.judgeCounts().get(0) + standard.judgeCounts().get(1) == 0
                        && standard.judgeCounts().get(2) + standard.judgeCounts().get(3) == 1,
                        "Always-on beatoraja retains its own RANK 2 judgment: " + rule);
            }
            if (rule == SecondaryScoringRule.LR2 || rule == SecondaryScoringRule.PULSUS || rule == SecondaryScoringRule.ANGOLMOIS) {
                var fallback = PrimaryScoringSelection.resolve(rule, chart, f.resource, EXHARD);
                check(fallback.effective() == SecondaryScoringRule.BEATORAJA && !fallback.reason().isBlank(), "Exact unsupported gauge fallback: " + rule);
            }
            f.resource.setPlayMode(BMSPlayerMode.PRACTICE);
            check(PrimaryScoringSelection.resolve(rule, chart, f.resource, NORMAL).isStandard(), "Practice fallback");
        }
        var dp = new BMSDecoder().decode(("#PLAYER 3\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 tap.wav\n#00119:01\n#00129:01\n")
                .getBytes(StandardCharsets.UTF_8), "primary-dp.bme", false, new int[0]);
        var parameters = BMSPlayerRule.ChartParameters.fromBms(dp);
        BMSPlayerRule.validate(dp);
        var f = new KeyboardJudgeCompatibilityTest.Fixture(dp, true, BMSPlayerRule.LunaticRave2, parameters, NORMAL);
        f.resource.setPrimaryScoringSelection(new PrimaryScoringSelection(SecondaryScoringRule.LR2, SecondaryScoringRule.LR2, ""));
        var group = SecondaryJudgingGroup.create(f.player, dp, f.resource, 6_000_000);
        f.input.setKeyState(6, true, 2_017_000); f.input.setKeyState(15, true, 2_017_000);
        f.judge.update(2_020_001, group);
        check(f.judge.getJudgeCount(0) == 2, "LR2 DP primary path consumes FIFO once");
        var standard = group.finish(true).stream().filter(r -> r.rule() == SecondaryScoringRule.BEATORAJA).findFirst().orElseThrow().judgment();
        check(standard.judgeCounts().get(2) + standard.judgeCounts().get(3) == 2, "DP beatoraja receives the same captured input");
        var replay = new ReplayData();
        f.resource.setPlayMode(BMSPlayerMode.REPLAY_1);
        f.playerConfig.setPrimaryScoringRule(dp.getMode(), SecondaryScoringRule.LR2);
        check(PrimaryScoringSelection.resolveReplay(replay, dp, f.resource).isStandard(), "Legacy replays ignore current primary settings");
        replay.primaryScoringRuleId = "lr2"; replay.gauge = NORMAL;
        check(PrimaryScoringSelection.resolveReplay(replay, dp, f.resource).effective() == SecondaryScoringRule.LR2, "Recorded replay rule");
        replay.scoringRuleVersion = 999;
        check(PrimaryScoringSelection.resolveReplay(replay, dp, f.resource).isStandard(), "Unsupported replay version falls back");
        var hold = new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#00159:0101\n")
                .getBytes(StandardCharsets.UTF_8), "primary-hold.bme", false, new int[0]);
        parameters = BMSPlayerRule.ChartParameters.fromBms(hold);
        BMSPlayerRule.validate(hold);
        f = new KeyboardJudgeCompatibilityTest.Fixture(hold, true, BMSPlayerRule.Pulsus, parameters, NORMAL);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(f.player, BMSPlayer.class, "players",
                new BMSPlayer.PlayerProperty[]{new BMSPlayer.PlayerProperty(null, f.judge, f.gauge, null)});
        f.input.setKeyState(6, true, 2_000_000); f.judge.update(2_000_001);
        check(!((BMSPlayer) f.player).isNoteEnd(), "Pulsus hold head must not finish the song");
        f.input.setKeyState(6, false, 3_000_000); f.judge.update(3_000_001);
        check(((BMSPlayer) f.player).isNoteEnd() && f.judge.getScoreData().getExscore() == 4, "Pulsus hold tail completes its own note count");
        check(f.judge.getTimingData().timings().length == 2, "Pulsus timing distribution includes both judged hold endpoints");
        var angModel = model();
        parameters = BMSPlayerRule.ChartParameters.fromBms(angModel); BMSPlayerRule.validate(angModel);
        f = new KeyboardJudgeCompatibilityTest.Fixture(angModel, true, BMSPlayerRule.Angolmois, parameters, NORMAL);
        f.input.setKeyState(6, true, 2_000_000); f.judge.update(2_000_001);
        var property = new ScoreDataProperty();
        property.setTargetScore(750, null, 750, null, 2, 750);
        property.update(f.judge.getScoreData(), 1);
        check(f.judge.getScoreData().getRuleScore() == 300 && property.getNowRateInt() == 100
                && property.getNowBestScore() == 300 && property.getNowRivalScore() == 300,
                "Angolmois perfect first note uses its actual prefix maximum for rates and comparison graphs");
        var controller = bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate(MainController.class);
        var pendingResource = f.resource;
        var pendingModel = angModel;
        var pendingReplay = new ReplayData();
        var pendingTarget = new ScoreData();
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(controller, MainController.class, "resource", pendingResource);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(pendingResource, PlayerResource.class, "model", pendingModel);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(pendingResource, PlayerResource.class, "replay", pendingReplay);
        pendingResource.setPrimaryScoringSelection(PrimaryScoringSelection.standard());
        pendingResource.setTargetScoreData(pendingTarget);
        var request = new TargetProperty.PendingTarget(controller, pendingResource, pendingModel, pendingReplay, pendingTarget);
        int[] applied = {0};
        check(request.apply(() -> applied[0]++), "Current IR target response applies");
        pendingResource.setPrimaryScoringSelection(new PrimaryScoringSelection(SecondaryScoringRule.ANGOLMOIS, SecondaryScoringRule.ANGOLMOIS, ""));
        check(!request.apply(() -> applied[0]++), "Late beatoraja response cannot update a native primary");
        pendingResource.setPrimaryScoringSelection(PrimaryScoringSelection.standard());
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(pendingResource, PlayerResource.class, "replay", new ReplayData());
        check(!request.apply(() -> applied[0]++), "A new play of the same chart rejects old IR responses");
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(pendingResource, PlayerResource.class, "replay", pendingReplay);
        pendingResource.setTargetScoreData(new ScoreData());
        check(!request.apply(() -> applied[0]++) && applied[0] == 1, "Replacing a target rejects the previous response");
        System.out.println("PrimaryScoringTest passed (profile, real primary engines, independent beatoraja, fallback, deduplication)");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
