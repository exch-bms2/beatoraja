package bms.player.beatoraja;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import bms.player.beatoraja.song.SongData;
import bms.table.BMSTableElement;
import bms.table.Course;
import bms.table.DifficultyTable;
import bms.table.DifficultyTableElement;
import bms.table.DifficultyTableParser;
import bms.table.Course.Trophy;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import bms.model.Mode;
import bms.player.beatoraja.CourseData.TrophyData;

/**
 * �썵�삌佯�烏ⓦ깈�꺖�궭�궋�궚�궩�궧�뵪�궚�꺀�궧
 * 
 * @author exch
 */
public class TableDataAccessor {
	private static TableDataAccessor instance = null;
	
	final private String tabledir = "table";
	static public TableDataAccessor getInstance() {
		if(instance == null)
			instance = new TableDataAccessor();
		return instance;
	}
	private TableDataAccessor() {
		
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
	 * �썵�삌佯�烏ⓦ깈�꺖�궭�굮�궘�깵�긿�궥�깷�걲�굥
	 * 
	 * @param td �썵�삌佯�烏ⓦ깈�꺖�궭
	 */
	public void write(TableData td) {
		try {
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
	 * �뀲�겍�겗�궘�깵�긿�궥�깷�걬�굦�걼�썵�삌佯�烏ⓦ깈�꺖�궭�굮沃��겳渦쇈�
	 * 
	 * @return �뀲�겍�겗�궘�깵�긿�궥�깷�걬�굦�걼�썵�삌佯�烏ⓦ깈�꺖�궭
	 */
	public TableData[] readAll() {
		List<TableData> result = new ArrayList<TableData>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				if (p.toString().endsWith(".bmt")) {
					try {
						Json json = new Json();
						TableData td = json.fromJson(TableData.class,
								new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
						result.add(td);
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
	 * �뙁若싥겗�궘�깵�긿�궥�깷�걬�굦�걼�썵�삌佯�烏ⓦ깈�꺖�궭�굮沃��겳渦쇈�
	 * 
	 * @param name �썵�삌佯�烏ⓨ릫
	 * @return �궘�깵�긿�궥�깷�걬�굦�걼�썵�삌佯�烏ⓦ깈�꺖�궭�귛춼�쑉�걮�겒�걚�졃�릦�겘null
	 */
	public TableData read(String name) {
		TableData td = null;
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(tabledir))) {
			for (Path p : paths) {
				if (p.getFileName().toString().equals(name + ".bmt")) {
					try {
						Json json = new Json();
						td = json.fromJson(TableData.class,
								new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
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
					tde.setLevel(lv);
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
				return td;
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void write(TableData td) {
			TableDataAccessor.getInstance().write(td);
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
		
		if(te instanceof DifficultyTableElement) {
			DifficultyTableElement dte = (DifficultyTableElement) te;
			song.setAppendurl(dte.getAppendURL());
		}
		
		return song;
	}
}
