package bms.player.beatoraja;

import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig.SynchronousMode;

/**
 * スコアログデータベースアクセサ
 * 
 * @author exch
 */
public class ScoreLogDatabaseAccessor extends SQLiteDatabaseAccessor {

	private SQLiteDataSource ds;

	private final QueryRunner qr;

	public ScoreLogDatabaseAccessor(String path) throws ClassNotFoundException {
		super(	new Table("scorelog",
						new Column("sha256", "TEXT", 1, 0),
						new Column("mode", "INTEGER"),
						new Column("clear", "INTEGER"),
						new Column("oldclear", "INTEGER"),
						new Column("score", "INTEGER"),
						new Column("oldscore", "INTEGER"),
						new Column("combo", "INTEGER"),
						new Column("oldcombo", "INTEGER"),
						new Column("minbp", "INTEGER"),
						new Column("oldminbp", "INTEGER"),
						new Column("date", "INTEGER")
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

	public void setScoreLog(ScoreLog log) {
		try {
			this.insert(qr, "scorelog", log);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * スコアログ
	 * 
	 * @author exch
	 */
	public static class ScoreLog implements Validatable {

		/**
		 * 譜面ハッシュ(SHA256)
		 */
		private String sha256;
		/**
		 * LNモード
		 */
		private int mode;
		/**
		 * 新クリアランプ
		 */
		private int clear;
		/**
		 * 旧クリアランプ
		 */
		private int oldclear;
		/**
		 * 新スコア
		 */
		private int score;		
		/**
		 * 旧スコア
		 */
		private int oldscore;		
		/**
		 * 新コンボ
		 */
		private int combo;		
		/**
		 * 旧コンボ
		 */
		private int oldcombo;		
		/**
		 * 新ミスカウント
		 */
		private int minbp;		
		/**
		 * 旧ミスカウント
		 */
		private int oldminbp;
		/**
		 * スコア最終更新日時(unixtime, 秒単位)
		 */
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

		@Override
		public boolean validate() {
			return mode >= 0 && clear >= 0 && clear <= ClearType.Max.id && oldclear >= 0 && oldclear<= clear &&
					score >= 0 && oldscore <= score && combo >= 0 && oldcombo <= combo && minbp >= 0 && oldminbp >= minbp && date >= 0;
		}
	}
}
