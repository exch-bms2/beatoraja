package bms.player.beatoraja.select;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.TableData;
import bms.player.lunaticrave2.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Json;

/**
 * 選曲部分。 楽曲一覧とカーソルが指す楽曲のステータスを表示し、選択した楽曲を 曲決定部分に渡す。
 * 
 * @author exch
 */
public class MusicSelector extends ApplicationAdapter {

	// TODO フォルダランプ
	// TODO 詳細オプション(BGA ON/OFF、JUDGE TIMING、等

	private MainController main;

	private BitmapFont titlefont;

	private Bar[] currentsongs;
	private int selectedindex;
	private List<Bar> dir = new ArrayList<Bar>();

	private long duration;
	private int angle;
	/**
	 * 楽曲DBアクセサ
	 */
	private LunaticRave2SongDatabaseManager songdb;
	/**
	 * スコアDBアクセサ
	 */
	private LunaticRave2ScoreDatabaseManager scoredb;

	private int mode;

	private static final String[] MODE = { "ALL", "7 KEY", "14 KEY", "9 KEY", "5 KEY", "10 KEY" };

	private int sort;

	private static final String[] SORT = { "Default", "CLEAR LAMP", "MISS COUNT" };

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private static final String[] SCOREOP = { "OFF", "MIRROR", "RANDOM", "R-RANDOM", "S-RANDOM", "SPIRAL", "H-RANDOM",
			"ALL-SCR", "RANDOM-EX", "S-RANDOM-EX" };

	private static final String[] GAUGEOP = { "ASSIST EASY", "EASY", "NORMAL", "HARD", "EX-HARD", "HAZARD" };

	private static final String[] FIXHISPEEDOP = { "OFF", "STARTBPM", "MAXBPM", "MAINBPM" };

	private Config config;

	private PlayerResource resource;

	private TableBar[] tables = new TableBar[0];

	private Sound bgm;
	private Sound move;
	private Sound folderopen;
	private Sound folderclose;
	private Sound sorts;
	
	private Texture background;

	public MusicSelector(MainController main, Config config) {
		this.main = main;
		this.config = config;
		try {
			Class.forName("org.sqlite.JDBC");
			scoredb = new LunaticRave2ScoreDatabaseManager(new File(".").getAbsoluteFile().getParent(), "/", "/");
			scoredb.createTable("Player");
			Logger.getGlobal().info("スコアデータベース接続");
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
					tables.add(new TableBar(td.getName(), levels.toArray(new TableLevelBar[0]),
							l.toArray(new GradeBar[0])));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.tables = tables.toArray(new TableBar[0]);
		}
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		if (this.resource == null) {
			this.resource = new PlayerResource();
		}
		int index = selectedindex;
		if (dir.size() > 0) {
			updateBar(dir.get(dir.size() - 1));
		} else {
			updateBar(null);
		}
		selectedindex = index;

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
		if(folderopen == null) {
			if (new File("skin/folder_open.wav").exists()) {
				folderopen = Gdx.audio.newSound(Gdx.files.internal("skin/folder_open.wav"));
			}			
		}
		if(folderclose == null) {
			if (new File("skin/folder_close.wav").exists()) {
				folderclose = Gdx.audio.newSound(Gdx.files.internal("skin/folder_close.wav"));
			}			
		}
		if(sorts == null) {
			if (new File("skin/sort.wav").exists()) {
				sorts = Gdx.audio.newSound(Gdx.files.internal("skin/sort.wav"));
			}			
		}

		
		if (background == null) {
			if (new File("skin/select.png").exists()) {
				background = new Texture("skin/select.png");
			}
		}

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
		if (background != null) {
			sprite.begin();
			sprite.draw(background, 0, 0, w, h);
			sprite.end();
		}

		// draw song bar
		final float barh = 36;
		for (int i = 0; i < h / barh + 2; i++) {
			int index = (int) (selectedindex + currentsongs.length * 100 + i - h / barh / 2) % currentsongs.length;
			Bar sd = currentsongs[index];
			int x = 720;
			if (i == h / barh / 2) {
				x = 700;
			}
			shape.begin(ShapeType.Filled);
			float y = h - i * barh;
			if (duration != 0) {
				long time = System.currentTimeMillis();
				float dy = barh * (Math.abs(angle) - duration + time) / angle + (angle >= 0 ? -1 : 1) * barh;
				y += dy;
			}
			shape.setColor(Color.valueOf("222222"));
			shape.rect(x + 4, y - 4, 560, barh - 6);

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
			shape.rect(x, y, 560, barh - 6);
			shape.end();
			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 20, y + barh - 12);
			sprite.end();

			if (currentsongs[index].getScore() != null) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf(LAMP[currentsongs[index].getScore().getClear()]));
				shape.rect(x, y, 15, barh - 6);
				shape.end();
			}

		}

		sprite.begin();
		if (currentsongs[selectedindex] instanceof SongBar) {
			SongData song = ((SongBar) currentsongs[selectedindex]).getSongData();
			titlefont.draw(sprite, song.getTitle() + " " + song.getSubtitle(), 100, 600);
			titlefont.draw(sprite, song.getArtist() + " " + song.getSubartist(), 100, 570);
			titlefont.draw(sprite, song.getMode() + " KEYS", 100, 530);
			titlefont.draw(sprite, "LEVEL : " + song.getLevel(), 100, 500);

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
		}
		titlefont.draw(sprite, "MODE : " + MODE[mode], 20, 30);
		titlefont.draw(sprite, "SORT : " + SORT[sort], 220, 30);
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
			if(sorts != null) {
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
			if(sorts != null) {
				sorts.play();
			}
		}

		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		if (keystate[7]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex++;
				if(move != null) {
					move.play();
				}
				duration = l + 300;
				angle = 300;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex++;
				if(move != null) {
					move.play();
				}
				angle = 50;
			}
		} else if (keystate[8]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				selectedindex += currentsongs.length - 1;
				if(move != null) {
					move.play();
				}
				duration = l + 300;
				angle = -300;
			}
			if (l > duration) {
				duration = l + 50;
				selectedindex += currentsongs.length - 1;
				if(move != null) {
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
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				config.setRandom(config.getRandom() + 1 < SCOREOP.length ? config.getRandom() + 1 : 0);
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setGauge(config.getGauge() + 1 < GAUGEOP.length ? config.getGauge() + 1 : 0);
			}
			if (keystate[6] && keytime[6] != 0) {
				keytime[6] = 0;
				config.setFixhispeed(
						config.getFixhispeed() + 1 < FIXHISPEEDOP.length ? config.getFixhispeed() + 1 : 0);
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
			titlefont.draw(sprite, FIXHISPEEDOP[config.getFixhispeed()], 300, 220);
			sprite.end();
		} else if (input.isSelectPressed()) {
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				config.setExpandjudge(!config.isExpandjudge());
			}
			if (keystate[1] && keytime[1] != 0) {
				keytime[1] = 0;
				config.setConstant(!config.isConstant());
			}
			if (keystate[3] && keytime[3] != 0) {
				keytime[3] = 0;
				config.setLnassist(config.getLnassist() == 0 ? 1 : 0);
			}
			if (keystate[5] && keytime[5] != 0) {
				keytime[5] = 0;
				config.setBpmguide(!config.isBpmguide());
			}
			if (keystate[6] && keytime[6] != 0) {
				keytime[6] = 0;
				config.setNomine(!config.isNomine());
			}
			shape.begin(ShapeType.Filled);
			shape.setColor(Color.BLACK);
			shape.rect(100, 200, 400, 400);
			shape.end();
			shape.begin(ShapeType.Line);
			shape.setColor(Color.CYAN);
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
			
			titlefont.setColor(config.isConstant() ? Color.WHITE : Color.valueOf("444444"));
			titlefont.draw(sprite, "CONSTANT", 110, 490);
			titlefont.setColor(config.getLnassist() == 1 ? Color.WHITE : Color.valueOf("444444"));
			titlefont.draw(sprite, "LEGACY NOTE", 200, 520);
			titlefont.setColor(config.isBpmguide() ? Color.WHITE : Color.valueOf("444444"));			
			titlefont.draw(sprite, "BPM GUIDE", 300, 490);
			titlefont.setColor(config.isExpandjudge() ? Color.WHITE : Color.valueOf("444444"));
			titlefont.draw(sprite, "EXPAND JUDGE", 90, 220);
			titlefont.setColor(config.isNomine() ? Color.WHITE : Color.valueOf("444444"));
			titlefont.draw(sprite, "NO MINE", 330, 220);
			sprite.end();
		} else {
			// 1鍵 (選曲 or フォルダを開く)
			if (keystate[0] && keytime[0] != 0) {
				keytime[0] = 0;
				if (currentsongs[selectedindex] instanceof FolderBar || currentsongs[selectedindex] instanceof TableBar
						|| currentsongs[selectedindex] instanceof TableLevelBar) {
					Bar bar = currentsongs[selectedindex];
					if (updateBar(bar)) {
						if(folderopen != null) {
							folderopen.play();							
						}
						dir.add(bar);
					}
				} else if (currentsongs[selectedindex] instanceof SongBar) {
					main.setAuto(0);
					resource.clear();
					resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 0);
					if (bgm != null) {
						bgm.stop();
					}
					main.changeState(MainController.STATE_DECIDE, resource);
				} else if (currentsongs[selectedindex] instanceof GradeBar) {
					if (((GradeBar) currentsongs[selectedindex]).existsAllSongs()) {
						main.setAuto(0);
						List<File> files = new ArrayList<File>();
						for (SongData song : ((GradeBar) currentsongs[selectedindex]).getSongDatas()) {
							files.add(new File(song.getPath()));
						}
						resource.clear();
						resource.setBMSFile(files.get(0), config, 0);
						resource.setCourseBMSFiles(files.toArray(new File[0]));
						if (bgm != null) {
							bgm.stop();
						}
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
					if(folderclose != null) {
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
					resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 1);
					if (bgm != null) {
						bgm.stop();
					}
					main.changeState(MainController.STATE_DECIDE, resource);
				}
			}
			if (keystate[6]) {
				if (currentsongs[selectedindex] instanceof SongBar) {
					resource.clear();
					resource.setBMSFile(new File(((SongBar) currentsongs[selectedindex]).getSongData().getPath()),
							config, 2);
					if (bgm != null) {
						bgm.stop();
					}
					main.changeState(MainController.STATE_DECIDE, resource);
				}
			}
		}
		if (input.isExitPressed()) {
			exit();
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

			List<String> hashes = new ArrayList();
			for (int i = 0; i < currentsongs.length; i++) {
				if (currentsongs[i] instanceof SongBar) {
					hashes.add(((SongBar) currentsongs[i]).getSongData().getHash());
				}
			}
			Map<String, IRScoreData> m = scoredb.getScoreDatas("Player", hashes.toArray(new String[0]), false);
			for (int i = 0; i < currentsongs.length; i++) {
				if (currentsongs[i] instanceof SongBar) {
					currentsongs[i].setScore(m.get(((SongBar) currentsongs[i]).getSongData().getHash()));
				}
			}
			Arrays.sort(currentsongs, this.getSortComparator());
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

	private Comparator<Bar> getSortComparator() {
		if (sort == 1) {
			return new Comparator<Bar>() {
				public int compare(Bar o1, Bar o2) {
					if (o1.getScore() == null && o2.getScore() == null) {
						return 0;
					}
					if (o1.getScore() == null) {
						return 1;
					}
					if (o2.getScore() == null) {
						return -1;
					}
					return o1.getScore().getClear() - o2.getScore().getClear();
				}

			};
		}
		if (sort == 2) {
			return new Comparator<Bar>() {
				public int compare(Bar o1, Bar o2) {
					if (o1.getScore() == null && o2.getScore() == null) {
						return 0;
					}
					if (o1.getScore() == null) {
						return 1;
					}
					if (o2.getScore() == null) {
						return -1;
					}
					return o1.getScore().getMinbp() - o2.getScore().getMinbp();
				}

			};
		}
		return new Comparator<Bar>() {

			public int compare(Bar o1, Bar o2) {
				// TODO Auto-generated method stub
				return 0;
			}

		};
	}
}

abstract class Bar {

	private IRScoreData score;

	public abstract String getTitle();

	public IRScoreData getScore() {
		return score;
	}

	public void setScore(IRScoreData score) {
		this.score = score;
	}
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
		for (SongData song : songs) {
			if (song == null) {
				return false;
			}
		}
		return true;
	}
}
