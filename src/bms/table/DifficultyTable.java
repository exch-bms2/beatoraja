package bms.table;

import java.io.Serializable;
import java.util.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 難易度表
 * 
 * @author exch
 */
public class DifficultyTable extends BMSTable<DifficultyTableElement> implements
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2757817491532398378L;
	/**
	 * レベル表記
	 */
	private String[] levelDescription = new String[0];
	/**
	 * 段位
	 */
	private Grade[] grade = new Grade[0];
	
	public DifficultyTable() {
		super();
	}

	public DifficultyTable(String sourceURL) {
		super();
		this.setSourceURL(sourceURL);
	}

	public DifficultyTableElement[] getElements() {
		DifficultyTableElement[] dte = this.getModels().toArray(
				new DifficultyTableElement[0]);
		Comparator asc = new Comparator() {
			public int compare(Object o1, Object o2) {
				DifficultyTableElement dte1 = (DifficultyTableElement) o1;
				DifficultyTableElement dte2 = (DifficultyTableElement) o2;
				int c = indexOf(dte1.getDifficultyID())
						- indexOf(dte2.getDifficultyID());
				if (c == 0) {
					return dte1.getTitle().compareToIgnoreCase(dte2.getTitle());
				}
				return c;
			}
		};
		Arrays.sort(dte, asc);
		return dte;
	}

	private int indexOf(String level) {
		for (int i = 0; i < levelDescription.length; i++) {
			if (levelDescription[i].equals(level)) {
				return i;
			}
		}
		return -1;
	}

	public String[] getLevelDescription() {
		return levelDescription;
	}

	public void setLevelDescription(String[] levelDescription) {
		this.levelDescription = levelDescription;
	}
	
	public Grade[] getGrade() {
		return grade;
	}

	public void setGrade(Grade[] grade) {
		this.grade = grade;
	}

	/**
	 * 段位
	 * 
	 * @author exch
	 */
	public static class Grade {
		/**
		 * 段位名
		 */
		private StringProperty name = new SimpleStringProperty("新規段位");
		/**
		 * 段位を構成する譜面のMD5
		 */
		private String[] hashes = new String[0];
		/**
		 * 段位名のスタイル
		 */
		private StringProperty style =  new SimpleStringProperty("");
		
		public String getName() {
			return name.get();
		}
		
		public void setName(String name) {
			this.name.set(name);
		}
		
		public StringProperty nameProperty() {
			return name;
		}
		
		public String[] getHashes() {
			return hashes;
		}
		
		public void setHashes(String[] hashes) {
			this.hashes = hashes;
		}
		
		public String getStyle() {
			return style.get();
		}
		
		public void setStyle(String style) {
			this.style.set(style);
		}
		
		public StringProperty styleProperty() {
			return style;
		}
 	}
}
