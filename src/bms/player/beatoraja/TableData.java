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
		private String[] hash;

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}

		public String[] getHash() {
			return hash;
		}

		public void setHash(String[] hash) {
			this.hash = hash;
		}
	}
}
