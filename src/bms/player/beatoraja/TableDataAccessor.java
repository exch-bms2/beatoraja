package bms.player.beatoraja;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import bms.model.Mode;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.song.SongData;
import bms.table.BMSTableElement;
import bms.table.Course;
import bms.table.Course.Trophy;
import bms.table.DifficultyTable;
import bms.table.DifficultyTableElement;
import bms.table.DifficultyTableParser;

/**
 * 難易度表データアクセス用クラス
 * 
 * @author exch
 */
public class TableDataAccessor {
	
	private String tabledir = "table";

	public TableDataAccessor() {
		
	}

	public TableDataAccessor(String tabledir) {
		this.tabledir = tabledir;
	}

	public void updateTableData(String[] urls) {
		final ConcurrentLinkedDeque< Thread> tasks = new ConcurrentLinkedDeque<Thread>();
		for (final String url : urls) {
			Thread task = new Thread(() -> {
                TableAccessor tr = new DifficultyTableAccessor(url);
                TableData td = tr.read();
                if(td != null) {
                    write(td);
                }
            });
			tasks.add(task);
			task.start();
		}
		
		while(!tasks.isEmpty()) {
			if(!tasks.getFirst().isAlive()) {
				tasks.removeFirst();
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	/**
	 * 難易度表データをキャッシュする
	 * 
	 * @param td 難易度表データ
	 */
	public void write(TableData td) {
		try {
			td.shrink();
			Json json = new Json();
			json.setElementType(TableData.class, "folder", ArrayList.class);
			json.setElementType(TableData.TableFolder.class, "songs", ArrayList.class);
			json.setElementType(TableData.class, "course", ArrayList.class);
			json.setElementType(CourseData.class, "trophy", ArrayList.class);
			json.setOutputType(OutputType.json);
			OutputStreamWriter fw = new OutputStreamWriter(new BufferedOutputStream(
					new GZIPOutputStream(new FileOutputStream(tabledir + "/" + td.getName() + ".bmt"))), "UTF-8");
			fw.write(json.prettyPrint(td));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				if (p.toString().endsWith(".bmt")) {
					try {
						Json json = new Json();
						json.setIgnoreUnknownFields(true);
						TableData td = json.fromJson(TableData.class,
								new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
						if(td != null && td.validate()) {
							result.add(td);
						}
					} catch(Throwable e) {
						e.printStackTrace();
					}
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
	 * @param name 難易度表名
	 * @return キャッシュされた難易度表データ。存在しない場合はnull
	 */
	public TableData read(String name) {
		TableData td = null;
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				if (p.getFileName().toString().equals(name + ".bmt")) {
					try {
						Json json = new Json();
						json.setIgnoreUnknownFields(true);
						td = json.fromJson(TableData.class,
								new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
						if(td == null || !td.validate()) {
							td = null;
						}
						break;
					} catch(Throwable e) {

					}
				}
			}
		} catch (IOException e) {

		}
		return td;
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

		private String url;

		public DifficultyTableAccessor(String url) {
			super(url);
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
			new TableDataAccessor().write(td);
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
