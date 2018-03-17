package bms.player.beatoraja;

import bms.player.beatoraja.song.SongData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * コースデータ
 *
 * @author exch
 */
@Data
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

    public SongData[] getSong() {
        return hash;
    }

    public void setSong(SongData[] hash) {
        this.hash = hash;
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
        CLASS("grade"),
        MIRROR("grade_mirror"),
        RANDOM("grade_random"),
        NO_SPEED("no_speed"),
        NO_GOOD("no_good"),
        NO_GREAT("no_great"),
    	GAUGE_LR2("gauge_lr2"),
    	GAUGE_5KEYS("gauge_5k"),
    	GAUGE_7KEYS("gauge_7k"),
    	GAUGE_9KEYS("gauge_9k"),
    	GAUGE_24KEYS("gauge_24k");

        public final String name;

        private CourseDataConstraint(String name) {
            this.name = name;
        }
    }
    /**
     * コースデータのトロフィー条件
     *
     * @author exch
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrophyData {

        private String name;

        private float missrate;

        private float scorerate;

    }

}
