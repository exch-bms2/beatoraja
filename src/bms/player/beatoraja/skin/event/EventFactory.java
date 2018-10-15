package bms.player.beatoraja.skin.event;

import bms.player.beatoraja.MainState;

import java.util.function.*;

public class EventFactory {

	/**
	 * ID指定によるイベント取得(組み込みまたはカスタムイベントとして登録したもの)
	 * @param eventId イベントID
	 * @return イベントオブジェクト
	 */
	public static Event getEvent(int eventId) {
		// 0～2引数に対応できるように2引数取っておく
		return createTwoArgEvent((state, arg1, arg2) -> state.executeEvent(eventId, arg1, arg2), eventId);
	}

	@FunctionalInterface
	public interface TriConsumer<T, U, V> {
		void accept(T t, U u, V v);
	}

	public static Event createZeroArgEvent(Consumer<MainState> action) {
		return createZeroArgEvent(action, Integer.MIN_VALUE);
	}

	public static Event createOneArgEvent(BiConsumer<MainState, Integer> action) {
		return createOneArgEvent(action, Integer.MIN_VALUE);
	}

	public static Event createTwoArgEvent(TriConsumer<MainState, Integer, Integer> action) {
		return createTwoArgEvent(action, Integer.MIN_VALUE);
	}

	public static Event createZeroArgEvent(Consumer<MainState> action, int eventId) {
		return new Event() {
			@Override
			public void exec(MainState state, int arg1, int arg2) {
				action.accept(state);
			}

			@Override
			public int getEventId() {
				return eventId;
			}
		};
	}

	public static Event createOneArgEvent(BiConsumer<MainState, Integer> action, int eventId) {
		return new Event() {
			@Override
			public void exec(MainState state, int arg1, int arg2) {
				action.accept(state, arg1);
			}

			@Override
			public int getEventId() {
				return eventId;
			}
		};
	}

	public static Event createTwoArgEvent(TriConsumer<MainState, Integer, Integer> action, int eventId) {
		return new Event() {
			@Override
			public void exec(MainState state, int arg1, int arg2) {
				action.accept(state, arg1, arg2);
			}

			@Override
			public int getEventId() {
				return eventId;
			}
		};
	}
}
