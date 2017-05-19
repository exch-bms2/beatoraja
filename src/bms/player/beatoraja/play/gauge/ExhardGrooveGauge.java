package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * EXハードゲージ
 *
 * @author exch
 */
public class ExhardGrooveGauge extends GrooveGauge {

	public ExhardGrooveGauge(BMSModel model) {
		final float pg = (float) Math.max(Math.min(0.15f, (2.5 * model.getTotal() - 250) / model.getTotalNotes()), 0);
//		System.out.println("exhard回復量:" + pg);
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			// TODO ポップンのHARDの仕様(閉店なし)に合わせるべきか
			init(0, 100, 100, 0, CLEARTYPE_EXHARD, new float[] { pg, pg / 3, 0, -10.0f, -15.0f, -15.0f });
//			init(2,100, 25, 66.7f, CLEARTYPE_HARD,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
//					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
//					-1.6f, -5.0f, -5.0f })	;			
		} else {
			init(0, 100, 100, 0, CLEARTYPE_EXHARD, new float[] { pg, pg / 3, 0, -10.0f, -15.0f, -10.0f });
		}

	}
}
