package bms.player.beatoraja.song;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import bms.player.beatoraja.Validatable;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteDataSource;

import bms.model.*;

/**
 * 楽曲データベースへのアクセスクラス
 * 
 * @author exch
 */
public class SQLiteSongDatabaseAccessor implements SongDatabaseAccessor {

	private SQLiteDataSource ds;

	private final Path root;
	private String[] bmsroot;

	private final ResultSetHandler<List<SongData>> songhandler = new BeanListHandler<SongData>(SongData.class);
	private final ResultSetHandler<List<FolderData>> folderhandler = new BeanListHandler<FolderData>(FolderData.class);

	private final QueryRunner qr;
	
	private List<SongDatabaseAccessorPlugin> plugins = new ArrayList();
	
	public SQLiteSongDatabaseAccessor(String filepath, String[] bmsroot) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + filepath);
		qr = new QueryRunner(ds);
		root = Paths.get(".");
		this.bmsroot = bmsroot;
		createTable();
	}
	
	public void addPlugin(SongDatabaseAccessorPlugin plugin) {
		plugins.add(plugin);
	}

	/**
	 * 楽曲データベースを初期テーブルを作成する。 すでに初期テーブルを作成している場合は何もしない。
	 */
	private void createTable() {
		try {
			// songテーブル作成(存在しない場合)
			if (qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';", new MapListHandler(), "song")
					.size() == 0) {
				qr.update("CREATE TABLE [song] ([md5] TEXT NOT NULL," + "[sha256] TEXT NOT NULL," + "[title] TEXT,"
						+ "[subtitle] TEXT," + "[genre] TEXT," + "[artist] TEXT," + "[subartist] TEXT," + "[tag] TEXT,"
						+ "[path] TEXT," + "[folder] TEXT," + "[stagefile] TEXT," + "[banner] TEXT," + "[backbmp] TEXT,"
						+ "[preview] TEXT," + "[parent] TEXT," + "[level] INTEGER," + "[difficulty] INTEGER," + "[maxbpm] INTEGER,"
						+ "[minbpm] INTEGER," + "[mode] INTEGER," + "[judge] INTEGER," + "[feature] INTEGER,"
						+ "[content] INTEGER," + "[date] INTEGER," + "[favorite] INTEGER," + "[notes] INTEGER,"
						+ "[adddate] INTEGER,"  + "[charthash] TEXT," + "PRIMARY KEY(sha256, path));");
			}

			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'song' AND sql LIKE '%preview%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE song ADD COLUMN preview [TEXT]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'song' AND sql LIKE '%length%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE song ADD COLUMN length [INTEGER]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'song' AND sql LIKE '%charthash%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE song ADD COLUMN charthash [TEXT]");
			}

			if (qr.query("SELECT * FROM sqlite_master WHERE name = ? and type='table';", new MapListHandler(), "folder")
					.size() == 0) {
				qr.update("CREATE TABLE [folder] (" + "[title] TEXT," + "[subtitle] TEXT," + "[command] TEXT,"
						+ "[path] TEXT," + "[type] INTEGER," + "[banner] TEXT," + "[parent] TEXT," + "[date] INTEGER,"
						+ "[max] INTEGER," + "[adddate] INTEGER," + "PRIMARY KEY(path));");
			}
			
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
		}
	}

	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @return 検索結果
	 */
	public SongData[] getSongDatas(String key, String value) {
		try {
			final List<SongData> m = qr.query("SELECT * FROM song WHERE " + key + " = ?", songhandler, value);
			return Validatable.removeInvalidElements(m).toArray(new SongData[m.size()]);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}
		return SongData.EMPTY;
	}

	/**
	 * MD5/SHA256で指定した楽曲をまとめて取得する
	 * 
	 * @param hashes
	 *            楽曲のMD5/SHA256
	 * @return 取得した楽曲
	 */
	public SongData[] getSongDatas(String[] hashes) {
		try {
			StringBuilder md5str = new StringBuilder();
			StringBuilder sha256str = new StringBuilder();
			for (String hash : hashes) {
				if (hash.length() > 32) {
					if (sha256str.length() > 0) {
						sha256str.append(',');
					}
					sha256str.append('\'').append(hash).append('\'');
				} else {
					if (md5str.length() > 0) {
						md5str.append(',');
					}
					md5str.append('\'').append(hash).append('\'');
				}
			}
			List<SongData> m = qr.query("SELECT * FROM song WHERE md5 IN (" + md5str.toString() + ") OR sha256 IN ("
					+ sha256str.toString() + ")", songhandler);

			return Validatable.removeInvalidElements(m).toArray(new SongData[m.size()]);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return SongData.EMPTY;
	}

	public SongData[] getSongDatas(String sql, String score, String scorelog, String info) {
		try (Statement stmt = qr.getDataSource().getConnection().createStatement()) {
			stmt.execute("ATTACH DATABASE '" + score + "' as scoredb");
			stmt.execute("ATTACH DATABASE '" + scorelog + "' as scorelogdb");
			List<SongData> m;
			if(info != null) {
				stmt.execute("ATTACH DATABASE '" + info + "' as infodb");
				String s = "SELECT DISTINCT song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length, charthash"
						+ " FROM song INNER JOIN (information LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON information.sha256 = score.sha256) "
						+ "ON song.sha256 = information.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
//				System.out.println(s + " -> result : " + m.size());
				stmt.execute("DETACH DATABASE infodb");
			} else {
				String s = "SELECT DISTINCT song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length, charthash"
						+ " FROM song LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON song.sha256 = score.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
			}
			stmt.execute("DETACH DATABASE scorelogdb");				
			stmt.execute("DETACH DATABASE scoredb");
			return Validatable.removeInvalidElements(m).toArray(new SongData[m.size()]);
		} catch(Throwable e) {
			e.printStackTrace();			
		}

		return SongData.EMPTY;

	}

	public SongData[] getSongDatasByText(String text) {
		try {
			List<SongData> m = qr.query(
					"SELECT * FROM song WHERE rtrim(title||' '||subtitle||' '||artist||' '||subartist||' '||genre) LIKE ?"
							+ " GROUP BY sha256",songhandler, "%" + text.replaceAll("'", "''") + "%");
			return Validatable.removeInvalidElements(m).toArray(new SongData[m.size()]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return SongData.EMPTY;
	}
	
	/**
	 * 楽曲を取得する
	 * 
	 * @param key
	 *            属性
	 * @param value
	 *            属性値
	 * @return 検索結果
	 */
	public FolderData[] getFolderDatas(String key, String value) {
		try {
			final List<FolderData> m = qr.query("SELECT * FROM folder WHERE " + key + " = ?", folderhandler, value);
			return m.toArray(new FolderData[m.size()]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return FolderData.EMPTY;
	}

	/**
	 * 楽曲を更新する
	 * 
	 * @param songs 更新する楽曲
	 */
	public void setSongDatas(SongData[] songs) {
		try (Connection conn = qr.getDataSource().getConnection()){
			conn.setAutoCommit(false);

			for (SongData sd : songs) {
				// TODO このメソッドは共通化させたい
				qr.update(conn,
						"INSERT OR REPLACE INTO song "
								+ "(md5, sha256, title, subtitle, genre, artist, subartist, tag, path,"
								+ "folder, stagefile, banner, backbmp, preview, parent, level, difficulty, "
								+ "maxbpm, minbpm, length, mode, judge, feature, content, "
								+ "date, favorite, notes, adddate, charthash)"
								+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
						sd.getMd5(), sd.getSha256(), sd.getTitle(), sd.getSubtitle(), sd.getGenre(),
						sd.getArtist(), sd.getSubartist(), sd.getTag(), sd.getPath(), sd.getFolder(),
						sd.getStagefile(), sd.getBanner(), sd.getBackbmp(), sd.getPreview(),sd.getParent(),
						sd.getLevel(), sd.getDifficulty(), sd.getMaxbpm(), sd.getMinbpm(), sd.getLength(),
						sd.getMode(), sd.getJudge(), sd.getFeature(), sd.getContent(),
						sd.getDate(), sd.getFavorite(), sd.getNotes(), sd.getAdddate(), sd.getCharthash());
			}
			conn.commit();
			conn.close();
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}
	}

	/**
	 * データベースを更新する
	 * 
	 * @param path
	 *            LR2のルートパス
	 */
	public void updateSongDatas(String path, boolean updateAll, SongInformationAccessor info) {
		SongDatabaseUpdater updater = new SongDatabaseUpdater(updateAll, info);
		Path[] paths = null;
		if (path == null) {
			paths = new Path[bmsroot.length];
			for (int i = 0; i < paths.length; i++) {
				paths[i] = Paths.get(bmsroot[i]);
			}
		} else {
			paths = new Path[]{Paths.get(path)};
		}
		updater.updateSongDatas(paths);
	}

	/**
	 * song database更新用クラス
	 * 
	 * @author exch
	 */
	class SongDatabaseUpdater {

		private final boolean updateAll;

		private SongInformationAccessor info;

		public SongDatabaseUpdater(boolean updateAll, SongInformationAccessor info) {
			this.updateAll = updateAll;
			this.info = info;
		}

		/**
		 * データベースを更新する
		 * 
		 * @param paths
		 *            更新するディレクトリ(ルートディレクトリでなくても可)
		 */
		public void updateSongDatas(Path[] paths) {
			long time = System.currentTimeMillis();
			SongDatabaseUpdaterProperty property = new SongDatabaseUpdaterProperty(Calendar.getInstance().getTimeInMillis() / 1000, updateAll, info);
			property.count.set(0);
			if(info != null) {
				info.startUpdate();
			}
			try (Connection conn = ds.getConnection()) {
				property.conn = conn;
				conn.setAutoCommit(false);
				// ルートディレクトリに含まれないフォルダの削除
				StringBuilder dsql = new StringBuilder();
				Object[] param = new String[bmsroot.length];
				for (int i = 0; i < bmsroot.length; i++) {
					dsql.append("path NOT LIKE ?");
					param[i] = bmsroot[i] + "%";
					if (i < bmsroot.length - 1) {
						dsql.append(" AND ");
					}
				}
				
				qr.update(conn,
						"DELETE FROM folder WHERE path NOT LIKE 'LR2files%' AND path NOT LIKE '%.lr2folder' AND "
								+ dsql.toString(), param);
				qr.update(conn, "DELETE FROM song WHERE " + dsql.toString(), param);
				// 楽曲のタグ,FAVORITEの保持
				for (SongData record : qr.query(conn, "SELECT md5, tag, favorite FROM song", songhandler)) {
					if (record.getTag().length() > 0) {
						property.tags.put(record.getMd5(), record.getTag());
					}
					if (record.getFavorite() > 0) {
						property.favorites.put(record.getMd5(), record.getFavorite());
					}
				}
				
				Arrays.stream(paths).parallel().forEach((p) -> {
					try {
						BMSFolder folder = new BMSFolder(p);
						folder.processDirectory(property);
					} catch (IOException | SQLException e) {
						Logger.getGlobal().severe("楽曲データベース更新時の例外:" + e.getMessage());
					}
				});
				conn.commit();
			} catch (Exception e) {
				Logger.getGlobal().severe("楽曲データベース更新時の例外:" + e.getMessage());
				e.printStackTrace();
			}

			if(info != null) {
				info.endUpdate();
			}
			long nowtime = System.currentTimeMillis();
			Logger.getGlobal().info("楽曲更新完了 : Time - " + (nowtime - time) + " 1曲あたりの時間 - "
					+ (property.count.get() > 0 ? (nowtime - time) / property.count.get() : "不明"));
		}

	}
	
	private class BMSFolder {
		
		public final Path path;
		public boolean updateFolder = true;
		private boolean txt = false;
		private final List<Path> bmsfiles = new ArrayList<Path>();
		private final List<BMSFolder> dirs = new ArrayList<BMSFolder>();
		private String previewpath = null;
		
		public BMSFolder(Path path) {
			this.path = path;
		}
		
		private void processDirectory(SongDatabaseUpdaterProperty property)
				throws IOException, SQLException {
			final List<SongData> records = qr.query(property.conn, "SELECT path,date FROM song WHERE folder = ?", songhandler,
					SongUtils.crc32(path.toString(), bmsroot, root.toString()));
			final List<FolderData> folders = qr.query(property.conn, "SELECT path,date FROM folder WHERE parent = ?",
					folderhandler, SongUtils.crc32(path.toString(), bmsroot, root.toString()));
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
				for (Path p : paths) {
					if(Files.isDirectory(p)) {
						dirs.add(new BMSFolder(p));
					} else {
						final String s = p.getFileName().toString().toLowerCase();
						if (!txt && s.endsWith(".txt")) {
							txt = true;
						}
						if (previewpath == null) {
							if(s.startsWith("preview") && (s.endsWith(".wav") ||
															s.endsWith(".ogg") ||
															s.endsWith(".mp3") ||
															s.endsWith(".flac"))) {
								previewpath = p.getFileName().toString();
							}
						}
						if (s.endsWith(".bms") || s.endsWith(".bme") || s.endsWith(".bml") || s.endsWith(".pms")
								|| s.endsWith(".bmson")) {
							bmsfiles.add(p);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			final boolean containsBMS = bmsfiles.size() > 0;
			property.count.addAndGet(this.processBMSFolder(records, property));

			final int len = folders.size();
			for (BMSFolder bf : dirs) {
				final String s = (bf.path.startsWith(root) ? root.relativize(bf.path).toString() : bf.path.toString())
						+ File.separatorChar;
				for (int i = 0; i < len;i++) {
					final FolderData record = folders.get(i);
					if (record != null && record.getPath().equals(s)) {
//						long t = System.nanoTime();
						folders.set(i, null);
//						System.out.println(System.nanoTime() - t);
						if (!property.updateAll && record.getDate() == Files.getLastModifiedTime(bf.path).toMillis() / 1000) {
							bf.updateFolder = false;
						}
						break;
					}
				}
			}
			
			if(!containsBMS) {
				dirs.forEach((bf) -> {
					try {
						bf.processDirectory(property);
					} catch (IOException | SQLException e) {
						Logger.getGlobal().severe("楽曲データベース更新時の例外:" + e.getMessage());
					}					
				});
			}

			// folderテーブルの更新
			if (updateFolder) {
				final String s = (path.startsWith(root) ? root.relativize(path).toString() : path.toString())
						+ File.separatorChar;
				// System.out.println("folder更新 : " + s);
				Path parentpath = path.getParent();
				if(parentpath == null) {
					parentpath = path.toAbsolutePath().getParent();
				}				
				qr.update(property.conn,
						"INSERT OR REPLACE INTO folder (title, subtitle, command, path, type, banner, parent, date, max, adddate)"
								+ "VALUES(?,?,?,?,?,?,?,?,?,?);",
								path.getFileName().toString(), "", "", s, 1, "",
						SongUtils.crc32(parentpath.toString() , bmsroot, root.toString()),
						Files.getLastModifiedTime(path).toMillis() / 1000, null,
						property.updatetime);
			}
			// ディレクトリ内に存在しないフォルダレコードを削除
			for (FolderData record : folders) {
				if(record != null) {
					// System.out.println("Song Database : folder deleted - " +
					// record.getPath());
					qr.update(property.conn, "DELETE FROM folder WHERE path LIKE ?", record.getPath() + "%");
					qr.update(property.conn, "DELETE FROM song WHERE path LIKE ?", record.getPath() + "%");
				}
			}
		}
		
		private int processBMSFolder(List<SongData> records, SongDatabaseUpdaterProperty property) {
			int count = 0;
			BMSDecoder bmsdecoder = null;
			BMSONDecoder bmsondecoder = null;
			final int len = records.size();
			for (Path path : bmsfiles) {
				boolean update = true;
				final String pathname = (path.startsWith(root) ? root.relativize(path).toString() : path.toString());
				for (int i = 0;i < len;i++) {
					final SongData record = records.get(i);
					if (record != null && record.getPath().equals(pathname)) {
						records.set(i, null);
						try {
							if (!property.updateAll && record.getDate() == Files.getLastModifiedTime(path).toMillis() / 1000) {
								update = false;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					}
				}
				if (!update) {
					continue;
				}
				BMSModel model = null;
				if (pathname.toLowerCase().endsWith(".bmson")) {
					if (bmsondecoder == null) {
						bmsondecoder = new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE);
					}
					model = bmsondecoder.decode(path);
				} else {
					if (bmsdecoder == null) {
						bmsdecoder = new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
					}
					model = bmsdecoder.decode(path);
				}

				if (model == null) {
					continue;
				}
				final SongData sd = new SongData(model, txt);
				if (sd.getNotes() != 0 || model.getWavList().length != 0) {
					if (sd.getDifficulty() == 0) {
						final String fulltitle = (sd.getTitle() + sd.getSubtitle()).toLowerCase();
						if (fulltitle.contains("beginner")) {
							sd.setDifficulty(1);
						} else if (fulltitle.contains("normal")) {
							sd.setDifficulty(2);
						} else if (fulltitle.contains("hyper")) {
							sd.setDifficulty(3);
						} else if (fulltitle.contains("another")) {
							sd.setDifficulty(4);
						} else if (fulltitle.contains("insane")) {
							sd.setDifficulty(5);
						} else {
							if (sd.getNotes() < 250) {
								sd.setDifficulty(1);
							} else if (sd.getNotes() < 600) {
								sd.setDifficulty(2);
							} else if (sd.getNotes() < 1000) {
								sd.setDifficulty(3);
							} else if (sd.getNotes() < 2000) {
								sd.setDifficulty(4);
							} else {
								sd.setDifficulty(5);
							}
						}
					}
					if((sd.getPreview() == null || sd.getPreview().length() == 0) && previewpath != null) {
						sd.setPreview(previewpath);
					}
					final String tag = property.tags.get(sd.getMd5());
					final Integer favorite = property.favorites.get(sd.getMd5());
					
					for(SongDatabaseAccessorPlugin plugin : plugins) {
						plugin.update(model, sd);
					}
					
					try {
						qr.update(property.conn,
								"INSERT OR REPLACE INTO song "
										+ "(md5, sha256, title, subtitle, genre, artist, subartist, tag, path,"
										+ "folder, stagefile, banner, backbmp, preview, parent, level, difficulty, "
										+ "maxbpm, minbpm, length, mode, judge, feature, content, "
										+ "date, favorite, notes, adddate, charthash)"
										+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
								sd.getMd5(), sd.getSha256(), sd.getTitle(), sd.getSubtitle(), sd.getGenre(),
								sd.getArtist(), sd.getSubartist(), tag != null ? tag : "",
								pathname, SongUtils.crc32(path.getParent().toString(), bmsroot, root.toString()),
								sd.getStagefile(), sd.getBanner(), sd.getBackbmp(), sd.getPreview(),
								SongUtils.crc32(path.getParent().getParent().toString(), bmsroot, root.toString()),
								sd.getLevel(), sd.getDifficulty(), sd.getMaxbpm(), sd.getMinbpm(), sd.getLength(),
								sd.getMode(), sd.getJudge(), sd.getFeature(), sd.getContent(),
								Files.getLastModifiedTime(path).toMillis() / 1000,
								favorite != null ? favorite.intValue() : 0, sd.getNotes(), property.updatetime,
								sd.getCharthash());
					} catch (SQLException | IOException e) {
						e.printStackTrace();
					}
					if(property.info != null) {
						property.info.update(model);
					}
					count++;
				} else {
					try {
						qr.update(property.conn, "DELETE FROM song WHERE path = ?", pathname);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			// ディレクトリ内のファイルに存在しないレコードを削除
			for (SongData record : records) {
				if(record != null) {
					try {
						qr.update(property.conn, "DELETE FROM song WHERE path = ?", record.getPath());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			
			return count;
		}
	}
	
	private static class SongDatabaseUpdaterProperty {
		private final Map<String, String> tags = new HashMap<String, String>();
		private final Map<String, Integer> favorites = new HashMap<String, Integer>();
		private final SongInformationAccessor info;
		private final long updatetime;
		private final boolean updateAll;
		private final AtomicInteger count = new AtomicInteger();
		private Connection conn;
		
		public SongDatabaseUpdaterProperty(long updatetime, boolean updateAll, SongInformationAccessor info) {
			this.updatetime = updatetime;
			this.updateAll = updateAll;
			this.info = info;
		}

	}
	
	public static interface SongDatabaseAccessorPlugin {
		
		public void update(BMSModel model, SongData song);
	}
}
