package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;

public interface BooleanProperty {
	
	public boolean isStatic(MainState state);
	
	public boolean get(MainState state);

	/**
	 * Whether skipping {@link #get} would change nothing but the returned value.
	 * A skin object may then skip its draw conditions while its timer is definitely off.
	 * Built-in properties are side-effect free; script properties override this.
	 */
	default boolean hasNoSideEffects() {
		return true;
	}
}