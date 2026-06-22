package bms.player.beatoraja.skin;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import bms.player.beatoraja.MainState;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * スキンのソースイメージ(システム参照)
 *
 * @author exch
 */
public class SkinSourceReference extends SkinSource {

	private static TextureRegion black;
	private static TextureRegion white;

	private final Function<MainState, TextureRegion> ref;
	
	public SkinSourceReference(int id) {
		if(black == null) {
			Pixmap plainPixmap = new Pixmap(2,1, Pixmap.Format.RGBA8888);
			plainPixmap.drawPixel(0,0, Color.toIntBits(255,0,0,0));
			plainPixmap.drawPixel(1,0, Color.toIntBits(255,255,255,255));
			Texture plainTexture = new Texture(plainPixmap);
			black = new TextureRegion(plainTexture,0,0,1,1);
			white = new TextureRegion(plainTexture,1,0,1,1);
			plainPixmap.dispose();
		}

		ref = switch(id) {
			case 101 -> state -> state.resource.getBMSResource().getBackbmp();
			case 100 -> state -> state.resource.getBMSResource().getStagefile();
			case 102 -> state -> state.resource.getBMSResource().getBanner();
			case 110 -> state -> black;
			case 111 -> state -> white;
			default -> (state) -> null;
		};
	}
	
	@Override
	public void dispose() {
	}

	public boolean validate() {
		return true;
	}
	
	@Override
	public TextureRegion getImage(long time, MainState state) {
		return ref.apply(state);
	}
}
