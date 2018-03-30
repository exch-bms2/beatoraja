package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * グラフイメージ
 */
public class SkinGraph extends SkinObject {

	/**
	 * イメージ
	 */
	private SkinSource source;
	/**
	 * グラフの参照値
	 */
	private int id = -1;
	
	private FloatProperty ref;	
	/**
	 * グラフの伸びる向き(1:下, それ以外:右)
	 */
	private int direction = 1;
	/**
	 * NUMBER値参照かどうか
	 */
	private boolean isRefNum = false;
	/**
	 * NUMBER値参照の場合の最小値
	 */
	private int min = 0;
	/**
	 * NUMBER値参照の場合の最大値
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
				float value = ref != null ? ref.get(state) : (id != -1 ? state.getSliderValue(id) : 0);
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
				float value = ref != null ? ref.get(state) : (id != -1 ? state.getSliderValue(id) : 0);
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
		ref = SkinPropertyMapper.getFloatProperty(id);
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
