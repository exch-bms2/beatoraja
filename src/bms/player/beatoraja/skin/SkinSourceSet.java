package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import bms.player.beatoraja.DisposableObject;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Validatable;

/**
 * スキンのソースイメージセット
 * 
 * @author exch
 */
public abstract class SkinSourceSet extends DisposableObject implements Validatable {
	
	public abstract TextureRegion[] getImages(long time, MainState state);
}
