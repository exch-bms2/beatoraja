package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainState;

public class TimerPropertyFactory {
	public static TimerProperty getTimerProperty(int timerId) {
		if (timerId < 0)
			return null;

		return new TimerProperty() {
			@Override
			public long getMicro(MainState state) {
				return state.main.getMicroTimer(timerId);
			}

			@Override
			public long get(MainState state) {
				return state.main.getTimer(timerId);
			}

			@Override
			public long getNowTime(MainState state) {
				return state.main.getNowTime(timerId);
			}

			@Override
			public boolean isOn(MainState state) {
				return state.main.isTimerOn(timerId);
			}

			@Override
			public boolean isOff(MainState state) {
				return !state.main.isTimerOn(timerId);
			}

			@Override
			public int getTimerId() {
				return timerId;
			}
		};
	}
}
