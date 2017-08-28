package bms.player.beatoraja.select;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
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

	// TODO　ミラーランダム段位のスコア表示

	private int selectedreplay;

	/**
	 * 楽曲DBアクセサ
	 */
	private SongDatabaseAccessor songdb;

	public static final Mode[] MODE = { null, Mode.BEAT_7K, Mode.BEAT_14K, Mode.POPN_9K, Mode.BEAT_5K, Mode.BEAT_10K, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE };
	/**
	 * 選択中のソート
	 */
	private int sort;
	/**
	 * 保存可能な最大リプレイ数
	 */
	public static final int REPLAY = 4;

	private PlayerConfig config;

	private PlayerData playerdata;
	/**
	 * 楽曲プレビュー処理
	 */
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

	private int panelstate;

	public static final int SOUND_BGM = 0;
	public static final int SOUND_SCRATCH = 1;
	public static final int SOUND_FOLDEROPEN = 2;
	public static final int SOUND_FOLDERCLOSE = 3;
	public static final int SOUND_CHANGEOPTION = 4;

	public MusicSelector(MainController main, boolean songUpdated) {
		super(main);
		this.config = main.getPlayerResource().getPlayerConfig();
		final Config conf = main.getPlayerResource().getConfig();

		songdb = main.getSongDatabase();

		scorecache = new ScoreDataCache(getMainController().getPlayDataAccessor());

		setSound(SOUND_BGM, conf.getBgmpath() + File.separatorChar + "select.wav", true);
		setSound(SOUND_SCRATCH, conf.getSoundpath() + File.separatorChar + "scratch.wav", false);
		setSound(SOUND_FOLDEROPEN, conf.getSoundpath() + File.separatorChar + "f-open.wav", false);
		setSound(SOUND_FOLDERCLOSE, conf.getSoundpath() + File.separatorChar + "f-close.wav", false);
		setSound(SOUND_CHANGEOPTION, conf.getSoundpath() + File.separatorChar + "o-change.wav", false);

		bar = new BarRenderer(this);
		musicinput = new MusicSelectInputProcessor(this);

		if (!songUpdated && main.getPlayerResource().getConfig().isUpdatesong()) {
			updateSong = new SongUpdateThread(null);
			updateSong.start();
		}
	}

	public ScoreDataCache getScoreDataCache() {
		return scorecache;
	}

	public void create() {
		play = -1;
		final MainController main = getMainController();
		playerdata = main.getPlayDataAccessor().readPlayerData();
		if (bar.getSelected() != null && bar.getSelected() instanceof SongBar) {
			scorecache.update(((SongBar) bar.getSelected()).getSongData(), config.getLnmode());
		}

		preview = new PreviewMusicProcessor(main.getAudioProcessor(), main.getPlayerResource().getConfig());
		preview.setDefault(getSound(SOUND_BGM));
		preview.start(null);

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayConfig pc = (config.getMusicselectinput() == 0 ? config.getMode7()
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
		if (current instanceof SongBar) {
			final SongData song = main.getPlayerResource().getSongdata();
			if (song != preview.getSongData() && getNowTime() > getTimer()[TIMER_SONGBAR_CHANGE] + previewDuration
					&& play < 0) {
				this.preview.start(song);
			}
		}

		// read bms information
		if (getNowTime() > getTimer()[TIMER_SONGBAR_CHANGE] + notesGraphDuration && !showNoteGraph && play < 0) {
			if (current instanceof SongBar && ((SongBar) current).existsSong()) {
				SongData song = main.getPlayerResource().getSongdata();
				Thread thread = new Thread() {
					public void run() {
						song.setBMSModel(resource.loadBMSModel(Paths.get(((SongBar) current).getSongData().getPath()),
								config.getLnmode()));
					}
				};
				thread.start();
			}
			showNoteGraph = true;
		}

		if (play >= 0) {
			if (current instanceof SongBar) {
				SongData song = ((SongBar) current).getSongData();
				if (((SongBar) current).existsSong()) {
					resource.clear();
					if (resource.setBMSFile(Paths.get(song.getPath()), play)) {
						preview.stop();
						getMainController().changeState(MainController.STATE_DECIDE);
					}
				} else {
					if (song.getUrl() != null) {
						try {
							URI uri = new URI(song.getUrl());
							Desktop.getDesktop().browse(uri);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					if (song.getAppendurl() != null) {
						try {
							URI uri = new URI(song.getAppendurl());
							Desktop.getDesktop().browse(uri);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
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
				resource.setCoursetitle(bar.getSelected().getTitle());
				resource.setBMSFile(files.get(0), autoplay);
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
		if (this.panelstate != panelstate) {
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
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).existsSong()) {
				SongBar song = (SongBar) bar.getSelected();
				PlayConfig pc = config.getPlayConfig(song.getSongData().getMode());
				return pc.getDuration();
			}
			return config.getMode7().getDuration();
		case NUMBER_JUDGETIMING:
			return config.getJudgetiming();
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
		case NUMBER_SCORE_RATE:
			return bar.getSelected().getScore() != null ? getScoreDataProperty().getRateInt() : Integer.MIN_VALUE;
		case NUMBER_SCORE_RATE_AFTERDOT:
			return bar.getSelected().getScore() != null ? getScoreDataProperty().getNowRateAfterDot()
					: Integer.MIN_VALUE;
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
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_BEGINNER:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 1) {
					return 0;
				}
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_NORMAL:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 2) {
					return 0;
				}
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_HYPER:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 3) {
					return 0;
				}
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_ANOTHER:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 4) {
					return 0;
				}
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
				}
			}
			return 0;
		case BARGRAPH_LEVEL_INSANE:
			if (bar.getSelected() instanceof SongBar && ((SongBar) bar.getSelected()).getSongData() != null) {
				SongData sd = ((SongBar) bar.getSelected()).getSongData();
				if (sd.getDifficulty() != 5) {
					return 0;
				}
				int maxLevel = getMaxLevel(sd.getMode());
				if (maxLevel > 0) {
					return (float)sd.getLevel() / maxLevel;
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

	private int getMaxLevel(int modeId) {
		switch (modeId) {
		case 5:
		case 10:
			return 9;
		case 7:
		case 14:
			return 12;
		case 9:
			return 50;
		case 25:
		case 50:
			return 10;
		default:
			return 0;
		}
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
		case OPTION_SELECT_REPLAYDATA:
			return selectedreplay == 0;
		case OPTION_SELECT_REPLAYDATA2:
			return selectedreplay == 1;
		case OPTION_SELECT_REPLAYDATA3:
			return selectedreplay == 2;
		case OPTION_SELECT_REPLAYDATA4:
			return selectedreplay == 3;
			case OPTION_GRADEBAR_MIRROR:
				return existsConstant(CourseData.CourseDataConstraint.MIRROR);
			case OPTION_GRADEBAR_RANDOM:
				return existsConstant(CourseData.CourseDataConstraint.RANDOM);
			case OPTION_GRADEBAR_NOSPEED:
				return existsConstant(CourseData.CourseDataConstraint.NO_SPEED);
			case OPTION_GRADEBAR_NOGOOD:
				return existsConstant(CourseData.CourseDataConstraint.NO_GOOD);
			case OPTION_GRADEBAR_NOGREAT:
				return existsConstant(CourseData.CourseDataConstraint.NO_GREAT);
		}
		return super.getBooleanValue(id);
	}

	private boolean existsConstant(CourseData.CourseDataConstraint constraint) {
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

	public void executeClickEvent(int id) {
		switch (id) {
		case BUTTON_PLAY:
			if (getMainController().getInputProcessor().getLastKeyChangedDevice() != null) {
				getMainController().getPlayerResource().setPlayDeviceType(getMainController().getInputProcessor().getLastKeyChangedDevice().getType());
			}
			play = 0;
			break;
		case BUTTON_AUTOPLAY:
			play = 1;
			break;
		case BUTTON_PRACTICE:
			if (getMainController().getInputProcessor().getLastKeyChangedDevice() != null) {
				getMainController().getPlayerResource().setPlayDeviceType(getMainController().getInputProcessor().getLastKeyChangedDevice().getType());
			}
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
		if (updateSong == null || !updateSong.isAlive()) {
			updateSong = new MusicSelector.SongUpdateThread(selected);
			updateSong.start();
		} else {
			Logger.getGlobal().warning("楽曲更新中のため、更新要求は取り消されました");
		}
	}

	public void selectedBarMoved() {
		resetReplayIndex();
		getTimer()[TIMER_SONGBAR_CHANGE] = getNowTime();
		if(preview.getSongData() != null && (!(bar.getSelected() instanceof SongBar) ||
				!((SongBar)bar.getSelected()).getSongData().getParent().equals(preview.getSongData().getParent())))
		preview.start(null);
		showNoteGraph = false;
	}

	public void selectSong(int mode) {
		if (mode < 3) {
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
				getSongDatabase().updateSongDatas(null, false, getMainController().getInfoDatabase());
			} else if (selected instanceof FolderBar) {
				FolderBar fb = (FolderBar) selected;
				getSongDatabase().updateSongDatas(fb.getFolderData().getPath(), false,
						getMainController().getInfoDatabase());
			} else if (selected instanceof TableBar) {
				TableBar tb = (TableBar) selected;
				TableData td = tb.getReader().read();
				if (td != null) {
					new TableDataAccessor().write(td);
					tb.setTableData(td);
				}
			}
		}
	}
}
