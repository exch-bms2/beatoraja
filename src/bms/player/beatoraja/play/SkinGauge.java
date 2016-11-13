package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.play.gauge.*;
import bms.player.beatoraja.skin.SkinObject;
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
    private TextureRegion[][] image;

    private PlaySkin skin;

    public SkinGauge(PlaySkin skin, TextureRegion[][] image) {
        this.skin = skin;
        this.image = image;
        Pixmap back = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        back.setColor(0, 0, 0, 0.7f);
        back.fill();
    }

    public TextureRegion[] getImage(MainState state, long time) {
        return image[getImageIndex(image.length, time, state)];
    }

    public void setImage(TextureRegion[][] image, int cycle) {
        this.image = image;
        setCycle(cycle);
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        if (skin.player.getGauge() != null) {
            Rectangle gr = skin.getGaugeRegion();
            
            final GrooveGauge gauge = skin.player.getGauge();
            final TextureRegion[] images = getImage(state, time);
            
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
