package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;
import com.badlogic.gdx.graphics.g2d.TextureRegion;


/**
 * EX段位ゲージ
 *
 * @author exch
 */
public class ExgradeGrooveGauge extends GrooveGauge {

	public ExgradeGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			// TODO ポップンの段位ゲージ仕様は？
			init(0, 100, 100, 0, CLEARTYPE_HARD,new float[] { 0.15f, 0.10f, 0.00f, -3.0f, -6.0f, -3.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_HARD,new float[] { 0.15f, 0.10f, 0.00f, -3.0f, -6.0f, -3.0f });
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
