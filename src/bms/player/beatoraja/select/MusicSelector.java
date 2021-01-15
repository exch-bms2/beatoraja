package bms.player.beatoraja.select;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.nio.file.DirectoryStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.lang.StringBuilder;
import java.nio.file.*;
import java.util.logging.Logger;

import bms.player.beatoraja.ir.IRPlayerData;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Keys;

import bms.model.BMSDecoder;
import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.external.ScoreDataImporter;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.ir.*;
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
	
	private final int rankingDuration = 5000;
	private final int rankingReloadDuration = 10 * 60 * 1000;
	
	private long currentRankingDuration = -1;

	private boolean showNoteGraph = false;

	private ScoreDataCache scorecache;
	private ScoreDataCache rivalcache;
	
	private RankingData currentir;
	private RankingDataCache ircache = new RankingDataCache();

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

	private SongData playedsong = null;
	private CourseData playedcourse = null;

	private PixmapResourcePool banners;

	private PixmapResourcePool stagefiles;

	public MusicSelector(MainController main, boolean songUpdated) {
		super(main);
		this.config = main.getPlayerResource().getPlayerConfig();

		songdb = main.getSongDatabase();

		final PlayDataAccessor pda = main.getPlayDataAccessor();

		scorecache = new ScoreDataCache() {
			@Override
			protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
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
					IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(main.getIRStatus()[0].player, null);
					if(scores.isSucceeded()) {
						ScoreDataImporter scoreimport = new ScoreDataImporter(new ScoreDatabaseAccessor(main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + File.separatorChar + "score.db"));
						scoreimport.importScores(convert(scores.getData()), main.getIRStatus()[0].config.getIrname());

						Logger.getGlobal().info("IRからのスコアインポート完了");
					} else {
						Logger.getGlobal().warning("IRからのスコアインポート失敗 : " + scores.getMessage());
					}					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
			IRResponse<IRPlayerData[]> response = main.getIRStatus()[0].connection.getRivals();
			if(response.isSucceeded()) {
				try {
					
					// ライバルスコアデータベース作成
					// TODO 別のクラスに移動
					if(!Files.exists(Paths.get("rival"))) {
						Files.createDirectory(Paths.get("rival"));
					}

					// ライバルキャッシュ作成
					if(main.getIRStatus()[0].config.isImportrival()) {
						for(IRPlayerData irplayer : response.getData()) {
							final PlayerInformation rival = new PlayerInformation();
							rival.setId(irplayer.id);
							rival.setName(irplayer.name);
							rival.setRank(irplayer.rank);
							final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor("rival/" + main.getIRStatus()[0].config.getIrname() + rival.getId() + ".db");
							rivalcaches.put(rival,  new ScoreDataCache() {

								@Override
								protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
									return scoredb.getScoreData(song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
								}

								protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
									scoredb.getScoreDatas(collector,songs, lnmode);
								}
							});
							new Thread(() -> {
								scoredb.createTable();
								scoredb.setInformation(rival);
								IRResponse<IRScoreData[]> scores = main.getIRStatus()[0].connection.getPlayData(irplayer, null);
								if(scores.isSucceeded()) {
									scoredb.setScoreData(convert(scores.getData()));
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
										protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
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
	
	private ScoreData[] convert(IRScoreData[] irscores) {
		ScoreData[] scores = new ScoreData[irscores.length];
		for(int i = 0;i < scores.length;i++) {
			final ScoreData score = new ScoreData();
			final bms.player.beatoraja.ir.IRScoreData irscore = irscores[i];
			score.setSha256(irscore.sha256);
			score.setMode(irscore.lntype);
			score.setPlayer(irscore.player);
			score.setClear(irscore.clear.id); 
			score.setDate(irscore.date);
			score.setEpg(irscore.epg);
			score.setLpg(irscore.lpg);
			score.setEgr(irscore.egr);
			score.setLgr(irscore.lgr);
			score.setEgd(irscore.egd);
			score.setLgd(irscore.lgd);
			score.setEbd(irscore.ebd);
			score.setLbd(irscore.lbd);
			score.setEpr(irscore.epr);
			score.setLpr(irscore.lpr);
			score.setEms(irscore.ems);
			score.setLms(irscore.lms);
			score.setCombo(irscore.maxcombo);
			score.setNotes(irscore.notes);
			score.setPassnotes(irscore.passnotes != 0 ? irscore.notes : irscore.passnotes);
			score.setMinbp(irscore.minbp);
			score.setOption(irscore.option);
			score.setAssist(irscore.assist);
			score.setGauge(irscore.gauge);
			score.setDeviceType(irscore.deviceType);
			
			scores[i] = score;
		}
		return scores;
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
		if (playedsong != null) {
			scorecache.update(playedsong, config.getLnmode());
			playedsong = null;
		}
		if (playedcourse != null) {
			for (SongData sd : playedcourse.getSong()) {
				scorecache.update(sd, config.getLnmode());
			}
			playedcourse = null;
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
		Rectangle searchRegion = ((MusicSelectSkin) getSkin()).getSearchTextRegion();
		if (searchRegion != null && (getStage() == null ||
				(search != null && !searchRegion.equals(search.getSearchBounds())))) {
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
		if (current instanceof SongBar && main.getConfig().isPlayPreview()) {
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
		// get ir ranking
		if (currentRankingDuration != -1 && main.getNowTime() > main.getTimer(TIMER_SONGBAR_CHANGE) + currentRankingDuration) {
			currentRankingDuration = -1;
			if (current instanceof SongBar && ((SongBar) current).existsSong() && play == null) {
				SongData song = ((SongBar) current).getSongData();
				RankingData irc = ircache.get(song, config.getLnmode());
				if(irc == null) {
					irc = new RankingData();
		            ircache.put(song, config.getLnmode(), irc);
				}
				irc.load(this, song);
	            currentir = irc;
			}				
			if (current instanceof GradeBar && ((GradeBar) current).existsAllSongs() && play == null) {
				CourseData course = ((GradeBar) current).getCourseData();
				RankingData irc = ircache.get(course, config.getLnmode());
				if(irc == null) {
					irc = new RankingData();
		            ircache.put(course, config.getLnmode(), irc);
				}
				irc.load(this, course);
	            currentir = irc;
			}				
		}
		final int irstate = currentir != null ? currentir.getState() : -1;
		main.switchTimer(TIMER_IR_CONNECT_BEGIN, irstate == RankingData.ACCESS);
		main.switchTimer(TIMER_IR_CONNECT_SUCCESS, irstate == RankingData.FINISH);
		main.switchTimer(TIMER_IR_CONNECT_FAIL, irstate == RankingData.FAIL);

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
						if(main.getIRStatus().length > 0 && currentir == null) {
							currentir = new RankingData();
				            ircache.put(song, config.getLnmode(), currentir);
						}
						resource.setRankingData(currentir);
						
						playedsong = song;
						changeState(MainStateType.DECIDE);
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
					playedsong = song;
					changeState(MainStateType.DECIDE);
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
							changeState(MainStateType.DECIDE);
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
			changeState(MainStateType.CONFIG);
		} else if (input.isActivated(KeyCommand.OPEN_SKIN_CONFIGURATION)) {
			changeState(MainStateType.SKINCONFIG);
		}

		musicinput.input();
	}
	
	void changeState(MainStateType type) {
		preview.stop();
		main.changeState(type);
		if (search != null) {
			search.unfocus(this);
		}
		banners.disposeOld();
		stagefiles.disposeOld();
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
			course.getCourseData().setSong(resource.getCourseBMSModels());
			resource.setCourseData(course.getCourseData());
			resource.setBMSFile(files[0], mode);
			playedcourse = course.getCourseData();
			
			if(main.getIRStatus().length > 0 && currentir == null) {
				currentir = new RankingData();
	            ircache.put(course.getCourseData(), config.getLnmode(), currentir);
			}
			resource.setRankingData(currentir);

			changeState(MainStateType.DECIDE);
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
		case BUTTON_KEYCONFIG:
			changeState(MainStateType.CONFIG);
			break;
		case BUTTON_SKINSELECT:
			changeState(MainStateType.SKINCONFIG);
			break;
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
		case BUTTON_JUDGE_TIMING:
			execute(arg1 >= 0 ? MusicSelectCommand.JUDGETIMING_UP : MusicSelectCommand.JUDGETIMING_DOWN);
			break;			
		case BUTTON_GAUGEAUTOSHIFT:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_GAUGEAUTOSHIFT : MusicSelectCommand.PREV_GAUGEAUTOSHIFT);
			break;
		case BUTTON_RIVAL:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_RIVAL : MusicSelectCommand.PREV_RIVAL);
			break;
		case BUTTON_FAVORITTE_SONG:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_SONG : MusicSelectCommand.PREV_FAVORITE_SONG);
			break;
		case BUTTON_FAVORITTE_CHART:
			execute(arg1 >= 0 ? MusicSelectCommand.NEXT_FAVORITE_CHART : MusicSelectCommand.PREV_FAVORITE_CHART);
			break;
		case BUTTON_OPEN_IR_WEBSITE:
			execute(MusicSelectCommand.OPEN_RANKING_ON_IR);
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

		final Bar current = bar.getSelected();
		if(main.getIRStatus().length > 0) {
			if(current instanceof SongBar && ((SongBar) current).existsSong()) {
				currentir = ircache.get(((SongBar) current).getSongData(), config.getLnmode());
				currentRankingDuration = (currentir != null ? Math.max(rankingReloadDuration - (System.currentTimeMillis() - currentir.getLastUpdateTime()) ,0) : 0) + rankingDuration;
			} else if(current instanceof GradeBar && ((GradeBar) current).existsAllSongs()) {
				currentir = ircache.get(((GradeBar) current).getCourseData(), config.getLnmode());
				currentRankingDuration = (currentir != null ? Math.max(rankingReloadDuration - (System.currentTimeMillis() - currentir.getLastUpdateTime()) ,0) : 0) + rankingDuration;
			} else {
				currentir = null;
				currentRankingDuration = -1;			
			}
		} else {
			currentir = null;
			currentRankingDuration = -1;			
		}
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
		} else {
			pc = main.getPlayerConfig().getPlayConfig(config.getMode()).getPlayconfig();
		}
		return pc;
	}
	
	public RankingData getCurrentRankingData() {
		return currentir;
	}
	
	public long getCurrentRankingDuration() {
		return currentRankingDuration;
	}
	/**
	 * IRアクセスデータのキャッシュ
	 *
	 * @author exch
	 */
	public static class RankingDataCache {

	    /**
	     * IRアクセスデータのキャッシュ
	     */
	    private ObjectMap<String, RankingData>[] scorecache;
	    private ObjectMap<String, RankingData>[] cscorecache;

	    public RankingDataCache() {
	        scorecache = new ObjectMap[4];
	        cscorecache = new ObjectMap[4];
	        for (int i = 0; i < scorecache.length; i++) {
	            scorecache[i] = new ObjectMap(2000);
	            cscorecache[i] = new ObjectMap(100);
	        }
	    }

	    /**
	     * 指定した楽曲データ、LN MODEに対するIRアクセスデータを返す
	     * @param song 楽曲データ
	     * @param lnmode LN MODE
	     * @return IRアクセスデータ。存在しない場合はnull
	     */
	    public RankingData get(SongData song, int lnmode) {
	        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
	        if (scorecache[cacheindex].containsKey(song.getSha256())) {
	            return scorecache[cacheindex].get(song.getSha256());
	        }
	        return null;
	    }

	    public RankingData get(CourseData course, int lnmode) {
	        int cacheindex = 3;
	        for(SongData song : course.getSong()) {
	        	if(song.hasUndefinedLongNote()) {
	        		cacheindex = lnmode;
	        	}
	        }
	        String hash = createCourseHash(course);
	        if (cscorecache[cacheindex].containsKey(hash)) {
	            return cscorecache[cacheindex].get(hash);
	        }
	        return null;
	    }

	    public void put(SongData song, int lnmode, RankingData iras) {
	        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
	        scorecache[cacheindex].put(song.getSha256(), iras);
	    }
	    
	    public void put(CourseData course, int lnmode, RankingData iras) {
	        int cacheindex = 3;
	        for(SongData song : course.getSong()) {
	        	if(song.hasUndefinedLongNote()) {
	        		cacheindex = lnmode;
	        	}
	        }
	        cscorecache[cacheindex].put(createCourseHash(course), iras);
	    }
	    
		private String createCourseHash(CourseData course) {
			StringBuilder sb = new StringBuilder();
			for(SongData song : course.getSong()) {
				if(song.getSha256() != null && song.getSha256().length() == 64) {
					sb.append(song.getSha256());
				} else {
					return null;
				}
			}
			for(CourseDataConstraint constraint : course.getConstraint()) {
				sb.append(constraint.name);
			}
			try {
				MessageDigest md = MessageDigest.getInstance("sha-256");
				md.update(sb.toString().getBytes());
				return BMSDecoder.convertHexString(md.digest());
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
}
