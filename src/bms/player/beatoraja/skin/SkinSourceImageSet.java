package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.TimerProperty;
import bms.player.beatoraja.skin.property.TimerPropertyFactory;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * スキンのソースイメージセット
 *
 * @author exch
 */
public class SkinSourceImageSet implements SkinSourceSet {

    /**
     * イメージ
     */
    private TextureRegion[][] image;

    private final TimerProperty timer;

    private final int cycle;

    public SkinSourceImageSet(TextureRegion[][] image, int timer, int cycle) {
        this(image, timer > 0 ? TimerPropertyFactory.getTimerProperty(timer) : null, cycle);
    }

    public SkinSourceImageSet(TextureRegion[][] image, TimerProperty timer, int cycle) {
        this.image = image;
        this.timer = timer;
        this.cycle = cycle;
    }

    public boolean validate() {
        if(image == null || image.length == 0) {
            return false;
        }

        boolean exist = false;
        for(TextureRegion[] trs : image) {
            if(trs != null) {
                for(TextureRegion tr : trs) {
                    if(tr != null) {
                        exist = true;
                    }
                }
            }
        }

        if(!exist) {
            return false;
        }
        return true;
    }

    public TextureRegion[] getImages(long time, MainState state) {
        if (image != null && image.length > 0) {
            return image[getImageIndex(image.length, time, state)];
        }
        return null;
    }

    public TextureRegion[][] getImages() {
        return image;
    }

    private int getImageIndex(int length, long time, MainState state) {
        if (cycle == 0) {
            return 0;
        }

        if (timer != null) {
            if (timer.isOff(state)) {
                return 0;
            }
            time -= timer.get(state);
        }
        if (time < 0) {
            return 0;
        }
        // System.out.println(index + " / " + image.length);
        return (int) ((time * length / cycle) % length);
    }

    public void dispose() {
        if (image != null) {
            for (TextureRegion[] trs : image) {
                if (trs != null) {
                    for (TextureRegion tr : trs) {
                        tr.getTexture().dispose();
                    }
                }
            }
            image = null;
        }
    }

}
