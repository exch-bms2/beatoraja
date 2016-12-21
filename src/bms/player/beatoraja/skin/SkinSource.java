package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * スキンのソースイメージ
 * 
 * @author exch
 */
public class SkinSource implements Disposable {

	/**
	 * イメージ
	 */
	private TextureRegion[][] image;

	private final int timer;
	private final int cycle;

	public SkinSource(TextureRegion image) {
		this(new TextureRegion[] { image }, 0, 0);
	}

	public SkinSource(TextureRegion[] image, int timer, int cycle) {
		this(new TextureRegion[][] { image }, timer, cycle);
	}

	public SkinSource(TextureRegion[][] image, int timer, int cycle) {
		this.image = image;
		this.timer = timer;
		this.cycle = cycle;
	}

	public TextureRegion getImage(long time, MainState state) {
		if (image != null && image.length > 0 & image[0] != null && image[0].length > 0) {
			return image[0][getImageIndex(image[0].length, time, state)];
		}
		return null;
	}

	public TextureRegion[] getImages(long time, MainState state) {
		if (image != null && image.length > 0) {
			return image[getImageIndex(image.length, time, state)];
		}
		return null;
	}

	public int getImageIndex(int length, long time, MainState state) {
		if (cycle == 0) {
			return 0;
		}

		if (timer != 0 && timer < 256) {
			if (state.getTimer()[timer] == Long.MIN_VALUE) {
				return 0;
			}
			time -= state.getTimer()[timer];
		}
		if (time < 0) {
			return 0;
		}
		// System.out.println(index + " / " + image.length);
		return (int) ((time * length / cycle) % length);
	}

	public void dispose() {
		if (image != null) {
			for (TextureRegion[] trs : image) {
				if (trs != null) {
					for (TextureRegion tr : trs) {
						tr.getTexture().dispose();
					}
				}
			}
			image = null;
		}
	}
}
