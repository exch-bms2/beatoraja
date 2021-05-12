package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.MusicSelectCommand;
import bms.player.beatoraja.select.MusicSelector;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.function.*;

public class EventFactory {

	/**
	 * ID指定によるイベント取得(組み込みまたはカスタムイベントとして登録したもの)
	 * @param eventId イベントID
	 * @return イベントオブジェクト
	 */
	public static Event getEvent(int eventId) {
		for(EventType t : EventType.values()) {
			if(t.id == eventId) {
				return t.event;
			}
		}

		// 0～2引数に対応できるように2引数取っておく
		return createTwoArgEvent((state, arg1, arg2) -> state.executeEvent(eventId, arg1, arg2), eventId);
	}

	/**
	 * name指定によるイベント取得(組み込みまたはカスタムイベントとして登録したもの)
	 * @param eventName イベント名称
	 * @return イベントオブジェクト
	 */
	public static Event getEvent(String eventName) {
		for(EventType t : EventType.values()) {
			if(t.name().equals(eventName)) {
				return t.event;
			}
		}

		return null;
	}

	public enum EventType {

		replay1(19, getReplayEventConsumer(0)),
		replay2(316, getReplayEventConsumer(1)),
		replay3(317, getReplayEventConsumer(2)),
		replay4(318, getReplayEventConsumer(3)),

		open_ir(210, (state) -> {
			if(state instanceof MusicSelector) {
				MusicSelectCommand.OPEN_RANKING_ON_IR.execute((MusicSelector) state);
			}
			if(state instanceof MusicResult) {
				MusicResultCommand.OPEN_RANKING_ON_IR.execute((MusicResult) state, true);
			}
			if(state instanceof CourseResult) {
				CourseResultCommand.OPEN_RANKING_ON_IR.execute((CourseResult) state);
			}
		}),
		favorite_chart(90, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_CHART : MusicSelectCommand.PREV_FAVORITE_CHART)	.execute((MusicSelector) state);			}
			if(state instanceof MusicResult) {
				MusicResultCommand.CHANGE_FAVORITE_CHART.execute((MusicResult) state, arg1 >= 0);
			}
		}),
		favorite_song(89, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_SONG : MusicSelectCommand.PREV_FAVORITE_SONG).execute((MusicSelector) state);			}
			if(state instanceof MusicResult) {
				MusicResultCommand.CHANGE_FAVORITE_SONG.execute((MusicResult) state, arg1 >= 0);
			}
		}),
		;

		/**
		 * property ID
		 */
		private final int id;
		/**
		 * StringProperty
		 */
		private final Event event;

		private EventType(int id, Consumer<MainState> action) {
			this.id = id;
			this.event = createZeroArgEvent(action, id);
		}

		private EventType(int id, BiConsumer<MainState, Integer> action) {
			this.id = id;
			this.event = createOneArgEvent(action, id);
		}

	}
	
	private static Consumer<MainState> getReplayEventConsumer(int index) {
		return (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).selectSong(BMSPlayerMode.getReplayMode(index));;
			}
			if(state instanceof MusicResult) {
				((MusicResult) state).saveReplayData(index);
			}
			if(state instanceof CourseResult) {
				((CourseResult) state).saveReplayData(index);
			}
		};
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
