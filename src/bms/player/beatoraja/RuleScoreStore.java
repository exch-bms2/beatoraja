package bms.player.beatoraja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import bms.player.beatoraja.PlayDataAccessor.ScoreAttempt;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;

/** Coordinates the standard score accessors across attached, rule-specific SQLite files. */
final class RuleScoreStore {
	private static final String LR2_MIGRATION = "score-lunaticrave2.db:v1";
	private static final String[] JUDGMENT_COLUMNS = {
			"epg", "lpg", "egr", "lgr", "egd", "lgd", "ebd", "lbd", "epr", "lpr", "ems", "lms"
	};

	private final ScoreDatabaseAccessor scores;
	private final ScoreLogDatabaseAccessor bestLog;
	private final ScoreDataLogDatabaseAccessor history;
	private final Path bestLogPath;
	private final Path historyPath;
	private final SQLiteDataSource source;
	private final QueryRunner query = new QueryRunner();

	RuleScoreStore(Path profile, String suffix) throws SQLException, ClassNotFoundException {
		var scorePath = profile.resolve("score" + suffix + ".db");
		bestLogPath = profile.resolve("scorelog" + suffix + ".db");
		historyPath = profile.resolve("scoredatalog" + suffix + ".db");
		scores = new ScoreDatabaseAccessor(scorePath.toString());
		scores.createTable();
		bestLog = new ScoreLogDatabaseAccessor(bestLogPath.toString());
		history = new ScoreDataLogDatabaseAccessor(historyPath.toString());
		history.createAttemptColumns();
		var config = new SQLiteConfig();
		config.setBusyTimeout(1_000);
		config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
		source = new SQLiteDataSource(config);
		source.setUrl("jdbc:sqlite:" + scorePath);
	}

	ScoreData readBest(String sha256, int mode) throws SQLException {
		try (var connection = source.getConnection()) {
			return scores.getScoreData(connection, sha256, mode);
		}
	}

	void write(ScoreAttempt attempt, boolean allowed) throws SQLException {
		inTransaction(null, connection -> save(connection, attempt, allowed, System.currentTimeMillis() / 1000));
	}

	void importLegacyLr2(Path legacy) throws SQLException {
		if (!Files.exists(legacy)) return;
		inTransaction(legacy, connection -> {
			query.update(connection, "CREATE TABLE IF NOT EXISTS main.score_store_migration (id TEXT PRIMARY KEY)");
			if (query.update(connection, "INSERT OR IGNORE INTO main.score_store_migration VALUES (?)", LR2_MIGRATION) == 0) {
				return;
			}
			// The old database wrote the history and aggregate together. Replaying eligible rows
			// in order reconstructs the aggregate and also provides the new best-update log.
			try (var statement = connection.createStatement();
				 var rows = statement.executeQuery("SELECT * FROM legacy.play_history ORDER BY id")) {
				while (rows.next()) {
					save(connection, legacyAttempt(rows), rows.getInt("best_eligible") != 0, rows.getLong("played_at"));
				}
			}
		});
	}

	private static ScoreAttempt legacyAttempt(ResultSet row) throws SQLException {
		var counts = new ArrayList<Integer>(12);
		for (String column : JUDGMENT_COLUMNS) counts.add(row.getInt(column));
		try {
			return new ScoreAttempt(row.getString("sha256"), row.getInt("mode"),
					ScoreOutcome.valueOf(row.getString("outcome")), row.getInt("notes"), row.getInt("passed"),
					row.getInt("combo"), row.getInt("minbp"), ClearType.getClearTypeByID(row.getInt("clear")),
					row.getInt("gauge"), row.getFloat("gauge_value"), counts);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new SQLException("Invalid entry in legacy LR2 history", e);
		}
	}

	private void save(Connection connection, ScoreAttempt attempt, boolean allowed, long date) throws SQLException {
		boolean eligible = allowed && attempt.canUpdateBest();
		var score = attempt.toScoreData(null);
		score.setDate(date);
		if (eligible) {
			var best = scores.getScoreData(connection, score.getSha256(), score.getMode());
			if (best == null) {
				best = new ScoreData();
				best.setSha256(score.getSha256());
				best.setMode(score.getMode());
			}
			best.setNotes(score.getNotes());
			var diff = PlayDataAccessor.updateScore(best, score, score.getSha256(), true);
			best.setPlaycount(best.getPlaycount() + 1);
			if (score.getClear() > ClearType.Failed.id) best.setClearcount(best.getClearcount() + 1);
			best.setDate(Math.max(best.getDate(), date));
			best.setScorehash(PlayDataAccessor.getScoreHash(best));
			scores.setScoreData(connection, best);
			if (diff.getSha256() != null) {
				diff.setDate(date);
				diff.setMode(score.getMode());
				bestLog.setScoreLog(connection, diff);
			}
			score.setPlaycount(best.getPlaycount());
			score.setClearcount(best.getClearcount());
		}
		score.setScorehash(PlayDataAccessor.getScoreHash(score));
		history.setScoreDataLog(connection, score, attempt, eligible);
	}

	private void inTransaction(Path legacy, Transaction action) throws SQLException {
		try (var connection = source.getConnection()) {
			query.update(connection, "ATTACH DATABASE ? AS bestlog", bestLogPath.toString());
			query.update(connection, "ATTACH DATABASE ? AS history", historyPath.toString());
			if (legacy != null) {
				query.update(connection, "ATTACH DATABASE ? AS legacy", legacy.toUri().toASCIIString() + "?mode=ro");
			}
			// SQLite only guarantees an atomic commit across files with a disk-backed main
			// database and rollback journals, not WAL.
			for (String schema : new String[]{"main", "bestlog", "history"}) {
				var mode = query.query(connection, "PRAGMA " + schema + ".journal_mode=DELETE", new ScalarHandler<String>());
				if (!"delete".equalsIgnoreCase(mode)) throw new SQLException("Could not enable rollback journal for " + schema);
				query.update(connection, "PRAGMA " + schema + ".synchronous=FULL");
			}
			connection.setAutoCommit(false);
			try {
				action.run(connection);
				connection.commit();
			} catch (SQLException | RuntimeException e) {
				try {
					connection.rollback();
				} catch (SQLException rollback) {
					e.addSuppressed(rollback);
				}
				throw e;
			}
		}
	}

	@FunctionalInterface
	private interface Transaction {
		void run(Connection connection) throws SQLException;
	}
}
