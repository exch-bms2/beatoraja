package bms.player.beatoraja;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

public class ScoreDatabaseAccessor {

	private String rootpath;

	private String playerpath = "/LR2files/Database/Score/";

	private String rivalpath = "/LR2files/Rival/";

	private final QueryRunner qr = new QueryRunner();

	public ScoreDatabaseAccessor(String path) throws ClassNotFoundException {
		rootpath = path;
	}

	public ScoreDatabaseAccessor(String path, String player, String rival) throws ClassNotFoundException {
		rootpath = path;
		this.playerpath = player;
		this.rivalpath = rival;
	}

	public void createTable(String playername) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");

			String sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
			// playerテーブル作成(存在しない場合)
			if (qr.query(conn, sql, new MapListHandler(), "player").size() == 0) {
				qr.update(conn, "CREATE TABLE [player] ([date] INTEGER,[playcount] INTEGER," + "[clear] INTEGER,"
						+ "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER," + "[lgr] INTEGER," + "[egd] INTEGER,"
						+ "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER," + "[epr] INTEGER," + "[lpr] INTEGER,"
						+ "[ems] INTEGER," + "[lms] INTEGER," + "[playtime] INTEGER," + "[combo] INTEGER,"
						+ "[maxcombo] INTEGER," + "[scorehash] TEXT," + "PRIMARY KEY(date));");

				qr.update(
						conn,
						"insert into player "
								+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxcombo, "
								+ "scorehash) " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);", 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
			}
			// scoreテーブル作成(存在しない場合)
			if (qr.query(conn, sql, new MapListHandler(), "score").size() == 0) {
				qr.update(conn, "CREATE TABLE [score] ([sha256] TEXT NOT NULL," + "[mode] INTEGER,"
						+ "[clear] INTEGER," + "[epg] INTEGER," + "[lpg] INTEGER," + "[egr] INTEGER,"
						+ "[lgr] INTEGER," + "[egd] INTEGER," + "[lgd] INTEGER," + "[ebd] INTEGER," + "[lbd] INTEGER,"
						+ "[epr] INTEGER," + "[lpr] INTEGER," + "[ems] INTEGER," + "[lms] INTEGER,"
						+ "[notes] INTEGER," + "[combo] INTEGER," + "[minbp] INTEGER," + "[playcount] INTEGER,"
						+ "[clearcount] INTEGER," + "[history] INTEGER," + "[scorehash] TEXT," + "[option] INTEGER,"
						+ "[random] INTEGER," + "[date] INTEGER," + "[state] INTEGER," + "PRIMARY KEY(sha256, mode));");
			}
		} catch (SQLException e) {
			Logger.getGlobal().severe("スコアデータベース初期化中の例外:" + e.getMessage());
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					// conn.rollback();
					conn.close();
				}
			} catch (SQLException e) {
				// どうしようもない
				Logger.getGlobal().severe("スコアデータベース初期化中の例外:" + e.getMessage());
			}
		}
	}

	// /**
	// * ライバルスコアを最新状態にする
	// *
	// * @param rivalId
	// * ライバルID
	// * @param scores
	// * スコアデータ
	// * @return 更新スコアデータ数
	// * @author KASAKON
	// */
	// public int updateRivalData(String rivalId, List<IRScoreData> scores) {
	// int num = 0;
	// try {
	// Connection con = DriverManager.getConnection("jdbc:sqlite:" + rootpath +
	// rivalpath + rivalId + ".db");
	// con.setAutoCommit(false);
	// Statement stmt = con.createStatement();
	//
	// for (IRScoreData score : scores) {
	// // ハッシュが存在し、スコアの更新があったか？
	// String sql = "SELECT hash FROM rival WHERE hash = '" + score.getHash() +
	// "' and (r_clear != "
	// + score.getClear() + " or r_maxcombo != " + score.getCombo() +
	// " or r_perfect != "
	// + score.getPg() + " or r_great != " + score.getGr() + " or r_minbp != " +
	// score.getMinbp()
	// + ");";
	// ResultSet rs = stmt.executeQuery(sql);
	// boolean isUpdate = false;
	// boolean isExist = false;
	//
	// isUpdate = rs.next();
	// if (isUpdate == true) {
	// // 更新処理
	// sql = "UPDATE rival SET r_clear = " + score.getClear() +
	// ", r_maxcombo = " + score.getCombo()
	// + ", r_perfect = " + score.getPg() + ", r_great = " + score.getGr() +
	// ", r_good = "
	// + score.getGd() + ", r_bad = " + score.getBd() + ", r_poor = " +
	// score.getPr()
	// + ", r_minbp = " + score.getMinbp() + ", r_option = " + score.getOption()
	// + ", r_lastupdate = " + score.getLastupdate() + " WHERE hash = '" +
	// score.getHash() + "';";
	// stmt.executeUpdate(sql);
	// num++;
	// } else {
	// // ハッシュが存在するか？
	// sql = "SELECT hash FROM rival WHERE hash = '" + score.getHash() + "';";
	// rs = stmt.executeQuery(sql);
	// isExist = rs.next();
	// if (isExist == false) {
	// // 新規追加処理
	// sql = "INSERT INTO rival VALUES ('" + score.getHash() + "'," +
	// score.getClear() + ","
	// + score.getNotes() + "," + score.getCombo() + "," + score.getPg() + "," +
	// score.getGr()
	// + "," + score.getGd() + "," + score.getBd() + "," + score.getPr() + ","
	// + score.getMinbp() + "," + score.getOption() + "," +
	// score.getLastupdate() + ");";
	// stmt.executeUpdate(sql);
	// num++;
	// }
	// }
	// }
	// con.commit();
	// // クローズ処理
	// stmt.close();
	// con.close();
	// } catch (Exception e) {
	// Logger.getGlobal().severe("ライバルデータ更新時の例外:" + e.getMessage());
	// }
	// return num;
	// }

	public IRScoreData getScoreData(String playername, String hash, int mode) {
		Connection con = null;
		IRScoreData result = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			List<IRScoreData> score;
			score = qr.query(con, "SELECT * FROM score WHERE sha256 = '" + hash + "' AND mode = " + mode, rh);
			if (score.size() > 0) {
				IRScoreData sc = null;
				for (IRScoreData s : score) {
					if (sc == null || s.getClear() > sc.getClear()) {
						sc = s;
					}
				}
				result = sc;
			}
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "取得時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
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
	public Map<String, IRScoreData> getScoreDatas(String playername, String[] hashes, int mode) {
		Map<String, IRScoreData> result = new HashMap<String, IRScoreData>();
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			StringBuilder str = new StringBuilder();
			for (String hash : hashes) {
				if (str.length() > 0) {
					str.append(',');
				}
				str.append('\'').append(hash).append('\'');
			}

			List<IRScoreData> scores = qr.query(con, "SELECT * FROM score WHERE sha256 IN (" + str.toString()
					+ ") AND mode = " + mode, rh);
			for (IRScoreData score : scores) {
				result.put(score.getSha256(), score);
			}
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "取得時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
		return result;
	}

	public List<IRScoreData> getScoreDatas(String playername, String sql) {
		Connection con = null;
		List<IRScoreData> score = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			score = qr
					.query(con,
							"SELECT * FROM score WHERE " + sql
							, rh);
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "取得時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
		return score;

	}

	public void setScoreData(String playername, IRScoreData score) {
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			con.setAutoCommit(false);
			String sql = "INSERT OR REPLACE INTO score "
					+ "(sha256, mode, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, notes, combo, "
					+ "minbp, playcount, clearcount, history, scorehash, option, random, date, state)"
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
			qr.update(con, sql, score.getSha256(), score.getMode(), score.getClear(), score.getEpg(), score.getLpg(),
					score.getEgr(), score.getLgr(), score.getEgd(), score.getLgd(), score.getEbd(), score.getLbd(),
					score.getEpr(), score.getLpr(), score.getEms(), score.getLms(), score.getNotes(), score.getCombo(),
					score.getMinbp(), score.getPlaycount(), score.getClearcount(), score.getHistory(),
					score.getScorehash(), score.getOption(), score.getRandom(), score.getDate(), score.getState());
			con.commit();
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "更新時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	public void setScoreData(String playername, Map<String, Map<String, Object>> map) {
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
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
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "更新時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	/**
	 * ライバルスコアデータを取得する
	 * 
	 * @param hashes
	 *            スコアを取得する楽曲のhash
	 * @return <ハッシュ, スコアデータ>のマップ
	 */
	public Map<String, IRScoreData> getRivalScoreDatas(String rivalname, String[] hashes) {
		Map<String, IRScoreData> result = new HashMap<String, IRScoreData>();
		Connection con = null;
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + rivalpath + rivalname + ".db");
			ResultSetHandler<List<IRScoreData>> rh = new BeanListHandler<IRScoreData>(IRScoreData.class);
			for (String hash : hashes) {
				List<IRScoreData> score = (List<IRScoreData>) qr.query(con,
						"SELECT r_clear as clear, CAST((r_perfect * 2 + r_great) * 9 / (r_totalnotes * 2) as int) as rank, "
								+ "r_perfect as pg,  r_great as gr, r_minbp as minbp FROM rival WHERE hash = '" + hash
								+ "'", rh);
				if (score.size() > 0) {
					result.put(hash, score.get(0));
				} else {
					result.put(hash, null);
				}
			}
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("ライバルスコア" + rivalname + "取得時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
		return result;
	}

	/**
	 * プレイヤーデータを取得する
	 * 
	 * @return プレイヤーデータ
	 */
	public PlayerData getPlayerData(String playername) {
		PlayerData[] pd = getPlayerDatas(playername, 1);
		if (pd.length > 0) {
			return pd[0];
		}
		return null;
	}

	public PlayerData[] getPlayerDatas(String playername, int count) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			ResultSetHandler<List<PlayerData>> rh = new BeanListHandler<PlayerData>(PlayerData.class);
			List<PlayerData> pd = qr.query(conn, "SELECT * FROM player ORDER BY date DESC"
					+ (count > 0 ? " limit " + count : ""), rh);
			conn.close();
			return pd.toArray(new PlayerData[0]);
		} catch (Exception e) {
			Logger.getGlobal().severe("プレイヤーデータ" + playername + "取得時の例外:" + e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return new PlayerData[0];
	}

	public void setPlayerData(String playername, PlayerData pd) {
		Connection con = null;
		PlayerData lpd = getPlayerData(playername);
		try {
			con = DriverManager.getConnection("jdbc:sqlite:" + rootpath + playerpath + playername + ".db");
			con.setAutoCommit(false);
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long unixtime = cal.getTimeInMillis() / 1000L;

			qr.update(
					con,
					"INSERT OR REPLACE INTO player "
							+ "(date, playcount, clear, epg, lpg, egr, lgr, egd, lgd, ebd, lbd, epr, lpr, ems, lms, playtime, combo, maxcombo, "
							+ "scorehash) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);", unixtime,
					pd.getPlaycount(), pd.getClear(), pd.getEpg(), pd.getLpg(), pd.getEgr(), pd.getLgr(),
					pd.getEgd(), pd.getLgd(), pd.getEbd(), pd.getLbd(), pd.getEpr(), pd.getLpr(), pd.getEms(),
					pd.getLms(), pd.getPlaytime(), 0, 0, "");
			con.commit();
			con.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア" + playername + "更新時の例外:" + e.getMessage());
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
				}
			}
		}
	}

}
