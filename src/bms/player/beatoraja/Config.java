package bms.player.beatoraja;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;
import bms.player.beatoraja.PlayConfig.MidiConfig;
import bms.player.beatoraja.play.JudgeManager;

import bms.player.beatoraja.skin.SkinType;
import com.badlogic.gdx.Input.Keys;

import static bms.player.beatoraja.Resolution.*;
import bms.player.beatoraja.select.*;
import bms.model.Mode;

/**
 * 各種設定項目。config.jsonで保持される
 * 
 * @author exch
 */
public class Config {

	// TODO プレイヤー毎に異なる見込みの大きい要素をPlayerConfigに移動

	private PlayerConfig[] players;

	private int player;
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
	private Resolution resolution = HD;

	/**
	 * フォルダランプの有効/無効
	 */
	private boolean folderlamp = true;
	/**
	 * オーディオドライバー
	 */
	private int audioDriver = 0;
	/**
	 * オーディオ:OpenAL (libGDX Sound)
	 */
	public static final int AUDIODRIVER_SOUND = 0;
	public static final int AUDIODRIVER_AUDIODEVICE = 1;
	
	/**
	 * オーディオ:ASIO
	 */
	public static final int AUDIODRIVER_ASIO = 2;
	
	private String audioDriverName = null;
	/**
	 * オーディオバッファサイズ。大きすぎると音声遅延が発生し、少なすぎるとノイズが発生する
	 */
	private int audioDeviceBufferSize = 384;
	/**
	 * オーディオ同時発音数
	 */
	private int audioDeviceSimultaneousSources = 64;
	/**
	 * キー音のボリューム
	 */
	private float keyvolume = 1.0f;
	/**
	 * BGノート音のボリューム
	 */
	private float bgvolume = 1.0f;
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
	public static final int FIX_HISPEED_MINBPM = 4;

	private int target;
	/**
	 * 最小入力感覚
	 */
	private int inputduration = 10;
	/**
	 * 判定タイミング
	 */
	private int judgetiming = 0;
	/**
	 * 判定アルゴリズム
	 */
	private int judgeAlgorithm = JudgeManager.JUDGE_ALGORITHM_LR2;

        /**
         * JKOC Hack (boolean) private variable
         */
    private boolean jkoc_hack = false;
    
	private Mode mode_sort = null;
	
    private boolean cacheSkinImage = false;
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

	private boolean showhiddennote = false;

	private boolean showpastnote = false;

	private String bgmpath = "";

	private String soundpath = "";

	private SkinConfig[] skin;
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
	private int bga = BGA_ON;
	public static final int BGA_ON = 0;
	public static final int BGA_AUTO = 1;
	public static final int BGA_OFF = 2;
	/**
	 * BGA拡大
	 */
	private int bgaExpand = BGAEXPAND_FULL;
	public static final int BGAEXPAND_FULL = 0;
	public static final int BGAEXPAND_KEEP_ASPECT_RATIO = 1;
	public static final int BGAEXPAND_OFF = 2;

	private int movieplayer = MOVIEPLAYER_FFMPEG;
	public static final int MOVIEPLAYER_FFMPEG = 0;
	public static final int MOVIEPLAYER_VLC = 1;

	private int frameskip = 1;
	private String vlcpath = "";

	private PlayConfig mode7 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
					Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN,
					Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			MidiConfig.default7());

	private PlayConfig mode14 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
					Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN,
					Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 },
				{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
						BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			MidiConfig.default14());

	private PlayConfig mode9 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B, Keys.COMMA, Keys.L,
					Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT,
					Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			MidiConfig.default9());

	private int musicselectinput = 0;

	private String irname = "";

	private String userid = "";

	private String password = "";

	public Config() {
		PlayerConfig sample = new PlayerConfig();
		sample.setName("playerscore");
		players = new PlayerConfig[]{sample};
		tableURL = new String[] { "http://bmsnormal2.syuriken.jp/table.html",
				"http://bmsnormal2.syuriken.jp/table_insane.html", "http://dpbmsdelta.web.fc2.com/table/dpdelta.html",
				"http://dpbmsdelta.web.fc2.com/table/insane.html",
				"http://flowermaster.web.fc2.com/lrnanido/gla/LN.html",
				"http://stellawingroad.web.fc2.com/new/pms.html" };
		int maxSkinType = 0;
		for (SkinType type : SkinType.values()) {
			if (type.getId() > maxSkinType) {
				maxSkinType = type.getId();
			}
		}
		skin = new SkinConfig[maxSkinType + 1];
		for (Map.Entry<SkinType, String> entry : SkinConfig.defaultSkinPathMap.entrySet()) {
			skin[entry.getKey().getId()] = new SkinConfig(entry.getValue());
		}
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

	public int getFixhispeed() {
		return fixhispeed;
	}

	public void setFixhispeed(int fixhispeed) {
		this.fixhispeed = fixhispeed;
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

        public boolean getJKOC()  {
            return jkoc_hack;
        }
        
        public void setJKOC(boolean jkoc)  {
            this.jkoc_hack = jkoc;
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

	public void setModeSort(Mode m)  {
		this.mode_sort = m;
	}
	
	public int getModeSort()  {
		for(int x = 0; x < MusicSelector.MODE.length; x++)  {
		    if(mode_sort == MusicSelector.MODE[x])
				return x;
			else
				continue;
		}
		return 0;
	}
	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}

	public int getMusicselectinput() {
		return musicselectinput;
	}

	public void setMusicselectinput(int musicselectinput) {
		this.musicselectinput = musicselectinput;
	}

	public boolean isShowhiddennote() {
		return showhiddennote;
	}

	public void setShowhiddennote(boolean showhiddennote) {
		this.showhiddennote = showhiddennote;
	}

	public int getMovieplayer() {
		return movieplayer;
	}

	public void setMovieplayer(int movieplayer) {
		this.movieplayer = movieplayer;
	}

	public int getFrameskip() {
		return frameskip;
	}

	public void setFrameskip(int frameskip) {
		this.frameskip = frameskip;
	}

	public String getBgmpath() {
		return bgmpath;
	}

	public void setBgmpath(String bgmpath) {
		this.bgmpath = bgmpath;
	}

	public String getSoundpath() {
		return soundpath;
	}

	public void setSoundpath(String soundpath) {
		this.soundpath = soundpath;
	}

	public int getInputduration() {
		return inputduration;
	}

	public void setInputduration(int inputduration) {
		this.inputduration = inputduration;
	}

	public SkinConfig[] getSkin() {
		return skin;
	}

	public void setSkin(SkinConfig[] skin) {
		this.skin = skin;
	}

	public float getKeyvolume() {
		return keyvolume;
	}

	public void setKeyvolume(float keyvolume) {
		this.keyvolume = keyvolume;
	}

	public float getBgvolume() {
		return bgvolume;
	}

	public void setBgvolume(float bgvolume) {
		this.bgvolume = bgvolume;
	}

	public boolean isShowpastnote() {
		return showpastnote;
	}

	public void setShowpastnote(boolean showpastnote) {
		this.showpastnote = showpastnote;
	}

	public int getAudioDriver() {
		return audioDriver;
	}

	public void setAudioDriver(int audioDriver) {
		this.audioDriver = audioDriver;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIrname() {
		return irname;
	}

	public void setIrname(String irname) {
		this.irname = irname;
	}

	public String getAudioDriverName() {
		return audioDriverName;
	}

	public void setAudioDriverName(String audioDriverName) {
		this.audioDriverName = audioDriverName;
	}
	
	public void validate() {
		if(skin.length < 16) {
			skin = Arrays.copyOf(skin, 16);
			Logger.getGlobal().warning("skinを再構成");
		}

		if(mode14 == null || mode14.getController().length < 2) {
			mode14 = new PlayConfig(
					new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
							Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN,
							Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
					new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
							BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 },
						{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
								BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
					MidiConfig.default14());
			Logger.getGlobal().warning("mode14のPlayConfigを再構成");
		}
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public int getBgaExpand() {
		return bgaExpand;
	}

	public void setBgaExpand(int bgaExpand) {
		this.bgaExpand = bgaExpand;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public PlayerConfig[] getPlayers() {
		return players;
	}

	public void setPlayers(PlayerConfig[] players) {
		this.players = players;
	}

	public boolean isCacheSkinImage() {
		return cacheSkinImage;
	}

	public void setCacheSkinImage(boolean cacheSkinImage) {
		this.cacheSkinImage = cacheSkinImage;
	}

	public static class SkinConfig {

		public static final String DEFAULT_PLAY7 = "skin/default/play7.json";
		public static final String DEFAULT_PLAY5 = "skin/default/play5.json";
		public static final String DEFAULT_PLAY14 = "skin/default/play14.json";
		public static final String DEFAULT_PLAY10 = "skin/default/play10.json";
		public static final String DEFAULT_PLAY9 = "skin/default/play9.json";
		public static final String DEFAULT_SELECT = "skin/default/select.json";
		public static final String DEFAULT_DECIDE = "skin/default/decide.json";
		public static final String DEFAULT_RESULT = "skin/default/result.json";
		public static final String DEFAULT_GRADERESULT = "skin/default/graderesult.json";

		public static final Map<SkinType, String> defaultSkinPathMap = new HashMap<SkinType, String>() {
			{
				put(SkinType.PLAY_7KEYS, DEFAULT_PLAY7);
				put(SkinType.PLAY_5KEYS, DEFAULT_PLAY5);
				put(SkinType.PLAY_14KEYS, DEFAULT_PLAY14);
				put(SkinType.PLAY_10KEYS, DEFAULT_PLAY10);
				put(SkinType.PLAY_9KEYS, DEFAULT_PLAY9);
				put(SkinType.MUSIC_SELECT, DEFAULT_SELECT);
				put(SkinType.DECIDE, DEFAULT_DECIDE);
				put(SkinType.RESULT, DEFAULT_RESULT);
				put(SkinType.COURSE_RESULT, DEFAULT_GRADERESULT);
			}
		};

		private String path;

		private Map property;

		public SkinConfig() {

		}

		public SkinConfig(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Map getProperty() {
			return property;
		}

		public void setProperty(Map property) {
			this.property = property;
		}
	}
}
