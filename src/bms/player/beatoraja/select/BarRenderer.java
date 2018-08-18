package bms.player.beatoraja.select;

import java.io.BufferedInputStream;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyCommand;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.select.MusicSelectKeyProperty.MusicSelectKey;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.Skin.SkinObjectRenderer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.external.BMSSearchAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.FolderData;
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
	private String dirString = "";
	/**
	 * 現在表示中のバー一覧
	 */
	private Bar[] currentsongs;
	/**
	 * 選択中のバーのインデックス
	 */
	private int selectedindex;

	private Bar[] commands;

	private TableBar courses;

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

	private int durationlow = 300;
	private int durationhigh = 50;
	/**
	 * バー移動中のカウンタ
	 */
	private long duration;
	/**
	 * バーの移動方向
	 */
	private int angle;
	private boolean keyinput;

	private final int barlength = 60;
	private final BarArea[] bararea = new BarArea[barlength];

	private static class BarArea {
		public Bar sd;
		public int x;
		public int y;
		public int value = -1;
	}

	public BarRenderer(MusicSelector select) {
		final MainController main = select.main;
		this.select = select;
		TableDataAccessor tdaccessor = new TableDataAccessor(main.getConfig().getTablepath());

		TableData[] tds = tdaccessor.readAll();

		BMSSearchAccessor bmssearcha = new BMSSearchAccessor(main.getConfig().getTablepath());

		Array<TableBar> table = new Array<TableBar>();
		TableBar bmssearch = null;

		durationlow = main.getConfig().getScrollDurationLow();
		durationhigh = main.getConfig().getScrollDurationHigh();

		for (int i = 0; i < tds.length; i++) {
			if(tds[i].getName().equals("BMS Search")) {
				bmssearch = new TableBar(select, tds[i], bmssearcha);
				table.add(bmssearch);
			} else {
				table.add(new TableBar(select, tds[i], new TableDataAccessor.DifficultyTableAccessor(main.getConfig().getTablepath(), tds[i].getUrl())));
			}
		}

		if(main.getIRConnection() != null) {
			IRResponse<TableData[]> response = main.getIRConnection().getTableDatas();
			if(response.isSucceeded()) {
				for(TableData td : response.getData()) {
					table.add(new TableBar(select, td, new TableDataAccessor.DifficultyTableAccessor(main.getConfig().getTablepath(), td.getUrl())));
				}
			} else {
				Logger.getGlobal().warning("IRからのテーブル取得失敗 : " + response.getMessage());
			}
		}

		new Thread(() -> {
			TableData td = bmssearcha.read();
			if(td != null) {
				tdaccessor.write(td);
			}
		}).start();

		this.tables = table.toArray(TableBar.class);

		TableDataAccessor.TableAccessor courseReader = new TableDataAccessor.TableAccessor("course") {
			@Override
			public TableData read() {
				TableData td = new TableData();
				td.setName("COURSE");
				td.setCourse(new CourseDataAccessor("course").readAll());
				return td;
			}

			@Override
			public void write(TableData td) {
			}
		};

		courses = new TableBar(select, courseReader.read(), courseReader);

		CourseData[] cds = new CourseDataAccessor("favorite").readAll();
//		if(cds.length == 0) {
//			cds = new CourseData[1];
//			cds[0] = new CourseData();
//			cds[0].setName("FAVORITE");
//		}
		favorites = new HashBar[cds.length];
		for (int i = 0; i < cds.length; i++) {
			favorites[i] = new HashBar(select, cds[i].getName(), cds[i].getSong());
		}

		List<Bar> l = new ArrayList<Bar>();

		List<Bar> lampupdate = new ArrayList<Bar>();
		List<Bar> scoreupdate = new ArrayList<Bar>();
		for(int i = 0;i < 30;i++) {
			String s = i == 0 ? "TODAY" : i + "DAYS AGO";
			long t = ((System.currentTimeMillis() / 86400000) - i) * 86400;
			lampupdate.add(new CommandBar(select,  s, "scorelog.clear > scorelog.oldclear AND scorelog.date >= "  + t + " AND scorelog.date < " + (t + 86400)));
			scoreupdate.add(new CommandBar(select,  s,  "scorelog.score > scorelog.oldscore AND scorelog.date >= "  + t + " AND scorelog.date < " + (t + 86400)));
		}
		l.add(new ContainerBar("LAMP UPDATE", lampupdate.toArray(new Bar[lampupdate.size()])));
		l.add(new ContainerBar("SCORE UPDATE", scoreupdate.toArray(new Bar[scoreupdate.size()])));
		try {
			Json json = new Json();
			CommandFolder[] cf = json.fromJson(CommandFolder[].class,
					new BufferedInputStream(Files.newInputStream(Paths.get("folder/default.json"))));
			for(CommandFolder folder : cf) {
				l.add(createCommandBar(main, folder));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		commands = l.toArray(new Bar[l.size()]);

		for(int i = 0;i < barlength;i++) {
			bararea[i] = new BarArea();
		}
	}

	private Bar createCommandBar(MainController main, CommandFolder folder) {
		if(folder.getFolder() != null && folder.getFolder().length > 0) {
			List<Bar> l = new ArrayList<Bar>();
			for(CommandFolder child : folder.getFolder()) {
				l.add(createCommandBar(main, child));
			}
			return new ContainerBar(folder.getName(), l.toArray(new Bar[l.size()]));
		} else {
			return new CommandBar(select, folder.getName(), folder.getSql());
		}
	}

	public static class CommandFolder {

		private String name;
		private CommandFolder[] folder = new CommandFolder[0];
		private String sql;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public CommandFolder[] getFolder() {
			return folder;
		}

		public void setFolder(CommandFolder[] songs) {
			this.folder = songs;
		}

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}
	}

	public Bar getSelected() {
		return currentsongs != null ? currentsongs[selectedindex] : null;
	}

	public void setSelected(Bar bar) {
		for (int i = 0; i < currentsongs.length; i++) {
			if (currentsongs[i].getTitle().equals(bar.getTitle())) {
				selectedindex = i;
				select.getScoreDataProperty().update(currentsongs[selectedindex].getScore(),
						currentsongs[selectedindex].getRivalScore());
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
		select.getScoreDataProperty().update(currentsongs[selectedindex].getScore(),
				currentsongs[selectedindex].getRivalScore());
	}

	public void move(boolean inclease) {
		if (inclease) {
			selectedindex++;
		} else {
			selectedindex += currentsongs.length - 1;
		}
		selectedindex = selectedindex % currentsongs.length;
		select.getScoreDataProperty().update(currentsongs[selectedindex].getScore(),
				currentsongs[selectedindex].getRivalScore());
	}

	public void close() {
		if(dir.isEmpty()) {
			select.execute(MusicSelectCommand.NEXT_SORT);
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

			Rectangle r = baro.getBarImages(on, i).getDestination(select.main.getNowTime(), select);
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

	public void render(SkinObjectRenderer sprite, MusicSelectSkin skin, SkinBar baro, int time) {
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
			int i = 0;
			for (char c : charset) {
				chars[i++] = c;
			}
			baro.getText()[0].prepareFont(String.valueOf(chars));
			baro.getText()[1].prepareFont(String.valueOf(chars));
		}
		// draw song bar
		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			boolean on = (i == skin.getCenterBar());
			if (baro.getBarImages(on, i) == null) {
				continue;
			}
			int index = (selectedindex + currentsongs.length * 100 + i - skin.getCenterBar())
					% currentsongs.length;
			Bar sd = currentsongs[index];
			ba.sd = sd;

			if (sd instanceof TableBar) {
				ba.value = 2;
			} else if (sd instanceof HashBar) {
				ba.value = 2;
			} else if (sd instanceof GradeBar) {
				ba.value = ((GradeBar) sd).existsAllSongs() ? 3 : 4;
			} else if (sd instanceof FolderBar) {
				ba.value = 1;
			} else if (sd instanceof SongBar) {
				ba.value = ((SongBar) sd).existsSong() ? 0 : 4;
			} else if (sd instanceof SearchWordBar) {
				ba.value = 6;
			} else if (sd instanceof CommandBar || sd instanceof ContainerBar) {
				ba.value = 5;
			} else {
				ba.value = -1;
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
				ba.x = (int) (r.x + dx);
				ba.y = (int) (r.y + dy + (baro.getPosition() == 1 ? r.height : 0));
				baro.getBarImages(on, i).draw(sprite, time, select, ba.value, (int) dx, (int) dy);
			} else {
				ba.value = -1;
			}
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}
			// folder graph
			if (ba.sd instanceof DirectoryBar) {
				if (baro.getGraph() != null) {
					baro.getGraph().draw(sprite, time, select, (DirectoryBar)ba.sd, ba.x, ba.y);
				}
			}
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}
			// Barの種類によってテキストを変える
			// SongBarかFolderBarの場合は新規かどうかでさらに変える
			// songstatus最終値 =
			// 0:通常 1:新規 2:SongBar(通常) 3:SongBar(新規) 4:FolderBar(通常) 5:FolderBar(新規) 6:TableBar or HashBar
			// 7:GradeBar(曲所持) 8:(SongBar or GradeBar)(曲未所持) 9:CommandBar or ContainerBar 10:SearchWordBar
			// 3以降で定義されてなければ0か1を用いる
			int songstatus = ba.value;
			if(songstatus >= 2) {
				songstatus += 4;
				//定義されてなければ0:通常を用いる
				if(baro.getText()[songstatus] == null) songstatus = 0;
			} else {
				if (songstatus == 0) {
					SongData song = ((SongBar) ba.sd).getSongData();
					songstatus = song == null || System.currentTimeMillis() / 1000 > song.getAdddate() + 3600 * 24 ? 2 : 3;
					//定義されてなければ0:通常か1:新規を用いる
					if(baro.getText()[songstatus] == null) songstatus = songstatus == 3 ? 1 : 0;
				} else {
					FolderData data = ((FolderBar) ba.sd).getFolderData();
					songstatus = data == null || System.currentTimeMillis() / 1000 > data.getAdddate() + 3600 * 24 ? 4 : 5;
					//定義されてなければ0:通常か1:新規を用いる
					if(baro.getText()[songstatus] == null) songstatus = songstatus == 5 ? 1 : 0;
				}
			}
			baro.getText()[songstatus].setText(ba.sd.getTitle());
			baro.getText()[songstatus].draw(sprite, time, select, ba.x, ba.y);
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}
			// trophy
			if (ba.sd instanceof GradeBar) {
				final TrophyData trophy = ((GradeBar) ba.sd).getTrophy();
				if (trophy != null) {
					for (int j = 0; j < TROPHY.length; j++) {
						if (TROPHY[j].equals(trophy.getName()) && baro.getTrophy()[j] != null) {
							baro.getTrophy()[j].draw(sprite, time, select, ba.x, ba.y);
							break;
						}
					}
				}
			}
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}
			// lamp
			if(select.getRival() != null) {
				if (baro.getPlayerLamp()[ba.sd.getLamp(true)] != null) {
					baro.getPlayerLamp()[ba.sd.getLamp(true)].draw(sprite, time, select, ba.x, ba.y);
				}
				if (baro.getRivalLamp()[ba.sd.getLamp(false)] != null) {
					baro.getRivalLamp()[ba.sd.getLamp(false)].draw(sprite, time, select, ba.x, ba.y);
				}
			} else {
				if (baro.getLamp()[ba.sd.getLamp(true)] != null) {
					baro.getLamp()[ba.sd.getLamp(true)].draw(sprite, time, select, ba.x, ba.y);
				}
			}
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}
			// level
			if (ba.sd instanceof SongBar && ((SongBar) ba.sd).existsSong()) {
				SongData song = ((SongBar) ba.sd).getSongData();

				SkinNumber leveln = baro.getBarlevel()[song.getDifficulty() >= 0 && song.getDifficulty() < 7
						? song.getDifficulty() : 0];
				if (leveln != null) {
					leveln.draw(sprite, time, song.getLevel(), select, ba.x, ba.y);
				}
			}
		}

		for (int i = 0; i < barlength; i++) {
			final BarArea ba = bararea[i];
			if(ba.value == -1) {
				continue;
			}

			int flag = 0;
			if (ba.sd instanceof SongBar && ((SongBar) ba.sd).existsSong()) {
				SongData song = ((SongBar) ba.sd).getSongData();
				flag |= song.getFeature();
			}

			if (ba.sd instanceof GradeBar) {
				GradeBar gb = (GradeBar) ba.sd;
				if (gb.existsAllSongs()) {
					for (SongData song : gb.getSongDatas()) {
						flag |= song.getFeature();
					}
				}
			}

			// LN
			int ln = -1;
			if((flag & SongData.FEATURE_UNDEFINEDLN) != 0) {
				ln = select.main.getPlayerConfig().getLnmode();
			}
			if((flag & SongData.FEATURE_LONGNOTE) != 0) {
				ln = ln > 0 ? ln : 0;
			}
			if((flag & SongData.FEATURE_CHARGENOTE) != 0) {
				ln = ln > 1 ? ln : 1;
			}
			if((flag & SongData.FEATURE_HELLCHARGENOTE) != 0) {
				ln = ln > 2 ? ln : 2;
			}

			if(ln >= 0) {
				// TODO LNラベル描画分岐
				final int[] lnindex = {0,0,0};
				if (baro.getLabel()[lnindex[ln]] != null) {
					baro.getLabel()[lnindex[ln]].draw(sprite, time, select, ba.x, ba.y);
				}
			}
			// MINE
			if ((flag & SongData.FEATURE_MINENOTE) != 0 && baro.getLabel()[2] != null) {
				baro.getLabel()[2].draw(sprite, time, select, ba.x, ba.y);
			}
			// RANDOM
			if ((flag & SongData.FEATURE_RANDOM) != 0 && baro.getLabel()[1] != null) {
				baro.getLabel()[1].draw(sprite, time, select, ba.x, ba.y);
			}
		}
	}

	public void input() {
		BMSPlayerInputProcessor input = select.main.getInputProcessor();
		boolean[] keystate = input.getKeystate();
		long[] keytime = input.getTime();
		boolean[] cursor = input.getCursorState();

        final MusicSelectKeyProperty property = MusicSelectKeyProperty.values()[select.main.getPlayerResource().getPlayerConfig().getMusicselectinput()];

		// song bar scroll on mouse wheel
		int mov = -input.getScroll();
		input.resetScroll();
		// song bar scroll
		if (property.isPressed(keystate, keytime, MusicSelectKey.UP, false) || cursor[1]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				keyinput = true;
				mov = 1;
				duration = l + durationlow;
				angle = durationlow;
			}
			if (l > duration && keyinput == true) {
				duration = l + durationhigh;
				mov = 1;
				angle = durationhigh;
			}
		} else if (property.isPressed(keystate, keytime, MusicSelectKey.DOWN, false) || cursor[0]) {
			long l = System.currentTimeMillis();
			if (duration == 0) {
				keyinput = true;
				mov = -1;
				duration = l + durationlow;
				angle = -durationlow;
			}
			if (l > duration && keyinput == true) {
				duration = l + durationhigh;
				mov = -1;
				angle = -durationhigh;
			}
		} else {
			keyinput = false;
		}
			long l = System.currentTimeMillis();
		if (l > duration && keyinput == false) {
				duration = 0;
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

		if(input.isActivated(KeyCommand.ADD_FAVORITE_SONG)) {
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
		if(input.isActivated(KeyCommand.ADD_FAVORITE_CHART)) {
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

	public String getDirectoryString() {
		return dirString;
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
			l.add(courses);
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

		if (!l.isEmpty()) {
			final PlayerConfig config = select.main.getPlayerResource().getPlayerConfig();
			int modeIndex = 0;
			for(;modeIndex < MusicSelector.MODE.length && MusicSelector.MODE[modeIndex] != config.getMode();modeIndex++);
			for(int trialCount = 0; trialCount < MusicSelector.MODE.length; trialCount++, modeIndex++) {
				config.setMode(MusicSelector.MODE[modeIndex % MusicSelector.MODE.length]);
				List<Bar> remove = new ArrayList<Bar>();
				for (Bar b : l) {
					final Mode mode = select.main.getPlayerResource().getPlayerConfig().getMode();
					if (mode != null && b instanceof SongBar && ((SongBar) b).getSongData().getMode() != 0 &&
							((SongBar) b).getSongData().getMode() != mode.id) {
						remove.add(b);
					}
				}
				if(l.size() != remove.size()) {
					l.removeAll(remove);
					break;
				}
			}

			if (bar != null) {
				dir.add((DirectoryBar) bar);
			}

			// 変更前と同じバーがあればカーソル位置を保持する
			currentsongs = l.toArray(new Bar[l.size()]);
			bartextupdate = true;

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
			select.getScoreDataProperty().update(currentsongs[selectedindex].getScore(),
					currentsongs[selectedindex].getRivalScore());

			StringBuilder str = new StringBuilder();
			for (Bar b : dir) {
				str.append(b.getTitle()).append(" > ");
			}
			dirString = str.toString();

			select.selectedBarMoved();

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
			final MainController main = select.main;
			PlayerConfig config = main.getPlayerResource().getPlayerConfig();
			final ScoreDataCache rival = select.getRivalScoreDataCache();

			final Array<SongData> songarray = new Array<>(bars.length);
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
					songarray.add(((SongBar) bar).getSongData());
				}
			}
			final SongData[] songs = songarray.toArray(SongData.class);
			// loading score
			// TODO collectorを使用してスコアをまとめて取得
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
					SongData sd = ((SongBar) bar).getSongData();
					if (bar.getScore() == null) {
						bar.setScore(select.getScoreDataCache().readScoreData(sd, config.getLnmode()));
					}
					if (rival != null && bar.getRivalScore() == null) {
						bar.setRivalScore(rival.readScoreData(sd, config.getLnmode()));
					}
					boolean[] replay = new boolean[MusicSelector.REPLAY];
					for (int i = 0; i < MusicSelector.REPLAY; i++) {
						replay[i] = main.getPlayDataAccessor().existsReplayData(sd.getSha256(), sd.hasUndefinedLongNote(),
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
							ln |= gb.getSongDatas()[j].hasUndefinedLongNote();
						}
						CourseDataConstraint[] constraint = gb.getCourseData().getConstraint();
						gb.setScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 0, constraint));
						gb.setMirrorScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 1, constraint));
						gb.setRandomScore(main.getPlayDataAccessor().readScoreData(hash, ln, config.getLnmode(), 2, constraint));
						boolean[] replay = new boolean[MusicSelector.REPLAY];
						for (int i = 0; i < MusicSelector.REPLAY; i++) {
							replay[i] = main.getPlayDataAccessor().existsReplayData(hash, ln, config.getLnmode(), i, constraint);
						}
						gb.setExistsReplayData(replay);
					}
				}

				if (main.getPlayerResource().getConfig().isFolderlamp()) {
					if (bar instanceof DirectoryBar) {
						((DirectoryBar) bar).updateFolderStatus();
					}
				}
				if (stop) {
					break;
				}
			}
			// loading song information
			final SongInformationAccessor info = main.getInfoDatabase();
			if(info != null) {
				info.getInformation(songs);
			}
			// loading banner
			// loading stagefile
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
					final SongBar songbar = (SongBar) bar;
					SongData song = songbar.getSongData();
					try {
						Path bannerfile = Paths.get(song.getPath()).getParent().resolve(song.getBanner());
						// System.out.println(bannerfile.getPath());
						if (song.getBanner().length() > 0 && Files.exists(bannerfile)) {
							songbar.setBanner(select.getBannerResource().get(bannerfile.toString()));
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("banner読み込み失敗 : " + song.getBanner());
					}
					try {
						Path stagefilefile = Paths.get(song.getPath()).getParent().resolve(song.getStagefile());
						// System.out.println(stagefilefile.getPath());
						if (song.getStagefile().length() > 0 && Files.exists(stagefilefile)) {
							songbar.setStagefile(select.getStagefileResource().get(stagefilefile.toString()));
						}
					} catch (Exception e) {
						Logger.getGlobal().warning("stagefile読み込み失敗 : " + song.getStagefile());
					}
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
