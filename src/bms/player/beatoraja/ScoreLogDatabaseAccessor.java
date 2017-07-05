package bms.player.beatoraja;

import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

public class ScoreLogDatabaseAccessor {

	private SQLiteDataSource ds;

	private final QueryRunner qr;

	public ScoreLogDatabaseAccessor(String path) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + path);
		qr = new QueryRunner(ds);
		createTable();		
	}

	public void createTable() {
		String sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
		// scorelogテーブル作成(存在しない場合)
		try {
			if (qr.query(sql, new MapListHandler(), "scorelog").size() == 0) {
				qr.update("CREATE TABLE [scorelog] ([sha256] TEXT NOT NULL," + "[mode] INTEGER," + "[clear] INTEGER," + "[oldclear] INTEGER,"
						+ "[score] INTEGER," + "[oldscore] INTEGER," + "[combo] INTEGER,"  + "[oldcombo] INTEGER," 
						+ "[minbp] INTEGER," + "[oldminbp] INTEGER," + "[date] INTEGER);");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setScoreLog(ScoreLog log) {
		try {
			qr.update(
					"INSERT INTO scorelog "
							+ "(sha256, mode, clear, oldclear, score, oldscore, combo, oldcombo, minbp, oldminbp, date) "
							+ "VALUES(?,?,?,?,?,?,?,?,?,?,?);", log.getSha256(), log.getMode(), log.getClear(), log.getOldclear(),
							log.getScore(), log.getOldscore(), log.getCombo(), log.getOldcombo(), log.getMinbp(), log.getOldminbp()
							,log.getDate());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class ScoreLog {
		
		private String sha256;
		private int mode;		
		private int clear;
		private int oldclear;
		private int score;		
		private int oldscore;		
		private int combo;		
		private int oldcombo;		
		private int minbp;		
		private int oldminbp;
		private long date;
		
		public String getSha256() {
			return sha256;
		}
		public void setSha256(String sha256) {
			this.sha256 = sha256;
		}
		public int getMode() {
			return mode;
		}
		public void setMode(int mode) {
			this.mode = mode;
		}
		public int getClear() {
			return clear;
		}
		public void setClear(int clear) {
			this.clear = clear;
		}
		public int getOldclear() {
			return oldclear;
		}
		public void setOldclear(int oldclear) {
			this.oldclear = oldclear;
		}
		public int getScore() {
			return score;
		}
		public void setScore(int score) {
			this.score = score;
		}
		public int getOldscore() {
			return oldscore;
		}
		public void setOldscore(int oldscore) {
			this.oldscore = oldscore;
		}
		public int getCombo() {
			return combo;
		}
		public void setCombo(int combo) {
			this.combo = combo;
		}
		public int getOldcombo() {
			return oldcombo;
		}
		public void setOldcombo(int oldcombo) {
			this.oldcombo = oldcombo;
		}
		public int getMinbp() {
			return minbp;
		}
		public void setMinbp(int minbp) {
			this.minbp = minbp;
		}
		public int getOldminbp() {
			return oldminbp;
		}
		public void setOldminbp(int oldminbp) {
			this.oldminbp = oldminbp;
		}
		public long getDate() {
			return date;
		}
		public void setDate(long date) {
			this.date = date;
		}
		
	}
}
