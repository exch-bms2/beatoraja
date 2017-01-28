package bms.player.beatoraja.play;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
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

	public SkinBGA() {
	}

	@Override
	public void draw(SpriteBatch sprite, long time, MainState state) {
		final PlayerResource resource = state.getMainController().getPlayerResource();
		if (resource.getAutoplay() == 2) {
			Rectangle r = getDestination(time, state);
			if (r != null) {
				((BMSPlayer) state).getPracticeConfiguration().draw(r, sprite, time, state);
			}
		} else if (resource.getBGAManager() != null) {
			final int s = ((BMSPlayer) state).getState();
			resource.getBGAManager().drawBGA(
					sprite,
					getDestination(time, state),
					s == BMSPlayer.STATE_PRELOAD || s == BMSPlayer.STATE_PRACTICE || s == BMSPlayer.STATE_READY ? -1
							: (int) (((BMSPlayer) state).getNowTime() - ((BMSPlayer) state).getTimer()[TIMER_PLAY]));
		}
	}

	@Override
	public void dispose() {

	}
}
