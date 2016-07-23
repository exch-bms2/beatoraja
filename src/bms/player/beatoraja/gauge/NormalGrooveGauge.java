package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * ノーマルゲージ
 *
 * @author exch
 */
public class NormalGrooveGauge extends GrooveGauge {

	private boolean ispms = false;

	public NormalGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			init(2,120, 30, 85, CLEARTYPE_NORMAL,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-2.0f, -6.0f, -6.0f })	;
			ispms = true;
		} else {
			init(2,100, 20, 80, CLEARTYPE_NORMAL,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-3.0f, -6.0f, -2.0f })	;		
		}
	}
	
	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
		sprite.begin();
		final int count = ispms ? 24 : 50;
		for(int i = 1; i <= count; i++) {
			final float border = i * getMaxValue() / count;
			if(getValue() >= border) {
				if (border < getBorder()) {
					sprite.draw(skin.getGauge()[0], x + w * (i - 1) / count,
							y, w / count, h);
				} else {
					sprite.draw(skin.getGauge()[1], x + w * (i - 1) / count,
							y, w / count, h);
				}
			}
		}
		sprite.end();
	}
}
