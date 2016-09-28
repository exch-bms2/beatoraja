package bms.player.beatoraja.select;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.beatoraja.Config.SkinConfig;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.decide.MusicDecideSkin;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.play.audio.SoundProcessor;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends MainState {

	public static final int OPTION_GRADEBAR = 3;
	public static final int BUTTON_MODE = 1011;
	public static final int BUTTON_SORT = 1012;
	public static final int BUTTON_LNMODE = 1911;

	// TODO テキスト表示
	// TODO 譜面情報表示
	// TODO オプション常時表示(スキン実装で実現？)

	private int selectedreplay;
	/**
	 * 現在のフォルダ階層
	 */
	private List<Bar> dir = new ArrayList<Bar>();

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

	private static final String[] MODE = { "ALL", "7 KEY", "14 KEY", "9 KEY", "5 KEY", "10 KEY" };
	/**
	 * 選択中のソート
	 */
	private int sort;

	public static final BarSorter[] SORT = { BarSorter.NAME_SORTER, BarSorter.LEVEL_SORTER, BarSorter.LAMP_SORTER,
			BarSorter.SCORE_SORTER, BarSorter.MISSCOUNT_SORTER };

	private static final String[] LAMP = { "404040", "800000", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };

	private static final String[] RANK = { "F-", "F-", "F", "F", "F+", "F+", "E-", "E", "E+", "D-", "D", "D+", "C-",
			"C", "C+", "B-", "B", "B+", "A-", "A", "A+", "AA-", "AA", "AA+", "AAA-", "AAA", "AAA+", "MAX" };

	private static final String[] RANKCOLOR = { "404040", "400040", "400040", "400040", "400040", "400040", "000040",
			"000040", "000040", "004040", "004040", "004040", "00c000", "00c000", "00c000", "80c000", "80c000",
			"80c000", "f08000", "f08000", "f08000", "e0e0e0", "e0e0e0", "e0e0e0", "ffff44", "ffff44", "ffff44",
			"ffffcc" };

	private static final String[] LNMODE = { "LONG NOTE", "CHARGE NOTE", "HELL CHARGE NOTE" };

	public static final int REPLAY = 4;

	private Config config;

	private PlayerData playerdata;

	private MusicSelectSkin skin;

	private Sound bgm;
	private Sound move;
	private Sound folderopen;
	private Sound folderclose;
	private Sound sorts;

	private FreeTypeFontGenerator generator;
	private BitmapFont titlefont;

	private TextureRegion banner;
	private Bar bannerbar;

	private BarRenderer bar;
	private GameOptionRenderer option;
	private AssistOptionRenderer aoption;
	private DetailOptionRenderer doption;

	/**
	 * スコアデータのキャッシュ
	 */
	private Map<String, IRScoreData>[] scorecache;

	public static final int KEY_PLAY = 1;
	public static final int KEY_AUTO = 2;
	public static final int KEY_REPLAY = 3;
	public static final int KEY_UP = 4;
	public static final int KEY_DOWN = 5;
	public static final int KEY_FOLDER_OPEN = 6;
	public static final int KEY_FOLDER_CLOSE = 7;

	private int panelstate;

	public MusicSelector(MainController main, Config config) {
		super(main);
		this.config = config;
		try {
			Class.forName("org.sqlite.JDBC");
			songdb = main.getSongDatabase();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		scorecache = new Map[3];
		for (int i = 0; i < scorecache.length; i++) {
			scorecache[i] = new HashMap();
		}

		bar = new BarRenderer(main, this, songdb);
	}

	IRScoreData readScoreData(SongData song, int lnmode) {
		if (scorecache[lnmode].containsKey(song.getSha256())) {
			return scorecache[lnmode].get(song.getSha256());
		}
		IRScoreData score = getMainController().getPlayDataAccessor().readScoreData(song.getSha256(),
				song.hasLongNote(), lnmode);
		for (int i = 0; i < scorecache.length; i++) {
			if (!song.hasLongNote() || i == lnmode) {
				scorecache[i].put(song.getSha256(), score);
			}
		}
		return score;
	}

	Map<String, IRScoreData> readScoreDatas(SongData[] songs, int lnmode) {
		Map<String, IRScoreData> result = new HashMap();
		List<SongData> noscore = new ArrayList();
		for (SongData song : songs) {
			if (scorecache[lnmode].containsKey(song.getSha256())) {
				result.put(song.getSha256(), scorecache[lnmode].get(song.getSha256()));
			} else {
				noscore.add(song);
			}
		}

		Map<String, IRScoreData> scores = getMainController().getPlayDataAccessor().readScoreDatas(
				noscore.toArray(new SongData[0]), lnmode);
		for (SongData song : noscore) {
			IRScoreData score = scores.get(song.getSha256());
			for (int i = 0; i < scorecache.length; i++) {
				if (!song.hasLongNote() || i == lnmode) {
					scorecache[i].put(song.getSha256(), score);
				}
			}
			result.put(song.getSha256(), score);
		}
		return result;

	}

	public void create() {
		final MainController main = getMainController();
		playerdata = main.getPlayDataAccessor().readPlayerData();
		for (Map cache : scorecache) {
			cache.clear();
		}

		BMSPlayerInputProcessor input = main.getInputProcessor();
		PlayConfig pc = (config.getMusicselectinput() == 0 ? config.getMode7() : config.getMode9());
		input.setKeyassign(pc.getKeyassign());
		input.setControllerassign(pc.getControllerassign());

		if (dir.size() > 0) {
			bar.updateBar(dir.get(dir.size() - 1));
		} else {
			bar.updateBar(null);
		}

		if (config.getBgmpath().length() > 0) {
			final File bgmfolder = new File(config.getBgmpath());
			if (bgmfolder.exists() && bgmfolder.isDirectory()) {
				for (File f : bgmfolder.listFiles()) {
					if (bgm == null && f.getName().startsWith("select.")) {
						bgm = SoundProcessor.getSound(f.getPath());
						break;
					}
				}
			}
		}
		if (bgm != null) {
			bgm.loop();
		}
		if (config.getSoundpath().length() > 0) {
			final File soundfolder = new File(config.getSoundpath());
			if (soundfolder.exists() && soundfolder.isDirectory()) {
				for (File f : soundfolder.listFiles()) {
					if (move == null && f.getName().startsWith("scratch.")) {
						move = SoundProcessor.getSound(f.getPath());
					}
					if (folderopen == null && f.getName().startsWith("f-open.")) {
						folderopen = SoundProcessor.getSound(f.getPath());
					}
					if (folderclose == null && f.getName().startsWith("f-close.")) {
						folderclose = SoundProcessor.getSound(f.getPath());
					}
					if (sorts == null && f.getName().startsWith("o-change.")) {
						sorts = SoundProcessor.getSound(f.getPath());
					}
				}
			}
		}

		if (skin == null) {
			if (config.getSkin()[5] != null) {
				try {
					SkinConfig sc = config.getSkin()[5];
					LR2SkinHeaderLoader loader = new LR2SkinHeaderLoader();
					LR2SkinHeader header = loader.loadSkin(Paths.get(sc.getPath()), this, sc.getProperty());
					Rectangle srcr = MainController.RESOLUTION[header.getResolution()];
					Rectangle dstr = MainController.RESOLUTION[config.getResolution()];
					LR2SelectSkinLoader dloader = new LR2SelectSkinLoader(srcr.width, srcr.height, dstr.width, dstr.height);
					skin = dloader.loadSelectSkin(new File(header.getInclude()), this, header,
							loader.getOption(), sc.getProperty());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					skin = new MusicSelectSkin(main.RESOLUTION[config.getResolution()]);
				}
			} else {
				skin = new MusicSelectSkin(main.RESOLUTION[config.getResolution()]);
			}
			this.setSkin(skin);
		}

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);

		option = new GameOptionRenderer(config);
		aoption = new AssistOptionRenderer(config);
		doption = new DetailOptionRenderer(config);

		getTimer()[TIMER_SONGBAR_CHANGE] = getNowTime();
	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		final PlayerResource resource = main.getPlayerResource();
		final Bar current = bar.getSelected();

		final int nowtime = getNowTime();
		// draw song information
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		if (current instanceof SongBar) {
			resource.setSongdata(((SongBar) current).getSongData());
			titlefont.draw(sprite, "LEVEL : " + resource.getSongdata().getLevel(), 100, 500);
			if (current.getScore() != null) {
				IRScoreData score = current.getScore();
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite,
						RANK[(score.getExscore() * 27 / (score.getNotes() * 2))] + " ( "
								+ ((score.getExscore() * 1000 / (score.getNotes() * 2)) / 10.0f) + "% )", 460, 390);
			}
			if (((SongBar) current).existsReplayData()) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < ((SongBar) current).getExistsReplayData().length; i++) {
					if (selectedreplay == i) {
						sb.append("[" + (i + 1) + "]");
					} else if (((SongBar) current).getExistsReplayData()[i]) {
						sb.append(" " + (i + 1) + " ");
					}
				}
				titlefont.setColor(Color.GREEN);
				titlefont.draw(sprite, "Replay exists : " + sb.toString(), 100, 270);
			}
		} else {
			resource.setSongdata(null);
		}
		// 段位用の表示(ミラー段位、EX段位)
		if (current instanceof GradeBar) {
			GradeBar gb = (GradeBar) current;

			for (int con : gb.getConstraint()) {
				switch (con) {
				case TableData.GRADE_NORMAL:
					break;
				case TableData.GRADE_MIRROR:
					titlefont.setColor(Color.CYAN);
					titlefont.draw(sprite, "MIRROR OK", 150, 570);
					break;
				case TableData.GRADE_RANDOM:
					titlefont.setColor(Color.CORAL);
					titlefont.draw(sprite, "RANDOM OK", 150, 570);
					break;
				case TableData.NO_HISPEED:
					titlefont.setColor(Color.RED);
					titlefont.draw(sprite, "x1.0 HI SPEED", 300, 570);
					break;
				case TableData.NO_GOOD:
					titlefont.setColor(Color.PURPLE);
					titlefont.draw(sprite, "NO GOOD", 450, 570);
					break;
				case TableData.NO_GREAT:
					titlefont.setColor(Color.PURPLE);
					titlefont.draw(sprite, "NO GREAT", 450, 570);
					break;
				}
			}

			if (gb.getMirrorScore() != null) {
				IRScoreData score = gb.getMirrorScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				// titlefont.draw(sprite, CLEAR[score.getClear()], 100, 270);
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : " + score.getExscore() + " / " + (score.getNotes() * 2), 100, 240);
				titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(), 100, 210);
				titlefont.draw(sprite, "CLEAR / PLAY : " + score.getClearcount() + " / " + score.getPlaycount(), 100,
						180);
			}
			if (gb.getRandomScore() != null) {
				IRScoreData score = gb.getRandomScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				// titlefont.draw(sprite, CLEAR[score.getClear()], 100, 130);
				// titlefont.setColor(Color.WHITE);
				// titlefont.draw(sprite, "EX-SCORE  : " + score.getExscore() +
				// " / " + (score.getNotes() * 2), 100, 240);
				// titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(),
				// 100, 210);
				// titlefont.draw(sprite, "CLEAR / PLAY : " +
				// score.getClearcount() + " / " + score.getPlaycount(), 100,
				// 180);
			}
			if (gb.existsReplayData()) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < gb.getExistsReplayData().length; i++) {
					if (selectedreplay == i) {
						sb.append("[" + (i + 1) + "]");
					} else if (gb.getExistsReplayData()[i]) {
						sb.append(" " + (i + 1) + " ");
					}
				}
				titlefont.setColor(Color.GREEN);
				titlefont.draw(sprite, "Replay exists : " + sb.toString(), 450, 300);
			}
		}
		if (current instanceof TableLevelBar) {
			titlefont.draw(sprite, current.getTitle(), 100, 600);
		}

		titlefont.setColor(Color.WHITE);
		if (current instanceof FolderBar) {
			if (config.isFolderlamp()) {
				int[] lamps = ((FolderBar) current).getLamps();
				int[] ranks = ((FolderBar) current).getRanks();
				int count = 0;
				for (int lamp : lamps) {
					count += lamp;
				}
				titlefont.draw(sprite, "TOTAL SONGS : ", 100, 500);
				sprite.end();
				shape.begin(ShapeType.Filled);

				if (count != 0) {
					for (int i = 10, x = 0; i >= 0; i--) {
						shape.setColor(Color.valueOf(LAMP[i]));
						shape.rect(100 + x * 400 / count, 360, lamps[i] * 400 / count, 30);
						x += lamps[i];
					}
					for (int i = 27, x = 0; i >= 0; i--) {
						shape.setColor(Color.valueOf(RANKCOLOR[i]));
						shape.rect(100 + x * 400 / count, 320, ranks[i] * 400 / count, 30);
						x += ranks[i];
					}
				}
				shape.end();
				sprite.begin();
			}
		}

		if (current instanceof TableBar) {
		}

		if (current instanceof TableLevelBar) {
			if (config.isFolderlamp()) {
				int[] lamps = ((TableLevelBar) current).getLamps();
				int[] ranks = ((TableLevelBar) current).getRanks();
				int count = 0;
				for (int lamp : lamps) {
					count += lamp;
				}
				titlefont.draw(sprite, "TOTAL SONGS : ", 100, 500);
				sprite.end();
				shape.begin(ShapeType.Filled);

				if (count != 0) {
					for (int i = 10, x = 0; i >= 0; i--) {
						shape.setColor(Color.valueOf(LAMP[i]));
						shape.rect(100 + x * 400 / count, 360, lamps[i] * 400 / count, 30);
						x += lamps[i];
					}
					for (int i = 27, x = 0; i >= 0; i--) {
						shape.setColor(Color.valueOf(RANKCOLOR[i]));
						shape.rect(100 + x * 400 / count, 320, ranks[i] * 400 / count, 30);
						x += ranks[i];
					}
				}
				shape.end();
				sprite.begin();
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

		boolean[] numberstate = input.getNumberState();
		long[] numtime = input.getNumberTime();
		if (numberstate[0] && numtime[0] != 0) {
			numtime[0] = 0;
			Gdx.input.getTextInput(new TextInputListener() {
				@Override
				public void input(String text) {
					if (text.length() > 1) {
						bar.addSearch(new SearchWordBar(MusicSelector.this, text));
						dir.clear();
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
			mode = (mode + 1) % MODE.length;
			numtime[1] = 0;
			if (dir.size() > 0) {
				bar.updateBar(dir.get(dir.size() - 1));
			}
			if (sorts != null) {
				sorts.play();
			}
		}
		if (numberstate[2] && numtime[2] != 0) {
			// ソートの切り替え
			sort = (sort + 1) % SORT.length;
			numtime[2] = 0;
			if (dir.size() > 0) {
				bar.updateBar(dir.get(dir.size() - 1));
			}
			if (sorts != null) {
				sorts.play();
			}
		}
		if (numberstate[3] && numtime[3] != 0) {
			// LNモードの切り替え
			config.setLnmode((config.getLnmode() + 1) % LNMODE.length);
			numtime[3] = 0;
			if (dir.size() > 0) {
				bar.updateBar(dir.get(dir.size() - 1));
			}
			if (sorts != null) {
				sorts.play();
			}
		}
		if (numberstate[4] && numtime[4] != 0) {
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
				sorts.play();
			}
		}

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		boolean[] cursor = input.getCursorState();

		final int prevpanelstate = panelstate;
		panelstate = 0;

		if (input.startPressed()) {
			panelstate = 1;
			option.render(keystate, keytime);
		} else if (input.isSelectPressed()) {
			panelstate = 2;
			aoption.render(keystate, keytime);
		} else if (input.getNumberState()[5]) {
			panelstate = 3;
			doption.render(keystate, keytime, current);
		} else if (input.getNumberState()[6]) {
			if (bgm != null) {
				bgm.stop();
			}
			main.changeState(MainController.STATE_CONFIG);
		} else {
			// 1鍵 (選曲 or フォルダを開く)
			if (isPressed(keystate, keytime, KEY_PLAY, true) || cursor[3]) {
				cursor[3] = false;
				if (current instanceof DirectoryBar) {
					if (bar.updateBar(current)) {
						if (folderopen != null) {
							folderopen.play();
						}
						dir.add(current);
					}
					resetReplayIndex();
				} else if (current instanceof SongBar) {
					resource.clear();
					if (resource.setBMSFile(Paths.get(((SongBar) current).getSongData().getPath()), config, 0)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);
					}
				} else if (current instanceof GradeBar) {
					readCourse(0);
				}
			}
			// 5鍵 (オートプレイ)
			if (isPressed(keystate, keytime, KEY_AUTO, true)) {
				if (current instanceof SongBar) {
					resource.clear();
					if (resource.setBMSFile(Paths.get(((SongBar) current).getSongData().getPath()), config, 1)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);

					}
				} else if (current instanceof GradeBar) {
					readCourse(1);
				}
			}
			// 7鍵 (リプレイ)
			if (isPressed(keystate, keytime, KEY_REPLAY, true)) {
				if (current instanceof SongBar) {
					resource.clear();
					if (resource.setBMSFile(Paths.get(((SongBar) current).getSongData().getPath()), config,
							2 + selectedreplay)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);
					}
				} else if (current instanceof GradeBar) {
					readCourse(2 + selectedreplay);
				}
			}
			// 白鍵 (フォルダを開く)
			if (isPressed(keystate, keytime, KEY_FOLDER_OPEN, true) || cursor[3]) {
				cursor[3] = false;
				if (current instanceof DirectoryBar) {
					if (bar.updateBar(current)) {
						if (folderopen != null) {
							folderopen.play();
						}
						dir.add(current);
					}
					resetReplayIndex();
				}
			}

			// 黒鍵 (フォルダを閉じる)
			if (isPressed(keystate, keytime, KEY_FOLDER_CLOSE, true) || cursor[2]) {
				keytime[1] = 0;
				cursor[2] = false;
				Bar pbar = null;
				Bar cbar = null;
				if (dir.size() > 1) {
					pbar = dir.get(dir.size() - 2);
				}
				if (dir.size() > 0) {
					cbar = dir.get(dir.size() - 1);
					dir.remove(dir.size() - 1);
					if (folderclose != null) {
						folderclose.play();
					}
				}
				bar.updateBar(pbar);
				if (cbar != null) {
					bar.setSelected(cbar);
				}
			}
		}

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

		// song bar scroll
		if (panelstate == 0 && (isPressed(keystate, keytime, KEY_UP, false)) || cursor[1]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				bar.move(true);
				if (move != null) {
					move.play();
				}
				duration = l + 300;
				angle = 300;
			}
			if (l > duration) {
				duration = l + 50;
				bar.move(true);
				if (move != null) {
					move.play();
				}
				angle = 50;
			}
		} else if (panelstate == 0 && (isPressed(keystate, keytime, KEY_DOWN, false)) || cursor[0]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				bar.move(false);
				if (move != null) {
					move.play();
				}
				duration = l + 300;
				angle = -300;
			}
			if (l > duration) {
				duration = l + 50;
				bar.move(false);
				if (move != null) {
					move.play();
				}
				angle = -50;
			}
		} else {
			long l = System.currentTimeMillis();
			if (l > duration) {
				duration = 0;
			}
		}
		// song bar scroll on mouse wheel
		if (input.getScroll() > 0) {
			for (int i = 0; i < input.getScroll(); i++) {
				bar.move(false);
			}
			input.resetScroll();
		}
		if (input.getScroll() < 0) {
			for (int i = 0; i < -input.getScroll(); i++) {
				bar.move(true);
			}
			input.resetScroll();
		}

		if (bar.getSelected() != current || selectedreplay == -1) {
			resetReplayIndex();
		}
		if (bar.getSelected() != current) {
			getTimer()[TIMER_SONGBAR_CHANGE] = nowtime;
		}

		if (input.isExitPressed()) {
			exit();
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
					bgm.stop();
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

	private boolean isPressed(boolean[] keystate, long[] keytime, int code, boolean resetState) {
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

	public List<Bar> getDir() {
		return dir;
	}

	public void exit() {
		getMainController().exit();
	}

	public void dispose() {
		bar.dispose();
		if (skin != null) {
			skin.dispose();
			skin = null;
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
			return (int) (playerdata.getEpg() + playerdata.getLpg())
					+ (int) (playerdata.getEgr() + playerdata.getLgr())
					+ (int) (playerdata.getEgd() + playerdata.getLgd())
					+ (int) (playerdata.getEbd() + playerdata.getLbd());
		case NUMBER_PLAYCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getPlaycount()
					: Integer.MIN_VALUE;
		case NUMBER_CLEARCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getClearcount()
					: Integer.MIN_VALUE;
		case NUMBER_FAILCOUNT:
			return bar.getSelected().getScore() != null ? bar.getSelected().getScore().getPlaycount()
					- bar.getSelected().getScore().getClearcount() : Integer.MIN_VALUE;
		case NUMBER_CLEAR:
			if (bar.getSelected().getScore() != null) {
				return bar.getSelected().getScore().getClear();
			}
			return Integer.MIN_VALUE;
		case NUMBER_SCORE:
			if (bar.getSelected().getScore() != null) {
				return bar.getSelected().getScore().getExscore();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MISSCOUNT:
			if (bar.getSelected().getScore() != null) {
				return bar.getSelected().getScore().getMinbp();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXCOMBO:
			if (bar.getSelected().getScore() != null) {
				return bar.getSelected().getScore().getCombo();
			}
			return Integer.MIN_VALUE;
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
			if(bar.getSelected() instanceof SongBar) {
				SongBar song = (SongBar)bar.getSelected();
				PlayConfig pc = (song.getSongData().getMode() == 5 || song.getSongData().getMode() == 7 ? config.getMode7()
						: (song.getSongData().getMode() == 10 || song.getSongData().getMode() == 14 ? config.getMode14() : config.getMode9()));
				return pc.getDuration();
			}
			return Integer.MIN_VALUE;
		case NUMBER_JUDGETIMING:
			return config.getJudgetiming();
			case BUTTON_MODE:
				final int[] mode_lr2 = {0,2,4,5,1,3};
				return mode_lr2[mode];
			case BUTTON_SORT:
				return sort;
			case BUTTON_LNMODE:
				return config.getLnmode();
		}
		return super.getNumberValue(id);
	}


	public String getTextValue(int id) {
		switch (id) {
		case STRING_COURSE1_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 0) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[0];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_COURSE2_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 1) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[1];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_COURSE3_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 2) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[2];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_COURSE4_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 3) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[3];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_COURSE5_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 4) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[4];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_COURSE6_TITLE:
			if (bar.getSelected() instanceof GradeBar) {
				if (((GradeBar) bar.getSelected()).getSongDatas().length > 5) {
					SongData song = ((GradeBar) bar.getSelected()).getSongDatas()[5];
					return song != null ? song.getTitle() : "-----";
				}
			}
			return "";
		case STRING_DIRECTORY:
			StringBuffer str = new StringBuffer();
			for (Bar b : dir) {
				str.append(b.getTitle() + " > ");
			}
			return str.toString();
		}
		return super.getTextValue(id);
	}

	PlayerResource getResource() {
		return getMainController().getPlayerResource();
	}

	SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	@Override
	public float getSliderValue(int id) {
		switch(id) {
			case SLIDER_MUSICSELECT_POSITION:
				return bar.getSelectedPosition();
			case BARGRAPH_RATE_PGREAT:
				if (bar.getSelected() instanceof SongBar) {
				IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)(score.getEpg() + score.getLpg())) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_LEVEL:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_LEVEL_BEGINNER:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getDifficulty() != 1) {
						return 0;
					}
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_LEVEL_NORMAL:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getDifficulty() != 2) {
						return 0;
					}
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_LEVEL_HYPER:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getDifficulty() != 3) {
						return 0;
					}
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_LEVEL_ANOTHER:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getDifficulty() != 4) {
						return 0;
					}
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_LEVEL_INSANE:
				if (bar.getSelected() instanceof SongBar) {
					SongData sd = ((SongBar) bar.getSelected()).getSongData();
					if(sd.getDifficulty() != 5) {
						return 0;
					}
					if(sd.getMode() == 5 || sd.getMode() == 10) {
						return sd.getLevel() / 9.0f;
					}
					if(sd.getMode() == 7 || sd.getMode() == 14) {
						return sd.getLevel() / 12.0f;
					}
					if(sd.getMode() == 9) {
						return sd.getLevel() / 50.0f;
					}
				}
				return 0;
			case BARGRAPH_RATE_GREAT:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)(score.getEgr() + score.getLgr())) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_RATE_GOOD:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)(score.getEgd() + score.getLgd())) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_RATE_BAD:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)(score.getEbd() + score.getLbd())) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_RATE_POOR:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)(score.getEpr() + score.getLpr())) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_RATE_MAXCOMBO:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)score.getCombo()) / ((SongBar) bar.getSelected()).getSongData().getNotes() : 0;
				}
				return 0;
			case BARGRAPH_RATE_EXSCORE:
				if (bar.getSelected() instanceof SongBar) {
					IRScoreData score = bar.getSelected().getScore();
					return score != null ? ((float)score.getExscore()) / ((SongBar) bar.getSelected()).getSongData().getNotes() / 2 : 0;
				}
				return 0;
		}
		return 0;
	}
	
	public void setSliderValue(int id, float value) {
		switch(id) {
		case SLIDER_MUSICSELECT_POSITION:
			bar.setSelectedPosition(value);
		}
	}

	public TextureRegion getImage(int imageid) {
		if (imageid == IMAGE_BANNER) {
			return banner;
		}
		return null;
	}

	public void renderBar(int time) {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		final float w = MainController.RESOLUTION[config.getResolution()].width;
		final float h = MainController.RESOLUTION[config.getResolution()].height;
		sprite.end();
		bar.render(sprite, shape, skin, w, h, duration, angle, time);
		sprite.begin();
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
				return (current instanceof SongBar) || ((current instanceof GradeBar) && ((GradeBar) current).existsAllSongs());
		}
		return super.getBooleanValue(id);
	}
}
