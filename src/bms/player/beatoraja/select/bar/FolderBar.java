package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ファイルシステムと連動したフォルダバー。
 * 
 * @author exch
 */
public class FolderBar extends DirectoryBar {

    private FolderData folder;
    private String crc;

    public FolderBar(MusicSelector selector, FolderData folder, String crc) {
        super(selector);
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
        final String rootpath = Paths.get(".").toAbsolutePath().toString();
        SongDatabaseAccessor songdb = selector.getSongDatabase();
        SongData[] songs = songdb.getSongDatas("parent", crc);
        if (songs.length > 0) {
            List<Bar> l = new ArrayList<Bar>(songs.length);
        	List<String> sha = new ArrayList<String>(songs.length);
            for (SongData song : songs) {
            	if(!sha.contains(song.getSha256())) {
                    l.add(new SongBar(song));
                    sha.add(song.getSha256());
            	}
            } 	
            return l.toArray(new Bar[l.size()]);
        }
        
        FolderData[] folders = songdb.getFolderDatas("parent", crc);
        Bar[] l = new Bar[folders.length];
        
        for(int i = 0;i < folders.length;i++) {
            String path = folders[i].getPath();
            if (path.endsWith(String.valueOf(File.separatorChar))) {
                path = path.substring(0, path.length() - 1);
            }

            String ccrc = SongUtils.crc32(path, new String[0], rootpath);
            l[i] = new FolderBar(selector, folders[i], ccrc);
        }
        	
        return l;
    }

    public void updateFolderStatus() {
        SongDatabaseAccessor songdb = selector.getSongDatabase();
        String path = folder.getPath();
        if (path.endsWith(String.valueOf(File.separatorChar))) {
            path = path.substring(0, path.length() - 1);
        }
        final String ccrc = SongUtils.crc32(path, new String[0], new File(".").getAbsolutePath());
        
        updateFolderStatus(songdb.getSongDatas("parent", ccrc));
    }
}
