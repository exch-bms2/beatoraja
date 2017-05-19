package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * ノーマルゲージ
 *
 * @author exch
 */
public class NormalGrooveGauge extends GrooveGauge {

	public NormalGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
			init(2,120, 30, 85, CLEARTYPE_NORMAL,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-2.0f, -6.0f, -6.0f })	;
		} else {
			init(2,100, 20, 80, CLEARTYPE_NORMAL,new float[] { (float) (model.getTotal() / model.getTotalNotes()),
					(float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
					-3.0f, -6.0f, -2.0f })	;		
		}
	}	
}
