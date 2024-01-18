package bms.player.beatoraja.select;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Queue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import bms.player.beatoraja.*;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.CourseData.TrophyData;
import bms.player.beatoraja.external.BMSSearchAccessor;
import bms.player.beatoraja.ir.IRResponse;
import bms.player.beatoraja.ir.IRTableData;
import bms.player.beatoraja.select.bar.*;
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
	TableBar[] tables = new TableBar[0];

	Bar[] commands;
	
	TableBar courses;

	HashBar[] favorites = new HashBar[0];

	/**
	 * 各階層のフォルダを開く元となったバー
	 */
	final Queue<Bar> sourcebars = new Queue<>();

	// jsonで定義したrandom bar (folder)
	List<RandomFolder> randomFolderList;

	// システム側で挿入されるルートフォルダ
	HashMap<String, Bar> appendFolders = new HashMap<String, Bar>();
	/**
	 * 検索結果バー一覧
	 */
	Array<SearchWordBar> search = new Array<SearchWordBar>();
	/**
	 * ランダムコース結果バー一覧
	 */
	Array<RandomCourseResult> randomCourseResult = new Array<>();

	BarContentsLoaderThread loader;

	public BarManager(MusicSelector select) {
		this.select = select;
	}
	
	void init() {
		TableDataAccessor tdaccessor = new TableDataAccessor(select.resource.getConfig().getTablepath());

		TableData[] unsortedtables = tdaccessor.readAll();
		List<TableData> sortedtables = new ArrayList<TableData>(unsortedtables.length);
		
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
		
		
		for(TableData td : unsortedtables) {
			if(td != null) {
				sortedtables.add(td);
			}
		}

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
		// TODO BarRendererから移行
		return select.getBarRender().updateBar();
	}

	public boolean updateBar(Bar bar) {
		// TODO BarRendererから移行
		return select.getBarRender().updateBar(bar);
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
					boolean[] replay = new boolean[MusicSelector.REPLAY];
					for (int i = 0; i < MusicSelector.REPLAY; i++) {
						replay[i] = main.getPlayDataAccessor().existsReplayData(sd.getSha256(), sd.hasUndefinedLongNote(),
								config.getLnmode(), i);
					}
					((SongBar) bar).setExistsReplayData(replay);
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
					boolean[] replay = new boolean[MusicSelector.REPLAY];
					for (int i = 0; i < MusicSelector.REPLAY; i++) {
						replay[i] = main.getPlayDataAccessor().existsReplayData(hash, ln, config.getLnmode(), i, constraint);
					}
					gb.setExistsReplayData(replay);
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
