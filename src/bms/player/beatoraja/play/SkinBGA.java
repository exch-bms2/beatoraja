package bms.player.beatoraja.play;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainState;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;
import bms.player.beatoraja.skin.SkinObject;
import bms.player.beatoraja.skin.StretchType;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * BGAオブジェクト
 * 
 * @author exch
 */
public class SkinBGA extends SkinObject {
	
	private BMSPlayer player;
	private long time;
	private final SkinPractice fallbackPractice = new SkinPractice(0);

	public SkinBGA(int bgaExpand) {
		setStretch(switch (bgaExpand) {
			case Config.BGAEXPAND_FULL -> StretchType.STRETCH;
			case Config.BGAEXPAND_KEEP_ASPECT_RATIO -> StretchType.KEEP_ASPECT_RATIO_FIT_INNER;
			case Config.BGAEXPAND_OFF -> StretchType.KEEP_ASPECT_RATIO_NO_EXPANDING;
			default -> StretchType.STRETCH;
		});
	}
	
	@Override
	public void prepare(long time, MainState state) {
		if(player == null) {
			player = (BMSPlayer)state;
		}
		this.time = time;
		super.prepare(time, state);
		if(draw) {
			final int s = player.getState();
			player.resource.getBGAManager().prepareBGA(
					s == BMSPlayer.STATE_PRELOAD || s == BMSPlayer.STATE_PRACTICE || s == BMSPlayer.STATE_READY ? -1
							: player.timer.getNowTime(TIMER_PLAY));
		}
	}

	public void draw(SkinObjectRenderer sprite) {
		final PlayerResource resource = player.resource;
		if (resource.getPlayMode().mode == BMSPlayerMode.Mode.PRACTICE) {
			if (!((PlaySkin) player.getSkin()).hasPractice()) {
				fallbackPractice.prepareFallback(time, player, region);
				fallbackPractice.draw(sprite);
			}
		} else if (resource.getBGAManager() != null) {
			resource.getBGAManager().drawBGA(this,sprite,region);
		}		
	}

	@Override
	public void dispose() {
		fallbackPractice.dispose();
	}
}
