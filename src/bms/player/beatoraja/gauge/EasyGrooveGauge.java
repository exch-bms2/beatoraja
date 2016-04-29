package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * イージーゲージ
 *
 * @author exch
 */
public class EasyGrooveGauge extends GrooveGauge {
	
	public EasyGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			init(2,100, 25, 66.7f, CLEARTYPE_EASY,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-0.8f, -2.5f, -2.5f })	;			
		} else {
			init(2,100, 20, 80, CLEARTYPE_EASY, new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-1.5f, -4.5f, -0.5f });
		}

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
}
