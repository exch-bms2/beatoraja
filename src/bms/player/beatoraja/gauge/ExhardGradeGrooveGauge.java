package bms.player.beatoraja.gauge;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * EXHARD段位ゲージ
 *
 * @author exch
 */
public class ExhardGradeGrooveGauge extends GrooveGauge {

	public ExhardGradeGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			// TODO ポップンの段位ゲージ仕様は？
			init(0, 100, 100, 0, CLEARTYPE_EXHARD,new float[] { 0.15f, 0.05f, 0, -5.0f, -10.0f, -10.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_EXHARD,new float[] { 0.15f, 0.05f, 0, -5.0f, -10.0f, -5.0f });
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
