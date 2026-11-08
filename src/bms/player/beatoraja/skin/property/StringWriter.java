package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;

/**
 * stringを反映させるためのインターフェイス
 */
@FunctionalInterface
public interface StringWriter {

	public void set(MainState state, String value);

	/** Captures the destination of one edit, or returns null when the value cannot be edited. */
	default StringWriter bind(MainState state) {
		return this;
	}
}
