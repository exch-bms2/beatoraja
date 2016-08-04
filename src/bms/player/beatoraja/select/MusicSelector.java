package bms.player.beatoraja.select;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.beatoraja.config.KeyConfiguration;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends MainState {

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

	private Texture banner;
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

	public MusicSelector(MainController main, Config config) {
		super(main);
		this.config = config;
		try {
			Class.forName("org.sqlite.JDBC");
			songdb = main.getSongDatabase();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		songdb.createTable();

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

		if (bgm == null) {
			if (new File("skin/select.wav").exists()) {
				bgm = Gdx.audio.newSound(Gdx.files.internal("skin/select.wav"));
			}
		}
		if (bgm != null) {
			bgm.loop();
		}
		if (move == null) {
			if (new File("skin/cursor.wav").exists()) {
				move = Gdx.audio.newSound(Gdx.files.internal("skin/cursor.wav"));
			}
		}
		if (folderopen == null) {
			if (new File("skin/folder_open.wav").exists()) {
				folderopen = Gdx.audio.newSound(Gdx.files.internal("skin/folder_open.wav"));
			}
		}
		if (folderclose == null) {
			if (new File("skin/folder_close.wav").exists()) {
				folderclose = Gdx.audio.newSound(Gdx.files.internal("skin/folder_close.wav"));
			}
		}
		if (sorts == null) {
			if (new File("skin/sort.wav").exists()) {
				sorts = Gdx.audio.newSound(Gdx.files.internal("skin/sort.wav"));
			}
		}

		if (config.getLr2selectskin() != null) {
			try {
				skin = new LR2SelectSkinLoader().loadSelectSkin(new File(config.getLr2selectskin()),
						config.getLr2selectskinoption());
			} catch (IOException e) {
				e.printStackTrace();
				skin = new MusicSelectSkin(main.RESOLUTION[config.getResolution()]);
			}

			// lr2playskin = "skin/spdframe/csv/left_ACwide.csv";

		} else {
			skin = new MusicSelectSkin(main.RESOLUTION[config.getResolution()]);
		}
		this.setSkin(skin);

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);

		option = new GameOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);
		aoption = new AssistOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);
		doption = new DetailOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);

	}

	public void render() {
		final MainController main = getMainController();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		final PlayerResource resource = main.getPlayerResource();
		final Bar current = bar.getSelected();

		final float w = MainController.RESOLUTION[config.getResolution()].width;
		final float h = MainController.RESOLUTION[config.getResolution()].height;

		// 背景描画
		// if (background != null) {
		// sprite.begin();
		// sprite.draw(background, 0, 0, w, h);
		// sprite.end();
		// }

		final int time = getNowTime();

		bar.render(sprite, shape, skin, w, h, duration, angle, time);
		// draw song information
		sprite.begin();
		titlefont.setColor(Color.WHITE);
		if (current instanceof SongBar) {
			SongData song = ((SongBar) current).getSongData();
			titlefont.draw(sprite, song.getMode() + " KEYS", 100, 530);
			titlefont.draw(sprite, "LEVEL : " + song.getLevel(), 100, 500);
			if (current.getScore() != null) {
				IRScoreData score = current.getScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : ", 50, 390);
				titlefont.draw(sprite, "RANK : " + RANK[(score.getExscore() * 27 / (score.getNotes() * 2))] + " ( "
						+ ((score.getExscore() * 1000 / (score.getNotes() * 2)) / 10.0f) + "% )", 300, 390);
				titlefont.draw(sprite, "MISS COUNT: ", 50, 360);
				titlefont.draw(sprite, "MAX COMBO : ", 300, 360);

				titlefont.draw(sprite, "CLEAR / PLAY : ", 50, 330);
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

			for (int i = 0; i < gb.getSongDatas().length; i++) {
				if (gb.getSongDatas()[i] != null) {
					titlefont.setColor(Color.YELLOW);
					titlefont.draw(sprite, gb.getSongDatas()[i].getTitle(), 120, 540 - i * 30);
				} else {
					titlefont.setColor(Color.GRAY);
					titlefont.draw(sprite, "no song", 120, 540 - i * 30);
				}
			}

			if (current.getScore() != null) {
				IRScoreData score = current.getScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : ", 50, 390);
				titlefont.draw(sprite, "MISS COUNT: ", 50, 360);
				titlefont.draw(sprite, "MAX COMBO : ", 300, 360);
				titlefont.draw(sprite, "CLEAR / PLAY : ", 50, 330);
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
		titlefont.draw(sprite, "PLAYCOUNT : ", 20, 120);
		titlefont.draw(sprite, " NOTESCOUNT : ", 300, 120);

		titlefont.setColor(Color.WHITE);
		if (current instanceof FolderBar) {
			if (config.isFolderlamp()) {
				int[] lamps = ((FolderBar) current).getLamps();
				int[] ranks = ((FolderBar) current).getRanks();
				int count = 0;
				for (int lamp : lamps) {
					count += lamp;
				}
				titlefont.draw(sprite, "TOTAL SONGS : " + count, 100, 500);
				titlefont.draw(sprite, "LAMP:", 36, 386);
				titlefont.draw(sprite, "RANK:", 36, 346);
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
				titlefont.draw(sprite, "TOTAL SONGS : " + count, 100, 500);
				titlefont.draw(sprite, "LAMP:", 36, 386);
				titlefont.draw(sprite, "RANK:", 36, 346);
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

		titlefont.draw(sprite, "MODE : " + MODE[mode], 20, 30);
		titlefont.draw(sprite, "SORT : " + SORT[sort].getName(), 180, 30);
		titlefont.draw(sprite, "LN MODE : " + LNMODE[config.getLnmode()], 440, 30);
		// banner
		if (current != bannerbar) {
			bannerbar = current;
			if (banner != null) {
				banner.dispose();
				banner = null;
			}
			if (bannerbar instanceof SongBar && ((SongBar) bannerbar).getBanner() != null) {
				banner = new Texture(((SongBar) bannerbar).getBanner());
			}
		}
		if (banner != null) {
			sprite.draw(banner, 400, 450, 300, 90);
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

		if (input.startPressed()) {
			option.render(keystate, keytime);
		} else if (input.isSelectPressed()) {
			aoption.render(keystate, keytime);
		} else if (input.getNumberState()[5]) {
			doption.render(keystate, keytime);
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
					if (resource.setBMSFile(new File(((SongBar) current).getSongData().getPath()), config, 0)) {
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
					if (resource.setBMSFile(new File(((SongBar) current).getSongData().getPath()), config, 1)) {
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
					if (resource.setBMSFile(new File(((SongBar) current).getSongData().getPath()), config,
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

		if (isPressed(keystate, keytime, KEY_UP, false) || cursor[1]) {
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
		} else if (isPressed(keystate, keytime, KEY_DOWN, false) || cursor[0]) {
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
			List<File> files = new ArrayList<File>();
			for (SongData song : ((GradeBar) bar.getSelected()).getSongDatas()) {
				files.add(new File(song.getPath()));
			}
			if (resource.setCourseBMSFiles(files.toArray(new File[0]))) {
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

	@Override
	public int getClear() {
		if (bar.getSelected().getScore() != null) {
			return bar.getSelected().getScore().getClear();
		}
		return Integer.MIN_VALUE;
	}

	public int getScore() {
		if (bar.getSelected().getScore() != null) {
			return bar.getSelected().getScore().getExscore();
		}
		return Integer.MIN_VALUE;
	}

	public int getMaxcombo() {
		if (bar.getSelected().getScore() != null) {
			return bar.getSelected().getScore().getCombo();
		}
		return Integer.MIN_VALUE;
	}

	public int getMisscount() {
		if (bar.getSelected().getScore() != null) {
			return bar.getSelected().getScore().getMinbp();
		}
		return Integer.MIN_VALUE;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
	}

	public int getMinBPM() {
		if (bar.getSelected() instanceof SongBar) {
			((SongBar) bar.getSelected()).getSongData().getMinbpm();
		}
		return Integer.MIN_VALUE;
	}

	public int getMaxBPM() {
		if (bar.getSelected() instanceof SongBar) {
			((SongBar) bar.getSelected()).getSongData().getMaxbpm();
		}
		return Integer.MIN_VALUE;
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
		}
		return super.getNumberValue(id);
	}

	public String getTextValue(int id) {
		switch (id) {
		case STRING_TITLE:
			return bar.getSelected().getTitle();
		case STRING_SUBTITLE:
			if (bar.getSelected() instanceof SongBar) {
				return ((SongBar) bar.getSelected()).getSongData().getSubtitle();
			}
			return "";
		case STRING_FULLTITLE:
			if (bar.getSelected() instanceof SongBar) {
				return bar.getSelected().getTitle() + " " + ((SongBar) bar.getSelected()).getSongData().getSubtitle();
			}
			return bar.getSelected().getTitle();
		case STRING_GENRE:
			if (bar.getSelected() instanceof SongBar) {
				return ((SongBar) bar.getSelected()).getSongData().getGenre();
			}
			return "";
		case STRING_ARTIST:
			if (bar.getSelected() instanceof SongBar) {
				return ((SongBar) bar.getSelected()).getSongData().getArtist();
			}
			return "";
		case STRING_SUBARTIST:
			if (bar.getSelected() instanceof SongBar) {
				return ((SongBar) bar.getSelected()).getSongData().getSubartist();
			}
			return "";
		case STRING_DIRECTORY:
			StringBuffer str = new StringBuffer();
			for (Bar b : dir) {
				str.append(b.getTitle() + " > ");
			}
			return str.toString();
		}
		return "";
	}

	PlayerResource getResource() {
		return getMainController().getPlayerResource();
	}

	SongDatabaseAccessor getSongDatabase() {
		return songdb;
	}

	@Override
	public float getSliderValue(int id) {
		if (id == MainState.SLIDER_MUSICSELECT_POSITION) {
			return bar.getSelectedPosition();
		}
		return 0;
	}

}
