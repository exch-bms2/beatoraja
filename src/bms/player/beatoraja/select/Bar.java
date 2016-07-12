package bms.player.beatoraja.select;

import bms.player.beatoraja.*;
import bms.player.beatoraja.play.bga.GdxVideoProcessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

    @Override
    public String getTitle() {
        return song.getTitle();
    }

    public int getLamp() {
        if (getScore() != null) {
            return getScore().getClear();
        }
        return 0;
    }

    public void loadBanner() {
        File bannerfile = new File(song.getPath().substring(0, song.getPath().lastIndexOf(File.separatorChar) + 1)
                + song.getBanner());
        // System.out.println(bannerfile.getPath());
        if (song.getBanner().length() > 0 && bannerfile.exists()) {
            try {
                banner = new Pixmap(Gdx.files.internal(bannerfile.getPath()));
            } catch (GdxRuntimeException e) {
                Logger.getGlobal().warning("banner読み込み失敗: " + e.getMessage());
            }
        }
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
        SongDatabaseAccessor songdb = selector.getSongDatabase();
        FolderData[] folders = songdb.getFolderDatas("parent", crc, new File(".").getAbsolutePath());
        SongData[] songs = songdb.getSongDatas("parent", crc, new File(".").getAbsolutePath());
        if (songs.length == 0) {
            for (FolderData folder : folders) {
                String path = folder.getPath();
                if (path.endsWith(String.valueOf(File.separatorChar))) {
                    path = path.substring(0, path.length() - 1);
                }

                String ccrc = songdb.crc32(path, new String[0], new File(".").getAbsolutePath());
                FolderBar cfolder = new FolderBar(selector, folder, ccrc);
                l.add(cfolder);
            }
        } else {
            for (SongData song : songs) {
                l.add(new SongBar(song));
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
        String ccrc = songdb.crc32(path, new String[0], new File(".").getAbsolutePath());
        int clear = 255;
        int[] clears = new int[11];
        int[] ranks = new int[28];
        SongData[] songdatas = songdb.getSongDatas("parent", ccrc, new File(".").getAbsolutePath());
        Map<String, IRScoreData> scores = selector.readScoreDatas(songdatas, selector.getResource().getConfig()
                .getLnmode());
        for (SongData sd : songdatas) {
            IRScoreData score = scores.get(sd.getSha256());
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

    private String name;
    private TableLevelBar[] levels;
    private GradeBar[] grades;
    private MusicSelector selector;

    public TableBar(MusicSelector selector, String name, TableLevelBar[] levels, GradeBar[] grades) {
        this.selector = selector;
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
        SongData[] songs = selector.getSongDatabase().getSongDatas(getHashes(), new File(".").getAbsolutePath());
        for (SongData song : songs) {
            songbars.add(new SongBar(song));
        }
        return songbars.toArray(new Bar[0]);
    }

    public void updateFolderStatus() {
        int clear = 255;
        int[] clears = new int[11];
        int[] ranks = new int[28];
        SongData[] songs = selector.getSongDatabase().getSongDatas(getHashes(), new File(".").getAbsolutePath());
        Map<String, IRScoreData> scores = selector
                .readScoreDatas(songs, selector.getResource().getConfig().getLnmode());
        for (SongData song : songs) {
            IRScoreData score = scores.get(song.getSha256());
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
            SongData[] song = selector.getSongDatabase().getSongDatas("sha256", score.getSha256(), new File(".").getAbsolutePath());
            if (song.length > 0 && (!song[0].hasLongNote() || selector.getResource().getConfig().getLnmode() == score.getMode())) {
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
        SongData[] songs = selector.getSongDatabase().getSongDatas(text, new File(".").getAbsolutePath());
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
