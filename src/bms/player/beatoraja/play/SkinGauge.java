package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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

    private Texture backtex;

    public SkinGauge(PlaySkin skin, TextureRegion[][] image) {
        this.skin = skin;
        this.image = image;
        Pixmap back = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        back.setColor(0, 0, 0, 0.7f);
        back.fill();
        backtex = new Texture(back);
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
            sprite.end();
            skin.player.getGauge().draw(sprite, getImage(state, time), gr.x, gr.y, gr.width, gr.height);
            sprite.begin();
        }
    }

    @Override
    public void dispose() {

    }
}
