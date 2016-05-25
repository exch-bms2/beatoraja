package bms.player.beatoraja;

import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;
import bms.player.beatoraja.play.JudgeManager;

import com.badlogic.gdx.Input.Keys;

/**
 * 各種設定項目。config.jsonで保持される
 * 
 * @author exch
 */
public class Config {
	
	/**
	 * フルスクリーン
	 */
	private boolean fullscreen;
	/**
	 * 垂直同期
	 */
	private boolean vsync;
	/**
	 * 解像度
	 */
	private int resolution = 1;
	
	/**
	 * フォルダランプの有効/無効
	 */
	private boolean folderlamp = true;
	
	/**
	 * オーディオバッファサイズ。大きすぎると音声遅延が発生し、少なすぎるとノイズが発生する
	 */
	private int audioDeviceBufferSize = 384;
	/**
	 * オーディオ同時発音数
	 */
	private int audioDeviceSimultaneousSources = 64;
	/**
	 * 最大FPS。垂直同期OFFの時のみ有効
	 */
	private int maxFramePerSecond = 240;
	/**
	 * ゲージの種類
	 */
	private int gauge = 0;
	/**
	 * 譜面オプション
	 */
	private int random;
	/**
	 * 譜面オプション(2P)
	 */
	private int random2;
	/**
	 * DP用オプション
	 */
	private int doubleoption;
	
	/**
	 * ハイスピード固定。固定する場合はデュレーションが有効となり、固定しない場合はハイスピードが有効になる
	 */
	private int fixhispeed = FIX_HISPEED_MAINBPM;
	
	public static final int FIX_HISPEED_OFF = 0;
	public static final int FIX_HISPEED_STARTBPM = 1;
	public static final int FIX_HISPEED_MAXBPM = 2;
	public static final int FIX_HISPEED_MAINBPM = 3;
	
	/**
	 * ハイスピード。1.0で等速
	 */
	private float hispeed = 1.0f;
	/**
	 * デュレーション(ノーツ表示時間)
	 */
	private int greenvalue = 300;
	
	/**
	 * レーンカバー表示量(0-1)
	 */
	private float lanecover = 0.2f;
	/**
	 * レーンカバー使用
	 */
	private boolean enablelanecover = true;
	/**
	 * リフト表示量(0-1)
	 */
	private float lift = 0.1f;
	/**
	 * リフト使用
	 */
	private boolean enablelift = true;
	/**
	 * 判定タイミング
	 */
	private int judgetiming = 0;
	/**
	 * 判定表示方法
	 */
	private int judgedetail = 0;
	/**
	 * 判定アルゴリズム
	 */
	private int judgeAlgorithm = JudgeManager.JUDGE_ALGORITHM_LR2;
	/**
	 * アシストオプション:コンスタント
	 */
	private boolean constant = false;
	/**
	 * アシストオプション:LNアシスト
	 */
	private boolean legacynote = false;
	/**
	 * LNモード
	 */
	private int lnmode = 0;
	/**
	 * アシストオプション:判定拡大
	 */
	private boolean expandjudge = false;
	/**
	 * アシストオプション:地雷除去
	 */
	private boolean nomine = false;

	/**
	 * アシストオプション:BPMガイド
	 */
	private boolean bpmguide = false;

	private boolean showjudgearea = false;
	
	private boolean markprocessednote = false;
	
	private String lr2playskin;
	
	private int[] lr2playskinoption = new int[0];
	
	private String lr2selectskin;
	
	private int[] lr2selectskinoption = new int[0];
	
	private String lr2decideskin;
	
	private int[] lr2decideskinoption = new int[0];
	
	private String lr2resultskin;
	
	private int[] lr2resultskinoption = new int[0];
	
	/**
	 * BMSルートディレクトリパス
	 */
	private String[] bmsroot = new String[0];
	/**
	 * 難易度表URL
	 */
	private String[] tableURL = new String[0];
	/**
	 * BGA表示
	 */
	private int bga = BGA_OFF;
	public static final int BGA_ON = 0;
	public static final int BGA_AUTO = 1;
	public static final int BGA_OFF = 2;	
	
	private String vlcpath = "";
	
	private PlayConfig mode7 = new PlayConfig(new int[]{
		Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
		Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.COMMA, Keys.L,
		Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
		Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q,
		Keys.W },  new int[]{
			BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2,
			BMKeys.BUTTON_7, BMKeys.BUTTON_1, BMKeys.BUTTON_4,
			BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8,
			BMKeys.BUTTON_9 });
	
	private PlayConfig mode14 = new PlayConfig(new int[]{
		Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
		Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.COMMA, Keys.L,
		Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
		Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q,
		Keys.W },  new int[]{
			BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2,
			BMKeys.BUTTON_7, BMKeys.BUTTON_1, BMKeys.BUTTON_4,
			BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8,
			BMKeys.BUTTON_9 });
	
	private PlayConfig mode9 = new PlayConfig(new int[]{
		Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
		Keys.G, Keys.B, Keys.COMMA, Keys.L,
		Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
		Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q,
		Keys.W },  new int[]{
			BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2,
			BMKeys.BUTTON_7, BMKeys.BUTTON_1, BMKeys.BUTTON_4,
			BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8,
			BMKeys.BUTTON_9 });	
	
	public Config() {
//		lr2playskin = "skin/spdframe/csv/left_ACwide.csv";
//
//		lr2decideskin = "skin/Seraphic/Decide/[+]decide.csv";
//		lr2decideskinoption = new int[]{900, 920, 910};
//		lr2selectskin = "skin/green_ary/Select/select.csv";
//		lr2selectskinoption = new int[] { 915, 918, 928, 920, 936, 950, 925, 930, 932 };		
//		lr2resultskin = "skin/RESULT SIMPLE FM/result_left.csv";
//		lr2resultskinoption = new int[] { 900, 905, 907, 909};		
		tableURL = new String[]{"http://bmsnormal2.syuriken.jp/table.html"};
		judgedetail = 2;
	}
	
	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	public boolean isVsync() {
		return vsync;
	}

	public void setVsync(boolean vsync) {
		this.vsync = vsync;
	}

	public int getGauge() {
		return gauge;
	}

	public void setGauge(int gauge) {
		this.gauge = gauge;
	}

	public int getRandom() {
		return random;
	}

	public void setRandom(int random) {
		this.random = random;
	}

	public float getHispeed() {
		return hispeed;
	}

	public void setHispeed(float hispeed) {
		this.hispeed = hispeed;
	}

	public int getFixhispeed() {
		return fixhispeed;
	}

	public void setFixhispeed(int fixhispeed) {
		this.fixhispeed = fixhispeed;
	}

	public int getGreenvalue() {
		return greenvalue;
	}

	public void setGreenvalue(int greenvalue) {
		this.greenvalue = greenvalue;
	}

	public float getLanecover() {
		return lanecover;
	}

	public void setLanecover(float lanecover) {
		this.lanecover = lanecover;
	}

	public boolean isEnablelanecover() {
		return enablelanecover;
	}

	public void setEnablelanecover(boolean enablelanecover) {
		this.enablelanecover = enablelanecover;
	}

	public float getLift() {
		return lift;
	}

	public void setLift(float lift) {
		this.lift = lift;
	}

	public boolean isEnablelift() {
		return enablelift;
	}

	public void setEnablelift(boolean enablelift) {
		this.enablelift = enablelift;
	}

	public int getBga() {
		return bga;
	}

	public void setBga(int bga) {
		this.bga = bga;
	}

	public int getJudgetiming() {
		return judgetiming;
	}

	public void setJudgetiming(int judgetiming) {
		this.judgetiming = judgetiming;
	}
	
	public boolean isConstant() {
		return constant;
	}

	public void setConstant(boolean constant) {
		this.constant = constant;
	}

	public boolean isBpmguide() {
		return bpmguide;
	}

	public void setBpmguide(boolean bpmguide) {
		this.bpmguide = bpmguide;
	}

	public int getLnmode() {
		return lnmode;
	}

	public void setLnmode(int lnmode) {
		this.lnmode = lnmode;
	}

	public String getVlcpath() {
		return vlcpath;
	}

	public void setVlcpath(String vlcpath) {
		this.vlcpath = vlcpath;
	}
	
	public String getLR2PlaySkinPath() {
		return lr2playskin;
	}
	
	public int getAudioDeviceBufferSize() {
		return audioDeviceBufferSize;
	}

	public void setAudioDeviceBufferSize(int audioDeviceBufferSize) {
		this.audioDeviceBufferSize = audioDeviceBufferSize;
	}

	public int getAudioDeviceSimultaneousSources() {
		return audioDeviceSimultaneousSources;
	}

	public void setAudioDeviceSimultaneousSources(int audioDeviceSimultaneousSources) {
		this.audioDeviceSimultaneousSources = audioDeviceSimultaneousSources;
	}

	public int getMaxFramePerSecond() {
		return maxFramePerSecond;
	}

	public void setMaxFramePerSecond(int maxFramePerSecond) {
		this.maxFramePerSecond = maxFramePerSecond;
	}

	public String[] getBmsroot() {
		return bmsroot;
	}

	public void setBmsroot(String[] bmsroot) {
		this.bmsroot = bmsroot;
	}

	public String[] getTableURL() {
		return tableURL;
	}

	public void setTableURL(String[] tableURL) {
		this.tableURL = tableURL;
	}

	public int getJudgedetail() {
		return judgedetail;
	}

	public void setJudgedetail(int judgedetail) {
		this.judgedetail = judgedetail;
	}

	public int getJudgeAlgorithm() {
		return judgeAlgorithm;
	}

	public void setJudgeAlgorithm(int judgeAlgorithm) {
		this.judgeAlgorithm = judgeAlgorithm;
	}

	public boolean isExpandjudge() {
		return expandjudge;
	}

	public void setExpandjudge(boolean expandjudge) {
		this.expandjudge = expandjudge;
	}

	public int getRandom2() {
		return random2;
	}

	public void setRandom2(int random2) {
		this.random2 = random2;
	}

	public int getDoubleoption() {
		return doubleoption;
	}

	public void setDoubleoption(int doubleoption) {
		this.doubleoption = doubleoption;
	}

	public boolean isNomine() {
		return nomine;
	}

	public void setNomine(boolean nomine) {
		this.nomine = nomine;
	}

	public boolean isLegacynote() {
		return legacynote;
	}

	public void setLegacynote(boolean legacynote) {
		this.legacynote = legacynote;
	}

	public boolean isShowjudgearea() {
		return showjudgearea;
	}

	public void setShowjudgearea(boolean showjudgearea) {
		this.showjudgearea = showjudgearea;
	}

	public boolean isMarkprocessednote() {
		return markprocessednote;
	}

	public void setMarkprocessednote(boolean markprocessednote) {
		this.markprocessednote = markprocessednote;
	}

	public int[] getLr2playskinoption() {
		return lr2playskinoption;
	}

	public void setLr2playskinoption(int[] lr2playskinoption) {
		this.lr2playskinoption = lr2playskinoption;
	}

	public String getLr2selectskin() {
		return lr2selectskin;
	}

	public void setLr2selectskin(String lr2selectskin) {
		this.lr2selectskin = lr2selectskin;
	}

	public int[] getLr2selectskinoption() {
		return lr2selectskinoption;
	}

	public void setLr2selectskinoption(int[] lr2selectskinoption) {
		this.lr2selectskinoption = lr2selectskinoption;
	}

	public String getLr2decideskin() {
		return lr2decideskin;
	}

	public void setLr2decideskin(String lr2decideskin) {
		this.lr2decideskin = lr2decideskin;
	}

	public int[] getLr2decideskinoption() {
		return lr2decideskinoption;
	}

	public void setLr2decideskinoption(int[] lr2decideskinoption) {
		this.lr2decideskinoption = lr2decideskinoption;
	}

	public boolean isFolderlamp() {
		return folderlamp;
	}

	public void setFolderlamp(boolean folderlamp) {
		this.folderlamp = folderlamp;
	}

	public PlayConfig getMode7() {
		return mode7;
	}

	public void setMode7(PlayConfig mode7) {
		this.mode7 = mode7;
	}

	public PlayConfig getMode14() {
		return mode14;
	}

	public void setMode14(PlayConfig mode14) {
		this.mode14 = mode14;
	}

	public PlayConfig getMode9() {
		return mode9;
	}

	public void setMode9(PlayConfig mode9) {
		this.mode9 = mode9;
	}

	public int getResolution() {
		return resolution;
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	public String getLr2resultskin() {
		return lr2resultskin;
	}

	public void setLr2resultskin(String lr2resultskin) {
		this.lr2resultskin = lr2resultskin;
	}

	public int[] getLr2resultskinoption() {
		return lr2resultskinoption;
	}

	public void setLr2resultskinoption(int[] lr2resultskinoption) {
		this.lr2resultskinoption = lr2resultskinoption;
	}
}
