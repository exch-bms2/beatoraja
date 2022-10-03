package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import bms.player.beatoraja.MainState;

/**
 * スキンのソースイメージセット
 * 
 * @author exch
 */
public interface SkinSourceSet extends Disposable {

	public boolean validate();
	
	public TextureRegion[] getImages(long time, MainState state);
}
