package bms.player.beatoraja.song;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

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

	private Path root;
	private String[] bmsroot;

	private final ResultSetHandler<List<SongData>> songhandler = new BeanListHandler<SongData>(SongData.class);
	private final ResultSetHandler<List<FolderData>> folderhandler = new BeanListHandler<FolderData>(FolderData.class);

	private final QueryRunner qr;
	
	private static final SongData[] EMPTYSONG = new SongData[0];
	private static final FolderData[] EMPTYFOLDER = new FolderData[0];

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
						+ "[adddate] INTEGER," + "PRIMARY KEY(sha256, path));");
			}

			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'song' AND sql LIKE '%preview%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE song ADD COLUMN preview [TEXT]");
			}
			if(qr.query("SELECT * FROM sqlite_master WHERE name = 'song' AND sql LIKE '%length%'", new MapListHandler()).size() == 0) {
				qr.update("ALTER TABLE song ADD COLUMN length [INTEGER]");
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
			return m.toArray(new SongData[m.size()]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}
		return EMPTYSONG;
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

			return m.toArray(new SongData[m.size()]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return EMPTYSONG;
	}

	public SongData[] getSongDatas(String sql, String score, String scorelog, String info) {
		try (Statement stmt = qr.getDataSource().getConnection().createStatement()) {
			stmt.execute("ATTACH DATABASE '" + score + "' as scoredb");
			stmt.execute("ATTACH DATABASE '" + scorelog + "' as scorelogdb");
			List<SongData> m;
			if(info != null) {
				stmt.execute("ATTACH DATABASE '" + info + "' as infodb");
				String s = "SELECT DISTINCT song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length"
						+ " FROM song INNER JOIN (information LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON information.sha256 = score.sha256) "
						+ "ON song.sha256 = information.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
//				System.out.println(s + " -> result : " + m.size());
				stmt.execute("DETACH DATABASE infodb");
			} else {
				String s = "SELECT DISTINCT song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length"
						+ " FROM song LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON song.sha256 = score.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
			}
			stmt.execute("DETACH DATABASE scorelogdb");				
			stmt.execute("DETACH DATABASE scoredb");				
			return m.toArray(new SongData[m.size()]);
		} catch(Throwable e) {
			e.printStackTrace();			
		}

		return EMPTYSONG;

	}

	public SongData[] getSongDatasByText(String text) {
		try {
			List<SongData> m = qr.query(
					"SELECT * FROM song WHERE rtrim(title||' '||subtitle||' '||artist||' '||subartist||' '||genre) LIKE ?"
							+ " GROUP BY sha256",songhandler, "%" + text.replaceAll("'", "''") + "%");
			return m.toArray(new SongData[m.size()]);
		} catch (Exception e) {
			Logger.getGlobal().severe("song.db更新時の例外:" + e.getMessage());
		}

		return EMPTYSONG;
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

		return EMPTYFOLDER;
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
								+ "date, favorite, notes, adddate)"
								+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
						sd.getMd5(), sd.getSha256(), sd.getTitle(), sd.getSubtitle(), sd.getGenre(),
						sd.getArtist(), sd.getSubartist(), sd.getTag(), sd.getPath(), sd.getFolder(),
						sd.getStagefile(), sd.getBanner(), sd.getBackbmp(), sd.getPreview(),sd.getParent(),
						sd.getLevel(), sd.getDifficulty(), sd.getMaxbpm(), sd.getMinbpm(), sd.getLength(),
						sd.getMode(), sd.getJudge(), sd.getFeature(), sd.getContent(),
						sd.getDate(), sd.getFavorite(), sd.getNotes(), sd.getAdddate());
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
			paths = new Path[1];
			paths[0] = Paths.get(path);
		}
		updater.updateSongDatas(paths);
	}

	/**
	 * song database更新用クラス
	 * 
	 * @author exch
	 */
	class SongDatabaseUpdater {

		private int count = 0;

		private Map<String, String> tags = new HashMap<String, String>();
		private boolean updateAll;

		private long updatetime;
		
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
			updatetime = Calendar.getInstance().getTimeInMillis() / 1000;
			count = 0;
			if(info != null) {
				info.startUpdate();
			}
			try (Connection conn = ds.getConnection()) {
				conn.setAutoCommit(false);
				// ルートディレクトリに含まれないフォルダの削除
				StringBuilder dsql = new StringBuilder();
				for (int i = 0; i < bmsroot.length; i++) {
					dsql.append("path NOT LIKE '").append(bmsroot[i]).append("%'");
					if (i < bmsroot.length - 1) {
						dsql.append(" AND ");
					}
				}
				qr.update(conn,
						"DELETE FROM folder WHERE path NOT LIKE 'LR2files%' AND path NOT LIKE '%.lr2folder' AND "
								+ dsql.toString());
				qr.update(conn, "DELETE FROM song WHERE " + dsql.toString());
				// 楽曲のタグの保持
				for (SongData record : qr.query(conn, "SELECT md5,tag FROM song WHERE length(tag) > 0", songhandler)) {
					tags.put(record.getMd5(), record.getTag());
				}
				for (Path f : paths) {
					this.processDirectory(conn, f, true);
				}
				while (!tasks.isEmpty()) {
					final BMSFolderThread task = tasks.getFirst();
					if (!task.isAlive()) {
						count += task.count;
						tasks.removeFirst();
					} else {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
					}
				}
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
					+ (count > 0 ? (nowtime - time) / count : "不明"));
		}

		private final ConcurrentLinkedDeque<BMSFolderThread> tasks = new ConcurrentLinkedDeque<BMSFolderThread>();

		private void processDirectory(Connection conn, final Path dir, boolean updateFolder)
				throws IOException, SQLException {
			final List<SongData> records = qr.query(conn, "SELECT path,date FROM song WHERE folder = ?", songhandler,
					SongUtils.crc32(dir.toString(), bmsroot, root.toString()));
			final List<FolderData> folders = qr.query(conn, "SELECT path,date FROM folder WHERE parent = ?",
					folderhandler, SongUtils.crc32(dir.toString(), bmsroot, root.toString()));
			boolean txt = false;
			final List<Path> bmsfiles = new ArrayList<Path>();
			final List<Path> dirs = new ArrayList<Path>();
			String previewpath = null;
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir)) {
				for (Path p : paths) {
					if(Files.isDirectory(p)) {
						dirs.add(p);
					} else {
						final String s = p.toString().toLowerCase();
						if (!txt && s.endsWith(".txt")) {
							txt = true;
						}
						if (previewpath == null) {
							String name = p.getFileName().toString().toLowerCase();
							if(name.startsWith("preview") && (name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".mp3"))) {
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
			if (containsBMS) {
				BMSFolderThread task = new BMSFolderThread(conn, bmsfiles.toArray(new Path[bmsfiles.size()]), records,
						updateFolder, txt, updatetime, previewpath, tags, info);
				tasks.addLast(task);
				task.start();
			}

			for (Path f : dirs) {
				boolean b = true;
				for (int i = folders.size() - 1;i >= 0;i--) {
					final FolderData record = folders.get(i);
					final String s = (f.startsWith(root) ? root.relativize(f).toString() : f.toString())
							+ File.separatorChar;
					if (record.getPath().equals(s)) {
						folders.remove(i);
						if (!updateAll && record.getDate() == Files.getLastModifiedTime(f).toMillis() / 1000) {
							b = false;
						}
						break;
					}
				}
				
				if(!containsBMS) {
					this.processDirectory(conn, f, b);					
				}
			}
			// folderテーブルの更新
			if (updateFolder) {
				final String s = (dir.startsWith(root) ? root.relativize(dir).toString() : dir.toString())
						+ File.separatorChar;
				// System.out.println("folder更新 : " + s);
				qr.update(conn,
						"INSERT OR REPLACE INTO folder (title, subtitle, command, path, type, banner, parent, date, max, adddate)"
								+ "VALUES(?,?,?,?,?,?,?,?,?,?);",
						dir.getFileName().toString(), "", "", s, 1, "",
						SongUtils.crc32(dir.getParent().toString(), bmsroot, root.toString()),
						Files.getLastModifiedTime(dir).toMillis() / 1000, null,
						Calendar.getInstance().getTimeInMillis() / 1000);
			}
			// ディレクトリ内に存在しないフォルダレコードを削除
			for (FolderData record : folders) {
				// System.out.println("Song Database : folder deleted - " +
				// record.getPath());
				qr.update(conn, "DELETE FROM folder WHERE path LIKE ?", record.getPath() + "%");
				qr.update(conn, "DELETE FROM song WHERE path LIKE ?", record.getPath() + "%");
			}
		}
	}

	class BMSFolderThread extends Thread {

		private final Path[] bmsfiles;
		private final List<SongData> records;
		private final boolean updateAll;
		private final boolean txt;
		private final Connection conn;
		private final long updatetime;
		private final String preview;
		private final Map<String, String> tags;
		private int count;
		private final SongInformationAccessor info;

		public BMSFolderThread(Connection conn, Path[] bmsfiles, List<SongData> records, boolean updateAll, boolean txt,
				long updatetime, String preview, Map<String, String> tags, SongInformationAccessor info) {
			this.bmsfiles = bmsfiles;
			this.records = records;
			this.updateAll = updateAll;
			this.txt = txt;
			this.conn = conn;
			this.updatetime = updatetime;
			this.preview = preview;
			this.tags = tags;
			this.info = info;
		}

		public void run() {
			BMSDecoder bmsdecoder = null;
			BMSONDecoder bmsondecoder = null;
			try {
				for (Path path : bmsfiles) {
					boolean b = true;
					final String pathname = (path.startsWith(root) ? root.relativize(path).toString() : path.toString());
					for (int i = records.size() - 1;i >= 0;i--) {
						final SongData record = records.get(i);
						if (record.getPath().equals(pathname)) {
							records.remove(i);
							if (!updateAll && record.getDate() == Files.getLastModifiedTime(path).toMillis() / 1000) {
								b = false;
							}
							break;
						}
					}
					if (b) {
						BMSModel model = null;
						if (pathname.toLowerCase().endsWith(".bmson")) {
							if (bmsondecoder == null) {
								bmsondecoder = new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE);
							}
							model = bmsondecoder.decode(path.toFile());
						} else {
							if (bmsdecoder == null) {
								bmsdecoder = new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
							}
							model = bmsdecoder.decode(path.toFile());
						}

						if (model != null) {
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
								if((sd.getPreview() == null || sd.getPreview().length() == 0) && preview != null) {
									sd.setPreview(preview);
								}
								final String tag = tags.get(sd.getMd5());
								qr.update(conn,
										"INSERT OR REPLACE INTO song "
												+ "(md5, sha256, title, subtitle, genre, artist, subartist, tag, path,"
												+ "folder, stagefile, banner, backbmp, preview, parent, level, difficulty, "
												+ "maxbpm, minbpm, length, mode, judge, feature, content, "
												+ "date, favorite, notes, adddate)"
												+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
										sd.getMd5(), sd.getSha256(), sd.getTitle(), sd.getSubtitle(), sd.getGenre(),
										sd.getArtist(), sd.getSubartist(), tag != null ? tag : "",
										pathname, SongUtils.crc32(path.getParent().toString(), bmsroot, root.toString()),
										sd.getStagefile(), sd.getBanner(), sd.getBackbmp(), sd.getPreview(),
										SongUtils.crc32(path.getParent().getParent().toString(), bmsroot, root.toString()),
										sd.getLevel(), sd.getDifficulty(), sd.getMaxbpm(), sd.getMinbpm(), sd.getLength(),
										sd.getMode(), sd.getJudge(), sd.getFeature(), sd.getContent(),
										Files.getLastModifiedTime(path).toMillis() / 1000, 0, sd.getNotes(), updatetime);
								if(info != null) {
									info.update(model);
								}
								count++;
							} else {
								qr.update(conn, "DELETE FROM song WHERE path = ?", pathname);
							}
						}
					}
				}
				// ディレクトリ内のファイルに存在しないレコードを削除
				for (SongData record : records) {
					qr.update(conn, "DELETE FROM song WHERE path = ?", record.getPath());
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
