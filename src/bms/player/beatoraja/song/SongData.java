package bms.player.beatoraja.song;

import java.util.ArrayList;
import java.util.List;

import bms.model.BMSModel;
import bms.model.TimeLine;

/**
 * 楽曲データ
 * 
 * @author exch
 */
public class SongData {
	
	public static final int FEATURE_LONGNOTE = 1;
	public static final int FEATURE_MINENOTE = 2;
	public static final int FEATURE_RANDOM = 4;

	public static final int CONTENT_TEXT = 1;
	public static final int CONTENT_BGA = 2;
	
	/**
	 * 楽曲タイトル
	 */
	private String title;
	/**
	 * 楽曲サブタイトル
	 */
	private String subtitle;
	private String fulltitle;
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
	private String fullartist;
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
	private int judge;
	private int minbpm;
	private int maxbpm;
	private int length;
	private int content;
	private int notes;
	private String stagefile = "";
	private String backbmp = "";
	private String preview = "";

	private String folder;
	private String parent;

	private BMSModel model;
	private TimeLine[] timelines;
	private SongInformation info;;
	
	public SongData() {
		
	}
	
	public SongData(BMSModel model, boolean containstxt) {
		content = containstxt ? CONTENT_TEXT : 0;
		setBMSModel(model);
	}

	public void setBMSModel(BMSModel model) {
		if(model == null) {
			return;
		}
		this.model = model;
		title = model.getTitle();
		subtitle = model.getSubTitle();
		genre = model.getGenre();
		artist = model.getArtist();
		subartist = model.getSubArtist();
		path.add(model.getPath());
		md5 = model.getMD5();
		sha256 = model.getSHA256();
		banner = model.getBanner();

		setStagefile(model.getStagefile());
		setBackbmp(model.getBackbmp());
        if(preview == null || preview.length() == 0) {
            setPreview(model.getPreview());
        }
		try {
			level = Integer.parseInt(model.getPlaylevel());
		} catch(NumberFormatException e) {

		}
		mode = model.getMode().id;
		if(difficulty == 0) {
			difficulty = model.getDifficulty();			
		}
		judge = model.getJudgerank();
		minbpm = (int) model.getMinBPM();
		maxbpm = (int) model.getMaxBPM();
		length = model.getLastTime();
		notes = model.getTotalNotes();

		timelines = model.getAllTimeLines();

		feature = model.containsLongNote() ? FEATURE_LONGNOTE : 0;
		feature |= model.containsMineNote() ? FEATURE_MINENOTE : 0;
		feature |= model.getRandom() != null && model.getRandom().length > 0 ? FEATURE_RANDOM : 0;
		content |= model.getBgaList().length > 0 ? CONTENT_BGA : 0;
	}

	public BMSModel getBMSModel() {
		return model;
	}

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
	public String getFullTitle() {
		if(fulltitle == null) {
			fulltitle = title + " " + subtitle;
		}
		return fulltitle;
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
	public String getFullArtist() {
		if(fullartist == null) {
			fullartist = artist + " " + subartist;
		}
		return fullartist;
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

	public int getNotes() {
		return notes;
	}

	public void setNotes(int totalnotes) {
		this.notes = totalnotes;
	}

	public int getJudge() {
		return judge;
	}

	public void setJudge(int judge) {
		this.judge = judge;
	}

	public TimeLine[] getTimelines() {
		return timelines;
	}

	public String getStagefile() {
		return stagefile;
	}

	public void setStagefile(String stagefile) {
		this.stagefile = stagefile;
	}

	public String getBackbmp() {
		return backbmp;
	}

	public void setBackbmp(String backbmp) {
		this.backbmp = backbmp;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public SongInformation getInformation() {
		return info;
	}

	public void setInformation(SongInformation info) {
		this.info = info;
	}
	
	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
}
