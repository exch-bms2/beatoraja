package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;

/**
 * コースデータ
 *
 * @author exch
 */
public class CourseData {
    /**
     * コース名
     */
    private String name;
    /**
     * 楽曲のハッシュ
     */
    private SongData[] hash = new SongData[0];
    /**
     * コースの制限
     */
    private CourseDataConstraint[] constraint = new CourseDataConstraint[0];
    /**
     * トロフィー条件
     */
    private TrophyData[] trophy = new TrophyData[0];

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SongData[] getSong() {
        return hash;
    }

    public void setSong(SongData[] hash) {
        this.hash = hash;
    }

    public CourseDataConstraint[] getConstraint() {
        return constraint;
    }

    public void setConstraint(CourseDataConstraint[] constraint) {
        this.constraint = constraint;
    }

    public TrophyData[] getTrophy() {
        return trophy;
    }

    public void setTrophy(TrophyData[] trophy) {
        this.trophy = trophy;
    }

    public boolean isClassCourse() {
        for(CourseDataConstraint con : constraint) {
            if(con == CourseDataConstraint.CLASS || con == CourseDataConstraint.MIRROR || con == CourseDataConstraint.RANDOM) {
                return true;
            }
        }
        return false;
    }

    /**
     * コースの制約
     *
     * @author exch
     */
    public enum CourseDataConstraint {
        CLASS(1,"grade"),
        MIRROR(2,"grade_mirror"),
        RANDOM(3,"grade_random"),
        NO_SPEED(4,"no_speed"),
        NO_GOOD(5,"no_good"),
        NO_GREAT(6,"no_great"),
    	GAUGE_LR2(7,"gauge_lr2"),
    	GAUGE_5KEYS(7,"gauge_5k"),
    	GAUGE_7KEYS(7,"gauge_7k"),
    	GAUGE_9KEYS(7,"gauge_9k"),
    	GAUGE_24KEYS(7,"gauge_24k");

        public final int id;
        public final String name;

        private CourseDataConstraint(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    /**
     * コースデータのトロフィー条件
     *
     * @author exch
     */
    public static class TrophyData {

        private String name;

        private float missrate;

        private float scorerate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public float getMissrate() {
            return missrate;
        }

        public void setMissrate(float missrate) {
            this.missrate = missrate;
        }

        public float getScorerate() {
            return scorerate;
        }

        public void setScorerate(float scorerate) {
            this.scorerate = scorerate;
        }
    }

}
