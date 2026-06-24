package bms.player.beatoraja.song;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

import bms.player.beatoraja.SQLiteDatabaseAccessor;

/**
 * ユーザーの楽曲評価データベースへのアクセスクラス
 * 
 * @author exch
 */
public class SongReviewAccessor extends SQLiteDatabaseAccessor {

	private final SQLiteDataSource ds;

	private final ResultSetHandler<List<SongReview>> songhandler = new BeanListHandler<SongReview>(
			SongReview.class);

	private final QueryRunner qr;

	private Connection conn;

	public SongReviewAccessor(String filepath) throws ClassNotFoundException {
		super(new Table("review", 
				new Column("sha256", "TEXT",1,1),
				new Column("tag", "TEXT"),
				new Column("favorite", "INTEGER"),
				new Column("level", "REAL"),
				new Column("comment", "TEXT")
				));
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + filepath);
		qr = new QueryRunner(ds);
		try {
			validate(qr);
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
		}
	}


}
