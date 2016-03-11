package bms.player.beatoraja.bga;

import com.badlogic.gdx.graphics.Pixmap;

public interface MovieProcessor {

	public abstract void create(String filepath);

	public abstract Pixmap getBGAData();
	
	public abstract void dispose();
}
