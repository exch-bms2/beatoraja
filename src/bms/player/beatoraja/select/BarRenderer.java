package bms.player.beatoraja.select;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.TableData.TableFolder;
import bms.player.beatoraja.TableData.TableSong;
import bms.player.beatoraja.external.BMSSearchAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongInformationAccessor;

/**
 * 楽曲バー描画用クラス
 * 
 * @author exch
 */
public class BarRenderer {

	private MusicSelector select;

	private BarContentsLoaderThread loader;

	/**
	 * 現在のフォルダ階層
	 */
	private Deque<DirectoryBar> dir = new ArrayDeque<DirectoryBar>();
	/**
	 * 現在表示中のバー一覧
	 */
	private Bar[] currentsongs;
	/**
	 * 選択中のバーのインデックス
	 */
	private int selectedindex;

	private Bar[] commands;

	private GradeBar[] courses = new GradeBar[0];

	private HashBar[] favorites = new HashBar[0];
	/**
	 * 難易度表バー一覧
	 */
	private TableBar[] tables = new TableBar[0];
	/**
	 * 検索結果バー一覧
	 */
	private List<SearchWordBar> search = new ArrayList<SearchWordBar>();

	private final String[] TROPHY = { "goldmedal", "silvermedal", "bronzemedal" };

	private final int SEARCHBAR_MAXCOUNT = 10;

	private PixmapResourcePool banners = new PixmapResourcePool();

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

	public BarRenderer(MusicSelector select) {
		final MainController main = select.getMainController();
		this.select = select;

		TableData[] tds = new TableDataAccessor().readAll();
		
		Array<TableBar> table = new Array<TableBar>();
		for (int i = 0; i < tds.length; i++) {
			table.add(new TableBar(select, tds[i]));
		}
		BMSSearchAccessor bmssearcha = new BMSSearchAccessor();
		table.add(new TableBar(select, bmssearcha.getTableData()));
		
		this.tables = table.toArray(TableBar.class);
		
		CourseData[] cds = new CourseDataAccessor("course").readAll();
		courses = new GradeBar[cds.length];
		for (int i = 0; i < cds.length; i++) {
			Set<String> hashset = new HashSet<String>();
			for (String hash : cds[i].getHash()) {
				hashset.add(hash);
			}
			SongData[] songs = select.getSongDatabase().getSongDatas(hashset.toArray(new String[hashset.size()]));

			SongData[] songdatas = new SongData[cds[i].getHash().length];
			for (int j = 0;j < songdatas.length;j++) {
				String hash = cds[i].getHash()[j];
				for(SongData sd : songs) {
					if(hash.equals(sd.getMd5()) || hash.equals(sd.getSha256())) {
						songdatas[j] = sd;
						break;
					}
				}
			}
			this.courses[i] = new GradeBar(cds[i].getName(), songdatas, cds[i]);
		}
		cds = new CourseDataAccessor("favorite").readAll();
//		if(cds.length == 0) {
//			cds = new CourseData[1];
//			cds[0] = new CourseData();
//			cds[0].setName("FAVORITE");
//		}
		favorites = new HashBar[cds.length];
		for (int i = 0; i < cds.length; i++) {
			TableData.TableSong[] songs = new TableData.TableSong[cds[i].getHash().length];
			for(int j = 0;j < songs.length;j++) {
				songs[j] = new TableData.TableSong(cds[i].getHash()[j]);
			}
			favorites[i] = new HashBar(select, cds[i].getName(), songs);
		}
		
		List<CommandBar> density = new ArrayList<CommandBar>();
		int i = 1;
		density.add(new CommandBar(main, select, "DENSITY < " + i, "density < 1", 2));
		for(;i < 50;i++) {
			density.add(new CommandBar(main, select, "DENSITY " + i + " - " + (i + 1), "density >= " + i + " AND density < " + (i + 1), 2));
		}
		density.add(new CommandBar(main, select, "DENSITY >= " + i, "density >= " + i, 2));
		
		commands = new Bar[] {
				new CommandBar(main, select, "MY BEST", "playcount > 0 ORDER BY playcount DESC LIMIT 10"),
				new ContainerBar("CLEAR TYPE", new Bar[]{new CommandBar(main, select, "FULL COMBO", "clear >= 8"),
						new CommandBar(main, select, "EX HARD CLEAR", "clear = 7"),
						new CommandBar(main, select, "HARD CLEAR", "clear = 6"),
						new CommandBar(main, select, "CLEAR", "clear = 5"),
						new CommandBar(main, select, "EASY CLEAR", "clear = 4"),
						new CommandBar(main, select, "ASSIST CLEAR", "clear IN (2, 3)"),
						new CommandBar(main, select, "FAILED", "clear = 1")}),	
				new ContainerBar("SCORE RANK", new Bar[]{new CommandBar(main, select, "RANK AAA", 
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 88.88"),
						new CommandBar(main, select, "RANK AA",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 77.77 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 88.88"),
						new CommandBar(main, select, "RANK A",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 66.66 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 77.77"),
						new CommandBar(main, select, "RANK B",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 55.55 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 66.66"),
						new CommandBar(main, select, "RANK C",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 44.44 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 55.55"),
						new CommandBar(main, select, "RANK D",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 33.33 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 44.44"),
						new CommandBar(main, select, "RANK E",
								"(lpg * 2 + epg * 2 + lgr + egr) * 50 / notes >= 22.22 AND (lpg * 2 + epg * 2 + lgr + egr) * 50 / notes < 33.33"),
						}),
				new ContainerBar("FEATURE", new Bar[]{
						new CommandBar(main, select, "SCRATCH 10 - 20%", "(n + ln) <= (s + ls) * 9 AND (n + ln) > (s + ls) * 4 ", 2),
						new CommandBar(main, select, "SCRATCH > 20%", "(n + ln) <= (s + ls) * 4 ", 2),
						new CommandBar(main, select, "LONG NOTE 10 - 20%", "(n + s) <= (ln + ls) * 9 AND (n + s) > (ln + ls) * 4 ", 2),
						new CommandBar(main, select, "LONG NOTE > 20%", "(n + s) <= (ln + ls) * 4 ", 2),
						}),	
				new ContainerBar("DENSITY", density.toArray(new Bar[density.size()])),
				new CommandBar(main, select, "FAVORITE SONG", "favorite & 1 != 0", 1),
				new CommandBar(main, select, "FAVORITE CHART", "favorite & 2 != 0", 1),
				};
	}

	public Bar getSelected() {
		return currentsongs != null ? currentsongs[selectedindex] : null;
	}

	public void setSelected(Bar bar) {
		for (int i = 0; i < currentsongs.length; i++) {
			if (currentsongs[i].getTitle().equals(bar.getTitle())) {
				selectedindex = i;
				select.getScoreDataProperty().update(currentsongs[selectedindex].getScore());
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
		select.getScoreDataProperty().update(currentsongs[selectedindex].getScore());
	}

	public void move(boolean inclease) {
		if (inclease) {
			selectedindex++;
		} else {
			selectedindex += currentsongs.length - 1;
		}
		selectedindex = selectedindex % currentsongs.length;
		select.getScoreDataProperty().update(currentsongs[selectedindex].getScore());
	}

	public void close() {
		if(dir.isEmpty()) {
			return;
		}
		
		final DirectoryBar current = dir.removeLast();
		final DirectoryBar parent = !dir.isEmpty() ? dir.getLast() : null;
		dir.addLast(current);
		updateBar(parent);
		select.play(MusicSelector.SOUND_FOLDERCLOSE);
	}

	public void addSearch(SearchWordBar bar) {
		for (SearchWordBar s : search) {
			if (s.getTitle().equals(bar.getTitle())) {
				search.remove(s);
				break;
			}
		}
		if (search.size() >= SEARCHBAR_MAXCOUNT) {
			search.remove(0);
		}
		search.add(bar);
	}

	public boolean mousePressed(SkinBar baro, int button, int x, int y) {
		for (int i : ((MusicSelectSkin) select.getSkin()).getClickableBar()) {
			boolean on = (i == ((MusicSelectSkin) select.getSkin()).getCenterBar());
			if (baro.getBarImages(on, i) == null) {
				continue;
			}
			int index = (int) (selectedindex + currentsongs.length * 100 + i
					- ((MusicSelectSkin) select.getSkin()).getCenterBar()) % currentsongs.length;
			Bar sd = currentsongs[index];

			Rectangle r = baro.getBarImages(on, i).getDestination(select.getNowTime(), select);
			if (r != null) {
				if (r != null && r.x <= x && r.x + r.width >= x && r.y <= y && r.y + r.height >= y) {
					if (button == 0) {
						select.select(sd);
					} else {
						close();
					}
					return true;
				}
			}
		}
		return false;
	}

	public void render(SpriteBatch sprite, MusicSelectSkin skin, SkinBar baro, int time) {
		if (skin == null) {
			return;
		}

		if (bartextupdate && baro.getText()[0] instanceof SkinTextFont) {
			bartextupdate = false;
			Set<Character> charset = new HashSet<Character>();

			for (Bar song : currentsongs) {
				for (char c : song.getTitle().toCharArray()) {
					charset.add(c);
				}
			}

			char[] chars = new char[charset.size()];
			int i = 0;
			for (char c : charset) {
				chars[i++] = c;
			}
			((SkinTextFont) baro.getText()[0]).prepareFont(String.valueOf(chars));
			((SkinTextFont) baro.getText()[1]).prepareFont(String.valueOf(chars));
		}
		// draw song bar
		for (int i = 0; i < 60; i++) {
			boolean on = (i == skin.getCenterBar());
			if (baro.getBarImages(on, i) == null) {
				continue;
			}
			int index = (selectedindex + currentsongs.length * 100 + i - skin.getCenterBar())
					% currentsongs.length;
			Bar sd = currentsongs[index];

			int value = -1;
			if (sd instanceof TableBar) {
				value = 2;
			} else if (sd instanceof HashBar) {
				value = 2;
			} else if (sd instanceof GradeBar) {
				value = ((GradeBar) sd).existsAllSongs() ? 3 : 4;
			} else if (sd instanceof FolderBar) {
				value = 1;
			} else if (sd instanceof SongBar) {
				value = ((SongBar) sd).existsSong() ? 0 : 4;
			} else if (sd instanceof SearchWordBar) {
				value = 6;
			} else if (sd instanceof CommandBar || sd instanceof ContainerBar) {
				value = 5;
			}

			float dx = 0;
			float dy = 0;
			Rectangle r = baro.getBarImages(on, i).getDestination(time, select);
			if (r != null) {
				if (duration != 0) {
					int nextindex = i + (angle >= 0 ? 1 : -1);
					SkinImage si = nextindex >= 0 ? baro.getBarImages(nextindex == skin.getCenterBar(), nextindex)
							: null;
					Rectangle r2 = si != null ? si.getDestination(time, select) : null;
					if (r2 != null) {
						final float a = angle < 0 ? ((float) (System.currentTimeMillis() - duration)) / angle
								: ((float) (duration - System.currentTimeMillis())) / angle;
						dx = (r2.x - r.x) * a;
						dy = (r2.y - r.y) * a;
					}
				}
				final int x = (int) (r.x + dx);
				final int y = (int) (r.y + dy + (baro.getPosition() == 1 ? r.height : 0));
				if (value != -1) {
					baro.getBarImages(on, i).draw(sprite, time, select, value, (int) dx, (int) dy);
					// 新規追加曲はテキストを変える
					int songstatus = 0;
					if (sd instanceof SongBar) {
						SongData song = ((SongBar) sd).getSongData();
						songstatus = song == null || System.currentTimeMillis() / 1000 > song.getAdddate() + 3600 * 24 ? 0 : 1;
					}
					baro.getText()[songstatus].setText(sd.getTitle());
					baro.getText()[songstatus].draw(sprite, time, select, x, y);
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
					TrophyData trophy = gb.getTrophy();
					if (trophy != null) {
						for (int j = 0; j < TROPHY.length; j++) {
							if (TROPHY[j].equals(trophy.getName()) && baro.getTrophy()[j] != null) {
								baro.getTrophy()[j].draw(sprite, time, select, x, y);
								break;
							}
						}
					}
				}

				if (baro.getLamp()[sd.getLamp()] != null) {
					baro.getLamp()[sd.getLamp()].draw(sprite, time, select, x, y);
				}

				if (sd instanceof SongBar && ((SongBar) sd).existsSong()) {
					SongData song = ((SongBar) sd).getSongData();

					SkinNumber leveln = baro.getBarlevel()[song.getDifficulty() >= 0 && song.getDifficulty() < 7
							? song.getDifficulty() : 0];
					if (leveln != null) {
						leveln.draw(sprite, time, song.getLevel(), select, x, y);
					}
					flag |= song.getFeature();
				}

				// LN
				if ((flag & SongData.FEATURE_LONGNOTE) != 0 && baro.getLabel()[0] != null) {
					baro.getLabel()[0].draw(sprite, time, select, x, y);
				}
				// MINE
				if ((flag & SongData.FEATURE_MINENOTE) != 0 && baro.getLabel()[2] != null) {
					baro.getLabel()[2].draw(sprite, time, select, x, y);
				}
				// RANDOM
				if ((flag & SongData.FEATURE_RANDOM) != 0 && baro.getLabel()[1] != null) {
					baro.getLabel()[1].draw(sprite, time, select, x, y);
				}
			}
		}

	}

	public void input() {
		BMSPlayerInputProcessor input = select.getMainController().getInputProcessor();
		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		boolean[] cursor = input.getCursorState();

		// song bar scroll on mouse wheel
		int mov = -input.getScroll();
		input.resetScroll();
		// song bar scroll
		if (select.isPressed(keystate, keytime, MusicSelectInputProcessor.KEY_UP, false) || cursor[1]) {
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
		} else if (select.isPressed(keystate, keytime, MusicSelectInputProcessor.KEY_DOWN, false) || cursor[0]) {
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

		while(mov > 0) {
			move(true);
			select.play(MusicSelector.SOUND_SCRATCH);
			mov--;
		}
		while(mov < 0) {
			move(false);
			select.play(MusicSelector.SOUND_SCRATCH);
			mov++;
		}

		if(input.getFunctionstate()[7] && input.getFunctiontime()[7] != 0) {
			input.getFunctiontime()[7] = 0;
			if(getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) getSelected()).getSongData();

				if(sd != null) {
					boolean enable = ((sd.getFavorite() & SongData.FAVORITE_SONG) == 0);
					SongData[] songs = select.getSongDatabase().getSongDatas("folder", sd.getFolder());
					for(SongData song : songs) {
						song.setFavorite(enable ? song.getFavorite() | SongData.FAVORITE_SONG : song.getFavorite() & (0xffffffff ^ SongData.FAVORITE_SONG));
					}
					select.getSongDatabase().setSongDatas(songs);
				}
			}
		}
		if(input.getFunctionstate()[8] && input.getFunctiontime()[8] != 0) {
			input.getFunctiontime()[8] = 0;
			if(getSelected() instanceof SongBar) {
				SongData sd = ((SongBar) getSelected()).getSongData();

				if(sd != null) {
					sd.setFavorite(sd.getFavorite() ^ SongData.FAVORITE_CHART);
					select.getSongDatabase().setSongDatas(new SongData[]{sd});
//					boolean exist = false;
//					for(TableData.TableSongData element : favorites[0].getElements()) {
//						if(element.getHash().equals(sd.getSha256())) {
//							exist = true;
//							break;
//						}
//					}
//					if(!exist) {
//						List<TableData.TableSongData> l = new ArrayList(Arrays.asList(favorites[0].getElements()));
//						l.add(new TableData.TableSongData(sd.getSha256()));
//						favorites[0].setElements(l.toArray(new TableData.TableSongData[l.size()]));
//						Logger.getGlobal().info("favorite追加 : " + sd.getTitle());
//					}
				}
			}
		}
	}

	public void resetInput() {
		long l = System.currentTimeMillis();
		if (l > duration) {
			duration = 0;
		}
	}

	private boolean bartextupdate = false;

	public Deque<DirectoryBar> getDirectory() {
		return dir;
	}

	public boolean updateBar() {
		if (dir.size() > 0) {
			return updateBar(dir.getLast());
		}
		return updateBar(null);
	}

	public boolean updateBar(Bar bar) {
		Bar prevbar = currentsongs != null ? currentsongs[selectedindex] : null;
		List<Bar> l = new ArrayList<Bar>();
		if (bar == null) {
			if (!dir.isEmpty()) {
				prevbar = dir.getFirst();
			}
			dir.clear();
			l.addAll(Arrays.asList(new FolderBar(select, null, "e2977170").getChildren()));
			l.add(new ContainerBar("COURSE", courses));
			l.addAll(Arrays.asList(favorites));
			l.addAll(Arrays.asList(tables));
			l.addAll(Arrays.asList(commands));
			l.addAll(search);
		} else if (bar instanceof DirectoryBar) {
			if(dir.contains(bar)) {
				while(dir.getLast() != bar) {
					prevbar = dir.pollLast();
				}
				dir.pollLast();
			}
			l.addAll(Arrays.asList(((DirectoryBar) bar).getChildren()));
		}

		List<Bar> remove = new ArrayList<Bar>();
		for (Bar b : l) {
			final Mode mode = select.getMainController().getPlayerResource().getPlayerConfig().getMode();
			if (mode != null && b instanceof SongBar && ((SongBar) b).existsSong()
					&& ((SongBar) b).getSongData().getMode() != mode.id) {
				remove.add(b);
			}
		}
		l.removeAll(remove);

		if (!l.isEmpty()) {
			if (bar != null) {
				dir.add((DirectoryBar) bar);
			}

			// 変更前と同じバーがあればカーソル位置を保持する
			currentsongs = l.toArray(new Bar[l.size()]);
			bartextupdate = true;

			final PlayerConfig config = select.getMainController().getPlayerResource().getPlayerConfig();
			for (Bar b : currentsongs) {
				if (b instanceof SongBar) {
					SongData sd = ((SongBar) b).getSongData();
					if (sd != null && select.getScoreDataCache().existsScoreDataCache(sd, config.getLnmode())) {
						b.setScore(select.getScoreDataCache().readScoreData(sd, config.getLnmode()));
					}
				}
			}

			Arrays.sort(currentsongs, BarSorter.values()[select.getSort()]);

			selectedindex = 0;

			if (prevbar != null) {
				if (prevbar instanceof SongBar && ((SongBar) prevbar).existsSong()) {
					final SongBar prevsong = (SongBar) prevbar;
					for (int i = 0; i < currentsongs.length; i++) {
						if (currentsongs[i] instanceof SongBar && ((SongBar) currentsongs[i]).existsSong() &&
								((SongBar) currentsongs[i]).getSongData().getSha256()
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
			select.getScoreDataProperty().update(currentsongs[selectedindex].getScore());
			return true;
		}

		if (dir.size() > 0) {
			updateBar(dir.getLast());
		} else {
			updateBar(null);
		}
		Logger.getGlobal().warning("楽曲がありません");
		return false;
	}

	public void updateFolder() {
		final Bar selected = getSelected();
		if (selected instanceof FolderBar) {
			FolderBar fb = (FolderBar) selected;
			select.getSongDatabase().updateSongDatas(fb.getFolderData().getPath(), false, select.getMainController().getInfoDatabase());
		} else if (selected instanceof TableBar) {
			TableBar tb = (TableBar) selected;
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

	public void dispose() {
		// favorite書き込み
//		CourseData course = new CourseData();
//		course.setName(favorites[0].getTitle());
//		List<String> l = new ArrayList<>();
//		for(TableData.TableSongData element : favorites[0].getElements()) {
//			l.add(element.getHash());
//		}
//		course.setHash(l.toArray(new String[l.size()]));
//		CourseDataAccessor cda = new CourseDataAccessor("favorite");
//		if(!Files.exists(Paths.get("favorite"))) {
//			try {
//				Files.createDirectory(Paths.get("favorite"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		cda.write("default", course);

		banners.dispose();
	}

	private void setBanner(SongBar songbar) {
		SongData song = songbar.getSongData();
		Path bannerfile = Paths.get(song.getPath()).getParent().resolve(song.getBanner());
		// System.out.println(bannerfile.getPath());
		if (song.getBanner().length() > 0 && Files.exists(bannerfile)) {
			songbar.setBanner(banners.get(bannerfile.toString()));
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
			PlayerConfig config = select.getMainController().getPlayerResource().getPlayerConfig();
			final MainController main = select.getMainController();
			final SongInformationAccessor info = select.getMainController().getInfoDatabase();
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
					SongData sd = ((SongBar) bar).getSongData();
					if (bar.getScore() == null) {
						bar.setScore(select.getScoreDataCache().readScoreData(sd, config.getLnmode()));
					}
					boolean[] replay = new boolean[MusicSelector.REPLAY];
					for (int i = 0; i < MusicSelector.REPLAY; i++) {
						replay[i] = main.getPlayDataAccessor().existsReplayData(sd.getSha256(), sd.hasUndefinedLongNote(),
								config.getLnmode(), i);
					}
					((SongBar) bar).setExistsReplayData(replay);
					if(info != null) {
						sd.setInformation(info.getInformation(sd.getSha256()));
					}
				}
				if (bar instanceof GradeBar) {
					GradeBar gb = (GradeBar) bar;
					if (gb.existsAllSongs()) {
						String[] hash = new String[gb.getSongDatas().length];
						boolean ln = false;
						for (int j = 0; j < gb.getSongDatas().length; j++) {
							hash[j] = gb.getSongDatas()[j].getSha256();
							ln |= gb.getSongDatas()[j].hasUndefinedLongNote();
						}
						gb.setScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 0, gb.getConstraint()));
						gb.setMirrorScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 1, gb.getConstraint()));
						gb.setRandomScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 2, gb.getConstraint()));
						boolean[] replay = new boolean[MusicSelector.REPLAY];
						for (int i = 0; i < MusicSelector.REPLAY; i++) {
							replay[i] = main.getPlayDataAccessor().existsReplayData(hash, ln, config.getLnmode(), i, gb.getConstraint());
						}
						gb.setExistsReplayData(replay);
					}
				}

				if (select.getMainController().getPlayerResource().getConfig().isFolderlamp()) {
					if (bar instanceof FolderBar) {
						((FolderBar) bar).updateFolderStatus();
					}
					if (bar instanceof HashBar) {
						((HashBar) bar).updateFolderStatus();
					}
				}
				if (stop) {
					break;
				}
			}
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
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
