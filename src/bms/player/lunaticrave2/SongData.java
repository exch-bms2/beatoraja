package bms.player.lunaticrave2;

import java.util.*;

/**
 * 楽曲データ
 * 
 * @author exch
 */
public class SongData {

	/**
	 * 楽曲タイトル
	 */
	private String title;
	/**
	 * 楽曲サブタイトル
	 */
	private String subtitle;
	/**
	 * 楽曲ジャンル
	 */
	private String genre;
	/**
	 * 楽曲アーティスト名
	 */
	private String artist;
	/**
	 * 楽曲サブアーティスト名
	 */
	private String subartist;
	private int favorite;
	private List<String> path = new ArrayList<String>();
	private String tag;	
	private String hash;
	private String banner;
	private int date;
	private int adddate;
	private int level;
	private int exlevel;
	private int mode;
	private int longnote;
	private int difficulty;
	private int minbpm;
	private int maxbpm;
	private int txt;

	public int getFavorite() {
		return favorite;
	}
	public void setFavorite(int favorite) {
		this.favorite = favorite;
	}
	public String getPath() {
		if(path.size() > 0) {
			return path.get(0);
		}
		return null;
	}
	
	public void setPath(String path) {
		if(this.path.size() == 0) {
			this.path.add(path);
		} else {
			this.path.set(0, path);			
		}
	}
	
	public void addAnotherPath(String path) {
		this.path.add(path);
	}
	
	public String[] getAllPaths() {
		return path.toArray(new String[0]);
	}
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public int getAdddate() {
		return adddate;
	}
	public void setAdddate(int adddate) {
		this.adddate = adddate;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public int getExlevel() {
		return exlevel;
	}
	public void setExlevel(int exlevel) {
		this.exlevel = exlevel;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getSubtitle() {
		return subtitle;
	}
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getSubartist() {
		return subartist;
	}
	public void setSubartist(String subartist) {
		this.subartist = subartist;
	}
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	public int getMode() {
		return mode;
	}
	public void setMode(int mode) {
		this.mode = mode;
	}
	public int getDate() {
		return date;
	}
	public void setDate(int date) {
		this.date = date;
	}
	public int getLongnote() {
		return longnote;
	}
	public void setLongnote(int longnote) {
		this.longnote = longnote;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public int getMinbpm() {
		return minbpm;
	}

	public void setMinbpm(int minbpm) {
		this.minbpm = minbpm;
	}

	public int getMaxbpm() {
		return maxbpm;
	}

	public void setMaxbpm(int maxbpm) {
		this.maxbpm = maxbpm;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}
	public int getTxt() {
		return txt;
	}
	public void setTxt(int txt) {
		this.txt = txt;
	}
	
	public boolean hasDocument() {
		return (txt & 1) != 0;
	}
	
	public boolean hasBGA() {
		return (txt & 2) != 0;
	}
	
	public boolean hasRandomSequence() {
		return (longnote & 4) != 0;
	}

	public boolean hasMineNote() {
		return (longnote & 2) != 0;
	}

	public boolean hasLongNote() {
		return (longnote & 1) != 0;
	}

}
