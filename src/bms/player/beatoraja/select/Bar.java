package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.song.FolderData;
import bms.player.beatoraja.song.*;

import com.badlogic.gdx.graphics.Pixmap;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public abstract class Bar {

    private IRScoreData score;

    public abstract String getTitle();

    public IRScoreData getScore() {
        return score;
    }

    public void setScore(IRScoreData score) {
        this.score = score;
    }

    public abstract int getLamp();
}

abstract class SelectableBar extends Bar {

    /**
     * リプレイデータが存在するか
     */
    private boolean[] existsReplay = new boolean[0];

    public boolean existsReplayData() {
        for (boolean b : existsReplay) {
            if (b) {
                return b;
            }
        }
        return false;
    }

    public boolean[] getExistsReplayData() {
        return existsReplay;
    }

    public void setExistsReplayData(boolean[] existsReplay) {
        this.existsReplay = existsReplay;
    }

}

class SongBar extends SelectableBar {

    private SongData song;

    private Pixmap banner;

    private TableData.TableSong info;

    public SongBar(SongData song) {
        this.song = song;
    }

    public SongBar(SongData song, TableData.TableSong info) {
    	if(song == null) {
    		song = new SongData();
    		song.setTitle(info.getTitle());
    		song.setArtist(info.getArtist());
    		song.setGenre(info.getGenre());
    	}
        this.song = song;
        this.info = info;
    }

    public SongData getSongData() {
        return song;
    }
    
    public boolean existsSong() {
    	return song.getSha256() != null;
    }

    public TableData.TableSong getSongInformation() {
        return info;
    }

    public Pixmap getBanner() {
        return banner;
    }
    
    public void setBanner(Pixmap banner) {
    	this.banner = banner;
    }

    @Override
    public String getTitle() {
        return song.getFullTitle();
    }

    public int getLamp() {
        if (getScore() != null) {
            return getScore().getClear();
        }
        return 0;
    }
}

class GradeBar extends SelectableBar {

    private SongData[] songs;
    private String name;

    private CourseData course;
    private IRScoreData mscore;
    private IRScoreData rscore;

    public GradeBar(String name, SongData[] songs, CourseData course) {
        this.songs = songs;
        this.name = name;
        this.course = course;
    }

    public SongData[] getSongDatas() {
        return songs;
    }

    @Override
    public String getTitle() {
        return (course.isClassCourse() ? "段位認定 " : "") + name;
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

    public CourseData.CourseDataConstraint[] getConstraint() {
        if (course.getConstraint() != null) {
            return course.getConstraint();
        }
        return new CourseData.CourseDataConstraint[0];
    }

    public TrophyData[] getAllTrophy() {
        return course.getTrophy();
    }

    public TrophyData getTrophy() {
        for (TrophyData trophy : course.getTrophy()) {
            if (qualified(this.getScore(), trophy)) {
                return trophy;
            }
            if (qualified(mscore, trophy)) {
                return trophy;
            }
            if (qualified(rscore, trophy)) {
                return trophy;
            }
        }
        return null;
    }

    private boolean qualified(IRScoreData score, TrophyData trophy) {
        return score != null && score.getNotes() != 0
                && trophy.getMissrate() >= score.getMinbp() * 100.0 / score.getNotes()
                && trophy.getScorerate() <= score.getExscore() * 100.0 / (score.getNotes() * 2);
    }

    public int getLamp() {
        int result = 0;
        if (getScore() != null && getScore().getClear() > result) {
            result = getScore().getClear();
        }
        if (getMirrorScore() != null && getMirrorScore().getClear() > result) {
            result = getMirrorScore().getClear();
        }
        if (getRandomScore() != null && getRandomScore().getClear() > result) {
            result = getRandomScore().getClear();
        }
        return result;
    }
}

abstract class DirectoryBar extends Bar {

    private int[] lamps = new int[11];
    private int[] ranks = new int[0];

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

    public int getLamp() {
        for (int i = 0; i < lamps.length; i++) {
            if (lamps[i] > 0) {
                return i;
            }
        }
        return 0;
    }

    public abstract Bar[] getChildren();

}

class FolderBar extends DirectoryBar {

    private FolderData folder;
    private String crc;
    private MusicSelector selector;

    public FolderBar(MusicSelector selector, FolderData folder, String crc) {
        this.selector = selector;
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

    @Override
    public Bar[] getChildren() {
        List<Bar> l = new ArrayList<Bar>();
        final String rootpath = Paths.get(".").toAbsolutePath().toString();
        SongDatabaseAccessor songdb = selector.getSongDatabase();
        FolderData[] folders = songdb.getFolderDatas("parent", crc);
        SongData[] songs = songdb.getSongDatas("parent", crc);
        if (songs.length == 0) {
            for (FolderData folder : folders) {
                String path = folder.getPath();
                if (path.endsWith(String.valueOf(File.separatorChar))) {
                    path = path.substring(0, path.length() - 1);
                }

                String ccrc = SongUtils.crc32(path, new String[0], rootpath);
                FolderBar cfolder = new FolderBar(selector, folder, ccrc);
                l.add(cfolder);
            }
        } else {
        	List<String> sha = new ArrayList<String>();
            for (SongData song : songs) {
            	if(!sha.contains(song.getSha256())) {
                    l.add(new SongBar(song));
                    sha.add(song.getSha256());
            	}
            }
        }
        return l.toArray(new Bar[0]);
    }

    public void updateFolderStatus() {
        SongDatabaseAccessor songdb = selector.getSongDatabase();
        String path = folder.getPath();
        if (path.endsWith(String.valueOf(File.separatorChar))) {
            path = path.substring(0, path.length() - 1);
        }
        final String ccrc = SongUtils.crc32(path, new String[0], new File(".").getAbsolutePath());
        int clear = 255;
        int[] clears = new int[11];
        int[] ranks = new int[28];
        final SongData[] songdatas = songdb.getSongDatas("parent", ccrc);
        final Map<String, IRScoreData> scores = selector.getScoreDataCache().readScoreDatas(songdatas, selector.getMainController().getPlayerResource().getPlayerConfig()
                .getLnmode());
        for (SongData sd : songdatas) {
            final IRScoreData score = scores.get(sd.getSha256());
            if (score != null) {
                clears[score.getClear()]++;
                if (score.getNotes() != 0) {
                    ranks[(score.getExscore() * 27 / (score.getNotes() * 2))]++;
                } else {
                    ranks[0]++;
                }
                if (score.getClear() < clear) {
                    clear = score.getClear();
                }
            } else {
                ranks[0]++;
                clears[0]++;
                clear = 0;
            }
        }
        setLamps(clears);
        setRanks(ranks);

    }
}

class ContainerBar extends DirectoryBar {

	private String title;
    private Bar[] childbar;

    public ContainerBar(String title, Bar[] bar) {
        this.title = title;
        childbar = bar;
    }

    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public Bar[] getChildren() {
        return childbar;
    }
}

class SameFolderBar extends DirectoryBar {

    private MusicSelector selector;
    private String crc;
    private String title;

    public SameFolderBar(MusicSelector selector, String title, String crc) {
        this.selector = selector;
        this.crc = crc;
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Bar[] getChildren() {
        List<Bar> l = new ArrayList<Bar>();
        SongData[] songs = selector.getSongDatabase().getSongDatas("folder", crc);
        List<String> sha = new ArrayList<String>();
        for (SongData song : songs) {
            if(!sha.contains(song.getSha256())) {
                l.add(new SongBar(song));
                sha.add(song.getSha256());
            }
        }
        return l.toArray(new Bar[0]);
    }
}

class TableBar extends DirectoryBar {

	private TableData td;
    private HashBar[] levels;
    private GradeBar[] grades;
    private MusicSelector selector;

    public TableBar(MusicSelector selector, TableData td) {
        this.selector = selector;
        setTableData(td);
    }

    @Override
    public String getTitle() {
        return td.getName();
    }
    
    public String getUrl() {
    	return td.getUrl();
    }
    
    public void setTableData(TableData td) {
    	this.td = td;

    	final long t = System.currentTimeMillis();
		List<HashBar> levels = new ArrayList<HashBar>();
		for (TableData.TableFolder lv : td.getFolder()) {
			levels.add(new HashBar(selector, lv.getName(), lv.getSong()));
		}

		this.levels = levels.toArray(new HashBar[levels.size()]);
		List<GradeBar> l = new ArrayList<GradeBar>();
		
		Set<String> hashset = new HashSet<String>();
		for (CourseData course : td.getCourse()) {
			for (String hash : course.getHash()) {
				hashset.add(hash);
			}
		}
		SongData[] songs = selector.getSongDatabase().getSongDatas(hashset.toArray(new String[hashset.size()]));
		
		for (CourseData course : td.getCourse()) {
			List<SongData> songlist = new ArrayList<SongData>();
			for (String hash : course.getHash()) {
				SongData song = null;
				for(SongData sd :songs) {
					if(hash.equals(sd.getMd5())) {
						song = sd;
						break;
					}
				}
				songlist.add(song);
			}

			l.add(new GradeBar(course.getName(), songlist.toArray(new SongData[0]), course));
		}
		grades = l.toArray(new GradeBar[l.size()]);
    }

    public HashBar[] getLevels() {
        return levels;
    }

    public GradeBar[] getGrades() {
        return grades;
    }

    @Override
    public Bar[] getChildren() {
        List<Bar> l = new ArrayList<Bar>();
        l.addAll(Arrays.asList(getLevels()));
        l.addAll(Arrays.asList(getGrades()));
        return l.toArray(new Bar[0]);
    }
}



/**
 * ハッシュ集合を持ち、各ハッシュ値に該当する楽曲を含むフォルダバー
 *
 * @author exch
 */
class HashBar extends DirectoryBar {
    private String title;
    private TableData.TableSong[] elements;
    private MusicSelector selector;
    private SongData[] songs;

    public HashBar(MusicSelector selector, String title, TableData.TableSong[] elements) {
        this.selector = selector;
        this.title = title;
        this.elements = elements;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public TableData.TableSong[] getElements() {
        return elements;
    }

    public void setElements(TableData.TableSong[] elements) {
        this.elements = elements;
        songs = null;
    }

    @Override
    public Bar[] getChildren() {
        List<SongBar> songbars = new ArrayList<SongBar>();
        String[] hashes = new String[elements.length];
        for(int i = 0;i < hashes.length;i++) {
            hashes[i] = elements[i].getHash();
        }
        if(songs == null) {
            songs = selector.getSongDatabase().getSongDatas(hashes);
        }
        for(TableData.TableSong element : elements) {
            boolean exist = false;
            for (SongData song : songs) {
                if(element.getHash().equals(song.getMd5()) || element.getHash().equals(song.getSha256())) {
                    songbars.add(new SongBar(song, element));
                    exist = true;
                    break;
                }
            }
            if(!exist && element.getTitle() != null) {
                songbars.add(new SongBar(null, element));
            }
        }

        return songbars.toArray(new Bar[0]);
    }

    public void updateFolderStatus() {
        int clear = 255;
        int[] clears = new int[11];
        int[] ranks = new int[28];
        String[] hashes = new String[elements.length];
        for(int i = 0;i < hashes.length;i++) {
            hashes[i] = elements[i].getHash();
        }
        songs = selector.getSongDatabase().getSongDatas(hashes);
        final Map<String, IRScoreData> scores = selector.getScoreDataCache()
                .readScoreDatas(songs, selector.getMainController().getPlayerResource().getPlayerConfig().getLnmode());
        for (SongData song : songs) {
            final IRScoreData score = scores.get(song.getSha256());
            if (score != null) {
                clears[score.getClear()]++;
                if (score.getNotes() != 0) {
                    ranks[(score.getExscore() * 27 / (score.getNotes() * 2))]++;
                } else {
                    ranks[0]++;
                }
                if (score.getClear() < clear) {
                    clear = score.getClear();
                }
            } else {
                ranks[0]++;
                clears[0]++;
                clear = 0;
            }
        }
        setLamps(clears);
        setRanks(ranks);
    }
}

/**
 * SQLで問い合わせた楽曲を表示するためのバー
 *
 * @author exch
 */
class CommandBar extends DirectoryBar {

    // TODO song.dbへの問い合わせの追加

    private MainController main;
    private MusicSelector selector;
    private String title;
    private String sql;
    private int type;

    public CommandBar(MainController main, MusicSelector selector, String title, String sql) {
    	this(main, selector, title, sql, 0);
    }

    public CommandBar(MainController main, MusicSelector selector, String title, String sql, int type) {
        this.main = main;
        this.selector = selector;
        this.title = title;
        this.sql = sql;
        this.type = type;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getLamp() {
        return 0;
    }

    @Override
    public Bar[] getChildren() {
    	if(type == 2) {
    		if(main.getInfoDatabase() == null) {
    			return new Bar[0];
    		}
    		SongInformation[] infos = main.getInfoDatabase().getInformations(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (SongInformation info : infos) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", info.getSha256());
                if(song.length > 0) {
                    l.add(new SongBar(song[0]));                	
                }
            }
            return l.toArray(new Bar[l.size()]);    		    		
    	} else if(type == 1) {
            SongData[] infos = main.getSongDatabase().getSongDatas(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (SongData info : infos) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", info.getSha256());
                if(song.length > 0) {
                    l.add(new SongBar(song[0]));
                }
            }
            return l.toArray(new Bar[l.size()]);
        } else{
            List<IRScoreData> scores = main.getPlayDataAccessor().readScoreDatas(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (IRScoreData score : scores) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", score.getSha256());
                if (song.length > 0 && (!song[0].hasUndefinedLongNote() || selector.getMainController().getPlayerResource().getPlayerConfig().getLnmode() == score.getMode())) {
                    l.add(new SongBar(song[0]));
                }
            }
            return l.toArray(new Bar[l.size()]);
        }
    }

}

/**
 * 検索用バー
 *
 * @author exch
 */
class SearchWordBar extends DirectoryBar {

    private String text;
    private MusicSelector selector;

    public SearchWordBar(MusicSelector selector, String text) {
        this.selector = selector;
        this.text = text;
    }

    @Override
    public Bar[] getChildren() {
        List<SongBar> songbars = new ArrayList<SongBar>();
        SongData[] songs = selector.getSongDatabase().getSongDatasByText(text, new File(".").getAbsolutePath());
        for (SongData song : songs) {
            songbars.add(new SongBar(song));
        }
        return songbars.toArray(new Bar[0]);
    }

    @Override
    public String getTitle() {
        return "Search : '" + text + "'";
    }

}
