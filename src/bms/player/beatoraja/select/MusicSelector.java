package bms.player.beatoraja.select;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import bms.model.TimeLine;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.MainController.PlayerResource;
import bms.player.beatoraja.input.MusicSelectorInputProcessor;
import bms.player.lunaticrave2.FolderData;
import bms.player.lunaticrave2.IRScoreData;
import bms.player.lunaticrave2.LunaticRave2ScoreDatabaseManager;
import bms.player.lunaticrave2.LunaticRave2SongDatabaseManager;
import bms.player.lunaticrave2.SongData;
import bms.table.DifficultyTable;
import bms.table.DifficultyTable.Grade;
import bms.table.DifficultyTableElement;
import bms.table.DifficultyTableParser;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends ApplicationAdapter {

	// TODO オプション選択
	// TODO 7key, 9key, 14key切り替え(テンキー1)
	// TODO Barのスムーズスクロール

	private MainController main;

	private BitmapFont titlefont;

	private MusicSelectorInputProcessor input;

	private Bar[] currentsongs;
	private IRScoreData[] currentscores;
	private int selectedindex;
	private List<Bar> dir = new ArrayList<Bar>();

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

	private TableBar[] tables = new TableBar[0];

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

		List<TableBar> tables = new ArrayList<TableBar>();
		for (String url : config.getTableURL()) {
			DifficultyTableParser dtp = new DifficultyTableParser();
			DifficultyTable dt = new DifficultyTable();
			dt.setSourceURL(url);
			try {
				dtp.decode(true, dt);
				List<TableLevelBar> levels = new ArrayList<TableLevelBar>();
				for (String lv : dt.getLevelDescription()) {
					List<String> hashes = new ArrayList<String>();
					for (DifficultyTableElement dte : dt.getElements()) {
						if (lv.equals(dte.getDifficultyID())) {
							hashes.add(dte.getHash());
						}
					}
					levels.add(new TableLevelBar(lv, hashes
							.toArray(new String[0])));
				}
				GradeBar[] grades = new GradeBar[0];
				if(dt.getGrade() != null) {
					List<GradeBar> l = new ArrayList();
					for(Grade g: dt.getGrade()) {
						List<SongData> songlist = new ArrayList();
						for (String hash : g.getHashes()) {
							SongData[] songs = songdb.getSongDatas("hash", hash, new File(
									".").getAbsolutePath());
							if (songs.length > 0) {
								songlist.add(songs[0]);
							} else {
								songlist.add(null);
							}
						}

						l.add(new GradeBar(g.getName(), songlist.toArray(new SongData[0])));
					}
					grades = l.toArray(new GradeBar[0]);
				}
				tables.add(new TableBar(dt.getName(), levels
						.toArray(new TableLevelBar[0]),grades));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.tables = tables.toArray(new TableBar[0]);
	}

	public void create() {
		int index = selectedindex;
		if (dir.size() > 0) {
			updateBar(dir.get(dir.size() - 1));
		} else {
			updateBar(null);
		}
		selectedindex = index;
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
			if (sd instanceof TableBar) {
				shape.setColor(Color.valueOf("008080"));
			}
			if (sd instanceof TableLevelBar) {
				shape.setColor(Color.valueOf("4040c0"));
			}
			if (sd instanceof GradeBar) {
				shape.setColor(Color.valueOf("804000"));
			}
			if (sd instanceof FolderBar) {
				shape.setColor(Color.valueOf("606000"));
			}
			if (sd instanceof SongBar) {
				shape.setColor(Color.valueOf("006000"));
			}
			shape.rect(x, h - i * barh, 560, barh - 1);
			shape.end();
			shape.begin(ShapeType.Line);
			shape.setColor(Color.valueOf("888888"));
			shape.rect(x, h - i * barh, 560, barh - 1);
			shape.end();
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 20, h - (i - 1) * barh
					- 2);
			sprite.end();

			if (currentscores[index] != null) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf(LAMP[currentscores[index]
						.getClear()]));
				shape.rect(x, h - i * barh, 15, barh - 1);
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

		if (input.startPressed()) {
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				config.setRandom(config.getRandom() + 1 < SCOREOP.length ? config
						.getRandom() + 1 : 0);
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setGauge(config.getGauge() + 1 < GAUGEOP.length ? config
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
			// 1鍵 (選曲 or フォルダを開く)
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				if (currentsongs[selectedindex] instanceof FolderBar
						|| currentsongs[selectedindex] instanceof TableBar
						|| currentsongs[selectedindex] instanceof TableLevelBar) {
					Bar bar = currentsongs[selectedindex];
					if (updateBar(bar)) {
						dir.add(bar);
					}
				} else if (currentsongs[selectedindex] instanceof SongBar) {
					main.setAuto(0);
					PlayerResource resource = new PlayerResource();
					resource.setBMSFile(new File(
							((SongBar) currentsongs[selectedindex])
									.getSongData().getPath()), config, 0);
					main.changeState(MainController.STATE_DECIDE, resource);
				} else if (currentsongs[selectedindex] instanceof GradeBar) {
					if(((GradeBar) currentsongs[selectedindex]).existsAllSongs()) {
						main.setAuto(0);
						PlayerResource resource = new PlayerResource();
						List<File> files = new ArrayList<File>();
						for (SongData song : ((GradeBar) currentsongs[selectedindex])
								.getSongDatas()) {
							files.add(new File(song.getPath()));
						}
						resource.setBMSFile(files.get(0), config, 0);
						resource.setCourseBMSFiles(files.toArray(new File[0]));
						main.changeState(MainController.STATE_DECIDE, resource);						
					} else {
						Logger.getGlobal().info("段位の楽曲が揃っていません");
					}
				}
			}

			// 2鍵 (フォルダを閉じる)
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				Bar pbar = null;
				Bar cbar = null;
				if (dir.size() > 1) {
					pbar = dir.get(dir.size() - 2);
				}
				if (dir.size() > 0) {
					cbar = dir.get(dir.size() - 1);
					dir.remove(dir.size() - 1);
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

	public boolean updateBar(Bar bar) {
		String crc = null;
		List<Bar> l = new ArrayList();
		if (bar == null) {
			crc = "e2977170";
			l.addAll(Arrays.asList(tables));
		} else if (bar instanceof FolderBar) {
			crc = ((FolderBar) bar).getCRC();
		}
		if (crc != null) {
			Logger.getGlobal().info("crc :" + crc);
			FolderData[] folders = songdb.getFolderDatas("parent", crc,
					new File(".").getAbsolutePath());
			SongData[] songs = songdb.getSongDatas("parent", crc,
					new File(".").getAbsolutePath());
			if (songs.length == 0) {
				for (FolderData folder : folders) {
					String path = folder.getPath();
					if (path.endsWith(String.valueOf(File.separatorChar))) {
						path = path.substring(0, path.length() - 1);
					}
					l.add(new FolderBar(folder, songdb.crc32(path,
							new String[0], new File(".").getAbsolutePath())));
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
			List<SongBar> songbars = new ArrayList();
			for (String hash : ((TableLevelBar) bar).getHashes()) {
				SongData[] songs = songdb.getSongDatas("hash", hash, new File(
						".").getAbsolutePath());
				if (songs.length > 0) {
					songbars.add(new SongBar(songs[0]));
				}
			}
			l.addAll(songbars);
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
	private String crc;

	public FolderBar(FolderData folder, String crc) {
		this.folder = folder;
		this.crc = crc;
	}

	public FolderData getFolderData() {
		return folder;
	}

	public String getCRC() {
		return crc;
	}

	@Override
	public String getTitle() {
		return folder.getTitle();
	}
}

class TableBar extends Bar {

	private String name;
	private TableLevelBar[] levels;
	private GradeBar[] grades;

	public TableBar(String name, TableLevelBar[] levels, GradeBar[] grades) {
		this.name = name;
		this.levels = levels;
		this.grades = grades;
	}

	@Override
	public String getTitle() {
		return name;
	}

	public TableLevelBar[] getLevels() {
		return levels;
	}
	
	public GradeBar[] getGrades() {
		return grades;
	}

}

class TableLevelBar extends Bar {
	private String level;
	private String[] hashes;

	public TableLevelBar(String level, String[] hashes) {
		this.level = level;
		this.hashes = hashes;
	}

	@Override
	public String getTitle() {
		return "LEVEL " + level;
	}

	public String[] getHashes() {
		return hashes;
	}
}

class GradeBar extends Bar {

	private SongData[] songs;
	private String name;

	public GradeBar(String name, SongData[] songs) {
		this.songs = songs;
		this.name = name;
	}

	public SongData[] getSongDatas() {
		return songs;
	}

	@Override
	public String getTitle() {
		return name;
	}
	
	public boolean existsAllSongs() {
		for(SongData song : songs) {
			if(song == null) {
				return false;
			}
		}
		return true;
	}
}
