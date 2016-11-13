package bms.player.beatoraja.play.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * イージーゲージ
 *
 * @author exch
 */
public class EasyGrooveGauge extends GrooveGauge {

    public EasyGrooveGauge(BMSModel model) {
        if (model.getUseKeys() == 9) {
            init(2, 120, 30, 85, CLEARTYPE_EASY, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.0f, -3.0f, -3.0f});
        } else {
            init(2, 100, 20, 80, CLEARTYPE_EASY, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.5f, -4.5f, -0.5f});
        }
    }
}
