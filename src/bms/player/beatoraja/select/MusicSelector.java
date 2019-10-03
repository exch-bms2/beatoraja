package bms.player.beatoraja.select;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.nio.file.DirectoryStream;
import java.io.File;
import java.nio.file.*;
import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Keys;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.external.ScoreDataImporter;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 *
 * @author exch
 */
public class MusicSelector extends MainState {

	// TODO　ミラーランダム段位のスコア表示

	private int selectedreplay;

	/**
	 * 楽曲DBアクセサ
	 */
	private SongDatabaseAccessor songdb;

	public static final Mode[] MODE = { null, Mode.BEAT_7K, Mode.BEAT_14K, Mode.POPN_9K, Mode.BEAT_5K, Mode.BEAT_10K, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE };

	/**
	 * 保存可能な最大リプレイ数
	 */
	public static final int REPLAY = 4;

	private PlayerConfig config;

	/**
	 * 楽曲プレビュー処理
	 */
	private PreviewMusicProcessor preview;

	/**
	 * 楽曲バー描画用
	 */
	private BarRenderer bar;
	private MusicSelectInputProcessor musicinput;

	private SearchTextField search;

	/**
	 * 楽曲が選択されてからbmsを読み込むまでの時間(ms)
	 */
	private final int notesGraphDuration = 350;
	/**
	 * 楽曲が選択されてからプレビュー曲を再生するまでの時間(ms)
	 */
	private final int previewDuration = 400;
	private boolean showNoteGraph = false;

	private ScoreDataCache scorecache;
	private ScoreDataCache rivalcache;

	private ObjectMap<PlayerInformation, ScoreDataCache> rivalcaches = new ObjectMap<PlayerInformation, ScoreDataCache>();
	private PlayerInformation rival;

	private int panelstate;

	public static final int SOUND_BGM = 0;
	public static final int SOUND_SCRATCH = 1;
	public static final int SOUND_FOLDEROPEN = 2;
	public static final int SOUND_FOLDERCLOSE = 3;
	public static final int SOUND_OPTIONCHANGE = 4;
	public static final int SOUND_OPTIONOPEN = 5;
	public static final int SOUND_OPTIONCLOSE = 6;

	private PlayMode play = null;

	private PixmapResourcePool banners;

	private PixmapResourcePool stagefiles;

	public MusicSelector(MainController main, boolean songUpdated) {
		super(main);
		this.config = main.getPlayerResource().getPlayerConfig();

		songdb = main.getSongDatabase();

		final PlayDataAccessor pda = main.getPlayDataAccessor();

		scorecache = new ScoreDataCache() {
			@Override
			protected IRScoreData readScoreDatasFromSource(SongData song, int lnmode) {
				return pda.readScoreData(song.getSha256(), song.hasUndefinedLongNote(), lnmode);
			}

			@Override
			protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
				pda.readScoreDatas(collector, songs, lnmode);
			}
		};

		if(main.getIRStatus().length > 0) {
			if(main.getIRStatus()[0].config.isImportscore()) {
				main.getIRStatus()[0].config.setImportscore(false);
				try {
					IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(null, null);
					if(scores.isSucceeded()) {
						ScoreDataImporter scoreimport = new ScoreDataImporter(new ScoreDatabaseAccessor(main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + File.separatorChar + "score.db"));
						scoreimport.importScores(scores.getData(), main.getIRStatus()[0].config.getIrname());

						Logger.getGlobal().info("IRからのスコアインポート完了");
					} else {
						Logger.getGlobal().warning("IRからのスコアインポート失敗 : " + scores.getMessage());
					}					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
			IRResponse<PlayerInformation[]> response = main.getIRStatus()[0].connection.getRivals();
			if(response.isSucceeded()) {
				try {
					
					// ライバルスコアデータベース作成
					// TODO 別のクラスに移動
					if(!Files.exists(Paths.get("rival"))) {
						Files.createDirectory(Paths.get("rival"));
					}

					// ライバルキャッシュ作成
					if(main.getIRStatus()[0].config.isImportrival()) {
						for(PlayerInformation rival : response.getData()) {
							final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor("rival/" + main.getIRStatus()[0].config.getIrname() + rival.getId() + ".db");
							rivalcaches.put(rival,  new ScoreDataCache() {

								@Override
								protected IRScoreData readScoreDatasFromSource(SongData song, int lnmode) {
									return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
								}

								protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
									scoredb.getScoreDatas(collector,songs, lnmode);
								}
							});
							new Thread(() -> {
								scoredb.createTable();
								scoredb.setInformation(rival);
								IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(rival.getId(), null);
								if(scores.isSucceeded()) {
									scoredb.setScoreData(scores.getData());
									Logger.getGlobal().info("IRからのライバルスコア取得完了 : " + rival.getName());
								} else {
									Logger.getGlobal().warning("IRからのライバルスコア取得失敗 : " + scores.getMessage());
								}
							}).start();
						}						
					}
					
					try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("rival"))) {
						for (Path p : paths) {
							boolean exists = false;
							for(PlayerInformation info : rivalcaches.keys()) {
								if(p.getFileName().toString().equals(main.getIRStatus()[0].config.getIrname() + info.getId() + ".db")) {
									exists = true;
									break;
								}
							}
							if(exists) {
								continue;
							}
							
							if(p.toString().endsWith(".db")) {
								final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor(p.toString());
								PlayerInformation info = scoredb.getInformation();
								if(info != null) {
									rivalcaches.put(info,  new ScoreDataCache() {

										@Override
										protected IRScoreData readScoreDatasFromSource(SongData song, int lnmode) {
											return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
										}

										protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
											scoredb.getScoreDatas(collector,songs, lnmode);
										}
									});
									Logger.getGlobal().info("ローカルに保存されているライバルスコア取得完了 : " + info.getName());
								}
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}

				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				Logger.getGlobal().warning("IRからのライバル取得失敗 : " + response.getMessage());
			}
		}

		bar = new BarRenderer(this);
		banners = new PixmapResourcePool(main.getConfig().getBannerPixmapGen());
		stagefiles = new PixmapResourcePool(main.getConfig().getStagefilePixmapGen());
		musicinput = new MusicSelectInputProcessor(this);

		if (!songUpdated && main.getPlayerResource().getConfig().isUpdatesong()) {
			main.updateSong(null);
		}
	}

	public void setRival(PlayerInformation rival) {
		this.rival = rival;
		rivalcache = rival != null ? rivalcaches.get(rival) : null;
		bar.updateBar();

		Logger.getGlobal().info("Rival変更:" + (rival != null ? rival.getName() : "なし"));
	}

	public PlayerInformation getRival() {
		return rival;
	}

	public Keys<PlayerInformation> getRivals() {
		return rivalcaches.keys();
	}

	public ScoreDataCache getScoreDataCache() {
		return scorecache;
	}

	public ScoreDataCache getRivalScoreDataCache() {
		return rivalcache;
	}

	public void create() {
		main.getSoundManager().shuffle();
		setSound(SOUND_BGM, "select.wav", SoundType.BGM, true);
		setSound(SOUND_SCRATCH, "scratch.wav", SoundType.SOUND, false);
		setSound(SOUND_FOLDEROPEN, "f-open.wav", SoundType.SOUND,false);
		setSound(SOUND_FOLDERCLOSE, "f-close.wav", SoundType.SOUND,false);
		setSound(SOUND_OPTIONCHANGE, "o-change.wav", SoundType.SOUND,false);
		setSound(SOUND_OPTIONOPEN, "o-open.wav", SoundType.SOUND,false);
		setSound(SOUND_OPTIONCLOSE, "o-close.wav", SoundType.SOUND,false);

		play = null;
		showNoteGraph = false;
		main.getPlayerResource().setPlayerData(main.getPlayDataAccessor().readPlayerData());
		if (bar.getSelected() != null && bar.getSelected() instanceof SongBar) {
			scorecache.update(((SongBar) bar.getSelected()).getSongData(), config.getLnmode());
		}

		preview = new PreviewMusicProcessor(main.getAudioProcessor(), main.getPlayerResource().getConfig());
		preview.setDefault(getSound(SOUND_BGM));
		preview.start(null);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayModeConfig pc = (config.getMusicselectinput() == 0 ? config.getMode7()
				: (config.getMusicselectinput() == 1 ? config.getMode9() : config.getMode14()));
		input.setKeyboardConfig(pc.getKeyboardConfig());
		input.setControllerConfig(pc.getController());
		input.setMidiConfig(pc.getMidiConfig());
		bar.updateBar();

		loadSkin(SkinType.MUSIC_SELECT);

		// search text field
		if (getStage() == null && ((MusicSelectSkin) getSkin()).getSearchTextRegion() != null) {
			if(search != null) {
				search.dispose();
			}
			search = new SearchTextField(this, main.getPlayerResource().getConfig().getResolution());
			setStage(search);
		}
	}

	public void render() {
		final PlayerResource resource = main.getPlayerResource();
		final Bar current = bar.getSelected();
        if(main.getNowTime() > getSkin().getInput()){
        	main.switchTimer(TIMER_STARTINPUT, true);
        }
		if(main.getNowTime(TIMER_SONGBAR_CHANGE) < 0) {
			main.setTimerOn(TIMER_SONGBAR_CHANGE);
		}
		// draw song information
		resource.setSongdata(current instanceof SongBar ? ((SongBar) current).getSongData() : null);
		resource.setCourseData(current instanceof GradeBar ? ((GradeBar) current).getCourseData() : null);

		// preview music
		if (current instanceof SongBar) {
			final SongData song = main.getPlayerResource().getSongdata();
			if (song != preview.getSongData() && main.getNowTime() > main.getTimer(TIMER_SONGBAR_CHANGE) + previewDuration
					&& play == null) {
				this.preview.start(song);
			}
		}

		// read bms information
		if (main.getNowTime() > main.getTimer(TIMER_SONGBAR_CHANGE) + notesGraphDuration && !showNoteGraph && play == null) {
			if (current instanceof SongBar && ((SongBar) current).existsSong()) {
				SongData song = main.getPlayerResource().getSongdata();
				new Thread(() ->  {
					song.setBMSModel(resource.loadBMSModel(Paths.get(((SongBar) current).getSongData().getPath()),
							config.getLnmode()));
				}).start();;
			}
			showNoteGraph = true;
		}

		if (play != null) {
			if (current instanceof SongBar) {
				SongData song = ((SongBar) current).getSongData();
				if (((SongBar) current).existsSong()) {
					resource.clear();
					if (resource.setBMSFile(Paths.get(song.getPath()), play)) {
						final Queue<DirectoryBar> dir = this.getBarRender().getDirectory();
						if(!(dir.last() instanceof SameFolderBar)) {
							Array<String> urls = new Array(main.getConfig().getTableURL());

							boolean isdtable = false;
							for (DirectoryBar bar : dir) {
								if (bar instanceof TableBar) {
									String currenturl = ((TableBar) bar).getUrl();
									if (currenturl != null && urls.contains(currenturl, false)) {
										isdtable = true;
										resource.setTablename(bar.getTitle());
									}
								}
								if (bar instanceof HashBar && isdtable) {
									resource.setTablelevel(bar.getTitle());
									break;
								}
							}
						}
						preview.stop();
						main.changeState(MainStateType.DECIDE);
						banners.disposeOld();
						stagefiles.disposeOld();
					} else {
						main.getMessageRenderer().addMessage("Failed to loading BMS : Song not found, or Song has error", 1200, Color.RED, 1);
					}
				} else if (song.getIpfs() != null && main.getMusicDownloadProcessor() != null
						&& main.getMusicDownloadProcessor().isAlive()) {
					execute(MusicSelectCommand.DOWNLOAD_IPFS);
				} else {
	                execute(MusicSelectCommand.OPEN_DOWNLOAD_SITE);
				}
			} else if (current instanceof ExecutableBar) {
				SongData song = ((ExecutableBar) current).getSongData();
				resource.clear();
				if (resource.setBMSFile(Paths.get(song.getPath()), play)) {
					final Queue<DirectoryBar> dir = this.getBarRender().getDirectory();
					if(!(dir.last() instanceof SameFolderBar)) {
						Array<String> urls = new Array(main.getConfig().getTableURL());

						boolean isdtable = false;
						for (DirectoryBar bar : dir) {
							if (bar instanceof TableBar) {
								String currenturl = ((TableBar) bar).getUrl();
								if (currenturl != null && urls.contains(currenturl, false)) {
									isdtable = true;
									resource.setTablename(bar.getTitle());
								}
							}
							if (bar instanceof HashBar && isdtable) {
								resource.setTablelevel(bar.getTitle());
								break;
							}
						}
					}
					preview.stop();
					main.changeState(MainStateType.DECIDE);
					banners.disposeOld();
					stagefiles.disposeOld();
				} else {
					main.getMessageRenderer().addMessage("Failed to loading BMS : Song not found, or Song has error", 1200, Color.RED, 1);
				}
			}else if (current instanceof GradeBar) {
				if (play == PlayMode.PRACTICE) {
					play = PlayMode.PLAY;
				}
				readCourse(play);
			} else if (current instanceof DirectoryBar) {
				if(play.isAutoPlayMode()) {
					Array<Path> paths = new Array<Path>();
					for(Bar bar : ((DirectoryBar) current).getChildren()) {
						if(bar instanceof SongBar && ((SongBar) bar).getSongData() != null && ((SongBar) bar).getSongData().getPath() != null) {
							paths.add(Paths.get(((SongBar) bar).getSongData().getPath()));
						}
					}
					if(paths.size > 0) {
						resource.clear();
						resource.setAutoPlaySongs(paths.toArray(Path.class), false);
						if(resource.nextSong()) {
							preview.stop();
							main.changeState(MainStateType.DECIDE);
							banners.disposeOld();
							stagefiles.disposeOld();
						}
					}
				}
			}
			play = null;
		}
	}

	public void input() {
		final BMSPlayerInputProcessor input = main.getInputProcessor();

		if (input.getNumberState()[6]) {
			preview.stop();
			main.changeState(MainStateType.CONFIG);
		} else if (input.isActivated(KeyCommand.OPEN_SKIN_CONFIGURATION)) {
			preview.stop();
			main.changeState(MainStateType.SKINCONFIG);
		}

		musicinput.input();
	}

	public void select(Bar current) {
		if (current instanceof DirectoryBar) {
			if (bar.updateBar(current)) {
				play(SOUND_FOLDEROPEN);
			}
			execute(MusicSelectCommand.RESET_REPLAY);
		} else {
			play = PlayMode.PLAY;
		}
	}

	public int getSelectedReplay() {
		return  selectedreplay;
	}

	public void setSelectedReplay(int index) {
		selectedreplay = index;
	}

	public void execute(MusicSelectCommand command) {
		command.execute(this);
	}

	private void readCourse(PlayMode mode) {
		final PlayerResource resource = main.getPlayerResource();
		final GradeBar course = (GradeBar) bar.getSelected();
		if (!course.existsAllSongs()) {
			Logger.getGlobal().info("段位の楽曲が揃っていません");
			return;
		}

		resource.clear();
		final SongData[] songs = course.getSongDatas();
		Path[] files = new Path[songs.length];
		int i = 0;
		for (SongData song : songs) {
			files[i++] = Paths.get(song.getPath());
		}
		if (resource.setCourseBMSFiles(files)) {
			if (mode == PlayMode.PLAY || mode.isAutoPlayMode()) {
				for (CourseData.CourseDataConstraint constraint : course.getCourseData().getConstraint()) {
					switch (constraint) {
					case CLASS:
						config.setRandom(0);
						config.setRandom2(0);
						config.setDoubleoption(0);
						break;
					case MIRROR:
						if (config.getRandom() == 1) {
							config.setRandom2(1);
							config.setDoubleoption(1);
						} else {
							config.setRandom(0);
							config.setRandom2(0);
							config.setDoubleoption(0);
						}
						break;
					case RANDOM:
						if (config.getRandom() > 5) {
							config.setRandom(0);
						}
						if (config.getRandom2() > 5) {
							config.setRandom2(0);
						}
						break;
					case LN:
						config.setLnmode(0);
						break;
					case CN:
						config.setLnmode(1);
						break;
					case HCN:
						config.setLnmode(2);
						break;
					default:
						break;
					}
				}
			}
			preview.stop();
			course.getCourseData().setSong(resource.getCourseBMSModels());
			resource.setCourseData(course.getCourseData());
			resource.setBMSFile(files[0], mode);
			main.changeState(MainStateType.DECIDE);
			banners.disposeOld();
			stagefiles.disposeOld();
		} else {
			main.getMessageRenderer().addMessage("Failed to loading Course : Some of songs not found", 1200, Color.RED, 1);
			Logger.getGlobal().info("段位の楽曲が揃っていません");
		}
	}

	public int getSort() {
		return config.getSort();
	}

	public void setSort(int sort) {
		config.setSort(sort);
	}

	public void dispose() {
		super.dispose();
		bar.dispose();
		banners.dispose();
		stagefiles.dispose();
		if (search != null) {
			search.dispose();
			search = null;
		}
	}

	public int getPanelState() {
		return panelstate;
	}

	public void setPanelState(int panelstate) {
		if (this.panelstate != panelstate) {
			if (this.panelstate != 0) {
				main.setTimerOn(TIMER_PANEL1_OFF + this.panelstate - 1);
				main.setTimerOff(TIMER_PANEL1_ON + this.panelstate - 1);
			}
			if (panelstate != 0) {
				main.setTimerOn(TIMER_PANEL1_ON + panelstate - 1);
				main.setTimerOff(TIMER_PANEL1_OFF + panelstate - 1);
			}
		}
		this.panelstate = panelstate;
	}

	public SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	public boolean existsConstraint(CourseData.CourseDataConstraint constraint) {
		if (!(bar.getSelected() instanceof GradeBar)) {
			return false;
		}

		GradeBar gb = (GradeBar) bar.getSelected();
		for (CourseData.CourseDataConstraint con : gb.getCourseData().getConstraint()) {
			if(con == constraint) {
				return true;
			}
		}
		return false;
	}

	public void executeEvent(int id, int arg1, int arg2) {
		switch (id) {
		case BUTTON_PLAY:
			play = PlayMode.PLAY;
			break;
		case BUTTON_AUTOPLAY:
			play = PlayMode.AUTOPLAY;
			break;
		case BUTTON_PRACTICE:
			play = PlayMode.PRACTICE;
			break;
		case BUTTON_REPLAY:
			play = PlayMode.REPLAY_1;
			break;
		case BUTTON_REPLAY2:
			play = PlayMode.REPLAY_2;
			break;
		case BUTTON_REPLAY3:
			play = PlayMode.REPLAY_3;
			break;
		case BUTTON_REPLAY4:
			play = PlayMode.REPLAY_4;
			break;
		case BUTTON_READTEXT:
			execute(MusicSelectCommand.OPEN_DOCUMENT);
			break;
		case BUTTON_MODE:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_MODE : MusicSelectCommand.PREV_MODE);
			break;
		case BUTTON_SORT:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_SORT : MusicSelectCommand.PREV_SORT);
			break;
		case BUTTON_LNMODE:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_LNMODE : MusicSelectCommand.PREV_LNMODE);
			break;
		case BUTTON_RANDOM_1P:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_OPTION_1P : MusicSelectCommand.PREV_OPTION_1P);
			break;
		case BUTTON_RANDOM_2P:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_OPTION_2P : MusicSelectCommand.PREV_OPTION_2P);
			break;
		case BUTTON_DPOPTION:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_OPTION_DP : MusicSelectCommand.PREV_OPTION_DP);
			break;
		case BUTTON_GAUGE_1P:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_GAUGE_1P : MusicSelectCommand.PREV_GAUGE_1P);
			break;
		case BUTTON_HSFIX:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_HSFIX : MusicSelectCommand.PREV_HSFIX);
			break;
		case BUTTON_TARGET:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_TARGET : MusicSelectCommand.PREV_TARGET);
			break;
		case BUTTON_BGA:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_BGA_SHOW : MusicSelectCommand.PREV_BGA_SHOW);
			break;
		case BUTTON_GAUGEAUTOSHIFT:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_GAUGEAUTOSHIFT : MusicSelectCommand.PREV_GAUGEAUTOSHIFT);
			break;
		case BUTTON_AUTOSAVEREPLAY_1:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_AUTOSAVEREPLAY_1 : MusicSelectCommand.PREV_AUTOSAVEREPLAY_1);
			break;
		case BUTTON_AUTOSAVEREPLAY_2:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_AUTOSAVEREPLAY_2 : MusicSelectCommand.PREV_AUTOSAVEREPLAY_2);
			break;
		case BUTTON_AUTOSAVEREPLAY_3:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_AUTOSAVEREPLAY_3 : MusicSelectCommand.PREV_AUTOSAVEREPLAY_3);
			break;
		case BUTTON_AUTOSAVEREPLAY_4:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_AUTOSAVEREPLAY_4 : MusicSelectCommand.PREV_AUTOSAVEREPLAY_4);
			break;
		default:
			super.executeEvent(id, arg1, arg2);
		}
	}

	public Bar getSelectedBar() {
		return bar.getSelected();
	}

	public BarRenderer getBarRender() {
		return bar;
	}

	public PixmapResourcePool getBannerResource() {
		return banners;
	}
	public PixmapResourcePool getStagefileResource() {
		return stagefiles;
	}

	public void selectedBarMoved() {
		execute(MusicSelectCommand.RESET_REPLAY);
		loadSelectedSongImages();

		main.setTimerOn(TIMER_SONGBAR_CHANGE);
		if(preview.getSongData() != null && (!(bar.getSelected() instanceof SongBar) ||
				((SongBar) bar.getSelected()).getSongData().getFolder().equals(preview.getSongData().getFolder()) == false))
		preview.start(null);
		showNoteGraph = false;
	}

	public void loadSelectedSongImages() {
		// banner
		// stagefile
		final Bar current = bar.getSelected();
		main.getPlayerResource().getBMSResource().setBanner(
				current instanceof SongBar ? ((SongBar) current).getBanner() : null);
		main.getPlayerResource().getBMSResource().setStagefile(
				current instanceof SongBar ? ((SongBar) current).getStagefile() : null);
	}

	public void selectSong(PlayMode mode) {
		if (!mode.isReplayMode()) {
			play = mode;
		} else {
			play = (selectedreplay >= 0) ? PlayMode.getReplayMode(selectedreplay) : PlayMode.PLAY;
		}
	}

	public PlayConfig getSelectedBarPlayConfig() {
		Bar current = bar.getSelected();
		PlayConfig pc = null;
		if (current instanceof SongBar && ((SongBar)current).existsSong()) {
			SongBar song = (SongBar) current;
			pc = main.getPlayerConfig().getPlayConfig(song.getSongData().getMode()).getPlayconfig();
		} else if(current instanceof GradeBar && ((GradeBar)current).existsAllSongs()) {
			GradeBar grade = (GradeBar)current;
			for(SongData song : grade.getSongDatas()) {
				PlayConfig pc2 = main.getPlayerConfig().getPlayConfig(song.getMode()).getPlayconfig();
				if(pc == null) {
					pc = pc2;
				}
				if(pc != pc2) {
					pc = null;
					break;
				}
			}
		}
		return pc;
	}
}
