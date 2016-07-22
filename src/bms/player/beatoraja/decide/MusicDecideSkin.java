package bms.player.beatoraja.decide;

import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.skin.*;

/**
 * 曲決定部分のスキン
 *
 * @author exch
 */
public class MusicDecideSkin extends Skin {

	private float dw;
	private float dh;
	
    public MusicDecideSkin() {
        super(640, 480, 1280, 720);
    }

	public MusicDecideSkin(Rectangle r) {
        super(1280, 720, r.width, r.height);
		dw = r.width / 1280.0f;
		dh = r.height / 720.0f;

        SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf",0,20, 2);
        genre.setTextResourceAccessor(TextResourceAccessor.GENRE);
        setDestination(genre, 0, 300, 420, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        setDestination(genre, 3000, 380, 420, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.add(genre);
        SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf",0,24, 2);
        title.setTextResourceAccessor(TextResourceAccessor.TITLE);
        setDestination(title, 0, 340, 360, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.add(title);
        SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf",0,20, 2);
        artist.setTextResourceAccessor(TextResourceAccessor.ARTIST);
        setDestination(artist, 0, 380, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        setDestination(artist, 3000, 300, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.add(artist);
    }
}
