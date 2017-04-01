package bms.player.beatoraja.select;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.PlaySkin;
import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.lr2.*;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.Resolution.*;
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

	private final int durationlow = 300;
	private final int durationhigh = 50;
	/**
	 * バー移動中のカウンタ
	 */
	private long duration;
	/**
	 * バーの移動方向
	 */
	private int angle;
	/**
	 * 楽曲DBアクセサ
	 */
	private SongDatabaseAccessor songdb;
	/**
	 * 選択中のモードフィルタ
	 */
	private int mode;
	/**
	 * 選択中のソート
	 */
	private int sort;

	public static final int REPLAY = 4;

	private Config config;

	private PlayerData playerdata;

	private String bgm;
	private String move;
	private String folderopen;
	private String folderclose;
	private String sorts;
	private String preview;

	private BitmapFont titlefont;

	private TextureRegion banner;
	private Bar bannerbar;
	/**
	 * 楽曲バー描画用
	 */
	private BarRenderer bar;

	private SearchTextField search;

	private final int notesGraphDuration = 1000;
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

	public MusicSelector(MainController main, Config config) {
		super(main);
		this.config = config;
		songdb = main.getSongDatabase();

		scorecache = new ScoreDataCache(getMainController().getPlayDataAccessor());

		if (config.getBgmpath().length() > 0) {
			final File bgmfolder = new File(config.getBgmpath());
			if (bgmfolder.exists() && bgmfolder.isDirectory()) {
				for (File f : bgmfolder.listFiles()) {
					if (bgm == null && f.getName().startsWith("select.")) {
						bgm = f.getPath();
						break;
					}
				}
			}
		}
		if (bgm != null) {
			getMainController().getAudioProcessor().play(bgm, true);
		}
		if (config.getSoundpath().length() > 0) {
			final File soundfolder = new File(config.getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (move == null && f.getName().startsWith("scratch.")) {
						move = f.getPath();
					}
					if (folderopen == null && f.getName().startsWith("f-open.")) {
						folderopen = f.getPath();
					}
					if (folderclose == null && f.getName().startsWith("f-close.")) {
						folderclose = f.getPath();
					}
					if (sorts == null && f.getName().startsWith("o-change.")) {
						sorts = f.getPath();
					}
				}
			}
		}

		bar = new BarRenderer(this, move, folderclose);
	}

	public ScoreDataCache getScoreDataCache() {
		return scorecache;
	}

	public void create() {
		play = -1;
		final MainController main = getMainController();
		playerdata = main.getPlayDataAccessor().readPlayerData();
		scorecache.clear();

		final BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayConfig pc = (config.getMusicselectinput() == 0 ? config.getMode7() : (config.getMusicselectinput() == 1 ? config.getMode9() : config.getMode14()));
		input.setKeyassign(pc.getKeyassign());
		input.setControllerConfig(pc.getController());

		bar.updateBar();

		if (getSkin() == null) {
			try {
				SkinConfig sc = config.getSkin()[5];
				if (sc.getPath().endsWith(".json")) {
					SkinLoader sl = new SkinLoader(
							RESOLUTION[getMainController().getPlayerResource().getConfig().getResolution()]);
					setSkin(sl.loadSelectSkin(Paths.get(sc.getPath()), sc.getProperty()));
				} else {
					LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
					SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
					Rectangle srcr = RESOLUTION[header.getResolution()];
					Rectangle dstr = RESOLUTION[config.getResolution()];
					LR2SelectSkinLoader dloader = new LR2SelectSkinLoader(srcr.width, srcr.height, dstr.width,
							dstr.height);
					setSkin(dloader.loadSelectSkin(Paths.get(sc.getPath()).toFile(), this, header, loader.getOption(),
							sc.getProperty()));
				}
			} catch (Throwable e) {
				e.printStackTrace();
				SkinLoader sl = new SkinLoader(
						RESOLUTION[getMainController().getPlayerResource().getConfig().getResolution()]);
				setSkin(sl.loadSelectSkin(Paths.get(SkinConfig.DEFAULT_SELECT), new HashMap()));
			}
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
		generator.dispose();

		getTimer()[TIMER_SONGBAR_CHANGE] = getNowTime();

		// search text field
		if (getStage() == null && ((MusicSelectSkin) getSkin()).getSearchTextRegion() != null) {
			search = new SearchTextField(this, RESOLUTION[config.getResolution()]);
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
		// 段位用の表示(ミラー段位、EX段位)
		if (current instanceof GradeBar) {
			GradeBar gb = (GradeBar) current;

			float dw = (float) getSkin().getScaleX();
			float dh = (float) getSkin().getScaleY();
			for (int con : gb.getConstraint()) {
				switch (con) {
				case TableData.GRADE_NORMAL:
					break;
				case TableData.GRADE_MIRROR:
					titlefont.setColor(Color.CYAN);
					titlefont.draw(sprite, "MIRROR OK", 150 * dw, 620 * dh);
					break;
				case TableData.GRADE_RANDOM:
					titlefont.setColor(Color.CORAL);
					titlefont.draw(sprite, "RANDOM OK", 150 * dw, 620 * dh);
					break;
				case TableData.NO_HISPEED:
					titlefont.setColor(Color.RED);
					titlefont.draw(sprite, "x1.0 HI SPEED", 300 * dw, 620 * dh);
					break;
				case TableData.NO_GOOD:
					titlefont.setColor(Color.PURPLE);
					titlefont.draw(sprite, "NO GOOD", 450 * dw, 620 * dh);
					break;
				case TableData.NO_GREAT:
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
		// read bms information
		if(getNowTime() > getTimer()[TIMER_SONGBAR_CHANGE] + notesGraphDuration && !showNoteGraph && play < 0) {
			if(current instanceof SongBar) {
				SongData song = main.getPlayerResource().getSongdata();
				song.setBMSModel(resource.loadBMSModel(Paths.get(((SongBar) current).getSongData().getPath()), config.getLnmode()));
				preview = song.getPreview();
				if(preview != null && preview.length() > 0) {
					preview = Paths.get(song.getPath()).getParent().resolve(preview).toString();
					getMainController().getAudioProcessor().stop(bgm);
					getMainController().getAudioProcessor().play(preview, false);					
				}
			}
			showNoteGraph = true;
		}

		if (play >= 0) {
			if (current instanceof SongBar) {
				resource.clear();
				if (resource.setBMSFile(Paths.get(((SongBar) current).getSongData().getPath()), config, play)) {
					if (bgm != null) {
						getMainController().getAudioProcessor().stop(bgm);
					}
					if(preview != null && preview.length() > 0) {
						getMainController().getAudioProcessor().stop(preview);
						getMainController().getAudioProcessor().dispose(preview);
						preview = null;
					}
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
		if (numberstate[0] && numtime[0] != 0) {
			// 検索用ポップアップ表示。これ必要？
			numtime[0] = 0;
			Gdx.input.getTextInput(new TextInputListener() {
				@Override
				public void input(String text) {
					if (text.length() > 1) {
						bar.addSearch(new SearchWordBar(MusicSelector.this, text));
						bar.updateBar(null);
					}
				}

				@Override
				public void canceled() {
				}
			}, "Search", "", "Search bms title");
		}

		if (numberstate[1] && numtime[1] != 0) {
			// KEYフィルターの切り替え
			mode = (mode + 1) % 6;
			numtime[1] = 0;
			bar.updateBar();
			if (sorts != null) {
				getMainController().getAudioProcessor().play(sorts, false);
			}
		}
		if (numberstate[2] && numtime[2] != 0) {
			// ソートの切り替え
			sort = (sort + 1) % BarSorter.getAllSorter().length;
			numtime[2] = 0;
			bar.updateBar();
			if (sorts != null) {
				getMainController().getAudioProcessor().play(sorts, false);
			}
		}
		if (numberstate[3] && numtime[3] != 0) {
			// LNモードの切り替え
			config.setLnmode((config.getLnmode() + 1) % 3);
			numtime[3] = 0;
			bar.updateBar();
			if (sorts != null) {
				getMainController().getAudioProcessor().play(sorts, false);
			}
		}
		if (numberstate[4] && numtime[4] != 0) {
			// change replay
			if (current != null && current instanceof SelectableBar) {
				boolean[] replays = ((SelectableBar) current).getExistsReplayData();
				for (int i = 1; i < replays.length; i++) {
					if (replays[(i + selectedreplay) % replays.length]) {
						selectedreplay = (i + selectedreplay) % replays.length;
						break;
					}
				}
			}
			numtime[4] = 0;
			if (sorts != null) {
				getMainController().getAudioProcessor().play(sorts, false);
			}
		}

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		boolean[] cursor = input.getCursorState();
		long[] cursortime = input.getCursorTime();

		final int prevpanelstate = panelstate;
		panelstate = 0;

		if (input.startPressed()) {
			bar.resetInput();
			// show play option
			panelstate = 1;
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				config.setRandom(config.getRandom() + 1 < 10 ? config.getRandom() + 1 : 0);
			}
			if (keystate[2] && keytime[2] != 0) {
				keytime[2] = 0;
				config.setGauge(config.getGauge() + 1 < 6 ? config.getGauge() + 1 : 0);
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setDoubleoption(config.getDoubleoption() + 1 < 3 ? config.getDoubleoption() + 1 : 0);
			}
			if (keystate[6] && keytime[6] != 0) {
				keytime[6] = 0;
				config.setRandom2(config.getRandom2() + 1 < 10 ? config.getRandom2() + 1 : 0);
			}
			if (keystate[4] && keytime[4] != 0) {
				keytime[4] = 0;
				config.setFixhispeed(config.getFixhispeed() + 1 < 5 ? config.getFixhispeed() + 1 : 0);
			}

			// song bar scroll on mouse wheel
			int mov = -input.getScroll();
			input.resetScroll();
			// song bar scroll
			if (isPressed(keystate, keytime, KEY_UP, false) || cursor[1]) {
				long l = System.currentTimeMillis();
				if (duration == 0) {
					mov = 1;
					duration = l + durationlow;
					angle = durationlow;
				}
				if (l > duration) {
					duration = l + durationhigh;
					mov = 1;
					angle = durationhigh;
				}
			} else if (isPressed(keystate, keytime, KEY_DOWN, false) || cursor[0]) {
				long l = System.currentTimeMillis();
				if (duration == 0) {
					mov = -1;
					duration = l + durationlow;
					angle = -durationlow;
				}
				if (l > duration) {
					duration = l + durationhigh;
					mov = -1;
					angle = -durationhigh;
				}
			} else {
				long l = System.currentTimeMillis();
				if (l > duration) {
					duration = 0;
				}
			}

			TargetProperty[] targets = TargetProperty.getAllTargetProperties(getMainController());
			while(mov > 0) {
				config.setTarget((config.getTarget() + 1) % targets.length);
				if (move != null) {
					getMainController().getAudioProcessor().play(move, false);
				}
				mov--;
			}
			while(mov < 0) {
				config.setTarget((config.getTarget() + targets.length - 1) % targets.length);
				if (move != null) {
					getMainController().getAudioProcessor().play(move, false);
				}
				mov++;
			}
		} else if (input.isSelectPressed()) {
			bar.resetInput();
			// show assist option
			panelstate = 2;
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				config.setExpandjudge(!config.isExpandjudge());
			}
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				config.setConstant(!config.isConstant());
			}
			if (keystate[2] && keytime[2] != 0) {
				keytime[2] = 0;
				config.setShowjudgearea(!config.isShowjudgearea());
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setLegacynote(!config.isLegacynote());
			}
			if (keystate[4] && keytime[4] != 0) {
				keytime[4] = 0;
				config.setMarkprocessednote(!config.isMarkprocessednote());
			}
			if (keystate[5] && keytime[5] != 0) {
				keytime[5] = 0;
				config.setBpmguide(!config.isBpmguide());
			}
			if (keystate[6] && keytime[6] != 0) {
				keytime[6] = 0;
				config.setNomine(!config.isNomine());
			}
		} else if (input.getNumberState()[5]) {
			bar.resetInput();
			// show detail option
			panelstate = 3;
			PlayConfig pc = null;
			if (current instanceof SongBar) {
				SongBar song = (SongBar) current;
				pc = (song.getSongData().getMode() == 5 || song.getSongData().getMode() == 7 ? config.getMode7()
						: (song.getSongData().getMode() == 10 || song.getSongData().getMode() == 14 ? config.getMode14()
								: config.getMode9()));
			}
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				config.setBga((config.getBga() + 1) % 3);
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				if (pc != null && pc.getDuration() > 1) {
					pc.setDuration(pc.getDuration() - 1);
				}
			}
			if (keystate[4] && keytime[4] != 0) {
				keytime[4] = 0;
				if (config.getJudgetiming() > -99) {
					config.setJudgetiming(config.getJudgetiming() - 1);
				}
			}
			if (keystate[5] && keytime[5] != 0) {
				keytime[5] = 0;
				if (pc != null && pc.getDuration() < 2000) {
					pc.setDuration(pc.getDuration() + 1);
				}
			}
			if (keystate[6] && keytime[6] != 0) {
				keytime[6] = 0;
				if (config.getJudgetiming() < 99) {
					config.setJudgetiming(config.getJudgetiming() + 1);
				}
			}
		} else if (input.getNumberState()[6]) {
			if (bgm != null) {
				getMainController().getAudioProcessor().stop(bgm);
			}
			if(preview != null && preview.length() > 0) {
				getMainController().getAudioProcessor().stop(preview);
				getMainController().getAudioProcessor().dispose(preview);
				preview = null;
			}
			getMainController().changeState(MainController.STATE_CONFIG);
		} else {
			bar.input();

			if (current instanceof SelectableBar) {
				if (isPressed(keystate, keytime, KEY_PLAY, true) || (cursor[3] && cursortime[3] != 0)) {
					// play
					cursortime[3] = 0;
					resource.setPlayDevice(getMainController().getInputProcessor().getLastKeyChangedDevice());
					play = 0;
				} else if (isPressed(keystate, keytime, KEY_PRACTICE, true)) {
					// practice mode
					resource.setPlayDevice(getMainController().getInputProcessor().getLastKeyChangedDevice());
					play = 2;
				} else if (isPressed(keystate, keytime, KEY_AUTO, true)) {
					// auto play
					play = 1;
				} else if (isPressed(keystate, keytime, KEY_REPLAY, true)) {
					// replay
					play = (selectedreplay >= 0) ? 3 + selectedreplay : 0;
				}
			} else {
				if (isPressed(keystate, keytime, KEY_FOLDER_OPEN, true) || (cursor[3] && cursortime[3] != 0)) {
					// open folder
					cursortime[3] = 0;
					if (bar.updateBar(current)) {
						if (folderopen != null) {
							getMainController().getAudioProcessor().play(folderopen, false);
						}
					}
					resetReplayIndex();
				}
			}

			// close folder
			if (isPressed(keystate, keytime, KEY_FOLDER_CLOSE, true) || (cursor[2] && cursortime[2] != 0)) {
				keytime[1] = 0;
				cursortime[2] = 0;
				bar.close();
			}
		}
		// panel state changed
		if (prevpanelstate != panelstate) {
			if (prevpanelstate != 0) {
				getTimer()[TIMER_PANEL1_OFF + prevpanelstate - 1] = nowtime;
				getTimer()[TIMER_PANEL1_ON + prevpanelstate - 1] = Long.MIN_VALUE;
			}
			if (panelstate != 0) {
				getTimer()[TIMER_PANEL1_ON + panelstate - 1] = nowtime;
				getTimer()[TIMER_PANEL1_OFF + panelstate - 1] = Long.MIN_VALUE;
			}
		}

		if (bar.getSelected() != current || selectedreplay == -1) {
			resetReplayIndex();
		}
		// song bar moved
		if (bar.getSelected() != current) {
			getTimer()[TIMER_SONGBAR_CHANGE] = nowtime;
			getMainController().getPlayerResource().setSongdata((bar.getSelected() instanceof SongBar) ? ((SongBar) bar.getSelected()).getSongData() : null);
			if(preview != null && preview.length() > 0) {
				getMainController().getAudioProcessor().stop(preview);
				getMainController().getAudioProcessor().dispose(preview);
				getMainController().getAudioProcessor().play(bgm, true);
				preview = null;
			}
			showNoteGraph = false;
		}
		// update folder
		if (input.getFunctionstate()[1] && input.getFunctiontime()[1] != 0) {
			input.getFunctiontime()[1] = 0;
			if (bar.getSelected() instanceof FolderBar) {
				FolderBar fb = (FolderBar) bar.getSelected();
				songdb.updateSongDatas(fb.getFolderData().getPath(), false);
			} else if (bar.getSelected() instanceof TableBar) {
				TableBar tb = (TableBar) bar.getSelected();
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

		if (input.isExitPressed()) {
			getMainController().exit();
		}
	}

	public void select(Bar current) {
		if (current instanceof DirectoryBar) {
			if (bar.updateBar(current)) {
				if (folderopen != null) {
					getMainController().getAudioProcessor().play(folderopen, false);
				}
			}
			resetReplayIndex();
		} else {
			play = 0;
		}
	}

	private void resetReplayIndex() {
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

	private void readCourse(int autoplay) {
		final PlayerResource resource = getMainController().getPlayerResource();
		if (((GradeBar) bar.getSelected()).existsAllSongs()) {
			resource.clear();
			List<Path> files = new ArrayList<Path>();
			for (SongData song : ((GradeBar) bar.getSelected()).getSongDatas()) {
				files.add(Paths.get(song.getPath()));
			}
			if (resource.setCourseBMSFiles(files.toArray(new Path[files.size()]))) {
				for (int constraint : ((GradeBar) bar.getSelected()).getConstraint()) {
					switch (constraint) {
					case TableData.GRADE_NORMAL:
						if (autoplay < 2) {
							config.setRandom(0);
							config.setRandom2(0);
							config.setDoubleoption(0);
						}
						break;
					case TableData.GRADE_MIRROR:
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
					case TableData.GRADE_RANDOM:
						if (autoplay < 2) {
							if (config.getRandom() > 5) {
								config.setRandom(0);
							}
							if (config.getRandom2() > 5) {
								config.setRandom2(0);
							}
						}
						break;
					case TableData.NO_HISPEED:
						resource.addConstraint(TableData.NO_HISPEED);
						break;
					case TableData.NO_GOOD:
						resource.addConstraint(TableData.NO_GOOD);
						break;
					case TableData.NO_GREAT:
						resource.addConstraint(TableData.NO_GREAT);
						break;
					}
				}
				if (bgm != null) {
					getMainController().getAudioProcessor().stop(bgm);
				}
				if(preview != null && preview.length() > 0) {
					getMainController().getAudioProcessor().stop(preview);
					getMainController().getAudioProcessor().dispose(preview);
					preview = null;
				}
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
		int[][] keyassign = KeyConfiguration.keyassign[config.getMusicselectinput()];
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

	public int getMode() {
		return mode;
	}

	public int getSort() {
		return sort;
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

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
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
			final int[] mode_lr2 = { 0, 2, 4, 5, 1, 3 };
			return mode_lr2[mode];
		case BUTTON_SORT:
			return sort;
		case BUTTON_LNMODE:
			return config.getLnmode();
		case NUMBER_SCORE_RATE:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				return score.getNotes() == 0 ? 0 : score.getExscore() * 100 / (score.getNotes() * 2);
			}
			return Integer.MIN_VALUE;
		case NUMBER_SCORE_RATE_AFTERDOT:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				return score.getNotes() == 0 ? 0 : (score.getExscore() * 1000 / (score.getNotes() * 2)) % 10;
			}
			return Integer.MIN_VALUE;
		}
		return super.getNumberValue(id);
	}

	public String getTextValue(int id) {
		switch (id) {
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
		case OPTION_1P_F:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate <= 2222;
			}
			return false;
		case OPTION_1P_E:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 2222 && drate <= 3333;
			}
			return false;
		case OPTION_1P_D:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 3333 && drate <= 4444;
			}
			return false;
		case OPTION_1P_C:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 4444 && drate <= 5555;
			}
			return false;
		case OPTION_1P_B:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 5555 && drate <= 6666;
			}
			return false;
		case OPTION_1P_A:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 6666 && drate <= 7777;
			}
			return false;
		case OPTION_1P_AA:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 7777 && drate <= 8888;
			}
			return false;
		case OPTION_1P_AAA:
			if (bar.getSelected().getScore() != null) {
				final IRScoreData score = bar.getSelected().getScore();
				final int drate = score.getNotes() == 0 ? 0 : score.getExscore() * 10000 / (score.getNotes() * 2);
				return drate > 8888;
			}
			return false;
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
}
