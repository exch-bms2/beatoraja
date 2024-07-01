package bms.player.beatoraja.select;

import static bms.player.beatoraja.SystemSoundManager.SoundType.FOLDER_CLOSE;

import java.io.BufferedInputStream;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.Sort;
import com.badlogic.gdx.utils.StringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.external.BMSSearchAccessor;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRTableData;
import bms.player.beatoraja.select.bar.*;
import bms.player.beatoraja.skin.property.EventFactory.EventType;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongInformationAccessor;

/**
 * 楽曲バー管理用クラス
 *
 * @author exch
 */
public class BarManager {
	
	private final MusicSelector select;
	/**
	 * 難易度表バー一覧
	 */
	private TableBar[] tables = new TableBar[0];

	private Bar[] commands;
	
	private TableBar courses;

	private HashBar[] favorites = new HashBar[0];

	/**
	 * 現在のフォルダ階層
	 */
	private final Queue<DirectoryBar> dir = new Queue<>();
	private String dirString = "";
	/**
	 * 現在表示中のバー一覧
	 */
	Bar[] currentsongs;
	/**
	 * 選択中のバーのインデックス
	 */
	int selectedindex;

	/**
	 * 各階層のフォルダを開く元となったバー
	 */
	private final Queue<Bar> sourcebars = new Queue<>();

	// jsonで定義したrandom bar (folder)
	private List<RandomFolder> randomFolderList;

	// システム側で挿入されるルートフォルダ
	private final HashMap<String, Bar> appendFolders = new HashMap<String, Bar>();
	/**
	 * 検索結果バー一覧
	 */
	private final Array<SearchWordBar> search = new Array<SearchWordBar>();
	/**
	 * ランダムコース結果バー一覧
	 */
	private final Array<RandomCourseResult> randomCourseResult = new Array<>();

	BarContentsLoaderThread loader;

	public BarManager(MusicSelector select) {
		this.select = select;
	}
	
	void init() {
		TableDataAccessor tdaccessor = new TableDataAccessor(select.resource.getConfig().getTablepath());

		TableData[] unsortedtables = tdaccessor.readAll();
		final List<TableData> sortedtables = new ArrayList<TableData>(unsortedtables.length);
		
		for(String url : select.resource.getConfig().getTableURL()) {
			for(int i = 0;i < unsortedtables.length;i++) {
				final TableData td = unsortedtables[i];
				if(td != null && url.equals(td.getUrl())) {
					sortedtables.add(td);
					unsortedtables[i] = null;
					break;
				}
			}
		}

		Arrays.stream(unsortedtables).filter(Objects::nonNull).forEach(td -> sortedtables.add(td));

		BMSSearchAccessor bmssearcha = new BMSSearchAccessor(select.resource.getConfig().getTablepath());

		Array<TableBar> table = new Array<TableBar>();

		sortedtables.stream().map(td -> {
			if (td.getName().equals("BMS Search")) {
				return new TableBar(select, td, bmssearcha);
			} else {
				return new TableBar(select, td,
						new TableDataAccessor.DifficultyTableAccessor(select.resource.getConfig().getTablepath(), td.getUrl()));
			}			
		}).forEach(table::add);;

		if(select.main.getIRStatus().length > 0) {
			IRResponse<IRTableData[]> response = select.main.getIRStatus()[0].connection.getTableDatas();
			if(response.isSucceeded()) {
				for(IRTableData irtd : response.getData()) {
					TableData td = new TableData();
					td.setName(irtd.name);
					td.setFolder(Stream.of(irtd.folders).map(folder -> {
						TableData.TableFolder tf = new TableData.TableFolder();
						tf.setName(folder.name);
						tf.setSong(Stream.of(folder.charts).map(chart -> {
							SongData song = new SongData();
							song.setSha256(chart.sha256);
							song.setMd5(chart.md5);
							song.setTitle(chart.title);
							song.setArtist(chart.artist);
							song.setGenre(chart.genre);
							song.setUrl(chart.url);
							song.setAppendurl(chart.appendurl);
							if(chart.mode != null) {
								song.setMode(chart.mode.id);								
							}
							return song;
						}).toArray(SongData[]::new));
						return tf;
					}).toArray(TableData.TableFolder[]::new));
					
					td.setCourse(Stream.of(irtd.courses).map(course -> {
						CourseData cd = new CourseData();
						cd.setName(course.name);
						cd.setSong(Stream.of(course.charts).map(chart -> {
							SongData song = new SongData();
							song.setSha256(chart.sha256);
							song.setMd5(chart.md5);
							song.setTitle(chart.title);
							song.setArtist(chart.artist);
							song.setGenre(chart.genre);
							song.setUrl(chart.url);
							song.setAppendurl(chart.appendurl);
							if(chart.mode != null) {
								song.setMode(chart.mode.id);								
							}
							return song;
						}).toArray(SongData[]::new));
						
						cd.setConstraint(course.constraint);
						cd.setTrophy(Stream.of(course.trophy).map(t -> {
						    TrophyData trophyData = new TrophyData();
						    trophyData.setName(t.name);
						    trophyData.setMissrate(t.smissrate);
						    trophyData.setScorerate(t.scorerate);
							return trophyData;
						}).toArray(TrophyData[]::new));
						
						cd.setRelease(true);
						return cd;
					}).toArray(CourseData[]::new));
					
					if(td.validate()) {
						table.add(new TableBar(select, td, new TableDataAccessor.DifficultyTableAccessor(select.resource.getConfig().getTablepath(), td.getUrl())));						
					}
				}
			} else {
				Logger.getGlobal().warning("IRからのテーブル取得失敗 : " + response.getMessage());
			}
		}

		new Thread(() -> {
			TableData td = bmssearcha.read();
			if (td != null) {
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
		
		favorites = Stream.of(cds).map(cd -> new HashBar(select, cd.getName(), cd.getSong())).toArray(HashBar[]::new);

		Array<Bar> l = new Array<Bar>();

		Array<Bar> lampupdate = new Array<Bar>();
		Array<Bar> scoreupdate = new Array<Bar>();
		for(int i = 0;i < 30;i++) {
			String s = i == 0 ? "TODAY" : i + "DAYS AGO";
			long t = ((System.currentTimeMillis() / 86400000) - i) * 86400;
			lampupdate.add(new CommandBar(select,  s, "scorelog.clear > scorelog.oldclear AND scorelog.date >= "  + t + " AND scorelog.date < " + (t + 86400)));
			scoreupdate.add(new CommandBar(select,  s,  "scorelog.score > scorelog.oldscore AND scorelog.date >= "  + t + " AND scorelog.date < " + (t + 86400)));
		}
		l.add(new ContainerBar("LAMP UPDATE", lampupdate.toArray(Bar.class)));
		l.add(new ContainerBar("SCORE UPDATE", scoreupdate.toArray(Bar.class)));
		try {
			Json json = new Json();
			CommandFolder[] cf = json.fromJson(CommandFolder[].class,
					new BufferedInputStream(Files.newInputStream(Paths.get("folder/default.json"))));
			Stream.of(cf).forEach(folder -> l.add(createCommandBar(select, folder)));
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			randomFolderList = objectMapper.readValue(
					new BufferedInputStream(Files.newInputStream(Paths.get("random/default.json"))),
					new TypeReference<List<RandomFolder>>() {
					});
		} catch (Throwable e) {
			randomFolderList = new ArrayList<RandomFolder>();
			RandomFolder randomFolder = new RandomFolder();
			randomFolder.setName("RANDOM SELECT");
			randomFolderList.add(randomFolder);
			e.printStackTrace();
		}

		commands = l.toArray(Bar.class);
	}
	
	public boolean updateBar() {
		if (dir.size > 0) {
			return updateBar(dir.last());
		}
		return updateBar(null);
	}

	public boolean updateBar(Bar bar) {
		Bar prevbar = currentsongs != null ? currentsongs[selectedindex] : null;
		int prevdirsize = dir.size;
		Bar sourcebar = null;
		Array<Bar> l = new Array<Bar>();
		boolean showInvisibleCharts = false;
		boolean isSortable = true;

		if (MainLoader.getIllegalSongCount() > 0) {
			l.addAll(SongBar.toSongBarArray(select.getSongDatabase().getSongDatas(MainLoader.getIllegalSongs())));
		} else if (bar == null) {
			// root bar
			if (dir.size > 0) {
				prevbar = dir.first();
			}
			dir.clear();
			sourcebars.clear();
			l.addAll(new FolderBar(select, null, "e2977170").getChildren());
			l.add(courses);
			l.addAll(favorites);
			appendFolders.keySet().forEach((key) -> {
			    l.add(appendFolders.get(key));
			});
			l.addAll(tables);
			l.addAll(commands);
			l.addAll(search);
		} else if (bar instanceof DirectoryBar) {
			showInvisibleCharts = ((DirectoryBar)bar).isShowInvisibleChart();
			if(dir.indexOf((DirectoryBar) bar, true) != -1) {
				while(dir.last() != bar) {
					prevbar = dir.removeLast();
					sourcebar = sourcebars.removeLast();
				}
				dir.removeLast();
			}
			l.addAll(((DirectoryBar) bar).getChildren());
			isSortable = ((DirectoryBar) bar).isSortable();

			if (bar instanceof ContainerBar && randomCourseResult.size > 0) {
				StringBuilder str = new StringBuilder();
				for (Bar b : dir) {
					str.append(b.getTitle()).append(" > ");
				}
				str.append(bar.getTitle()).append(" > ");
				final String ds = str.toString();
				for (RandomCourseResult r : randomCourseResult) {
					if (r.dirString.equals(ds)) {
						l.add(r.course);
					}
				}
			}
		}

		if(!select.resource.getConfig().isShowNoSongExistingBar()) {
			Array<Bar> remove = new Array<Bar>();
			for (Bar b : l) {
				if ((b instanceof SongBar && !((SongBar) b).existsSong())
					|| b instanceof GradeBar && !((GradeBar) b).existsAllSongs()) {
					remove.add(b);
				}
			}
			l.removeAll(remove, true);
		}

		if (l.size > 0) {
			final PlayerConfig config = select.resource.getPlayerConfig();
			int modeIndex = 0;
			for(;modeIndex < MusicSelector.MODE.length && MusicSelector.MODE[modeIndex] != config.getMode();modeIndex++);
			for(int trialCount = 0; trialCount < MusicSelector.MODE.length; trialCount++, modeIndex++) {
				final Mode mode = MusicSelector.MODE[modeIndex % MusicSelector.MODE.length];
				config.setMode(mode);
				Array<Bar> remove = new Array<Bar>();
				for (Bar b : l) {
					if(b instanceof SongBar && ((SongBar) b).getSongData() != null) {
						final SongData song = ((SongBar) b).getSongData();
						if((!showInvisibleCharts && (song.getFavorite() & (SongData.INVISIBLE_SONG | SongData.INVISIBLE_CHART)) != 0)
								|| (mode != null && song.getMode() != 0 && song.getMode() != mode.id)) {
							remove.add(b);
						}
					}
				}
				if(l.size != remove.size) {
					l.removeAll(remove, true);
					break;
				}
			}

			if (bar != null) {
				dir.addLast((DirectoryBar) bar);
				if (dir.size > prevdirsize) {
					sourcebars.addLast(prevbar);
				}
			}

			Bar[] newcurrentsongs = l.toArray(Bar.class);
			for (Bar b : newcurrentsongs) {
				if (b instanceof SongBar) {
					SongData sd = ((SongBar) b).getSongData();
					if (sd != null && select.getScoreDataCache().existsScoreDataCache(sd, config.getLnmode())) {
						b.setScore(select.getScoreDataCache().readScoreData(sd, config.getLnmode()));
					}
				}
			}

			if(isSortable) {
				final BarSorter sorter = BarSorter.valueOf(select.main.getPlayerConfig().getSortid());
			    Sort.instance().sort(newcurrentsongs, sorter != null ? sorter.sorter : BarSorter.TITLE.sorter);
			}

			Array<Bar> bars = new Array<Bar>();
			if (select.main.getPlayerConfig().isRandomSelect()) {
				try {
					for (RandomFolder randomFolder : randomFolderList) {
						SongData[] randomTargets = Stream.of(newcurrentsongs).filter(
								songBar -> songBar instanceof SongBar && ((SongBar) songBar).getSongData().getPath() != null)
								.map(songBar -> ((SongBar) songBar).getSongData()).toArray(SongData[]::new);
						if (randomFolder.getFilter() != null) {
							Set<String> filterKey = randomFolder.getFilter().keySet();
							randomTargets = Stream.of(randomTargets).filter(r -> {
								ScoreData scoreData = select.getScoreDataCache().readScoreData(r, config.getLnmode());
								for (String key : filterKey) {
									String getterMethodName = "get" + key.substring(0, 1).toUpperCase()
											+ key.substring(1);
									try {
										Object value = randomFolder.getFilter().get(key);
										if (scoreData == null) {
											if (value instanceof String && !"".equals((String) value)) {
												return false;
											}
											if (value instanceof Integer && 0 != (Integer) value) {
												return false;
											}
										} else {
											Method getterMethod = ScoreData.class.getMethod(getterMethodName);
											Object propertyValue = getterMethod.invoke(scoreData);
											if (!propertyValue.equals(value)) {
												return false;
											}
										}
									} catch (Throwable e) {
										e.printStackTrace();
										return false;
									}
								}
								return true;
							}).toArray(SongData[]::new);
						}
						if ((randomFolder.getFilter() != null && randomTargets.length >= 1)
								|| (randomFolder.getFilter() == null && randomTargets.length >= 2)) {
							Bar randomBar = new ExecutableBar(randomTargets, select.main.getCurrentState(),
									randomFolder.getName());
							bars.add(randomBar);
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			bars.addAll(newcurrentsongs);

			currentsongs = bars.toArray(Bar.class);
			
			select.getBarRender().updateBarText();

			selectedindex = 0;

			// 変更前と同じバーがあればカーソル位置を保持する
			if (sourcebar != null) {
				prevbar = sourcebar;
			}
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
						if (currentsongs[i].getClass() == prevbar.getClass() && currentsongs[i].getTitle().equals(prevbar.getTitle())) {
							selectedindex = i;
							break;
						}
					}

				}
			}

			if (loader != null) {
				loader.stopRunning();
			}
			loader = new BarContentsLoaderThread(select, currentsongs);
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

		if (dir.size > 0) {
			updateBar(dir.last());
		} else {
			updateBar(null);
		}
		Logger.getGlobal().warning("楽曲がありません");
		return false;
	}

	public void close() {
		if(dir.size == 0) {
			select.executeEvent(EventType.sort);
			return;
		}

		final DirectoryBar current = dir.removeLast();
		final DirectoryBar parent = dir.size > 0 ? dir.last() : null;
		dir.addLast(current);
		updateBar(parent);
		select.play(FOLDER_CLOSE);
	}

	public Queue<DirectoryBar> getDirectory() {
		return dir;
	}

	public String getDirectoryString() {
		return dirString;
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

	private Bar createCommandBar(MusicSelector select, CommandFolder folder) {
		return (folder.getFolder() != null && folder.getFolder().length > 0 || folder.getRandomCourse() != null && folder.getRandomCourse().length > 0) ?
			new ContainerBar(folder.getName(), Stream.concat(
					Stream.of(folder.getFolder()).map(child -> createCommandBar(select, child))
					,Stream.of(folder.getRandomCourse()).map(RandomCourseBar::new)).toArray(Bar[]::new)) : 
			new CommandBar(select, folder.getName(), folder.getSql(), folder.isShowall());
	}

	public void addSearch(SearchWordBar bar) {
		for (SearchWordBar s : search) {
			if (s.getTitle().equals(bar.getTitle())) {
				search.removeValue(s, true);
				break;
			}
		}
		if (search.size >= select.resource.getConfig().getMaxSearchBarCount()) {
			search.removeIndex(0);
		}
		search.add(bar);
	}

	public void addRandomCourse(GradeBar bar, String dirString) {
		if (randomCourseResult.size >= 100) {
			randomCourseResult.removeIndex(0);
		}
		randomCourseResult.add(new RandomCourseResult(bar, dirString));
	}

	synchronized public void setAppendDirectoryBar(String key, Bar bar) {
	    this.appendFolders.put(key, bar);
	}

	public static class CommandFolder {

		private String name;
		private CommandFolder[] folder = new CommandFolder[0];
		private String sql;
		private RandomCourseData[] rcourse = RandomCourseData.EMPTY;
		private boolean showall = false;

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

		public RandomCourseData[] getRandomCourse() { return rcourse; }

		public void setRandomCourse(RandomCourseData[] course) { this.rcourse = course; }

		public boolean isShowall() {
			return showall;
		}

		public void setShowall(boolean showall) {
			this.showall = showall;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RandomFolder {
		private String name;
		private Map<String, Object> filter;
		public String getName() {
			return "[RANDOM] " + name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, Object> getFilter() {
			return filter;
		}

		public void setFilter(Map<String, Object> filter) {
			this.filter = filter;
		}
	}

	static class RandomCourseResult {
		public GradeBar course;
		public String dirString;

		public RandomCourseResult(GradeBar course, String dirString) {
			this.course = course;
			this.dirString = dirString;
		}
	}

	/**
	 * 選曲バー内のスコアデータ等を読み込むためのスレッド
	 */
	static class BarContentsLoaderThread extends Thread {

		private final MusicSelector select;
		/**
		 * データ読み込み対象の選曲バー
		 */
		private Bar[] bars;
		/**
		 * 読み込み終了フラグ
		 */
		private boolean stop = false;

		public BarContentsLoaderThread(MusicSelector select, Bar[] bar) {
			this.select = select;
			this.bars = bar;
		}

		@Override
		public void run() {
			final MainController main = select.main;
			final PlayerConfig config = select.resource.getPlayerConfig();
			final ScoreDataCache rival = select.getRivalScoreDataCache();
			final String rivalName = rival != null ? select.getRival().getName() : null;

			final SongData[] songs = Stream.of(bars).filter(bar -> bar instanceof SongBar && ((SongBar) bar).existsSong())
					.map(bar -> ((SongBar) bar).getSongData()).toArray(SongData[]::new);
			// loading score
			// TODO collectorを使用してスコアをまとめて取得
			for (Bar bar : bars) {
				if (bar instanceof SongBar && ((SongBar) bar).existsSong()) {
					SongData sd = ((SongBar) bar).getSongData();
					if (bar.getScore() == null) {
						bar.setScore(select.getScoreDataCache().readScoreData(sd, config.getLnmode()));
					}
					if (rival != null && bar.getRivalScore() == null) {
						final ScoreData rivalScore = rival.readScoreData(sd, config.getLnmode());
						if(rivalScore != null) {
							rivalScore.setPlayer(rivalName);							
						}
						bar.setRivalScore(rivalScore);
					}
					for(int i = 0;i < MusicSelector.REPLAY;i++) {
						((SongBar) bar).setExistsReplay(i, main.getPlayDataAccessor().existsReplayData(sd.getSha256(), sd.hasUndefinedLongNote(),config.getLnmode(), i));						
					}
				} else if (bar instanceof GradeBar && ((GradeBar)bar).existsAllSongs()) {
					final GradeBar gb = (GradeBar) bar;
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
					for(int i = 0;i < MusicSelector.REPLAY;i++) {
						gb.setExistsReplay(i, main.getPlayDataAccessor().existsReplayData(hash, ln ,config.getLnmode(), i, constraint));						
					}
				}

				if (select.resource.getConfig().isFolderlamp()) {
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
