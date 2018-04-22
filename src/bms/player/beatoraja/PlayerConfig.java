package bms.player.beatoraja;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.play.TargetProperty;
import bms.player.beatoraja.skin.SkinType;

import bms.model.Mode;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * プレイヤー毎の設定項目
 * 
 * @author exch
 */
public class PlayerConfig {

	private String id;
    /**
     * プレイヤーネーム
     */
    private String name = "NO NAME";
    
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

	private int target;
	/**
	 * 判定タイミング
	 */
	private int judgetiming = 0;

    /**
     * 選曲時のモードフィルター
     */
	private Mode mode = null;
	/**
	 * 指定がない場合のミスレイヤー表示時間(ms)
	 */
	private int misslayerDuration = 500;
	
	/**
	 * LNモード
	 */
	private int lnmode = 0;
	/**
	 * アシストオプション:コンスタント
	 */
	private boolean constant = false;
	/**
	 * アシストオプション:LNアシスト
	 */
	private boolean legacynote = false;
	/**
	 * アシストオプション:判定拡大
	 */
	private int judgewindowrate = 100;
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

	/**
	 * H-RANDOM連打しきい値BPM
	 */
	private int hranThresholdBPM = 120;
	/**
	 * プレイ中のゲージ切替
	 */
	private int gaugeAutoShift = GAUGEAUTOSHIFT_NONE;
	
	public static final int GAUGEAUTOSHIFT_NONE = 0;
	public static final int GAUGEAUTOSHIFT_CONTINUE = 1;
	public static final int GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE = 2;
	public static final int GAUGEAUTOSHIFT_BESTCLEAR = 3;
	public static final int GAUGEAUTOSHIFT_SELECT_TO_UNDER = 4;

	/**
	 * 7to9 スクラッチ鍵盤位置関係 0:OFF 1:SC1KEY2~8 2:SC1KEY3~9 3:SC2KEY3~9 4:SC8KEY1~7 5:SC9KEY1~7 6:SC9KEY2~8
	 */
	private int sevenToNinePattern = 0;

	/**
	 * 7to9 スクラッチ処理タイプ 0:そのまま 1:連打回避 2:交互
	 */
	private int sevenToNineType = 0;

	/**
	 * Guide SE
	 */
	private boolean isGuideSE = false;

	/**
	 * Window Hold
	 */
	private boolean isWindowHold = false;

	private SkinConfig[] skin = new SkinConfig[SkinType.getMaxSkinTypeID() + 1];

	private PlayModeConfig mode7 = new PlayModeConfig(Mode.BEAT_7K);

	private PlayModeConfig mode14 = new PlayModeConfig(Mode.BEAT_14K);

	private PlayModeConfig mode9 = new PlayModeConfig(Mode.POPN_9K);

	private PlayModeConfig mode24 = new PlayModeConfig(Mode.KEYBOARD_24K);

	private PlayModeConfig mode24double = new PlayModeConfig(Mode.KEYBOARD_24K_DOUBLE);

	/**
	 * 選曲時でのキー入力方式
	 */
	private int musicselectinput = 0;

	private String irname = "";

	private String userid = "";

	private String password = "";
	
	private int irsend = 0;
	
	public static final int IR_SEND_ALWAYS = 0;
	public static final int IR_SEND_COMPLETE_SONG = 1;
	public static final int IR_SEND_UPDATE_SCORE = 2;

	private String twitterConsumerKey;

	private String twitterConsumerSecret;

	private String twitterAccessToken;

	private String twitterAccessTokenSecret;
	
	public PlayerConfig() {
		validate();
	}
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

	public int getGaugeAutoShift() {
		return gaugeAutoShift;
	}

	public void setGaugeAutoShift(int gaugeAutoShift) {
		this.gaugeAutoShift = gaugeAutoShift;
	}
	
	public int getLnmode() {
		return lnmode;
	}

	public void setLnmode(int lnmode) {
		this.lnmode = lnmode;
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

	public PlayModeConfig getPlayConfig(Mode modeId) {
		switch (modeId) {
		case BEAT_5K:
		case BEAT_7K:
			return getMode7();
		case BEAT_10K:
		case BEAT_14K:
			return getMode14();
		case POPN_9K:
			return getMode9();
		case KEYBOARD_24K:
			return getMode24();
		case KEYBOARD_24K_DOUBLE:
			return getMode24double();
		default:
			return getMode7();
		}
	}

	public PlayModeConfig getPlayConfig(int modeId) {
		switch (modeId) {
		case 7:
		case 5:
			return getMode7();
		case 14:
		case 10:
			return getMode14();
		case 9:
			return getMode9();
		case 25:
			return getMode24();
		case 50:
			return getMode24double();
		default:
			return getMode7();
		}
	}

	public PlayModeConfig getMode7() {
		return mode7;
	}

	public void setMode7(PlayModeConfig mode7) {
		this.mode7 = mode7;
	}

	public PlayModeConfig getMode14() {
		if(mode14 == null || mode14.getController().length < 2) {
			mode14 = new PlayModeConfig(Mode.BEAT_14K);
			Logger.getGlobal().warning("mode14のPlayConfigを再構成");
		}
		return mode14;
	}

	public void setMode14(PlayModeConfig mode14) {
		this.mode14 = mode14;
	}

	public PlayModeConfig getMode9() {
		return mode9;
	}

	public void setMode9(PlayModeConfig mode9) {
		this.mode9 = mode9;
	}

	public PlayModeConfig getMode24() {
		return mode24;
	}

	public void setMode24(PlayModeConfig mode24) {
		this.mode24 = mode24;
	}

	public PlayModeConfig getMode24double() {
		if(mode24double == null || mode24double.getController().length < 2) {
			mode24double = new PlayModeConfig(Mode.KEYBOARD_24K_DOUBLE);
			Logger.getGlobal().warning("mode24doubleのPlayConfigを再構成");
		}
		return mode24double;
	}

	public void setMode24double(PlayModeConfig mode24double) {
		this.mode24double = mode24double;
	}

	public void setMode(Mode m)  {
		this.mode = m;
	}
	
	public Mode getMode()  {
		return mode;
	}
	
	public int getMusicselectinput() {
		return musicselectinput;
	}

	public void setMusicselectinput(int musicselectinput) {
		this.musicselectinput = musicselectinput;
	}

	public SkinConfig[] getSkin() {
		if(skin.length <= SkinType.getMaxSkinTypeID()) {
			skin = Arrays.copyOf(skin, SkinType.getMaxSkinTypeID() + 1);
			Logger.getGlobal().warning("skinを再構成");
		}
		return skin;
	}

	public void setSkin(SkinConfig[] skin) {
		this.skin = skin;
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

	public int getIrsend() {
		return irsend;
	}

	public void setIrsend(int irsend) {
		this.irsend = irsend;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public int getMisslayerDuration() {
		if(misslayerDuration < 0) {
			misslayerDuration = 0;
		}
		return misslayerDuration;
	}

	public void setMisslayerDuration(int misslayerTime) {
		this.misslayerDuration = misslayerTime;
	}

	public int getJudgewindowrate() {
		if(judgewindowrate < 25 || judgewindowrate > 400) {
			judgewindowrate = 100;
		}
		return judgewindowrate;
	}

	public void setJudgewindowrate(int judgewindowrate) {
		this.judgewindowrate = judgewindowrate;
	}

	public int getHranThresholdBPM() {
		return hranThresholdBPM;
	}

	public void setHranThresholdBPM(int hranThresholdBPM) {
		this.hranThresholdBPM = hranThresholdBPM;
	}

	public int getSevenToNinePattern() {
		return sevenToNinePattern;
	}

	public void setSevenToNinePattern(int sevenToNinePattern) {
		this.sevenToNinePattern = sevenToNinePattern;
	}

	public int getSevenToNineType() {
		return sevenToNineType;
	}

	public void setSevenToNineType(int sevenToNineType) {
		this.sevenToNineType = sevenToNineType;
	}

	public boolean isGuideSE() {
		return isGuideSE;
	}

	public void setGuideSE(boolean isGuideSE) {
		this.isGuideSE = isGuideSE;
	}

	public boolean isWindowHold() {
		return isWindowHold;
	}

	public void setWindowHold(boolean isWindowHold) {
		this.isWindowHold = isWindowHold;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTwitterConsumerKey() {
		return twitterConsumerKey;
	}

	public void setTwitterConsumerKey(String twitterConsumerKey) {
		this.twitterConsumerKey = twitterConsumerKey;
	}

	public String getTwitterConsumerSecret() {
		return twitterConsumerSecret;
	}

	public void setTwitterConsumerSecret(String twitterConsumerSecret) {
		this.twitterConsumerSecret = twitterConsumerSecret;
	}

	public String getTwitterAccessToken() {
		return twitterAccessToken;
	}

	public void setTwitterAccessToken(String twitterAccessToken) {
		this.twitterAccessToken = twitterAccessToken;
	}

	public String getTwitterAccessTokenSecret() {
		return twitterAccessTokenSecret;
	}

	public void setTwitterAccessTokenSecret(String twitterAccessTokenSecret) {
		this.twitterAccessTokenSecret = twitterAccessTokenSecret;
	}

	public void validate() {
		if(skin == null) {
			skin = new SkinConfig[SkinType.getMaxSkinTypeID() + 1];
		}
		if(skin.length != SkinType.getMaxSkinTypeID() + 1) {
			skin = Arrays.copyOf(skin, SkinType.getMaxSkinTypeID() + 1);
		}
		for(int i = 0;i < skin.length;i++) {
			if(skin[i] == null) {
				skin[i] = SkinConfig.getDefault(i);
			}
			skin[i].validate();
		}

		if(mode7 == null) {
			mode7 = new PlayModeConfig(Mode.BEAT_7K);
		}
		if(mode14 == null) {
			mode14 = new PlayModeConfig(Mode.BEAT_14K);
		}
		if(mode9 == null) {
			mode9 = new PlayModeConfig(Mode.POPN_9K);			
		}
		if(mode24 == null) {
			mode24 = new PlayModeConfig(Mode.KEYBOARD_24K);
		}
		if(mode24double == null) {
			mode24double = new PlayModeConfig(Mode.KEYBOARD_24K_DOUBLE);
		}
		mode7.validate(9);
		mode14.validate(18);
		mode9.validate(9);
		mode24.validate(26);
		mode24double.validate(52);
		
		gauge = MathUtils.clamp(gauge, 0, 5);
		random = MathUtils.clamp(random, 0, 9);
		random2 = MathUtils.clamp(random2, 0, 9);
		doubleoption = MathUtils.clamp(doubleoption, 0, 3);
		target = MathUtils.clamp(target, 0, TargetProperty.getAllTargetProperties().length);
		judgetiming = MathUtils.clamp(judgetiming, -100, 100);
		misslayerDuration = MathUtils.clamp(misslayerDuration, 0, 5000);
		lnmode = MathUtils.clamp(lnmode, 0, 2);
		judgewindowrate = MathUtils.clamp(judgewindowrate, 10, 400);
		hranThresholdBPM = MathUtils.clamp(hranThresholdBPM, 1, 1000);
		sevenToNinePattern = MathUtils.clamp(sevenToNinePattern, 0, 6);
		sevenToNineType = MathUtils.clamp(sevenToNineType, 0, 2);
		
		irsend = MathUtils.clamp(irsend, 0, 2);

	}

	public static void init(Config config) {
		// TODO プレイヤーアカウント検証
		try {
			if(!Files.exists(Paths.get("player"))) {
				Files.createDirectory(Paths.get("player"));
			}
			if(readAllPlayerID().length == 0 || readPlayerConfig(config.getPlayername()) == null) {
				PlayerConfig pc = new PlayerConfig();
				create("player1");
				// スコアデータコピー
				if(Files.exists(Paths.get("playerscore.db"))) {
					Files.copy(Paths.get("playerscore.db"), Paths.get("player/player1/score.db"));
				}
				// リプレイデータコピー
				Files.createDirectory(Paths.get("player/player1/replay"));
				if(Files.exists(Paths.get("replay"))) {
					try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("replay"))) {
						for (Path p : paths) {
							Files.copy(p, Paths.get("player/player1/replay").resolve(p.getFileName()));
						}
					} catch(Throwable e) {
						e.printStackTrace();
					}
				}

				config.setPlayername("player1");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void create(String playerid) {
		try {
			Path p = Paths.get("player/" + playerid);
			if(Files.exists(p)) {
				return;
			}
			Files.createDirectory(p);
			PlayerConfig player = new PlayerConfig();
			player.setId(playerid);
			write(player);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] readAllPlayerID() {
		List<String> l = new ArrayList<>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("player"))) {
			for (Path p : paths) {
				if(Files.isDirectory(p)) {
					l.add(p.getFileName().toString());
				}
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return l.toArray(new String[l.size()]);
	}

	public static PlayerConfig readPlayerConfig(String playerid) {
		PlayerConfig player = new PlayerConfig();
		Path p = Paths.get("player/" + playerid + "/config.json");
		Json json = new Json();
		try {
			json.setIgnoreUnknownFields(true);
			player = json.fromJson(PlayerConfig.class, new FileReader(p.toFile()));
			player.setId(playerid);
			player.validate();
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return player;
	}

	public static void write(PlayerConfig player) {
		Json json = new Json();
		json.setOutputType(JsonWriter.OutputType.json);
		Path p = Paths.get("player/" + player.getId() + "/config.json");
		try (FileWriter fw = new FileWriter(p.toFile())) {
			fw.write(json.prettyPrint(player));
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
