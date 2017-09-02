package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * Created by exch on 2017/09/02.
 */
public class SongBar extends SelectableBar {

    private SongData song;

    private Pixmap banner;

    public SongBar(SongData song) {
        this.song = song;
    }

    public SongData getSongData() {
        return song;
    }

    public boolean existsSong() {
    	return song.getPath() != null;
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
