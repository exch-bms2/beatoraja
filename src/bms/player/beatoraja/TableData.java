package bms.player.beatoraja;

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
		private TableSong[] songs = new TableSong[0];

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public TableSong[] getSong() {
			return songs;
		}

		public void setSong(TableSong[] songs) {
			this.songs = songs;
		}
	}

	public static class TableSong {

		private String hash;
		private String title;
		private String artist;
		private String genre;
		private String url;
		private String appendurl;

		public TableSong() {

		}

		public TableSong(String hash) {
			this(hash,null,null, null, null,null);
		}

		public TableSong(String hash, String title, String artist, String genre, String url, String appendurl) {
			this.hash = hash;
			this.title = title;
			this.artist = artist;
			this.genre = genre;
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

		public String getArtist() {
			return artist;
		}

		public void setArtist(String artist) {
			this.artist = artist;
		}

		public String getGenre() {
			return genre;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}
	}

}
