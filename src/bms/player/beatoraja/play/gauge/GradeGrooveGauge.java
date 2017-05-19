package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * 段位ゲージ
 *
 * @author exch
 */
public class GradeGrooveGauge extends GrooveGauge {

	public GradeGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			init(0, 100, 100, 0, CLEARTYPE_NORMAL, new float[] { 0.15f, 0.10f, 0.05f, -1.5f, -3f, -1.5f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_NORMAL, new float[] { 0.15f, 0.10f, 0.05f, -1.5f, -3f, -1.5f });
		}

	}

	@Override
	protected float getGaugeValue(int judge) {
		float value = super.getGaugeValue(judge);
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
