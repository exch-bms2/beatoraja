package bms.player.beatoraja;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.SkinType;

import bms.model.Mode;
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
	 * 途中閉店の有無
	 */
	private boolean continueUntilEndOfSong = false;

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

	private SkinConfig[] skin = new SkinConfig[SkinType.getMaxSkinTypeID() + 1];

	private PlayConfig mode7 = new PlayConfig(Mode.BEAT_7K);

	private PlayConfig mode14 = new PlayConfig(Mode.BEAT_14K);

	private PlayConfig mode9 = new PlayConfig(Mode.POPN_9K);

	private PlayConfig mode24 = new PlayConfig(Mode.KEYBOARD_24K);

	private PlayConfig mode24double = new PlayConfig(Mode.KEYBOARD_24K_DOUBLE);

	private int musicselectinput = 0;

	private String irname = "";

	private String userid = "";

	private String password = "";
	
	private int irsend = 0;
	
	private String twitterConsumerKey;

	private String twitterConsumerSecret;

	private String twitterAccessToken;

	private String twitterAccessTokenSecret;
	
	public static final int IR_SEND_ALWAYS = 0;
	public static final int IR_SEND_COMPLETE_SONG = 1;
	public static final int IR_SEND_UPDATE_SCORE = 2;

	public PlayerConfig() {
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

	public int getFixhispeed() {
		if(fixhispeed < 0 || fixhispeed > FIX_HISPEED_MINBPM) {
			fixhispeed = FIX_HISPEED_OFF;
		}
		return fixhispeed;
	}

	public void setFixhispeed(int fixhispeed) {
		this.fixhispeed = fixhispeed;
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

	public boolean isContinueUntilEndOfSong() {
		return continueUntilEndOfSong;
	}

	public void setContinueUntilEndOfSong(boolean continueUntilEndOfSong) {
		this.continueUntilEndOfSong = continueUntilEndOfSong;
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

	public PlayConfig getPlayConfig(Mode modeId) {
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

	public PlayConfig getPlayConfig(int modeId) {
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

	public PlayConfig getMode7() {
		return mode7;
	}

	public void setMode7(PlayConfig mode7) {
		this.mode7 = mode7;
	}

	public PlayConfig getMode14() {
		if(mode14 == null || mode14.getController().length < 2) {
			mode14 = new PlayConfig(Mode.BEAT_14K);
			Logger.getGlobal().warning("mode14のPlayConfigを再構成");
		}
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

	public PlayConfig getMode24() {
		return mode24;
	}

	public void setMode24(PlayConfig mode24) {
		this.mode24 = mode24;
	}

	public PlayConfig getMode24double() {
		if(mode24double == null || mode24double.getController().length < 2) {
			mode24double = new PlayConfig(Mode.KEYBOARD_24K_DOUBLE);
			Logger.getGlobal().warning("mode24doubleのPlayConfigを再構成");
		}
		return mode24double;
	}

	public void setMode24double(PlayConfig mode24double) {
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
		mode7.validate(9);
		mode14.validate(18);
		mode9.validate(9);
		mode24.validate(26);
		mode24double.validate(52);
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
