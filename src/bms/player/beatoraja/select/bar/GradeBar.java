package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.song.SongData;

/**
 * コース選択用バー
 * 
 * @author exch
 */
public class GradeBar extends SelectableBar {

	/**
	 * コースデータ
	 */
    private final CourseData course;
	/**
	 * ミラー使用時のスコア
	 * TODO 不要かも
	 */
    private ScoreData mscore;
	/**
	 * ランダム使用時のスコア
	 * TODO 不要かも
	 */
    private ScoreData rscore;

    public GradeBar(CourseData course) {
        this.course = course;
    }

    public final CourseData getCourseData() {
        return course;
    }

    public final SongData[] getSongDatas() {
    	return course.getSong();
    }

    @Override
    public final String getTitle() {
        return course.getName();
    }

    public final boolean existsAllSongs() {
        for (SongData song : course.getSong()) {
            if (song == null || song.getPath() == null) {
                return false;
            }
        }
        return true;
    }

    public final ScoreData getMirrorScore() {
        return mscore;
    }

    public final void setMirrorScore(ScoreData score) {
        this.mscore = score;
    }

    public final ScoreData getRandomScore() {
        return rscore;
    }

    public final void setRandomScore(ScoreData score) {
        this.rscore = score;
    }

    public final CourseData.TrophyData getTrophy() {
    	ScoreData[] scores = {this.getScore(), mscore, rscore};

        CourseData.TrophyData[] trophies = course.getTrophy();
        for (int i = trophies.length - 1; i >= 0; i--) {
            for (ScoreData score : scores) {
                if (qualified(score, trophies[i])) {
                    return trophies[i];
                }
            }
        }
        return null;
    }

    private boolean qualified(ScoreData score, CourseData.TrophyData trophy) {
        return score != null && score.getNotes() != 0
                && trophy.getMissrate() >= score.getMinbp() * 100.0 / score.getNotes()
                && trophy.getScorerate() <= score.getExscore() * 100.0 / (score.getNotes() * 2);
    }

    public final int getLamp(boolean isPlayer) {
    	// TODO ライバルスコア
        int result = 0;
        if (getScore() != null && getScore().getClear() > result) {
            result = getScore().getClear();
        }
        if (getMirrorScore() != null && getMirrorScore().getClear() > result) {
            result = getMirrorScore().getClear();
        }
        if (getRandomScore() != null && getRandomScore().getClear() > result) {
            result = getRandomScore().getClear();
        }
        return result;
    }
}
