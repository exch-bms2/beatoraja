package bms.player.beatoraja;

import java.util.ArrayList;
import java.util.List;

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
	private String md5;
	private String sha256;
	private String banner;
	private int date;
	private int adddate;
	private int level;
	private int mode;
	private int feature;
	private int difficulty;
	private int minbpm;
	private int maxbpm;
	private int content;

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
	
	public boolean hasDocument() {
		return (content & 1) != 0;
	}
	
	public boolean hasBGA() {
		return (content & 2) != 0;
	}
	
	public boolean hasRandomSequence() {
		return (feature & 4) != 0;
	}

	public boolean hasMineNote() {
		return (feature & 2) != 0;
	}

	public boolean hasLongNote() {
		return (feature & 1) != 0;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	public String getSha256() {
		return sha256;
	}
	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}
	public int getFeature() {
		return feature;
	}
	public void setFeature(int feature) {
		this.feature = feature;
	}
	public int getContent() {
		return content;
	}
	public void setContent(int content) {
		this.content = content;
	}

}
