package bms.player.beatoraja.select;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lr2.*;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends MainState {

	// TODO テキスト表示
	// TODO 譜面情報表示(簡易/詳細表示の切り替え)
	// TODO オプション常時表示(スキン実装で実現？)
	// TODO ターゲットスコア選択の実装

	private int selectedreplay;

	/**
	 * 楽曲DBアクセサ
	 */
	private SongDatabaseAccessor songdb;
	/**
	 * 選択中のモードフィルタ
	 */
	private int mode;
	
	public static final Mode[] MODE = {null, Mode.BEAT_7K, Mode.BEAT_14K, Mode.POPN_9K, Mode.BEAT_5K, Mode.BEAT_10K};
	/**
	 * 選択中のソート
	 */
	private int sort;
	/**
	 * 保存可能な最大リプレイ数
	 */
	public static final int REPLAY = 4;

	private Config config;

	private PlayerData playerdata;

	private PreviewMusicProcessor preview;

	private BitmapFont titlefont;

	private TextureRegion banner;
	private Bar bannerbar;
	/**
	 * 楽曲バー描画用
	 */
	private BarRenderer bar;
	private MusicSelectInputProcessor musicinput;

	private SearchTextField search;

	private boolean songUpdated = false;
	private Thread updateSong;

	/**
	 * 楽曲が選択されてからbmsを読み込むまでの時間(ms)
	 */
	private final int notesGraphDuration = 1000;
	/**
	 * 楽曲が選択されてからプレビュー曲を再生するまでの時間(ms)
	 */
	private final int previewDuration = 400;
	private boolean showNoteGraph = false;

	private ScoreDataCache scorecache;

	public static final int KEY_PLAY = 1;
	public static final int KEY_AUTO = 2;
	public static final int KEY_REPLAY = 3;
	public static final int KEY_UP = 4;
	public static final int KEY_DOWN = 5;
	public static final int KEY_FOLDER_OPEN = 6;
	public static final int KEY_FOLDER_CLOSE = 7;
	public static final int KEY_PRACTICE = 8;

	private int panelstate;

	public static final int SOUND_BGM = 0;
	public static final int SOUND_SCRATCH = 1;
	public static final int SOUND_FOLDEROPEN = 2;
	public static final int SOUND_FOLDERCLOSE = 3;
	public static final int SOUND_CHANGEOPTION = 4;
	
	public MusicSelector(MainController main, Config config, boolean songUpdated) {
		super(main);
		this.config = config;
		this.songUpdated = songUpdated;

		songdb = main.getSongDatabase();

		scorecache = new ScoreDataCache(getMainController().getPlayDataAccessor());

		setSound(SOUND_BGM, config.getBgmpath() + File.separatorChar + "select.wav", true);		
		setSound(SOUND_SCRATCH, config.getSoundpath() + File.separatorChar + "scratch.wav", false);
		setSound(SOUND_FOLDEROPEN, config.getSoundpath() + File.separatorChar + "f-open.wav", false);
		setSound(SOUND_FOLDERCLOSE, config.getSoundpath() + File.separatorChar + "f-close.wav", false);
		setSound(SOUND_CHANGEOPTION, config.getSoundpath() + File.separatorChar + "o-change.wav", false);

		bar = new BarRenderer(this);
		musicinput = new MusicSelectInputProcessor(this);
	}

	public ScoreDataCache getScoreDataCache() {
		return scorecache;
	}

	public void create() {
		play = -1;
		final MainController main = getMainController();
		playerdata = main.getPlayDataAccessor().readPlayerData();
		scorecache.clear();

        preview = new PreviewMusicProcessor(main.getAudioProcessor(), config);
        preview.setDefault(getSound(SOUND_BGM));
        preview.start(null);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayConfig pc = (config.getMusicselectinput() == 0 ? config.getMode7() : (config.getMusicselectinput() == 1 ? config.getMode9() : config.getMode14()));
		input.setKeyassign(pc.getKeyassign());
		input.setControllerConfig(pc.getController());
		bar.updateBar();

		if (getSkin() == null) {
			try {
				SkinConfig sc = config.getSkin()[5];
				if (sc.getPath().endsWith(".json")) {
					SkinLoader sl = new SkinLoader(getMainController().getPlayerResource().getConfig());
					setSkin(sl.loadSelectSkin(Paths.get(sc.getPath()), sc.getProperty()));
				} else {
					LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
					SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
					LR2SelectSkinLoader dloader = new LR2SelectSkinLoader(header.getResolution(), getMainController().getPlayerResource().getConfig());
					setSkin(dloader.loadSelectSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
							sc.getProperty()));
				}
			} catch (Throwable e) {
				e.printStackTrace();
				SkinLoader sl = new SkinLoader(
						getMainController().getPlayerResource().getConfig());
				setSkin(sl.loadSelectSkin(Paths.get(SkinConfig.DEFAULT_SELECT), new HashMap()));
			}
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
		generator.dispose();

		// search text field
		if (getStage() == null && ((MusicSelectSkin) getSkin()).getSearchTextRegion() != null) {
			search = new SearchTextField(this, config.getResolution());
			setStage(search);
		}

		if(!songUpdated && config.isUpdatesong()) {
			updateSong = new SongUpdateThread(null);
			updateSong.start();
		}
	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final PlayerResource resource = main.getPlayerResource();
		final Bar current = bar.getSelected();
		
		// draw song information
		sprite.begin();
		if (current instanceof SongBar) {
			resource.setSongdata(((SongBar) current).getSongData());
		} else {
			resource.setSongdata(null);
		}
		// 段位用の表示(ミラー段位、EX段位)
		if (current instanceof GradeBar) {
			GradeBar gb = (GradeBar) current;

			float dw = (float) getSkin().getScaleX();
			float dh = (float) getSkin().getScaleY();
			for (CourseData.CourseDataConstraint con : gb.getConstraint()) {
				switch (con) {
                    case CLASS:
					break;
                    case MIRROR:
					titlefont.setColor(Color.CYAN);
					titlefont.draw(sprite, "MIRROR OK", 150 * dw, 620 * dh);
					break;
                    case RANDOM:
					titlefont.setColor(Color.CORAL);
					titlefont.draw(sprite, "RANDOM OK", 150 * dw, 620 * dh);
					break;
                    case NO_SPEED:
					titlefont.setColor(Color.RED);
					titlefont.draw(sprite, "x1.0 HI SPEED", 300 * dw, 620 * dh);
					break;
				    case NO_GOOD:
					titlefont.setColor(Color.PURPLE);
					titlefont.draw(sprite, "NO GOOD", 450 * dw, 620 * dh);
					break;
				    case NO_GREAT:
					titlefont.setColor(Color.PURPLE);
					titlefont.draw(sprite, "NO GREAT", 450 * dw, 620 * dh);
					break;
				}
			}

			if (gb.getMirrorScore() != null) {
				IRScoreData score = gb.getMirrorScore();
				// titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				// titlefont.draw(sprite, CLEAR[score.getClear()], 100, 270);
				// titlefont.setColor(Color.WHITE);
				// titlefont.draw(sprite, "EX-SCORE : " + score.getExscore() + "
				// / " + (score.getNotes() * 2), 100, 240);
				// titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(),
				// 100, 210);
				// titlefont.draw(sprite, "CLEAR / PLAY : " +
				// score.getClearcount() + " / " + score.getPlaycount(), 100,
				// 180);
			}
			if (gb.getRandomScore() != null) {
				IRScoreData score = gb.getRandomScore();
				// titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				// titlefont.draw(sprite, CLEAR[score.getClear()], 100, 130);
				// titlefont.setColor(Color.WHITE);
				// titlefont.draw(sprite, "EX-SCORE : " + score.getExscore() +
				// " / " + (score.getNotes() * 2), 100, 240);
				// titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(),
				// 100, 210);
				// titlefont.draw(sprite, "CLEAR / PLAY : " +
				// score.getClearcount() + " / " + score.getPlaycount(), 100,
				// 180);
			}
		}

		// banner
		if (current != bannerbar) {
			bannerbar = current;
			if (banner != null) {
				banner.getTexture().dispose();
				banner = null;
			}
			if (bannerbar instanceof SongBar && ((SongBar) bannerbar).getBanner() != null) {
				banner = new TextureRegion(new Texture(((SongBar) bannerbar).getBanner()));
			}
		}
		sprite.end();
		// preview music
		if(current instanceof SongBar) {
			final SongData song = main.getPlayerResource().getSongdata();
			if(song != preview.getSongData() && getNowTime() > getTimer()[TIMER_SONGBAR_CHANGE] + previewDuration && play < 0) {
				this.preview.start(song);
			}
		}

		// read bms information
		if(getNowTime() > getTimer()[TIMER_SONGBAR_CHANGE] + notesGraphDuration && !showNoteGraph && play < 0) {
			if(current instanceof SongBar) {
				SongData song = main.getPlayerResource().getSongdata();
				Thread thread = new Thread() {
					public void run() {
						song.setBMSModel(resource.loadBMSModel(Paths.get(((SongBar) current).getSongData().getPath()), config.getLnmode()));
					}
				};
				thread.start();
			}
			showNoteGraph = true;
		}

		if (play >= 0) {
			if (current instanceof SongBar) {
				resource.clear();
				if (resource.setBMSFile(Paths.get(((SongBar) current).getSongData().getPath()), config, play)) {
				    preview.stop();
					getMainController().changeState(MainController.STATE_DECIDE);
				}
			} else if (current instanceof GradeBar) {
				if (play == 2) {
					play = 0;
				}
				readCourse(play);
			}
			play = -1;
		} else if (play == -255) {
			getMainController().exit();
		}
	}

	private int play = -1;

	public void input() {
		final BMSPlayerInputProcessor input = getMainController().getInputProcessor();
		final PlayerResource resource = getMainController().getPlayerResource();
		final Bar current = bar.getSelected();
		final int nowtime = getNowTime();

		boolean[] numberstate = input.getNumberState();
		long[] numtime = input.getNumberTime();

		if (input.getNumberState()[6]) {
			preview.stop();
			getMainController().changeState(MainController.STATE_CONFIG);
		}

		musicinput.input();
	}

	public void select(Bar current) {
		if (current instanceof DirectoryBar) {
			if (bar.updateBar(current)) {
				play(SOUND_FOLDEROPEN);
			}
			resetReplayIndex();
		} else {
			play = 0;
		}
	}

	public void resetReplayIndex() {
		if (bar.getSelected() instanceof SelectableBar) {
			boolean[] replays = ((SelectableBar) bar.getSelected()).getExistsReplayData();
			for (int i = 0; i < replays.length; i++) {
				if (replays[i]) {
					selectedreplay = i;
					return;
				}
			}
		}
		selectedreplay = -1;
	}

	public void changeReplayIndex() {
		Bar current = bar.getSelected();
		if (current != null && current instanceof SelectableBar) {
			boolean[] replays = ((SelectableBar) current).getExistsReplayData();
			for (int i = 1; i < replays.length; i++) {
				if (replays[(i + selectedreplay) % replays.length]) {
					selectedreplay = (i + selectedreplay) % replays.length;
					break;
				}
			}
		}
	}

	private void readCourse(int autoplay) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (((GradeBar) bar.getSelected()).existsAllSongs()) {
			resource.clear();
			List<Path> files = new ArrayList<Path>();
			for (SongData song : ((GradeBar) bar.getSelected()).getSongDatas()) {
				files.add(Paths.get(song.getPath()));
			}
			if (resource.setCourseBMSFiles(files.toArray(new Path[files.size()]))) {
				for (CourseData.CourseDataConstraint constraint : ((GradeBar) bar.getSelected()).getConstraint()) {
					switch (constraint) {
                        case CLASS:
						if (autoplay < 2) {
							config.setRandom(0);
							config.setRandom2(0);
							config.setDoubleoption(0);
						}
						break;
                        case MIRROR:
						if (autoplay < 2) {
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
						if (autoplay < 2) {
							if (config.getRandom() > 5) {
								config.setRandom(0);
							}
							if (config.getRandom2() > 5) {
								config.setRandom2(0);
							}
						}
						break;
                        case NO_SPEED:
						resource.addConstraint(constraint);
						break;
                        case NO_GOOD:
						resource.addConstraint(constraint);
						break;
					    case NO_GREAT:
						resource.addConstraint(constraint);
						break;
					}
				}
				preview.stop();
				resource.setCoursetitle(((GradeBar) bar.getSelected()).getTitle());
				resource.setBMSFile(files.get(0), config, autoplay);
				getMainController().changeState(MainController.STATE_DECIDE);
			} else {
				Logger.getGlobal().info("段位の楽曲が揃っていません");
			}
		} else {
			Logger.getGlobal().info("段位の楽曲が揃っていません");
		}
	}

	boolean isPressed(boolean[] keystate, long[] keytime, int code, boolean resetState) {
		int[][] keyassign = MusicSelectInputProcessor.keyassign[config.getMusicselectinput()];
		for (int i = 0; i < keyassign.length; i++) {
			for (int index : keyassign[i]) {
				if (code == index && keystate[i]) {
					if (resetState) {
						if (keytime[i] != 0) {
							keytime[i] = 0;
							return true;
						}
						return false;
					} else {
						return true;
					}

				}
			}
		}
		return false;
	}

	public Mode getMode() {
		return MODE[mode];
	}

	public void setMode(Mode mode) {
		for(int i = 0;i < MODE.length;i++) {
			if(mode == MODE[i]) {
				this.mode = i;
				break;
			}
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
		if( this.panelstate != panelstate) {
			if (this.panelstate != 0) {
				getTimer()[TIMER_PANEL1_OFF + this.panelstate - 1] = getNowTime();
				getTimer()[TIMER_PANEL1_ON + this.panelstate - 1] = Long.MIN_VALUE;
			}
			if (panelstate != 0) {
				getTimer()[TIMER_PANEL1_ON + panelstate - 1] = getNowTime();
				getTimer()[TIMER_PANEL1_OFF + panelstate - 1] = Long.MIN_VALUE;
			}
		}
		this.panelstate = panelstate;
	}

	public int getNumberValue(int id) {
		switch (id) {
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
		case NUMBER_CLEAR:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getClear() : Integer.MIN_VALUE;
		case NUMBER_SCORE:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getExscore() : Integer.MIN_VALUE;
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
			if (bar.getSelected() instanceof SongBar) {
				SongBar song = (SongBar) bar.getSelected();
				PlayConfig pc = (song.getSongData().getMode() == 5 || song.getSongData().getMode() == 7
						? config.getMode7()
						: (song.getSongData().getMode() == 10 || song.getSongData().getMode() == 14 ? config.getMode14()
								: config.getMode9()));
				return pc.getDuration();
			}
			return config.getMode7().getDuration();
		case NUMBER_JUDGETIMING:
			return config.getJudgetiming();
		case BUTTON_MODE:
		mode = config.getModeSort();
			final int[] mode_lr2 = { 0, 2, 4, 5, 1, 3 };
			return mode < mode_lr2.length ? mode_lr2[mode] : mode;
		case BUTTON_SORT:
			return sort;
		case BUTTON_LNMODE:
			return config.getLnmode();
		case NUMBER_SCORE_RATE:
			return bar.getSelected().getScore() != null ? getScoreDataProperty().getRateInt() : Integer.MIN_VALUE;
		case NUMBER_SCORE_RATE_AFTERDOT:
			return bar.getSelected().getScore() != null ? getScoreDataProperty().getNowRateAfterDot() : Integer.MIN_VALUE;
		}
		return super.getNumberValue(id);
	}

	public String getTextValue(int id) {
		switch (id) {
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
			StringBuffer str = new StringBuffer();
			for (Bar b : bar.getDirectory()) {
				str.append(b.getTitle() + " > ");
			}
			return str.toString();
		}
		return super.getTextValue(id);
	}

	private String getCourseTitle(int index) {
		if (bar.getSelected() instanceof GradeBar) {
			if (((GradeBar) bar.getSelected()).getSongDatas().length > index) {
				SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[index];
				return song != null ? song.getTitle() : "-----";
			}
		}
		return "";
	}

	SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	@Override
	public float getSliderValue(int id) {
		switch (id) {
		case SLIDER_MUSICSELECT_POSITION:
			return bar.getSelectedPosition();
		case BARGRAPH_RATE_PGREAT:
			if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
				return score != null ? ((float) (score.getEpg() + score.getLpg()))
						/ ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
			}
			return 0;
		case BARGRAPH_LEVEL:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_BEGINNER:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 1) {
					return 0;
				}
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_NORMAL:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 2) {
					return 0;
				}
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_HYPER:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 3) {
					return 0;
				}
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_ANOTHER:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 4) {
					return 0;
				}
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_INSANE:
			if (bar.getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 5) {
					return 0;
				}
				if (sd.getMode() == 5 || sd.getMode() == 10) {
					return sd.getLevel() / 9.0f;
				}
				if (sd.getMode() == 7 || sd.getMode() == 14) {
					return sd.getLevel() / 12.0f;
				}
				if (sd.getMode() == 9) {
					return sd.getLevel() / 50.0f;
				}
			}
			return 0;
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
		return 0;
	}

	public void setSliderValue(int id, float value) {
		switch (id) {
		case SLIDER_MUSICSELECT_POSITION:
			bar.setSelectedPosition(value);
		}
	}

	public TextureRegion getImage(int imageid) {
		if (imageid == IMAGE_BANNER) {
			return banner;
		}
		return super.getImage(imageid);
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
		}
		return super.getBooleanValue(id);
	}

	public void executeClickEvent(int id) {
		switch (id) {
		case BUTTON_PLAY:
			getMainController().getPlayerResource().setPlayDevice(getMainController().getInputProcessor().getLastKeyChangedDevice());
			play = 0;
			break;
		case BUTTON_AUTOPLAY:
			play = 1;
			break;
		case BUTTON_PRACTICE:
			getMainController().getPlayerResource().setPlayDevice(getMainController().getInputProcessor().getLastKeyChangedDevice());
			play = 2;
			break;
		case BUTTON_REPLAY:
			play = 3;
			break;
		case BUTTON_REPLAY2:
			play = 4;
			break;
		case BUTTON_REPLAY3:
			play = 5;
			break;
		case BUTTON_REPLAY4:
			play = 6;
			break;
		}
	}

	public Bar getSelectedBar() {
		return bar.getSelected();
	}

	public BarRenderer getBarRender() {
		return bar;
	}

	public void updateSong(Bar selected) {
		if(updateSong == null || !updateSong.isAlive()) {
			updateSong = new MusicSelector.SongUpdateThread(selected);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("楽曲更新中のため、更新要求は取り消されました");
		}
	}

	public void selectedBarMoved() {
		resetReplayIndex();
		getTimer()[TIMER_SONGBAR_CHANGE] = getNowTime();
		preview.start(null);
		showNoteGraph = false;
	}

	public void selectSong(int mode) {
		if(mode < 3) {
			play = mode;
		} else {
			play = (selectedreplay >= 0) ? 3 + selectedreplay : 0;
		}
	}

	/**
	 * 楽曲データベース更新用スレッド
	 *
	 * @author exch
	 */
	class SongUpdateThread extends Thread {

		private final Bar selected;

		public SongUpdateThread(Bar bar) {
			selected = bar;
		}

		public void run() {
			if (selected == null) {
				getSongDatabase().updateSongDatas(null, false);
			} else if (selected instanceof FolderBar) {
				FolderBar fb = (FolderBar) selected;
				getSongDatabase().updateSongDatas(fb.getFolderData().getPath(), false);
			} else if (selected instanceof TableBar) {
				TableBar tb = (TableBar) selected;
				if (tb.getUrl() != null && tb.getUrl().length() > 0) {
					TableDataAccessor tda = new TableDataAccessor();
					String[] url = new String[] { tb.getUrl() };
					tda.updateTableData(url);
					TableData td = tda.read(tb.getTitle());
					if (td != null) {
						tb.setTableData(td);
					}
				}
			}
		}
	}
}
