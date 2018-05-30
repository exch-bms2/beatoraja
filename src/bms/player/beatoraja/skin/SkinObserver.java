package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

public interface SkinObserver {
	public abstract void draw(SkinObjectRenderer sprite, long time, MainState state);
}
