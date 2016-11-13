package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;

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
}
