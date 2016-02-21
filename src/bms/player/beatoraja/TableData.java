package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;

public class TableData {

	private String name = "";
	
	private String[] level = new String[0];
	
	private HashMap<String, String[]> hash = new HashMap<String, String[]>();
	
	private String[] grade = new String[0];
	
	private HashMap<String, String[]> gradehash;

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
}
