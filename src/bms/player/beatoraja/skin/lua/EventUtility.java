package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua用のイベント関連の便利関数集
 */
public class EventUtility {
	private final MainState state;

	public EventUtility(MainState state) {
		this.state = state;
	}

	public void export(LuaTable table) {
		table.set("event_observe_turn_true", new event_observe_turn_true());
		table.set("event_observe_timer", new event_observe_timer());
		table.set("event_observe_timer_on", new event_observe_timer_on());
		table.set("event_observe_timer_off", new event_observe_timer_off());
		table.set("event_min_interval", this.new event_min_interval());
	}

	/**
	 * 与えられた関数の実行結果がtrueになった瞬間に特定の処理を実行するイベントを作成する
	 * arg func: 観測対象の関数 (() -> boolean)
	 * arg action: 実行するイベント関数
	 * return: イベント関数 (ゼロ引数関数, CustomEvent の action に設定する想定)
	 * equivalent to:
	 *   local isOn = false
	 *   return function()
	 *     local on = func()
	 *     if isOn != on then
	 *       isOn = on
	 *       if isOn then
	 *         action()
	 *       end
	 *     end
	 *   end
	 */
	private static class event_observe_turn_true extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue func, LuaValue action) {
			LuaFunction f = func.checkfunction();
			LuaFunction act = action.checkfunction();
			State state = new State();
			state.isOn = false;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					boolean on = f.call().toboolean();
					if (state.isOn != on) {
						state.isOn = on;
						if (state.isOn) {
							act.call();
						}
					}
					return TRUE;
				}
			};
		}
		private static class State {
			boolean isOn;
		}
	}

	/**
	 * タイマーが設定された瞬間に特定の処理を実行するイベントを作成する
	 * arg timerFunc: タイマー関数 (() -> number (micro sec))
	 * arg action: 実行する関数
	 * return: イベント関数 (ゼロ引数関数, CustomEvent の action に設定する想定)
	 */
	private static class event_observe_timer extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue timerFunc, LuaValue action) {
			LuaFunction timer = timerFunc.checkfunction();
			LuaFunction act = action.checkfunction();
			State state = new State();
			state.value = Long.MIN_VALUE;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					long newValue = timer.call().tolong();
					if (newValue != state.value && newValue != Long.MIN_VALUE) {
						state.value = newValue;
						act.call();
					}
					return TRUE;
				}
			};
		}
		private static class State {
			long value;
		}
	}

	/**
	 * タイマーがOFFからONになった瞬間に特定の処理を実行するイベントを作成する
	 * arg timerFunc: タイマー関数 (() -> number (micro sec))
	 * arg action: 実行する関数
	 * return: イベント関数 (ゼロ引数関数, CustomEvent の action に設定する想定)
	 * equivalent to:
	 *   event_observe_turn_true(
	 *     function()
	 *       return timer_util.is_on(timerFunc())
	 *     end,
	 *     action)
	 */
	private static class event_observe_timer_on extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue timerFunc, LuaValue action) {
			LuaFunction timer = timerFunc.checkfunction();
			LuaFunction act = action.checkfunction();
			State state = new State();
			state.isOn = false;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					boolean on = timer.call().tolong() != Long.MIN_VALUE;
					if (state.isOn != on) {
						state.isOn = on;
						if (state.isOn) {
							act.call();
						}
					}
					return TRUE;
				}
			};
		}
		private static class State {
			boolean isOn;
		}
	}

	/**
	 * タイマーがONからOFFになった瞬間に特定の処理を実行するイベントを作成する
	 * arg timerFunc: タイマー関数 (() -> number (micro sec))
	 * arg action: 実行する関数
	 * return: イベント関数 (ゼロ引数関数, CustomEvent の action に設定する想定)
	 * equivalent to:
	 *   event_observe_turn_true(
	 *     function()
	 *       return timer_util.is_off(timerFunc())
	 *     end,
	 *     action)
	 */
	private static class event_observe_timer_off extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue timerFunc, LuaValue action) {
			LuaFunction timer = timerFunc.checkfunction();
			LuaFunction act = action.checkfunction();
			State state = new State();
			state.isOff = false;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					boolean off = timer.call().tolong() == Long.MIN_VALUE;
					if (state.isOff != off) {
						state.isOff = off;
						if (state.isOff) {
							act.call();
						}
					}
					return TRUE;
				}
			};
		}
		private static class State {
			boolean isOff;
		}
	}

	/**
	 * イベントの実行間隔が最低でも与えられた値以上になるように制限されたイベントを作成する
	 * arg minInterval: 最小間隔 (milliseconds)
	 * arg action: イベント関数
	 * return: イベント関数
	 * NOTE: カスタムイベントとして登録するイベントを間引くには minInterval を設定すれば同様のことができる。
	 *   タイマーの観測は毎フレーム行うが、ONになった瞬間の処理は間引きたいという場合に、
	 *   目的のアクションにこの関数を適用してから event_observe_timer_on に渡すとよい。
	 *   event_observe_timer_on の結果に minInterval を設定するとタイマーを観測する間隔が間引かれてしまう。
	 */
	private class event_min_interval extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue minInterval, LuaValue action) {
			int interval = minInterval.toint();
			LuaFunction act = action.checkfunction();
			State s = new State();
			s.lastExecution = Long.MIN_VALUE;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					if (s.lastExecution == Long.MIN_VALUE
							|| (state.timer.getNowMicroTime() - s.lastExecution) / 1000 >= interval) {
						s.lastExecution = state.timer.getNowMicroTime();
						act.call();
					}
					return TRUE;
				}
			};
		}
		private class State {
			long lastExecution;
		}
	}
}
