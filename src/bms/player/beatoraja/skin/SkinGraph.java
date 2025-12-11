package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.property.FloatProperty;
import bms.player.beatoraja.skin.property.FloatPropertyFactory;

import bms.player.beatoraja.skin.property.TimerProperty;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンオブジェクト:グラフ
 * 
 * @author exch
 */
public final class SkinGraph extends SkinObject {

	/**
	 * イメージ
	 */
	private final SkinSource source;
	/**
	 * グラフ値参照先
	 */	
	private final FloatProperty ref;	
	/**
	 * グラフの伸びる向き(1:下, それ以外:右)
	 */
	public final int direction;

	private final TextureRegion current = new TextureRegion();
	
	private TextureRegion currentImage;
	private float currentValue;

	public SkinGraph(int imageid, int id, int direction) {
		source = new SkinSourceReference(imageid);
		ref = FloatPropertyFactory.getRateProperty(id);
		this.direction = direction;
	}

	public SkinGraph(int imageid, int id, int min, int max, int direction) {
		source = new SkinSourceReference(imageid);
		ref = new RateProperty(id, min, max);
		this.direction = direction;
	}

	public SkinGraph(TextureRegion[] image, int timer, int cycle, int id, int direction) {
		source = new SkinSourceImage(image, timer, cycle);
		ref = FloatPropertyFactory.getRateProperty(id);
		this.direction = direction;
	}

	public SkinGraph(TextureRegion[] image, int timer, int cycle, int id, int min, int max, int direction) {
		source = new SkinSourceImage(image, timer, cycle);
		ref = new RateProperty(id, min, max);
		this.direction = direction;
	}

	public SkinGraph(TextureRegion[] image, TimerProperty timer, int cycle, int id, int direction) {
		source = new SkinSourceImage(image, timer, cycle);
		ref = FloatPropertyFactory.getRateProperty(id);
		this.direction = direction;
	}

	public SkinGraph(TextureRegion[] image, TimerProperty timer, int cycle, FloatProperty ref, int direction) {
		source = new SkinSourceImage(image, timer, cycle);
		this.ref = ref;
		this.direction = direction;
	}

	public SkinGraph(TextureRegion[] image, TimerProperty timer, int cycle, int id, int min, int max, int direction) {
		source = new SkinSourceImage(image, timer, cycle);
		ref = new RateProperty(id, min, max);
		this.direction = direction;
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
		if (direction == 1) {
			current.setRegion(currentImage, 0, currentImage.getRegionHeight() - (int) (currentImage.getRegionHeight() * currentValue),
					currentImage.getRegionWidth(), (int) (currentImage.getRegionHeight() * currentValue));
			draw(sprite, current, region.x, region.y, region.width, region.height * currentValue);
		} else {
			current.setRegion(currentImage, 0, 0, (int) (currentImage.getRegionWidth() * currentValue), currentImage.getRegionHeight());
			draw(sprite, current, region.x, region.y, region.width * currentValue, region.height);
		}							
	}

	public void dispose() {
		disposeAll(source);
		setDisposed();
	}
}
