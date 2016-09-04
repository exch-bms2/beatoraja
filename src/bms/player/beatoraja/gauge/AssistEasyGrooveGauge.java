package bms.player.beatoraja.gauge;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import bms.model.BMSModel;
import bms.player.beatoraja.play.PlaySkin;

/**
 * アシストイージーゲージ
 *
 * @author exch
 */
public class AssistEasyGrooveGauge extends GrooveGauge {

    private boolean ispms = false;

    public AssistEasyGrooveGauge(BMSModel model) {
        if (model.getUseKeys() == 9) {
            init(2, 120, 30, 65, CLEARTYPE_LIGHT_ASSTST, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.0f, -3.0f, -1.0f});
            ispms = true;
        } else {
            init(2, 100, 20, 60, CLEARTYPE_LIGHT_ASSTST, new float[]{(float) (model.getTotal() / model.getTotalNotes()),
                    (float) (model.getTotal() / model.getTotalNotes()), (float) (model.getTotal() / model.getTotalNotes()) / 2,
                    -1.5f, -3.0f, 0});
        }

    }

    @Override
    public void draw(PlaySkin skin, SpriteBatch sprite, float x, float y, float w, float h) {
        sprite.begin();
        final int count = ispms ? 24 : 50;
        for (int i = 1; i <= count; i++) {
            final float border = i * getMaxValue() / count;
			sprite.draw(skin.getGauge()[4 + (getValue() >= border ? 0 : 2) + (border < getBorder() ? 1 : 0)], x + w * (i - 1) / count,
					y, w / count, h);			
        }
        sprite.end();
    }
}
