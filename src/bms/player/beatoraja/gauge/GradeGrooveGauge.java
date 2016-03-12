package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.PlaySkin;

public class GradeGrooveGauge extends GrooveGauge {

	private float[] gauge;

	public GradeGrooveGauge(BMSModel model) {
		super(0, 100, 100, 0, CLEARTYPE_NORMAL);
		gauge = new float[] { 0.15f, 0.15f, 0.03f, -1.5f, -3f, -1.5f };
	}

	@Override
	public void update(int judge) {
		if(this.getValue() > 30 || judge < 3) {
			this.setValue(this.getValue() + gauge[judge]);			
		} else if(this.getValue() > 15) {
			this.setValue(this.getValue() + gauge[judge] * 0.75f);			
		} else if(this.getValue() > 5) {
			this.setValue(this.getValue() + gauge[judge] * 0.5f);			
		} else {
			this.setValue(this.getValue() + gauge[judge] * 0.25f);			
		}
	}

	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
		sprite.begin();
		for (int i = 2; i <= 100 && i <= getValue(); i += 2) {
			sprite.draw(skin.getGauge()[1], x + w * (i - 2) / 100, y, w / 50, h);
		}
		sprite.end();
	}
	
	@Override
	public float getGaugeValue(int judge) {
		if(this.getValue() > 30 || judge < 3) {
			return gauge[judge];			
		} else {
			return gauge[judge] * 0.5f;			
		}
	}
}
