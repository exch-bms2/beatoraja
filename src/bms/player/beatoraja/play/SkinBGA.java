package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * BGAオブジェクト
 *
 * @author exch
 */
public class SkinBGA extends SkinObject {

    private PlaySkin skin;

    public SkinBGA(PlaySkin skin) {
        this.skin = skin;
    }

    @Override
    public void draw(SpriteBatch sprite, long time, MainState state) {
        if (skin.player.getMainController().getPlayerResource().getBGAManager() != null) {
            BMSPlayer player = (BMSPlayer) state;
            skin.player
                    .getMainController()
                    .getPlayerResource()
                    .getBGAManager()
                    .drawBGA(
                            sprite,
                            getDestination(time, state),
                            player.getState() == BMSPlayer.STATE_PRELOAD
                                    || player.getState() == BMSPlayer.STATE_READY ? -1
                                    : (int) (player.getNowTime() - player.getTimer()[TIMER_PLAY]));
        }
    }

    @Override
    public void dispose() {

    }
}
