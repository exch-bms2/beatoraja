package bms.player.beatoraja;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bms.model.BMSDecoder;
import bms.model.Mode;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.song.SongData;
import bms.table.Course.Trophy;
import bms.table.*;

/**
 * 難易度表データアクセス用クラス
 * 
 * @author exch
 */
public class TableDataAccessor {
	
	private final String tabledir;

	public TableDataAccessor(String tabledir) {
		this.tabledir = tabledir;
	}

	public void updateTableData(String[] urls) {
		Arrays.stream(urls).parallel().forEach(url -> {
            TableAccessor tr = new DifficultyTableAccessor(tabledir, url);
            TableData td = tr.read();
            if(td != null) {
                write(td);
            }
		});		
	}

	public void loadNewTableData(String[] urls) {
		Set<String> localTables = getLocalTableFilenames();
		Arrays.stream(urls).parallel().forEach(url -> {
			if (localTables.contains(getFileName(url) + ".bmt")) {
				return;
			}
            TableAccessor tr = new DifficultyTableAccessor(tabledir, url);
            TableData td = tr.read();
            if(td != null) {
                write(td);
            }
		});		
	}

	private Set<String> getLocalTableFilenames() {
		try (Stream<Path> paths = Files.list(Paths.get(tabledir))) {
			return paths.map(p -> p.getFileName().toString())
					.filter(s -> s.toLowerCase().endsWith(".bmt")).collect(Collectors.toSet());
		} catch (IOException e) {
			return null;
		}
	}

	public HashMap<String,String> readLocalTableNames(String[] urls) {
		HashMap<String,String> fileNameToTableNameMap = new HashMap<>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				String fileName = p.getFileName().toString();
				if (!fileName.endsWith(".bmt")) continue;
				TableData td = TableData.read(p);
				if (td == null) continue;
				fileNameToTableNameMap.put(fileName, td.getName());
			}
		} catch (IOException e) {
			return null;
		}
		HashMap<String,String> urlToTableNameMap = new HashMap<>();
		for (String url : urls) {
			urlToTableNameMap.put(url, fileNameToTableNameMap.get(getFileName(url) + ".bmt"));
		}
		return urlToTableNameMap;
	}
	
	/**
	 * 難易度表データをキャッシュする
	 * 
	 * @param td 難易度表データ
	 */
	public void write(TableData td) {
		TableData.write(Paths.get(tabledir + "/" + getFileName(td.getUrl()) + ".bmt"), td);
	}

	public void write(TableData td, String filename) {
		TableData.write(Paths.get(tabledir + "/" + filename), td);
	}

	/**
	 * 全てのキャッシュされた難易度表データを読み込む
	 * 
	 * @return 全てのキャッシュされた難易度表データ
	 */
	public TableData[] readAll() {
		try (Stream<Path> paths = Files.list(Paths.get(tabledir))) {
			return paths.map(p -> TableData.read(p)).filter(Objects::nonNull).toArray(TableData[]::new);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new TableData[0];
	}

	/**
	 * 指定のキャッシュされた難易度表データを読み込む
	 * 
	 * @param url 難易度表URL
	 * @return キャッシュされた難易度表データ。存在しない場合はnull
	 */
	public TableData readCache(String url) {
		TableData td = null;
		try (Stream<Path> paths = Files.list(Paths.get(tabledir))) {
			return paths.filter(p -> p.getFileName().toString().equals(getFileName(url) + ".bmt")).findFirst()
					.map(p -> TableData.read(p)).orElse(null);
		} catch (IOException e) {
			return null;
		}
	}
	
	public TableData read(String filename) {
		return TableData.read(Paths.get(tabledir + "/" + filename));
	}

	private String getFileName(String name) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(name.getBytes());
			return BMSDecoder.convertHexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static abstract class TableAccessor {

		public final String name;

		public TableAccessor(String name) {
			this.name = name;
		}

		public abstract TableData read();
		public abstract void write(TableData td);
	}

	public static class DifficultyTableAccessor extends TableAccessor {

		private String tabledir;
		private String url;

		public DifficultyTableAccessor(String tabledir, String url) {
			super(url);
			this.tabledir = tabledir;
			this.url = url;
		}

		@Override
		public TableData read() {
			DifficultyTableParser dtp = new DifficultyTableParser();
			DifficultyTable dt = new DifficultyTable();
			if (url.endsWith(".json")) {
				dt.setHeadURL(url);
			} else {
				dt.setSourceURL(url);
			}
			try {
				dtp.decode(true, dt);
				TableData td = new TableData();
				td.setUrl(url);
				td.setName(dt.getName());
				td.setTag(dt.getTag());
				Mode defaultMode = dt.getMode() != null ? Mode.getMode(dt.getMode()) : null;
				td.setFolder(Stream.of(dt.getLevelDescription()).map(lv -> {
					TableData.TableFolder tde = new TableData.TableFolder();
					tde.setName(td.getTag() + lv);
					tde.setSong(Stream.of(dt.getElements()).filter(dte -> lv.equals(dte.getLevel()))
							.map(dte -> toSongData(dte, defaultMode)).toArray(SongData[]::new));
					return tde;
				}).toArray(TableData.TableFolder[]::new));

				if (dt.getCourse() != null && dt.getCourse().length > 0) {
					td.setCourse(Stream.of(dt.getCourse()).flatMap(courses -> Stream.of(courses)).map(g -> {
						CourseData cd = new CourseData();
						cd.setName(g.getName());
						cd.setSong(Stream.of(g.getCharts()).map(chart -> toSongData(chart, defaultMode))
								.toArray(SongData[]::new));

						cd.setConstraint(Stream.of(g.getConstraint()).map(c -> CourseData.CourseDataConstraint.getValue(c))
								.filter(Objects::nonNull).toArray(CourseData.CourseDataConstraint[]::new));

						if (g.getTrophy() != null) {
							cd.setTrophy(Stream.of(g.getTrophy()).map(trophy -> {
								TrophyData t = new TrophyData();
								t.setName(trophy.getName());
								t.setMissrate((float) trophy.getMissrate());
								t.setScorerate((float) trophy.getScorerate());
								return t;
							}).toArray(TrophyData[]::new));
						}
						return cd;
					}).toArray(CourseData[]::new));
				}
				if(td == null || !td.validate()) {
					throw new RuntimeException("難易度表の値が不正です");
				}
				return td;
			} catch (Throwable e) {
				e.printStackTrace();
				Logger.getGlobal().warning("難易度表 - "+url+" の読み込み失敗。");
			}
			return null;
		}

		@Override
		public void write(TableData td) {
			new TableDataAccessor(tabledir).write(td);
		}
	}
	
	private static SongData toSongData(BMSTableElement te, Mode defaultMode) {
		SongData song = new SongData();
		if(te.getMD5() != null) {
			song.setMd5(te.getMD5().toLowerCase());
		}
		if(te.getSHA256() != null) {
			song.setSha256(te.getSHA256().toLowerCase());
		}
		song.setTitle(te.getTitle());
		song.setArtist(te.getArtist());
		Mode mode = te.getMode() != null ? Mode.getMode(te.getMode()) : null;
		song.setMode(mode != null ? mode.id : (defaultMode != null ? defaultMode.id : 0));
		song.setUrl(te.getURL());
		song.setIpfs(te.getIPFS());
		song.setOrg_md5(te.getParentHash());
		if(te instanceof DifficultyTableElement) {
			DifficultyTableElement dte = (DifficultyTableElement) te;
			song.setAppendurl(dte.getAppendURL());
			song.setAppendIpfs(dte.getAppendIPFS());
		}
		
		return song;
	}
}
