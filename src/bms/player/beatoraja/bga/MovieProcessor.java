package bms.player.beatoraja.bga;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public interface MovieProcessor {

	public abstract void create(String filepath);

	public abstract Texture getBGAData();
	
	public abstract void dispose();
}
