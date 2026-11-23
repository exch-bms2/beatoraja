package bms.player.beatoraja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import bms.player.beatoraja.PlayDataAccessor.ScoreAttempt;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;

public final class AngolmoisScoreDatabaseTest {
    private static final String HASH = "a".repeat(64);

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("angolmois-score-");
        var store = new RuleScoreStore(dir, "_angolmois");
        store.write(attempt(700, 2, ScoreOutcome.FINISHED), true);
        store.write(attempt(800, 1, ScoreOutcome.FINISHED), true);
        store.write(attempt(750, 3, ScoreOutcome.FINISHED), true);
        store.write(attempt(1000, 3, ScoreOutcome.FINISHED), false);
        store.write(attempt(1100, 3, ScoreOutcome.INTERRUPTED), true);
        var best = new RuleScoreStore(dir, "_angolmois").readBest(HASH, 0);
        check(best.getNativeScore() == 800 && best.getNativeMaxScore() == 1200 && best.getEpg() == 1,
                "Native best survives reopen with its own judgment counts, independently of EX best");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("scoredatalog_angolmois.db"));
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT native_score, native_max_score FROM scoredatalog ORDER BY rowid")) {
                for (int value : new int[]{700, 800, 750, 1000, 1100}) {
                    check(rows.next() && rows.getInt(1) == value && rows.getInt(2) == 1200, "Every native attempt is retained");
                }
            }
            statement.execute("CREATE TRIGGER reject_native BEFORE INSERT ON scoredatalog WHEN NEW.epg = 0 BEGIN SELECT RAISE(ABORT, 'reject'); END");
        }
        try {
            store.write(attempt(1199, 0, ScoreOutcome.FINISHED), true);
            throw new AssertionError("Injected rollback must fail");
        } catch (java.sql.SQLException expected) {
            check(store.readBest(HASH, 0).getNativeScore() == 800, "Native best rolls back with score/history");
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("scoredatalog_angolmois.db"));
             var statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER reject_native");
        }
        check(!Files.exists(dir.resolve("score.db")), "Native store does not write the primary database");
        var zero = new ScoreAttempt("b".repeat(64), 0, ScoreOutcome.FINISHED, 3, 3, 0, 3,
                ClearType.Failed, 0, -20, java.util.Collections.nCopies(12, 0), 0, 1200);
        store.write(zero, true);
        check(store.readBest(zero.sha256(), 0).getNativeScore() == 0, "Zero native score is retained");
        System.out.println("AngolmoisScoreDatabaseTest passed (native best/history/reopen/rollback)");
    }

    private static ScoreAttempt attempt(int nativeScore, int cool, ScoreOutcome outcome) {
        Integer[] counts = new Integer[12];
        Arrays.fill(counts, 0);
        counts[0] = cool;
        counts[4] = 3 - cool;
        return new ScoreAttempt(HASH, 0, outcome, 3, 3, cool, 0, ClearType.Normal, 0, 50,
                List.of(counts), nativeScore, 1200);
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
