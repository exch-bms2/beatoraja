package bms.player.beatoraja.ir;

import java.util.HashMap;

import bms.model.Mode;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.CourseData.CourseDataConstraint;
import bms.player.beatoraja.input.BMSPlayerInputDevice;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.JudgeAlgorithm;

/**
 * 本体プロセスとIR workerプロセスの間で使うJSONプロトコル定義。
 * <p>
 * nested DTOはlibGDX {@code Json}で直接読み書きするため、フィールドはpublicにしている。
 * 既存のIR用データクラスと相互変換し、worker境界でbeatoraja本体の状態オブジェクトを渡さない。
 * </p>
 */
final class IRWorkerProtocol {

	static final String METHOD_REGISTER = "register";
	static final String METHOD_LOGIN = "login";
	static final String METHOD_GET_RIVALS = "getRivals";
	static final String METHOD_GET_TABLE_DATAS = "getTableDatas";
	static final String METHOD_GET_PLAY_DATA = "getPlayData";
	static final String METHOD_GET_COURSE_PLAY_DATA = "getCoursePlayData";
	static final String METHOD_SEND_PLAY_DATA = "sendPlayData";
	static final String METHOD_SEND_COURSE_PLAY_DATA = "sendCoursePlayData";
	static final String METHOD_GET_SONG_URL = "getSongURL";
	static final String METHOD_GET_COURSE_URL = "getCourseURL";
	static final String METHOD_GET_PLAYER_URL = "getPlayerURL";
	static final String METHOD_GET_VERSION_INFO = "getVersionInfo";
	static final String METHOD_GET_ILLEGAL_SONGS = "getIllegalSongs";

	private IRWorkerProtocol() {
	}

	/**
	 * workerへ送る1リクエスト分のデータ。
	 */
	static class Request {
		public String method;
		public Account account;
		public Player player;
		public Chart chart;
		public Course course;
		public Score score;
		public String currentVersion;
	}

	/**
	 * workerから返す1レスポンス分のデータ。
	 */
	static class Response {
		public boolean succeeded;
		public String message;
		public Player player;
		public Player[] players;
		public Table[] tables;
		public Score[] scores;
		public String text;
		public Version version;
		public String[] illegalSongs;

		/**
		 * worker内で発生したエラーを通常のIR失敗レスポンスとして返す。
		 */
		static Response error(String message) {
			Response response = new Response();
			response.succeeded = false;
			response.message = message;
			return response;
		}
	}

	/**
	 * {@link IRAccount}のJSON転送用DTO。
	 */
	static class Account {
		public String id;
		public String password;
		public String name;

		static Account from(IRAccount account) {
			if(account == null) {
				return null;
			}
			Account dto = new Account();
			dto.id = account.id;
			dto.password = account.password;
			dto.name = account.name;
			return dto;
		}

		IRAccount toIRAccount() {
			return new IRAccount(id, password, name);
		}
	}

	/**
	 * {@link IRVersionInfo}のJSON転送用DTO。
	 */
	static class Version {
		public String version;
		public String message;
		public String downloadURL;

		static Version from(IRVersionInfo versionInfo) {
			if(versionInfo == null) {
				return null;
			}
			Version dto = new Version();
			dto.version = versionInfo.version;
			dto.message = versionInfo.message;
			dto.downloadURL = versionInfo.downloadURL;
			return dto;
		}

		IRVersionInfo toIRVersionInfo() {
			return new IRVersionInfo(version, message, downloadURL);
		}
	}

	/**
	 * {@link IRPlayerData}のJSON転送用DTO。
	 */
	static class Player {
		public String id;
		public String name;
		public String rank;

		static Player from(IRPlayerData player) {
			if(player == null) {
				return null;
			}
			Player dto = new Player();
			dto.id = player.id;
			dto.name = player.name;
			dto.rank = player.rank;
			return dto;
		}

		IRPlayerData toIRPlayerData() {
			return new IRPlayerData(id, name, rank);
		}
	}

	/**
	 * {@link IRChartData}のJSON転送用DTO。
	 */
	static class Chart {
		public String md5;
		public String sha256;
		public String title;
		public String subtitle;
		public String genre;
		public String artist;
		public String subartist;
		public String url;
		public String appendurl;
		public int level;
		public int total;
		public Mode mode;
		public int lntype;
		public int judge;
		public int minbpm;
		public int maxbpm;
		public int notes;
		public boolean hasUndefinedLN;
		public boolean hasLN;
		public boolean hasCN;
		public boolean hasHCN;
		public boolean hasMine;
		public boolean hasRandom;
		public boolean hasStop;
		public HashMap<String, String> values;

		static Chart from(IRChartData chart) {
			if(chart == null) {
				return null;
			}
			Chart dto = new Chart();
			dto.md5 = chart.md5;
			dto.sha256 = chart.sha256;
			dto.title = chart.title;
			dto.subtitle = chart.subtitle;
			dto.genre = chart.genre;
			dto.artist = chart.artist;
			dto.subartist = chart.subartist;
			dto.url = chart.url;
			dto.appendurl = chart.appendurl;
			dto.level = chart.level;
			dto.total = chart.total;
			dto.mode = chart.mode;
			dto.lntype = chart.lntype;
			dto.judge = chart.judge;
			dto.minbpm = chart.minbpm;
			dto.maxbpm = chart.maxbpm;
			dto.notes = chart.notes;
			dto.hasUndefinedLN = chart.hasUndefinedLN;
			dto.hasLN = chart.hasLN;
			dto.hasCN = chart.hasCN;
			dto.hasHCN = chart.hasHCN;
			dto.hasMine = chart.hasMine;
			dto.hasRandom = chart.hasRandom;
			dto.hasStop = chart.hasStop;
			dto.values = new HashMap<>(chart.values);
			return dto;
		}

		IRChartData toIRChartData() {
			return new IRChartData(md5, sha256, title, subtitle, genre, artist, subartist, url, appendurl, level, total,
					mode, lntype, judge, minbpm, maxbpm, notes, hasUndefinedLN, hasLN, hasCN, hasHCN, hasMine, hasRandom,
					hasStop, values);
		}
	}

	/**
	 * {@link IRCourseData}のJSON転送用DTO。
	 */
	static class Course {
		public String name;
		public Chart[] charts;
		public CourseDataConstraint[] constraint;
		public Trophy[] trophy;
		public int lntype;

		static Course from(IRCourseData course) {
			if(course == null) {
				return null;
			}
			Course dto = new Course();
			dto.name = course.name;
			dto.charts = fromCharts(course.charts);
			dto.constraint = course.constraint;
			dto.trophy = fromTrophies(course.trophy);
			dto.lntype = course.lntype;
			return dto;
		}

		IRCourseData toIRCourseData() {
			return new IRCourseData(name, toIRCharts(charts), constraint, toIRTrophies(trophy), lntype);
		}
	}

	/**
	 * {@link IRCourseData.IRTrophyData}のJSON転送用DTO。
	 */
	static class Trophy {
		public String name;
		public float scorerate;
		public float smissrate;

		static Trophy from(IRCourseData.IRTrophyData trophy) {
			if(trophy == null) {
				return null;
			}
			Trophy dto = new Trophy();
			dto.name = trophy.name;
			dto.scorerate = trophy.scorerate;
			dto.smissrate = trophy.smissrate;
			return dto;
		}

		IRCourseData.IRTrophyData toIRTrophyData() {
			return new IRCourseData.IRTrophyData(name, scorerate, smissrate);
		}
	}

	/**
	 * {@link IRScoreData}のJSON転送用DTO。
	 */
	static class Score {
		public String sha256;
		public int lntype;
		public String id;
		public String player;
		public ClearType clear;
		public long date;
		public int epg;
		public int lpg;
		public int egr;
		public int lgr;
		public int egd;
		public int lgd;
		public int ebd;
		public int lbd;
		public int epr;
		public int lpr;
		public int ems;
		public int lms;
		public long avgjudge;
		public int maxcombo;
		public int notes;
		public int passnotes;
		public int minbp;
		public int option;
		public long seed;
		public int assist;
		public int gauge;
		public BMSPlayerInputDevice.Type deviceType;
		public JudgeAlgorithm judgeAlgorithm;
		public BMSPlayerRule rule;
		public String skin;

		static Score from(IRScoreData score) {
			if(score == null) {
				return null;
			}
			Score dto = new Score();
			dto.sha256 = score.sha256;
			dto.lntype = score.lntype;
			dto.id = score.id;
			dto.player = score.player;
			dto.clear = score.clear;
			dto.date = score.date;
			dto.epg = score.epg;
			dto.lpg = score.lpg;
			dto.egr = score.egr;
			dto.lgr = score.lgr;
			dto.egd = score.egd;
			dto.lgd = score.lgd;
			dto.ebd = score.ebd;
			dto.lbd = score.lbd;
			dto.epr = score.epr;
			dto.lpr = score.lpr;
			dto.ems = score.ems;
			dto.lms = score.lms;
			dto.avgjudge = score.avgjudge;
			dto.maxcombo = score.maxcombo;
			dto.notes = score.notes;
			dto.passnotes = score.passnotes;
			dto.minbp = score.minbp;
			dto.option = score.option;
			dto.seed = score.seed;
			dto.assist = score.assist;
			dto.gauge = score.gauge;
			dto.deviceType = score.deviceType;
			dto.judgeAlgorithm = score.judgeAlgorithm;
			dto.rule = score.rule;
			dto.skin = score.skin;
			return dto;
		}

		IRScoreData toIRScoreData() {
			return new IRScoreData(sha256, lntype, id, player, clear, date, epg, lpg, egr, lgr, egd, lgd, ebd, lbd,
					epr, lpr, ems, lms, avgjudge, maxcombo, notes, passnotes, minbp, option, seed, assist, gauge,
					deviceType, judgeAlgorithm, rule, skin);
		}
	}

	/**
	 * {@link IRTableData}のJSON転送用DTO。
	 */
	static class Table {
		public String name;
		public TableFolder[] folders;
		public Course[] courses;

		static Table from(IRTableData table) {
			if(table == null) {
				return null;
			}
			Table dto = new Table();
			dto.name = table.name;
			dto.folders = fromFolders(table.folders);
			dto.courses = fromCourses(table.courses);
			return dto;
		}

		IRTableData toIRTableData() {
			return new IRTableData(name, toIRFolders(folders), toIRCourses(courses));
		}
	}

	/**
	 * {@link IRTableData.IRTableFolder}のJSON転送用DTO。
	 */
	static class TableFolder {
		public String name;
		public Chart[] charts;

		static TableFolder from(IRTableData.IRTableFolder folder) {
			if(folder == null) {
				return null;
			}
			TableFolder dto = new TableFolder();
			dto.name = folder.name;
			dto.charts = fromCharts(folder.charts);
			return dto;
		}

		IRTableData.IRTableFolder toIRTableFolder() {
			return new IRTableData.IRTableFolder(name, toIRCharts(charts));
		}
	}

	static Player[] fromPlayers(IRPlayerData[] players) {
		if(players == null) {
			return null;
		}
		Player[] dto = new Player[players.length];
		for(int i = 0;i < players.length;i++) {
			dto[i] = Player.from(players[i]);
		}
		return dto;
	}

	static IRPlayerData[] toIRPlayers(Player[] players) {
		if(players == null) {
			return null;
		}
		IRPlayerData[] dto = new IRPlayerData[players.length];
		for(int i = 0;i < players.length;i++) {
			dto[i] = players[i] != null ? players[i].toIRPlayerData() : null;
		}
		return dto;
	}

	static Chart[] fromCharts(IRChartData[] charts) {
		if(charts == null) {
			return null;
		}
		Chart[] dto = new Chart[charts.length];
		for(int i = 0;i < charts.length;i++) {
			dto[i] = Chart.from(charts[i]);
		}
		return dto;
	}

	static IRChartData[] toIRCharts(Chart[] charts) {
		if(charts == null) {
			return new IRChartData[0];
		}
		IRChartData[] dto = new IRChartData[charts.length];
		for(int i = 0;i < charts.length;i++) {
			dto[i] = charts[i] != null ? charts[i].toIRChartData() : null;
		}
		return dto;
	}

	static Score[] fromScores(IRScoreData[] scores) {
		if(scores == null) {
			return null;
		}
		Score[] dto = new Score[scores.length];
		for(int i = 0;i < scores.length;i++) {
			dto[i] = Score.from(scores[i]);
		}
		return dto;
	}

	static IRScoreData[] toIRScores(Score[] scores) {
		if(scores == null) {
			return null;
		}
		IRScoreData[] dto = new IRScoreData[scores.length];
		for(int i = 0;i < scores.length;i++) {
			dto[i] = scores[i] != null ? scores[i].toIRScoreData() : null;
		}
		return dto;
	}

	static Course[] fromCourses(IRCourseData[] courses) {
		if(courses == null) {
			return null;
		}
		Course[] dto = new Course[courses.length];
		for(int i = 0;i < courses.length;i++) {
			dto[i] = Course.from(courses[i]);
		}
		return dto;
	}

	static IRCourseData[] toIRCourses(Course[] courses) {
		if(courses == null) {
			return new IRCourseData[0];
		}
		IRCourseData[] dto = new IRCourseData[courses.length];
		for(int i = 0;i < courses.length;i++) {
			dto[i] = courses[i] != null ? courses[i].toIRCourseData() : null;
		}
		return dto;
	}

	private static Trophy[] fromTrophies(IRCourseData.IRTrophyData[] trophies) {
		if(trophies == null) {
			return null;
		}
		Trophy[] dto = new Trophy[trophies.length];
		for(int i = 0;i < trophies.length;i++) {
			dto[i] = Trophy.from(trophies[i]);
		}
		return dto;
	}

	private static IRCourseData.IRTrophyData[] toIRTrophies(Trophy[] trophies) {
		if(trophies == null) {
			return new IRCourseData.IRTrophyData[0];
		}
		IRCourseData.IRTrophyData[] dto = new IRCourseData.IRTrophyData[trophies.length];
		for(int i = 0;i < trophies.length;i++) {
			dto[i] = trophies[i] != null ? trophies[i].toIRTrophyData() : null;
		}
		return dto;
	}

	private static TableFolder[] fromFolders(IRTableData.IRTableFolder[] folders) {
		if(folders == null) {
			return null;
		}
		TableFolder[] dto = new TableFolder[folders.length];
		for(int i = 0;i < folders.length;i++) {
			dto[i] = TableFolder.from(folders[i]);
		}
		return dto;
	}

	private static IRTableData.IRTableFolder[] toIRFolders(TableFolder[] folders) {
		if(folders == null) {
			return new IRTableData.IRTableFolder[0];
		}
		IRTableData.IRTableFolder[] dto = new IRTableData.IRTableFolder[folders.length];
		for(int i = 0;i < folders.length;i++) {
			dto[i] = folders[i] != null ? folders[i].toIRTableFolder() : null;
		}
		return dto;
	}

	static Table[] fromTables(IRTableData[] tables) {
		if(tables == null) {
			return null;
		}
		Table[] dto = new Table[tables.length];
		for(int i = 0;i < tables.length;i++) {
			dto[i] = Table.from(tables[i]);
		}
		return dto;
	}

	static IRTableData[] toIRTables(Table[] tables) {
		if(tables == null) {
			return null;
		}
		IRTableData[] dto = new IRTableData[tables.length];
		for(int i = 0;i < tables.length;i++) {
			dto[i] = tables[i] != null ? tables[i].toIRTableData() : null;
		}
		return dto;
	}
}
