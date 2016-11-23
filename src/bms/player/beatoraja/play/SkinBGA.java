package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.SkinObject;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * BGAオブジェクト
 * 
 * @author exch
 */
public class SkinBGA extends SkinObject {

	public SkinBGA(PlaySkin skin) {
	}

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		final BMSPlayer player = (BMSPlayer) state;
		if (player.getMainController().getPlayerResource().getBGAManager() != null) {
			player.getMainController()
					.getPlayerResource()
					.getBGAManager()
					.drawBGA(
							sprite,
							getDestination(time, state),
							player.getState() == BMSPlayer.STATE_PRELOAD
									|| player.getState() == BMSPlayer.STATE_PRACTICE
									|| player.getState() == BMSPlayer.STATE_READY ? -1
									: (int) (player.getNowTime() - player.getTimer()[TIMER_PLAY]));
		}
		if (player.getState() == BMSPlayer.STATE_PRACTICE) {
			Rectangle r = getDestination(time, state);
			if(r != null) {
				player.getPracticeConfiguration().draw(r, sprite, time, state);				
			}
		}
	}

	@Override
	public void dispose() {

	}
}
