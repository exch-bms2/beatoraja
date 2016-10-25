package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンのソースイメージ
 * 
 * @author exch
 */
public class SkinSource {

	/**
	 * イメージ
	 */
	private TextureRegion[] image;

	private final int timer;
	private final int cycle;

	public SkinSource(TextureRegion[] image, int timer, int cycle) {
		this.image = image;
		this.timer = timer;
		this.cycle = cycle;
	}
	
	public TextureRegion getImage(long time, MainState state) {
		if(image != null) {
			return image[getImageIndex(image.length, time, state)];			
		}
		return null;
	}

	public int getImageIndex(int length, long time, MainState state) {
		if(cycle == 0) {
			return 0;
		}

		if(timer != 0 && timer < 256) {
			if(state.getTimer()[timer] == Long.MIN_VALUE) {
				return 0;
			}
			time -= state.getTimer()[timer];
		}
		if(time < 0) {
			return 0;
		}
//		System.out.println(index + " / " + image.length);
		return ((int) (time / (((float)cycle)  / length))) % length;
	}
	
	public void dispose() {
		if(image != null) {
			for(TextureRegion tr : image) {
				tr.getTexture().dispose();
			}
			image = null;
		}
	}
}
