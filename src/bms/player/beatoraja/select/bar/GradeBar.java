package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.song.SongData;

/**
 * Created by exch on 2017/09/02.
 */
public class GradeBar extends SelectableBar {

    private CourseData course;
    private IRScoreData mscore;
    private IRScoreData rscore;

    public GradeBar(CourseData course) {
        this.course = course;
    }

    public CourseData getCourseData() {
        return course;
    }

    public SongData[] getSongDatas() {
    	return course.getSong();
    }

    @Override
    public String getTitle() {
        return course.getName();
    }

    @Override
    public String getArtist() {
        return null;
    }

    public boolean existsAllSongs() {
        for (SongData song : course.getSong()) {
            if (song == null || song.getPath() == null) {
                return false;
            }
        }
        return true;
    }

    public IRScoreData getMirrorScore() {
        return mscore;
    }

    public void setMirrorScore(IRScoreData score) {
        this.mscore = score;
    }

    public IRScoreData getRandomScore() {
        return rscore;
    }

    public void setRandomScore(IRScoreData score) {
        this.rscore = score;
    }

    public CourseData.TrophyData getTrophy() {
        IRScoreData[] scores = {this.getScore(), mscore, rscore};

        CourseData.TrophyData[] trophies = course.getTrophy();
        for (int i = trophies.length - 1; i >= 0; i--) {
            for (IRScoreData score : scores) {
                if (qualified(score, trophies[i])) {
                    return trophies[i];
                }
            }
        }
        return null;
    }

    private boolean qualified(IRScoreData score, CourseData.TrophyData trophy) {
        return score != null && score.getNotes() != 0
                && trophy.getMissrate() >= score.getMinbp() * 100.0 / score.getNotes()
                && trophy.getScorerate() <= score.getExscore() * 100.0 / (score.getNotes() * 2);
    }

    public int getLamp(boolean isPlayer) {
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
