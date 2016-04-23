package bms.player.beatoraja.select;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.*;
import bms.player.lunaticrave2.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends ApplicationAdapter {

	// TODO フォルダランプ
	// TODO バナー、テキスト表示
	// TODO 詳細オプション(BGA ON/OFF、JUDGE TIMING、JUDGE DETAIL等

	private MainController main;

	private BitmapFont titlefont;

	/**
	 * 現在表示中のバー一覧
	 */
	private Bar[] currentsongs;
	/**
	 * 選択中のバーのインデックス
	 */
	private int selectedindex;
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
	private LunaticRave2SongDatabaseManager songdb;
    /**
     * 選択中のモードフィルタ
     */
	private int mode;

	private static final String[] MODE = { "ALL", "7 KEY", "14 KEY", "9 KEY", "5 KEY", "10 KEY" };
    /**
     * 選択中のソート
     */
	private int sort;

	private final BarSorter[] SORT = { BarSorter.NAME_SORTER, BarSorter.LEVEL_SORTER, BarSorter.LAMP_SORTER,
			BarSorter.SCORE_SORTER, BarSorter.MISSCOUNT_SORTER };

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private static final String[] RANK = { "F-", "F-", "F", "F", "F+", "F+", "E-", "E", "E+", "D-", "D", "D+", "C-",
			"C", "C+", "B-", "B", "B+", "A-", "A", "A+", "AA-", "AA", "AA+", "AAA-", "AAA", "AAA+" };

	private static final String[] LNMODE = { "LONG NOTE", "CHARGE NOTE", "HELL CHARGE NOTE" };

	private Config config;

	private PlayerResource resource;

	private TableBar[] tables = new TableBar[0];

	private MusicSelectSkin skin;

	private Sound bgm;
	private Sound move;
	private Sound folderopen;
	private Sound folderclose;
	private Sound sorts;

	private Texture background;

	private GameOptionRenderer option;
	private AssistOptionRenderer aoption;
	private DetailOptionRenderer doption;

	private long starttime;

	public MusicSelector(MainController main, Config config) {
		this.main = main;
		this.config = config;
		try {
			Class.forName("org.sqlite.JDBC");
			songdb = main.getSongDatabase();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		songdb.createTable();

		File dir = new File("table");
		if (dir.exists()) {
			List<TableBar> tables = new ArrayList<TableBar>();

			for (File f : dir.listFiles()) {
				try {
					Json json = new Json();
					TableData td = json.fromJson(TableData.class, new FileReader(f));
					List<TableLevelBar> levels = new ArrayList<TableLevelBar>();
					for (String lv : td.getLevel()) {
						levels.add(new TableLevelBar(lv, td.getHash().get(lv)));
					}
					List<GradeBar> l = new ArrayList();
					for (String s : td.getGrade()) {
						List<SongData> songlist = new ArrayList();
						for (String hash : td.getGradehash().get(s)) {
							SongData[] songs = songdb.getSongDatas("hash", hash, new File(".").getAbsolutePath());
							if (songs.length > 0) {
								songlist.add(songs[0]);
							} else {
								songlist.add(null);
							}
						}

						l.add(new GradeBar(s, songlist.toArray(new SongData[0])));
					}
					tables.add(new TableBar(td.getName(), levels.toArray(new TableLevelBar[0]), l
							.toArray(new GradeBar[0])));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.tables = tables.toArray(new TableBar[0]);
		}
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		if (dir.size() > 0) {
			updateBar(dir.get(dir.size() - 1));
		} else {
			updateBar(null);
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

		if (background == null) {
			if (new File("skin/select.png").exists()) {
				background = new Texture("skin/select.png");
			}
		}

		if (config.getLr2selectskin() != null) {
			try {
				skin = new LR2SelectSkinLoader().loadSelectSkin(new File(config.getLr2selectskin()),
						config.getLr2selectskinoption());
			} catch (IOException e) {
				e.printStackTrace();
				skin = new MusicSelectSkin();
			}

			// lr2playskin = "skin/spdframe/csv/left_ACwide.csv";

		} else {
			skin = new MusicSelectSkin();
		}

		option = new GameOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);
		aoption = new AssistOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);
		doption = new DetailOptionRenderer(main.getShapeRenderer(), main.getSpriteBatch(), titlefont, config);

		starttime = System.currentTimeMillis();
	}

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		BMSPlayerInputProcessor input = main.getInputProcessor();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		// 背景描画
		// if (background != null) {
		// sprite.begin();
		// sprite.draw(background, 0, 0, w, h);
		// sprite.end();
		// }

		final long time = System.currentTimeMillis() - starttime;

		sprite.begin();
		for (SkinImage part : skin.getSkinPart()) {
			int[] op = part.getOption();
			boolean draw = true;
			for (int option : op) {
				if (option != 0) {
					draw = false;
					break;
				}
			}
			if (part.getTiming() == 0 && draw) {
				Rectangle r = part.getDestination(time);
				if (r != null) {
					sprite.setColor(part.getColor(time));
					sprite.draw(part.getImage(time), r.x, r.y, r.width, r.height);
					sprite.setColor(Color.WHITE);
				}
			}
		}

		for (SkinNumber num : skin.getSkinNumbers()) {
			int value = -1;
			if (num.getId() == 71) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getExscore();
				}
			}
			if (num.getId() == 72) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getNotes() * 2;
				}
			}
			if (num.getId() == 76) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getMinbp();
				}
			}
			if (num.getId() == 77) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getPlaycount();
				}
			}
			if (num.getId() == 78) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getClearcount();
				}
			}
			if (num.getId() == 79) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getPlaycount()
							- currentsongs[selectedindex].getScore().getClearcount();
				}
			}
			if (num.getId() == 80) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getPg();
				}
			}
			if (num.getId() == 81) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getGr();
				}
			}
			if (num.getId() == 82) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getGd();
				}
			}
			if (num.getId() == 83) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getBd();
				}
			}
			if (num.getId() == 84) {
				if (currentsongs[selectedindex].getScore() != null) {
					value = currentsongs[selectedindex].getScore().getPr();
				}
			}

			if (value != -1) {
				Rectangle r = num.getDestination(time);
				int[] values = new int[num.getKeta()];
				for (int i = values.length - 1; i >= 0; i--) {
					values[i] = value % 10;
					value /= 10;
				}

				// sprite.setColor(num.getColor(time));
				for (int i = 0; i < values.length; i++) {
					sprite.draw(num.getImage()[values[i]], r.x + r.width * i, r.y, r.width, r.height);
				}
				// sprite.setColor(Color.WHITE);
			}
		}

		sprite.end();

		// draw song bar
		final float barh = 36;
		for (int i = 0; i < h / barh + 2; i++) {
			int index = (int) (selectedindex + currentsongs.length * 100 + i - h / barh / 2) % currentsongs.length;
			Bar sd = currentsongs[index];
			float x = w * 3 / 5;
			if (i == (int) (h / barh / 2)) {
				x -= 20;
			}
			sprite.begin();
			float y = h - i * barh;

			Sprite barimage = skin.getBar()[0];
			if (duration != 0) {
				float dy = barh * (Math.abs(angle) - duration + System.currentTimeMillis()) / angle
						+ (angle >= 0 ? -1 : 1) * barh;
				y += dy;
			}
			if (sd instanceof TableBar) {
				barimage = skin.getBar()[2];
			}
			if (sd instanceof TableLevelBar) {
				barimage = skin.getBar()[2];
			}
			if (sd instanceof GradeBar) {
				barimage = skin.getBar()[6];
			}
			if (sd instanceof FolderBar) {
				barimage = skin.getBar()[1];
			}
			if (sd instanceof SongBar) {
				barimage = skin.getBar()[0];
			}

			sprite.draw(barimage, x, y, w * 2 / 5, barh);
			titlefont.setColor(Color.BLACK);
			titlefont.draw(sprite, sd.getTitle(), x + 62, y + barh - 8);
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 60, y + barh - 6);
			sprite.end();

			if (sd instanceof GradeBar) {
				int lamp = -1;
				if (sd.getScore() != null) {
					lamp = sd.getScore().getClear();
				}
				GradeBar gb = (GradeBar) sd;
				if (gb.getMirrorScore() != null) {
					lamp = (lamp > gb.getMirrorScore().getClear()) ? lamp : gb.getMirrorScore().getClear();
				}
				if (lamp != -1) {
					// sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);
					sprite.begin();
					sprite.draw(skin.getLamp()[lamp].getKeyFrame(time / 1000f), x, y, 15, barh);
					sprite.end();
					// sprite.setBlendFunction(GL11.GL_SRC_ALPHA,
					// GL11.GL_ONE_MINUS_SRC_ALPHA);
				}
			} else {
				if (sd.getScore() != null && skin.getLamp()[sd.getScore().getClear()] != null) {
					// sprite.setBlendFunction(GL11.GL_ONE, GL11.GL_ONE);
					sprite.begin();
					sprite.draw(skin.getLamp()[sd.getScore().getClear()].getKeyFrame(time / 1000f), x, y, 15, barh);
					sprite.end();
					// sprite.setBlendFunction(GL11.GL_SRC_ALPHA,
					// GL11.GL_ONE_MINUS_SRC_ALPHA);
				}
			}

			if (sd instanceof SongBar) {
				SongData song = ((SongBar) sd).getSongData();
				sprite.begin();
				String level = String.format("%2d", song.getLevel());
				titlefont.setColor(Color.BLACK);
				titlefont.draw(sprite, level, x + 22, y + barh - 8);
				final Color[] difficulty = { Color.GRAY, Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED, Color.PURPLE };
				titlefont.setColor(song.getDifficulty() < difficulty.length ? difficulty[song.getDifficulty()]
						: Color.WHITE);
				titlefont.draw(sprite, level, x + 20, y + barh - 6);
				sprite.end();
				if (song.getLongnote() != 0) {
					shape.begin(ShapeType.Filled);
					shape.setColor(Color.valueOf("222200"));
					shape.rect(x - 36, y - 4, 30, barh - 6);
					shape.setColor(Color.YELLOW);
					shape.rect(x - 40, y, 30, barh - 6);
					shape.end();
					sprite.begin();
					titlefont.setColor(Color.BLACK);
					titlefont.draw(sprite, "LN", x - 36, y + barh - 12);
					sprite.end();
				}
			}
		}

		// draw song bar position
		Rectangle progress = skin.getSeekRegion();
		shape.begin(ShapeType.Line);
		shape.setColor(Color.WHITE);
		shape.rect(progress.x, progress.y, progress.width, progress.height);
		shape.end();
		shape.begin(ShapeType.Filled);
		shape.setColor(Color.BLACK);
		shape.rect(progress.x + 1, progress.y + 1, progress.width - 2, progress.height - 2);

		shape.setColor(Color.ORANGE);
		float dy = progress.y + 1 + (progress.height - 20) * (1.0f - (float) selectedindex / currentsongs.length);
		if (duration != 0) {
			dy -= (float) progress.height / currentsongs.length
					* (Math.abs(angle) - duration + System.currentTimeMillis()) / angle + (angle >= 0 ? -1 : 1)
					* (float) progress.height / currentsongs.length;
		}
		while (dy > progress.y + progress.height) {
			dy -= progress.height;
		}
		shape.rect(progress.x + 1, dy, progress.width - 2, 20);
		shape.end();

		sprite.begin();

		StringBuffer str = new StringBuffer();
		for (Bar b : dir) {
			str.append(b.getTitle() + " > ");
		}
		titlefont.setColor(Color.VIOLET);
		titlefont.draw(sprite, str.toString(), 40, 670);

		titlefont.setColor(Color.WHITE);
		if (currentsongs[selectedindex] instanceof SongBar) {
			SongData song = ((SongBar) currentsongs[selectedindex]).getSongData();
            titlefont.draw(sprite, song.getGenre(), 100, 630);
			titlefont.draw(sprite, song.getTitle() + " " + song.getSubtitle(), 100, 600);
			titlefont.draw(sprite, song.getArtist() + " " + song.getSubartist(), 100, 570);
			titlefont.draw(sprite, song.getMode() + " KEYS", 100, 530);
			titlefont.draw(sprite, "LEVEL : " + song.getLevel(), 100, 500);
			if (currentsongs[selectedindex].getScore() != null) {
				IRScoreData score = currentsongs[selectedindex].getScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.draw(sprite, CLEAR[score.getClear()], 100, 420);
				titlefont.setColor(Color.WHITE);
				titlefont.draw(
						sprite,
						"EX-SCORE  : " + score.getExscore() + " / " + (score.getNotes() * 2) + "    RANK : "
								+ RANK[(score.getExscore() * 27 / (score.getNotes() * 2))] + " ( "
								+ ((score.getExscore() * 1000 / (score.getNotes() * 2)) / 10.0f) + "% )", 100, 390);
				titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(), 100, 360);
				titlefont.draw(sprite, "CLEAR / PLAY : " + score.getClearcount() + " / " + score.getPlaycount(), 100,
						330);
			}
			if (((SongBar) currentsongs[selectedindex]).existsReplayData()) {
				titlefont.setColor(Color.GREEN);
				titlefont.draw(sprite, "7 KEY : Replay", 100, 270);
			}
		}
		// 段位用の表示(ミラー段位、EX段位)
		if (currentsongs[selectedindex] instanceof GradeBar) {
			GradeBar gb = (GradeBar) currentsongs[selectedindex];
			titlefont.draw(sprite, currentsongs[selectedindex].getTitle(), 100, 600);
			if (currentsongs[selectedindex].getScore() != null) {
				IRScoreData score = currentsongs[selectedindex].getScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.draw(sprite, CLEAR[score.getClear()], 100, 420);
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : " + score.getExscore() + " / " + (score.getNotes() * 2), 100, 390);
				titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(), 100, 360);
				titlefont.draw(sprite, "CLEAR / PLAY : " + score.getClearcount() + " / " + score.getPlaycount(), 100,
						330);
			}
			if (gb.getMirrorScore() != null) {
				IRScoreData score = gb.getMirrorScore();
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.draw(sprite, CLEAR[score.getClear()], 100, 270);
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : " + score.getExscore() + " / " + (score.getNotes() * 2), 100, 240);
				titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(), 100, 210);
				titlefont.draw(sprite, "CLEAR / PLAY : " + score.getClearcount() + " / " + score.getPlaycount(), 100,
						180);
			}
		}

		titlefont.setColor(Color.WHITE);
		if (currentsongs[selectedindex] instanceof FolderBar) {
			titlefont.draw(sprite, currentsongs[selectedindex].getTitle(), 100, 600);
		}

		if (currentsongs[selectedindex] instanceof TableBar) {
			titlefont.draw(sprite, currentsongs[selectedindex].getTitle(), 100, 600);
		}

		if (currentsongs[selectedindex] instanceof TableLevelBar) {
			titlefont.draw(sprite, currentsongs[selectedindex].getTitle(), 100, 600);
		}

		titlefont.draw(sprite, "MODE : " + MODE[mode], 20, 60);
		titlefont.draw(sprite, "SORT : " + SORT[sort].getName(), 220, 60);
		titlefont.draw(sprite, "LN MODE : " + LNMODE[config.getLnmode()], 20, 30);
		sprite.end();

		boolean[] numberstate = input.getNumberState();
		long[] numtime = input.getNumberTime();
		if (numberstate[1] && numtime[1] != 0) {
			// KEYフィルターの切り替え
			mode = (mode + 1) % MODE.length;
			numtime[1] = 0;
			if (dir.size() > 0) {
				updateBar(dir.get(dir.size() - 1));
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
				updateBar(dir.get(dir.size() - 1));
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
				updateBar(dir.get(dir.size() - 1));
			}
			if (sorts != null) {
				sorts.play();
			}
		}

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		boolean[] cursor = input.getCursorState();
		if (keystate[7] || cursor[1]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex++;
				if (move != null) {
					move.play();
				}
				duration = l + 300;
				angle = 300;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex++;
				if (move != null) {
					move.play();
				}
				angle = 50;
			}
		} else if (keystate[8] || cursor[0]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex += currentsongs.length - 1;
				if (move != null) {
					move.play();
				}
				duration = l + 300;
				angle = -300;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex += currentsongs.length - 1;
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
		selectedindex = selectedindex % currentsongs.length;

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
			if ((keystate[0] && keytime[0] != 0) || cursor[3]) {
				keytime[0] = 0;
				cursor[3] = false;
				if (currentsongs[selectedindex] instanceof FolderBar || currentsongs[selectedindex] instanceof TableBar
						|| currentsongs[selectedindex] instanceof TableLevelBar) {
					Bar bar = currentsongs[selectedindex];
					if (updateBar(bar)) {
						if (folderopen != null) {
							folderopen.play();
						}
						dir.add(bar);
					}
				} else if (currentsongs[selectedindex] instanceof SongBar) {
					main.setAuto(0);
					resource.clear();
					if (resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 0)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);
					}
				} else if (currentsongs[selectedindex] instanceof GradeBar) {
					if (((GradeBar) currentsongs[selectedindex]).existsAllSongs()) {
						main.setAuto(0);
						List<File> files = new ArrayList<File>();
						for (SongData song : ((GradeBar) currentsongs[selectedindex]).getSongDatas()) {
							files.add(new File(song.getPath()));
						}
						resource.clear();
						resource.setCoursetitle(((GradeBar) currentsongs[selectedindex]).getTitle());
						resource.setBMSFile(files.get(0), config, 0);
						resource.setCourseBMSFiles(files.toArray(new File[0]));
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);
					} else {
						Logger.getGlobal().info("段位の楽曲が揃っていません");
					}
				}
			}

			// 2鍵 (フォルダを閉じる)
			if ((keystate[1] && keytime[1] != 0) || cursor[2]) {
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
				updateBar(pbar);
				if (cbar != null) {
					for (int i = 0; i < currentsongs.length; i++) {
						if (currentsongs[i].getTitle().equals(cbar.getTitle())) {
							selectedindex = i;
							break;
						}
					}
				}
			}

			if (keystate[4]) {
				if (currentsongs[selectedindex] instanceof SongBar) {
					resource.clear();
					if (resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 1)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);

					}
				}
			}
			if (keystate[6]) {
				if (currentsongs[selectedindex] instanceof SongBar) {
					resource.clear();
					if (resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 2)) {
						if (bgm != null) {
							bgm.stop();
						}
						main.changeState(MainController.STATE_DECIDE);
					}
				}
			}
		}
		if (input.isExitPressed()) {
			exit();
		}
	}

	private boolean updateBar(Bar bar) {
		final Bar prevbar = currentsongs != null ? currentsongs[selectedindex] : null;
		String crc = null;
		List<Bar> l = new ArrayList<Bar>();
		if (bar == null) {
			crc = "e2977170";
			l.addAll(Arrays.asList(tables));
		} else if (bar instanceof FolderBar) {
			crc = ((FolderBar) bar).getCRC();
		}
		if (crc != null) {
			Logger.getGlobal().info("crc :" + crc);
			FolderData[] folders = songdb.getFolderDatas("parent", crc, new File(".").getAbsolutePath());
			SongData[] songs = songdb.getSongDatas("parent", crc, new File(".").getAbsolutePath());
			if (songs.length == 0) {
				for (FolderData folder : folders) {
					String path = folder.getPath();
					if (path.endsWith(String.valueOf(File.separatorChar))) {
						path = path.substring(0, path.length() - 1);
					}
					l.add(new FolderBar(folder, songdb.crc32(path, new String[0], new File(".").getAbsolutePath())));
				}
			} else {
				for (SongData song : songs) {
					l.add(new SongBar(song));
				}
			}
		}
		if (bar instanceof TableBar) {
			l.addAll((Arrays.asList(((TableBar) bar).getLevels())));
			l.addAll((Arrays.asList(((TableBar) bar).getGrades())));
		}
		if (bar instanceof TableLevelBar) {
			List<SongBar> songbars = new ArrayList<SongBar>();
			for (String hash : ((TableLevelBar) bar).getHashes()) {
				SongData[] songs = songdb.getSongDatas("hash", hash, new File(".").getAbsolutePath());
				if (songs.length > 0) {
					songbars.add(new SongBar(songs[0]));
				}
			}
			l.addAll(songbars);
		}

		List<Bar> remove = new ArrayList<Bar>();
		for (Bar b : l) {
			final int[] modes = { 0, 7, 14, 9, 5, 10 };
			if (modes[mode] != 0 && b instanceof SongBar && ((SongBar) b).getSongData().getMode() != modes[mode]) {
				remove.add(b);
			}
		}
		l.removeAll(remove);

		if (l.size() > 0) {
			// 変更前と同じバーがあればカーソル位置を保持する
			currentsongs = l.toArray(new Bar[0]);
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
					Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;

			StringBuffer str = new StringBuffer(parameter.characters);

			for (Bar song : currentsongs) {
				str.append(song.getTitle());
				if (song instanceof SongBar) {
					SongData s = ((SongBar) song).getSongData();
					str.append(s.getSubtitle());
					str.append(s.getArtist());
					str.append(s.getSubartist());
                    str.append(s.getGenre());
				}
			}

			if (bar != null) {
				str.append(bar.getTitle());
			}
			for (Bar b : dir) {
				str.append(b.getTitle());
			}

			parameter.characters = str.toString();
			titlefont = generator.generateFont(parameter);

			for (int i = 0; i < currentsongs.length; i++) {
				if (currentsongs[i] instanceof SongBar) {
					SongData sd = ((SongBar) currentsongs[i]).getSongData();
					currentsongs[i].setScore(main.getPlayDataAccessor().readScoreData(sd.getHash(),
							sd.getLongnote() == 1, config.getLnmode()));
					if (currentsongs[i].getScore() != null && config.getLnmode() == 2
							&& ((SongBar) currentsongs[i]).getSongData().getLongnote() == 1) {
						currentsongs[i].getScore().setClear(currentsongs[i].getScore().getExclear());
					}
					((SongBar) currentsongs[i]).setExistsReplayData(main.getPlayDataAccessor().existsReplayData(
							sd.getHash(), sd.getLongnote() == 1, config.getLnmode()));
				}
				if (currentsongs[i] instanceof GradeBar) {
					GradeBar gb = (GradeBar) currentsongs[i];
					if (gb.existsAllSongs()) {
						String hash = "";
						boolean ln = false;
						for (SongData sd : gb.getSongDatas()) {
							hash += sd.getHash();
							ln |= sd.getLongnote() == 1;
						}
						gb.setScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), false));
						if (gb.getScore() != null && config.getLnmode() == 2 && ln) {
							gb.getScore().setClear(gb.getScore().getExclear());
						}
						gb.setMirrorScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), true));
						if (gb.getMirrorScore() != null && config.getLnmode() == 2 && ln) {
							gb.getMirrorScore().setClear(gb.getMirrorScore().getExclear());
						}
					}
				}
			}
			Arrays.sort(currentsongs, SORT[sort]);

			selectedindex = 0;

			if (prevbar != null) {
				if (prevbar instanceof SongBar) {
					final SongBar prevsong = (SongBar) prevbar;
					for (int i = 0; i < currentsongs.length; i++) {
						if (currentsongs[i] instanceof SongBar
								&& ((SongBar) currentsongs[i]).getSongData().getHash()
										.equals(prevsong.getSongData().getHash())) {
							selectedindex = i;
							break;
						}
					}
				} else {
					for (int i = 0; i < currentsongs.length; i++) {
						if (currentsongs[i].getTitle().equals(prevbar.getTitle())) {
							selectedindex = i;
							break;
						}
					}

				}
			}
			return true;
		}
		Logger.getGlobal().warning("楽曲がありません");
		return false;
	}

	public void exit() {
		main.exit();
	}

	public void dispose() {
		titlefont.dispose();
	}

}
