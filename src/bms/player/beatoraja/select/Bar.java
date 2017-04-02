package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.TableData.CourseData;
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

    public SongBar(SongData song) {
        this.song = song;
    }

    public SongData getSongData() {
        return song;
    }

    public Pixmap getBanner() {
        return banner;
    }
    
    public void setBanner(Pixmap banner) {
    	this.banner = banner;
    }

    @Override
    public String getTitle() {
        return song.getTitle() + " " + song.getSubtitle();
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
        if (course.getConstraint() != null) {
            return course.getConstraint();
        }
        return new int[0];
    }

    public TableData.TrophyData[] getAllTrophy() {
        return course.getTrophy();
    }

    public TableData.TrophyData getTrophy() {
        for (TableData.TrophyData trophy : course.getTrophy()) {
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

    private boolean qualified(IRScoreData score, TableData.TrophyData trophy) {
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
        final Map<String, IRScoreData> scores = selector.getScoreDataCache().readScoreDatas(songdatas, selector.getMainController().getPlayerResource().getConfig()
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

class TableBar extends DirectoryBar {

	private TableData td;
    private TableLevelBar[] levels;
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
		List<TableLevelBar> levels = new ArrayList<TableLevelBar>();
		for (String lv : td.getLevel()) {
			levels.add(new TableLevelBar(selector, lv, td.getHash().get(lv)));
		}

		this.levels = levels.toArray(new TableLevelBar[levels.size()]);
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

    public TableLevelBar[] getLevels() {
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

class TableLevelBar extends DirectoryBar {
    private String level;
    private String[] hashes;
    private MusicSelector selector;
    private SongData[] songs;

    public TableLevelBar(MusicSelector selector, String level, String[] hashes) {
        this.selector = selector;
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

    @Override
    public Bar[] getChildren() {
        List<SongBar> songbars = new ArrayList<SongBar>();
        if(songs == null) {
            songs = selector.getSongDatabase().getSongDatas(getHashes());
        }
    	List<String> sha = new ArrayList<String>();
        for (SongData song : songs) {
        	if(!sha.contains(song.getSha256())) {
        		songbars.add(new SongBar(song));
                sha.add(song.getSha256());
        	}
        }
        return songbars.toArray(new Bar[0]);
    }

    public void updateFolderStatus() {
        int clear = 255;
        int[] clears = new int[11];
        int[] ranks = new int[28];
        songs = selector.getSongDatabase().getSongDatas(getHashes());
        final Map<String, IRScoreData> scores = selector.getScoreDataCache()
                .readScoreDatas(songs, selector.getMainController().getPlayerResource().getConfig().getLnmode());
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

    public CommandBar(MainController main, MusicSelector selector, String title, String sql) {
        this.main = main;
        this.selector = selector;
        this.title = title;
        this.sql = sql;
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
        List<IRScoreData> scores = main.getPlayDataAccessor().readScoreDatas(sql);
        List<Bar> l = new ArrayList<Bar>();
        for (IRScoreData score : scores) {
            SongData[] song = selector.getSongDatabase().getSongDatas("sha256", score.getSha256());
            if (song.length > 0 && (!song[0].hasLongNote() || selector.getMainController().getPlayerResource().getConfig().getLnmode() == score.getMode())) {
                l.add(new SongBar(song[0]));
            }
        }
        return l.toArray(new Bar[0]);
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
