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

	/**
	 * slider移動方向(0:上, 1:右, 2:下, 3:左)
	 */
	private int direction;
	/**
	 * slider移動範囲
	 */
	private int range = 100;
	/**
	 * slider値参照ID
	 */
	private int type;
	/**
	 * ユーザーによる値変更を受け付けるかどうか
	 */
	private boolean changable;

	public SkinSlider(TextureRegion[] image, int cycle, int angle, int range, int type) {
		this.image = image;
		setCycle(cycle);
		this.direction = angle;
		this.range = range;
		this.type = type;
	}

	public TextureRegion[] getImage() {
		return image;
	}

	public TextureRegion getImage(long time, MainState state) {
		if(getImageID() != -1) {
			return state.getImage(getImageID());
		}
		return image[getImageIndex(image.length, time, state)];
	}

	public void setImage(TextureRegion[] image, int cycle) {
		this.image = image;
		setCycle(cycle);
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (image == null) {
			return;
		}
		Rectangle r = this.getDestination(time,state);
		if (r != null) {
			TextureRegion image = getImage(time, state);
			draw(sprite, image, r.x
					+ (direction == 1 ? state.getSliderValue(type) * range : (direction == 3 ? -state.getSliderValue(type) * range : 0)), r.y
					+ (direction == 0 ? state.getSliderValue(type) * range : (direction == 2 ? -state.getSliderValue(type) * range : 0)),
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
		return direction;
	}
}
