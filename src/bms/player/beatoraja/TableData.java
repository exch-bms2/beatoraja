package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;

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
	private String tag = "";
	
	private TableFolder[] folder = new TableFolder[0];
	
	private CourseData[] course = new CourseData[0];
	
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

	public static class TableFolder {

		private String name;
		private SongData[] songs = new SongData[0];
		private String level;

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

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}
	}

}
