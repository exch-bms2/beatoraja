package bms.player.beatoraja;

/**
 * コースデータ
 */
public class CourseData {

    private String name;

    private String[] hash;

    private CourseDataConstraint[] constraint;

    private TrophyData[] trophy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getHash() {
        return hash;
    }

    public void setHash(String[] hash) {
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

    public enum CourseDataConstraint {
        CLASS(1,"grade"),
        MIRROR(2,"grade_mirror"),
        RANDOM(3,"grade_random"),
        NO_SPEED(4,"no_speed"),
        NO_GOOD(5,"no_good"),
        NO_GREAT(6,"no_great");

        public final int id;
        public final String name;

        private CourseDataConstraint(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    /**
     * コースデータのトロフィー条件
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
