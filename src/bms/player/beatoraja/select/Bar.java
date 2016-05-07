package bms.player.beatoraja.select;

import bms.player.lunaticrave2.FolderData;
import bms.player.lunaticrave2.SongData;
import bms.player.beatoraja.*;

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

class SongBar extends Bar {

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

	public boolean existsReplayData() {
		return existsReplay;
	}

	public void setExistsReplayData(boolean existsReplay) {
		this.existsReplay = existsReplay;
	}
}

class FolderBar extends Bar {

	private FolderData folder;
	private String crc;

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
}

class GradeBar extends Bar {

	private SongData[] songs;
	private String name;

	private IRScoreData score;

	public GradeBar(String name, SongData[] songs) {
		this.songs = songs;
		this.name = name;
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
		return score;
	}

	public void setMirrorScore(IRScoreData score) {
		this.score = score;
	}

}
