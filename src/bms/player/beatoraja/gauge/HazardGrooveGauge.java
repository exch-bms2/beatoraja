package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * ハザードゲージ
 *
 * @author exch
 */
public class HazardGrooveGauge extends GrooveGauge {

	public HazardGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			init(0, 100, 100, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -100.0f, -100.0f, -100.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -100.0f, -100.0f, -2.5f });
		}

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
