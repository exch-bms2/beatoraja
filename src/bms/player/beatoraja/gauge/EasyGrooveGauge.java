package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * イージーゲージ
 *
 * @author exch
 */
public class EasyGrooveGauge extends GrooveGauge {

    private boolean ispms = false;

    public EasyGrooveGauge(BMSModel model) {
        if (model.getUseKeys() == 9) {
            init(2, 100, 25, 66.66f, CLEARTYPE_EASY, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -0.8f, -2.5f, -2.5f});
            ispms = true;
        } else {
            init(2, 100, 20, 80, CLEARTYPE_EASY, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.5f, -4.5f, -0.5f});
        }

    }

    @Override
    public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
        sprite.begin();
        final int count = ispms ? 24 : 50;
        for (int i = 1; i <= count; i++) {
            final float border = i * 100f / count;
            if (getValue() >= border) {
                if (border < getBorder()) {
                    sprite.draw(skin.getGauge()[2], x + w * (i - 1) / count,
                            y, w / count, h);
                } else {
                    sprite.draw(skin.getGauge()[1], x + w * (i - 1) / count,
                            y, w / count, h);
                }
            }
        }
        sprite.end();
    }
}
