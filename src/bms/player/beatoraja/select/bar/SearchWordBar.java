package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 検索用バー
 *
 * @author exch
 */
public class SearchWordBar extends DirectoryBar {

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
