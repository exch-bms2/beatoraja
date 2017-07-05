package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;

/**
 * 難易度表データ
 *
 * @author exch
 */
public class TableData {
	/**
	 * 難易度表URL
	 */
	private String url = "";
	/**
	 * 難易度表名
	 */
	private String name = "";
	
	private TableDataELement[] folder = new TableDataELement[0];
	
	private CourseData[] course = new CourseData[0];
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TableDataELement[] getFolder() {
		return folder;
	}

	public void setFolder(TableDataELement[] folder) {
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

	public static class TableDataELement {

		private String level;
		private TableSongData[] songs = new TableSongData[0];

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}

		public TableSongData[] getSongs() {
			return songs;
		}

		public void setSongs(TableSongData[] songs) {
			this.songs = songs;
		}
	}

	public static class TableSongData {

		private String hash;
		private String title;
		private String url;
		private String appendurl;

		public TableSongData() {

		}

		public TableSongData(String hash) {
			this(hash,null,null,null);
		}

		public TableSongData(String hash, String title, String url, String appendurl) {
			this.hash = hash;
			this.title = title;
			this.url = url;
			this.appendurl = appendurl;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getAppendurl() {
			return appendurl;
		}

		public void setAppendurl(String appendurl) {
			this.appendurl = appendurl;
		}
	}

}
