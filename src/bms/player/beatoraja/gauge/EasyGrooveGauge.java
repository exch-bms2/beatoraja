package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

public class EasyGrooveGauge extends GrooveGauge {

	private float[] gauge;
	
	public EasyGrooveGauge(BMSModel model) {
		super(2,100, 20, 80, CLEARTYPE_EASY);
		float g = (float) (model.getTotal() / model.getTotalNotes());
		gauge = new float[] { g, g, g / 2, -1.5f, -3.0f, -1.0f };
	}
	
	@Override
	public void update(int judge) {
		this.setValue(this.getValue() + gauge[judge]);
	}

	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
		sprite.begin();
		for(int i = 2; i <= 100 && i <= getValue(); i+=2) {
			if (i < 80) {
				sprite.draw(skin.getGauge()[2], x + w * (i - 2) / 100,
						y, w / 50, h);
			} else {
				sprite.draw(skin.getGauge()[1], x + w * (i - 2) / 100,
						y, w / 50, h);
			}
		}
		sprite.end();
	}
	
	@Override
	public float getGaugeValue(int judge) {
		return gauge[judge];
	}
}
