package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * �궛�꺀�깢�궎�깳�꺖�궦
 */
public class SkinGraph extends SkinObject implements SkinObserver {

	/**
	 * �궎�깳�꺖�궦
	 */
	private SkinSource source;
	/**
	 * �궛�꺀�깢�겗�뢿�뀱��
	 */
	private int id = -1;
	/**
	 * �궛�꺀�깢�겗鴉멥겞�굥�릲�걤(1:訝�, �걹�굦餓ε쨼:�뤂)
	 */
	private int direction = 1;
	/**
	 * NUMBER�ㅵ뢿�뀱�걢�겑�걝�걢
	 */
	private boolean isRefNum = false;
	/**
	 * NUMBER�ㅵ뢿�뀱�겗�졃�릦�겗��弱뤷��
	 */
	private int min = 0;
	/**
	 * NUMBER�ㅵ뢿�뀱�겗�졃�릦�겗��鸚㎩��
	 */
	private int max = 0;

	private final TextureRegion current = new TextureRegion();

	public SkinGraph(int imageid) {
		setImageID(imageid);
	}

	public SkinGraph(TextureRegion image) {
		source = new SkinSourceImage(new TextureRegion[] { image }, 0, 0);
	}

	public SkinGraph(TextureRegion[] image, int timer, int cycle) {
		source = new SkinSourceImage(image, timer, cycle);
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if (getImageID() != -1) {
			Rectangle r = this.getDestination(time, state);
			TextureRegion image = state.getImage(getImageID());
			if (r != null && image != null) {
				float value = id != -1 ? state.getSliderValue(id) : 0;
				if(id != -1 && isRefNum && max != min) {
					if(min < max) {
						if(state.getNumberValue(id) > max) value = 1;
						else if(state.getNumberValue(id) < min) value = 0;
						else value = Math.abs( ((float) state.getNumberValue(id) - min) / (max - min) );
					} else {
						if(state.getNumberValue(id) < max) value = 1;
						else if(state.getNumberValue(id) > min) value = 0;
						else value = Math.abs( ((float) state.getNumberValue(id) - min) / (max - min) );
					}
				}
				if (direction == 1) {
					current.setRegion(image, 0,
							image.getRegionY() + image.getRegionHeight() - (int) (image.getRegionHeight() * value),
							image.getRegionWidth(), (int) (image.getRegionHeight() * value));
					draw(sprite, current, r.x, r.y, r.width, r.height * value, state);
				} else {
					current.setRegion(image, 0, image.getRegionY(), (int) (image.getRegionWidth() * value),
							image.getRegionHeight());
					draw(sprite, current, r.x, r.y, r.width * value, r.height, state);
				}
			}
		} else if (source != null) {
			Rectangle r = this.getDestination(time, state);
			if (r != null) {
				float value = id != -1 ? state.getSliderValue(id) : 0;
				if(id != -1 && isRefNum && max != min) {
					if(min < max) {
						if(state.getNumberValue(id) > max) value = 1;
						else if(state.getNumberValue(id) < min) value = 0;
						else value = Math.abs( ((float) state.getNumberValue(id) - min) / (max - min) );
					} else {
						if(state.getNumberValue(id) < max) value = 1;
						else if(state.getNumberValue(id) > min) value = 0;
						else value = Math.abs( ((float) state.getNumberValue(id) - min) / (max - min) );
					}
				}
				TextureRegion image = source.getImage(time, state);
				if (direction == 1) {
					current.setRegion(image, 0, image.getRegionHeight() - (int) (image.getRegionHeight() * value),
							image.getRegionWidth(), (int) (image.getRegionHeight() * value));
					draw(sprite, current, r.x, r.y, r.width, r.height * value, state);
				} else {
					current.setRegion(image, 0, 0, (int) (image.getRegionWidth() * value), image.getRegionHeight());
					draw(sprite, current, r.x, r.y, r.width * value, r.height, state);
				}
			}
		}
	}

	public void dispose() {
		if (source != null) {
			source.dispose();
			source = null;
		}
	}

	public void setReferenceID(int id) {
		this.id = id;
	}

	public int getReferenceID() {
		return id;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public boolean isRefNum() {
		return isRefNum;
	}

	public void setRefNum(boolean isRefNum) {
		this.isRefNum = isRefNum;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}
}
