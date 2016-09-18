package bms.player.beatoraja;

import java.util.Arrays;
import java.util.Calendar;

import bms.player.beatoraja.skin.Skin;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public abstract class MainState {

	private final MainController main;

	private long starttime;

	private long[] timer = new long[256];

	private Skin skin;

	public static final int IMAGE_STAGEFILE = 100;
	public static final int IMAGE_BACKBMP = 101;
	public static final int IMAGE_BANNER = 102;
	public static final int IMAGE_BLACK = 110;
	public static final int IMAGE_WHITE = 111;

	public static final int TIMER_STARTINPUT = 1;
	public static final int TIMER_FADEOUT = 2;
	public static final int TIMER_FAILED = 3;

	public static final int TIMER_SONGBAR_MOVE = 10;
	public static final int TIMER_SONGBAR_CHANGE = 11;
	public static final int TIMER_SONGBAR_MOVE_UP = 12;
	public static final int TIMER_SONGBAR_MOVE_DOWN = 13;
	public static final int TIMER_SONGBAR_STOP = 14;
	public static final int TIMER_README_BEGIN = 15;
	public static final int TIMER_README_END = 16;

	public static final int TIMER_PANEL1_ON = 21;
	public static final int TIMER_PANEL2_ON = 22;
	public static final int TIMER_PANEL3_ON = 23;
	public static final int TIMER_PANEL4_ON = 24;
	public static final int TIMER_PANEL5_ON = 25;
	public static final int TIMER_PANEL6_ON = 26;
	public static final int TIMER_PANEL1_OFF = 31;
	public static final int TIMER_PANEL2_OFF = 32;
	public static final int TIMER_PANEL3_OFF = 33;
	public static final int TIMER_PANEL4_OFF = 34;
	public static final int TIMER_PANEL5_OFF = 35;
	public static final int TIMER_PANEL6_OFF = 36;

	public static final int TIMER_READY = 40;
	public static final int TIMER_PLAY = 41;
	public static final int TIMER_GAUGE_INCLEASE_1P = 42;
	public static final int TIMER_GAUGE_INCLEASE_2P = 43;
	public static final int TIMER_GAUGE_MAX_1P = 44;
	public static final int TIMER_GAUGE_MAX_2P = 45;
	public static final int TIMER_JUDGE_1P = 46;
	public static final int TIMER_JUDGE_2P = 47;
	public static final int TIMER_FULLCOMBO_1P = 48;
	public static final int TIMER_FULLCOMBO_2P = 49;
	public static final int TIMER_BOMB = 50;
	public static final int TIMER_HOLD = 70;
	public static final int TIMER_KEYON = 100;
	public static final int TIMER_KEYOFF = 120;
	public static final int TIMER_RHYTHM = 140;
	public static final int TIMER_ENDOFNOTE_1P = 143;
	public static final int TIMER_ENDOFNOTE_2P = 144;

	public static final int TIMER_RESULTGRAPH_BEGIN = 150;
	public static final int TIMER_RESULTGRAPH_END = 151;
	public static final int TIMER_RESULT_UPDATESCORE = 152;

	// 選曲専用
	public static final int SLIDER_MUSICSELECT_POSITION = 1;
	// プレイ専用
	public static final int SLIDER_MUSIC_PROGRESS = 6;

	public static final int BARGRAPH_MUSIC_PROGRESS = 1001;
	public static final int BARGRAPH_LOAD_PROGRESS = 1002;
	public static final int BARGRAPH_LEVEL = 1003;
	public static final int BARGRAPH_LEVEL_BEGINNER = 1005;
	public static final int BARGRAPH_LEVEL_NORMAL = 1006;
	public static final int BARGRAPH_LEVEL_HYPER = 1007;
	public static final int BARGRAPH_LEVEL_ANOTHER = 1008;
	public static final int BARGRAPH_LEVEL_INSANE = 1009;
	public static final int BARGRAPH_SCORERATE = 1010;
	public static final int BARGRAPH_SCORERATE_FINAL = 1011;
	public static final int BARGRAPH_BESTSCORERATE_NOW = 1012;
	public static final int BARGRAPH_BESTSCORERATE = 1013;
	public static final int BARGRAPH_TARGETSCORERATE_NOW = 1014;
	public static final int BARGRAPH_TARGETSCORERATE = 1015;

	public static final int BARGRAPH_RATE_PGREAT = 1040;
	public static final int BARGRAPH_RATE_GREAT = 1041;
	public static final int BARGRAPH_RATE_GOOD = 1042;
	public static final int BARGRAPH_RATE_BAD = 1043;
	public static final int BARGRAPH_RATE_POOR = 1044;
	public static final int BARGRAPH_RATE_MAXCOMBO = 1045;
	public static final int BARGRAPH_RATE_SCORE = 1046;
	public static final int BARGRAPH_RATE_EXSCORE = 1047;

	public static final int OFFSET_LIFT = 100;

	public static final int STRING_RIVAL = 1;
	public static final int STRING_PLAYER = 2;
	public static final int STRING_TITLE = 10;
	public static final int STRING_SUBTITLE = 11;
	public static final int STRING_FULLTITLE = 12;
	public static final int STRING_GENRE = 13;
	public static final int STRING_ARTIST = 14;
	public static final int STRING_SUBARTIST = 15;
	public static final int STRING_COURSE1_TITLE = 150;
	public static final int STRING_COURSE2_TITLE = 151;
	public static final int STRING_COURSE3_TITLE = 152;
	public static final int STRING_COURSE4_TITLE = 153;
	public static final int STRING_COURSE5_TITLE = 154;
	public static final int STRING_COURSE6_TITLE = 155;
	public static final int STRING_COURSE7_TITLE = 156;
	public static final int STRING_COURSE8_TITLE = 157;
	public static final int STRING_COURSE9_TITLE = 158;
	public static final int STRING_COURSE10_TITLE = 159;
	public static final int STRING_DIRECTORY = 1000;

	public static final int NUMBER_HISPEED_LR2 = 10;
	public static final int NUMBER_JUDGETIMING = 12;
    public static final int NUMBER_LANECOVER1 = 14;
	public static final int NUMBER_TIME_YEAR = 21;
	public static final int NUMBER_TIME_MONTH = 22;
	public static final int NUMBER_TIME_DAY = 23;
	public static final int NUMBER_TIME_HOUR = 24;
	public static final int NUMBER_TIME_MINUTE = 25;
	public static final int NUMBER_TIME_SECOND = 26;

	public static final int NUMBER_TOTALPLAYCOUNT = 30;
	public static final int NUMBER_TOTALCLEARCOUNT = 31;
	public static final int NUMBER_TOTALFAILCOUNT = 32;
	public static final int NUMBER_TOTALPERFECT = 33;
	public static final int NUMBER_TOTALGREAT = 34;
	public static final int NUMBER_TOTALGOOD = 35;
	public static final int NUMBER_TOTALBAD = 36;
	public static final int NUMBER_TOTALPOOR = 37;
	public static final int NUMBER_TOTALPLAYNOTES = 1037;
	public static final int NUMBER_SCORE = 71;
	public static final int NUMBER_MAXSCORE = 72;
	public static final int NUMBER_TOTALNOTES = 74;
	public static final int NUMBER_MAXCOMBO = 75;
	public static final int NUMBER_MISSCOUNT = 76;
	public static final int NUMBER_PLAYCOUNT = 77;
	public static final int NUMBER_CLEARCOUNT = 78;
	public static final int NUMBER_FAILCOUNT = 79;
	public static final int NUMBER_PERFECT2 = 80;
	public static final int NUMBER_GREAT2 = 81;
	public static final int NUMBER_GOOD2 = 82;
	public static final int NUMBER_BAD2 = 83;
	public static final int NUMBER_POOR2 = 84;
	public static final int NUMBER_PERFECT_RATE = 85;
	public static final int NUMBER_GREAT_RATE = 86;
	public static final int NUMBER_GOOD_RATE = 87;
	public static final int NUMBER_BAD_RATE = 88;
	public static final int NUMBER_POOR_RATE = 89;
	public static final int NUMBER_MAXBPM = 90;
	public static final int NUMBER_MINBPM = 91;
	public static final int NUMBER_PLAYLEVEL = 96;
	public static final int NUMBER_SCORE2 = 101;
	public static final int NUMBER_SCORE_RATE = 102;
	public static final int NUMBER_SCORE_RATE_AFTERDOT = 103;
	public static final int NUMBER_COMBO = 104;
	public static final int NUMBER_MAXCOMBO2 = 105;
	public static final int NUMBER_TOTALNOTES2 = 106;
	public static final int NUMBER_GROOVEGAUGE = 107;
	public static final int NUMBER_GROOVEGAUGE_AFTERDOT = 207;
	public static final int NUMBER_DIFF_EXSCORE = 108;
	public static final int NUMBER_PERFECT = 110;
	public static final int NUMBER_EARLY_PERFECT = 1110;
	public static final int NUMBER_LATE_PERFECT = 2110;
	public static final int NUMBER_GREAT = 111;
	public static final int NUMBER_EARLY_GREAT = 1111;
	public static final int NUMBER_LATE_GREAT = 2111;
	public static final int NUMBER_GOOD = 112;
	public static final int NUMBER_EARLY_GOOD = 1112;
	public static final int NUMBER_LATE_GOOD = 2112;
	public static final int NUMBER_BAD = 113;
	public static final int NUMBER_EARLY_BAD = 1113;
	public static final int NUMBER_LATE_BAD = 2113;
	public static final int NUMBER_POOR = 114;
	public static final int NUMBER_EARLY_POOR = 1114;
	public static final int NUMBER_LATE_POOR = 2114;
	public static final int NUMBER_MISS = 1115;
	public static final int NUMBER_EARLY_MISS = 2115;
	public static final int NUMBER_LATE_MISS = 3115;
	public static final int NUMBER_TOTAL_RATE = 115;
	public static final int NUMBER_TOTAL_RATE_AFTERDOT = 116;
	public static final int NUMBER_TARGET_SCORE = 121;
	public static final int NUMBER_HIGHSCORE = 150;
	public static final int NUMBER_TARGET_SCORE2 = 151;
	public static final int NUMBER_DIFF_HIGHSCORE = 152;
	public static final int NUMBER_DIFF_TARGETSCORE = 153;
	public static final int NUMBER_DIFF_NEXTRANK = 154;
	public static final int NUMBER_NOWBPM = 160;
	public static final int NUMBER_TIMELEFT_MINUTE = 163;
	public static final int NUMBER_TIMELEFT_SECOND = 164;
	public static final int NUMBER_LOADING_PROGRESS = 165;
	public static final int NUMBER_HIGHSCORE2 = 170;
	public static final int NUMBER_SCORE3 = 171;
	public static final int NUMBER_DIFF_HIGHSCORE2 = 172;
	public static final int NUMBER_TARGET_MAXCOMBO = 173;
	public static final int NUMBER_MAXCOMBO3 = 174;
	public static final int NUMBER_DIFF_MAXCOMBO = 175;
	public static final int NUMBER_TARGET_MISSCOUNT = 176;
	public static final int NUMBER_MISSCOUNT2 = 177;
	public static final int NUMBER_DIFF_MISSCOUNT = 178;

	public static final int NUMBER_CLEAR = 20000;
	public static final int NUMBER_TARGET_CLEAR = 20001;
	public static final int NUMBER_HISPEED = 30000;
	public static final int NUMBER_HISPEED_AFTERDOT = 30001;
	public static final int NUMBER_DURATION = 30002;
	public static final int NUMBER_TOTALEARLY = 40001;
	public static final int NUMBER_TOTALLATE = 40002;
	public static final int NUMBER_FOLDER_TOTALSONGS = 50000;

	public static final int BUTTON_GAUGE_1P = 1040;
	public static final int BUTTON_GAUGE_2P = 1041;
	public static final int BUTTON_RANDOM_1P = 1042;
	public static final int BUTTON_RANDOM_2P = 1043;
	public static final int BUTTON_DPOPTION = 1054;
	public static final int BUTTON_HSFIX = 1055;
	public static final int BUTTON_BGA = 1072;
	public static final int BUTTON_JUDGEDETAIL = 1078;
	public static final int BUTTON_ASSIST_EXJUDGE = 2001;
	public static final int BUTTON_ASSIST_CONSTANT = 2002;
	public static final int BUTTON_ASSIST_JUDGEAREA = 2003;
	public static final int BUTTON_ASSIST_LEGACY = 2004;
	public static final int BUTTON_ASSIST_MARKNOTE = 2005;
	public static final int BUTTON_ASSIST_BPMGUIDE = 2006;
	public static final int BUTTON_ASSIST_NOMINE = 2007;

	public static final int OPTION_FOLDERBAR = 1;
	public static final int OPTION_SONGBAR = 2;
	public static final int OPTION_PLAYABLEBAR = 5;

	public static final int OPTION_PANEL1 = 21;
	public static final int OPTION_PANEL2 = 22;
	public static final int OPTION_PANEL3 = 23;

	public static final int OPTION_BGANORMAL = 30;
	public static final int OPTION_BGAEXTEND = 31;
	public static final int OPTION_AUTOPLAYOFF = 32;
	public static final int OPTION_AUTOPLAYON = 33;
	public static final int OPTION_SCOREGRAPHOFF = 38;
	public static final int OPTION_SCOREGRAPHON = 39;

	public static final int OPTION_BGAOFF = 40;
	public static final int OPTION_BGAON = 41;
	public static final int OPTION_GAUGE_GROOVE = 42;
	public static final int OPTION_GAUGE_HARD = 43;

	public static final int OPTION_NOW_LOADING = 80;
	public static final int OPTION_LOADED = 81;

	public static final int OPTION_GROOVE = 118;
	public static final int OPTION_HARD = 119;
	public static final int OPTION_HAZARD = 120;
	public static final int OPTION_EASY = 121;
	public static final int OPTION_AEASY = 124;
	public static final int OPTION_EXHARD = 125;

	public static final int OPTION_NORMAL = 126;
	public static final int OPTION_MIRROR = 127;
	public static final int OPTION_RANDOM = 128;
	public static final int OPTION_SRANDOM = 129;
	public static final int OPTION_HRANDOM = 130;
	public static final int OPTION_ALLSCR = 131;
	public static final int OPTION_RRANDOM = 928;
	public static final int OPTION_SPIRAL = 929;
	public static final int OPTION_EXRANDOM = 930;
	public static final int OPTION_EXSRANDOM = 931;

	public static final int OPTION_DIFFICULTY0 = 150;
	public static final int OPTION_DIFFICULTY1 = 151;
	public static final int OPTION_DIFFICULTY2 = 152;
	public static final int OPTION_DIFFICULTY3 = 153;
	public static final int OPTION_DIFFICULTY4 = 154;
	public static final int OPTION_DIFFICULTY5 = 155;

	public static final int OPTION_7KEYSONG = 160;
	public static final int OPTION_5KEYSONG = 161;
	public static final int OPTION_14KEYSONG = 162;
	public static final int OPTION_10KEYSONG = 163;
	public static final int OPTION_9KEYSONG = 164;

	public static final int OPTION_NO_BGA = 170;
	public static final int OPTION_BGA = 171;
	public static final int OPTION_NO_LN = 172;
	public static final int OPTION_LN = 173;
	public static final int OPTION_NO_TEXT = 174;
	public static final int OPTION_TEXT = 175;
	public static final int OPTION_NO_BPMCHANGE = 176;
	public static final int OPTION_BPMCHANGE = 177;
	public static final int OPTION_NO_RANDOMSEQUENCE = 178;
	public static final int OPTION_RANDOMSEQUENCE = 179;

	public static final int OPTION_JUDGE_VERYHARD = 180;
	public static final int OPTION_JUDGE_HARD = 181;
	public static final int OPTION_JUDGE_NORMAL = 182;
	public static final int OPTION_JUDGE_EASY = 183;

	public static final int OPTION_NO_STAGEFILE = 190;
	public static final int OPTION_STAGEFILE = 191;
	public static final int OPTION_NO_BANNER = 192;
	public static final int OPTION_BANNER = 193;
	public static final int OPTION_NO_BACKBMP = 194;
	public static final int OPTION_BACKBMP = 195;
	public static final int OPTION_NO_REPLAYDATA = 196;
	public static final int OPTION_REPLAYDATA = 197;

	public static final int OPTION_1P_AAA = 200;
	public static final int OPTION_1P_AA = 201;
	public static final int OPTION_1P_A = 202;
	public static final int OPTION_1P_B = 203;
	public static final int OPTION_1P_C = 204;
	public static final int OPTION_1P_D = 205;
	public static final int OPTION_1P_E = 206;
	public static final int OPTION_1P_F = 207;
	public static final int OPTION_2P_AAA = 210;
	public static final int OPTION_2P_AA = 211;
	public static final int OPTION_2P_A = 212;
	public static final int OPTION_2P_B = 213;
	public static final int OPTION_2P_C = 214;
	public static final int OPTION_2P_D = 215;
	public static final int OPTION_2P_E = 216;
	public static final int OPTION_2P_F = 217;
	public static final int OPTION_AAA = 220;
	public static final int OPTION_AA = 221;
	public static final int OPTION_A = 222;
	public static final int OPTION_B = 223;
	public static final int OPTION_C = 224;
	public static final int OPTION_D = 225;
	public static final int OPTION_E = 226;
	public static final int OPTION_F = 227;

	public static final int OPTION_1P_0_9 = 230;
	public static final int OPTION_1P_10_19 = 231;
	public static final int OPTION_1P_20_29 = 232;
	public static final int OPTION_1P_30_39 = 233;
	public static final int OPTION_1P_40_49 = 234;
	public static final int OPTION_1P_50_59 = 235;
	public static final int OPTION_1P_60_69 = 236;
	public static final int OPTION_1P_70_79 = 237;
	public static final int OPTION_1P_80_89 = 238;
	public static final int OPTION_1P_90_99 = 239;
	public static final int OPTION_1P_100 = 240;

	public static final int OPTION_1P_PERFECT = 241;
	public static final int OPTION_1P_GREAT = 242;
	public static final int OPTION_1P_GOOD = 243;
	public static final int OPTION_1P_BAD = 244;
	public static final int OPTION_1P_POOR = 245;
	public static final int OPTION_1P_MISS = 246;

	public static final int OPTION_RESULT_AAA_1P = 300;
	public static final int OPTION_RESULT_AA_1P = 301;
	public static final int OPTION_RESULT_A_1P = 302;
	public static final int OPTION_RESULT_B_1P = 303;
	public static final int OPTION_RESULT_C_1P = 304;
	public static final int OPTION_RESULT_D_1P = 305;
	public static final int OPTION_RESULT_E_1P = 306;
	public static final int OPTION_RESULT_F_1P = 307;
	public static final int OPTION_RESULT_0_1P = 308;

	public static final int OPTION_RESULT_AAA_2P = 310;
	public static final int OPTION_RESULT_AA_2P = 311;
	public static final int OPTION_RESULT_A_2P = 312;
	public static final int OPTION_RESULT_B_2P = 313;
	public static final int OPTION_RESULT_C_2P = 314;
	public static final int OPTION_RESULT_D_2P = 315;
	public static final int OPTION_RESULT_E_2P = 316;
	public static final int OPTION_RESULT_F_2P = 317;
	public static final int OPTION_RESULT_0_2P = 318;

	public static final int OPTION_BEST_AAA_1P = 320;
	public static final int OPTION_BEST_AA_1P = 321;
	public static final int OPTION_BEST_A_1P = 322;
	public static final int OPTION_BEST_B_1P = 323;
	public static final int OPTION_BEST_C_1P = 324;
	public static final int OPTION_BEST_D_1P = 325;
	public static final int OPTION_BEST_E_1P = 326;
	public static final int OPTION_BEST_F_1P = 327;

	public static final int OPTION_UPDATE_SCORE = 330;
	public static final int OPTION_UPDATE_MAXCOMBO = 331;
	public static final int OPTION_UPDATE_MISSCOUNT = 332;
	public static final int OPTION_UPDATE_TRIAL = 333;
	public static final int OPTION_UPDATE_IRRANK = 334;
	public static final int OPTION_UPDATE_SCORERANK = 335;

	public static final int OPTION_NOW_AAA_1P = 340;
	public static final int OPTION_NOW_AA_1P = 341;
	public static final int OPTION_NOW_A_1P = 342;
	public static final int OPTION_NOW_B_1P = 343;
	public static final int OPTION_NOW_C_1P = 344;
	public static final int OPTION_NOW_D_1P = 345;
	public static final int OPTION_NOW_E_1P = 346;
	public static final int OPTION_NOW_F_1P = 347;

	public static final int OPTION_DISABLE_RESULTFLIP = 350;
	public static final int OPTION_ENABLE_RESULTFLIP = 351;
	public static final int OPTION_1PWIN = 352;
	public static final int OPTION_2PWIN = 353;
	public static final int OPTION_DRAW = 354;

	public MainState() {
		this(null);
	}

	public MainState(MainController main) {
		this.main = main;
		Arrays.fill(timer, Long.MIN_VALUE);
		Pixmap bp = new Pixmap(1,1, Pixmap.Format.RGBA8888);
		bp.drawPixel(0,0, Color.toIntBits(255,0,0,0));
		black = new TextureRegion(new Texture(bp));
		Pixmap hp = new Pixmap(1,1, Pixmap.Format.RGBA8888);
		hp.drawPixel(0,0, Color.toIntBits(255,255,255,255));
		white = new TextureRegion(new Texture(hp));
	}

	public MainController getMainController() {
		return main;
	}

	public abstract void create();

	public abstract void render();

	public void pause() {

	}

	public void resume() {

	}

	public void resize(int width, int height) {

	}

	public abstract void dispose();

	public long getStartTime() {
		return starttime;
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
	}

	public int getNowTime() {
		return (int) (System.currentTimeMillis() - starttime);
	}

	public long[] getTimer() {
		return timer;
	}

	public boolean getBooleanValue(int id) {
		final SongData model = getMainController().getPlayerResource().getSongdata();
		switch (id) {
		case OPTION_STAGEFILE:
			return model != null && model.getStagefile().length() > 0;
		case OPTION_NO_STAGEFILE:
			return model != null && model.getStagefile().length() == 0;
		case OPTION_BACKBMP:
			return model != null && model.getBackbmp().length() > 0;
		case OPTION_NO_BACKBMP:
			return model != null && model.getBackbmp().length() == 0;
		case OPTION_BANNER:
			return model != null && model.getBanner().length() > 0;
		case OPTION_NO_BANNER:
			return model != null && model.getBanner().length() == 0;
		case OPTION_BGAEXTEND:
			return true;
		case OPTION_SCOREGRAPHOFF:
			return false;
		case OPTION_SCOREGRAPHON:
			return true;
		case OPTION_DIFFICULTY0:
			return model != null && (model.getDifficulty() <= 0 || model.getDifficulty() > 5);
		case OPTION_DIFFICULTY1:
			return model != null && model.getDifficulty() == 1;
		case OPTION_DIFFICULTY2:
			return model != null && model.getDifficulty() == 2;
		case OPTION_DIFFICULTY3:
			return model != null && model.getDifficulty() == 3;
		case OPTION_DIFFICULTY4:
			return model != null && model.getDifficulty() == 4;
		case OPTION_DIFFICULTY5:
			return model != null && model.getDifficulty() == 5;
		case OPTION_JUDGE_EASY:
			return model != null && (model.getJudge() == 3 || model.getJudge() >= 100);
		case OPTION_JUDGE_NORMAL:
			return model != null && (model.getJudge() == 2 || (model.getJudge() >= 80 && model.getJudge() < 100));
		case OPTION_JUDGE_HARD:
			return model != null && (model.getJudge() == 1 || (model.getJudge() >= 50 && model.getJudge() < 80));
		case OPTION_JUDGE_VERYHARD:
			return model != null && (model.getJudge() == 0 || (model.getJudge() >= 10 && model.getJudge() < 50));
		case OPTION_5KEYSONG:
			return model != null && model.getMode() == 5;
		case OPTION_7KEYSONG:
			return model != null && model.getMode() == 7;
		case OPTION_9KEYSONG:
			return model != null && model.getMode() == 9;
		case OPTION_10KEYSONG:
			return model != null && model.getMode() == 10;
		case OPTION_14KEYSONG:
			return model != null && model.getMode() == 14;
		case OPTION_NO_TEXT:
			return model != null && !model.hasDocument();
		case OPTION_TEXT:
			return model != null && model.hasDocument();
		case OPTION_NO_LN:
			return model != null && !model.hasLongNote();
		case OPTION_LN:
			return model != null && model.hasLongNote();
		case OPTION_NO_BGA:
			return model != null && !model.hasBGA();
		case OPTION_BGA:
			return model != null && model.hasBGA();
		case OPTION_NO_RANDOMSEQUENCE:
			return model != null && !model.hasRandomSequence();
		case OPTION_RANDOMSEQUENCE:
			return model != null && model.hasRandomSequence();
		case OPTION_NO_BPMCHANGE:
			return model != null && model.getMinbpm() == model.getMaxbpm();
		case OPTION_BPMCHANGE:
			return model != null && model.getMinbpm() < model.getMaxbpm();

		}
		return false;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}

	public int getJudgeCount(int judge, boolean fast) {
		return 0;
	}

	private Calendar cl = Calendar.getInstance();

	public int getNumberValue(int id) {
		switch (id) {
		case NUMBER_JUDGETIMING:
			return getMainController().getPlayerResource().getConfig().getJudgetiming();
		case NUMBER_TIME_YEAR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.YEAR);
		case NUMBER_TIME_MONTH:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MONTH) + 1;
		case NUMBER_TIME_DAY:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.DATE);
		case NUMBER_TIME_HOUR:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.HOUR_OF_DAY);
		case NUMBER_TIME_MINUTE:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.MINUTE);
		case NUMBER_TIME_SECOND:
			cl.setTimeInMillis(System.currentTimeMillis());
			return cl.get(Calendar.SECOND);
		case NUMBER_PERFECT:
			return getJudgeCount(0, true) + getJudgeCount(0, false);
		case NUMBER_EARLY_PERFECT:
			return getJudgeCount(0, true);
		case NUMBER_LATE_PERFECT:
			return getJudgeCount(0, false);
		case NUMBER_GREAT:
			return getJudgeCount(1, true) + getJudgeCount(1, false);
		case NUMBER_EARLY_GREAT:
			return getJudgeCount(1, true);
		case NUMBER_LATE_GREAT:
			return getJudgeCount(1, false);
		case NUMBER_GOOD:
			return getJudgeCount(2, true) + getJudgeCount(2, false);
		case NUMBER_EARLY_GOOD:
			return getJudgeCount(2, true);
		case NUMBER_LATE_GOOD:
			return getJudgeCount(2, false);
		case NUMBER_BAD:
			return getJudgeCount(3, true) + getJudgeCount(3, false);
		case NUMBER_EARLY_BAD:
			return getJudgeCount(3, true);
		case NUMBER_LATE_BAD:
			return getJudgeCount(3, false);
		case NUMBER_POOR:
			return getJudgeCount(4, true) + getJudgeCount(4, false);
		case NUMBER_EARLY_POOR:
			return getJudgeCount(4, true);
		case NUMBER_LATE_POOR:
			return getJudgeCount(4, false);
		case NUMBER_MISS:
			return getJudgeCount(5, true) + getJudgeCount(5, false);
		case NUMBER_EARLY_MISS:
			return getJudgeCount(5, true);
		case NUMBER_LATE_MISS:
			return getJudgeCount(5, false);
		case BUTTON_GAUGE_1P:
			return getMainController().getPlayerResource().getConfig().getGauge();
		case BUTTON_RANDOM_1P:
			return getMainController().getPlayerResource().getConfig().getRandom();
		case BUTTON_RANDOM_2P:
			return getMainController().getPlayerResource().getConfig().getRandom2();
		case BUTTON_DPOPTION:
			return getMainController().getPlayerResource().getConfig().getDoubleoption();
		case BUTTON_HSFIX:
			return getMainController().getPlayerResource().getConfig().getFixhispeed();
		case BUTTON_BGA:
			return getMainController().getPlayerResource().getConfig().getBga();
		case BUTTON_JUDGEDETAIL:
			return getMainController().getPlayerResource().getConfig().getJudgedetail();
		case BUTTON_ASSIST_EXJUDGE:
			return getMainController().getPlayerResource().getConfig().isExpandjudge() ? 1 : 0;
		case BUTTON_ASSIST_CONSTANT:
			return getMainController().getPlayerResource().getConfig().isConstant() ? 1 : 0;
		case BUTTON_ASSIST_JUDGEAREA:
			return getMainController().getPlayerResource().getConfig().isShowjudgearea() ? 1 : 0;
		case BUTTON_ASSIST_LEGACY:
			return getMainController().getPlayerResource().getConfig().isLegacynote() ? 1 : 0;
		case BUTTON_ASSIST_MARKNOTE:
			return getMainController().getPlayerResource().getConfig().isMarkprocessednote() ? 1 : 0;
		case BUTTON_ASSIST_BPMGUIDE:
			return getMainController().getPlayerResource().getConfig().isBpmguide() ? 1 : 0;
		case BUTTON_ASSIST_NOMINE:
			return getMainController().getPlayerResource().getConfig().isNomine() ? 1 : 0;
		case NUMBER_TOTALNOTES:
		case NUMBER_TOTALNOTES2:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getNotes();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MINBPM:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getMinbpm();
			}
			return Integer.MIN_VALUE;
		case NUMBER_MAXBPM:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getMaxbpm();
			}
			return Integer.MIN_VALUE;
		case NUMBER_PLAYLEVEL:
			if (getMainController().getPlayerResource().getSongdata() != null) {
				return getMainController().getPlayerResource().getSongdata().getLevel();
			}
			return Integer.MIN_VALUE;

		}
		return 0;
	}

	public float getSliderValue(int id) {
		return 0;
	}

	public void setSliderValue(int id, float value) {
	}

	public String getTextValue(int id) {
		if (getMainController().getPlayerResource() != null) {
			SongData song = getMainController().getPlayerResource().getSongdata();
			switch (id) {
			case STRING_TITLE:
				return song != null ? song.getTitle() : "";
			case STRING_SUBTITLE:
				return song != null ? song.getSubtitle() : "";
			case STRING_FULLTITLE:
				return song != null ? song.getTitle() + " " + song.getSubtitle() : "";
			case STRING_ARTIST:
				return song != null ? song.getArtist() : "";
			case STRING_SUBARTIST:
				return song != null ? song.getSubartist() : "";
			case STRING_GENRE:
				return song != null ? song.getGenre() : "";
			}
		}
		return "";
	}

	private TextureRegion black;
	private TextureRegion white;

	public TextureRegion getImage(int imageid) {
		if (getMainController().getPlayerResource().getBGAManager() != null) {
			if (imageid == IMAGE_BACKBMP) {
				return getMainController().getPlayerResource().getBGAManager().getBackbmpData();
			}
			if (imageid == IMAGE_STAGEFILE) {
				return getMainController().getPlayerResource().getBGAManager().getStagefileData();
			}
		}
		if(imageid == IMAGE_BLACK) {
			return black;
		}
		if(imageid == IMAGE_WHITE) {
			return white;
		}
		return null;
	}
}
