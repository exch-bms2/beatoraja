package bms.player.beatoraja.play.bga;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public interface MovieProcessor {

	public abstract void create(String filepath);

	public abstract Texture getBGAData(boolean cont);
	
	public abstract void dispose();
}
