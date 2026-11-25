package bms.player.beatoraja.result;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.FloatArray;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

/** Executes the actual result save path with isolated profile files. */
public final class PrimaryResultPersistenceTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("primary-result-");
        var config = new Config(); config.setPlayerpath(directory.toString()); config.setPlayername("test");
        Files.createDirectories(directory.resolve("test"));
        var data = new PlayDataAccessor(config);
        var model = new BMSDecoder().decode(("#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#00119:01\n#00219:01\n")
                .getBytes(StandardCharsets.UTF_8), "result.bme", false, new int[0]);
        var standard = score(model, 0, 2, BMSPlayerRule.Beatoraja_7);
        var primary = score(model, 1, 1, BMSPlayerRule.Angolmois);
        primary.setNativeScore(700); primary.setNativeMaxScore(750);
        primary.setOption(3); primary.setSeed(12345); primary.setPlaytime(9000);
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        set(resource, PlayerResource.class, "model", model);
        set(resource, PlayerResource.class, "pconfig", new PlayerConfig());
        resource.setPlayMode(BMSPlayerMode.PLAY);
        resource.setUpdateScore(true);
        resource.setScoreData(primary);
        resource.setGrooveGauge(new GrooveGauge(model, AbstractGrooveGauge.NORMAL, GaugeProperty.SEVENKEYS));
        resource.setPrimaryScoringSelection(new PrimaryScoringSelection(SecondaryScoringRule.ANGOLMOIS, SecondaryScoringRule.ANGOLMOIS, ""));
        var previousStandard = score(model, 0, 2, BMSPlayerRule.Beatoraja_7);
        var standardGauge = new FloatArray[]{new FloatArray(), new FloatArray(), FloatArray.with(80)};
        var alternate = new PlayerResource.AlternateResultScore("beatoraja", "beatoraja", standard, previousStandard,
                new PlayerResource.GaugeHistory(null, standardGauge, null, 0));
        resource.setAlternateResultScores(List.of(alternate));
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        set(main, MainController.class, "resource", resource);
        set(main, MainController.class, "playdata", data);
        var result = new MusicResult(main);
        var save = MusicResult.class.getDeclaredMethod("updateScoreDatabase"); save.setAccessible(true);
        save.invoke(result);
        var best = data.readRuleBest(SecondaryScoringRule.ANGOLMOIS, model.getSHA256(), 0);
        check(best != null && best.getNativeScore() == 700 && best.getNativeMaxScore() == 750 && best.getExscore() == 3, "Actual primary goes to its own rule store");
        check(data.readScoreData(model, 0).getExscore() == 2, "Canonical database contains only independent beatoraja scoring");
        check(result.getDisplayScoringSummary().startsWith("Primary: Angolmois") && result.getDisplayScoringSummary().contains("700 / 750"), "Primary native result summary");
        var irScore = result.selectIRScore();
        check(irScore.score() == standard && irScore.previousBest() == previousStandard, "IR selects beatoraja and its pre-save best");
        check(irScore.shouldSend(IRConfig.IR_SEND_ALWAYS), "Always-send policy");
        check(irScore.shouldSend(IRConfig.IR_SEND_COMPLETE_SONG), "Completion uses beatoraja gauge even when primary gauge is empty");
        check(!irScore.shouldSend(IRConfig.IR_SEND_UPDATE_SCORE), "Primary score improvement does not qualify as beatoraja improvement");
        previousStandard.setEgr(1);
        check(irScore.shouldSend(IRConfig.IR_SEND_UPDATE_SCORE), "Beatoraja improvement qualifies against its own best");
        standardGauge[2].set(0, 0);
        check(!result.selectIRScore().shouldSend(IRConfig.IR_SEND_COMPLETE_SONG), "Failed beatoraja gauge blocks completion policy");
        IRScoreData[] submitted = {null};
        var connection = new TestIRConnectionBase() {
            public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) {
                submitted[0] = score;
                return new IRResponse<>() {
                    public boolean isSucceeded() { return true; }
                    public String getMessage() { return "fixture"; }
                    public Object getData() { return null; }
                };
            }
        };
        check(new MusicResult.IRSendStatus(connection, new IRChartData(new SongData()), irScore.score()).send(), "IR queue sends selected score");
        check(submitted[0].getExscore() == 2 && submitted[0].rule == BMSPlayerRule.Beatoraja_7
                && submitted[0].option == 3 && submitted[0].seed == 12345 && submitted[0].playtime == 9000,
                "IR payload contains only beatoraja judgments with actual play metadata");
        resource.setAlternateResultScores(List.of());
        check(result.selectIRScore() == null, "Missing beatoraja result never substitutes native primary score");
        resource.setAlternateResultScores(List.of(alternate));
        // Reading/saving a replay never writes either score store.
        resource.setPlayMode(BMSPlayerMode.REPLAY_1);
        primary.setNativeScore(740);
        save.invoke(result);
        check(data.readRuleBest(SecondaryScoringRule.ANGOLMOIS, model.getSHA256(), 0).getNativeScore() == 700, "Replay is read-only");
        System.out.println("PrimaryResultPersistenceTest passed (native primary/canonical beatoraja separation, IR payload/policies and replay)");
    }
    private static ScoreData score(BMSModel model, int pg, int gr, BMSPlayerRule rule) {
        var score = new ScoreData(model.getMode());
        score.setSha256(model.getSHA256()); score.setNotes(2); score.setPassnotes(2);
        score.setEpg(pg); score.setEgr(gr); score.setCombo(2); score.setMinbp(0);
        score.setClear(ClearType.Normal.id); score.setGauge(AbstractGrooveGauge.NORMAL); score.setRule(rule);
        return score;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
