package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * スキンのソースイメージ
 * 
 * @author exch
 */
public interface SkinSource extends Disposable {

	public boolean validate();
	
	public TextureRegion getImage(long time, MainState state);
}
