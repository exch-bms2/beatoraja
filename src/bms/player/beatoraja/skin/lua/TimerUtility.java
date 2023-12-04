package bms.player.beatoraja.skin.lua;

import bms.player.beatoraja.MainState;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

/**
 * Lua用のタイマー関連の便利関数集
 */
public class TimerUtility {

	private final MainState state;

	public TimerUtility(MainState state) {
		this.state = state;
	}

	public void export(LuaTable table) {
		table.set("now_timer", new now_timer());
		table.set("is_timer_on", new is_timer_on());
		table.set("is_timer_off", new is_timer_off());
		table.set("timer_function", this.new timer_function());
		table.set("timer_observe_boolean", this.new timer_observe_boolean());
		table.set("new_passive_timer", this.new new_passive_timer());
	}

	/**
	 * タイマーの経過時間を取得する
	 * arg luaValue: タイマーの値 (タイマーIDではない)
	 * return: ONになってからの経過時間 (micro sec) | 0 (OFFのとき)
	 */
	private class now_timer extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			long time = luaValue.tolong();
			return LuaNumber.valueOf(time != Long.MIN_VALUE ? state.timer.getNowMicroTime() - time : 0);
		}
	}

	/**
	 * タイマーの値がONかどうか
	 * arg luaValue: タイマーの値 (タイマーIDではない)
	 */
	private static class is_timer_on extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			return LuaBoolean.valueOf(luaValue.tolong() != Long.MIN_VALUE);
		}
	}

	/**
	 * タイマーの値がOFFかどうか
	 * arg luaValue: タイマーの値 (タイマーIDではない)
	 */
	private static class is_timer_off extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue luaValue) {
			return LuaBoolean.valueOf(luaValue.tolong() == Long.MIN_VALUE);
		}
	}

	/**
	 * ID指定のタイマーを観測するタイマー関数を作成する
	 * arg timerId: TIMER_* or custom timer ID
	 * return: function (() -> number)
	 * equivalent to:
	 *   function()
	 *     return main_state.timer(timerId)
	 *   end
	 */
	private class timer_function extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue timerId) {
			final int id = timerId.toint();
			return new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue luaValue) {
					return LuaNumber.valueOf(state.timer.getMicroTimer(id));
				}
			};
		}
	}

	/**
	 * 与えられた関数の実行結果がtrueになった瞬間にON、falseになった瞬間にOFFになるタイマー関数を作成する
	 * arg func: 観測対象の関数 (() -> boolean)
	 * return: タイマー関数 (() -> number)
	 */
	private class timer_observe_boolean extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue func) {
			LuaFunction f = func.checkfunction();
			State observerState = new State();
			observerState.timerValue = Long.MIN_VALUE;
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					boolean on = f.call().toboolean();
					if (on && observerState.timerValue == Long.MIN_VALUE) {
						observerState.timerValue = state.timer.getNowMicroTime();
					} else if (!on && observerState.timerValue != Long.MIN_VALUE) {
						observerState.timerValue = Long.MIN_VALUE;
					}
					return LuaNumber.valueOf(observerState.timerValue);
				}
			};
		}
		private class State {
			long timerValue;
		}
	}

	/**
	 * 受動的なタイマーを生成し、タイマー関数およびタイマーをON/OFFにするイベント関数を返す
	 * return: {
	 *     timer: タイマー関数 (() -> number)
	 *     turn_on: ONにする(既にONの場合はリセットしない)関数
	 *     turn_on_reset: ONにする(既にONの場合も時刻をリセットする)関数
	 *     turn_off: OFFにする関数
	 * }
	 * NOTE: 受動的なカスタムタイマーでも同様のことが可能
	 */
	private class new_passive_timer extends ZeroArgFunction {
		@Override
		public LuaValue call() {
			State s = new State();
			s.timerValue = Long.MIN_VALUE;
			LuaTable table = new LuaTable();
			table.set("timer", new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					return LuaNumber.valueOf(s.timerValue);
				}
			});
			table.set("turn_on", new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					if (s.timerValue == Long.MIN_VALUE) {
						s.timerValue = state.timer.getNowMicroTime();
					}
					return TRUE;
				}
			});
			table.set("turn_on_reset", new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					s.timerValue = state.timer.getNowMicroTime();
					return TRUE;
				}
			});
			table.set("turn_off", new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					s.timerValue = Long.MIN_VALUE;
					return TRUE;
				}
			});
			return table;
		}
		private class State {
			long timerValue;
		}
	}
}
