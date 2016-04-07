package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * ハードゲージ
 *
 * @author exch
 */
public class HardGrooveGauge extends GrooveGauge {

	public HardGrooveGauge(BMSModel model) {
		super(0, 100, 100, 0, CLEARTYPE_HARD, new float[] { 0.15f, 0.15f, 0, -5.0f, -10.0f, -5.0f });
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
	protected float getGaugeValue(int judge) {
		float value = super.getGaugeValue(judge);
		if(this.getValue() > 30 || judge < 3) {
			return value;
		} else if(this.getValue() > 15) {
			return value * 0.75f;
		} else if(this.getValue() > 7.5) {
			return value * 0.5f;
		} else {
			return value * 0.25f;
		}
	}
}
