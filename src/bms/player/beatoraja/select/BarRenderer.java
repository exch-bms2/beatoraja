package bms.player.beatoraja.select;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.SkinNumber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.*;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;

/**
 * 楽曲バー描画用クラス
 * 
 * @author exch
 */
public class BarRenderer {

	private MainController main;

	private MusicSelector select;

	private BarContentsLoaderThread loader;

	/**
	 * 現在表示中のバー一覧
	 */
	private Bar[] currentsongs;
	/**
	 * 選択中のバーのインデックス
	 */
	private int selectedindex;

	private CommandBar[] commands;
	private TableBar[] tables = new TableBar[0];
	private List<SearchWordBar> search = new ArrayList<SearchWordBar>();

	private FreeTypeFontGenerator generator;
	private BitmapFont titlefont;

	private final String[] TROPHY = { "goldmedal", "silvermedal", "bronzemedal" };

	private Map<String, Pixmap> bannermap = new HashMap<String, Pixmap>();

	public BarRenderer(MainController main, MusicSelector select, SongDatabaseAccessor songdb) {
		this.main = main;
		this.select = select;

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/default/VL-Gothic-Regular.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = 24;
		titlefont = generator.generateFont(parameter);
		try {
			Files.createDirectory(Paths.get("table"));
		} catch (Exception e) {

		}
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("table"))) {
			List<TableBar> tables = new ArrayList<TableBar>();
			for (Path p : paths) {
				Json json = new Json();
				TableData td = json.fromJson(TableData.class, new FileReader(p.toFile()));
				tables.add(new TableBar(select, td));
			}
			this.tables = tables.toArray(new TableBar[0]);
		} catch (IOException e) {

		}

		commands = new CommandBar[] {
				new CommandBar(main, select, "MY BEST", "playcount > 0 ORDER BY playcount DESC LIMIT 10"),
				new CommandBar(main, select, "FULL COMBO", "clear >= 8"),
				new CommandBar(main, select, "EX HARD CLEAR", "clear = 7"),
				new CommandBar(main, select, "HARD CLEAR", "clear = 6"),
				new CommandBar(main, select, "CLEAR", "clear = 5"),
				new CommandBar(main, select, "EASY CLEAR", "clear = 4"),
				new CommandBar(main, select, "ASSIST CLEAR", "clear IN (2, 3)"),
				new CommandBar(main, select, "RANK AAA", "(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 88.88"),
				new CommandBar(main, select, "RANK AA",
						"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 77.77 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 88.88"),
				new CommandBar(main, select, "RANK A",
						"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 66.66 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 77.77"), };
	}

	public void updateTableBar(TableBar tb) {

	}

	public Bar getSelected() {
		return currentsongs[selectedindex];
	}

	public void setSelected(Bar bar) {
		for (int i = 0; i < currentsongs.length; i++) {
			if (currentsongs[i].getTitle().equals(bar.getTitle())) {
				selectedindex = i;
				break;
			}
		}
	}

	public float getSelectedPosition() {
		return ((float) selectedindex) / currentsongs.length;
	}

	public void setSelectedPosition(float value) {
		if (value >= 0 && value < 1) {
			selectedindex = (int) (currentsongs.length * value);
		}
	}

	public void move(boolean inclease) {
		if (inclease) {
			selectedindex++;
		} else {
			selectedindex += currentsongs.length - 1;
		}
		selectedindex = selectedindex % currentsongs.length;
	}

	public void addSearch(SearchWordBar bar) {
		search.add(bar);
	}

	public void mousePressed(SkinBar baro, int x, int y) {
		for (int i = 0; i < 60; i++) {
			boolean on = (i == ((MusicSelectSkin) select.getSkin()).getCenterBar());
			if (baro.getBarImages(on, i) == null) {
				continue;
			}
			int index = (int) (selectedindex + currentsongs.length * 100 + i -  ((MusicSelectSkin) select.getSkin()).getCenterBar())
					% currentsongs.length;
			Bar sd = currentsongs[index];

			Rectangle r = baro.getBarImages(on, i).getDestination(select.getNowTime(), select);
			if (r != null) {
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					select.select(sd);
					break;
				}
			}
		}

	}

	public void render(SpriteBatch sprite, ShapeRenderer shape, MusicSelectSkin skin, SkinBar baro, float w, float h,
			long duration, int angle, int time) {
		if (skin == null) {
			return;
		}

		if (bartextupdate) {
			bartextupdate = false;
			Set<Character> charset = new HashSet<Character>();

			for (Bar song : currentsongs) {
				for (char c : song.getTitle().toCharArray()) {
					charset.add(c);
				}
			}

			char[] chars = new char[charset.size()];
			Character[] chars2 = charset.toArray(new Character[0]);
			for (int i = 0; i < chars.length; i++) {
				chars[i] = chars2[i];
			}
			baro.getBarText()[0].setText(String.valueOf(chars));
			baro.getBarText()[1].setText(String.valueOf(chars));
		}
		// draw song bar
		for (int i = 0; i < 60; i++) {
			boolean on = (i == skin.getCenterBar());
			if (baro.getBarImages(on, i) == null) {
				continue;
			}
			int index = (int) (selectedindex + currentsongs.length * 100 + i - skin.getCenterBar())
					% currentsongs.length;
			Bar sd = currentsongs[index];

			int value = -1;
			if (sd instanceof TableBar) {
				value = 2;
			}
			if (sd instanceof TableLevelBar) {
				value = 2;
			}
			if (sd instanceof GradeBar) {
				value = ((GradeBar) sd).existsAllSongs() ? 3 : 4;
			}
			if (sd instanceof FolderBar) {
				value = 1;
			}
			if (sd instanceof SongBar) {
				value = 0;
			}
			if (sd instanceof SearchWordBar) {
				value = 6;
			}
			if (sd instanceof CommandBar) {
				value = 5;
			}

			float dy = 0;
			Rectangle r = baro.getBarImages(on, i).getDestination(time, select);
			if (r != null) {

				if (duration != 0) {
					dy = r.height * (Math.abs(angle) - duration + System.currentTimeMillis()) / angle
							+ (angle >= 0 ? -1 : 1) * r.height;
				}
				final int y = (int) (r.y + dy + (baro.getPosition() == 1 ? r.height : 0));
				if (value != -1) {
					sprite.begin();
					TextureRegion barimage = baro.getBarImages(on, i).getImage(value, time, select);
					baro.getBarImages(on, i).draw(sprite, time, select, value, 0, (int) dy);
					// TODO 新規追加曲はテキストを変える
					baro.getBarText()[0].draw(sprite, time, select, sd.getTitle(), (int) r.x, y);
					sprite.end();
				}

				int flag = 0;

				if (sd instanceof GradeBar) {
					GradeBar gb = (GradeBar) sd;
					if (gb.existsAllSongs()) {
						for (SongData song : gb.getSongDatas()) {
							flag |= song.getFeature();
						}
					}
					// trophy
					TableData.TrophyData trophy = gb.getTrophy();
					if (trophy != null) {
						for (int j = 0; j < TROPHY.length; j++) {
							if (TROPHY[j].equals(trophy.getName()) && baro.getTrophy()[j] != null) {
								sprite.begin();
								sprite.draw(baro.getTrophy()[j].getImage(time, select), r.x + 20, y + 4);
								sprite.end();
								break;
							}
						}
					}
				}

				if (baro.getLamp()[sd.getLamp()] != null) {
					sprite.begin();
					baro.getLamp()[sd.getLamp()].draw(sprite, time, select, (int) r.x, y);
					sprite.end();
				}

				if (sd instanceof SongBar) {
					sprite.begin();
					SongData song = ((SongBar) sd).getSongData();

					SkinNumber leveln = baro.getBarlevel()[song.getDifficulty() >= 0 && song.getDifficulty() < 7 ? song
							.getDifficulty() : 0];
					if (leveln != null) {
						leveln.draw(sprite, time, song.getLevel(), select, (int) r.x, y);
					}
					sprite.end();

					// String level = String.format("%2d", song.getLevel());
					// titlefont.setColor(Color.BLACK);
					// titlefont.draw(sprite, level, r.x + 22, r.y + dy +
					// r.height - 8);
					// final Color[] difficulty = { Color.GRAY, Color.GREEN,
					// Color.BLUE, Color.YELLOW, Color.RED, Color.PURPLE };
					// titlefont
					// .setColor(song.getDifficulty() >= 0 &&
					// song.getDifficulty() < difficulty.length ?
					// difficulty[song
					// .getDifficulty()] : Color.WHITE);
					// titlefont.draw(sprite, level, r.x + 20, r.y + dy +
					// r.height - 6);

					flag |= song.getFeature();
				}

				// LN
				if ((flag & 1) != 0) {
					shape.begin(ShapeType.Filled);
					shape.setColor(Color.valueOf("222200"));
					shape.rect(r.x - 36, r.y + dy, 30, r.height - 6);
					shape.setColor(Color.YELLOW);
					shape.rect(r.x - 40, r.y + dy + 4, 30, r.height - 6);
					shape.end();
					sprite.begin();
					titlefont.setColor(Color.BLACK);
					titlefont.draw(sprite, "LN", r.x - 36, r.y + dy + r.height - 8);
					sprite.end();
				}
				// MINE
				if ((flag & 2) != 0) {
					shape.begin(ShapeType.Filled);
					shape.setColor(Color.valueOf("222200"));
					shape.rect(r.x - 70, r.y + dy, 30, r.height - 6);
					shape.setColor(Color.PURPLE);
					shape.rect(r.x - 74, r.y + dy + 4, 30, r.height - 6);
					shape.end();
					sprite.begin();
					titlefont.setColor(Color.BLACK);
					titlefont.draw(sprite, "MI", r.x - 70, r.y + dy + r.height - 8);
					sprite.end();
				}
				// RANDOM
				if ((flag & 4) != 0) {
					shape.begin(ShapeType.Filled);
					shape.setColor(Color.valueOf("222200"));
					shape.rect(r.x - 104, r.y + dy, 30, r.height - 6);
					shape.setColor(Color.GREEN);
					shape.rect(r.x - 108, r.y + dy + 4, 30, r.height - 6);
					shape.end();
					sprite.begin();
					titlefont.setColor(Color.BLACK);
					titlefont.draw(sprite, "RA", r.x - 104, r.y + dy + r.height - 8);
					sprite.end();
				}
			}
		}
	}

	private boolean bartextupdate = false;

	public boolean updateBar(Bar bar) {
		final Bar prevbar = currentsongs != null ? currentsongs[selectedindex] : null;
		List<Bar> l = new ArrayList<Bar>();
		if (bar == null) {
			l.addAll(Arrays.asList(new FolderBar(select, null, "e2977170").getChildren()));
			l.addAll(Arrays.asList(tables));
			l.addAll(Arrays.asList(commands));
			l.addAll(search);
		} else if (bar instanceof DirectoryBar) {
			l.addAll(Arrays.asList(((DirectoryBar) bar).getChildren()));
		}

		List<Bar> remove = new ArrayList<Bar>();
		for (Bar b : l) {
			final int[] modes = { 0, 7, 14, 9, 5, 10 };
			if (modes[select.getMode()] != 0 && b instanceof SongBar
					&& ((SongBar) b).getSongData().getMode() != modes[select.getMode()]) {
				remove.add(b);
			}
		}
		l.removeAll(remove);

		if (l.size() > 0) {
			// 変更前と同じバーがあればカーソル位置を保持する
			currentsongs = l.toArray(new Bar[l.size()]);
			bartextupdate = true;

			final Config config = select.getResource().getConfig();
			for (Bar b : currentsongs) {
				if (b instanceof SongBar) {
					SongData sd = ((SongBar) b).getSongData();
					if (select.existsScoreDataCache(sd, config.getLnmode())) {
						b.setScore(select.readScoreData(sd, config.getLnmode()));
					}
				}
			}

			Arrays.sort(currentsongs, MusicSelector.SORT[select.getSort()]);

			selectedindex = 0;

			if (prevbar != null) {
				if (prevbar instanceof SongBar) {
					final SongBar prevsong = (SongBar) prevbar;
					for (int i = 0; i < currentsongs.length; i++) {
						if (currentsongs[i] instanceof SongBar
								&& ((SongBar) currentsongs[i]).getSongData().getSha256()
										.equals(prevsong.getSongData().getSha256())) {
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
			if (loader != null) {
				loader.stopRunning();
			}
			loader = new BarContentsLoaderThread(currentsongs);
			loader.start();
			return true;
		}
		Logger.getGlobal().warning("楽曲がありません");
		return false;

	}

	public void dispose() {
		if (titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		for (String path : bannermap.keySet()) {
			if (bannermap.get(path) != null) {
				bannermap.get(path).dispose();
			}
		}
		bannermap.clear();
	}

	private void setBanner(SongBar songbar) {
		SongData song = songbar.getSongData();
		Path bannerfile = Paths.get(song.getPath()).getParent().resolve(song.getBanner());
		// System.out.println(bannerfile.getPath());
		if (song.getBanner().length() > 0 && Files.exists(bannerfile)) {
			try {
				if (bannermap.containsKey(bannerfile.toString())) {
					songbar.setBanner(bannermap.get(bannerfile.toString()));
				} else {
					Pixmap pixmap = new Pixmap(Gdx.files.internal(bannerfile.toString()));
					songbar.setBanner(pixmap);
					bannermap.put(bannerfile.toString(), pixmap);
				}
			} catch (GdxRuntimeException e) {
				bannermap.put(bannerfile.toString(), null);
				Logger.getGlobal().warning("banner読み込み失敗: " + e.getMessage());
			}
		}
	}

	/**
	 * 選曲バー内のスコアデータ等を読み込むためのスレッド
	 */
	class BarContentsLoaderThread extends Thread {

		/**
		 * データ読み込み対象の選曲バー
		 */
		private Bar[] bars;
		/**
		 * 読み込み終了フラグ
		 */
		private boolean stop = false;

		public BarContentsLoaderThread(Bar[] bar) {
			this.bars = bar;
		}

		@Override
		public void run() {
			Config config = select.getResource().getConfig();
			for (Bar bar : bars) {
				if (bar instanceof SongBar) {
					SongData sd = ((SongBar) bar).getSongData();
					if (bar.getScore() == null) {
						bar.setScore(select.readScoreData(sd, config.getLnmode()));
					}
					boolean[] replay = new boolean[MusicSelector.REPLAY];
					for (int i = 0; i < MusicSelector.REPLAY; i++) {
						replay[i] = main.getPlayDataAccessor().existsReplayData(sd.getSha256(), sd.hasLongNote(),
								config.getLnmode(), i);
					}
					((SongBar) bar).setExistsReplayData(replay);
				}
				if (bar instanceof GradeBar) {
					GradeBar gb = (GradeBar) bar;
					if (gb.existsAllSongs()) {
						String[] hash = new String[gb.getSongDatas().length];
						boolean ln = false;
						for (int j = 0; j < gb.getSongDatas().length; j++) {
							hash[j] = gb.getSongDatas()[j].getSha256();
							ln |= gb.getSongDatas()[j].hasLongNote();
						}
						gb.setScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 0,
								gb.getConstraint()));
						gb.setMirrorScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 1,
								gb.getConstraint()));
						gb.setRandomScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 2,
								gb.getConstraint()));
						boolean[] replay = new boolean[MusicSelector.REPLAY];
						for (int i = 0; i < MusicSelector.REPLAY; i++) {
							replay[i] = main.getPlayDataAccessor().existsReplayData(hash, ln, config.getLnmode(), i,
									gb.getConstraint());
						}
						gb.setExistsReplayData(replay);
					}
				}

				if (config.isFolderlamp()) {
					if (bar instanceof FolderBar) {
						((FolderBar) bar).updateFolderStatus();
					}
					if (bar instanceof TableLevelBar) {
						((TableLevelBar) bar).updateFolderStatus();
					}
				}
				if (stop) {
					break;
				}
			}
			for (Bar bar : bars) {
				if (bar instanceof SongBar) {
					setBanner((SongBar) bar);
				}
				if (stop) {
					break;
				}
			}
		}

		/**
		 * データ読み込みを中断する
		 */
		public void stopRunning() {
			stop = true;
		}
	}
}
