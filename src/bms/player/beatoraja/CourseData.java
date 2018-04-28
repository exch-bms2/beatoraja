package bms.player.beatoraja;

import bms.model.BMSModel;
import bms.player.beatoraja.song.SongData;

/**
 * コースデータ
 *
 * @author exch
 */
public class CourseData implements Validatable {
	
	public static final CourseData[] EMPTY = new CourseData[0];
	
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
    private CourseDataConstraint[] constraint = CourseDataConstraint.EMPTY;
    /**
     * トロフィー条件
     */
    private TrophyData[] trophy = TrophyData.EMPTY;

    public CourseData() {
    	
    }
    
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
    
    public void setSong(BMSModel[] models) {
    	SongData[] hash = new SongData[models.length];
    	for(int i = 0;i < models.length;i++) {
    		hash[i] = new SongData(models[i], false);
    	}
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
    
    public boolean validate() {
    	if((hash = Validatable.removeInvalidElements(hash)).length == 0) {
    		return false;
    	}
    	
    	constraint = constraint != null ? Validatable.removeInvalidElements(constraint) : CourseDataConstraint.EMPTY;
    	trophy = trophy != null ? Validatable.removeInvalidElements(trophy) : TrophyData.EMPTY;    	
    	return true;
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

    	public static final CourseDataConstraint[] EMPTY = new CourseDataConstraint[0];
    	
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
    public static class TrophyData implements Validatable {
    	
    	public static final TrophyData[] EMPTY = new TrophyData[0];

        private String name;

        private float missrate;

        private float scorerate;

        public TrophyData() {
        	
        }
        
        public TrophyData(String name, float missrate, float scorerate) {
        	this.name = name;
        	this.missrate = missrate;
        	this.scorerate = scorerate;
        }
        
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

		@Override
		public boolean validate() {
			return name != null && missrate > 0 && scorerate < 100;
		}
    }

}
