package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;

public class TableData {

	private String name = "";
	
	private String[] level = new String[0];
	
	private HashMap<String, String[]> hash = new HashMap<String, String[]>();
	
	private String[] grade = new String[0];
	
	private HashMap<String, String[]> gradehash = new HashMap<String, String[]>();;
	
	private  HashMap<String, int[]> gradeconstraint = new HashMap<String, int[]>();;

	public static final int GRADE_NORMAL = 1;
	public static final int GRADE_MIRROR = 2;
	public static final int GRADE_RANDOM = 3;
	
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

	public String[] getGrade() {
		return grade;
	}

	public void setGrade(String[] grade) {
		this.grade = grade;
	}

	public Map<String, String[]> getHash() {
		return hash;
	}

	public void setHash(HashMap<String, String[]> hash) {
		this.hash = hash;
	}

	public Map<String, String[]> getGradehash() {
		return gradehash;
	}

	public void setGradehash(HashMap<String, String[]> gradehash) {
		this.gradehash = gradehash;
	}

	public HashMap<String, int[]> getGradeconstraint() {
		return gradeconstraint;
	}

	public void setGradeconstraint(HashMap<String, int[]> gradeconstraint) {
		this.gradeconstraint = gradeconstraint;
	}	
}
