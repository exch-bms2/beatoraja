package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

/**
 * 検索用バー
 *
 * @author exch
 */
public class SearchWordBar extends DirectoryBar {

    private final String text;
    private final String title;

    public SearchWordBar(MusicSelector selector, String text) {
        super(selector);
        this.text = text;
        title = "Search : '" + text + "'";
    }

    @Override
    public final Bar[] getChildren() {
        return SongBar.toSongBarArray(selector.getSongDatabase().getSongDatasByText(text));
    }

    public final void updateFolderStatus() {
        updateFolderStatus(selector.getSongDatabase().getSongDatasByText(text));
    }

    @Override
    public final String getTitle() {
        return title;
    }
}
