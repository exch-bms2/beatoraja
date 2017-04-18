package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * EX段位ゲージ
 *
 * @author exch
 */
public class ExgradeGrooveGauge extends GrooveGauge {

	public ExgradeGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			// TODO ポップンの段位ゲージ仕様は？
			init(0, 100, 100, 0, CLEARTYPE_HARD,new float[] { 0.15f, 0.10f, 0.00f, -3.0f, -6.0f, -3.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_HARD,new float[] { 0.15f, 0.10f, 0.00f, -3.0f, -6.0f, -3.0f });
		}

	}
}
