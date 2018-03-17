package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;
import lombok.Data;

/**
 * 難易度表データ
 *
 * @author exch
 */
@Data
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

	@Data
	public static class TableFolder {

		private String name;
		private SongData[] songs = new SongData[0];
		private String level;

	}

}
