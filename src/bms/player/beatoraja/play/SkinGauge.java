package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.gauge.*;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.SkinSource;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * ゲージオブジェクト
 *
 * @author exch
 */
public class SkinGauge extends SkinObject {

    /**
     * イメージ
     */
    private SkinSource image;

    private PlaySkin skin;

    public SkinGauge(PlaySkin skin, TextureRegion[][] image, int timer, int cycle) {
        this.skin = skin;
        this.image = new SkinSource(image, timer, cycle);
        Pixmap back = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        back.setColor(0, 0, 0, 0.7f);
        back.fill();
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        if (skin.player.getGauge() != null) {
            Rectangle gr = skin.getGaugeRegion();
            
            final GrooveGauge gauge = skin.player.getGauge();
            final TextureRegion[] images = image.getImages(time, state);
            
            int count = gauge.getMaxValue() > 100 ? 24 : 50;
            int exgauge = 0;
            if(gauge instanceof AssistEasyGrooveGauge || gauge instanceof EasyGrooveGauge || gauge instanceof ExhardGrooveGauge
            		|| gauge instanceof ExgradeGrooveGauge || gauge instanceof ExhardGradeGrooveGauge || gauge instanceof HazardGrooveGauge) {
                exgauge = 4;            	
            }
            for (int i = 1; i <= count; i++) {
                final float border = i * gauge.getMaxValue() / count;
    			sprite.draw(images[exgauge + (gauge.getValue() >= border ? 0 : 2) + (border < gauge.getBorder() ? 1 : 0)], gr.x + gr.width * (i - 1) / count,
    					gr.y, gr.width / count, gr.height);			
            }
        }
    }

    @Override
    public void dispose() {
    }
}
