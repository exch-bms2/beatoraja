package bms.player.beatoraja.play;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
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

	public SkinBGA(int bgaExpand) {
		switch (bgaExpand) {
		case Config.BGAEXPAND_FULL:
			setStretch(StretchType.STRETCH);
			break;
		case Config.BGAEXPAND_KEEP_ASPECT_RATIO:
			setStretch(StretchType.KEEP_ASPECT_RATIO_FIT_INNER);
			break;
		case Config.BGAEXPAND_OFF:
			setStretch(StretchType.KEEP_ASPECT_RATIO_NO_EXPANDING);
			break;
		}
	}

	@Override
	public void draw(SkinObjectRenderer sprite, long time, MainState state) {
		final PlayerResource resource = state.main.getPlayerResource();
		if (resource.getPlayMode() == PlayMode.PRACTICE) {
			Rectangle r = getDestination(time, state);
			if (r != null) {
				((BMSPlayer) state).getPracticeConfiguration().draw(r, sprite, time, state);
			}
		} else if (resource.getBGAManager() != null) {
			final int s = ((BMSPlayer) state).getState();
			resource.getBGAManager().drawBGA(
					this,
					sprite,
					getDestination(time, state),
					s == BMSPlayer.STATE_PRELOAD || s == BMSPlayer.STATE_PRACTICE || s == BMSPlayer.STATE_READY ? -1
							: state.main.getNowTime(TIMER_PLAY));
		}
	}

	@Override
	public void dispose() {

	}
}
