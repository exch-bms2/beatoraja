package bms.player.beatoraja.skin;

import bms.player.beatoraja.DisposableObject;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.Validatable;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンのソースイメージ
 * 
 * @author exch
 */
public abstract class SkinSource extends DisposableObject implements Validatable {

	public abstract TextureRegion getImage(long time, MainState state);
}
