package bms.player.beatoraja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.PlayDataAccessor.ScoreAttempt;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;

public final class LunaticRave2ScoreDatabaseTest {
    private static final String HASH = "1".repeat(64);
    private static final String INTERRUPTED_HASH = "2".repeat(64);

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("lr2-score-test");
        try {
            run(dir);
            migrationRetry(Files.createDirectory(dir.resolve("migration-retry")));
            System.out.println("LunaticRave2ScoreDatabaseTest passed (separate files, interrupted history, idempotent migration and rollback)");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void migrationRetry(Path profile) throws Exception {
        var old = profile.resolve("score-lunaticrave2.db");
        createLegacy(old);
        var store = new RuleScoreStore(profile, "_lr2");
        var history = profile.resolve("scoredatalog_lr2.db");
        var bestLog = profile.resolve("scorelog_lr2.db");
        execute(history, """
                CREATE TRIGGER reject_second_attempt BEFORE INSERT ON scoredatalog WHEN NEW.epg = 120
                BEGIN SELECT RAISE(ABORT, 'injected migration failure'); END
                """);
        try {
            store.importLegacyLr2(old);
            throw new AssertionError("migration should fail on the second legacy history row");
        } catch (java.sql.SQLException e) {
            check(e.getMessage().contains("injected migration failure"), "migration reached the injected failure");
        }
        check(store.readBest(HASH, 0) == null && count(history, "scoredatalog") == 0 && count(bestLog, "scorelog") == 0,
                "failure rolls back earlier migrated rows and bests");
        execute(history, "DROP TRIGGER reject_second_attempt");
        store.importLegacyLr2(old);
        new RuleScoreStore(profile, "_lr2").importLegacyLr2(old);
        check(store.readBest(HASH, 0).getPlaycount() == 1
                        && count(history, "scoredatalog") == 2 && count(bestLog, "scorelog") == 1,
                "migration can retry and then is idempotent across restarts");
    }

    private static void run(Path dir) throws Exception {
        var config = new Config();
        config.setPlayerpath(dir.toString());
        config.setPlayername("test");
        var profile = Files.createDirectory(dir.resolve("test"));
        var scores = new PlayDataAccessor(config);
        var lr2File = profile.resolve("score_lr2.db");
        var historyFile = profile.resolve("scoredatalog_lr2.db");
        var bestLogFile = profile.resolve("scorelog_lr2.db");
        var standardFile = profile.resolve("score.db");
        var model = new BMSModel();
        model.setMode(Mode.BEAT_7K);
        model.setSHA256(HASH);
        var old = new ScoreData(Mode.BEAT_7K);
        old.setSha256(HASH);
        old.setNotes(250);
        old.setEpg(100);
        old.setClear(ClearType.Easy.id);
        old.setCombo(150);
        new ScoreDatabaseAccessor(standardFile.toString()).setScoreData(old);
        check(scores.readScoreData(model, 0).getExscore() == 200, "legacy best readable");
        check(scores.readLunaticRave2Best(HASH, 0) == null && !Files.exists(lr2File),
                "a read does not create the LR2 file or use the standard best");

        var legacy = profile.resolve("score-lunaticrave2.db");
        createLegacy(legacy);
        byte[] legacyBytes = Files.readAllBytes(legacy);
        var initial = scores.readLunaticRave2Best(HASH, 0);
        check(initial != null && initial.getExscore() == 180 && initial.getPlaycount() == 1,
                "legacy migration reconstructs best from eligible history");
        scores.writeLunaticRave2Attempt(attempt(HASH, 0, ScoreOutcome.FAILED, 220, 100, 6, ClearType.Failed), true);
        scores.writeLunaticRave2Attempt(attempt(INTERRUPTED_HASH, 0, ScoreOutcome.INTERRUPTED, 230, 120, 1, ClearType.Failed), true);
        scores.writeLunaticRave2Attempt(attempt(HASH, 0, ScoreOutcome.INPUT_LOST, 250, 120, 0, ClearType.Failed), true);
        scores.writeLunaticRave2Attempt(attempt(HASH, 0, ScoreOutcome.FINISHED, 260, 130, 0, ClearType.Hard), false);
        scores.writeLunaticRave2Attempt(attempt(HASH, 1, ScoreOutcome.FINISHED, 300, 150, 3, ClearType.Hard), true);
        check(Files.isRegularFile(standardFile) && Files.isRegularFile(lr2File)
                        && Files.isRegularFile(historyFile) && Files.isRegularFile(bestLogFile), "separate LR2 files");

        var reopened = new PlayDataAccessor(config);
        var best = reopened.readLunaticRave2Best(HASH, 0);
        check(best.getClear() == ClearType.Normal.id && best.getExscore() == 220 && best.getCombo() == 110
                        && best.getMinbp() == 5 && best.getPlaycount() == 2 && best.getClearcount() == 1,
                "completed and closed runs update metrics; interrupted, lost input and ineligible do not");
        check(reopened.readLunaticRave2Best(HASH, 1).getExscore() == 300
                        && reopened.readLunaticRave2Best(INTERRUPTED_HASH, 0) == null,
                "best is keyed separately by effective LN mode and chart");
        var standard = reopened.readScoreData(model, 0);
        check(standard.getExscore() == 200 && standard.getCombo() == 150 && standard.getClear() == ClearType.Easy.id,
                "the existing standard best is unchanged after reopening");
        check(Arrays.equals(legacyBytes, Files.readAllBytes(legacy)), "legacy DB remains unchanged");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + historyFile);
             var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                    SELECT outcome, best_eligible, epg, date FROM scoredatalog ORDER BY rowid
                    """)) {
            String[] outcomes = {"FINISHED", "INTERRUPTED", "FAILED", "INTERRUPTED", "INPUT_LOST", "FINISHED", "FINISHED"};
            int[] eligible = {1, 0, 1, 0, 0, 0, 1};
            int[] exscores = {180, 240, 220, 230, 250, 260, 300};
            for (int i = 0; i < outcomes.length; i++) {
                check(rows.next() && rows.getString(1).equals(outcomes[i]) && rows.getInt(2) == eligible[i]
                                && rows.getInt(3) == exscores[i] / 2 && (i > 1 || rows.getLong(4) == 10 + i),
                        "history retains each run with outcome and judgments, row " + i);
            }
            check(!rows.next(), "one history entry per run");
        }
        check(count(bestLogFile, "scorelog") == 3, "history-only entries never add a best-update log");
        execute(historyFile, """
                CREATE TRIGGER reject_attempt BEFORE INSERT ON scoredatalog
                BEGIN SELECT RAISE(ABORT, 'injected history failure'); END
                """);
        reopened.writeLunaticRave2Attempt(attempt(HASH, 0, ScoreOutcome.FINISHED, 400, 200, 0, ClearType.Hard), true);
        check(reopened.readLunaticRave2Best(HASH, 0).getExscore() == 220
                        && count(bestLogFile, "scorelog") == 3 && count(historyFile, "scoredatalog") == 7,
                "failed history insert rolls back the best and best-update log in other files");
        execute(historyFile, "DROP TRIGGER reject_attempt");
        reopened.writeLunaticRave2Attempt(attempt(HASH, 0, ScoreOutcome.FINISHED, 400, 200, 0, ClearType.Hard), true);
        check(reopened.readLunaticRave2Best(HASH, 0).getExscore() == 400
                        && count(bestLogFile, "scorelog") == 4 && count(historyFile, "scoredatalog") == 8,
                "retry commits all three files once");
    }

    private static void createLegacy(Path legacy) throws Exception {
        execute(legacy, """
                CREATE TABLE play_history (
                    id INTEGER PRIMARY KEY, sha256 TEXT, mode INTEGER, outcome TEXT, played_at INTEGER,
                    best_eligible INTEGER, notes INTEGER, passed INTEGER, exscore INTEGER, combo INTEGER,
                    minbp INTEGER, clear INTEGER, gauge INTEGER, gauge_value REAL,
                    epg INTEGER, lpg INTEGER, egr INTEGER, lgr INTEGER, egd INTEGER, lgd INTEGER,
                    ebd INTEGER, lbd INTEGER, epr INTEGER, lpr INTEGER, ems INTEGER, lms INTEGER
                )
                """);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + legacy);
             var insert = connection.prepareStatement("""
                     INSERT INTO play_history VALUES
                     (1, ?, 0, 'FINISHED', 10, 1, 250, 200, 180, 110, 5, 5, 2, 80, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                     (2, ?, 0, 'INTERRUPTED', 11, 0, 250, 200, 240, 120, 1, 1, 2, 80, 120, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                     """)) {
            insert.setString(1, HASH);
            insert.setString(2, HASH);
            insert.executeUpdate();
        }
    }

    private static int count(Path path, String table) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement();
             var row = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return row.getInt(1);
        }
    }

    private static void execute(Path path, String sql) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static ScoreAttempt attempt(String hash, int mode, ScoreOutcome outcome, int exscore, int combo, int minbp, ClearType clear) {
        return new ScoreAttempt(hash, mode, outcome, 250, 200, combo, minbp, clear, 2, 80f,
                List.of(exscore / 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
