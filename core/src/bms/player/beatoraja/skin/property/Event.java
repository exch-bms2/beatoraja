package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;

/**
 * Events can be specified for reaction to buttons, and can be defined by users in a skin.
 */
public interface Event {

	default void exec(MainState state) {
		exec(state, 0, 0);
	}

	default void exec(MainState state, int arg1) {
		exec(state, arg1, 0);
	}

	void exec(MainState state, int arg1, int arg2);

	/**
	 * イベントIDに依存した処理用
	 * @return タイマーID (スクリプトによるタイマー定義の場合は {@code Integer.MIN_VALUE})
	 */
	default int getEventId() {
		return Integer.MIN_VALUE;
	}

}
