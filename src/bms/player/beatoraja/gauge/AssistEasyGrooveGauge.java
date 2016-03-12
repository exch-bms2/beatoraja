package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.PlaySkin;

public class AssistEasyGrooveGauge extends GrooveGauge {

	private float[] gauge;
	
	public AssistEasyGrooveGauge(BMSModel model) {
		super(2,100, 20, 60, CLEARTYPE_LIGHT_ASSTST);
		float g = (float) (model.getTotal() / model.getTotalNotes());
		gauge = new float[] { g, g, g / 2, -1.6f, -4.8f, -1.6f };
	}
	
	@Override
	public void update(int judge) {
		this.setValue(this.getValue() + gauge[judge]);
	}

	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
		sprite.begin();
		for(int i = 2; i <= 100 && i <= getValue(); i+=2) {
			if (i < 60) {
				sprite.draw(skin.getGauge()[2], x + w * (i - 2)/ 100,
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
