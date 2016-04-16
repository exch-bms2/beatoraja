package bms.player.beatoraja.decide;

import bms.player.beatoraja.skin.*;

public class MusicDecideSkin extends Skin {

    private SkinText genre;

    private SkinText title;
    private SkinText artist;

    public MusicDecideSkin() {
        genre = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        genre.setDestination(0, 300, 420, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        genre.setDestination(2000, 380, 420, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        title = new SkinText("skin/VL-Gothic-Regular.ttf",0,24);
        title.setDestination(0, 340, 360, 18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        artist = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        artist.setDestination(0, 380, 300,18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        artist.setDestination(2000, 300, 300,18, 18, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public SkinText getGenre() {
        return genre;
    }

    public void setGenre(SkinText genre) {
        this.genre = genre;
    }

    public SkinText getTitle() {
        return title;
    }

    public void setTitle(SkinText title) {
        this.title = title;
    }

    public SkinText getArtist() {
        return artist;
    }

    public void setArtist(SkinText artist) {
        this.artist = artist;
    }
}
