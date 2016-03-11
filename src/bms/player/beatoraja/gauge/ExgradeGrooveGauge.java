package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.PlaySkin;

public class ExgradeGrooveGauge extends GrooveGauge {

	private float[] gauge;

	public ExgradeGrooveGauge(BMSModel model) {
		super(0, 100, 100, 0, CLEARTYPE_HARD);
		gauge = new float[] { 0.15f, 0.15f, 0.03f, -3.0f, -6.0f, -3.0f };
	}

	@Override
	public void update(int judge) {
		this.setValue(this.getValue() + gauge[judge]);
	}

	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
		sprite.begin();
		for (int i = 2; i <= 100 && i <= getValue(); i += 2) {
			sprite.draw(skin.getGauge()[3], x + w * (i - 2) / 100, y, w / 50, h);
		}
		sprite.end();
	}
}
