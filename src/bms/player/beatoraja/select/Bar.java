package bms.player.beatoraja.select;

import bms.player.lunaticrave2.FolderData;
import bms.player.lunaticrave2.SongData;
import bms.player.beatoraja.*;
import bms.table.Course;

public abstract class Bar {

	private IRScoreData score;

	public abstract String getTitle();

	public IRScoreData getScore() {
		return score;
	}

	public void setScore(IRScoreData score) {
		this.score = score;
	}
}

abstract class SelectableBar extends Bar {

	/**
	 * リプレイデータが存在するか
	 */
	private boolean existsReplay;

	public boolean existsReplayData() {
		return existsReplay;
		}

	public void setExistsReplayData(boolean existsReplay) {
		this.existsReplay = existsReplay;
		}

}

class SongBar extends SelectableBar {

	private SongData song;

	/**
	 * リプレイデータが存在するか
	 */
	private boolean existsReplay;

	public SongBar(SongData song) {
		this.song = song;
	}

	public SongData getSongData() {
		return song;
	}

	@Override
	public String getTitle() {
		return song.getTitle();
	}
}

class FolderBar extends Bar {

	private FolderData folder;
	private String crc;
	private int[] lamps = new int[11];
	private int[] ranks = new int[0];

	public FolderBar(FolderData folder, String crc) {
		this.folder = folder;
		this.crc = crc;
	}

	public FolderData getFolderData() {
		return folder;
	}

	public String getCRC() {
		return crc;
	}

	@Override
	public String getTitle() {
		return folder.getTitle();
	}

	public int[] getLamps() {
		return lamps;
	}

	public void setLamps(int[] lamps) {
		this.lamps = lamps;
	}

	public int[] getRanks() {
		return ranks;
	}

	public void setRanks(int[] ranks) {
		this.ranks = ranks;
	}
}

class TableBar extends Bar {

	private String name;
	private TableLevelBar[] levels;
	private GradeBar[] grades;

	public TableBar(String name, TableLevelBar[] levels, GradeBar[] grades) {
		this.name = name;
		this.levels = levels;
		this.grades = grades;
	}

	@Override
	public String getTitle() {
		return name;
	}

	public TableLevelBar[] getLevels() {
		return levels;
	}

	public GradeBar[] getGrades() {
		return grades;
	}

}

class TableLevelBar extends Bar {
	private String level;
	private String[] hashes;
	private int[] lamps = new int[11];
	private int[] ranks = new int[0];

	public TableLevelBar(String level, String[] hashes) {
		this.level = level;
		this.hashes = hashes;
	}

	@Override
	public String getTitle() {
		return "LEVEL " + level;
	}

	public String[] getHashes() {
		return hashes;
	}

	public int[] getLamps() {
		return lamps;
	}

	public void setLamps(int[] lamps) {
		this.lamps = lamps;
	}

	public int[] getRanks() {
		return ranks;
	}

	public void setRanks(int[] ranks) {
		this.ranks = ranks;
	}
}

class GradeBar extends SelectableBar {

	private SongData[] songs;
	private String name;
	
	private TableData.CourseData course;
	private IRScoreData mscore;
	private IRScoreData rscore;

	public GradeBar(String name, SongData[] songs, TableData.CourseData course) {
		this.songs = songs;
		this.name = name;
		this.course = course;
	}

	public SongData[] getSongDatas() {
		return songs;
	}

	@Override
	public String getTitle() {
		return "段位認定 " + name;
	}

	public boolean existsAllSongs() {
		for (SongData song : songs) {
			if (song == null) {
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

	public int[] getConstraint() {
		if(course.getConstraint() != null) {
			return course.getConstraint();
		}
		return new int[0];
	}

	public TableData.TrophyData[] getAllTrophy() {
		return course.getTrophy();
	}
	public TableData.TrophyData getTrophy() {
		for(TableData.TrophyData trophy : course.getTrophy()) {
			if(qualified(this.getScore(), trophy)) {
				return trophy;
			}
			if(qualified(mscore, trophy)) {
				return trophy;
			}
			if(qualified(rscore, trophy)) {
				return trophy;
			}
		}
		return null;
	}

	private boolean qualified(IRScoreData score, TableData.TrophyData trophy) {
		return score != null && score.getNotes() != 0 && trophy.getMissrate() >= score.getMinbp() * 100.0 / score.getNotes()
				&& trophy.getScorerate() <= score.getExscore() * 100.0 / (score.getNotes() * 2);
	}
}
