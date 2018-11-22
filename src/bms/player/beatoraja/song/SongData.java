package bms.player.beatoraja.song;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bms.model.*;
import bms.player.beatoraja.Validatable;
import bms.tool.mdprocessor.IpfsInformation;

/**
 * 楽曲データ
 * 
 * @author exch
 */
public class SongData implements Validatable, IpfsInformation {
	
	public static final int FEATURE_UNDEFINEDLN = 1;
	public static final int FEATURE_MINENOTE = 2;
	public static final int FEATURE_RANDOM = 4;
	public static final int FEATURE_LONGNOTE = 8;
	public static final int FEATURE_CHARGENOTE = 16;
	public static final int FEATURE_HELLCHARGENOTE = 32;
	public static final int FEATURE_STOPSEQUENCE = 64;

	public static final int CONTENT_TEXT = 1;
	public static final int CONTENT_BGA = 2;

	public static final int FAVORITE_SONG = 1;
	public static final int FAVORITE_CHART = 2;

	/**
	 * 楽曲タイトル
	 */
	private String title = "";
	/**
	 * 楽曲サブタイトル
	 */
	private String subtitle = "";
	private String fulltitle;
	/**
	 * 楽曲ジャンル
	 */
	private String genre = "";
	/**
	 * 楽曲アーティスト名
	 */
	private String artist = "";
	/**
	 * 楽曲サブアーティスト名
	 */
	private String subartist = "";
	private String fullartist;
	private int favorite;
	private List<String> path = new ArrayList<String>();
	private String tag = "";
	private String md5 = "";
	private String sha256 = "";
	private String banner = "";
	private String url;
	private String appendurl;
	private String ipfs;
	private String appendipfs;
	private int date;
	private int adddate;
	private int level;
	private int mode;
	private int feature;
	private int difficulty;
	private int judge;
	private int minbpm;
	private int maxbpm;
	private int mainbpm;
	/**
	 * 曲の長さ(ms)
	 */
	private int length;
	private int content;
	private int notes;
	/**
	 * STAGEFILE path
	 */
	private String stagefile = "";
	/**
	 * BACKBMP path
	 */
	private String backbmp = "";
	/**
	 * PREVIEW path
	 */
	private String preview = "";

	private String folder = "";
	private String parent = "";

	private BMSModel model;
	private TimeLine[] timelines;
	private SongInformation info;

	private String charthash;
	private List<String> org_md5;

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
		setTitle(model.getTitle());
		setSubtitle(model.getSubTitle());
		genre = model.getGenre();
		setArtist(model.getArtist());
		setSubartist(model.getSubArtist());
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
		feature = 0;
		Map<Double, Integer> bpmMap = new HashMap<Double, Integer>();
		final int keys = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			Integer count = bpmMap.get(tl.getBPM());
			if (count == null) {
				count = 0;
			}
			bpmMap.put(tl.getBPM(), count + tl.getTotalNotes());
			if(tl.getStop() > 0) feature |= FEATURE_STOPSEQUENCE;

			for(int i = 0;i < keys;i++) {
				if(tl.getNote(i) instanceof LongNote) {
					switch(((LongNote) tl.getNote(i)).getType()) {
						case LongNote.TYPE_UNDEFINED:
							feature |= FEATURE_UNDEFINEDLN;
							break;
						case LongNote.TYPE_LONGNOTE:
							feature |= FEATURE_LONGNOTE;
							break;
						case LongNote.TYPE_CHARGENOTE:
							feature |= FEATURE_CHARGENOTE;
							break;
						case LongNote.TYPE_HELLCHARGENOTE:
							feature |= FEATURE_HELLCHARGENOTE;
							break;
					}
				}
				if(tl.getNote(i) instanceof MineNote) {
					feature |= FEATURE_MINENOTE;
				}
			}
		}
		int maxcount = 0;
		for (double bpm : bpmMap.keySet()) {
			if (bpmMap.get(bpm) > maxcount) {
				maxcount = bpmMap.get(bpm);
				mainbpm = (int) bpm;
			}
		}
		length = model.getLastTime();
		notes = model.getTotalNotes();

		timelines = model.getAllTimeLines();

		feature |= model.getRandom() != null && model.getRandom().length > 0 ? FEATURE_RANDOM : 0;
		feature |= model.containsLongNote() ? FEATURE_LONGNOTE : 0;
		content |= model.getBgaList().length > 0 ? CONTENT_BGA : 0;
		
		info = new SongInformation(model);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			charthash = BMSDecoder.convertHexString(md.digest(model.toChartString().getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
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
		fulltitle = null;
	}
	
	public String getSubtitle() {
		return subtitle;
	}
	
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
		fulltitle = null;
	}
	
	public String getFullTitle() {
		if(fulltitle == null) {
			fulltitle = subtitle.length() > 0 ? title + " " + subtitle : title;
		}
		return fulltitle;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
		fullartist = null;
	}
	public String getSubartist() {
		return subartist;
	}
	
	public void setSubartist(String subartist) {
		this.subartist = subartist;
		fullartist = null;
	}
	
	public String getFullArtist() {
		if(fullartist == null) {
			fullartist = subartist.length() > 0 ? artist + " " + subartist : artist;
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

	public int getMainbpm() {
		return mainbpm;
	}

	public void setMainbpm(int mainbpm) {
		this.mainbpm = mainbpm;
	}

	public boolean isBpmstop() {
		return (feature & FEATURE_STOPSEQUENCE) != 0;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	public boolean isBannerExists(){
		if(this.getBanner() != null && this.getBanner().length() > 0) {
			return Files.exists(Paths.get(this.getPath()).getParent().resolve(this.getBanner()));
		}
		return false;
	}
	
	public boolean hasDocument() {
		return (content & 1) != 0;
	}
	
	public boolean hasBGA() {
		return (content & 2) != 0;
	}
	
	public boolean hasRandomSequence() {
		return (feature & FEATURE_RANDOM) != 0;
	}

	public boolean hasMineNote() {
		return (feature & FEATURE_MINENOTE) != 0;
	}

	public boolean hasUndefinedLongNote() {
		return (feature & FEATURE_UNDEFINEDLN) != 0;
	}

	public boolean hasLongNote() {
		return (feature & FEATURE_LONGNOTE) != 0;
	}

	public boolean hasAnyLongNote() {
		return (feature & (FEATURE_UNDEFINEDLN | FEATURE_LONGNOTE | FEATURE_CHARGENOTE | FEATURE_HELLCHARGENOTE)) != 0;
	}

	public String getMd5() {
		if(md5 == null) {
			md5 = "";
		}
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	public String getSha256() {
		if(sha256 == null) {
			sha256 = "";
		}
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

	public boolean isStagefileExists(){
		if(this.getStagefile() != null && this.getStagefile().length() > 0) {
			return Files.exists(Paths.get(this.getPath()).getParent().resolve(this.getStagefile()));
		}
		return false;
	}

	public String getBackbmp() {
		return backbmp;
	}

	public void setBackbmp(String backbmp) {
		this.backbmp = backbmp;
	}

	public boolean isBackbmpExists(){
		if(this.getBackbmp() != null && this.getBackbmp().length() > 0) {
			return Files.exists(Paths.get(this.getPath()).getParent().resolve(this.getBackbmp()));
		}
		return false;
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAppendurl() {
		return appendurl;
	}

	public void setAppendurl(String appendurl) {
		this.appendurl = appendurl;
	}

	public String getCharthash() {
		return charthash;
	}

	public void setCharthash(String charthash) {
		this.charthash = charthash;
	}

	public String getIpfs() {
		return ipfs;
	}

	public void setIpfs(String ipfs) {
		this.ipfs = ipfs;
	}

	public String getAppendIpfs() {
		return appendipfs;
	}

	public void setAppendIpfs(String appendipfs) {
		this.appendipfs = appendipfs;
	}

	public List<String> getOrg_md5() {
		return org_md5;
	}

	public void setOrg_md5(List<String> org_md5) {
		this.org_md5 = org_md5;
	}
	
	public void merge(SongData song) {
		if(url == null || url.length() == 0) {
			url = song.getUrl();
		}
		if(appendurl == null || appendurl.length() == 0) {
			appendurl = song.getAppendurl();
		}
	}
	
	public void shrink() {
		fulltitle = fullartist = null;
		path.clear();
		date = adddate = level = mode = feature = difficulty = judge = minbpm = maxbpm = mainbpm = notes = length = 0;
		folder = parent = preview = "";
	}

	@Override
	public boolean validate() {
		if(title == null || title.length() == 0) {
			return false;
		}
		if((md5 == null || md5.length() == 0) && (sha256 == null || sha256.length() == 0)) {
			return false;
		}
		if(subtitle == null) {
			subtitle = "";
		}
		if(artist == null) {
			artist = "";
		}
		if(subartist == null) {
			subartist = "";
		}
		if(banner == null) {
			banner = "";
		}
		if(backbmp == null) {
			backbmp = "";
		}
		if(stagefile == null) {
			stagefile = "";
		}
		return true;
	}
}
