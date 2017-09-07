package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.FolderData;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import bms.player.beatoraja.song.SongUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by exch on 2017/09/03.
 */
public class FolderBar extends DirectoryBar {

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
