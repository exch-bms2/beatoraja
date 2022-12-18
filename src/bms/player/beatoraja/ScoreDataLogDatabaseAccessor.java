package bms.player.beatoraja;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * スコアデータログデータベースアクセサ
 * 
 * @author omi
 */
public class ScoreDataLogDatabaseAccessor extends SQLiteDatabaseAccessor {

	private SQLiteDataSource ds;
	private final ResultSetHandler<List<ScoreData>> scoreHandler = new BeanListHandler<ScoreData>(ScoreData.class);

	private final QueryRunner qr;

	public ScoreDataLogDatabaseAccessor(String path) throws ClassNotFoundException {
		super(	new Table("scoredatalog",
						new Column("sha256", "TEXT", 1, 1),
						new Column("mode", "INTEGER",0,1),
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
						));

		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + path);
		qr = new QueryRunner(ds);
		
		try {
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
				this.insert(qr, con, "scoredatalog", score);
			}
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}
}
