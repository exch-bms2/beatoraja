package bms.player.beatoraja.select;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.MusicSelectorInputProcessor;
import bms.player.lunaticrave2.FolderData;
import bms.player.lunaticrave2.IRScoreData;
import bms.player.lunaticrave2.LunaticRave2ScoreDatabaseManager;
import bms.player.lunaticrave2.LunaticRave2SongDatabaseManager;
import bms.player.lunaticrave2.SongData;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends ApplicationAdapter {

	// TODO オプション選択
	// TODO スコア取得、閲覧
	// TODO フォルダの実装(LR2DBアクセサ側も要変更)

	private MainController main;

	private BitmapFont titlefont;

	private MusicSelectorInputProcessor input;

	private Bar[] currentsongs;
	private IRScoreData[] currentscores;
	private int selectedindex;
	private List<String> dir = new ArrayList();

	private long duration;
	/**
	 * 楽曲DBアクセサ
	 */
	private LunaticRave2SongDatabaseManager songdb;
	/**
	 * スコアDBアクセサ
	 */
	private LunaticRave2ScoreDatabaseManager scoredb;

	private static final String[] LAMP = { "000000", "808080", "800080",
			"ff00ff", "40ff40", "f0c000", "ffffff", "ffff88", "88ffff",
			"ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED",
			"ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR", "CLEAR",
			"HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private static final String[] SCOREOP = { "OFF", "MIRROR", "RANDOM",
			"R-RANDOM", "S-RANDOM", "H-RANDOM", "ALL-SCR", "RANDOM-EX",
			"S-RANDOM-EX" };

	private static final String[] GAUGEOP = { "ASSIST EASY", "EASY", "NORMAL",
			"HARD", "EX-HARD", "HAZARD" };

	private Config config;

	public MusicSelector(MainController main, Config config) {
		this.main = main;
		this.config = config;
		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new LunaticRave2ScoreDatabaseManager(new File(".")
					.getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable("Player");
			Logger.getGlobal().info("スコアデータベース接続");
			songdb = new LunaticRave2SongDatabaseManager(
					new File("song.db").getPath(), true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		songdb.createTable();
	}

	public void create() {
		updateBar("e2977170");
		input = new MusicSelectorInputProcessor(this);

	}

	public void render() {
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float w = 1280;
		final float h = 720;

		// 背景描画
		// sprite.begin();
		// sprite.draw(skin.getBackground(), 0, 0, w, h);
		// sprite.end();

		// draw song bar
		final float barh = 30;
		for (int i = 0; i < h / barh; i++) {
			int index = (int) (selectedindex + currentsongs.length * 100 + i - h
					/ barh / 2)
					% currentsongs.length;
			Bar sd = currentsongs[index];
			int x = 720;
			if (i == h / barh / 2) {
				x = 700;
			}
			shape.begin(ShapeType.Filled);
			if (sd instanceof FolderBar) {
				shape.setColor(Color.valueOf("606000"));
			}
			if (sd instanceof SongBar) {
				shape.setColor(Color.valueOf("006000"));
			}
			shape.rect(x, i * barh, 560, barh - 1);
			shape.end();
			shape.begin(ShapeType.Line);
			shape.setColor(Color.valueOf("888888"));
			shape.rect(x, i * barh, 560, barh - 1);
			shape.end();
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 20, (i + 1) * barh);
			sprite.end();

			if (currentscores[index] != null) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf(LAMP[currentscores[index]
						.getClear()]));
				shape.rect(x, i * barh, 15, barh - 1);
				shape.end();
			}

		}

		sprite.begin();
		if (currentsongs[selectedindex] instanceof SongBar) {
			SongData song = ((SongBar) currentsongs[selectedindex])
					.getSongData();
			titlefont.draw(sprite, song.getTitle() + " " + song.getSubtitle(),
					100, 600);
			titlefont.draw(sprite,
					song.getArtist() + " " + song.getSubartist(), 100, 570);
			titlefont.draw(sprite, song.getMode() + " KEYS", 100, 530);
			titlefont.draw(sprite, "LEVEL : " + song.getLevel(), 100, 500);

			if (currentscores[selectedindex] != null) {
				IRScoreData score = currentscores[selectedindex];
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.draw(sprite, CLEAR[score.getClear()], 100, 420);
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, "EX-SCORE  : " + score.getExscore()
						+ " / " + (score.getNotes() * 2), 100, 390);
				titlefont.draw(sprite, "MISS COUNT: " + score.getMinbp(), 100,
						360);
			}
		}
		sprite.end();

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		if (keystate[7]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex++;
				duration = l + 500;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex++;
			}
		} else if (keystate[8]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex += currentsongs.length - 1;
				duration = l + 500;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex += currentsongs.length - 1;
			}
		} else {
			duration = 0;
		}
		selectedindex = selectedindex % currentsongs.length;

		if (keystate[0] && keytime[0] != 0) {
			keytime[0] = 0;
			if (currentsongs[selectedindex] instanceof FolderBar) {
				FolderData fd = ((FolderBar) currentsongs[selectedindex])
						.getFolderData();
				String path = fd.getPath();
				if (path.endsWith(String.valueOf(File.separatorChar))) {
					path = path.substring(0, path.length() - 1);
				}
				if (updateBar(songdb.crc32(path, new String[0],
						new File(".").getAbsolutePath()))) {
					dir.add(fd.getParent());
				}
			} else if (currentsongs[selectedindex] instanceof SongBar) {
				main.setAuto(0);
				PlayerResource resource = new PlayerResource();
				resource.setBMSFile(new File(
						((SongBar) currentsongs[selectedindex]).getSongData()
								.getPath()), config, 0);
				main.changeState(MainController.STATE_DECIDE, resource);
			}
		}

		if (input.startPressed()) {
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				config.setRandom(config.getRandom() + 1 < SCOREOP.length ? config
						.getRandom() + 1 : 0);
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setRandom(config.getGauge() + 1 < GAUGEOP.length ? config
						.getGauge() + 1 : 0);
			}
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.BLACK);
			shape.rect(100, 200, 400, 400);
			shape.end();
			shape.begin(ShapeType.Line);
			shape.setColor(Color.WHITE);
			shape.rect(100, 200, 400, 400);
			shape.rect(150, 250, 55, 95);
			shape.rect(180, 350, 55, 95);
			shape.rect(210, 250, 55, 95);
			shape.rect(240, 350, 55, 95);
			shape.rect(270, 250, 55, 95);
			shape.rect(300, 350, 55, 95);
			shape.rect(330, 250, 55, 95);
			shape.end();

			sprite.begin();
			titlefont.draw(sprite, SCOREOP[config.getRandom()], 110, 490);
			titlefont.draw(sprite, GAUGEOP[config.getGauge()], 200, 520);
			sprite.end();
		} else {
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				if (dir.size() > 0) {
					String crc = dir.get(dir.size() - 1);
					updateBar(crc);
					dir.remove(dir.size() - 1);
				}
			}

			if (keystate[4]) {
				if (currentsongs[selectedindex] instanceof SongBar) {
					PlayerResource resource = new PlayerResource();
					resource.setBMSFile(new File(
							((SongBar) currentsongs[selectedindex])
									.getSongData().getPath()), config, 1);
					main.changeState(MainController.STATE_DECIDE, resource);
				}
			}
			if (keystate[6]) {
				if (currentsongs[selectedindex] instanceof SongBar) {
					PlayerResource resource = new PlayerResource();
					resource.setBMSFile(new File(
							((SongBar) currentsongs[selectedindex])
									.getSongData().getPath()), config, 2);
					main.changeState(MainController.STATE_DECIDE, resource);
				}
			}
		}
	}

	public boolean updateBar(String crc) {
		Logger.getGlobal().info("crc :" + crc);
		FolderData[] folders = songdb.getFolderDatas("parent", crc, new File(
				".").getAbsolutePath());
		SongData[] songs = songdb.getSongDatas("parent", crc,
				new File(".").getAbsolutePath());
		List<Bar> l = new ArrayList();
		if (songs.length == 0) {
			for (FolderData folder : folders) {
				l.add(new FolderBar(folder));
			}
		} else {
			for (SongData song : songs) {
				l.add(new SongBar(song));
			}
		}
		if (l.size() > 0) {
			currentsongs = l.toArray(new Bar[0]);
			selectedindex = 0;

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
				}
			}
			parameter.characters = str.toString();
			titlefont = generator.generateFont(parameter);

			currentscores = new IRScoreData[currentsongs.length];
			List<String> hashes = new ArrayList();
			for (int i = 0; i < currentscores.length; i++) {
				if (currentsongs[i] instanceof SongBar) {
					hashes.add(((SongBar) currentsongs[i]).getSongData()
							.getHash());
				}
			}
			Map<String, IRScoreData> m = scoredb.getScoreDatas("Player",
					hashes.toArray(new String[0]), false);
			for (int i = 0; i < currentscores.length; i++) {
				if (currentsongs[i] instanceof SongBar) {
					currentscores[i] = m.get(((SongBar) currentsongs[i])
							.getSongData().getHash());
				} else {
					currentscores[i] = null;
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

abstract class Bar {

	public abstract String getTitle();
}

class SongBar extends Bar {

	private SongData song;

	public SongBar(SongData song) {
		this.song = song;
	}

	public SongData getSongData() {
		return song;
	}

	@Override
	public String getTitle() {
		return song.getTitle();
	}
}

class FolderBar extends Bar {

	private FolderData folder;

	public FolderBar(FolderData folder) {
		this.folder = folder;
	}

	public FolderData getFolderData() {
		return folder;
	}

	@Override
	public String getTitle() {
		return folder.getTitle();
	}
}
