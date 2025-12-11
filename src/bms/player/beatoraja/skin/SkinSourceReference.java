package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import bms.player.beatoraja.MainState;

/**
 * スキンのソースイメージ(システム参照)
 *
 * @author exch
 */
public class SkinSourceReference extends SkinSource {

	private final int id;
	
	public SkinSourceReference(int id) {
		this.id = id;
	}
	
	@Override
	public void dispose() {
	}

	public boolean validate() {
		return true;
	}
	
	@Override
	public TextureRegion getImage(long time, MainState state) {
		return state.getImage(id);
	}
}
