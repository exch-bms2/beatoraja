package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SQL問い合わせ結果から曲を抽選するコースのデータ
 */
public class RandomCourseData {

	public static final RandomCourseData[] EMPTY = new RandomCourseData[0];

	/**
	 * コース名
	 */
	private String name;
	/**
	 * ステージ情報
	 */
	private RandomStageData[] stage = RandomStageData.EMPTY;
	/**
	 * コースの制約
	 */
	private CourseData.CourseDataConstraint[] constraint = CourseData.CourseDataConstraint.EMPTY;
	/**
	 * ランダムコースの制約
	 */
	private RandomCourseDataConstraint[] rconstraint = RandomCourseDataConstraint.EMPTY;
	/**
	 * トロフィー条件
	 */
	private CourseData.TrophyData[] trophy = CourseData.TrophyData.EMPTY;

	/**
	 * 各ステージの抽選結果の楽曲
	 */
	private SongData[] songDatas = SongData.EMPTY;

	private final java.util.Random random = new java.util.Random();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RandomStageData[] getStage() { return stage; }

	public void setStage(RandomStageData[] stage) { this.stage = stage; }

	public SongData[] getSongDatas() { return songDatas; }

	public void setSongDatas(SongData[] songDatas) { this.songDatas = songDatas; }

	public CourseData.CourseDataConstraint[] getConstraint() { return constraint; }

	public void setConstraint(CourseData.CourseDataConstraint[] constraint) { this.constraint = constraint; }

	public CourseData.TrophyData[] getTrophy() {
		return trophy;
	}

	public void setTrophy(CourseData.TrophyData[] trophy) {
		this.trophy = trophy;
	}

	public boolean isRelease() { return false; }

	public CourseData createCourseData() {
		CourseData courseData = new CourseData();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		courseData.setName(name + " " + sdf.format(new Date()));
		courseData.setSong(songDatas);
		courseData.setConstraint(constraint);
		courseData.setTrophy(trophy);
		courseData.setRelease(false);
		return courseData;
	}

	public void lotterySongDatas(final MainController main) {
		boolean isDistinct = false;
		for(RandomCourseDataConstraint rcon : rconstraint) {
			if(rcon == RandomCourseDataConstraint.DISTINCT) {
				isDistinct = true;
			}
		}

		songDatas = new SongData[stage.length];
		SongData[] lots = SongData.EMPTY;
		for (int i = 0; i < stage.length; i++) {
			String sql = stage[i].getSql();
			if (sql == null || sql.isEmpty()) {
				if (i > 0) {
					lotterySongData(i, songDatas, lots, isDistinct);
					continue;
				} else {
					sql = "1";
				}
			}
			lots = main.getSongDatabase().getSongDatas(sql ,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/score.db"
					,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/scorelog.db",main.getInfoDatabase() != null ? "songinfo.db" : null);
			lotterySongData(i, songDatas, lots, isDistinct);
		}
	}

	private void lotterySongData(int i, SongData[] songDatas, SongData[] lots, boolean isDistinct) {
		if (lots.length == 0) {
			return;
		}
		if (!isDistinct) {
			songDatas[i] = lots[random.nextInt(lots.length)];
			return;
		}

		// 曲を抽選し、以前のステージと曲が重複したら再抽選する。再抽選できなくなったら重複を許容する。
		List<SongData> tempLots = new ArrayList(Arrays.asList(lots));
		while (tempLots.size() > 0) {
			int ri = random.nextInt(tempLots.size());
			songDatas[i] = tempLots.get(ri);
			boolean isDuplicate = false;
			for (int j = 0; j < i; j++) {
				if (songDatas[j] == null) {
					break;
				}
				if (Objects.equals(songDatas[i].getSha256(), songDatas[j].getSha256())) {
					tempLots.remove(ri);
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate) {
				return;
			}
		}
		songDatas[i] = lots[random.nextInt(lots.length)];
		return;
	}

	/**
	 * ランダムコースの制約
	 */
	public enum RandomCourseDataConstraint {
		/**
		 * 曲の重複なし
		 */
		DISTINCT("distinct", 0);

		public static final RandomCourseDataConstraint[] EMPTY = new RandomCourseDataConstraint[0];

		public final String name;
		public final int type;

		RandomCourseDataConstraint(String name, int type) {
			this.name = name;
			this.type = type;
		}

		public static RandomCourseDataConstraint getValue(String name) {
			for(RandomCourseDataConstraint constraint : RandomCourseDataConstraint.values()) {
				if(constraint.name.equals(name)) {
					return constraint;
				}
			}
			return null;
		}
	}
}
