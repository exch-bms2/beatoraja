package bms.player.beatoraja.play.gauge;

import bms.model.BMSModel;

/**
 * アシストイージーゲージ
 *
 * @author exch
 */
public class AssistEasyGrooveGauge extends GrooveGauge {

    public AssistEasyGrooveGauge(BMSModel model) {
        if (model.getUseKeys() == 9) {
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
