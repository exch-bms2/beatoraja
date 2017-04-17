package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * アシストイージーゲージ
 *
 * @author exch
 */
public class AssistEasyGrooveGauge extends GrooveGauge {

    public AssistEasyGrooveGauge(BMSModel model) {
        if (model.getMode() == Mode.POPN_5K || model.getMode() == Mode.POPN_9K) {
            init(2, 120, 30, 65, CLEARTYPE_LIGHT_ASSTST, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.0f, -3.0f, -1.0f});
        } else {
            init(2, 100, 20, 60, CLEARTYPE_LIGHT_ASSTST, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.5f, -3.0f, 0});
        }

    }
}
