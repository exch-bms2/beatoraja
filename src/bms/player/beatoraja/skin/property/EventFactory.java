package bms.player.beatoraja.skin.property;

import bms.player.beatoraja.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.play.JudgeAlgorithm;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.select.BarSorter;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.SkinProperty;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import static bms.player.beatoraja.SystemSoundManager.SoundType.OPTION_CHANGE;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
			if(state instanceof MusicSelector selector) {
				int mode = 0;
				PlayerConfig config = selector.resource.getPlayerConfig();
				for(;mode < MusicSelector.MODE.length && MusicSelector.MODE[mode] != config.getMode();mode++);
				config.setMode(MusicSelector.MODE[(mode + (arg1 >= 0 ? 1 : MusicSelector.MODE.length - 1)) % MusicSelector.MODE.length]);
				selector.getBarManager().updateBar();
				selector.play(OPTION_CHANGE);
			}
		}),
		/**
		 * 選曲バーソート(曲名,  クリアランプ, ...)変更
		 */
		sort(12, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				selector.setSort((selector.getSort() + (arg1 >= 0 ? 1 : BarSorter.defaultSorter.length - 1)) % BarSorter.defaultSorter.length);
				selector.getBarManager().updateBar();
				selector.play(OPTION_CHANGE);
			}
		}),
		/**
		 * 選曲バーソート(曲名,  クリアランプ, ...)変更
		 */
		songbar_sort(312, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				for(int index = 0;index < BarSorter.allSorter.length;index++) {
					if(BarSorter.allSorter[index].name().equals(selector.main.getPlayerConfig().getSortid())) {
						selector.main.getPlayerConfig().setSortid(BarSorter.allSorter[(index + (arg1 >= 0 ? 1 : BarSorter.allSorter.length - 1)) % BarSorter.allSorter.length].name());
						selector.getBarManager().updateBar();
						selector.play(OPTION_CHANGE);
						return;
					}
				}
			}
		}),
		/**
		 * キーコンフィグへ遷移
		 */
		keyconfig(13, (state) -> {
			if(state instanceof MusicSelector) {
				state.main.changeState(MainStateType.CONFIG);
			}
		}),
		/**
		 * スキンコンフィグへ遷移
		 */
		skinconfig(14, (state) -> {
			if(state instanceof MusicSelector) {
				state.main.changeState(MainStateType.SKINCONFIG);
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
			if(state instanceof MusicSelector selector && selector.getBarManager().getSelected() instanceof SongBar songbar && songbar.existsSong()) {
				try (Stream<Path> paths = Files.list(Paths.get(songbar.getSongData().getPath()).getParent())) {
					paths.filter(p -> !Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".txt")).forEach(p -> {
						try {
							Desktop.getDesktop().open(p.toFile());
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}),
		
	    /**
	     * ゲージオプションの変更
	     */
		gauge1p(40, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.resource.getPlayerConfig();
	            config.setGauge((config.getGauge() + (arg1 >= 0 ? 1 : 5)) % 6);
	            state.play(OPTION_CHANGE);				
			}
		}),
	    /**
	     * 1P側譜面オプションの変更
	     */
		option1p(42, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.resource.getPlayerConfig();
	            config.setRandom((config.getRandom() + (arg1 >= 0 ? 1 : 9)) % 10);
	            state.play(OPTION_CHANGE);				
			}
		}),
	    /**
	     * 2P側譜面オプションの変更
	     */
		option2p(43, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.resource.getPlayerConfig();
	            config.setRandom2((config.getRandom2() + (arg1 >= 0 ? 1 : 9)) % 10);
	            state.play(OPTION_CHANGE);
			}
		}),
	    /**
	     * DP譜面オプションの変更
	     */
		optiondp(54, (state, arg1) -> {
			if(state instanceof MusicSelector) {
	            PlayerConfig config = state.resource.getPlayerConfig();
	            config.setDoubleoption((config.getDoubleoption() + (arg1 >= 0 ? 1 : 3)) % 4);
	            state.play(OPTION_CHANGE);
			}
		}),
	    /**
	     * ハイスピード固定オプションの変更
	     */
		hsfix(55, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
	            PlayConfig pc = selector.getSelectedBarPlayConfig();
	            if (pc != null) {
	                pc.setFixhispeed((pc.getFixhispeed() + (arg1 >= 0 ? 1 : 4)) % 5);
	                state.play(OPTION_CHANGE);
	            }				
			}
		}),
	    /**
	     * hispeedの変更
	     */
		hispeed1p(57, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
	            PlayConfig pc = selector.getSelectedBarPlayConfig();	            
            	float hispeed = pc.getHispeed() + (arg1 >= 0 ? pc.getHispeedMargin() : -pc.getHispeedMargin());
            	hispeed = MathUtils.clamp(hispeed, PlayConfig.HISPEED_MIN, PlayConfig.HISPEED_MAX);
            	if(hispeed != pc.getHispeed()) {
            		pc.setHispeed(hispeed);
	                state.play(OPTION_CHANGE);		        	
            	}
			}
		}),
	    /**
	     * durationの変更
	     */
		duration1p(59, (state, arg1, arg2) -> {
			if(state instanceof MusicSelector selector) {
	            PlayConfig pc = selector.getSelectedBarPlayConfig();	            
            	final int inc = arg2 > 0 ? arg2 : 1;
            	int duration = pc.getDuration() + (arg1 >= 0 ? inc : -inc);
        		duration = MathUtils.clamp(duration, PlayConfig.DURATION_MIN, PlayConfig.DURATION_MAX);
		        if(duration != pc.getDuration()) {
		        	pc.setDuration(duration);
	                state.play(OPTION_CHANGE);		        	
		        }	            	
			}
		}),
		hispeedautoadjust(342, (state) -> {
			if(state instanceof MusicSelector selector) {
				PlayConfig pc = selector.getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setHispeedAutoAdjust(!pc.isEnableHispeedAutoAdjust());
					state.play(OPTION_CHANGE);
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
			if(state instanceof MusicSelector selector) {
				Bar current = selector.getBarManager().getSelected();
				if(current instanceof SongBar songbar) {
					url = ir.getSongURL(new IRChartData(songbar.getSongData()));
				} else if(current instanceof GradeBar coursebar) {
					url = ir.getCourseURL(new IRCourseData(coursebar.getCourseData()));
				}
			} else if(state instanceof MusicResult) {
	            url = ir.getSongURL(new IRChartData(state.resource.getSongdata()));
			} else if(state instanceof CourseResult) {
	            url = ir.getCourseURL(new IRCourseData(state.resource.getCourseData()));
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
		/**
		 * 楽曲フォルダ/難易度表を更新する
		 */
		update_folder(211, state -> {
			if(state instanceof MusicSelector selector) {
				Bar selected = selector.getBarManager().getSelected();
				if (selected instanceof FolderBar) {
					selector.main.updateSong(((FolderBar) selected).getFolderData().getPath());
				} else if (selected instanceof TableBar) {
					selector.main.updateTable((TableBar) selected);
				} else if (selected instanceof SongBar) {
					final String path = ((SongBar) selected).getSongData().getPath();
					if (path != null) {
						selector.main.updateSong(Paths.get(path).getParent().toString());
					}
				}				
			}
		}),
		/**
		 * 楽曲ファイルの場所をOS既定のファイルブラウザーで開く
		 */
		open_with_explorer(212, state -> {
			if(state instanceof MusicSelector selector) {
				Bar current = selector.getBarManager().getSelected();
				try {
					if (Desktop.isDesktopSupported()) {
						if (current instanceof SongBar songbar) {
							if (songbar.existsSong()) {
								Desktop.getDesktop().open(Paths.get(songbar.getSongData().getPath()).getParent().toFile());
							} else if (songbar.getSongData() != null && songbar.getSongData().getOrg_md5() != null) {
								String[] md5 = songbar.getSongData().getOrg_md5()
										.toArray(new String[songbar.getSongData().getOrg_md5().size()]);
								SongData[] songdata = selector.getSongDatabase().getSongDatas(md5);
								for (SongData sd : songdata) {
									if (sd.getPath() != null) {
										Desktop.getDesktop().open(Paths.get(sd.getPath()).getParent().toFile());
										break;
									}
								}
							} else {
								Matcher m = Pattern.compile(".[^\\(\\[～~]*").matcher(current.getTitle());
								if (m.find()) {
									SongData[] songdata = selector.getSongDatabase().getSongDatasByText(m.group());
									for (SongData sd : songdata) {
										if (sd.getPath() != null) {
											Desktop.getDesktop().open(Paths.get(sd.getPath()).getParent().toFile());
											break;
										}
									}
								}
							}
						} else if (current instanceof FolderBar) {
							Desktop.getDesktop().open(Paths.get(((FolderBar) current).getFolderData().getPath()).toFile());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}),
		/**
		 * 楽曲ファイルのDLサイトをOS既定のブラウザーで開く
		 */
		open_download_site(213, state -> {
			if(state instanceof MusicSelector selector) {
				Bar current = selector.getBarManager().getSelected();
				if (current instanceof SongBar) {
					final SongData song = ((SongBar) current).getSongData();
					if (song != null) {
						if (song.getUrl() != null && song.getUrl().length() > 0) {
							try {
								URI uri = new URI(song.getUrl());
								Desktop.getDesktop().browse(uri);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
						if (song.getAppendurl() != null && song.getAppendurl().length() > 0
								&& !song.getAppendurl().equals(song.getUrl())) {
							try {
								URI uri = new URI(song.getAppendurl());
								Desktop.getDesktop().browse(uri);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					}
				}				
			}
		}), 

		bga(72, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				state.main.getConfig().setBga((state.resource.getConfig().getBga() + (arg1 >= 0 ? 1 : 2)) % 3);
				state.play(OPTION_CHANGE);				
			}
		}),
		bgaexpand(73, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				state.main.getConfig().setBgaExpand((state.resource.getConfig().getBgaExpand() + (arg1 >= 0 ? 1 : 2)) % 3);
				state.play(OPTION_CHANGE);				
			}
		}),
		notesdisplaytiming(74, (state, arg1) -> {
	        final PlayerConfig config = state.resource.getPlayerConfig();

	        int inc = arg1 >= 0 ? (config.getJudgetiming() < PlayerConfig.JUDGETIMING_MAX ? 1 : 0)
	        		: (config.getJudgetiming() > PlayerConfig.JUDGETIMING_MIN ? -1 : 0);
	        
	        if(inc != 0) {
                config.setJudgetiming(config.getJudgetiming() + inc);
    			if(state instanceof MusicSelector) {
                    state.play(OPTION_CHANGE);		        	
    			}
	        }
		}),
		notesdisplaytimingautoadjust(75, (state) -> {
	        final PlayerConfig config = state.resource.getPlayerConfig();
            config.setNotesDisplayTimingAutoAdjust(!config.isNotesDisplayTimingAutoAdjust());
			if(state instanceof MusicSelector) {
                state.play(OPTION_CHANGE);		        	
			}
		}),
		target(77, (state, arg1) -> {
			if(state instanceof MusicSelector) {
		        final PlayerConfig config = state.resource.getPlayerConfig();
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
			if(state instanceof MusicSelector selector) {
				final int gaslength = 5;
	            selector.resource.getPlayerConfig().setGaugeAutoShift((selector.resource.getPlayerConfig().getGaugeAutoShift() + (arg1 >= 0 ?1 : gaslength - 1)) % gaslength);
	            selector.play(OPTION_CHANGE);
			}
		}),
		bottomshiftablegauge(341, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				final int gaugelength = 3;
				selector.resource.getPlayerConfig().setBottomShiftableGauge((selector.resource.getPlayerConfig().getBottomShiftableGauge() + (arg1 >= 0 ? 1 : gaugelength - 1)) % gaugelength);
				selector.play(OPTION_CHANGE);
			}
		}),
		/**
		 * 選択ライバル変更
		 */
		rival(79, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				final RivalDataAccessor rivals = state.main.getRivalDataAccessor();
	            int index = -1;
	            for(int i = 0;i < rivals.getRivalCount();i++) {
	            	if(selector.getRival() == rivals.getRivalInformation(i)) {
	            		index = i;
	            		break;
	            	}
	            }
	            index = (index + (arg1 >= 0 ? 2 : rivals.getRivalCount() + 1)) % (rivals.getRivalCount() + 1) - 1;
	            selector.setRival(index != -1 ? rivals.getRivalInformation(index) : null);
	            selector.play(OPTION_CHANGE);
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
			if(state instanceof MusicSelector selector && selector.getSelectedBar() instanceof SongBar songbar) {
				final SongData sd = songbar.getSongData();

				if (sd != null && sd.getPath() != null) {
					String message = next ? "Added to Invisible Chart" : "Removed from Favorite Chart";
					if ((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
						message = next ? "Added to Favorite Chart" : "Added to Invisible Chart";
					} else if ((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
						message = next ? "Removed from Invisible Chart" : "Added to Favorite Chart";
					}
					
					changeFav.accept(sd);

					selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
					selector.getBarManager().updateBar();
					selector.play(OPTION_CHANGE);
				}
			}
			if(state instanceof MusicResult) {
				final SongData sd = state.resource.getSongdata();
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

			if(state instanceof MusicSelector selector && selector.getSelectedBar() instanceof SongBar songbar) {
				final SongData sd = songbar.getSongData();
				if(sd != null && sd.getPath() != null) {
					String message = next ? "Added to Invisible Song" : "Removed from Favorite Song";
					if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
						message = next ? "Added to Favorite Song" : "Added to Invisible Song";
					} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
						message =next ?  "Removed from Invisible Song" : "Added to Favorite Song";
					}
					changeFav.accept(sd);					
					selector.main.getMessageRenderer().addMessage(message, 1200, Color.GREEN, 1);
					selector.getBarManager().updateBar();
					selector.play(OPTION_CHANGE);
				}
			}
			if(state instanceof MusicResult) {
				final SongData sd = state.resource.getSongdata();
				if(sd != null) {
					changeFav.accept(sd);					
				}
			}
		}),
		keyassign1(101, changeKeyAssign(0)),
		keyassign2(102, changeKeyAssign(1)),
		keyassign3(103, changeKeyAssign(2)),
		keyassign4(104, changeKeyAssign(3)),
		keyassign5(105, changeKeyAssign(4)),
		keyassign6(106, changeKeyAssign(5)),
		keyassign7(107, changeKeyAssign(6)),
		keyassign8(108, changeKeyAssign(7)),
		keyassign9(109, changeKeyAssign(8)),
		keyassign10(110, changeKeyAssign(9)),
		keyassign11(111, changeKeyAssign(10)),
		keyassign12(112, changeKeyAssign(11)),
		keyassign13(113, changeKeyAssign(12)),
		keyassign14(114, changeKeyAssign(13)),
		keyassign15(115, changeKeyAssign(14)),
		keyassign16(116, changeKeyAssign(15)),
		keyassign17(117, changeKeyAssign(16)),
		keyassign18(118, changeKeyAssign(17)),
		keyassign19(119, changeKeyAssign(18)),
		keyassign20(120, changeKeyAssign(19)),
		keyassign21(121, changeKeyAssign(20)),
		keyassign22(122, changeKeyAssign(21)),
		keyassign23(123, changeKeyAssign(22)),
		keyassign24(124, changeKeyAssign(23)),
		keyassign25(125, changeKeyAssign(24)),
		keyassign26(126, changeKeyAssign(25)),
		keyassign27(127, changeKeyAssign(26)),
		keyassign28(128, changeKeyAssign(27)),
		keyassign29(129, changeKeyAssign(28)),
		keyassign30(130, changeKeyAssign(29)),
		keyassign31(131, changeKeyAssign(30)),
		keyassign32(132, changeKeyAssign(31)),
		keyassign33(133, changeKeyAssign(32)),
		keyassign34(134, changeKeyAssign(33)),
		keyassign35(135, changeKeyAssign(34)),
		keyassign36(136, changeKeyAssign(35)),
		keyassign37(137, changeKeyAssign(36)),
		keyassign38(138, changeKeyAssign(37)),
		keyassign39(139, changeKeyAssign(38)),
		keyassign40(150, changeKeyAssign(39)),
		keyassign41(151, changeKeyAssign(40)),
		keyassign42(152, changeKeyAssign(41)),
		keyassign43(153, changeKeyAssign(42)),
		keyassign44(154, changeKeyAssign(43)),
		keyassign45(155, changeKeyAssign(44)),
		keyassign46(156, changeKeyAssign(45)),
		keyassign47(157, changeKeyAssign(46)),
		keyassign48(158, changeKeyAssign(47)),
		keyassign49(159, changeKeyAssign(48)),
		keyassign50(160, changeKeyAssign(49)),
		keyassign51(161, changeKeyAssign(50)),
		keyassign52(162, changeKeyAssign(51)),
		keyassign53(163, changeKeyAssign(52)),
		keyassign54(164, changeKeyAssign(53)),

	    /**
	     * LNモードの変更
	     */
		lnmode(308, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				final int lnmodelength = 3;
	            PlayerConfig config = selector.resource.getPlayerConfig();
	            config.setLnmode((config.getLnmode() + (arg1 >= 0 ? 1 : lnmodelength - 1)) % lnmodelength);
	            selector.getBarManager().updateBar();
	            selector.play(OPTION_CHANGE);
			}
		}),
		autosavereplay1(321, changeAutoSaveReplay(0)),
		autosavereplay2(322, changeAutoSaveReplay(1)),
		autosavereplay3(323, changeAutoSaveReplay(2)),
		autosavereplay4(324, changeAutoSaveReplay(3)),
		lanecover(330, (state) -> {
			if(state instanceof MusicSelector selector) {
				PlayConfig pc = selector.getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablelanecover(!pc.isEnablelanecover());
					state.play(OPTION_CHANGE);
				}
			}
		}),
		lift(331, (state) -> {
			if(state instanceof MusicSelector) {
				PlayConfig pc = ((MusicSelector)state).getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablelift(!pc.isEnablelift());
					state.play(OPTION_CHANGE);
				}
			}
		}),
		hidden(332, (state) -> {
			if(state instanceof MusicSelector selector) {
				PlayConfig pc = selector.getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnablehidden(!pc.isEnablehidden());
					state.play(OPTION_CHANGE);
				}
			}
		}),
		judgealgorithm(340, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				PlayConfig pc = selector.getSelectedBarPlayConfig();
				if (pc == null) {
					return;
				}
				final JudgeAlgorithm[] algorithms = JudgeAlgorithm.defaultAlgorithm;
				final String jt = pc.getJudgetype();
				for (int i = 0; i < algorithms.length; i++) {
					if (jt.equals(algorithms[i].name())) {
						pc.setJudgetype(algorithms[(arg1 >= 0 ? i + 1 : i + algorithms.length - 1) % algorithms.length].name());
						state.play(OPTION_CHANGE);
					}
				}
			}
		}),
		guidese(343, (state) -> {
			if(state instanceof MusicSelector) {
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setGuideSE(!config.isGuideSE());
				state.play(OPTION_CHANGE);
			}
		}),
		chartreplicationmode(344, (state, arg1) -> {
			if(state instanceof MusicSelector selector) {
				var values = MusicSelector.ChartReplicationMode.values();
				for(int index = 0;index < values.length;index++) {
					if(values[index].name().equals(selector.main.getPlayerConfig().getSortid())) {
						selector.main.getPlayerConfig().setSortid(values[(index + (arg1 >= 0 ? 1 : values.length - 1)) % values.length].name());
						selector.play(OPTION_CHANGE);
						return;
					}
				}
			}
		}),

		extranotedepth(350, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int depthlength = 4;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setExtranoteDepth((config.getExtranoteDepth() + (arg1 >= 0 ? 1 : depthlength - 1)) % depthlength);
				state.play(OPTION_CHANGE);
			}
		}),
		minemode(351, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 5;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setMineMode((config.getMineMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				state.play(OPTION_CHANGE);
			}
		}),
		scrollmode(352, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 3;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setScrollMode((config.getScrollMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				state.play(OPTION_CHANGE);
			}
		}),
		longnotemode(353, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int modelength = 6;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setLongnoteMode((config.getLongnoteMode() + (arg1 >= 0 ? 1 : modelength - 1)) % modelength);
				state.play(OPTION_CHANGE);
			}
		}),
		seventonine_pattern(360, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int patternlength = 7;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setSevenToNinePattern((config.getSevenToNinePattern() + (arg1 >= 0 ? 1 : patternlength - 1)) % patternlength);
				state.play(OPTION_CHANGE);
			}
		}),
		seventonine_type(361, (state, arg1) -> {
			if(state instanceof MusicSelector) {
				final int typelength = 3;
				PlayerConfig config = state.resource.getPlayerConfig();
				config.setSevenToNineType((config.getSevenToNineType() + (arg1 >= 0 ? 1 : typelength - 1)) % typelength);
				state.play(OPTION_CHANGE);
			}
		}),
		constant(SkinProperty.OPTION_CONSTANT, (state) -> {
			if(state instanceof MusicSelector selector) {
				PlayConfig pc = selector.getSelectedBarPlayConfig();
				if (pc != null) {
					pc.setEnableConstant(!pc.isEnableConstant());
					state.play(OPTION_CHANGE);
				}
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
		
	    private static BiConsumer<MainState, Integer> changeAutoSaveReplay(final int index) {
	    	return (state, arg1) -> {
	    		if(state instanceof MusicSelector selector) {
	    	        int[] asr = selector.resource.getPlayerConfig().getAutoSaveReplay();
	    	        final int length = AbstractResult.ReplayAutoSaveConstraint.values().length;
	    	        asr[index] = (asr[index] + (arg1 >= 0 ? 1 : length - 1)) % length;
	    	        selector.resource.getPlayerConfig().setAutoSaveReplay(asr);
	    	        selector.play(OPTION_CHANGE);
	    		}
	    	};
	    }
	    
	    private static Consumer<MainState> changeKeyAssign(final int index) {
	    	return (state) -> {
	    		if(state instanceof KeyConfiguration) {
					final KeyConfiguration keyconfig = (KeyConfiguration) state;
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
