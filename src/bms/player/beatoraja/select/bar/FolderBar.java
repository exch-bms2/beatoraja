package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * ファイルシステムと連動したフォルダバー。
 *
 * @author exch
 */
public class FolderBar extends DirectoryBar {

    private final FolderData folder;
    private final String crc;

    public FolderBar(MusicSelector selector, FolderData folder, String crc) {
        super(selector);
        this.folder = folder;
        this.crc = crc;
    }

    public final FolderData getFolderData() {
        return folder;
    }

    public final String getCRC() {
        return crc;
    }

    @Override
    public final String getTitle() {
        return folder.getTitle();
    }

    @Override
    public Bar[] getChildren() {
        final SongDatabaseAccessor songdb = selector.getSongDatabase();
        final SongData[] songs = songdb.getSongDatas("parent", crc);
        if (songs.length > 0) {
            return SongBar.toSongBarArray(songs);
        }

        final String rootpath = Paths.get(".").toAbsolutePath().toString();
        return Stream.of(songdb.getFolderDatas("parent", crc)).map(folder -> {
            String path = folder.getPath();
            if (path.endsWith(String.valueOf(File.separatorChar))) {
                path = path.substring(0, path.length() - 1);
            }

            String ccrc = SongUtils.crc32(path, new String[0], rootpath);
            return new FolderBar(selector, folder, ccrc);
        }).toArray(Bar[]::new);
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
