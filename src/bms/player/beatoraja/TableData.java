package bms.player.beatoraja;

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
