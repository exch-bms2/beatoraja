package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;

public interface TimerProperty {
	long getMicro(MainState state);

	default long get(MainState state) {
		return getMicro(state) / 1000;
	}

	default long getNowTime(MainState state) {
		long time = getMicro(state);
		return time == Long.MIN_VALUE ? 0 : state.timer.getNowTime() - time / 1000;
	}

	default boolean isOn(MainState state) {
		return getMicro(state) != Long.MIN_VALUE;
	}

	default boolean isOff(MainState state) {
		return getMicro(state) == Long.MIN_VALUE;
	}

	/**
	 * タイマーIDに依存した処理のためのバックドア
	 * @return タイマーID (スクリプトによるタイマー定義の場合は {@code Integer.MIN_VALUE})
	 */
	default int getTimerId() {
		return Integer.MIN_VALUE;
	}
}
