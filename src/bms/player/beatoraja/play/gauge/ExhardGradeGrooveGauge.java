package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;

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
}
