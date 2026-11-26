package bms.player.beatoraja;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.play.SecondaryScoringRule;
import java.nio.file.Files;
import java.util.List;

/** The note denominator belongs to the best score, not the latest BMS branch. */
public final class BestScoreNoteCountTest {
    public static void main(String[] args) throws Exception {
        var root = Files.createTempDirectory("best-note-count-"); Files.createDirectories(root.resolve("test"));
        var config = new Config(); config.setPlayerpath(root.toString()); config.setPlayername("test");
        var data = new PlayDataAccessor(config);
        for (var rule : new SecondaryScoringRule[]{SecondaryScoringRule.LR2, SecondaryScoringRule.PULSUS, SecondaryScoringRule.ANGOLMOIS}) {
            boolean nativePoints = rule == SecondaryScoringRule.ANGOLMOIS;
            String hash = Integer.toHexString(rule.ordinal() + 1).repeat(64);
            data.writeRuleAttempt(rule, attempt(hash, 2, 2, nativePoints ? 700 : -1, nativePoints ? 750 : -1, ClearType.Normal), true);
            data.writeRuleAttempt(rule, attempt(hash, 1, 0, nativePoints ? 300 : -1, nativePoints ? 300 : -1, ClearType.Failed), true);
            checkBest(data.readRuleBest(rule, hash, 0), 2, 2, nativePoints ? 700 : 4, nativePoints ? 750 : 4, "Lower shorter branch: " + rule);
            data.writeRuleAttempt(rule, attempt(hash, 3, 2, nativePoints ? 700 : -1, nativePoints ? 1200 : -1, ClearType.Hard), true);
            checkBest(data.readRuleBest(rule, hash, 0), 2, 2, nativePoints ? 700 : 4, nativePoints ? 750 : 4, "Tied score with improved clear: " + rule);
            data.writeRuleAttempt(rule, attempt(hash, 3, 3, nativePoints ? 800 : -1, nativePoints ? 1200 : -1, ClearType.Normal), true);
            checkBest(new PlayDataAccessor(config).readRuleBest(rule, hash, 0), 3, 3, nativePoints ? 800 : 6, nativePoints ? 1200 : 6, "Winning branch persists after reopen: " + rule);
            data.writeRuleAttempt(rule, attempt(hash, 4, 4, nativePoints ? 1000 : -1, nativePoints ? 1800 : -1, ClearType.Normal), false);
            check(data.readRuleBest(rule, hash, 0).getNotes() == 3, "Ineligible score preserves the best denominator");
        }
        var model = new BMSModel(); model.setMode(Mode.BEAT_7K); model.setSHA256("b".repeat(64));
        var first = score(model, 2, 2); data.writeScoreData(first, model, 0, true);
        data.writeScoreData(score(model, 1, 0), model, 0, true);
        checkBest(data.readScoreData(model, 0), 2, 2, 4, 4, "Independent beatoraja best also retains its branch");
        data.writeScoreData(score(model, 3, 3), model, 0, true);
        checkBest(data.readScoreData(model, 0), 3, 3, 6, 6, "Beatoraja score improvement replaces the denominator");
        System.out.println("BestScoreNoteCountTest passed (native/EX best notes, ties, clear-only updates, reopen and independent beatoraja)");
    }
    private static ScoreData score(BMSModel model, int notes, int pg) {
        var score = new ScoreData(Mode.BEAT_7K); score.setSha256(model.getSHA256()); score.setNotes(notes); score.setEpg(pg);
        score.setEpr(notes - pg); score.setClear(ClearType.Failed.id); score.setMinbp(notes - pg); return score;
    }
    private static PlayDataAccessor.ScoreAttempt attempt(String hash, int notes, int pg, int points, int maximum, ClearType clear) {
        return new PlayDataAccessor.ScoreAttempt(hash, 0, PlayDataAccessor.ScoreOutcome.FINISHED, notes, notes, pg, notes - pg,
                clear, 2, 100, List.of(pg, 0, 0, 0, 0, 0, 0, 0, notes - pg, 0, 0, 0), points, maximum);
    }
    private static void checkBest(ScoreData best, int notes, int pg, int points, int maximum, String message) {
        check(best != null && best.getNotes() == notes && best.getJudgeCount(0) == pg
                && best.getRuleScore() == points && best.getRuleMaxScore() == maximum, message);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
