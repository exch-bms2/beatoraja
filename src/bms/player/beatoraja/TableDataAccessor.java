package bms.player.beatoraja;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

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
		HashSet<String> localTables = getLocalTableFilenames();
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

	private HashSet<String> getLocalTableFilenames() {
		HashSet<String> set = new HashSet<>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				String fileName = p.getFileName().toString();
				if (fileName.endsWith(".bmt")) {
					set.add(fileName);
				}
			}
		} catch (IOException e) {
			return null;
		}
		return set;
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
		List<TableData> result = new ArrayList<TableData>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				TableData td = TableData.read(p);
				if(td != null) {
					result.add(td);						
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toArray(new TableData[result.size()]);
	}

	/**
	 * 指定のキャッシュされた難易度表データを読み込む
	 * 
	 * @param name 難易度表URL
	 * @return キャッシュされた難易度表データ。存在しない場合はnull
	 */
	public TableData readCache(String url) {
		TableData td = null;
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				if (p.getFileName().toString().equals(getFileName(url) + ".bmt")) {
					td = TableData.read(p);
					break;
				}
			}
		} catch (IOException e) {

		}
		return td;
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
				String[] levels = dt.getLevelDescription();
				List<TableData.TableFolder> tdes = new ArrayList<>(levels.length);
				for (String lv : levels) {
					TableData.TableFolder tde = new TableData.TableFolder();
					tde.setName(td.getTag() + lv);
					List<SongData> hashes = new ArrayList<SongData>();
					for (DifficultyTableElement dte : dt.getElements()) {
						if (lv.equals(dte.getLevel())) {
							SongData sd = toSongData(dte, defaultMode);
							hashes.add(sd);
						}
					}
					tde.setSong(hashes.toArray(new SongData[hashes.size()]));
					tdes.add(tde);
				}
				td.setFolder(tdes.toArray(new TableData.TableFolder[tdes.size()]));

				if (dt.getCourse() != null && dt.getCourse().length > 0) {
					List<CourseData> gname = new ArrayList<CourseData>();
					for (Course[] course : dt.getCourse()) {
						for (Course g : course) {
							CourseData cd = new CourseData();
							cd.setName(g.getName());
							SongData[] songs = new SongData[g.getCharts().length];
							for(int i = 0;i < songs.length;i++) {
								songs[i] = toSongData(g.getCharts()[i], defaultMode);
							}
							cd.setSong(songs);
							List<CourseData.CourseDataConstraint> l = new ArrayList<>();
							for(int i = 0;i < g.getConstraint().length;i++) {
								for (CourseData.CourseDataConstraint constraint : CourseData.CourseDataConstraint.values()) {
									if (constraint.name.equals(g.getConstraint()[i])) {
										l.add(constraint);
										break;
									}
								}
							}
							cd.setConstraint(l.toArray(new CourseData.CourseDataConstraint[l.size()]));
							if (g.getTrophy() != null) {
								List<TrophyData> tr = new ArrayList<TrophyData>();
								for (Trophy trophy : g.getTrophy()) {
									TrophyData t = new TrophyData();
									t.setName(trophy.getName());
									t.setMissrate((float) trophy.getMissrate());
									t.setScorerate((float) trophy.getScorerate());
									tr.add(t);
								}
								cd.setTrophy(tr.toArray(new TrophyData[tr.size()]));
							}
							gname.add(cd);
						}
					}

					td.setCourse(gname.toArray(new CourseData[gname.size()]));
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
