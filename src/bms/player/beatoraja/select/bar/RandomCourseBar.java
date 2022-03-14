package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.RandomCourseData;
import bms.player.beatoraja.RandomStageData;
import bms.player.beatoraja.song.SongData;

/**
 * ランダムコース選択用バー
 */
public class RandomCourseBar extends SelectableBar {

	private RandomCourseData course;

	public RandomCourseBar(RandomCourseData course) {
		this.course = course;
	}

	public RandomCourseData getCourseData() {
		return course;
	}

	@Override
	public String getTitle() {
		return course.getName();
	}

	@Override
	public String getArtist() {
		return null;
	}

	public SongData[] getSongDatas() {
		return course.getSongDatas();
	}

	public boolean existsAllSongs() {
		if (course.getStage().length == 0) {
			return false;
		}
		for (RandomStageData stage : course.getStage()) {
			if (stage == null) {
				return false;
			}
		}
		return true;
	}

	public int getLamp(boolean isPlayer) {
		return 0;
	}
}
