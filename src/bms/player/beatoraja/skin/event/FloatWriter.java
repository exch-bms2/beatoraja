package bms.player.beatoraja.skin.event;

import bms.player.beatoraja.MainState;

/**
 * floatを反映させるためのインターフェイス
 *
 * @author exch
 */
public interface FloatWriter {

	public void set(MainState state, float value);

}
