package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.RandomCourseData;
import bms.player.beatoraja.RandomStageData;
import bms.player.beatoraja.song.SongData;

/**
 * ランダムコース選択用バー
 */
public class RandomCourseBar extends SelectableBar {

	private final RandomCourseData course;

	public RandomCourseBar(RandomCourseData course) {
		this.course = course;
	}

	public final RandomCourseData getCourseData() {
		return course;
	}

	@Override
	public final String getTitle() {
		return course.getName();
	}

	public final SongData[] getSongDatas() {
		return course.getSongDatas();
	}

	public final boolean existsAllSongs() {
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

	public final int getLamp(boolean isPlayer) {
		return 0;
	}
}
