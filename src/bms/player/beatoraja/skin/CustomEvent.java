package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.Event;
import bms.player.beatoraja.skin.property.BooleanProperty;

public class CustomEvent {
	private final int id;
	private final Event action;
	private final BooleanProperty condition;
	private final int minInterval;
	private long lastExecuteTime = Long.MIN_VALUE;

	public CustomEvent(int id, Event action, BooleanProperty condition, int minInterval) {
		this.id = id;
		this.action = action;
		this.condition = condition;
		this.minInterval = minInterval;
	}

	public int getId() {
		return id;
	}

	public void execute(MainState state, int arg1, int arg2) {
		action.exec(state, arg1, arg2);
		lastExecuteTime = state.timer.getNowMicroTime();
	}

	public void update(MainState state) {
		if (condition == null)
			return;

		if (condition.get(state) && (lastExecuteTime == Long.MIN_VALUE || (state.timer.getNowMicroTime() - lastExecuteTime) / 1000 >= minInterval)) {
			action.exec(state);
			lastExecuteTime = state.timer.getNowMicroTime();
		}
	}
}
