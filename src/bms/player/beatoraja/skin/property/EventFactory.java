package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRCourseData;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.BarSorter;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.Bar;
import bms.player.beatoraja.select.bar.GradeBar;
import bms.player.beatoraja.select.bar.SongBar;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import static bms.player.beatoraja.select.MusicSelector.SOUND_OPTIONCHANGE;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.*;

/**
 * EventのFactoryクラス
 * 
 * @author excln
 */
public class EventFactory {

	/**
	 * ID指定によるイベント取得(組み込みまたはカスタムイベントとして登録したもの)
	 * 
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
	 * 
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
		 * キーコンフィグへ遷移
		 */
		keyconfig(13, (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).changeState(MainStateType.CONFIG);
			}
		}),
		/**
		 * スキンコンフィグへ遷移
		 */
		skinconfig(14, (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).changeState(MainStateType.SKINCONFIG);
			}			
		}),
		play(15, (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).selectSong(BMSPlayerMode.PLAY);
			}						
		}),
		autoplay(16, (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).selectSong(BMSPlayerMode.AUTOPLAY);
			}						
		}),
		practice(315, (state) -> {
			if(state instanceof MusicSelector) {
				((MusicSelector) state).selectSong(BMSPlayerMode.PRACTICE);
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
		
	    /**
	     * ゲージオプションの変更
	     */
		gauge1p(40, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.main.getPlayerConfig();
	            config.setGauge((config.getGauge() + (arg1 >= 0 ? 1 : 5)) % 6);
	            state.play(SOUND_OPTIONCHANGE);				
			}
		}),
	    /**
	     * 1P側譜面オプションの変更
	     */
		option1p(42, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.main.getPlayerConfig();
	            config.setRandom((config.getRandom() + (arg1 >= 0 ? 1 : 9)) % 10);
	            state.play(SOUND_OPTIONCHANGE);				
			}
		}),
	    /**
	     * 2P側譜面オプションの変更
	     */
		option2p(43, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.main.getPlayerConfig();
	            config.setRandom2((config.getRandom2() + (arg1 >= 0 ? 1 : 9)) % 10);
	            state.play(SOUND_OPTIONCHANGE);
			}
		}),
	    /**
	     * DP譜面オプションの変更
	     */
		optiondp(54, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.main.getPlayerConfig();
	            config.setDoubleoption((config.getDoubleoption() + (arg1 >= 0 ? 1 : 3)) % 4);
	            state.play(SOUND_OPTIONCHANGE);
			}
		}),
	    /**
	     * ハイスピード固定オプションの変更
	     */
		hsfix(55, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
	            if (pc != null) {
	                pc.setFixhispeed((pc.getFixhispeed() + (arg1 >= 0 ? 1 : 4)) % 5);
	                state.play(SOUND_OPTIONCHANGE);
	            }				
			}
		}),
	    /**
	     * hispeedの変更
	     */
		hispeed1p(57, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
	            PlayConfig pc = selector.getSelectedBarPlayConfig();	            
            	float hispeed = pc.getHispeed() + (arg1 >= 0 ? pc.getHispeedMargin() : -pc.getHispeedMargin());
            	hispeed = MathUtils.clamp(hispeed, PlayConfig.HISPEED_MIN, PlayConfig.HISPEED_MAX);
            	if(hispeed != pc.getHispeed()) {
            		pc.setHispeed(hispeed);
	                state.play(SOUND_OPTIONCHANGE);		        	
            	}
			}
		}),
	    /**
	     * durationの変更
	     */
		duration1p(59, (state, arg1, arg2) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
	            PlayConfig pc = selector.getSelectedBarPlayConfig();	            
            	final int inc = arg2 > 0 ? arg2 : 1;
            	int duration = pc.getDuration() + (arg1 >= 0 ? inc : -inc);
        		duration = MathUtils.clamp(duration, PlayConfig.DURATION_MIN, PlayConfig.DURATION_MAX);
		        if(duration != pc.getDuration()) {
		        	pc.setDuration(duration);
	                state.play(SOUND_OPTIONCHANGE);		        	
		        }	            	
			}
		}),
		hispeedautoadjust(342, (state) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setHispeedAutoAdjust(!pc.isEnableHispeedAutoAdjust());
					state.play(SOUND_OPTIONCHANGE);
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
			IRConnection ir = state.main.getIRStatus().length > 0 ? state.main.getIRStatus()[0].connection : null;
			if(ir == null) {
				return;
			}
			String url = null;
			if(state instanceof MusicSelector) {
				Bar current = ((MusicSelector)state).getBarRender().getSelected();
				if(current instanceof SongBar) {
					url = ir.getSongURL(new IRChartData(((SongBar) current).getSongData()));
				} else if(current instanceof GradeBar) {
					url = ir.getCourseURL(new IRCourseData(((GradeBar) current).getCourseData()));
				}
			} else if(state instanceof MusicResult) {
	            url = ir.getSongURL(new IRChartData(state.main.getPlayerResource().getSongdata()));
			} else if(state instanceof CourseResult) {
	            url = ir.getCourseURL(new IRCourseData(state.main.getPlayerResource().getCourseData()));
			}
			if (url != null) {
				try {
					URI uri = new URI(url);
					Desktop.getDesktop().browse(uri);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}),
		bga(72, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				state.main.getConfig().setBga((state.main.getConfig().getBga() + (arg1 >= 0 ? 1 : 2)) % 3);
				state.play(SOUND_OPTIONCHANGE);				
			}
		}),
		bgaexpand(73, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				state.main.getConfig().setBgaExpand((state.main.getConfig().getBgaExpand() + (arg1 >= 0 ? 1 : 2)) % 3);
				state.play(SOUND_OPTIONCHANGE);				
			}
		}),
		notesdisplaytiming(74, (state, arg1) -> {
	        final PlayerConfig config = state.main.getPlayerConfig();

	        int inc = arg1 >= 0 ? (config.getJudgetiming() < PlayerConfig.JUDGETIMING_MAX ? 1 : 0)
	        		: (config.getJudgetiming() > PlayerConfig.JUDGETIMING_MIN ? -1 : 0);
	        
	        if(inc != 0) {
                config.setJudgetiming(config.getJudgetiming() + inc);
    			if(state instanceof MusicSelector) {
                    state.play(SOUND_OPTIONCHANGE);		        	
    			}
	        }
		}),
		notesdisplaytimingautoadjust(75, (state) -> {
	        final PlayerConfig config = state.main.getPlayerConfig();
            config.setNotesDisplayTimingAutoAdjust(!config.isNotesDisplayTimingAutoAdjust());
			if(state instanceof MusicSelector) {
                state.play(SOUND_OPTIONCHANGE);		        	
			}
		}),
		target(77, (state, arg1) -> {
			if(state instanceof MusicSelector) {
		        final PlayerConfig config = state.main.getPlayerResource().getPlayerConfig();
	            final String[] targets = TargetProperty.getTargets();
	            int index = 0;
	            for(;index < targets.length;index++) {
	            	if(targets[index].equals(config.getTargetid())) {
	            		break;
	            	}
	            }
	            config.setTargetid(targets[(index + (arg1 >= 0 ? 1 : targets.length - 1)) % targets.length]);
			}
		}),
		gaugeautoshift(78, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				final int gaslength = 5;
	            selector.main.getPlayerConfig().setGaugeAutoShift((selector.main.getPlayerConfig().getGaugeAutoShift() + (arg1 >= 0 ?1 : gaslength - 1)) % gaslength);
	            selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		bottomshiftablegauge(341, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				final int gaugelength = 3;
				selector.main.getPlayerConfig().setBottomShiftableGauge((selector.main.getPlayerConfig().getBottomShiftableGauge() + (arg1 >= 0 ? 1 : gaugelength - 1)) % gaugelength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		/**
		 * 選択ライバル変更
		 */
		rival(79, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				PlayerInformation[] rivals = state.main.getRivalDataAccessor().getRivals();
	            int index = -1;
	            for(int i = 0;i < rivals.length;i++) {
	            	if(selector.getRival() == rivals[i]) {
	            		index = i;
	            		break;
	            	}
	            }
	            index = (index + (arg1 >= 0 ? 2 : rivals.length + 1)) % (rivals.length + 1) - 1;
	            selector.setRival(index != -1 ? rivals[index] : null);
	            selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		favorite_chart(90, (state, arg1) -> {
			final boolean next = arg1 >= 0;
			final Consumer<SongData> changeFav = (sd) -> {
				int type = 0;
				if((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
					type = 2;
				} else if((sd.getFavorite() & SongData.FAVORITE_CHART) != 0) {
					type = 1;
				}				
				type = (type + (next ? 1 : 2)) % 3;
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
				state.main.getSongDatabase().setSongDatas(new SongData[]{sd});
			};
			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				if (selector.getSelectedBar() instanceof SongBar) {
					final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

					if (sd != null) {
						String message = next ? "Added to Invisible Chart" : "Removed from Favorite Chart";
						if ((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
							message = next ? "Added to Favorite Chart" : "Added to Invisible Chart";
						} else if ((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
							message = next ? "Removed from Invisible Chart" : "Added to Favorite Chart";
						}
						
						changeFav.accept(sd);

						selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
						selector.getBarRender().updateBar();
						selector.play(SOUND_OPTIONCHANGE);
					}
				}
			}
			if(state instanceof MusicResult) {
				final SongData sd = state.main.getPlayerResource().getSongdata();
				if(sd != null) {
					changeFav.accept(sd);					
				}
			}
		}),
		favorite_song(89, (state, arg1) -> {
			final boolean next = arg1 >= 0;
			final Consumer<SongData> changeFav = (sd) -> {
				int type = 0;
				if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
					type = 2;
				} else if((sd.getFavorite() & SongData.FAVORITE_SONG) != 0) {
					type = 1;
				}				
				type = (type + (next ? 1 : 2)) % 3;
				SongData[] songs = state.main.getSongDatabase().getSongDatas("folder", sd.getFolder());
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
				state.main.getSongDatabase().setSongDatas(songs);
			};

			if(state instanceof MusicSelector) {
				final MusicSelector selector = (MusicSelector) state;
				if(selector.getSelectedBar() instanceof SongBar) {
					final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();
					if(sd != null) {
						String message = next ? "Added to Invisible Song" : "Removed from Favorite Song";
						if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
							message = next ? "Added to Favorite Song" : "Added to Invisible Song";
						} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
							message =next ?  "Removed from Invisible Song" : "Added to Favorite Song";
						}
						changeFav.accept(sd);					
						selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
						selector.getBarRender().updateBar();
						selector.play(SOUND_OPTIONCHANGE);
					}
				}
			}
			if(state instanceof MusicResult) {
				final SongData sd = state.main.getPlayerResource().getSongdata();
				if(sd != null) {
					changeFav.accept(sd);					
				}
			}
		}),
	    /**
	     * LNモードの変更
	     */
		lnmode(308, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int lnmodelength = 3;
				final MusicSelector selector = (MusicSelector) state;
	            PlayerConfig config = selector.main.getPlayerConfig();
	            config.setLnmode((config.getLnmode() + (arg1 >= 0 ? 1 : lnmodelength - 1)) % lnmodelength);
	            selector.getBarRender().updateBar();
	            selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		autosavereplay1(321, changeAutoSaveReplay(0)),
		autosavereplay2(322, changeAutoSaveReplay(1)),
		autosavereplay3(323, changeAutoSaveReplay(2)),
		autosavereplay4(324, changeAutoSaveReplay(3)),
		lanecover(330, (state) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablelanecover(!pc.isEnablelanecover());
					state.play(SOUND_OPTIONCHANGE);
				}
			}
		}),
		lift(331, (state) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablelift(!pc.isEnablelift());
					state.play(SOUND_OPTIONCHANGE);
				}
			}
		}),
		hidden(332, (state) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablehidden(!pc.isEnablehidden());
					state.play(SOUND_OPTIONCHANGE);
				}
			}
		}),
		judgealgorithm(340, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc == null) {
					return;
				}
				final String[] algorithms = {JudgeAlgorithm.Combo.name(), JudgeAlgorithm.Duration.name(), JudgeAlgorithm.Lowest.name()};
				final String jt = pc.getJudgetype();
				for (int i = 0; i < algorithms.length; i++) {
					if (jt.equals(algorithms[i])) {
						pc.setJudgetype(algorithms[(arg1 >= 0 ? i + 1 : i + algorithms.length - 1) % algorithms.length]);
						state.play(SOUND_OPTIONCHANGE);
					}
				}
			}
		}),
		extranotedepth(350, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int depthlength = 4;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setExtranoteDepth((config.getExtranoteDepth() + (arg1 >= 0 ? 1 : depthlength - 1)) % depthlength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		minemode(351, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 5;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setMineMode((config.getMineMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		scrollmode(352, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 3;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setScrollMode((config.getScrollMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		longnotemode(353, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 6;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setLongnoteMode((config.getLongnoteMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		seventonine_pattern(360, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int patternlength = 7;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setSevenToNinePattern((config.getSevenToNinePattern() + (arg1 >= 0 ? 1 : patternlength - 1)) % patternlength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		seventonine_type(361, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int typelength = 3;
				final MusicSelector selector = (MusicSelector) state;
				PlayerConfig config = selector.main.getPlayerConfig();
				config.setSevenToNineType((config.getSevenToNineType() + (arg1 >= 0 ? 1 : typelength - 1)) % typelength);
				selector.play(SOUND_OPTIONCHANGE);
			}
		}),
		;

		/**
		 * property ID
		 */
		public final int id;
		/**
		 * event
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
		
		private EventType(int id, TriConsumer<MainState, Integer, Integer> action) {
			this.id = id;
			this.event = createTwoArgEvent(action, id);
		}
		
	    private static BiConsumer<MainState, Integer> changeAutoSaveReplay(int index) {
	    	return (state, arg1) -> {
	    		if(state instanceof MusicSelector) {
					final MusicSelector selector = (MusicSelector) state;
	    	        int[] asr = selector.main.getPlayerConfig().getAutoSaveReplay();
	    	        final int length = AbstractResult.ReplayAutoSaveConstraint.values().length;
	    	        asr[index] = (asr[index] + (arg1 >= 0 ? 1 : length - 1)) % length;
	    	        selector.main.getPlayerConfig().setAutoSaveReplay(asr);
	    	        selector.play(SOUND_OPTIONCHANGE);
	    		}
	    	};
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
