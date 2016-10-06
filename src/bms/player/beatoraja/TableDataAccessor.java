package bms.player.beatoraja;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bms.player.beatoraja.TableData.CourseData;
import bms.player.beatoraja.TableData.TrophyData;
import bms.table.Course;
import bms.table.DifficultyTable;
import bms.table.DifficultyTableElement;
import bms.table.DifficultyTableParser;
import bms.table.Course.Trophy;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

public class TableDataAccessor {
	
	private static final String[] CONSTRAINT = { "null", "grade", "grade_mirror", "grade_random", "no_speed",
		"no_good", "no_great" };

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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(TableData td) {
		try {
			Json json = new Json();
			json.setElementType(TableData.class, "hash", HashMap.class);
			json.setElementType(TableData.class, "course", ArrayList.class);
			json.setElementType(CourseData.class, "trophy", ArrayList.class);
			json.setOutputType(OutputType.json);
			FileWriter fw = new FileWriter("table/" + td.getName() + ".json");
			fw.write(json.prettyPrint(td));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public TableData[] readAll() {
		List<TableData> result = new ArrayList<TableData>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("table"))) {
			for(Path p : paths) {
				Json json = new Json();
				TableData td = json.fromJson(TableData.class, new FileReader(p.toFile()));
				result.add(td);
			}
		} catch(IOException e) {
			
		}
		return result.toArray(new TableData[result.size()]);
	}
	
	public TableData read(String name) {
		TableData td = null;
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("table"))) {
			for(Path p : paths) {
				if(p.getFileName().toString().equals(name + ".json")) {
					Json json = new Json();
					td = json.fromJson(TableData.class, new FileReader(p.toFile()));
					break;
				}
			}
		} catch(IOException e) {
			
		}
		return td;
	}

}
