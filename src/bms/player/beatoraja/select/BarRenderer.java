package bms.player.beatoraja.select;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

import bms.player.beatoraja.*;
import bms.player.beatoraja.TableData.CourseData;

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

		generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		File dir = new File("table");
		if (dir.exists()) {
			List<TableBar> tables = new ArrayList<TableBar>();

			for (File f : dir.listFiles()) {
				try {
					Json json = new Json();
					TableData td = json.fromJson(TableData.class, new FileReader(f));
					List<TableLevelBar> levels = new ArrayList<TableLevelBar>();
					for (String lv : td.getLevel()) {
						levels.add(new TableLevelBar(select, lv, td.getHash().get(lv)));
					}
					List<GradeBar> l = new ArrayList<GradeBar>();
					for (CourseData course : td.getCourse()) {
						List<SongData> songlist = new ArrayList<SongData>();
						for (String hash : course.getHash()) {
							SongData[] songs = songdb.getSongDatas("md5", hash, new File(".").getAbsolutePath());
							if (songs.length > 0) {
								songlist.add(songs[0]);
							} else {
								songlist.add(null);
							}
						}

						l.add(new GradeBar(course.getName(), songlist.toArray(new SongData[0]), course));
					}
					tables.add(new TableBar(select, td.getName(), levels.toArray(new TableLevelBar[0]), l
							.toArray(new GradeBar[0])));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.tables = tables.toArray(new TableBar[0]);
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
		return ((float)selectedindex) / currentsongs.length;
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

	public void render(SpriteBatch sprite, ShapeRenderer shape, MusicSelectSkin skin, float w, float h, long duration,
			int angle, int time) {
		if(skin == null) {
			return;
		}
		// draw song bar
		final float barh = 36;
		for (int i = 0; i < h / barh + 2; i++) {
			int index = (int) (selectedindex + currentsongs.length * 100 + i - h / barh / 2) % currentsongs.length;
			Bar sd = currentsongs[index];
			float x = w * 3 / 5;
			if (i == (int) ((h / barh + 1) / 2)) {
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
				barimage = skin.getBar()[((GradeBar) sd).existsAllSongs() ? 3 : 4];
			}
			if (sd instanceof FolderBar) {
				barimage = skin.getBar()[1];
			}
			if (sd instanceof SongBar) {
				barimage = skin.getBar()[0];
			}
			if (sd instanceof SearchWordBar) {
				barimage = skin.getBar()[6];
			}
			if (sd instanceof CommandBar) {
				barimage = skin.getBar()[5];
			}

			sprite.draw(barimage, x, y, w * 2 / 5, barh);
			titlefont.setColor(Color.BLACK);
			titlefont.draw(sprite, sd.getTitle(), x + 62, y + barh - 8);
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, sd.getTitle(), x + 60, y + barh - 6);
			sprite.end();

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
						if (TROPHY[j].equals(trophy.getName()) && skin.getTrophy()[j] != null) {
							sprite.begin();
							sprite.draw(skin.getTrophy()[j], x + 20, y + 4);
							sprite.end();
							break;
						}
					}
				}
			}

			if (skin.getLamp()[sd.getLamp()] != null) {
				sprite.begin();
				sprite.draw(skin.getLamp()[sd.getLamp()].getKeyFrame(time / 1000f), x, y + 2, 15, barh - 2);
				sprite.end();
			}

			if (sd instanceof SongBar) {
				SongData song = ((SongBar) sd).getSongData();
				sprite.begin();
				String level = String.format("%2d", song.getLevel());
				titlefont.setColor(Color.BLACK);
				titlefont.draw(sprite, level, x + 22, y + barh - 8);
				final Color[] difficulty = { Color.GRAY, Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED, Color.PURPLE };
				titlefont
						.setColor(song.getDifficulty() >= 0 && song.getDifficulty() < difficulty.length ? difficulty[song
								.getDifficulty()] : Color.WHITE);
				titlefont.draw(sprite, level, x + 20, y + barh - 6);
				sprite.end();

				flag |= song.getFeature();
			}

			// LN
			if ((flag & 1) != 0) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf("222200"));
				shape.rect(x - 36, y, 30, barh - 6);
				shape.setColor(Color.YELLOW);
				shape.rect(x - 40, y + 4, 30, barh - 6);
				shape.end();
				sprite.begin();
				titlefont.setColor(Color.BLACK);
				titlefont.draw(sprite, "LN", x - 36, y + barh - 8);
				sprite.end();
			}
			// MINE
			if ((flag & 2) != 0) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf("222200"));
				shape.rect(x - 70, y, 30, barh - 6);
				shape.setColor(Color.PURPLE);
				shape.rect(x - 74, y + 4, 30, barh - 6);
				shape.end();
				sprite.begin();
				titlefont.setColor(Color.BLACK);
				titlefont.draw(sprite, "MI", x - 70, y + barh - 8);
				sprite.end();
			}
			// RANDOM
			if ((flag & 4) != 0) {
				shape.begin(ShapeType.Filled);
				shape.setColor(Color.valueOf("222200"));
				shape.rect(x - 104, y, 30, barh - 6);
				shape.setColor(Color.GREEN);
				shape.rect(x - 108, y + 4, 30, barh - 6);
				shape.end();
				sprite.begin();
				titlefont.setColor(Color.BLACK);
				titlefont.draw(sprite, "RA", x - 104, y + barh - 8);
				sprite.end();
			}
		}

		// move song bar position by mouse
		if (main.getInputProcessor().isMouseConsumed()) {
			main.getInputProcessor().setMouseConsumed();
			Rectangle progress = new Rectangle(skin.getSeekRegion());
			progress.x -= progress.width * 2;
			progress.width *= 5;
			if (progress.contains(main.getInputProcessor().getMouseX(), main.getInputProcessor().getMouseY())) {
				selectedindex = (int) ((main.getInputProcessor().getMouseY() - progress.y) * 0.999 / progress.height * currentsongs.length);
			}
		}
	}

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
			currentsongs = l.toArray(new Bar[0]);
			FreeTypeFontParameter parameter = new FreeTypeFontParameter();
			parameter.size = 24;

			StringBuilder str = new StringBuilder(parameter.characters);

			for (Bar song : currentsongs) {
				str.append(song.getTitle());
				if (song instanceof SongBar) {
					SongData s = ((SongBar) song).getSongData();
					str.append(s.getSubtitle());
					str.append(s.getArtist());
					str.append(s.getSubartist());
					str.append(s.getGenre());
				}
				if (song instanceof GradeBar) {
					for (SongData sd : ((GradeBar) song).getSongDatas()) {
						if (sd != null) {
							str.append(sd.getTitle());
						}
					}

					for (TableData.TrophyData tr : ((GradeBar) song).getAllTrophy()) {
						str.append(tr.getName());
					}
				}
			}

			if (bar != null) {
				str.append(bar.getTitle());
			}
			for (Bar b : select.getDir()) {
				str.append(b.getTitle());
			}

			parameter.characters = str.toString();
			if (titlefont != null) {
				titlefont.dispose();
			}
			titlefont = generator.generateFont(parameter);
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
		if(titlefont != null) {
			titlefont.dispose();
			titlefont = null;
		}
		for(String path : bannermap.keySet()) {
			if(bannermap.get(path) != null) {
				bannermap.get(path).dispose();
			}
		}
		bannermap.clear();
	}

    private void setBanner(SongBar songbar) {
    	SongData song = songbar.getSongData();
        File bannerfile = new File(song.getPath().substring(0, song.getPath().lastIndexOf(File.separatorChar) + 1)
                + song.getBanner());
        // System.out.println(bannerfile.getPath());
        if (song.getBanner().length() > 0 && bannerfile.exists()) {
            try {
            	if(bannermap.containsKey(bannerfile.getPath())) {
            		songbar.setBanner(bannermap.get(bannerfile.getPath()));
            	} else {
            		Pixmap pixmap = new Pixmap(Gdx.files.internal(bannerfile.getPath()));
                    songbar.setBanner(pixmap);
                    bannermap.put(bannerfile.getPath(), pixmap);
            	}
            } catch (GdxRuntimeException e) {
        		bannermap.put(bannerfile.getParent(), null);
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
					bar.setScore(select.readScoreData(sd, config.getLnmode()));
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
