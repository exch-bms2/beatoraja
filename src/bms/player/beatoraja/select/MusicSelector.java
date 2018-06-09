package bms.player.beatoraja.select;

import static bms.player.beatoraja.skin.SkinProperty.*;
import static bms.player.beatoraja.ClearType.*;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.IRScoreData.SongTrophy;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.keyData;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.SkinType;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

/**
 * �겦�쎊�깿�늽�� 璵썸쎊訝�誤㎯겏�궖�꺖�궫�꺂�걣�뙁�걲璵썸쎊�겗�궧�깇�꺖�궭�궧�굮烏①ㅊ�걮�곲겦�뒢�걮�걼璵썸쎊�굮 �쎊黎뷴츣�깿�늽�겓歷▲걲��
 *
 * @author exch
 */
public class MusicSelector extends MainState {

	// TODO �깇�궘�궧�깉烏①ㅊ
	// TODO���깱�꺀�꺖�꺀�꺍���깲餘듕퐤�겗�궧�궠�궋烏①ㅊ

	private int selectedreplay;

	/**
	 * 璵썸쎊DB�궋�궚�궩�궢
	 */
	private SongDatabaseAccessor songdb;

	public static final Mode[] MODE = { null, Mode.BEAT_7K, Mode.BEAT_14K, Mode.POPN_9K, Mode.BEAT_5K, Mode.BEAT_10K, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE };
	/**
	 * �겦�뒢訝��겗�궫�꺖�깉
	 */
	private int sort;
	/**
	 * 岳앭춼�룾�꺗�겒��鸚㎯꺁�깤�꺃�궎�빊
	 */
	public static final int REPLAY = 4;

	private PlayerConfig config;

	private PlayerData playerdata;
	/**
	 * 璵썸쎊�깤�꺃�깛�깷�꺖�눇�릤
	 */
	private PreviewMusicProcessor preview;

	private BitmapFont titlefont;

	/**
	 * 璵썸쎊�깘�꺖�룒�뵽�뵪
	 */
	private BarRenderer bar;
	private MusicSelectInputProcessor musicinput;

	private SearchTextField search;

	/**
	 * 璵썸쎊�걣�겦�뒢�걬�굦�겍�걢�굢bms�굮沃��겳渦쇈��겲�겎�겗�셽�뼋(ms)
	 */
	private final int notesGraphDuration = 350;
	/**
	 * 璵썸쎊�걣�겦�뒢�걬�굦�겍�걢�굢�깤�꺃�깛�깷�꺖�쎊�굮�냽�뵟�걲�굥�겲�겎�겗�셽�뼋(ms)
	 */
	private final int previewDuration = 400;
	private boolean showNoteGraph = false;

	private ScoreDataCache scorecache;
	private ScoreDataCache rivalcache;

	private Map<PlayerInformation, ScoreDataCache> rivalcaches = new HashMap<PlayerInformation, ScoreDataCache>();
	private PlayerInformation rival;

	private int panelstate;

	public static final int SOUND_BGM = 0;
	public static final int SOUND_SCRATCH = 1;
	public static final int SOUND_FOLDEROPEN = 2;
	public static final int SOUND_FOLDERCLOSE = 3;
	public static final int SOUND_CHANGEOPTION = 4;

	private PlayMode play = null;

	private PixmapResourcePool banners;

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

		try {
			// �꺀�궎�깘�꺂�궧�궠�궋�깈�꺖�궭�깧�꺖�궧鵝쒏닇
			// TODO �닪�겗�궚�꺀�궧�겓燁삣땿
			if(!Files.exists(Paths.get("rival"))) {
				Files.createDirectory(Paths.get("rival"));
			}
			if(main.getIRConnection() != null) {
				IRResponse<PlayerInformation[]> response = main.getIRConnection().getRivals();
				if(response.isSuccessed()) {
					for(PlayerInformation rival : response.getData()) {
						new Thread(() -> {
							try {
								final ScoreDatabaseAccessor scoredb = new ScoreDatabaseAccessor("rival/" + rival.getId() + ".db");
								scoredb.createTable();
								scoredb.setInformation(rival);
								IRResponse<IRScoreData[]> scores = main.getIRConnection().getPlayData(rival.getId(), null);
								if(scores.isSuccessed()) {
									scoredb.setScoreData(scores.getData());
									Logger.getGlobal().info("IR�걢�굢�겗�궧�궠�궋�룚孃쀥츑雅� : " + rival.getName());
								} else {
									Logger.getGlobal().warning("IR�걢�굢�겗�궧�궠�궋�룚孃쀥ㅁ�븮 : " + scores.getMessage());
								}
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							};
						}).start();
					}
				} else {
					Logger.getGlobal().warning("IR�걢�굢�겗�꺀�궎�깘�꺂�룚孃쀥ㅁ�븮 : " + response.getMessage());
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		// �꺀�궎�깘�꺂�궘�깵�긿�궥�깷鵝쒏닇
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("rival"))) {
			for (Path p : paths) {
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
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		bar = new BarRenderer(this);
		banners = new PixmapResourcePool(main.getConfig().getBannerPixmapGen());
		musicinput = new MusicSelectInputProcessor(this);

		if (!songUpdated && main.getPlayerResource().getConfig().isUpdatesong()) {
			main.updateSong(null);
		}
	}

	public void setRival(PlayerInformation rival) {
		this.rival = rival;
		rivalcache = rivalcaches.get(rival);
		bar.updateBar();

		Logger.getGlobal().info("Rival鸚됪쎍:" + (rival != null ? rival.getName() : "�겒�걮"));
	}

	public PlayerInformation getRival() {
		return rival;
	}

	public Set<PlayerInformation> getRivals() {
		return rivalcaches.keySet();
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
		setSound(SOUND_CHANGEOPTION, "o-change.wav", SoundType.SOUND,false);

		play = null;
		showNoteGraph = false;
		playerdata = main.getPlayDataAccessor().readPlayerData();
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

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
		generator.dispose();

		// search text field
		if (getStage() == null && ((MusicSelectSkin) getSkin()).getSearchTextRegion() != null) {
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
						final Deque<DirectoryBar> dir = this.getBarRender().getDirectory();
						for(DirectoryBar bar: dir){
							if(bar instanceof TableBar){
								resource.setTablename(bar.getTitle());
							}
							if(bar instanceof HashBar){
								resource.setTablelevel(bar.getTitle());
								break;
							}
						}
						preview.stop();
						main.changeState(MainController.STATE_DECIDE);
						banners.disposeOld();
					}
				} else {
	                execute(MusicSelectCommand.OPEN_DOWNLOAD_SITE);
				}
			} else if (current instanceof GradeBar) {
				if (play == PlayMode.PRACTICE) {
					play = PlayMode.PLAY;
				}
				readCourse(play);
			} else if (current instanceof DirectoryBar) {
				if(play.isAutoPlayMode()) {
					Array<Path> paths = new Array();
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
							main.changeState(MainController.STATE_DECIDE);
							banners.disposeOld();
						}
					}
				}
			}
			play = null;
		}
	}

	public void input() {
		final BMSPlayerInputProcessor input = main.getInputProcessor();

		if (keyData.getNumberState(6)) {
			preview.stop();
			main.changeState(MainController.STATE_CONFIG);
		} else if (keyData.getFunctionstate(11)) {
			preview.stop();
			main.changeState(MainController.STATE_SKIN_SELECT);
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
		if (course.existsAllSongs()) {
			resource.clear();
			final SongData[] songs = course.getSongDatas();
			Path[] files = new Path[songs.length];
			int i = 0;
			for (SongData song : songs) {
				files[i++] = Paths.get(song.getPath());
			}
			if (resource.setCourseBMSFiles(files)) {
				for (CourseData.CourseDataConstraint constraint : course.getConstraint()) {
					switch (constraint) {
					case CLASS:
						if (mode == PlayMode.PLAY || mode.isAutoPlayMode()) {
							config.setRandom(0);
							config.setRandom2(0);
							config.setDoubleoption(0);
						}
						break;
					case MIRROR:
						if (mode == PlayMode.PLAY || mode.isAutoPlayMode()) {
							if (config.getRandom() == 1) {
								config.setRandom2(1);
								config.setDoubleoption(1);
							} else {
								config.setRandom(0);
								config.setRandom2(0);
								config.setDoubleoption(0);
							}
						}
						break;
					case RANDOM:
						if (mode == PlayMode.PLAY || mode.isAutoPlayMode()) {
							if (config.getRandom() > 5) {
								config.setRandom(0);
							}
							if (config.getRandom2() > 5) {
								config.setRandom2(0);
							}
						}
						break;
					case NO_SPEED:
						case NO_GOOD:
						case NO_GREAT:
						case GAUGE_LR2:
						resource.addConstraint(constraint);
						break;
					}
				}
				preview.stop();
				resource.setCoursetitle(bar.getSelected().getTitle());
				resource.setBMSFile(files[0], mode);
				main.changeState(MainController.STATE_DECIDE);
				banners.disposeOld();
			} else {
				Logger.getGlobal().info("餘듕퐤�겗璵썸쎊�걣�룂�겂�겍�걚�겲�걵�굯");
			}
		} else {
			Logger.getGlobal().info("餘듕퐤�겗璵썸쎊�걣�룂�겂�겍�걚�겲�걵�굯");
		}
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

	public void dispose() {
		super.dispose();
		bar.dispose();
		banners.dispose();
		if (titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
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

	public int getNumberValue(int id) {
		switch (id) {
		case NUMBER_TOTALPLAYTIME_HOUR:
			return (int) playerdata.getPlaytime() / 3600;
		case NUMBER_TOTALPLAYTIME_MINUTE:
			return (int) (playerdata.getPlaytime() / 60) % 60;
		case NUMBER_TOTALPLAYTIME_SECOND:
			return (int) (playerdata.getPlaytime() % 60);
		case NUMBER_TOTALPLAYCOUNT:
			return (int) playerdata.getPlaycount();
		case NUMBER_TOTALCLEARCOUNT:
			return (int) playerdata.getClear();
		case NUMBER_TOTALFAILCOUNT:
			return (int) ((int) playerdata.getPlaycount() - playerdata.getClear());
		case NUMBER_TOTALPERFECT:
			return (int) (playerdata.getEpg() + playerdata.getLpg());
		case NUMBER_TOTALGREAT:
			return (int) (playerdata.getEgr() + playerdata.getLgr());
		case NUMBER_TOTALGOOD:
			return (int) (playerdata.getEgd() + playerdata.getLgd());
		case NUMBER_TOTALBAD:
			return (int) (playerdata.getEbd() + playerdata.getLbd());
		case NUMBER_TOTALPOOR:
			return (int) (playerdata.getEpr() + playerdata.getLpr());
		case NUMBER_TOTALPLAYNOTES:
			return (int) (playerdata.getEpg() + playerdata.getLpg()) + (int) (playerdata.getEgr() + playerdata.getLgr())
					+ (int) (playerdata.getEgd() + playerdata.getLgd())
					+ (int) (playerdata.getEbd() + playerdata.getLbd());
		case NUMBER_PLAYCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getPlaycount()
					: Integer.MIN_VALUE;
		case NUMBER_CLEARCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getClearcount()
					: Integer.MIN_VALUE;
		case NUMBER_FAILCOUNT:
			return bar.getSelected().getScore() != null
					? bar.getSelected().getScore().getPlaycount() - bar.getSelected().getScore().getClearcount()
					: Integer.MIN_VALUE;
		case NUMBER_MISSCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getMinbp() : Integer.MIN_VALUE;
		case NUMBER_MAXCOMBO:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getCombo() : Integer.MIN_VALUE;
		case NUMBER_FOLDER_TOTALSONGS:
			if (bar.getSelected() instanceof DirectoryBar) {
				int[] lamps = ((DirectoryBar) bar.getSelected()).getLamps();
				int count = 0;
				for (int lamp : lamps) {
					count += lamp;
				}
				return count;
			}
			return Integer.MIN_VALUE;
		case NUMBER_DURATION:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).existsSong()) {
				SongBar song = (SongBar) bar.getSelected();
				PlayConfig pc = config.getPlayConfig(song.getSongData().getMode()).getPlayconfig();
				return pc.getDuration();
			}
			return config.getMode7().getPlayconfig().getDuration();
		case NUMBER_DURATION_GREEN:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).existsSong()) {
				SongBar song = (SongBar) bar.getSelected();
				PlayConfig pc = config.getPlayConfig(song.getSongData().getMode()).getPlayconfig();
				return pc.getDuration() * 3 / 5;
			}
			return config.getMode7().getPlayconfig().getDuration() * 3 / 5;
		case NUMBER_JUDGETIMING:
			return config.getJudgetiming();
		}
		return super.getNumberValue(id);
	}

	public String getTextValue(int id) {
		switch (id) {
			case STRING_RIVAL:
				return rival != null ? rival.getName() : "";
		case STRING_TITLE:
		case STRING_FULLTITLE:
			if (bar.getSelected() instanceof DirectoryBar) {
				return bar.getSelected().getTitle();
			}
			break;
		case STRING_COURSE1_TITLE:
			return getCourseTitle(0);
		case STRING_COURSE2_TITLE:
			return getCourseTitle(1);
		case STRING_COURSE3_TITLE:
			return getCourseTitle(2);
		case STRING_COURSE4_TITLE:
			return getCourseTitle(3);
		case STRING_COURSE5_TITLE:
			return getCourseTitle(4);
		case STRING_COURSE6_TITLE:
			return getCourseTitle(5);
		case STRING_DIRECTORY:
			return bar.getDirectoryString();
		}
		return super.getTextValue(id);
	}

	private String getCourseTitle(int index) {
		if (bar.getSelected() instanceof GradeBar) {
			if (((GradeBar) bar.getSelected()).getSongDatas().length > index) {
				SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[index];
				final String songname = song != null && song.getTitle() != null ? song.getTitle() : "----";
				return song != null && song.getPath() != null ? songname : "(no song) " + songname;
			}
		}
		return "";
	}

	public SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	@Override
	public float getSliderValue(int id) {
		switch (id) {
		case SLIDER_MUSICSELECT_POSITION:
			return bar.getSelectedPosition();
			case SLIDER_MASTER_VOLUME:
				return main.getConfig().getSystemvolume();
			case SLIDER_KEY_VOLUME:
				return main.getConfig().getKeyvolume();
			case SLIDER_BGM_VOLUME:
				return main.getConfig().getBgvolume();
			case BARGRAPH_RATE_PGREAT:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEpg() + score.getLpg()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_LEVEL:
			return getLevelRate(-1);
		case BARGRAPH_LEVEL_BEGINNER:
			return getLevelRate(1);
		case BARGRAPH_LEVEL_NORMAL:
			return getLevelRate(2);
		case BARGRAPH_LEVEL_HYPER:
			return getLevelRate(3);
		case BARGRAPH_LEVEL_ANOTHER:
			return getLevelRate(4);
		case BARGRAPH_LEVEL_INSANE:
			return getLevelRate(5);
		case BARGRAPH_RATE_GREAT:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEgr() + score.getLgr()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_RATE_GOOD:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEgd() + score.getLgd()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_RATE_BAD:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEbd() + score.getLbd()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_RATE_POOR:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEpr() + score.getLpr()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_RATE_MAXCOMBO:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null
						? ((float) score.getCombo()) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_RATE_EXSCORE:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null
						? ((float) score.getExscore()) / ((SongBar) bar.getSelected()).getSongData().getNotes() / 2 : 0;
			}
			return 0;
		}
		return super.getSliderValue(id);
	}
	
	private float getLevelRate(int difficulty) {
		if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
			SongData sd = ((SongBar) bar.getSelected()).getSongData();
			if (difficulty >= 0 && sd.getDifficulty() != difficulty) {
				return 0;
			}
			int maxLevel = 0;
			switch (sd.getMode()) {
			case 5:
			case 10:
				maxLevel = 9;
			case 7:
			case 14:
				maxLevel = 12;
			case 9:
				maxLevel =  50;
			case 25:
			case 50:
				maxLevel = 10;
			}
			if (maxLevel > 0) {
				return (float)sd.getLevel() / maxLevel;
			}
		}
		return 0;
	}

	public void setSliderValue(int id, float value) {
		switch (id) {
		case SLIDER_MUSICSELECT_POSITION:
			selectedBarMoved();
			bar.setSelectedPosition(value);
			return;
			case SLIDER_MASTER_VOLUME:
				main.getConfig().setSystemvolume(value);
				return;
			case SLIDER_KEY_VOLUME:
				main.getConfig().setKeyvolume(value);
				return;
			case SLIDER_BGM_VOLUME:
				main.getConfig().setBgvolume(value);
				return;
		}
		super.setSliderValue(id, value);
	}

	public boolean getBooleanValue(int id) {
		final Bar current = bar.getSelected();
		switch (id) {
		case OPTION_PANEL1:
			return panelstate == 1;
		case OPTION_PANEL2:
			return panelstate == 2;
		case OPTION_PANEL3:
			return panelstate == 3;
		case OPTION_SONGBAR:
			return current instanceof SongBar;
		case OPTION_FOLDERBAR:
			return current instanceof DirectoryBar;
		case OPTION_GRADEBAR:
			return current instanceof GradeBar;
		case OPTION_PLAYABLEBAR:
			return (current instanceof SongBar)
					|| ((current instanceof GradeBar) && ((GradeBar) current).existsAllSongs());
		case OPTION_REPLAYDATA:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 0
					&& ((SelectableBar) current).getExistsReplayData()[0];
		case OPTION_NO_REPLAYDATA:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 0
					&& !((SelectableBar) current).getExistsReplayData()[0];
		case OPTION_REPLAYDATA2:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 1
					&& ((SelectableBar) current).getExistsReplayData()[1];
		case OPTION_NO_REPLAYDATA2:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 1
					&& !((SelectableBar) current).getExistsReplayData()[1];
		case OPTION_REPLAYDATA3:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 2
					&& ((SelectableBar) current).getExistsReplayData()[2];
		case OPTION_NO_REPLAYDATA3:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 2
					&& !((SelectableBar) current).getExistsReplayData()[2];
		case OPTION_REPLAYDATA4:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 3
					&& ((SelectableBar) current).getExistsReplayData()[3];
		case OPTION_NO_REPLAYDATA4:
			return (current instanceof SelectableBar) && ((SelectableBar) current).getExistsReplayData().length > 3
					&& !((SelectableBar) current).getExistsReplayData()[3];
		case OPTION_SELECT_REPLAYDATA:
			return selectedreplay == 0;
		case OPTION_SELECT_REPLAYDATA2:
			return selectedreplay == 1;
		case OPTION_SELECT_REPLAYDATA3:
			return selectedreplay == 2;
		case OPTION_SELECT_REPLAYDATA4:
			return selectedreplay == 3;
		case OPTION_GRADEBAR_MIRROR:
			return existsConstraint(CourseData.CourseDataConstraint.MIRROR);
		case OPTION_GRADEBAR_RANDOM:
			return existsConstraint(CourseData.CourseDataConstraint.RANDOM);
		case OPTION_GRADEBAR_NOSPEED:
			return existsConstraint(CourseData.CourseDataConstraint.NO_SPEED);
		case OPTION_GRADEBAR_NOGOOD:
			return existsConstraint(CourseData.CourseDataConstraint.NO_GOOD);
		case OPTION_GRADEBAR_NOGREAT:
			return existsConstraint(CourseData.CourseDataConstraint.NO_GREAT);
		case OPTION_NOT_COMPARE_RIVAL:
			return rival == null;
		case OPTION_COMPARE_RIVAL:
			return rival != null;
		case OPTION_SELECT_BAR_NOT_PLAYED:
			return (current instanceof SongBar || current instanceof GradeBar)
					&& (bar.getSelected().getScore() == null || (bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == NoPlay.id));
		case OPTION_SELECT_BAR_FAILED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Failed.id;
		case OPTION_SELECT_BAR_ASSIST_EASY_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == AssistEasy.id;
		case OPTION_SELECT_BAR_LIGHT_ASSIST_EASY_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == LightAssistEasy.id;
		case OPTION_SELECT_BAR_EASY_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Easy.id;
		case OPTION_SELECT_BAR_NORMAL_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Normal.id;
		case OPTION_SELECT_BAR_HARD_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Hard.id;
		case OPTION_SELECT_BAR_EXHARD_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == ExHard.id;
		case OPTION_SELECT_BAR_FULL_COMBO_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == FullCombo.id;
		case OPTION_SELECT_BAR_PERFECT_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Perfect.id;
		case OPTION_SELECT_BAR_MAX_CLEARED:
			return bar.getSelected().getScore() != null && bar.getSelected().getScore().getClear() == Max.id;
		case OPTION_CLEAR_EASY:
			return existsTrophy(SongTrophy.EASY);
		case OPTION_CLEAR_GROOVE:
			return existsTrophy(SongTrophy.GROOVE);
		case OPTION_CLEAR_HARD:
			return existsTrophy(SongTrophy.HARD);
		case OPTION_CLEAR_EXHARD:
			return existsTrophy(SongTrophy.EXHARD);
		case OPTION_CLEAR_NORMAL:
			return existsTrophy(SongTrophy.NORMAL);
		case OPTION_CLEAR_MIRROR:
			return existsTrophy(SongTrophy.MIRROR);
		case OPTION_CLEAR_RANDOM:
			return existsTrophy(SongTrophy.RANDOM);
		case OPTION_CLEAR_RRANDOM:
			return existsTrophy(SongTrophy.R_RANDOM);
		case OPTION_CLEAR_SRANDOM:
			return existsTrophy(SongTrophy.S_RANDOM);
		case OPTION_CLEAR_SPIRAL:
			return existsTrophy(SongTrophy.SPIRAL);
		case OPTION_CLEAR_HRANDOM:
			return existsTrophy(SongTrophy.H_RANDOM);
		case OPTION_CLEAR_ALLSCR:
			return existsTrophy(SongTrophy.ALL_SCR);
		case OPTION_CLEAR_EXRANDOM:
			return existsTrophy(SongTrophy.EX_RANDOM);
		case OPTION_CLEAR_EXSRANDOM:
			return existsTrophy(SongTrophy.EX_S_RANDOM);
		}
		return super.getBooleanValue(id);
	}

	private boolean existsConstraint(CourseData.CourseDataConstraint constraint) {
		if (!(bar.getSelected() instanceof GradeBar)) {
			return false;
		}

		GradeBar gb = (GradeBar) bar.getSelected();
		for (CourseData.CourseDataConstraint con : gb.getConstraint()) {
			if(con == constraint) {
				return true;
			}
		}
		return false;
	}
	
	private boolean existsTrophy(SongTrophy trophy) {
		final IRScoreData score = getScoreDataProperty().getScoreData();
		return score != null && score.getTrophy() != null && score.getTrophy().indexOf(trophy.character) >= 0;
	}

	public int getImageIndex(int id) {
		switch(id) {
			case BUTTON_MODE:
				int mode = 0;
				for (; mode < MODE.length; mode++) {
					if (MODE[mode] == config.getMode()) {
						break;
					}
				}
				final int[] mode_lr2 = { 0, 2, 4, 5, 1, 3 };
				return mode < mode_lr2.length ? mode_lr2[mode] : mode;
			case BUTTON_SORT:
				return sort;
			case NUMBER_CLEAR:
				return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getClear() : Integer.MIN_VALUE;
			case NUMBER_TARGET_CLEAR:
				return bar.getSelected().getRivalScore() != null ? bar.getSelected().getRivalScore().getClear() : Integer.MIN_VALUE;
		}
		return super.getImageIndex(id);
	}

	public void executeClickEvent(int id, int arg) {
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
			execute(arg >= 0 ? MusicSelectCommand.NEXT_MODE : MusicSelectCommand.PREV_MODE);
			break;
		case BUTTON_SORT:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_SORT : MusicSelectCommand.PREV_SORT);
			break;
		case BUTTON_LNMODE:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_LNMODE : MusicSelectCommand.PREV_LNMODE);
			break;
		case BUTTON_RANDOM_1P:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_OPTION_1P : MusicSelectCommand.PREV_OPTION_1P);
			break;
		case BUTTON_RANDOM_2P:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_OPTION_2P : MusicSelectCommand.PREV_OPTION_2P);
			break;
		case BUTTON_DPOPTION:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_OPTION_DP : MusicSelectCommand.PREV_OPTION_DP);
			break;
		case BUTTON_GAUGE_1P:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_GAUGE_1P : MusicSelectCommand.PREV_GAUGE_1P);
			break;
		case BUTTON_HSFIX:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_HSFIX : MusicSelectCommand.PREV_HSFIX);
			break;
		case BUTTON_BGA:
			execute(arg >= 0 ? MusicSelectCommand.NEXT_BGA_SHOW : MusicSelectCommand.PREV_BGA_SHOW);
			break;
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

	public void selectedBarMoved() {
		execute(MusicSelectCommand.RESET_REPLAY);
		// banner
		final Bar current = bar.getSelected();
		main.getPlayerResource().getBMSResource().setBanner(
				current instanceof SongBar ? ((SongBar) current).getBanner() : null);

		main.setTimerOn(TIMER_SONGBAR_CHANGE);
		if(preview.getSongData() != null && (!(bar.getSelected() instanceof SongBar) ||
				((SongBar) bar.getSelected()).getSongData().getFolder().equals(preview.getSongData().getFolder()) == false))
		preview.start(null);
		showNoteGraph = false;
	}

	public void selectSong(PlayMode mode) {
		if (!mode.isReplayMode()) {
			play = mode;
		} else {
			play = (selectedreplay >= 0) ? PlayMode.getReplayMode(selectedreplay) : PlayMode.PLAY;
		}
	}
}
