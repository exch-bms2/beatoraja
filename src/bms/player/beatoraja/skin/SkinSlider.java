package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class SkinSlider extends SkinObject {

	/**
	 * イメージ
	 */
	private SkinSource source;

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

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, int type) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		this.type = type;
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if(source == null) {
			return;
		}
		Rectangle r = this.getDestination(time,state);
		if (r != null) {
			TextureRegion image = source.getImage(time, state);
			float value = type != -1 ? state.getSliderValue(type) : 0;
			if(type != -1 && isRefNum && max != min) {
				if(min < max) {
					if(state.getNumberValue(type) > max) value = 1;
					else if(state.getNumberValue(type) < min) value = 0;
					else value = Math.abs( ((float) state.getNumberValue(type) - min) / (max - min) );
				} else {
					if(state.getNumberValue(type) < max) value = 1;
					else if(state.getNumberValue(type) > min) value = 0;
					else value = Math.abs( ((float) state.getNumberValue(type) - min) / (max - min) );
				}
			}
			draw(sprite, image, r.x
					+ (direction == 1 ? value * range : (direction == 3 ? -value * range : 0)), r.y
					+ (direction == 0 ? value * range : (direction == 2 ? -value * range : 0)),
					r.width, r.height);
		}
	}

	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (isChangable()) {
			Rectangle r = getDestination(state.main.getNowTime(), state);
			if (r != null) {
				switch (getSliderAngle()) {
				case 0:
					if (r.x <= x && r.x + r.width >= x && r.y <= y && r.y + range >= y) {
						state.setSliderValue(type, (y - r.y) / range);
						return true;
					}
					break;
				case 1:
					if (r.x <= x && r.x + range >= x && r.y <= y && r.y + r.height >= y) {
						state.setSliderValue(type, (x - r.x) / range);
						return true;
					}
					break;
				case 2:
					if (r.x <= x && r.x + r.width >= x && r.y - range <= y && r.y >= y) {
						state.setSliderValue(type, (r.y - y) / range);
						return true;
					}
					break;
				case 3:
					if (r.x <= x && r.x + range >= x && r.y <= y && r.y + r.height >= y) {
						state.setSliderValue(type, (r.x + range - x) / range);
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	public void dispose() {
		if (source != null) {
			source.dispose();
			source = null;
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
