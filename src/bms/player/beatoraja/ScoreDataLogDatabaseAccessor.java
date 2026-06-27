package bms.player.beatoraja;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * スコアデータログデータベースアクセサ
 * 
 * @author omi
 */
public class ScoreDataLogDatabaseAccessor extends SQLiteDatabaseAccessor {

	private static final String TABLE_NAME = "scoredatalog";
	private static final Column[] SCORE_DATA_LOG_COLUMNS = {
			new Column("sha256", "TEXT", 1, 0),
			new Column("mode", "INTEGER"),
			new Column("clear", "INTEGER"),
			new Column("epg", "INTEGER"),
			new Column("lpg", "INTEGER"),
			new Column("egr", "INTEGER"),
			new Column("lgr", "INTEGER"),
			new Column("egd", "INTEGER"),
			new Column("lgd", "INTEGER"),
			new Column("ebd", "INTEGER"),
			new Column("lbd", "INTEGER"),
			new Column("epr", "INTEGER"),
			new Column("lpr", "INTEGER"),
			new Column("ems", "INTEGER"),
			new Column("lms", "INTEGER"),
			new Column("notes", "INTEGER"),
			new Column("combo", "INTEGER"),
			new Column("minbp", "INTEGER"),
			new Column("avgjudge", "INTEGER", 1, 0, String.valueOf(Integer.MAX_VALUE)),
			new Column("playcount", "INTEGER"),
			new Column("clearcount", "INTEGER"),
			new Column("trophy", "TEXT"),
			new Column("ghost", "TEXT"),
			new Column("option", "INTEGER"),
			new Column("seed", "INTEGER"),
			new Column("random", "INTEGER"),
			new Column("date", "INTEGER"),
			new Column("state", "INTEGER"),
			new Column("scorehash", "TEXT")
	};

	private SQLiteDataSource ds;
	private final ResultSetHandler<List<Column>> columnHandler = new BeanListHandler<Column>(Column.class);

	private final QueryRunner qr;

	public ScoreDataLogDatabaseAccessor(String path) throws ClassNotFoundException {
		super(new Table(TABLE_NAME, SCORE_DATA_LOG_COLUMNS));

		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + path);
		qr = new QueryRunner(ds);
		
		try {
			migrateLegacyPrimaryKey();
			this.validate(qr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setScoreDataLog(ScoreData score) {
		setScoreDataLog(new ScoreData[] { score });
	}

	public void setScoreDataLog(ScoreData[] scores) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			for (ScoreData score : scores) {
				this.insert(qr, con, TABLE_NAME, score);
			}
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}

	private void migrateLegacyPrimaryKey() throws SQLException {
		if (!existsTable(TABLE_NAME)) {
			return;
		}

		List<Column> columns = qr.query("PRAGMA table_info('" + TABLE_NAME + "');", columnHandler);
		if (columns.stream().noneMatch(column -> column.getPk() == 1)) {
			return;
		}

		String legacyTableName = TABLE_NAME + "_legacy_pk_" + System.currentTimeMillis();
		Logger.getGlobal().info("scoredatalog.dbの旧PRIMARY KEYを解除します : " + legacyTableName);

		qr.update("ALTER TABLE " + TABLE_NAME + " RENAME TO " + legacyTableName);
		this.validate(qr);

		Set<String> legacyColumnNames = new HashSet<String>();
		for (Column column : qr.query("PRAGMA table_info('" + legacyTableName + "');", columnHandler)) {
			legacyColumnNames.add(column.getName());
		}

		StringBuilder columnNames = new StringBuilder();
		boolean comma = false;
		for (Column column : SCORE_DATA_LOG_COLUMNS) {
			if (legacyColumnNames.contains(column.getName())) {
				columnNames.append(comma ? "," : "").append(column.getName());
				comma = true;
			}
		}

		if (columnNames.length() > 0) {
			qr.update("INSERT INTO " + TABLE_NAME + " (" + columnNames + ") SELECT " + columnNames + " FROM " + legacyTableName);
		}
		qr.update("DROP TABLE " + legacyTableName);
	}

	private boolean existsTable(String tableName) throws SQLException {
		return qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';",
				new MapListHandler(), tableName).size() > 0;
	}
}
