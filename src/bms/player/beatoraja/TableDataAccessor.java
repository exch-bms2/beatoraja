package bms.player.beatoraja;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import bms.player.beatoraja.TableData.CourseData;
import bms.player.beatoraja.TableData.TrophyData;
import bms.table.Course;
import bms.table.DifficultyTable;
import bms.table.DifficultyTableElement;
import bms.table.DifficultyTableParser;
import bms.table.Course.Trophy;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * 難易度表データアクセス用クラス
 * 
 * @author exch
 */
public class TableDataAccessor {
	
	private String tabledir = "table";

	private static final String[] CONSTRAINT = { "null", "grade", "grade_mirror", "grade_random", "no_speed", "no_good",
			"no_great" };
	
	public TableDataAccessor() {
		
	}

	public TableDataAccessor(String tabledir) {
		this.tabledir = tabledir;
	}

	
	public void updateTableData(String[] urls) {
		for (String url : urls) {
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
				td.setLevel(dt.getLevelDescription());
				HashMap<String, String[]> levels = new HashMap<String, String[]>();
				for (String lv : dt.getLevelDescription()) {
					List<String> hashes = new ArrayList<String>();
					for (DifficultyTableElement dte : dt.getElements()) {
						if (lv.equals(dte.getDifficultyID())) {
							hashes.add(dte.getSHA256() != null ? dte.getSHA256() : dte.getMD5());
						}
					}
					levels.put(lv, hashes.toArray(new String[0]));
				}
				td.setHash(levels);

				if (dt.getCourse() != null && dt.getCourse().length > 0) {
					List<CourseData> gname = new ArrayList<CourseData>();
					for (Course[] course : dt.getCourse()) {
						for (Course g : course) {
							CourseData cd = new CourseData();
							cd.setName(g.getName());
							cd.setHash(g.getHash());
							int[] con = new int[g.getConstraint().length];
							for (int i = 0; i < con.length; i++) {
								for (int index = 0; index < CONSTRAINT.length; index++) {
									if (CONSTRAINT[index].equals(g.getConstraint()[i])) {
										con[i] = index;
										break;
									}
								}
							}
							cd.setConstraint(con);
							if (g.getTrophy() != null) {
								List<TrophyData> tr = new ArrayList<TrophyData>();
								for (Trophy trophy : g.getTrophy()) {
									TrophyData t = new TrophyData();
									t.setName(trophy.getName());
									t.setMissrate((float) trophy.getMissrate());
									t.setScorerate((float) trophy.getScorerate());
									tr.add(t);
								}
								cd.setTrophy(tr.toArray(new TrophyData[0]));
							}
							gname.add(cd);
						}
					}

					td.setCourse(gname.toArray(new CourseData[0]));
				}
				write(td);
			} catch (Throwable e) {
				e.printStackTrace();
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
			Json json = new Json();
			json.setElementType(TableData.class, "hash", HashMap.class);
			json.setElementType(TableData.class, "course", ArrayList.class);
			json.setElementType(CourseData.class, "trophy", ArrayList.class);
			json.setOutputType(OutputType.json);
			OutputStreamWriter fw = new OutputStreamWriter(new BufferedOutputStream(
					new GZIPOutputStream(new FileOutputStream(tabledir + "/" + td.getName() + ".bmt"))));
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
				if (p.toString().endsWith(".json")) {
					// TODO この分岐は後で削除
					Json json = new Json();
					TableData td = json.fromJson(TableData.class, new FileReader(p.toFile()));
					result.add(td);
				} else if (p.toString().endsWith(".bmt")) {
					Json json = new Json();
					TableData td = json.fromJson(TableData.class,
							new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
					result.add(td);
				}
			}
		} catch (IOException e) {

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
				if (p.getFileName().toString().equals(name + ".json")) {
					// TODO この分岐は後で削除
					Json json = new Json();
					td = json.fromJson(TableData.class, new FileReader(p.toFile()));
					break;
				} else if (p.getFileName().toString().equals(name + ".bmt")) {
					Json json = new Json();
					td = json.fromJson(TableData.class,
							new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));
					break;
				}

			}
		} catch (IOException e) {

		}
		return td;
	}

}
