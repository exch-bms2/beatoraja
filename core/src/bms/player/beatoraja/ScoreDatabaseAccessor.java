package bms.player.beatoraja;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import bms.player.beatoraja.song.SongData;

import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * スコアデータベースアクセサ
 * 
 * @author exch
 */
public class ScoreDatabaseAccessor extends SQLiteDatabaseAccessor {

	private final QueryRunner qr;
	
	private final ResultSetHandler<List<PlayerInformation>> infoHandler = new BeanListHandler<PlayerInformation>(PlayerInformation.class);
	private final ResultSetHandler<List<ScoreData>> scoreHandler = new BeanListHandler<ScoreData>(ScoreData.class);
	private final ResultSetHandler<List<PlayerData>> playerHandler = new BeanListHandler<PlayerData>(PlayerData.class);

	public ScoreDatabaseAccessor(String path) throws ClassNotFoundException {
		super(new Table("info", 
				new Column("id", "TEXT",1,1),
				new Column("name", "TEXT",1,0),
				new Column("rank", "TEXT")
				),
				new Table("player", 
						new Column("date", "INTEGER",0,1),
						new Column("playcount", "INTEGER"),
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
						new Column("playtime", "INTEGER"),
						new Column("maxcombo", "INTEGER")
						),
				new Table("score",
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
		SQLiteDataSource ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + path);
		qr = new QueryRunner(ds);
	}

	public void createTable() {
		try {
			validate(qr);
			if(this.getPlayerDatas(1).length == 0) {
				this.insert(qr, "player", new PlayerData());
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
			insert(qr, "info", info);
//			qr.update("insert into info " + "(id, name, rank) " + "values(?,?,?);", info.getId(), info.getName(), info.getRank());
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
	}

	public ScoreData getScoreData(String hash, int mode) {
		ScoreData result = null;
		try {
			List<ScoreData> score = Validatable.removeInvalidElements(qr.query("SELECT * FROM score WHERE sha256 = '" + hash + "' AND mode = " + mode, scoreHandler));
			if (score.size() > 0) {
				ScoreData sc = null;
				for (ScoreData s : score) {
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

			List<ScoreData> scores = Validatable.removeInvalidElements(qr
					.query("SELECT * FROM score WHERE sha256 IN (" + str.toString() + ") AND mode = " + mode, scoreHandler));
			for(SongData song : songs) {
				if((hasln && song.hasUndefinedLongNote()) || (!hasln && !song.hasUndefinedLongNote())) {
					boolean b = true;
					for (ScoreData score : scores) {
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

	public List<ScoreData> getScoreDatas(String sql) {
		List<ScoreData> score = null;
		try {
			score = Validatable.removeInvalidElements(qr.query("SELECT * FROM score WHERE " + sql, scoreHandler));
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア取得時の例外:" + e.getMessage());
		}
		return score;

	}

	public void setScoreData(ScoreData score) {
		setScoreData(new ScoreData[] { score });
	}

	public void setScoreData(ScoreData[] scores) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			for (ScoreData score : scores) {
				this.insert(qr, con, "score", score);
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

	public void deleteScoreData(String sha256, int mode) {
		try {
			qr.update("DELETE FROM score WHERE sha256 = ? and mode = ?", sha256, mode);
		} catch (SQLException e) {
			e.printStackTrace();
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

	/**
	 * プレイヤーデータを設定する
	 * 
	 * @param pd プレイヤーデータ
	 */
	public void setPlayerData(PlayerData pd) {
		try (Connection con = qr.getDataSource().getConnection()) {
			con.setAutoCommit(false);
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long unixtime = cal.getTimeInMillis() / 1000L;
			pd.setDate(unixtime);
			this.insert(qr, con, "player", pd);
			con.commit();
		} catch (Exception e) {
			Logger.getGlobal().severe("スコア更新時の例外:" + e.getMessage());
		}
	}
	
	public interface ScoreDataCollector {
		
		public void collect(SongData hash, ScoreData score);
	}
}
