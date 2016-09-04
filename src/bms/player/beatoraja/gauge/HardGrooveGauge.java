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

	private boolean noadjust = false;
	
	public HardGrooveGauge(BMSModel model) {
		if(model.getUseKeys() == 9) {
			noadjust = true;
			// TODO ポップンのHARDの仕様(閉店なし)に合わせるべきか
			init(0, 100, 100, 0, CLEARTYPE_HARD, new float[] { 0.15f, 0.10f, 0.05f, -5.0f, -10.0f, -10.0f });
//			init(2,100, 25, 66.7f, CLEARTYPE_HARD,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
//					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
//					-1.6f, -5.0f, -5.0f })	;			
		} else {
			init(0, 100, 100, 0, CLEARTYPE_HARD, new float[] { 0.15f, 0.10f, 0.05f, -5.0f, -10.0f, -5.0f });
		}

	}

	@Override
	public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
        sprite.begin();
        for (int i = 1; i <= 50; i++) {
            final float border = i * getMaxValue() / 50;
			sprite.draw(skin.getGauge()[(getValue() >= border ? 0 : 2) + (border < getBorder() ? 1 : 0)], x + w * (i - 1) / 50,
					y, w / 50, h);			
        }
        sprite.end();
	}

	@Override
	protected float getGaugeValue(int judge) {
		float value = super.getGaugeValue(judge);
//		if(noadjust) {
//			return value;
//		}
		if(this.getValue() > 50 || judge < 3) {
			return value;
		} else if(this.getValue() > 40) {
			return value * 0.8f;
		} else if(this.getValue() > 30) {
			return value * 0.7f;
		} else if(this.getValue() > 20) {
			return value * 0.6f;
		} else if(this.getValue() > 10) {
			return value * 0.5f;
		} else {
			return value * 0.4f;
		}
	}
}
