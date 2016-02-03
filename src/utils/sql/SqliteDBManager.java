package utils.sql;

import java.io.File;

import javax.sql.DataSource;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class SqliteDBManager {

	private String filepath;

	public SqliteDBManager(String filepath) {
		this.filepath = filepath;
	}

	public SqliteDBManager(final File file) {
		filepath = file.getPath();
	}

	/**
	 * @param file DBデータファイル
	 * @throws ClassNotFoundException SQLite3のJDBCクラスが見つからない
	 * @return sqlite3コネクションオブジェクト
	 */
	public final DataSource getDataSource() {
		try {

			Class.forName("org.sqlite.JDBC");
			SQLiteConfig conf;
			SQLiteDataSource ds;

			conf = new SQLiteConfig();
			conf.setSharedCache(true);

			ds = new SQLiteDataSource(conf);
			ds.setUrl("jdbc:sqlite:" + filepath);


			return ds;
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return SQLite3 database file path.
	 */
	public final String getFilePath() {
		return filepath;
	}
}
