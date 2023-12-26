package bms.player.beatoraja.song;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import bms.player.beatoraja.SQLiteDatabaseAccessor;
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
public class SQLiteSongDatabaseAccessor extends SQLiteDatabaseAccessor implements SongDatabaseAccessor {

	private SQLiteDataSource ds;

	private final Path root;

	private final ResultSetHandler<List<SongData>> songhandler = new BeanListHandler<SongData>(SongData.class);
	private final ResultSetHandler<List<FolderData>> folderhandler = new BeanListHandler<FolderData>(FolderData.class);

	private final QueryRunner qr;
	
	private List<SongDatabaseAccessorPlugin> plugins = new ArrayList();
	
	public SQLiteSongDatabaseAccessor(String filepath, String[] bmsroot) throws ClassNotFoundException {
		super(new Table("folder", 
				new Column("title", "TEXT"),
				new Column("subtitle", "TEXT"),
				new Column("command", "TEXT"),
				new Column("path", "TEXT", 0, 1),
				new Column("banner", "TEXT"),
				new Column("parent", "TEXT"),
				new Column("type", "INTEGER"),
				new Column("date", "INTEGER"),
				new Column("adddate", "INTEGER"),
				new Column("max", "INTEGER")
				),
				new Table("song",
						new Column("md5", "TEXT", 1, 0),
						new Column("sha256", "TEXT", 1, 0),
						new Column("title", "TEXT"),
						new Column("subtitle", "TEXT"),
						new Column("genre", "TEXT"),
						new Column("artist", "TEXT"),
						new Column("subartist", "TEXT"),
						new Column("tag", "TEXT"),
						new Column("path", "TEXT", 0, 1),
						new Column("folder", "TEXT"),
						new Column("stagefile", "TEXT"),
						new Column("banner", "TEXT"),
						new Column("backbmp", "TEXT"),
						new Column("preview", "TEXT"),
						new Column("parent", "TEXT"),
						new Column("level", "INTEGER"),
						new Column("difficulty", "INTEGER"),
						new Column("maxbpm", "INTEGER"),
						new Column("minbpm", "INTEGER"),
						new Column("length", "INTEGER"),
						new Column("mode", "INTEGER"),
						new Column("judge", "INTEGER"),
						new Column("feature", "INTEGER"),
						new Column("content", "INTEGER"),
						new Column("date", "INTEGER"),
						new Column("favorite", "INTEGER"),
						new Column("adddate", "INTEGER"),
						new Column("notes", "INTEGER"),
						new Column("charthash", "TEXT")
						));
		
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig conf = new SQLiteConfig();
		conf.setSharedCache(true);
		conf.setSynchronous(SynchronousMode.OFF);
		// conf.setJournalMode(JournalMode.MEMORY);
		ds = new SQLiteDataSource(conf);
		ds.setUrl("jdbc:sqlite:" + filepath);
		qr = new QueryRunner(ds);
		root = Paths.get(".");
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
			validate(qr);
			
			if(qr.query("PRAGMA TABLE_INFO(song)", new MapListHandler()).stream().anyMatch(m -> m.get("name").equals("sha256") && (int)(m.get("pk")) == 1)) {
				qr.update("ALTER TABLE [song] RENAME TO [old_song]");
				validate(qr);
				qr.update("INSERT INTO song SELECT "
						+ "md5, sha256, title, subtitle, genre, artist, subartist, tag, path,"
						+ "folder, stagefile, banner, backbmp, preview, parent, level, difficulty,"
						+ "maxbpm, minbpm, length, mode, judge, feature, content,"
						+ "date, favorite, notes, adddate, charthash "
						+ "FROM old_song GROUP BY path HAVING MAX(adddate)");
				qr.update("DROP TABLE old_song");
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
			
			// 検索並び順保持
			List<SongData> sorted = m.stream().sorted((a, b) -> {
			    int aIndexSha256 = Arrays.asList(hashes).indexOf(a.getSha256());
			    int aIndexMd5 = Arrays.asList(hashes).indexOf(a.getMd5());
			    int bIndexSha256 = Arrays.asList(hashes).indexOf(b.getSha256());
			    int bIndexMd5 = Arrays.asList(hashes).indexOf(b.getMd5());
			    int aIndex = Math.min((aIndexSha256 == -1 ? Integer.MAX_VALUE : aIndexSha256), (aIndexMd5 == -1 ? Integer.MAX_VALUE : aIndexMd5));
			    int bIndex = Math.min((bIndexSha256 == -1 ? Integer.MAX_VALUE : bIndexSha256), (bIndexMd5 == -1 ? Integer.MAX_VALUE : bIndexMd5));
			    return bIndex - aIndex;
            }).collect(Collectors.toList());

			SongData[] validated = Validatable.removeInvalidElements(sorted).toArray(new SongData[m.size()]);
			return validated;
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
				String s = "SELECT DISTINCT md5, song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length, charthash"
						+ " FROM song INNER JOIN (information LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON information.sha256 = score.sha256) "
						+ "ON song.sha256 = information.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
//				System.out.println(s + " -> result : " + m.size());
				stmt.execute("DETACH DATABASE infodb");
			} else {
				String s = "SELECT DISTINCT md5, song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,level,difficulty,"
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
							+ " GROUP BY sha256",songhandler, "%" + text + "%");
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
				this.insert(qr, conn, "song", sd);
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
	public void updateSongDatas(String path, String[] bmsroot, boolean updateAll, SongInformationAccessor info) {
		if(bmsroot == null || bmsroot.length == 0) {
			Logger.getGlobal().warning("楽曲ルートフォルダが登録されていません");
			return;
		}
		SongDatabaseUpdater updater = new SongDatabaseUpdater(updateAll, bmsroot, info);
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
		private final String[] bmsroot;

		private SongInformationAccessor info;

		public SongDatabaseUpdater(boolean updateAll, String[] bmsroot, SongInformationAccessor info) {
			this.updateAll = updateAll;
			this.bmsroot = bmsroot;
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
			SongDatabaseUpdaterProperty property = new SongDatabaseUpdaterProperty(Calendar.getInstance().getTimeInMillis() / 1000, info);
			property.count.set(0);
			if(info != null) {
				info.startUpdate();
			}
			try (Connection conn = ds.getConnection()) {
				property.conn = conn;
				conn.setAutoCommit(false);
				// 楽曲のタグ,FAVORITEの保持
				for (SongData record : qr.query(conn, "SELECT sha256, tag, favorite FROM song", songhandler)) {
					if (record.getTag().length() > 0) {
						property.tags.put(record.getSha256(), record.getTag());
					}
					if (record.getFavorite() > 0) {
						property.favorites.put(record.getSha256(), record.getFavorite());
					}
				}
				if(updateAll) {
					qr.update(conn, "DELETE FROM folder");					
					qr.update(conn, "DELETE FROM song");
				} else {
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
				}
				
				Arrays.asList(paths).parallelStream().forEach((p) -> {
					try {
						BMSFolder folder = new BMSFolder(p, bmsroot);
						folder.processDirectory(property);
					} catch (IOException | SQLException | IllegalArgumentException | ReflectiveOperationException | IntrospectionException e) {
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
		private final String[] bmsroot;

		public BMSFolder(Path path, String[] bmsroot) {
			this.path = path;
			this.bmsroot = bmsroot;
		}
		
		private void processDirectory(SongDatabaseUpdaterProperty property)
				throws IOException, SQLException, ReflectiveOperationException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
			final List<SongData> records = qr.query(property.conn, "SELECT path,date FROM song WHERE folder = ?", songhandler,
					SongUtils.crc32(path.toString(), bmsroot, root.toString()));
			final List<FolderData> folders = qr.query(property.conn, "SELECT path,date FROM folder WHERE parent = ?",
					folderhandler, SongUtils.crc32(path.toString(), bmsroot, root.toString()));
			try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
				for (Path p : paths) {
					if(Files.isDirectory(p)) {
						dirs.add(new BMSFolder(p, bmsroot));
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
						if (record.getDate() == Files.getLastModifiedTime(bf.path).toMillis() / 1000) {
							bf.updateFolder = false;
						}
						break;
					}
				}
			}
			
			if(!containsBMS) {
				dirs.parallelStream().forEach((bf) -> {
					try {
						bf.processDirectory(property);
					} catch (IOException | SQLException | IllegalArgumentException | ReflectiveOperationException | IntrospectionException e) {
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
				FolderData folder = new FolderData();
				folder.setTitle(path.getFileName().toString());
				folder.setPath(s);
				folder.setParent(SongUtils.crc32(parentpath.toString() , bmsroot, root.toString()));
				folder.setDate((int) (Files.getLastModifiedTime(path).toMillis() / 1000));
				folder.setAdddate((int) property.updatetime);
				
				SQLiteSongDatabaseAccessor.this.insert(qr, property.conn, "folder", folder);			
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
							if (record.getDate() == Files.getLastModifiedTime(path).toMillis() / 1000) {
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
					try {
						model = bmsondecoder.decode(path);
					} catch (Exception e) {
						Logger.getGlobal().severe("Error while decoding bmson at path: " + pathname + e.getMessage());
					}
				} else {
					if (bmsdecoder == null) {
						bmsdecoder = new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
					}
					try {
						model = bmsdecoder.decode(path);
					} catch (Exception e) {
						Logger.getGlobal().severe("Error while decoding bms at path: " + pathname + e.getMessage());
					}
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
					final String tag = property.tags.get(sd.getSha256());
					final Integer favorite = property.favorites.get(sd.getSha256());
					
					for(SongDatabaseAccessorPlugin plugin : plugins) {
						plugin.update(model, sd);
					}
					
					
					try {
						sd.setTag(tag != null ? tag : "");
						sd.setPath(pathname);
						sd.setFolder(SongUtils.crc32(path.getParent().toString(), bmsroot, root.toString()));
						sd.setParent(SongUtils.crc32(path.getParent().getParent().toString(), bmsroot, root.toString()));
						sd.setDate((int) (Files.getLastModifiedTime(path).toMillis() / 1000));
						sd.setFavorite(favorite != null ? favorite.intValue() : 0);
						sd.setAdddate((int) property.updatetime);
						SQLiteSongDatabaseAccessor.this.insert(qr, property.conn, "song", sd);
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
		private final AtomicInteger count = new AtomicInteger();
		private Connection conn;
		
		public SongDatabaseUpdaterProperty(long updatetime, SongInformationAccessor info) {
			this.updatetime = updatetime;
			this.info = info;
		}

	}
	
	public static interface SongDatabaseAccessorPlugin {
		
		public void update(BMSModel model, SongData song);
	}
}
