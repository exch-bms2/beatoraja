package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRCourseData;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.BarSorter;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Color;

import static bms.player.beatoraja.select.MusicSelector.SOUND_OPTIONCHANGE;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

		/**
		 * 次のMODEフィルター(5KEY, 7KEY, ...)へ移動
		 */
		mode(11, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				int mode = 0;
				PlayerConfig config = selector.main.getPlayerConfig();
				for(;mode < MusicSelector.MODE.length && MusicSelector.MODE[mode] != config.getMode();mode++);
				config.setMode(MusicSelector.MODE[(mode + (arg1 >= 0 ? 1 : MusicSelector.MODE.length - 1)) % MusicSelector.MODE.length]);
				selector.getBarRender().updateBar();
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		/**
		 * 選曲バーソート(曲名,  クリアランプ, ...)変更
		 */
		sort(12, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				selector.setSort((selector.getSort() + (arg1 >= 0 ? 1 : BarSorter.values().length - 1)) % BarSorter.values().length);
				selector.getBarRender().updateBar();
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		/**
		 * 楽曲ファイルのドキュメントをOS既定のドキュメントビューアーで開く
		 */
		open_document(17, (state) -> {
			if (!Desktop.isDesktopSupported()) {
				return;
			}
			if(state instanceof MusicSelector) {
				Bar current = ((MusicSelector)state).getBarRender().getSelected();
				if(current instanceof SongBar && ((SongBar) current).existsSong()) {
					try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(((SongBar) current).getSongData().getPath()).getParent())) {
						paths.forEach(p -> {
							if(!Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".txt")) {
								try {
									Desktop.getDesktop().open(p.toFile());
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						});
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}),

		replay1(19, getReplayEventConsumer(0)),
		replay2(316, getReplayEventConsumer(1)),
		replay3(317, getReplayEventConsumer(2)),
		replay4(318, getReplayEventConsumer(3)),
		/**
		 * 楽曲ファイルのIRサイトをOS既定のブラウザーで開く
		 */
		open_ir(210, (state) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector)state;
				IRConnection ir = selector.main.getIRStatus().length > 0 ? selector.main.getIRStatus()[0].connection : null;
				if(ir == null) {
					return;
				}

				Bar current = selector.getBarRender().getSelected();
				String url = null;
				if(current instanceof SongBar) {
					url = ir.getSongURL(new IRChartData(((SongBar) current).getSongData()));
				}
				if(current instanceof GradeBar) {
					url = ir.getCourseURL(new IRCourseData(((GradeBar) current).getCourseData()));
				}
				if (url != null) {
					try {
						URI uri = new URI(url);
						Desktop.getDesktop().browse(uri);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
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
				final MusicSelector selector = (MusicSelector) state;
				final boolean next = arg1 >= 0;
				if (selector.getSelectedBar() instanceof SongBar) {
					final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

					if (sd != null) {
						int type = next ? 2 : 0;
						String message = next ? "Added to Invisible Chart" : "Removed from Favorite Chart";
						if ((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
							type = next ? 1 : 2;
							message = next ? "Added to Favorite Chart" : "Added to Invisible Chart";
						} else if ((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
							type = next ? 0 : 1;
							message = next ? "Removed from Invisible Chart" : "Added to Favorite Chart";
						}

						int favorite = sd.getFavorite();
						switch (type) {
							case 0:
								favorite &= 0xffffffff ^ (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART);
								break;
							case 1:
								favorite |= SongData.FAVORITE_CHART;
								favorite &= 0xffffffff ^ SongData.INVISIBLE_CHART;
								break;
							case 2:
								favorite |= SongData.INVISIBLE_CHART;
								favorite &= 0xffffffff ^ SongData.FAVORITE_CHART;
								break;
						}
						sd.setFavorite(favorite);
						selector.getSongDatabase().setSongDatas(new SongData[]{sd});
						selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
						selector.getBarRender().updateBar();
						selector.play(SOUND_OPTIONCHANGE);
					}
				}
			}
			if(state instanceof MusicResult) {
				MusicResultCommand.CHANGE_FAVORITE_CHART.execute((MusicResult) state, arg1 >= 0);
			}
		}),
		favorite_song(89, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				final boolean next = arg1 >= 0;
				if(selector.getSelectedBar() instanceof SongBar) {
					final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

					if(sd != null) {
						int type = next ? 2 : 0;
						String message = next ? "Added to Invisible Song" : "Removed from Favorite Song";
						if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
							type = next ? 1 : 2;
							message = next ? "Added to Favorite Song" : "Added to Invisible Song";
						} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
							type = next ? 0 : 1;
							message =next ?  "Removed from Invisible Song" : "Added to Favorite Song";
						}

						SongData[] songs = selector.getSongDatabase().getSongDatas("folder", sd.getFolder());
						for(SongData song : songs) {
							int favorite = song.getFavorite();
							switch (type) {
								case 0:
									favorite &= 0xffffffff ^ (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG);
									break;
								case 1:
									favorite |= SongData.FAVORITE_SONG;
									favorite &= 0xffffffff ^ SongData.INVISIBLE_SONG;
									break;
								case 2:
									favorite |= SongData.INVISIBLE_SONG;
									favorite &= 0xffffffff ^ SongData.FAVORITE_SONG;
									break;
							}
							song.setFavorite(favorite);
						}
						selector.getSongDatabase().setSongDatas(songs);
						selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
						selector.getBarRender().updateBar();
						selector.play(SOUND_OPTIONCHANGE);
					}
				}
			}
			if(state instanceof MusicResult) {
				MusicResultCommand.CHANGE_FAVORITE_SONG.execute((MusicResult) state, arg1 >= 0);
			}
		}),
		;

		/**
		 * property ID
		 */
		public final int id;
		/**
		 * StringProperty
		 */
		public final Event event;

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
