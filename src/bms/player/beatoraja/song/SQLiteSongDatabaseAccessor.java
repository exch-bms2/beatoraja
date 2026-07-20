package bms.player.beatoraja.song;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private final SongReviewAccessor reviewdb;

	private final String reviewpath;

	private Map<String, SongReview> songReviewCache = Collections.emptyMap();

	private boolean songReviewCacheLoaded;
	
	private List<SongDatabaseAccessorPlugin> plugins = new ArrayList();
	
	public SQLiteSongDatabaseAccessor(String filepath, String[] bmsroot) throws ClassNotFoundException {
		this(filepath, bmsroot, null);
	}

	public SQLiteSongDatabaseAccessor(String filepath, String[] bmsroot, String reviewpath) throws ClassNotFoundException {
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
		this.reviewpath = reviewpath;
		reviewdb = reviewpath != null && reviewpath.length() > 0 ? new SongReviewAccessor(reviewpath) : null;
		root = Paths.get(".");
		createTable();
		migrateLegacySongReviews();
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
			createIndexes();
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲データベース初期化中の例外:" + e.getMessage());
		}
	}

	private void createIndexes() throws SQLException {
		qr.update("CREATE INDEX IF NOT EXISTS idx_song_folder ON song(folder)");
		qr.update("CREATE INDEX IF NOT EXISTS idx_song_sha256 ON song(sha256)");
		qr.update("CREATE INDEX IF NOT EXISTS idx_folder_parent ON folder(parent)");
	}

	private void migrateLegacySongReviews() {
		if (reviewdb == null) {
			return;
		}
		try {
			List<SongData> legacyReviews = qr.query(
					"SELECT sha256, tag, favorite FROM song WHERE (tag IS NOT NULL AND tag != '') OR favorite > 0",
					songhandler);
			Map<String, SongReview> existingReviews = reviewdb.getSongReviews();
			List<SongData> migrationTargets = new ArrayList<>();
			for (SongData legacyReview : legacyReviews) {
				SongReview existingReview = existingReviews.get(legacyReview.getSha256());
				SongReview review = existingReview != null ? existingReview : new SongReview();
				review.setSha256(legacyReview.getSha256());
				boolean migrate = false;
				if ((review.getTag() == null || review.getTag().length() == 0)
						&& legacyReview.getTag() != null && legacyReview.getTag().length() > 0) {
					review.setTag(legacyReview.getTag());
					migrate = true;
				}
				if (review.getFavorite() == 0 && legacyReview.getFavorite() > 0) {
					review.setFavorite(legacyReview.getFavorite());
					migrate = true;
				}
				if (migrate) {
					legacyReview.setSongReview(review);
					migrationTargets.add(legacyReview);
				}
			}
			reviewdb.setSongReviews(migrationTargets.toArray(new SongData[0]));
			qr.update("UPDATE song SET favorite = 0 WHERE favorite > 0");
			songReviewCacheLoaded = false;
		} catch (SQLException e) {
			Logger.getGlobal().severe("楽曲評価データ移行中の例外:" + e.getMessage());
		}
	}

	private SongData[] toSongDataArray(List<SongData> songs) {
		List<SongData> validSongs = Validatable.removeInvalidElements(songs);
		applySongReviews(validSongs);
		return validSongs.toArray(new SongData[validSongs.size()]);
	}

	private void applySongReviews(List<SongData> songs) {
		if (songs == null || songs.size() == 0) {
			return;
		}
		Map<String, SongReview> reviews = reviewdb != null ? getSongReviews() : Collections.emptyMap();
		for (SongData song : songs) {
			SongReview review = reviews.get(song.getSha256());
			if (review == null) {
				review = new SongReview();
				review.setSha256(song.getSha256());
			}
			song.setSongReview(review);
		}
	}

	private Map<String, SongReview> getSongReviews() {
		if (reviewdb == null) {
			return Collections.emptyMap();
		}
		if (!songReviewCacheLoaded) {
			songReviewCache = reviewdb.getSongReviews();
			songReviewCacheLoaded = true;
		}
		return songReviewCache;
	}

	private String getReviewedSongSource() {
		if (reviewpath == null || reviewpath.length() == 0) {
			return "song";
		}
		return "(SELECT md5, song.sha256 AS sha256, title, subtitle, genre, artist, subartist,"
				+ "COALESCE(review.tag, '') AS tag, path, folder, stagefile, banner, backbmp, preview, parent,"
				+ "level, difficulty, maxbpm, minbpm, length, mode, judge, feature, content, song.date AS date,"
				+ "COALESCE(review.favorite, 0) AS favorite, adddate, notes, charthash"
				+ " FROM song LEFT OUTER JOIN reviewdb.review AS review ON song.sha256 = review.sha256)";
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
			return toSongDataArray(m);
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
				int aIndexSha256 = -1,aIndexMd5 = -1,bIndexSha256 = -1,bIndexMd5 = -1;
				for(int i = 0;i < hashes.length;i++) {
					if(hashes[i].equals(a.getSha256())) aIndexSha256 = i;
					if(hashes[i].equals(a.getMd5())) aIndexMd5 = i;
					if(hashes[i].equals(b.getSha256())) bIndexSha256 = i;
					if(hashes[i].equals(b.getMd5())) bIndexMd5 = i;
				}
			    int aIndex = Math.min((aIndexSha256 == -1 ? Integer.MAX_VALUE : aIndexSha256), (aIndexMd5 == -1 ? Integer.MAX_VALUE : aIndexMd5));
			    int bIndex = Math.min((bIndexSha256 == -1 ? Integer.MAX_VALUE : bIndexSha256), (bIndexMd5 == -1 ? Integer.MAX_VALUE : bIndexMd5));
			    return bIndex - aIndex;
            }).collect(Collectors.toList());

			return toSongDataArray(sorted);
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
			if (reviewpath != null && reviewpath.length() > 0) {
				stmt.execute("ATTACH DATABASE '" + reviewpath + "' as reviewdb");
			}
			List<SongData> m;
			String songSource = getReviewedSongSource();

			if(info != null) {
				stmt.execute("ATTACH DATABASE '" + info + "' as infodb");
				String s = "SELECT DISTINCT md5, song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,song.level AS level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length, charthash"
						+ " FROM " + songSource + " AS song INNER JOIN (information LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON information.sha256 = score.sha256) "
						+ "ON song.sha256 = information.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
//				System.out.println(s + " -> result : " + m.size());
				stmt.execute("DETACH DATABASE infodb");
			} else {
				String s = "SELECT DISTINCT md5, song.sha256 AS sha256, title, subtitle, genre, artist, subartist,path,folder,stagefile,banner,backbmp,parent,song.level AS level,difficulty,"
						+ "maxbpm,minbpm,song.mode AS mode, judge, feature, content, song.date AS date, favorite, song.notes AS notes, adddate, preview, length, charthash"
						+ " FROM " + songSource + " AS song LEFT OUTER JOIN (score LEFT OUTER JOIN scorelog ON score.sha256 = scorelog.sha256) ON song.sha256 = score.sha256 WHERE " + sql;
				ResultSet rs = stmt.executeQuery(s);
				m = songhandler.handle(rs);
			}
			if (reviewpath != null && reviewpath.length() > 0) {
				stmt.execute("DETACH DATABASE reviewdb");
			}
			stmt.execute("DETACH DATABASE scorelogdb");				
			stmt.execute("DETACH DATABASE scoredb");
			return toSongDataArray(m);
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
			return toSongDataArray(m);
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
		if (reviewdb != null) {
			reviewdb.setSongReviews(songs);
			songReviewCacheLoaded = false;
		}
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

	public void setSongReviews(SongData[] songs) {
		if (reviewdb != null) {
			reviewdb.setSongReviews(songs);
			songReviewCacheLoaded = false;
		} else {
			for (SongData song : songs) {
				if (song != null) {
					song.setFavorite(song.getSongReview().getFavorite());
				}
			}
			setSongDatas(songs);
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
		updater.updateSongDatas(path == null ? Stream.of(bmsroot).map(p -> Paths.get(p)) : Stream.of(Paths.get(path)));
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
		public void updateSongDatas(Stream<Path> paths) {
			long time = System.currentTimeMillis();
			SongDatabaseUpdaterProperty property = new SongDatabaseUpdaterProperty(Calendar.getInstance().getTimeInMillis() / 1000, info);
			property.count.set(0);
			if(info != null) {
				info.startUpdate();
			}
			try (Connection conn = ds.getConnection(); property) {
				property.conn = conn;
				conn.setAutoCommit(false);
				property.prepareStatements();
				// 楽曲のタグ,FAVORITEの保持
				if (reviewdb != null) {
					for (Map.Entry<String, SongReview> entry : getSongReviews().entrySet()) {
						SongReview review = entry.getValue();
						if (review.getTag() != null && review.getTag().length() > 0) {
							property.tags.put(entry.getKey(), review.getTag());
						}
					}
				} else {
					for (SongData record : qr.query(conn, "SELECT sha256, tag, favorite FROM song", songhandler)) {
						if (record.getTag() != null && record.getTag().length() > 0) {
							property.tags.put(record.getSha256(), record.getTag());
						}
						if (record.getFavorite() > 0) {
							property.favorites.put(record.getSha256(), record.getFavorite());
						}
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

				paths.forEach((p) -> {
					try {
						BMSFolder folder = scanDirectory(p);
						folder.processDirectory(property);
					} catch (IOException | SQLException | IllegalArgumentException | ReflectiveOperationException | IntrospectionException e) {
						Logger.getGlobal().severe("楽曲データベース更新時の例外:" + e.getMessage());
					}
				});
				property.flush();
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

		private BMSFolder scanDirectory(Path start) throws IOException {
			Map<Path, BMSFolder> folders = new LinkedHashMap<>();
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					BMSFolder folder = new BMSFolder(dir, bmsroot, attrs.lastModifiedTime().toMillis() / 1000);
					folders.put(dir, folder);

					Path parent = dir.getParent();
					BMSFolder parentFolder = parent != null ? folders.get(parent) : null;
					if (parentFolder != null) {
						parentFolder.dirs.add(folder);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					BMSFolder parent = folders.get(file.getParent());
					if (parent != null) {
						parent.addFile(file, attrs.lastModifiedTime().toMillis() / 1000);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) {
					Logger.getGlobal().warning("楽曲ファイル探索中の例外:" + file + " : " + e.getMessage());
					return FileVisitResult.CONTINUE;
				}
			});

			BMSFolder rootFolder = folders.get(start);
			if (rootFolder == null) {
				throw new IOException("楽曲フォルダを読み込めません : " + start);
			}
			return rootFolder;
		}

	}
	
	private class BMSFolder {
		
		public final Path path;
		public boolean updateFolder = true;
		private boolean txt = false;
		private final List<ChartFile> bmsfiles = new ArrayList<ChartFile>();
		private final List<BMSFolder> dirs = new ArrayList<BMSFolder>();
		private String previewpath = null;
		private final String[] bmsroot;
		private final long lastModifiedTime;

		public BMSFolder(Path path, String[] bmsroot) {
			this(path, bmsroot, -1);
		}

		public BMSFolder(Path path, String[] bmsroot, long lastModifiedTime) {
			this.path = path;
			this.bmsroot = bmsroot;
			this.lastModifiedTime = lastModifiedTime;
		}

		private void addFile(Path file, long lastModifiedTime) {
			final String s = file.getFileName().toString().toLowerCase(Locale.ROOT);
			if (!txt && s.endsWith(".txt")) {
				txt = true;
			}
			if (previewpath == null && isPreviewFile(s)) {
				previewpath = file.getFileName().toString();
			}
			if (isChartFile(s)) {
				bmsfiles.add(new ChartFile(file, lastModifiedTime));
			}
		}
		
		private void processDirectory(SongDatabaseUpdaterProperty property)
				throws IOException, SQLException, ReflectiveOperationException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
			final List<SongData> records = qr.query(property.conn, "SELECT path, date, preview FROM song WHERE folder = ?", songhandler,
					SongUtils.crc32(path.toString(), bmsroot, root.toString()));
			final List<FolderData> folders = qr.query(property.conn, "SELECT path,date FROM folder WHERE parent = ?",
					folderhandler, SongUtils.crc32(path.toString(), bmsroot, root.toString()));

			final boolean containsBMS = bmsfiles.size() > 0;
			property.count.addAndGet(this.processBMSFolder(records, property));

			final int len = folders.size();
			dirs.forEach(bf -> {
				final String s = (bf.path.startsWith(root) ? root.relativize(bf.path).toString() : bf.path.toString())
						+ File.separatorChar;
				for (int i = 0; i < len;i++) {
					final FolderData record = folders.get(i);
					if (record != null && record.getPath().equals(s)) {
//						long t = System.nanoTime();
						folders.set(i, null);
//						System.out.println(System.nanoTime() - t);
						if (record.getDate() == bf.getLastModifiedTime()) {
							bf.updateFolder = false;
						}
						break;
					}
				}
			});

			if(!containsBMS) {
				dirs.forEach(bf -> {
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
				folder.setDate((int) getLastModifiedTime());
				folder.setAdddate((int) property.updatetime);
				
				property.addFolder(folder);
			}
			// ディレクトリ内に存在しないフォルダレコードを削除
			for (FolderData folder : folders) {
				if (folder == null) {
					continue;
				}
				try {
					// System.out.println("Song Database : folder deleted - " +
					// record.getPath());
					qr.update(property.conn, "DELETE FROM folder WHERE path LIKE ?", folder.getPath() + "%");
					qr.update(property.conn, "DELETE FROM song WHERE path LIKE ?", folder.getPath() + "%");
				} catch (SQLException e) {
					Logger.getGlobal().severe("ディレクトリ内に存在しないフォルダレコード削除の例外:" + e.getMessage());
				}
			}
		}
		
		private int processBMSFolder(List<SongData> records, SongDatabaseUpdaterProperty property) {
			record UpdateTarget(Path path, String pathname, long lastModifiedTime) {}
			record DecodedSong(UpdateTarget target, BMSModel model, SongData song) {}

			List<UpdateTarget> targets = new ArrayList<>();
			final int len = records.size();
			for (ChartFile chart : bmsfiles) {
				Path chartPath = chart.path();
				long lastModifiedTime = chart.lastModifiedTime();
				boolean update = true;
				final String pathname = (chartPath.startsWith(root) ? root.relativize(chartPath).toString() : chartPath.toString());
				for (int i = 0;i < len;i++) {
					final SongData record = records.get(i);
					if (record != null && record.getPath().equals(pathname)) {
						records.set(i, null);
						if (record.getDate() == lastModifiedTime) {
							update = false;

							String oldpp = record.getPreview() == null ? "" : record.getPreview();
							String newpp = previewpath == null ? "" : previewpath;
							if (!oldpp.equals(newpp)) {
								try {
									qr.update(property.conn, "UPDATE song SET preview=? WHERE path=?", newpp, pathname);
								} catch (SQLException e) {
									Logger.getGlobal().warning("Error while updating preview at " + pathname + ": " + e.getMessage());
								}
							}
						}
						break;
					}
				}
				if (!update) {
					continue;
				}
				targets.add(new UpdateTarget(chartPath, pathname, lastModifiedTime));
			}

			List<DecodedSong> decodedSongs = targets.parallelStream().map(target -> {
				BMSModel model = decode(target.path(), target.pathname());
				return new DecodedSong(target, model, model != null ? new SongData(model, txt) : null);
			}).collect(Collectors.toList());

			int count = 0;
			for (DecodedSong decoded : decodedSongs) {
				BMSModel model = decoded.model();
				if (model == null || decoded.song() == null) {
					continue;
				}
				final SongData sd = decoded.song();
				if (sd.getNotes() != 0 || model.getWavList().length != 0) {
					if (sd.getDifficulty() == 0) {
						final String fulltitle = (sd.getTitle() + sd.getSubtitle()).toLowerCase();
						final String diffname = (sd.getSubtitle()).toLowerCase();
						if (diffname.contains("beginner")) {
							sd.setDifficulty(1);
						} else if (diffname.contains("normal")) {
							sd.setDifficulty(2);
						} else if (diffname.contains("hyper")) {
							sd.setDifficulty(3);
						} else if (diffname.contains("another")) {
							sd.setDifficulty(4);
						} else if (diffname.contains("insane") || diffname.contains("leggendaria")) {
							sd.setDifficulty(5);
						} else {
							if (fulltitle.contains("beginner")) {
								sd.setDifficulty(1);
							} else if (fulltitle.contains("normal")) {
								sd.setDifficulty(2);
							} else if (fulltitle.contains("hyper")) {
								sd.setDifficulty(3);
							} else if (fulltitle.contains("another")) {
								sd.setDifficulty(4);
							} else if (fulltitle.contains("insane") || fulltitle.contains("leggendaria")) {
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
					}
					if((sd.getPreview() == null || sd.getPreview().length() == 0) && previewpath != null) {
						sd.setPreview(previewpath);
					}
					final String tag = property.tags.get(sd.getSha256());
					final Integer favorite = property.favorites.get(sd.getSha256());
					
					for(SongDatabaseAccessorPlugin plugin : plugins) {
						plugin.update(model, sd);
					}

					sd.setTag(tag != null ? tag : "");
					sd.setPath(decoded.target().pathname());
					sd.setFolder(SongUtils.crc32(decoded.target().path().getParent().toString(), bmsroot, root.toString()));
					sd.setParent(SongUtils.crc32(decoded.target().path().getParent().getParent().toString(), bmsroot, root.toString()));
					sd.setDate((int) decoded.target().lastModifiedTime());
					sd.setFavorite(favorite != null ? favorite.intValue() : 0);
					sd.setAdddate((int) property.updatetime);
					try {
						property.addSong(sd);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					if(property.info != null) {
						property.info.update(model);
					}
					count++;
				} else {
					try {
						qr.update(property.conn, "DELETE FROM song WHERE path = ?", decoded.target().pathname());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			// ディレクトリ内のファイルに存在しないレコードを削除
			for (SongData record : records) {
				if (record == null) {
					continue;
				}
				try {
					qr.update(property.conn, "DELETE FROM song WHERE path = ?", record.getPath());
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			return count;
		}

		private BMSModel decode(Path path, String pathname) {
			try {
				if (pathname.toLowerCase(Locale.ROOT).endsWith(".bmson")) {
					return new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE).decode(path);
				}
				return new BMSDecoder(BMSModel.LNTYPE_LONGNOTE).decode(path);
			} catch (Exception e) {
				Logger.getGlobal().severe("Error while decoding chart at path: " + pathname + e.getMessage());
				return null;
			}
		}

		private long getLastModifiedTime() {
			if (lastModifiedTime >= 0) {
				return lastModifiedTime;
			}
			try {
				return Files.getLastModifiedTime(path).toMillis() / 1000;
			} catch (IOException e) {
				return -1;
			}
		}

		private boolean isPreviewFile(String filename) {
			return filename.startsWith("preview") && (filename.endsWith(".wav")
					|| filename.endsWith(".ogg")
					|| filename.endsWith(".mp3")
					|| filename.endsWith(".flac"));
		}

		private boolean isChartFile(String filename) {
			return filename.endsWith(".bms")
					|| filename.endsWith(".bme")
					|| filename.endsWith(".bml")
					|| filename.endsWith(".pms")
					|| filename.endsWith(".bmson");
		}
	}

	private record ChartFile(Path path, long lastModifiedTime) {}
	
	private static class SongDatabaseUpdaterProperty implements AutoCloseable {
		private static final int BATCH_SIZE = 500;

		private final Map<String, String> tags = new HashMap<String, String>();
		private final Map<String, Integer> favorites = new HashMap<String, Integer>();
		private final SongInformationAccessor info;
		private final long updatetime;
		private final AtomicInteger count = new AtomicInteger();
		private Connection conn;
		private PreparedStatement folderInsertStatement;
		private PreparedStatement songInsertStatement;
		private int folderBatchCount;
		private int songBatchCount;
		
		public SongDatabaseUpdaterProperty(long updatetime, SongInformationAccessor info) {
			this.updatetime = updatetime;
			this.info = info;
		}

		private void prepareStatements() throws SQLException {
			folderInsertStatement = conn.prepareStatement("INSERT OR REPLACE INTO folder "
					+ "(title,subtitle,command,path,banner,parent,type,date,adddate,max) "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?)");
			songInsertStatement = conn.prepareStatement("INSERT OR REPLACE INTO song "
					+ "(md5,sha256,title,subtitle,genre,artist,subartist,tag,path,folder,stagefile,banner,backbmp,preview,parent,"
					+ "level,difficulty,maxbpm,minbpm,length,mode,judge,feature,content,date,favorite,adddate,notes,charthash) "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		}

		private void addFolder(FolderData folder) throws SQLException {
			folderInsertStatement.setString(1, folder.getTitle());
			folderInsertStatement.setString(2, folder.getSubtitle());
			folderInsertStatement.setString(3, folder.getCommand());
			folderInsertStatement.setString(4, folder.getPath());
			folderInsertStatement.setString(5, folder.getBanner());
			folderInsertStatement.setString(6, folder.getParent());
			folderInsertStatement.setInt(7, folder.getType());
			folderInsertStatement.setInt(8, folder.getDate());
			folderInsertStatement.setInt(9, folder.getAdddate());
			folderInsertStatement.setInt(10, folder.getMax());
			folderInsertStatement.addBatch();
			if (++folderBatchCount >= BATCH_SIZE) {
				flushFolders();
			}
		}

		private void addSong(SongData song) throws SQLException {
			songInsertStatement.setString(1, song.getMd5());
			songInsertStatement.setString(2, song.getSha256());
			songInsertStatement.setString(3, song.getTitle());
			songInsertStatement.setString(4, song.getSubtitle());
			songInsertStatement.setString(5, song.getGenre());
			songInsertStatement.setString(6, song.getArtist());
			songInsertStatement.setString(7, song.getSubartist());
			songInsertStatement.setString(8, song.getTag());
			songInsertStatement.setString(9, song.getPath());
			songInsertStatement.setString(10, song.getFolder());
			songInsertStatement.setString(11, song.getStagefile());
			songInsertStatement.setString(12, song.getBanner());
			songInsertStatement.setString(13, song.getBackbmp());
			songInsertStatement.setString(14, song.getPreview());
			songInsertStatement.setString(15, song.getParent());
			songInsertStatement.setInt(16, song.getLevel());
			songInsertStatement.setInt(17, song.getDifficulty());
			songInsertStatement.setInt(18, song.getMaxbpm());
			songInsertStatement.setInt(19, song.getMinbpm());
			songInsertStatement.setInt(20, song.getLength());
			songInsertStatement.setInt(21, song.getMode());
			songInsertStatement.setInt(22, song.getJudge());
			songInsertStatement.setInt(23, song.getFeature());
			songInsertStatement.setInt(24, song.getContent());
			songInsertStatement.setInt(25, song.getDate());
			songInsertStatement.setInt(26, song.getFavorite());
			songInsertStatement.setInt(27, song.getAdddate());
			songInsertStatement.setInt(28, song.getNotes());
			songInsertStatement.setString(29, song.getCharthash());
			songInsertStatement.addBatch();
			if (++songBatchCount >= BATCH_SIZE) {
				flushSongs();
			}
		}

		private void flush() throws SQLException {
			flushFolders();
			flushSongs();
		}

		private void flushFolders() throws SQLException {
			if (folderBatchCount > 0) {
				folderInsertStatement.executeBatch();
				folderBatchCount = 0;
			}
		}

		private void flushSongs() throws SQLException {
			if (songBatchCount > 0) {
				songInsertStatement.executeBatch();
				songBatchCount = 0;
			}
		}

		@Override
		public void close() throws SQLException {
			SQLException failure = null;
			try {
				if (folderInsertStatement != null) {
					folderInsertStatement.close();
				}
			} catch (SQLException e) {
				failure = e;
			}
			try {
				if (songInsertStatement != null) {
					songInsertStatement.close();
				}
			} catch (SQLException e) {
				if (failure != null) {
					failure.addSuppressed(e);
				} else {
					failure = e;
				}
			}
			if (failure != null) {
				throw failure;
			}
		}
	}
	
	public static interface SongDatabaseAccessorPlugin {
		
		public void update(BMSModel model, SongData song);
	}
}
