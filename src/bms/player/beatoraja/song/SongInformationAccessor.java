package bms.player.beatoraja.song;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.Validatable;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import bms.model.BMSModel;

import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * 楽曲情報データベースへのアクセスクラス
 * 
 * @author exch
 */
public class SongInformationAccessor {

	private SQLiteDataSource ds;

	private final ResultSetHandler<List<SongInformation>> songhandler = new BeanListHandler<SongInformation>(
			SongInformation.class);

	private final QueryRunner qr;

	private Connection conn;

	public SongInformationAccessor(String filepath) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + filepath);
		qr = new QueryRunner(ds);
		createTable();
	}

	/**
	 * 楽曲データベースを初期テーブルを作成する。 すでに初期テーブルを作成している場合は何もしない。
	 */
	private void createTable() {
		try {
			// songテーブル作成(存在しない場合)
			if (qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';", new MapListHandler(),
					"information").size() == 0) {
				qr.update("CREATE TABLE [information] (" + "[sha256] TEXT NOT NULL," + "[n] INTEGER," + "[ln] INTEGER,"
						+ "[s] INTEGER," + "[ls] INTEGER," + "[total] REAL," + "[density] REAL," + "[peakdensity] REAL," + "[enddensity] REAL," + "[distribution] TEXT," + "PRIMARY KEY(sha256));");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'information' AND sql LIKE '%peakdensity%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE information ADD COLUMN peakdensity [REAL]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'information' AND sql LIKE '%enddensity%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE information ADD COLUMN enddensity [REAL]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'information' AND sql LIKE '%mainbpm%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE information ADD COLUMN mainbpm [REAL]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'information' AND sql LIKE '%speedchange%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE information ADD COLUMN speedchange [TEXT]");
			}

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

	public void startUpdate() {
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			conn = null;
		}
	}

	public void update(BMSModel model) {
		SongInformation info = new SongInformation(model);
		try {
			qr.update(conn,
					"INSERT OR REPLACE INTO information (sha256, n, ln, s, ls, total, density, peakdensity, enddensity, mainbpm, distribution, speedchange)"
							+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?);",
					model.getSHA256(), info.getN(), info.getLn(), info.getS(), info.getLs(), info.getTotal(), info.getDensity(), info.getPeakdensity(), info.getEnddensity(), info.getMainbpm(), info.getDistribution(), info.getSpeedchange());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void endUpdate() {
		if (conn != null) {
			try {
				conn.commit();
				conn.close();
			} catch (SQLException e) {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
				conn = null;
			}
		}
	}

}
