package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.song.SongData;

/**
 * Created by exch on 2017/09/02.
 */
public class GradeBar extends SelectableBar {

    private SongData[] songs;
    private String name;

    private CourseData course;
    private IRScoreData mscore;
    private IRScoreData rscore;

    public GradeBar(String name, SongData[] songs, CourseData course) {
        this.songs = songs;
        this.name = name;
        this.course = course;        
    }

    public SongData[] getSongDatas() {
        return songs;
    }

    @Override
    public String getTitle() {
        return name;
    }

    public boolean existsAllSongs() {
        for (SongData song : songs) {
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

    public CourseData.CourseDataConstraint[] getConstraint() {
        if (course.getConstraint() != null) {
            return course.getConstraint();
        }
        return new CourseData.CourseDataConstraint[0];
    }

    public CourseData.TrophyData[] getAllTrophy() {
        return course.getTrophy();
    }

    public CourseData.TrophyData getTrophy() {
        for (CourseData.TrophyData trophy : course.getTrophy()) {
            if (qualified(this.getScore(), trophy)) {
                return trophy;
            }
            if (qualified(mscore, trophy)) {
                return trophy;
            }
            if (qualified(rscore, trophy)) {
                return trophy;
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
