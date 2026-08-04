package bms.player.beatoraja.song;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.Validatable;
import bms.player.beatoraja.SQLiteDatabaseAccessor;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import bms.model.BMSModel;

import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * 楽曲情報データベースへのアクセスクラス
 * 
 * @author exch
 */
public class SongInformationAccessor extends SQLiteDatabaseAccessor {

	private final SQLiteDataSource ds;

	private final ResultSetHandler<List<SongInformation>> songhandler = new BeanListHandler<SongInformation>(
			SongInformation.class);

	private final QueryRunner qr;

	private Connection conn;

	public SongInformationAccessor(String filepath) throws ClassNotFoundException {
		super(new Table("information", 
				new Column("sha256", "TEXT",1,1),
				new Column("n", "INTEGER"),
				new Column("ln", "INTEGER"),
				new Column("s", "INTEGER"),
				new Column("ls", "INTEGER"),
				new Column("total", "REAL"),
				new Column("density", "REAL"),
				new Column("peakdensity", "REAL"),
				new Column("enddensity", "REAL"),
				new Column("mainbpm", "REAL"),
				new Column("distribution", "TEXT"),
				new Column("speedchange", "TEXT"),
				new Column("lanenotes", "TEXT")
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

	public SongInformation[] getInformations(String sql) {
		try {
			List<SongInformation> m = Validatable.removeInvalidElements(qr.query("SELECT * FROM information WHERE " + sql, songhandler));
			return m.toArray(new SongInformation[m.size()]);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new SongInformation[0];		
	}

	public SongInformation getInformation(String sha256) {
		try {
			List<SongInformation> m = Validatable.removeInvalidElements(qr.query("SELECT * FROM information WHERE sha256 = ?", songhandler, sha256));
			if(m.size() > 0) {
				return m.get(0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void getInformation(SongData[] songs) {
		try {
			StringBuilder str = new StringBuilder(songs.length * 64);
			for (SongData song : songs) {
				if(song.getSha256() != null) {
					if (str.length() > 0) {
						str.append(',');
					}
					str.append('\'').append(song.getSha256()).append('\'');
				}
			}

			List<SongInformation> infos = Validatable.removeInvalidElements(qr
					.query("SELECT * FROM information WHERE sha256 IN (" + str.toString() + ")", songhandler));
			for(SongData song : songs) {
				for(SongInformation info : infos) {
					if(info.getSha256().equals(song.getSha256())) {
						song.setInformation(info);
						break;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean startUpdate() {
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			return true;
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			conn = null;
			Logger.getGlobal().warning("楽曲情報データベース更新を開始できません : " + e.getMessage());
			return false;
		}
	}

	public void update(BMSModel model) {
		if (conn == null) {
			return;
		}
		try {
			SongInformation info = new SongInformation(model);
			insert(qr, conn, "information", info);
		} catch (SQLException | RuntimeException e) {
			Logger.getGlobal().warning("楽曲情報の更新をスキップしました : " + model.getPath() + " : " + e.getMessage());
		}
	}

	public void endUpdate() {
		Connection current = conn;
		conn = null;
		if (current != null) {
			try {
				current.commit();
			} catch (SQLException e) {
				Logger.getGlobal().warning("楽曲情報データベース更新の確定に失敗しました : " + e.getMessage());
			} finally {
				try {
					current.close();
				} catch (SQLException e) {
					Logger.getGlobal().warning("楽曲情報データベースを閉じられません : " + e.getMessage());
				}
			}
		}
	}

}
