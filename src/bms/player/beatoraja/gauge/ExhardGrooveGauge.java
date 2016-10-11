package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * EXハードゲージ
 *
 * @author exch
 */
public class ExhardGrooveGauge extends GrooveGauge {

	public ExhardGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			// TODO ポップンのHARDの仕様(閉店なし)に合わせるべきか
			init(0, 100, 100, 0, CLEARTYPE_EXHARD, new float[] { 0.15f, 0.05f, 0, -10.0f, -15.0f, -15.0f });
//			init(2,100, 25, 66.7f, CLEARTYPE_HARD,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
//					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
//					-1.6f, -5.0f, -5.0f })	;			
		} else {
			init(0, 100, 100, 0, CLEARTYPE_EXHARD, new float[] { 0.15f, 0.05f, 0, -10.0f, -15.0f, -10.0f });
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
