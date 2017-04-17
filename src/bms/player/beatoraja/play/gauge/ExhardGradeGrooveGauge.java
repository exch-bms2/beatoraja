package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * EXHARD段位ゲージ
 *
 * @author exch
 */
public class ExhardGradeGrooveGauge extends GrooveGauge {

	public ExhardGradeGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			// TODO ポップンの段位ゲージ仕様は？
			init(0, 100, 100, 0, CLEARTYPE_EXHARD,new float[] { 0.15f, 0.05f, 0, -5.0f, -10.0f, -10.0f });
		} else {
			init(0, 100, 100, 0, CLEARTYPE_EXHARD,new float[] { 0.15f, 0.05f, 0, -5.0f, -10.0f, -5.0f });
		}

	}
}
