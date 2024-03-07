package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.FloatWriter;
import bms.player.beatoraja.skin.property.FloatProperty;
import bms.player.beatoraja.skin.property.FloatPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンオブジェクト:スライダー
 * 
 * @author exch
 */
public final class SkinSlider extends SkinObject {

	/**
	 * イメージ
	 */
	private final SkinSource source;

	/**
	 * slider移動方向(0:上, 1:右, 2:下, 3:左)
	 */
	private final int direction;
	/**
	 * slider移動範囲
	 */
	private final int range;
	/**
	 * slider値参照元
	 */	
	private final FloatProperty ref;
	/**
	 * slider値反映先
	 */	
	private final FloatWriter writer;

	private TextureRegion currentImage;
	private float currentValue;

	public SkinSlider(TextureRegion[] image, int timer, int cycle, int angle, int range, int type, boolean changeable) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		ref = FloatPropertyFactory.getRateProperty(type);
		writer = changeable ? FloatPropertyFactory.getRateWriter(type) : null;
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

	public SkinSlider(TextureRegion[] image, TimerProperty timer, int cycle, int angle, int range, int type, boolean changeable) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		ref = FloatPropertyFactory.getRateProperty(type);
		writer = changeable ? FloatPropertyFactory.getRateWriter(type) : null;
	}

	public SkinSlider(TextureRegion[] image, TimerProperty timer, int cycle, int angle, int range, FloatProperty ref, FloatWriter writer) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		this.ref = ref;
		this.writer = writer;
	}

	public SkinSlider(TextureRegion[] image, TimerProperty timer, int cycle, int angle, int range, int type, int min, int max) {
		source = new SkinSourceImage(image, timer ,cycle);
		this.direction = angle;
		this.range = range;
		ref = new RateProperty(type, min, max);
		writer = null;
	}

	public boolean validate() {
		if(source == null || !source.validate()) {
			return false;
		}
		return super.validate();
	}

	public void prepare(long time, MainState state) {
		super.prepare(time, state);
		if(!draw) {
			return;
		}
		if((currentImage = source.getImage(time, state)) == null) {
			draw = false;
			return;			
		}
		currentValue = ref != null ? ref.get(state) : 0;
	}

	public void draw(SkinObjectRenderer sprite) {
		draw(sprite, currentImage, region.x
				+ (direction == 1 ? currentValue * range : (direction == 3 ? -currentValue * range : 0)), region.y
				+ (direction == 0 ? currentValue * range : (direction == 2 ? -currentValue * range : 0)),
				region.width, region.height);		
	}

	protected boolean mousePressed(MainState state, int button, int x, int y) {
		if (writer != null) {
			switch (direction) {
			case 0:
				if (region.x <= x && region.x + region.width >= x && region.y <= y && region.y + range >= y) {
					float value;
					if (Math.abs(y - region.y) < 1) {
						value = 0;
					} else if (Math.abs(y - (region.y + range)) < 1) {
						value = 1;
					} else {
						value = (y - region.y) / range;
					}
					writer.set(state, value);
					return true;
				}
				break;
			case 1:
				if (region.x <= x && region.x + range >= x && region.y <= y && region.y + region.height >= y) {
					float value;
					if (Math.abs(x - region.x) < 1) {
						value = 0;
					} else if (Math.abs(x - (region.x + range)) < 1) {
						value = 1;
					} else {
						value = (x - region.x) / range;
					}
					writer.set(state, value);
					return true;
				}
				break;
			case 2:
				if (region.x <= x && region.x + region.width >= x && region.y - range <= y && region.y >= y) {
					float value;
					if (Math.abs(y - region.y) < 1) {
						value = 0;
					} else if (Math.abs(y - (region.y - range)) < 1) {
						value = 1;
					} else {
						value = (region.y - y) / range;
					}
					writer.set(state, value);
					return true;
				}
				break;
			case 3:
				if (region.x >= x && region.x - range <= x && region.y <= y && region.y + region.height >= y) {
					float value;
					if (Math.abs(x - region.x) < 1) {
						value = 0;
					} else if (Math.abs(x - (region.x - range)) < 1) {
						value = 1;
					} else {
						value = (region.x - x) / range;
					}
					writer.set(state, value);
					return true;
				}
				break;
			}
		}
		return false;
	}

	public void dispose() {
		disposeAll(source);
		setDisposed();
	}

	public int getRange() {
		return range;
	}
	
	public int getSliderAngle() {
		return direction;
	}
}
