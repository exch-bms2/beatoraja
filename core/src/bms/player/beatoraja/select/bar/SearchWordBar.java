package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

/**
 * 検索用バー
 *
 * @author exch
 */
public class SearchWordBar extends DirectoryBar {

    private String text;
    private String title;

    public SearchWordBar(MusicSelector selector, String text) {
        super(selector);
        this.text = text;
        title = "Search : '" + text + "'";
    }

    @Override
    public Bar[] getChildren() {
        return SongBar.toSongBarArray(selector.getSongDatabase().getSongDatasByText(text));
    }

    public void updateFolderStatus() {
        updateFolderStatus(selector.getSongDatabase().getSongDatasByText(text));
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return null;
    }
}
