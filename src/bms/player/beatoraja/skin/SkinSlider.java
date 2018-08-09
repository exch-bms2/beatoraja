package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.FloatProperty;
import bms.player.beatoraja.skin.property.FloatPropertyFactory;

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
	 * slider値参照先
	 */	
	private final FloatProperty ref;
	private final FloatWriter writer;
	/**
	 * ユーザーによる値変更を受け付けるかどうか
	 */
	private boolean changable;

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, int type) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		ref = FloatPropertyFactory.getFloatProperty(type);
		writer = FloatPropertyFactory.getFloatWriter(type);
	}

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, FloatProperty ref) {
		this(image, timer, cycle, angle, range, ref, null);
	}

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, FloatProperty ref, FloatWriter writer) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		this.ref = ref;
		this.writer = writer;
	}

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, int type, int min, int max) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		ref = new RateProperty(type, min, max);
		writer = null;
	}

	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		if(source == null) {
			return;
		}
		Rectangle r = this.getDestination(time,state);
		if (r != null) {
			TextureRegion image = source.getImage(time, state);
			float value = ref != null ? ref.get(state) : 0;
			draw(sprite, image, r.x
					+ (direction == 1 ? value * range : (direction == 3 ? -value * range : 0)), r.y
					+ (direction == 0 ? value * range : (direction == 2 ? -value * range : 0)),
					r.width, r.height, state);
		}
	}

	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (isChangable()) {
			Rectangle r = getDestination(state.main.getNowTime(), state);
			if (r != null) {
				switch (getSliderAngle()) {
				case 0:
					if (r.x <= x && r.x + r.width >= x && r.y <= y && r.y + range >= y) {
						if(writer != null) {
							writer.set(state, (y - r.y) / range);
						}
						return true;
					}
					break;
				case 1:
					if (r.x <= x && r.x + range >= x && r.y <= y && r.y + r.height >= y) {
						if(writer != null) {
							writer.set(state, (x - r.x) / range);
						}
						return true;
					}
					break;
				case 2:
					if (r.x <= x && r.x + r.width >= x && r.y - range <= y && r.y >= y) {
						if(writer != null) {
							writer.set(state, (r.y - y) / range);
						}
						return true;
					}
					break;
				case 3:
					if (r.x <= x && r.x + range >= x && r.y <= y && r.y + r.height >= y) {
						if(writer != null) {
							writer.set(state, (r.x + range - x) / range);
						}
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
	
	public int getSliderAngle() {
		return direction;
	}
}
