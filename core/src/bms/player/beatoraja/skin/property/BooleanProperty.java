package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;

public interface BooleanProperty {
	
	public boolean isStatic(MainState state);
	
	public boolean get(MainState state);
}