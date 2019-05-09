package bms.player.beatoraja;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import bms.player.beatoraja.song.SongData;

/**
 * 難易度表データ
 *
 * @author exch
 */
public class TableData implements Validatable {
	
	/**
	 * 難易度表URL
	 */
	private String url = "";
	/**
	 * 難易度表名
	 */
	private String name = "";
	private String tag = "";
	
	private TableFolder[] folder = TableFolder.EMPTY;
	
	private CourseData[] course = CourseData.EMPTY;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TableFolder[] getFolder() {
		return folder;
	}

	public void setFolder(TableFolder[] folder) {
		this.folder = folder;
	}

	public CourseData[] getCourse() {
		return course;
	}

	public void setCourse(CourseData[] grade) {
		this.course = grade;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public void shrink() {
		for(CourseData c : course) {
			c.shrink();
		}
		for(TableFolder tf : folder) {
			tf.shrink();
		}
	}

	public boolean validate() {
		if(name == null || name.length() == 0) {
			return false;
		}
		folder = folder != null ? Validatable.removeInvalidElements(folder) : TableFolder.EMPTY;
		course = course != null ? Validatable.removeInvalidElements(course) : CourseData.EMPTY;;
		return folder.length + course.length > 0;
	}
	
	public static TableData read(Path p) {
		try {
			InputStream is = null;
			if (p.toString().endsWith(".bmt")) {
				is = new GZIPInputStream(Files.newInputStream(p));
			} else if(p.toString().endsWith(".json")) {
				is = Files.newInputStream(p);			
			}

			if(is != null) {
				Json json = new Json();
				json.setIgnoreUnknownFields(true);
				TableData td = json.fromJson(TableData.class, new BufferedInputStream(is));
				if(td == null || !td.validate()) {
					td = null;
				}
				return td;				
			}
		} catch(Throwable e) {

		}
		return null;
	}
	
	public static void write(Path p, TableData td) {
		try {
			td.shrink();
			OutputStream os = null;
			if(p.toString().endsWith(".bmt")) {
				os = new GZIPOutputStream(new FileOutputStream(p.toFile()));
			} else if(p.toString().endsWith(".json")) {
				os = new FileOutputStream(p.toFile());
			}
			
			if(os != null) {
				Json json = new Json();
				json.setElementType(TableData.class, "folder", ArrayList.class);
				json.setElementType(TableData.TableFolder.class, "songs", ArrayList.class);
				json.setElementType(TableData.class, "course", ArrayList.class);
				json.setElementType(CourseData.class, "trophy", ArrayList.class);
				json.setOutputType(OutputType.json);
				OutputStreamWriter fw = new OutputStreamWriter(new BufferedOutputStream(os), "UTF-8");
				fw.write(json.prettyPrint(td));
				fw.flush();
				fw.close();				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class TableFolder implements Validatable {

		public static final TableFolder[] EMPTY = new TableFolder[0];
		
		private String name;
		private SongData[] songs = new SongData[0];

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SongData[] getSong() {
			return songs;
		}

		public void setSong(SongData[] songs) {
			this.songs = songs;
		}
		
		public void shrink() {
			for(SongData song : songs) {
				song.shrink();
			}
		}

		@Override
		public boolean validate() {
			songs = Validatable.removeInvalidElements(songs);
			return name != null && name.length() > 0 && songs.length > 0;
		}
	}

}
