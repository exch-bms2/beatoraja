package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class SkinSlider extends SkinObject {

	/**
	 * イメージ
	 */
	private TextureRegion[] image;

	private int timing;

	private int muki;
	private int range = 100;
	private int type;
	
	private boolean changable;

	public SkinSlider(TextureRegion[] image, int cycle, int muki, int range, int type) {
		this.image = image;
		setCycle(cycle);
		this.muki = muki;
		this.range = range;
		this.type = type;
	}

	public TextureRegion[] getImage() {
		return image;
	}

	public TextureRegion getImage(long time) {
		if (getCycle() == 0) {
			return image[0];
		}
		final int index = (int) ((time / (getCycle() / image.length))) % image.length;
		// System.out.println(index + " / " + image.length);
		return image[index];
	}

	public void setImage(TextureRegion[] image, int cycle) {
		this.image = image;
		setCycle(cycle);
	}

	public int getTiming() {
		return timing;
	}

	public void setTiming(int timing) {
		this.timing = timing;
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (image == null) {
			return;
		}
		Rectangle r = this.getDestination(time,state);
		if (r != null) {
			TextureRegion image = getImage(time);
			draw(sprite, image, r.x
					+ (muki == 1 ? state.getSliderValue(type) * range : (muki == 3 ? -state.getSliderValue(type) * range : 0)), r.y
					+ (muki == 0 ? state.getSliderValue(type) * range : (muki == 2 ? -state.getSliderValue(type) * range : 0)),
					r.width, r.height, getColor(time,state),getAngle(time,state));
		}
	}

	public void dispose() {
		if (image != null) {
			for (TextureRegion tr : image) {
				tr.getTexture().dispose();
			}
			image = null;
		}
	}

	public boolean isChangable() {
		return changable;
	}

	public void setChangable(boolean changable) {
		this.changable = changable;
	}
	
	public int getRange() {
		return range;
	}
	
	public int getType() {
		return type;
	}
	
	public int getSliderAngle() {
		return muki;
	}
}
