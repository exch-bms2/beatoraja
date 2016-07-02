package bms.player.beatoraja;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import utils.sql.SqliteDBManager;
import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.BMSONDecoder;

/**
 * 楽曲データベースへのアクセスクラス
 * 
 * @author exch
 */
public class SongDatabaseAccessor {

	public static final String HASH = "hash";
	public static final String TITLE = "title";
	public static final String SUBTITLE = "subtitle";
	public static final String TAG = "tag";

	public static final int FEATURE_LONGNOTE = 1;
	public static final int FEATURE_MINENOTE = 2;
	public static final int FEATURE_RANDOM = 4;

	public static final int CONTENT_TEXT = 1;
	public static final int CONTENT_BGA = 2;

	private SqliteDBManager songdb;

	private QueryRunner qr;

	public SongDatabaseAccessor(String filepath) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		songdb = new SqliteDBManager(filepath);
		qr = new QueryRunner(songdb.getDataSource());
	}

	/**
	 * 楽曲データベースを初期テーブルを作成する。 すでに初期テーブルを作成している場合は何もしない。
	 */
	public void createTable() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = songdb.getDataSource().getConnection();
			String sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
			// conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(sql);

			// songテーブル作成(存在しない場合)
			pstmt.setString(1, "song");
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				QueryRunner qr = new QueryRunner();
				if (qr.query(conn, sql, new MapListHandler(), "song").size() == 0) {
					sql = "CREATE TABLE [song] ([md5] TEXT NOT NULL," + "[sha256] TEXT NOT NULL," + "[title] TEXT,"
							+ "[subtitle] TEXT," + "[genre] TEXT," + "[artist] TEXT," + "[subartist] TEXT,"
							+ "[tag] TEXT," + "[path] TEXT," + "[folder] TEXT," + "[stagefile] TEXT,"
							+ "[banner] TEXT," + "[backbmp] TEXT," + "[parent] TEXT," + "[level] INTEGER,"
							+ "[difficulty] INTEGER," + "[maxbpm] INTEGER," + "[minbpm] INTEGER," + "[mode] INTEGER,"
							+ "[judge] INTEGER," + "[feature] INTEGER," + "[content] INTEGER," + "[date] INTEGER,"
							+ "[favorite] INTEGER," + "[notes] INTEGER," + "[adddate] INTEGER,"
							+ "PRIMARY KEY(sha256, path));";
					qr.update(conn, sql);
				}
			}
			rs.close();

			sql = "SELECT * FROM sqlite_master WHERE name = ? and type='table';";
			// conn.setAutoCommit(false);
			pstmt.setString(1, "folder");
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				QueryRunner qr = new QueryRunner();
				if (qr.query(conn, sql, new MapListHandler(), "folder").size() == 0) {
					sql = "CREATE TABLE [folder] (" + "[title] TEXT," + "[subtitle] TEXT," + "[command] TEXT,"
							+ "[path] TEXT," + "[type] INTEGER," + "[banner] TEXT," + "[parent] TEXT,"
							+ "[date] INTEGER," + "[max] INTEGER," + "[adddate] INTEGER," + "PRIMARY KEY(path));";
					qr.update(conn, sql);
				}
			}
			rs.close();
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
		} finally {
			try {
				if (rs != null /* && !rs.isClosed() */) {
					rs.close();
				}
				if (pstmt != null /* isClosedは使えないので && !pstmt.isClosed() */) {
					pstmt.close();
				}
				if (conn != null && !conn.isClosed()) {
					// conn.rollback();
					conn.close();
				}
			} catch (SQLException e) {
				// どうしようもない
				Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
			}
		}
	}

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @param lr2path
	 *            LR2ルートパス
	 * @return 検索結果
	 */
	public SongData[] getSongDatas(String key, String value, String lr2path) {
		Map<String, String> values = new HashMap<String, String>();
		values.put(key, value);
		return getSongDatas(values, lr2path);
	}

	/**
	 * 楽曲を取得する
	 * 
	 * @param values
	 *            検索条件とする<属性, 属性値>のマップ
	 * @param lr2path
	 *            LR2ルートパス
	 * @return 検索結果
	 */
	public SongData[] getSongDatas(Map<String, String> values, String lr2path) {
		SongData[] result = new SongData[0];
		try {
			String str = "";
			for (String key : values.keySet()) {
				String value = values.get(key);
				if (value != null && value.length() > 0) {
					str += (str.length() > 0 ? "," : "") + key + " = '" + value + "'";
				}
			}
			List<SongData> m = qr.query("SELECT * FROM song WHERE " + str,
					new BeanListHandler<SongData>(SongData.class));

			for (SongData song : m) {
				if (!song.getPath().startsWith("/") && !song.getPath().contains(":\\")) {
					song.setPath(lr2path + "\\" + song.getPath());
				}
			}
			result = m.toArray(new SongData[0]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return result;
	}

	public SongData[] getSongDatas(String[] hashes, String lr2path) {
		SongData[] result = new SongData[0];
		try {
			StringBuilder str = new StringBuilder();
			for (String hash : hashes) {
				if (str.length() > 0) {
					str.append(',');
				}
				str.append('\'').append(hash).append('\'');
			}
			List<SongData> m = qr.query("SELECT * FROM song WHERE md5 IN (" + str.toString() + ")",
					new BeanListHandler<SongData>(SongData.class));

			for (SongData song : m) {
				if (!song.getPath().startsWith("/") && !song.getPath().contains(":\\")) {
					song.setPath(lr2path + "\\" + song.getPath());
				}
			}
			result = m.toArray(new SongData[0]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return result;
	}

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @param lr2path
	 *            LR2ルートパス
	 * @return 検索結果
	 */
	public FolderData[] getFolderDatas(String key, String value, String lr2path) {
		Map<String, String> values = new HashMap<String, String>();
		values.put(key, value);
		return getFolderDatas(values, lr2path);
	}

	/**
	 * 楽曲を取得する
	 * 
	 * @param values
	 *            検索条件とする<属性, 属性値>のマップ
	 * @param lr2path
	 *            LR2ルートパス
	 * @return 検索結果
	 */
	public FolderData[] getFolderDatas(Map<String, String> values, String lr2path) {
		FolderData[] result = new FolderData[0];
		try {
			String str = "";
			for (String key : values.keySet()) {
				String value = values.get(key);
				if (value != null && value.length() > 0) {
					str += (str.length() > 0 ? "," : "") + key + " = '" + value + "'";
				}
			}
			List<FolderData> m = qr.query("SELECT * FROM folder WHERE " + str, new BeanListHandler<FolderData>(
					FolderData.class));

			for (FolderData song : m) {
				if (!song.getPath().startsWith("/") && !song.getPath().contains(":\\")) {
					song.setPath(lr2path + "\\" + song.getPath());
				}
			}
			result = m.toArray(new FolderData[0]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return result;
	}

	/**
	 * 楽曲を更新する
	 * 
	 * @param datas
	 *            <楽曲のmd5, <属性, 属性値>>のマップ
	 */
	public void setSongDatas(Map<String, Map<String, String>> datas) {
		Connection conn = null;
		try {
			conn = qr.getDataSource().getConnection();
			conn.setAutoCommit(false);

			for (String te : datas.keySet()) {
				Map<String, String> data = datas.get(te);
				String values = "";
				for (String key : data.keySet()) {
					values += key + " = " + data.get(key) + " ,";
				}
				qr.update(conn, "UPDATE song SET " + values.substring(0, values.length() - 1) + "WHERE hash = '" + te
						+ "'");
			}
			conn.commit();
			conn.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
				}
			}
		}
	}

	/**
	 * 譜面のパスを取得する
	 * 
	 * @param hash
	 *            譜面のhash値
	 * @return <譜面のhash値, 譜面パス>のマップ
	 */
	public Map<String, String> getSongPaths(String[] hash, String lr2path) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			ResultSetHandler<List<SongData>> rh = new BeanListHandler<SongData>(SongData.class);
			for (int i = 0; i < hash.length; i++) {
				List<SongData> rs = qr.query("SELECT * FROM song WHERE hash = '" + hash[i] + "'", rh);

				for (SongData song : rs) {
					String path = song.getPath();
					if (!song.getPath().startsWith("/") && !song.getPath().contains(":\\")) {
						path = lr2path + "\\" + path;
					}
					result.put(hash[i], path);
				}
			}
		} catch (Exception e) {
			Logger.getGlobal().severe("譜面パス取得時の例外:" + e.getMessage());
		}
		return result;
	}

	/**
	 * データベースを更新する
	 * 
	 * @param files
	 *            更新するディレクトリ(ルートディレクトリでなくても可)
	 * @param rootdirs
	 *            楽曲のルートパス
	 * @param path
	 *            LR2のルートパス
	 */
	public void updateSongDatas(File[] files, String[] rootdirs, String path, boolean updateAll) {
		SongDatabaseUpdater updater = new SongDatabaseUpdater(rootdirs, path, updateAll);
		updater.updateSongDatas(files);
	}

	private static final int Polynomial = 0xEDB88320;

	public String crc32(String path, String[] rootdirs, String bmspath) {
		for (String s : rootdirs) {
			if (new File(s).getParentFile().getAbsolutePath().equals(path)) {
				return "e2977170";
			}
		}

		if (path.startsWith(bmspath)) {
			path = path.substring(bmspath.length() + 1);
		}
		int previousCrc32 = 0;
		int crc = ~previousCrc32; // same as previousCrc32 ^ 0xFFFFFFFF

		for (byte b : (path + "\\\0").getBytes()) {
			crc ^= b;
			for (int j = 0; j < 8; j++)
				if ((crc & 1) != 0)
					crc = (crc >>> 1) ^ Polynomial;
				else
					crc = crc >>> 1;
		}
		return Integer.toHexString(~crc); // same as crc ^ 0xFFFFFFFF
	}

	class SongDatabaseUpdater {

		private final ResultSetHandler<List<SongData>> rh = new BeanListHandler<SongData>(SongData.class);

		private final ResultSetHandler<List<FolderData>> rh2 = new BeanListHandler<FolderData>(FolderData.class);

		private final QueryRunner qr = new QueryRunner();

		private final BMSDecoder bmsdecoder = new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
		private final BMSONDecoder bmsondecoder = new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE);

		private int count = 0;

		private String[] rootdirs;
		private String path;

		private Map<String, String> tags = new HashMap<String, String>();
		private Connection conn;
		private boolean updateAll;

		private final String[] TEXT = { "txt" };
		private final String[] BMSON = { "bmson" };
		private final String[] BMS = { "bms", "bme", "bml", "pms" };
		private final String[] ALLBMS = { "bms", "bme", "bml", "pms", "bmson" };

		public SongDatabaseUpdater(String[] rootdirs, String path, boolean updateAll) {
			this.rootdirs = rootdirs;
			this.path = path;
			this.updateAll = updateAll;
		}

		/**
		 * データベースを更新する
		 * 
		 * @param files
		 *            更新するディレクトリ(ルートディレクトリでなくても可)
		 * @param rootdirs
		 *            楽曲のルートパス
		 * @param path
		 *            LR2のルートパス
		 */
		public void updateSongDatas(File[] files) {
			long time = System.currentTimeMillis();
			count = 0;
			DataSource ds = songdb.getDataSource();
			try {
				conn = ds.getConnection();
				conn.setAutoCommit(false);
				// ルートディレクトリに含まれないフォルダの削除
				String dsql = "";
				for (int i = 0; i < rootdirs.length; i++) {
					dsql += "path NOT LIKE '" + rootdirs[i] + "%'";
					if (i < rootdirs.length - 1) {
						dsql += " AND ";
					}
				}
				qr.update(conn,
						"DELETE FROM folder WHERE path NOT LIKE 'LR2files%' AND path NOT LIKE '%.lr2folder' AND "
								+ dsql);
				qr.update(conn, "DELETE FROM song WHERE " + dsql);
				// 楽曲のタグの保持
				for (File f : files) {
					String s = f.getAbsolutePath();
					if (s.startsWith(path)) {
						s = s.substring(path.length() + 1);
					}
					List<SongData> records = qr.query(conn, "SELECT * FROM song WHERE path LIKE ?", rh, s + "%");
					for (SongData record : records) {
						tags.put(record.getMd5(), record.getTag());
					}
				}
				for (File f : files) {
					this.processDirectory(f, true);
				}
				conn.commit();
				conn.close();
			} catch (Exception e) {
				Logger.getGlobal().severe("楽曲データベース更新時の例外:" + e.getMessage());
				e.printStackTrace();
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
					}
				}
			}
			long nowtime = System.currentTimeMillis();
			Logger.getGlobal().info(
					"楽曲更新完了 : Time - " + (nowtime - time) + " 1曲あたりの時間 - "
							+ (count > 0 ? (nowtime - time) / count : "不明"));
		}

		private void processDirectory(final File dir, boolean updateFolder) throws SQLException {
			List<SongData> records = qr.query(conn, "SELECT path,date FROM song WHERE folder = ?", rh,
					crc32(dir.getAbsolutePath(), rootdirs, path));
			List<FolderData> folders = qr.query(conn, "SELECT path,date FROM folder WHERE parent = ?", rh2,
					crc32(dir.getAbsolutePath(), rootdirs, path));
			boolean txt = FileUtils.listFiles(dir, TEXT, false).size() > 0;
			final Collection<File> bmsfiles = FileUtils.listFiles(dir, ALLBMS, false);

			List<SongData> removes = new ArrayList<SongData>(records);

			for (File f : bmsfiles) {
				boolean b = true;
				String s = f.getAbsolutePath();
				if (s.startsWith(path)) {
					s = s.substring(path.length() + 1);
				}
				for (SongData record : records) {
					if (record.getPath().equals(s)) {
						removes.remove(record);
						if (!updateAll && record.getDate() == f.lastModified() / 1000) {
							b = false;
						}
						break;
					}
				}
				if (b) {
					this.processFile(f, txt);
				}
			}
			// ディレクトリ内のファイルに存在しないレコードを削除
			for (SongData record : removes) {
				qr.update(conn, "DELETE FROM song WHERE path = ?", record.getPath());
			}

			List<FolderData> fremoves = new ArrayList<FolderData>(folders);
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					boolean b = true;
					String s = f.getAbsolutePath() + File.separatorChar;
					if (s.startsWith(path)) {
						s = s.substring(path.length() + 1);
					}
					for (FolderData record : folders) {
						if (record.getPath().equals(s)) {
							fremoves.remove(record);
							if(!updateAll && record.getDate() == f.lastModified() / 1000) {
								b = false;
							}
							break;
						}
					}
					this.processDirectory(f, b);
				}
			}
			// folderテーブルの更新

			if (updateFolder) {
				String s = dir.getAbsolutePath() + File.separatorChar;
				if (s.startsWith(path)) {
					s = s.substring(path.length() + 1);
				}
				System.out.println("folder更新 : " + s);
				qr.update(conn,
						"INSERT OR REPLACE INTO folder (title, subtitle, command, path, type, banner, parent, date, max, adddate)"
								+ "VALUES(?,?,?,?,?,?,?,?,?,?);", dir.getName(), "", "", s, 1, "",
						crc32(dir.getParentFile().getAbsolutePath(), rootdirs, path), dir.lastModified() / 1000, null,
						Calendar.getInstance().getTimeInMillis() / 1000);
			}
			// ディレクトリ内に存在しないフォルダレコードを削除
			for (FolderData record : fremoves) {
				System.out.println("Song Database : folder deleted - " + record.getPath());
				qr.update(conn, "DELETE FROM folder WHERE path = ?", record.getPath());
				qr.update(conn, "DELETE FROM song WHERE path LIKE ?", record.getPath() + "%");
			}
		}

		private void processFile(final File dir, boolean containstxt) throws SQLException {
			// ファイル処理
			String name = dir.getName().toLowerCase();
			String s = dir.getAbsolutePath();
			if (s.startsWith(path)) {
				s = s.substring(path.length() + 1);
			}
			BMSModel model = null;
			if (FilenameUtils.isExtension(name, BMS)) {
				model = bmsdecoder.decode(dir);
				qr.update(conn, "DELETE FROM song WHERE path = ?", s);
			} else if (FilenameUtils.isExtension(name, BMSON)) {
				model = bmsondecoder.decode(dir);
				qr.update(conn, "DELETE FROM song WHERE path = ?", s);
			}

			if (model != null && (model.getTotalNotes() != 0 || model.getWavList().length != 0)) {
				// TODO LR2ではDIFFICULTY未定義の場合に同梱譜面を見て振り分けている
				if (model.getDifficulty() == 0) {
					try {
						int level = (Integer.parseInt(model.getPlaylevel()) - 1) / 3 + 1;
						model.setDifficulty(level <= 5 ? level : 5);
					} catch (NumberFormatException e) {
						model.setDifficulty(5);
					}
				}

				int ln = model.containsLongNote() ? FEATURE_LONGNOTE : 0;
				ln += model.containsMineNote() ? FEATURE_MINENOTE : 0;
				ln += model.getRandom() > 1 ? FEATURE_RANDOM : 0;
				int txt = containstxt ? CONTENT_TEXT : 0;
				txt += model.getBgaList().length > 0 ? CONTENT_BGA : 0;
				qr.update(conn, "INSERT INTO song "
						+ "(md5, sha256, title, subtitle, genre, artist, subartist, tag, path,"
						+ "folder, stagefile, banner, backbmp, parent, level, difficulty, "
						+ "maxbpm, minbpm, mode, judge, feature, content, " + "date, favorite, notes, adddate)"
						+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);", model.getMD5(),
						model.getSHA256(), model.getTitle(), model.getSubTitle(), model.getGenre(), model.getArtist(),
						model.getSubArtist(), tags.get(model.getMD5()) != null ? tags.get(model.getMD5()) : "", s,
						crc32(dir.getParentFile().getAbsolutePath(), rootdirs, path), model.getStagefile(),
						model.getBanner(), model.getBackbmp(),
						crc32(dir.getParentFile().getParentFile().getAbsolutePath(), rootdirs, path),
						model.getPlaylevel(), model.getDifficulty(), model.getMaxBPM(), model.getMinBPM(),
						model.getUseKeys(), model.getJudgerank(), ln, txt, dir.lastModified() / 1000, 0,
						model.getTotalNotes(), Calendar.getInstance().getTimeInMillis() / 1000);
				count++;
			}
		}
	}
}
