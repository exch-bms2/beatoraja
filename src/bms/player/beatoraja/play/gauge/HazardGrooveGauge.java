package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * ハザードゲージ
 *
 * @author exch
 */
public class HazardGrooveGauge extends GrooveGauge {

	public HazardGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			init(0, 100, 100, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -100.0f, -100.0f, -100.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_FULLCOMBO, new float[] { 0.15f, 0.15f, 0, -100.0f, -100.0f, -2.5f });
		}

	}
}
