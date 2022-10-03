package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

import java.util.ArrayList;
import java.util.List;

/**
 * 同ディレクトリに配置されている全譜面を子に持つバー
 *
 * @author exch
 */
public class SameFolderBar extends DirectoryBar {

    private String crc;
    private String title;

    public SameFolderBar(MusicSelector selector, String title, String crc) {
        super(selector);
        this.crc = crc;
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return null;
    }

    @Override
    public Bar[] getChildren() {
        return SongBar.toSongBarArray(selector.getSongDatabase().getSongDatas("folder", crc));
    }
}
