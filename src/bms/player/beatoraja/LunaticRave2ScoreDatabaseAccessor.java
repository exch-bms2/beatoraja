package bms.player.beatoraja;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.dbutils.QueryRunner;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/** Local history and personal bests for the experimental LunaticRave2 rule only. */
public final class LunaticRave2ScoreDatabaseAccessor {
	static final String FILE_NAME = "score-lunaticrave2.db";

	public enum Outcome { FINISHED, FAILED, INTERRUPTED, INPUT_LOST, ERROR }

	public record Attempt(String sha256, int mode, Outcome outcome, int notes, int passed, int exscore,
			int combo, int minbp, ClearType clear, int gauge, float gaugeValue, List<Integer> judgeCounts) {
		public Attempt {
			Objects.requireNonNull(sha256);
			Objects.requireNonNull(outcome);
			Objects.requireNonNull(clear);
			judgeCounts = List.copyOf(judgeCounts);
			if (sha256.length() != 64 || mode < 0 || mode > 2 || judgeCounts.size() != 12) {
				throw new IllegalArgumentException("Invalid LR2 score identity or judgment counts");
			}
		}

		boolean canUpdateBest() {
			return outcome == Outcome.FINISHED || outcome == Outcome.FAILED;
		}
	}

	public record Best(int clear, int exscore, int combo, int minbp, int playcount, int clearcount) {}

	private final QueryRunner query;

	LunaticRave2ScoreDatabaseAccessor(Path path) throws SQLException {
		var config = new SQLiteConfig();
		config.setBusyTimeout(1_000);
		var source = new SQLiteDataSource(config);
		source.setUrl("jdbc:sqlite:" + path);
		query = new QueryRunner(source);
		try (var connection = source.getConnection()) {
			query.update(connection, """
					CREATE TABLE IF NOT EXISTS score (
						sha256 TEXT NOT NULL, mode INTEGER NOT NULL, clear INTEGER NOT NULL,
						exscore INTEGER NOT NULL, combo INTEGER NOT NULL, minbp INTEGER NOT NULL,
						playcount INTEGER NOT NULL, clearcount INTEGER NOT NULL,
						PRIMARY KEY (sha256, mode)
					)
					""");
			query.update(connection, """
					CREATE TABLE IF NOT EXISTS play_history (
						id INTEGER PRIMARY KEY, sha256 TEXT NOT NULL, mode INTEGER NOT NULL,
						outcome TEXT NOT NULL, played_at INTEGER NOT NULL, best_eligible INTEGER NOT NULL,
						notes INTEGER NOT NULL, passed INTEGER NOT NULL, exscore INTEGER NOT NULL,
						combo INTEGER NOT NULL, minbp INTEGER NOT NULL, clear INTEGER NOT NULL,
						gauge INTEGER NOT NULL, gauge_value REAL NOT NULL,
						epg INTEGER NOT NULL, lpg INTEGER NOT NULL, egr INTEGER NOT NULL, lgr INTEGER NOT NULL,
						egd INTEGER NOT NULL, lgd INTEGER NOT NULL, ebd INTEGER NOT NULL, lbd INTEGER NOT NULL,
						epr INTEGER NOT NULL, lpr INTEGER NOT NULL, ems INTEGER NOT NULL, lms INTEGER NOT NULL
					)
					""");
			query.update(connection, "CREATE INDEX IF NOT EXISTS play_history_chart ON play_history (sha256, mode, id)");
		}
	}

	Best readBest(String sha256, int mode) throws SQLException {
		return query.query("""
				SELECT clear, exscore, combo, minbp, playcount, clearcount FROM score WHERE sha256 = ? AND mode = ?
				""", rs -> rs.next() ? new Best(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
				rs.getInt(5), rs.getInt(6)) : null, sha256, mode);
	}

	void write(Attempt attempt, boolean scoreAllowed) throws SQLException {
		boolean eligible = scoreAllowed && attempt.canUpdateBest();
		try (Connection connection = query.getDataSource().getConnection()) {
			connection.setAutoCommit(false);
			try {
				var c = attempt.judgeCounts();
				query.update(connection, """
						INSERT INTO play_history (sha256, mode, outcome, played_at, best_eligible, notes, passed, exscore,
							combo, minbp, clear, gauge, gauge_value, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms)
						VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
						""", attempt.sha256(), attempt.mode(), attempt.outcome().name(), System.currentTimeMillis() / 1000,
						eligible ? 1 : 0, attempt.notes(), attempt.passed(), attempt.exscore(), attempt.combo(), attempt.minbp(),
						attempt.clear().id, attempt.gauge(), attempt.gaugeValue(),
						c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5),
						c.get(6), c.get(7), c.get(8), c.get(9), c.get(10), c.get(11));
				if (eligible) {
					query.update(connection, """
							INSERT INTO score (sha256, mode, clear, exscore, combo, minbp, playcount, clearcount)
							VALUES (?, ?, ?, ?, ?, ?, 1, ?)
							ON CONFLICT (sha256, mode) DO UPDATE SET
								clear = MAX(score.clear, excluded.clear), exscore = MAX(score.exscore, excluded.exscore),
								combo = MAX(score.combo, excluded.combo), minbp = MIN(score.minbp, excluded.minbp),
								playcount = score.playcount + 1, clearcount = score.clearcount + excluded.clearcount
							""", attempt.sha256(), attempt.mode(), attempt.clear().id, attempt.exscore(), attempt.combo(),
							attempt.minbp(), attempt.clear().id > ClearType.Failed.id ? 1 : 0);
				}
				connection.commit();
			} catch (SQLException e) {
				try {
					connection.rollback();
				} catch (SQLException rollback) {
					e.addSuppressed(rollback);
				}
				throw e;
			}
		}
	}
}
