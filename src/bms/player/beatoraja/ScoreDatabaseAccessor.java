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

import bms.player.beatoraja.song.SongData;

import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * スコアデータベースアクセサ
 * 
 * @author exch
 */
public class ScoreDatabaseAccessor {

	private final QueryRunner qr;
	
	private final ResultSetHandler<List<PlayerInformation>> infoHandler = new BeanListHandler<PlayerInformation>(PlayerInformation.class);
	private final ResultSetHandler<List<IRScoreData>> scoreHandler = new BeanListHandler<IRScoreData>(IRScoreData.class);
	private final ResultSetHandler<List<PlayerData>> playerHandler = new BeanListHandler<PlayerData>(PlayerData.class);

	public ScoreDatabaseAccessor(String path) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		SQLiteDataSource ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + path);
		qr = new QueryRunner(ds);
	}

	public void createTable() {
		try {
			final MapListHandler mh = new MapListHandler();
			String sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
			// infoテーブル作成(存在しない場合)
			if (qr.query(sql, mh, "info").size() == 0) {
				qr.update("CREATE TABLE [info] ([id] TEXT NOT NULL,[name] TEXT NOT NULL," + "[rank] TEXT, "
						+ "PRIMARY KEY(id));");
			}
			// playerテーブル作成(存在しない場合)
			if (qr.query(sql, mh, "player").size() == 0) {
				qr.update("CREATE TABLE [player] ([date] INTEGER,[playcount] INTEGER," + "[clear] INTEGER,"
						+ "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER," + "[lgr] INTEGER," + "[egd] INTEGER,"
						+ "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER," + "[epr] INTEGER," + "[lpr] INTEGER,"
						+ "[ems] INTEGER," + "[lms] INTEGER," + "[playtime] INTEGER," + "[combo] INTEGER,"
						+ "[maxCombo] INTEGER," + "[scorehash] TEXT," + "PRIMARY KEY(date));");

				qr.update(
						"insert into player "
								+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxCombo, "
								+ "scorehash) " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
			}
			// scoreテーブル作成(存在しない場合)
			if (qr.query(sql, mh, "score").size() == 0) {
				qr.update("CREATE TABLE [score] ([sha256] TEXT NOT NULL," + "[mode] INTEGER," + "[clear] INTEGER,"
						+ "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER," + "[lgr] INTEGER," + "[egd] INTEGER,"
						+ "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER," + "[epr] INTEGER," + "[lpr] INTEGER,"
						+ "[ems] INTEGER," + "[lms] INTEGER," + "[notes] INTEGER," + "[combo] INTEGER,"
						+ "[minBP] INTEGER," + "[playcount] INTEGER," + "[clearcount] INTEGER," + "[trophy] TEXT,"
						+ "[scorehash] TEXT," + "[option] INTEGER," + "[random] INTEGER," + "[date] INTEGER,"
						+ "[state] INTEGER," + "PRIMARY KEY(sha256, mode));");
			}			
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'score' AND sql LIKE '%trophy%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE score ADD COLUMN trophy [TEXT]");
			}

		} catch (SQLException e) {
			Logger.getGlobal().severe("スコアデータベース初期化中の例外:" + e.getMessage());
		}
	}
	
	public PlayerInformation getInformation() {
		try {
			List<PlayerInformation> info =  qr.query("SELECT * FROM info", infoHandler);
			if (info.size() > 0) {
				return info.get(0);
			}
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
		return null;
	}
	
	public void setInformation(PlayerInformation info) {
		try {
			qr.update("DELETE FROM info");
			qr.update("insert into info " + "(id, name, rank) " + "values(?,?,?);", info.getId(), info.getName(), info.getRank());
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
	}

	public IRScoreData getScoreData(String hash, int mode) {
		IRScoreData result = null;
		try {
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			List<IRScoreData> score;
			score = qr.query("SELECT * FROM score WHERE sha256 = '" + hash + "' AND mode = " + mode, rh);
			if (score.size() > 0) {
				IRScoreData sc = null;
				for (IRScoreData s : score) {
					if (sc == null || s.getClear() > sc.getClear()) {
						sc = s;
					}
				}
				result = sc;
			}
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
		return result;
	}

	/**
	 * プレイヤースコアデータを取得する
	 * 
	 * @param hashes
	 *            スコアを取得する楽曲のhash
	 * @return <ハッシュ, スコアデータ>のマップ
	 */
	public void getScoreDatas(ScoreDataCollector collector, SongData[] songs, int mode) {
		StringBuilder str = new StringBuilder(songs.length * 68);
		getScoreDatas(collector, songs, mode, str, true);
		str.setLength(0);
		getScoreDatas(collector, songs, 0, str, false);
	}
	
	private void getScoreDatas(ScoreDataCollector collector, SongData[] songs, int mode, StringBuilder str, boolean hasln) {
		try {
			for (SongData song : songs) {
				if((hasln && song.hasUndefinedLongNote()) || (!hasln && !song.hasUndefinedLongNote())) {
					if (str.length() > 0) {
						str.append(',');
					}
					str.append('\'').append(song.getSha256()).append('\'');					
				}
			}

			List<IRScoreData> scores = qr
					.query("SELECT * FROM score WHERE sha256 IN (" + str.toString() + ") AND mode = " + mode, scoreHandler);
			for(SongData song : songs) {
				if((hasln && song.hasUndefinedLongNote()) || (!hasln && !song.hasUndefinedLongNote())) {
					boolean b = true;
					for (IRScoreData score : scores) {
						if(song.getSha256().equals(score.getSha256())) {
							collector.collect(song, score);
							b = false;
							break;
						}
					}
					if(b) {
						collector.collect(song, null);					
					}					
				}
			}
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}		
	}

	public List<IRScoreData> getScoreDatas(String sql) {
		List<IRScoreData> score = null;
		try {
			score = qr.query("SELECT * FROM score WHERE " + sql, scoreHandler);
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
		return score;

	}

	public void setScoreData(IRScoreData score) {
		setScoreData(new IRScoreData[] { score });
	}

	public void setScoreData(IRScoreData[] scores) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			String sql = "INSERT OR REPLACE INTO score "
					+ "(sha256, mode, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, notes, combo, "
					+ "minBP, playcount, clearcount, trophy, scorehash, option, random, date, state)"
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
			for (IRScoreData score : scores) {
				qr.update(con, sql, score.getSha256(), score.getMode(), score.getClear(), score.getEpg(),
						score.getLpg(), score.getEgr(), score.getLgr(), score.getEgd(), score.getLgd(), score.getEbd(),
						score.getLbd(), score.getEpr(), score.getLpr(), score.getEms(), score.getLms(),
						score.getNotes(), score.getMaxCombo(), score.getMinBP(), score.getPlaycount(),
						score.getClearcount(), score.getTrophy(), score.getScorehash(), score.getOption(),
						score.getRandom(), score.getDate(), score.getState());
			}
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}

	public void setScoreData(Map<String, Map<String, Object>> map) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			for (String hash : map.keySet()) {
				Map<String, Object> values = map.get(hash);
				String vs = "";
				for (String key : values.keySet()) {
					vs += key + " = " + values.get(key) + ",";
				}
				if (vs.length() > 0) {
					vs = vs.substring(0, vs.length() - 1) + " ";
					qr.update(con, "UPDATE score SET " + vs + "WHERE sha256 = '" + hash + "'");
				}
			}
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}

	/**
	 * プレイヤーデータを取得する
	 * 
	 * @return プレイヤーデータ
	 */
	public PlayerData getPlayerData() {
		PlayerData[] pd = getPlayerDatas(1);
		if (pd.length > 0) {
			return pd[0];
		}
		return null;
	}

	public PlayerData[] getPlayerDatas(int count) {
		PlayerData[] result = null;
		try {
			List<PlayerData> pd = qr
					.query("SELECT * FROM player ORDER BY date DESC" + (count > 0 ? " limit " + count : ""), playerHandler);
			result = pd.toArray(new PlayerData[0]);
		} catch (Exception e) {
			Logger.getGlobal().severe("プレイヤーデータ取得時の例外:" + e.getMessage());
		}
		return result != null ? result : new PlayerData[0];
	}

	public void setPlayerData(PlayerData pd) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long unixtime = cal.getTimeInMillis() / 1000L;

			qr.update(con,
					"INSERT OR REPLACE INTO player "
							+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxCombo, "
							+ "scorehash) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
					unixtime, pd.getPlaycount(), pd.getClear(), pd.getEpg(), pd.getLpg(), pd.getEgr(), pd.getLgr(),
					pd.getEgd(), pd.getLgd(), pd.getEbd(), pd.getLbd(), pd.getEpr(), pd.getLpr(), pd.getEms(),
					pd.getLms(), pd.getPlaytime(), 0, 0, "");
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}
	
	public interface ScoreDataCollector {
		
		public void collect(SongData hash, IRScoreData score);
	}
}
