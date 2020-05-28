package bms.player.beatoraja.select;

import static bms.player.beatoraja.select.MusicSelector.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerInformation;
import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRCourseData;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.result.AbstractResult;
import bms.player.beatoraja.song.SongData;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Queue;

public enum MusicSelectCommand {

	/**
	 * 次のMODEフィルター(5KEY, 7KEY, ...)へ移動
	 */
    NEXT_MODE {
        @Override
        public void execute(MusicSelector selector) {
            int mode = 0;
            PlayerConfig config = selector.main.getPlayerConfig();
            for(;mode < MusicSelector.MODE.length && MusicSelector.MODE[mode] != config.getMode();mode++);
            config.setMode(MusicSelector.MODE[(mode + 1) % MusicSelector.MODE.length]);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
	/**
	 * 前のMODEフィルター(5KEY, 7KEY, ...)へ移動
	 */
    PREV_MODE {
        @Override
        public void execute(MusicSelector selector) {
            int mode = 0;
            PlayerConfig config = selector.main.getPlayerConfig();
            for(;mode < MusicSelector.MODE.length && MusicSelector.MODE[mode] != config.getMode();mode++);
            config.setMode(MusicSelector.MODE[(mode - 1 + MusicSelector.MODE.length) % MusicSelector.MODE.length]);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次の選曲バーソート(曲名,  クリアランプ, ...)へ移動
     */
    NEXT_SORT {
        @Override
        public void execute(MusicSelector selector) {
            selector.setSort((selector.getSort() + 1) % BarSorter.values().length);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前の選曲バーソート(曲名,  クリアランプ, ...)へ移動
     */
    PREV_SORT {
        @Override
        public void execute(MusicSelector selector) {
            selector.setSort((selector.getSort() - 1 + BarSorter.values().length) % BarSorter.values().length);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次のLNモードへ移動
     */
    NEXT_LNMODE {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setLnmode((config.getLnmode() + 1) % 3);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前のLNモードへ移動
     */
    PREV_LNMODE {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setLnmode((config.getLnmode() - 1 + 3) % 3);
            selector.getBarRender().updateBar();
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    RESET_REPLAY {
        @Override
        public void execute(MusicSelector selector) {
            if (selector.getBarRender().getSelected() instanceof SelectableBar) {
                boolean[] replays = ((SelectableBar) selector.getBarRender().getSelected()).getExistsReplayData();
                for (int i = 0; i < replays.length; i++) {
                    if (replays[i]) {
                        selector.setSelectedReplay(i);
                        return;
                    }
                }
            }
            selector.setSelectedReplay(-1);
        }
    },
    NEXT_REPLAY {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
            if (current != null && current instanceof SelectableBar) {
                boolean[] replays = ((SelectableBar) current).getExistsReplayData();
                for (int i = 1; i < replays.length; i++) {
                    final int selectedreplay = selector.getSelectedReplay();
                    if (replays[(i + selectedreplay) % replays.length]) {
                        selector.setSelectedReplay((i + selectedreplay) % replays.length);
                        selector.play(SOUND_OPTIONCHANGE);
                        break;
                    }
                }
            }
        }
    },
    PREV_REPLAY {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
            if (current != null && current instanceof SelectableBar) {
                boolean[] replays = ((SelectableBar) current).getExistsReplayData();
                for (int i = 1; i < replays.length; i++) {
                    final int selectedreplay = selector.getSelectedReplay();
                    if (replays[(selectedreplay + replays.length - i) % replays.length]) {
                        selector.setSelectedReplay((selectedreplay + replays.length - i) % replays.length);
                        selector.play(SOUND_OPTIONCHANGE);
                        break;
                    }
                }
            }
        }
    },
    NEXT_RIVAL {
        @Override
        public void execute(MusicSelector selector) {
            PlayerInformation nowrival = selector.getRival();
            boolean match = (nowrival == null);
            for(PlayerInformation rival : selector.getRivals()) {
                if(match) {
                    nowrival = rival;
                    match = false;
                    break;
                }
                match = (nowrival == rival);
            }
            if(match) {
                nowrival = null;
            }
            selector.setRival(nowrival);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_RIVAL {
        @Override
        public void execute(MusicSelector selector) {
            PlayerInformation nowrival = null;
            for(PlayerInformation rival : selector.getRivals()) {
                if (rival == selector.getRival() && nowrival != null) {
                    break;
                }
                nowrival = rival;
            }
            selector.setRival(nowrival);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_TARGET {
        @Override
        public void execute(MusicSelector selector) {
            PlayerInformation nowrival = selector.getRival();
            boolean match = (nowrival == null);
            for(PlayerInformation rival : selector.getRivals()) {
                if(match) {
                    nowrival = rival;
                    match = false;
                    break;
                }
                match = (nowrival == rival);
            }
            if(match) {
                nowrival = null;
            }
            selector.setRival(nowrival);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_TARGET {
        @Override
        public void execute(MusicSelector selector) {
            PlayerInformation nowrival = null;
            for(PlayerInformation rival : selector.getRivals()) {
                if (rival == selector.getRival() && nowrival != null) {
                    break;
                }
                nowrival = rival;
            }
            selector.setRival(nowrival);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次の1P側譜面オプションへ移動
     */
    NEXT_OPTION_1P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setRandom((config.getRandom() + 1) % 10);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前の1P側譜面オプションへ移動
     */
    PREV_OPTION_1P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setRandom((config.getRandom() - 1 + 10) % 10);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次の2P側譜面オプションへ移動
     */
    NEXT_OPTION_2P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setRandom2((config.getRandom2() + 1) % 10);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前の2P側譜面オプションへ移動
     */
    PREV_OPTION_2P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setRandom2((config.getRandom2() - 1 + 10) % 10);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次のDP譜面オプションへ移動
     */
    NEXT_OPTION_DP {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setDoubleoption((config.getDoubleoption() + 1) % 4);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前のDP譜面オプションへ移動
     */
    PREV_OPTION_DP {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setDoubleoption((config.getDoubleoption() - 1 + 4) % 4);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次のゲージオプションへ移動
     */
    NEXT_GAUGE_1P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setGauge((config.getGauge() + 1) % 6);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 前のゲージオプションへ移動
     */
    PREV_GAUGE_1P {
        @Override
        public void execute(MusicSelector selector) {
            PlayerConfig config = selector.main.getPlayerConfig();
            config.setGauge((config.getGauge() - 1 + 6) % 6);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次のハイスピード固定オプションへ移動
     */
    NEXT_HSFIX {
        @Override
        public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null) {
                pc.setFixhispeed((pc.getFixhispeed() + 1) % 5);
                selector.play(SOUND_OPTIONCHANGE);
            }
        }
    },
    /**
     * 前のハイスピード固定オプションへ移動
     */
    PREV_HSFIX {
        @Override
        public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null) {
                pc.setFixhispeed((pc.getFixhispeed() + 4) % 5);
                selector.play(SOUND_OPTIONCHANGE);
            }
        }
    },
    /**
     * 楽曲ファイルの場所をOS既定のファイルブラウザーで開く
     */
    OPEN_WITH_EXPLORER {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
            try {
                if (Desktop.isDesktopSupported()) {
					if (current instanceof SongBar) {
						final SongBar songbar = (SongBar) current;
						if (songbar.existsSong()) {
							Desktop.getDesktop().open(Paths.get(songbar.getSongData().getPath()).getParent().toFile());
						} else if (songbar.getSongData() != null && songbar.getSongData().getOrg_md5() != null) {
							String[] md5 = songbar.getSongData().getOrg_md5().toArray(
									new String[songbar.getSongData().getOrg_md5().size()]);
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
                    } else if(current instanceof FolderBar) {
                        Desktop.getDesktop().open(Paths.get(((FolderBar) current).getFolderData().getPath()).toFile());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    },
    /**
     * 楽曲ファイルのDLサイトをOS既定のブラウザーで開く
     */
    OPEN_DOWNLOAD_SITE {
        @Override
        public void execute(MusicSelector selector) {
            Bar current = selector.getBarRender().getSelected();
            if(current instanceof SongBar) {
            	final SongData song = ((SongBar) current).getSongData();
            	if(song != null) {
					if (song.getUrl() != null && song.getUrl().length() > 0) {
						try {
							URI uri = new URI(song.getUrl());
							Desktop.getDesktop().browse(uri);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					if (song.getAppendurl() != null && song.getAppendurl().length() > 0 && !song.getAppendurl().equals(song.getUrl())) {
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
    },
    /**
     * 楽曲ファイルのIRサイトをOS既定のブラウザーで開く
     */
    OPEN_RANKING_ON_IR {
        @Override
        public void execute(MusicSelector selector) {
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
    },
    DOWNLOAD_IPFS {
        @Override
        public void execute(MusicSelector selector) {
			Queue<DirectoryBar> dir = selector.getBarRender().getDirectory();
			String[] acceptdomain = {"lnt.softether.net","www.ribbit.xyz","rattoto10.jounin.jp","flowermaster.web.fc2.com",
					"stellawingroad.web.fc2.com","pmsdifficulty.xxxxxxxx.jp","walkure.net","stellabms.xyz","dpbmsdelta.web.fc2.com",
					"cgi.geocities.jp/asahi3jpn","nekokan.dyndns.info"};
			boolean startdownload = false;
			for (DirectoryBar d : dir) {
				if (d instanceof TableBar) {
					String selecturl = ((TableBar) d).getUrl();
					if (selecturl == null)
						break;
					for (String url : acceptdomain) {
						if (selecturl.startsWith("http://" + url) || selecturl.startsWith("https://" + url)) {
							Bar current = selector.getBarRender().getSelected();
							if (current instanceof SongBar) {
								final SongData song = ((SongBar) current).getSongData();
								if (song != null && song.getIpfs() != null) {
									selector.main.getMusicDownloadProcessor().start(song);
									startdownload = true;
								}
							}
							break;
						}
					}
					if (!startdownload) {
						Logger.getGlobal().info("ダウンロードは開始されませんでした。");
					}
					break;
				}

			}

        }
    },
    /**
     * 楽曲ファイルのドキュメントをOS既定のドキュメントビューアーで開く
     */
    OPEN_DOCUMENT {
        @Override
        public void execute(MusicSelector selector) {
            if (!Desktop.isDesktopSupported()) {
            	return;
            }
            Bar current = selector.getBarRender().getSelected();
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
    },
    /**
     * 楽曲フォルダ/難易度表を更新する
     */
    UPDATE_FOLDER {

		@Override
		public void execute(MusicSelector selector) {
            Bar selected = selector.getBarRender().getSelected();
			if(selected instanceof FolderBar) {
				selector.main.updateSong(((FolderBar) selected).getFolderData().getPath());
			} else if(selected instanceof TableBar) {
				selector.main.updateTable((TableBar) selected);
			} else if(selected instanceof SongBar) {
				final String path = ((SongBar) selected).getSongData().getPath();
				if(path != null) {
					selector.main.updateSong(Paths.get(path).getParent().toString());
				}
			}
		}
    },
    JUDGETIMING_UP {
		@Override
		public void execute(MusicSelector selector) {
	        final PlayerConfig config = selector.main.getPlayerConfig();
            if (config.getJudgetiming() < PlayerConfig.JUDGETIMING_MAX) {
                config.setJudgetiming(config.getJudgetiming() + 1);
                selector.play(SOUND_OPTIONCHANGE);
            }
		}
    },
    JUDGETIMING_DOWN {
		@Override
		public void execute(MusicSelector selector) {
	        final PlayerConfig config = selector.main.getPlayerConfig();
            if (config.getJudgetiming() > PlayerConfig.JUDGETIMING_MIN) {
                config.setJudgetiming(config.getJudgetiming() - 1);
                selector.play(SOUND_OPTIONCHANGE);
            }
		}
    },
    DURATION_UP {
		@Override
		public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null && pc.getDuration() < 5000) {
                pc.setDuration(pc.getDuration() + 1);
                selector.play(SOUND_OPTIONCHANGE);
            }
		}
    },
    DURATION_DOWN {
		@Override
		public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null && pc.getDuration() > 1) {
                pc.setDuration(pc.getDuration() - 1);
                selector.play(SOUND_OPTIONCHANGE);
            }
		}
    },
    DURATION_UP_LARGE {
        @Override
        public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null && pc.getDuration() < 5000) {
                int duration = pc.getDuration() + 10;
                pc.setDuration(duration - duration % 10);
                selector.play(SOUND_OPTIONCHANGE);
            }
        }
    },
    DURATION_DOWN_LARGE {
        @Override
        public void execute(MusicSelector selector) {
            PlayConfig pc = selector.getSelectedBarPlayConfig();
            if (pc != null && pc.getDuration() > 10) {
                int duration = pc.getDuration() - 10;
                pc.setDuration(duration - duration % 10);
                selector.play(SOUND_OPTIONCHANGE);
            }
        }
    },
    NEXT_BGA_SHOW {
		@Override
		public void execute(MusicSelector selector) {
            selector.main.getConfig().setBga((selector.main.getConfig().getBga() + 1) % 3);
            selector.play(SOUND_OPTIONCHANGE);
		}
    },
    PREV_BGA_SHOW {
        @Override
        public void execute(MusicSelector selector) {
            selector.main.getConfig().setBga((selector.main.getConfig().getBga() - 1 + 3) % 3);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_GAUGEAUTOSHIFT {
		@Override
		public void execute(MusicSelector selector) {
            selector.main.getPlayerConfig().setGaugeAutoShift((selector.main.getPlayerConfig().getGaugeAutoShift() + 1) % 5);
            selector.play(SOUND_OPTIONCHANGE);
		}
    },
    PREV_GAUGEAUTOSHIFT {
        @Override
        public void execute(MusicSelector selector) {
            selector.main.getPlayerConfig().setGaugeAutoShift((selector.main.getPlayerConfig().getGaugeAutoShift() - 1 + 5) % 5);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_AUTOSAVEREPLAY_1 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[0] = (asr[0] + 1) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_AUTOSAVEREPLAY_1 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[0] = (asr[0] - 1 + AbstractResult.ReplayAutoSaveConstraint.values().length) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_AUTOSAVEREPLAY_2 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[1] = (asr[1] + 1) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_AUTOSAVEREPLAY_2 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[1] = (asr[1] - 1 + AbstractResult.ReplayAutoSaveConstraint.values().length) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_AUTOSAVEREPLAY_3 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[2] = (asr[2] + 1) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_AUTOSAVEREPLAY_3 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[2] = (asr[2] - 1 + AbstractResult.ReplayAutoSaveConstraint.values().length) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    NEXT_AUTOSAVEREPLAY_4 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[3] = (asr[3] + 1) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    PREV_AUTOSAVEREPLAY_4 {
        @Override
        public void execute(MusicSelector selector) {
            int[] asr = selector.main.getConfig().getAutoSaveReplay();
            asr[3] = (asr[3] - 1 + AbstractResult.ReplayAutoSaveConstraint.values().length) % AbstractResult.ReplayAutoSaveConstraint.values().length;
            selector.main.getConfig().setAutoSaveReplay(asr);
            selector.play(SOUND_OPTIONCHANGE);
        }
    },
    /**
     * 次の楽曲FAVORITE属性へ遷移
     */
    NEXT_FAVORITE_SONG {
        @Override
        public void execute(MusicSelector selector) {
			if(selector.getSelectedBar() instanceof SongBar) {
				final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

				if(sd != null) {
					int type = 2;
					String message = "Added to Invisible Song";
					if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
						type = 1;
						message = "Added to Favorite Song";
					} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
						type = 0;
						message = "Removed from Invisible Song";
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
		            selector.play(SOUND_OPTIONCHANGE);
				}
			}
        }
    },
    /**
     * 前の楽曲FAVORITE属性へ遷移
     */
    PREV_FAVORITE_SONG {
        @Override
        public void execute(MusicSelector selector) {
			if(selector.getSelectedBar() instanceof SongBar) {
				final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

				if(sd != null) {
					int type = 0;
					String message = "Removed from Favorite Song";
					if((sd.getFavorite() & (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG)) == 0) {
						type = 2;
						message = "Added to Invisible Song";
					} else if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
						type = 1;
						message = "Added to Favorite Song";
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
		            selector.play(SOUND_OPTIONCHANGE);
				}
			}
        }
    },
    /**
     * 次の譜面FAVORITE属性へ遷移
     */
    NEXT_FAVORITE_CHART {
        @Override
        public void execute(MusicSelector selector) {
			if(selector.getSelectedBar() instanceof SongBar) {
				final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

				if(sd != null) {
					int type = 2;
					String message = "Added to Invisible Chart";
					if((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
						type = 1;
						message = "Added to Favorite Chart";
					} else if((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
						type = 0;
						message = "Removed from Invisible Chart";
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
		            selector.play(SOUND_OPTIONCHANGE);
				}
			}
        }
    },
    /**
     * 前の譜面FAVORITE属性へ遷移
     */
    PREV_FAVORITE_CHART {
        @Override
        public void execute(MusicSelector selector) {
			if(selector.getSelectedBar() instanceof SongBar) {
				final SongData sd = ((SongBar) selector.getSelectedBar()).getSongData();

				if(sd != null) {
					int type = 0;
					String message = "Removed from Favorite Chart";
					if((sd.getFavorite() & (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART)) == 0) {
						type = 2;
						message = "Added to Invisible Chart";
					} else if((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
						type = 1;
						message = "Added to Favorite Chart";
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
		            selector.play(SOUND_OPTIONCHANGE);
				}
			}
        }
    },
    /**
     * 同一フォルダにある譜面を全て表示する．コースの場合は構成譜面を全て表示する
     */
    SHOW_SONGS_ON_SAME_FOLDER {
        @Override
        public void execute(MusicSelector selector) {
        	final BarRenderer bar = selector.getBarRender();
            Bar current = selector.getBarRender().getSelected();
            if (current instanceof SongBar && ((SongBar) current).existsSong() &&
                    (bar.getDirectory().size == 0 || !(bar.getDirectory().last() instanceof SameFolderBar))) {
                SongData sd = ((SongBar) current).getSongData();
                bar.updateBar(new SameFolderBar(selector, sd.getFullTitle(), sd.getFolder()));
                selector.play(SOUND_FOLDEROPEN);
            } else if (current instanceof GradeBar) {
                List<Bar> songbars = Arrays.asList(((GradeBar) current).getSongDatas()).stream()
                        .distinct()
                        .map(SongBar::new)
                        .collect(Collectors.toList());
                bar.updateBar(new ContainerBar(current.getTitle(), songbars.toArray(new Bar[songbars.size()])));
                selector.play(SOUND_FOLDEROPEN);
            }
        }    	
    }
    ;

    public abstract void execute(MusicSelector selector);
}
