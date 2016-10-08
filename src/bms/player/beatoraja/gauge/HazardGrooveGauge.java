package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * ハザードゲージ
 *
 * @author exch
 */
public class HazardGrooveGauge extends GrooveGauge {

	public HazardGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			init(0, 120, 120, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -120.0f, -120.0f, -120.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -100.0f, -100.0f, -2.5f });
		}

	}

	@Override
	public void draw(SpriteBatch sprite, TextureRegion[] images, float x, float y, float w, float h) {
        sprite.begin();
        for (int i = 1; i <= 50; i++) {
            final float border = i * getMaxValue() / 50;
			sprite.draw(images[4 + (getValue() >= border ? 0 : 2) + (border < getBorder() ? 1 : 0)], x + w * (i - 1) / 50,
					y, w / 50, h);			
        }
        sprite.end();
	}
}
