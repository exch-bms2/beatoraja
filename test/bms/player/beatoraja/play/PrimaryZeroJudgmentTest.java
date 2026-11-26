package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.result.MusicResult;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

/** A native miss must not discard independently valid beatoraja judgments. */
public final class PrimaryZeroJudgmentTest {
    public static void main(String[] args) throws Exception {
        var model = new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 3\n#TOTAL 400\n#WAV01 a.wav\n#00119:01\n")
                .getBytes(StandardCharsets.UTF_8), "zero-primary.bme", false, new int[0]);
        var parameters = BMSPlayerRule.ChartParameters.fromBms(model); BMSPlayerRule.validate(model);
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, BMSPlayerRule.Pulsus, parameters, AbstractGrooveGauge.NORMAL);
        var player = (BMSPlayer) fixture.player;
        set(player, BMSPlayer.class, "model", model);
        set(player, BMSPlayer.class, "players", new BMSPlayer.PlayerProperty[]{new BMSPlayer.PlayerProperty(null, fixture.judge, fixture.gauge, null)});
        set(player, BMSPlayer.class, "playTimeTracker", new BMSPlayer.PlayTimeTracker());
        set(player, BMSPlayer.class, "playinfo", new ReplayData());
        set(player, BMSPlayer.class, "state", BMSPlayer.STATE_FINISHED);
        set(player.getSkin(), Skin.class, "header", new SkinHeader());
        set(fixture.resource, PlayerResource.class, "replay", new ReplayData());
        set(fixture.resource, PlayerResource.class, "model", model);
        fixture.resource.setUpdateScore(true);
        fixture.resource.setPrimaryScoringSelection(new PrimaryScoringSelection(SecondaryScoringRule.PULSUS, SecondaryScoringRule.PULSUS, ""));
        check(player.createScoreData() == null, "No-input behavior remains unchanged when neither rule has judgments");
        var group = SecondaryJudgingGroup.create(player, model, fixture.resource, 4_000_000);
        fixture.input.setKeyState(6, true, 2_205_000); fixture.judge.update(2_205_001, group);
        var judgment = group.finish(true).stream().filter(result -> result.rule() == SecondaryScoringRule.BEATORAJA).findFirst().orElseThrow().judgment();
        check(fixture.judge.getJudgeCount(0) + fixture.judge.getJudgeCount(1) + fixture.judge.getJudgeCount(2) + fixture.judge.getJudgeCount(3) == 0,
                "The real Pulsus engine has only miss judgments");
        var standard = judgment.toLocalAttempt(model).toScoreData(model.getMode());
        check(standard.getJudgeCount(3) == 1, "The same input is a valid beatoraja BAD");
        fixture.resource.setAlternateResultScores(List.of(new PlayerResource.AlternateResultScore("beatoraja", "beatoraja", standard, null, judgment.gaugeHistory(), true)));
        var primary = player.createScoreData();
        check(primary != null && primary.getRule() == BMSPlayerRule.Pulsus && primary.getRuleScore() == 0,
                "Keep the real zero-score native result instead of substituting beatoraja");
        check(fixture.resource.getReplayData().primaryScoringRuleId.equals("pulsus"), "Replay still records the actual native primary");
        fixture.resource.setScoreData(primary); fixture.resource.setGrooveGauge(fixture.gauge);
        var directory = Files.createTempDirectory("zero-primary-store-"); Files.createDirectories(directory.resolve("test"));
        var config = new Config(); config.setPlayerpath(directory.toString()); config.setPlayername("test");
        var data = new PlayDataAccessor(config);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        set(main, MainController.class, "resource", fixture.resource); set(main, MainController.class, "playdata", data);
        set(main, MainController.class, "player", fixture.playerConfig);
        var result = new MusicResult(main);
        var save = MusicResult.class.getDeclaredMethod("updateScoreDatabase"); save.setAccessible(true); save.invoke(result);
        check(data.readRuleBest(SecondaryScoringRule.PULSUS, model.getSHA256(), 0) != null
                && data.readScoreData(model, 0).getPlaycount() == 1 && data.readScoreData(model, 0).getMinbp() == 1,
                "Native and canonical results both reach their own stores, including an EX-zero beatoraja play");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("test/scoredatalog.db"));
                var rows = connection.createStatement().executeQuery("SELECT lbd FROM scoredatalog")) {
            check(rows.next() && rows.getInt(1) == 1, "Canonical performance history retains the actual BAD judgment");
        }
        var select = MusicResult.class.getDeclaredMethod("selectIRScore"); select.setAccessible(true); var irScore = select.invoke(result);
        var score = irScore.getClass().getDeclaredMethod("score"); score.setAccessible(true);
        check(score.invoke(irScore) == standard && standard.getRule() == BMSPlayerRule.Beatoraja_7,
                "IR still selects independent beatoraja judgments");
        var shouldSend = MusicResult.class.getDeclaredMethod("shouldSendIRScore", irScore.getClass(), int.class); shouldSend.setAccessible(true);
        check((boolean) shouldSend.invoke(result, irScore, IRConfig.IR_SEND_ALWAYS), "Valid beatoraja result remains eligible for IR transmission");
        System.out.println("PrimaryZeroJudgmentTest passed (real native miss/beatoraja BAD, result/replay creation, separate saves and IR eligibility)");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
