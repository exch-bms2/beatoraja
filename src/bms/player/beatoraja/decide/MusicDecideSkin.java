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
    	
    }

	public MusicDecideSkin(Rectangle r) {
		dw = r.width / 1280.0f;
		dh = r.height / 720.0f;

        SkinText genre = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        setDestination(genre, 0, 300, 420, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        setDestination(genre, 3000, 380, 420, 18, 18, 0, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setGenre(genre);
        SkinText title = new SkinText("skin/VL-Gothic-Regular.ttf",0,24);
        setDestination(title, 0, 340, 360, 18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setTitle(title);
        SkinText artist = new SkinText("skin/VL-Gothic-Regular.ttf",0,20);
        setDestination(artist, 0, 380, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        setDestination(artist, 3000, 300, 300,18, 18, 0, 255,255,255,255, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.setArtist(artist);
    }
    
    private void setDestination(SkinObject object, long time, float x, float y, float w, float h, int acc, int a, int r,
			int g, int b, int blend, int filter, int angle, int center, int loop, int timer, int op1, int op2, int op3) {
    	object.setDestination(time, x * dw, y * dh, w * dw, h * dh, acc, a, r, g, b, blend, filter, angle, center, loop, timer, op1, op2, op3);
    }
}
