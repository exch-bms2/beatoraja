package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayerResource.PlayMode;
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
		if (eventId == BUTTON_REPLAY) {
			return createZeroArgEvent(getReplayEventConsumer(0), eventId);
		}
		if (eventId == BUTTON_REPLAY2) {
			return createZeroArgEvent(getReplayEventConsumer(1), eventId);
		}
		if (eventId == BUTTON_REPLAY3) {
			return createZeroArgEvent(getReplayEventConsumer(2), eventId);
		}
		if (eventId == BUTTON_REPLAY4) {
			return createZeroArgEvent(getReplayEventConsumer(3), eventId);
		}
		
		if (eventId == BUTTON_OPEN_IR_WEBSITE) {
			return createZeroArgEvent((state) -> {
				if(state instanceof MusicSelector) {
					MusicSelectCommand.OPEN_RANKING_ON_IR.execute((MusicSelector) state);
				}
				if(state instanceof MusicResult) {
					MusicResultCommand.OPEN_RANKING_ON_IR.execute((MusicResult) state, true);
				}
				if(state instanceof CourseResult) {
					CourseResultCommand.OPEN_RANKING_ON_IR.execute((CourseResult) state);
				}
			}, eventId);			
		}		
		if (eventId == BUTTON_FAVORITTE_CHART) {
			return createOneArgEvent((state, arg1) -> {
				if(state instanceof MusicSelector) {
					(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_CHART : MusicSelectCommand.PREV_FAVORITE_CHART)	.execute((MusicSelector) state);			}
				if(state instanceof MusicResult) {
					MusicResultCommand.CHANGE_FAVORITE_CHART.execute((MusicResult) state, arg1 >= 0);
				}
			}, eventId);
		}
		if (eventId == BUTTON_FAVORITTE_SONG) {
			return createOneArgEvent((state, arg1) -> {
				if(state instanceof MusicSelector) {
					(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_SONG : MusicSelectCommand.PREV_FAVORITE_SONG).execute((MusicSelector) state);			}
				if(state instanceof MusicResult) {
					MusicResultCommand.CHANGE_FAVORITE_SONG.execute((MusicResult) state, arg1 >= 0);
				}
			}, eventId);
		}

		// 0～2引数に対応できるように2引数取っておく
		return createTwoArgEvent((state, arg1, arg2) -> state.executeEvent(eventId, arg1, arg2), eventId);
	}
	
	private static Consumer<MainState> getReplayEventConsumer(int index) {
		return (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).selectSong(PlayMode.getReplayMode(index));;
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
