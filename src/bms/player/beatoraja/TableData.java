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

	public static class TableFolder {

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
	}

}
