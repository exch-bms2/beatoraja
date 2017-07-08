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
 * スコアデータベースアクセサ
 * 
 * @author exch
 */
public class ScoreDatabaseAccessor {

	private final QueryRunner qr;

	public ScoreDatabaseAccessor(String player) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		SQLiteDataSource ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:player/" + player + "/score.db");
		qr = new QueryRunner(ds);
	}

	public void createTable() {
		try {
			String sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
			// playerテーブル作成(存在しない場合)
			if (qr.query(sql, new MapListHandler(), "player").size() == 0) {
				qr.update("CREATE TABLE [player] ([date] INTEGER,[playcount] INTEGER," + "[clear] INTEGER,"
						+ "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER," + "[lgr] INTEGER," + "[egd] INTEGER,"
						+ "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER," + "[epr] INTEGER," + "[lpr] INTEGER,"
						+ "[ems] INTEGER," + "[lms] INTEGER," + "[playtime] INTEGER," + "[combo] INTEGER,"
						+ "[maxcombo] INTEGER," + "[scorehash] TEXT," + "PRIMARY KEY(date));");

				qr.update(
						"insert into player "
								+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxcombo, "
								+ "scorehash) " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
			}
			// scoreテーブル作成(存在しない場合)
			if (qr.query(sql, new MapListHandler(), "score").size() == 0) {
				qr.update("CREATE TABLE [score] ([sha256] TEXT NOT NULL," + "[mode] INTEGER," + "[clear] INTEGER,"
						+ "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER," + "[lgr] INTEGER," + "[egd] INTEGER,"
						+ "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER," + "[epr] INTEGER," + "[lpr] INTEGER,"
						+ "[ems] INTEGER," + "[lms] INTEGER," + "[notes] INTEGER," + "[combo] INTEGER,"
						+ "[minbp] INTEGER," + "[playcount] INTEGER," + "[clearcount] INTEGER," + "[history] INTEGER,"
						+ "[scorehash] TEXT," + "[option] INTEGER," + "[random] INTEGER," + "[date] INTEGER,"
						+ "[state] INTEGER," + "PRIMARY KEY(sha256, mode));");
			}
		} catch (SQLException e) {
			Logger.getGlobal().severe("スコアデータベース初期化中の例外:" + e.getMessage());
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
	public Map<String, IRScoreData> getScoreDatas(String[] hashes, int mode) {
		Map<String, IRScoreData> result = new HashMap<String, IRScoreData>();
		try {
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			StringBuilder str = new StringBuilder();
			for (String hash : hashes) {
				if (str.length() > 0) {
					str.append(',');
				}
				str.append('\'').append(hash).append('\'');
			}

			List<IRScoreData> scores = qr
					.query("SELECT * FROM score WHERE sha256 IN (" + str.toString() + ") AND mode = " + mode, rh);
			for (IRScoreData score : scores) {
				result.put(score.getSha256(), score);
			}
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
		return result;
	}

	public List<IRScoreData> getScoreDatas(String sql) {
		List<IRScoreData> score = null;
		try {
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			score = qr.query("SELECT * FROM score WHERE " + sql, rh);
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
					+ "minbp, playcount, clearcount, history, scorehash, option, random, date, state)"
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
			for (IRScoreData score : scores) {
				qr.update(con, sql, score.getSha256(), score.getMode(), score.getClear(), score.getEpg(),
						score.getLpg(), score.getEgr(), score.getLgr(), score.getEgd(), score.getLgd(), score.getEbd(),
						score.getLbd(), score.getEpr(), score.getLpr(), score.getEms(), score.getLms(),
						score.getNotes(), score.getCombo(), score.getMinbp(), score.getPlaycount(),
						score.getClearcount(), score.getHistory(), score.getScorehash(), score.getOption(),
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
			ResultSetHandler<List<PlayerData>> rh = new BeanListHandler<PlayerData>(PlayerData.class);
			List<PlayerData> pd = qr
					.query("SELECT * FROM player ORDER BY date DESC" + (count > 0 ? " limit " + count : ""), rh);
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
							+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxcombo, "
							+ "scorehash) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
					unixtime, pd.getPlaycount(), pd.getClear(), pd.getEpg(), pd.getLpg(), pd.getEgr(), pd.getLgr(),
					pd.getEgd(), pd.getLgd(), pd.getEbd(), pd.getLbd(), pd.getEpr(), pd.getLpr(), pd.getEms(),
					pd.getLms(), pd.getPlaytime(), 0, 0, "");
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}
}
