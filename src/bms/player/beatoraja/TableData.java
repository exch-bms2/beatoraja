package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;

public class TableData {

	private String name = "";
	
	private String[] level = new String[0];
	
	private HashMap<String, String[]> hash = new HashMap<String, String[]>();
	
	private CourseData[] course = new CourseData[0];
	
	public static final int GRADE_NORMAL = 1;
	public static final int GRADE_MIRROR = 2;
	public static final int GRADE_RANDOM = 3;
	public static final int NO_HISPEED = 4;
	public static final int NO_GOOD = 5;
	public static final int NO_GREAT = 6;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getLevel() {
		return level;
	}

	public void setLevel(String[] level) {
		this.level = level;
	}

	public CourseData[] getCourse() {
		return course;
	}

	public void setCourse(CourseData[] grade) {
		this.course = grade;
	}

	public Map<String, String[]> getHash() {
		return hash;
	}

	public void setHash(HashMap<String, String[]> hash) {
		this.hash = hash;
	}

	public static class CourseData {
		
		private String name;
		
		private String[] hash;
		
		private int[] constraint;
		
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

		public int[] getConstraint() {
			return constraint;
		}

		public void setConstraint(int[] constraint) {
			this.constraint = constraint;
		}

		public TrophyData[] getTrophy() {
			return trophy;
		}

		public void setTrophy(TrophyData[] trophy) {
			this.trophy = trophy;
		}
	}
	
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
