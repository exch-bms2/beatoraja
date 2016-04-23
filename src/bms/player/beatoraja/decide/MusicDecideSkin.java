package bms.player.beatoraja.decide;

import bms.player.beatoraja.skin.*;

/**
 * 曲決定部分のスキン
 *
 * @author exch
 */
public class MusicDecideSkin extends Skin {

    public MusicDecideSkin() {
        SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        genre.setDestination(0, 300, 420, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        genre.setDestination(3000, 380, 420, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setGenre(genre);
        SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf",0,24);
        title.setDestination(0, 340, 360, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setTitle(title);
        SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        artist.setDestination(0, 380, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        artist.setDestination(3000, 300, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setArtist(artist);
    }
}
