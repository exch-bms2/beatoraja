package bms.player.beatoraja;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.ir.IRConnectionManager;
import bms.player.beatoraja.pattern.*;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.select.BarSorter;
import bms.player.beatoraja.skin.SkinType;

import bms.model.Mode;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.SerializationException;

/**
 * プレイヤー毎の設定項目
 *
 * @author exch
 */
public final class PlayerConfig {

	/**
	 * 旧コンフィグパス。そのうち削除
	 */
	static final Path configpath_old = Paths.get("config.json");
	/**
	 * コンフィグパス(UTF-8)
	 */
	static final Path configpath = Paths.get("config_player.json");	

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
	
	private String chartReplicationMode = "RIVALCHART";

	/**
	 * スコアターゲット
	 */
	private String targetid = "MAX";
	
	private String[] targetlist = new String[] {"RATE_A-","RATE_A", "RATE_A+","RATE_AA-","RATE_AA", "RATE_AA+", "RATE_AAA-", "RATE_AAA", "RATE_AAA+", "RATE_MAX-", "MAX"
			,"RANK_NEXT", "IR_NEXT_1", "IR_NEXT_2", "IR_NEXT_3", "IR_NEXT_4", "IR_NEXT_5", "IR_NEXT_10"
			, "IR_RANK_1", "IR_RANK_5", "IR_RANK_10", "IR_RANK_20", "IR_RANK_30", "IR_RANK_40", "IR_RANK_50"
			, "IR_RANKRATE_5", "IR_RANKRATE_10", "IR_RANKRATE_15", "IR_RANKRATE_20", "IR_RANKRATE_25", "IR_RANKRATE_30", "IR_RANKRATE_35", "IR_RANKRATE_40", "IR_RANKRATE_45","IR_RANKRATE_50"
			,"RIVAL_RANK_1","RIVAL_RANK_2","RIVAL_RANK_3","RIVAL_NEXT_1","RIVAL_NEXT_2","RIVAL_NEXT_3"};
	/**
	 * 判定タイミング
	 */
	private int judgetiming = 0;
	
	public static final int JUDGETIMING_MAX = 500;
	public static final int JUDGETIMING_MIN = -500;
	
	/**
	 * ディスプレイ表示タイミング自動調整
	 */
	private boolean notesDisplayTimingAutoAdjust = false;

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
	 * スクロール追加/削除モード
	 */
    private int scrollMode = 0;
    private int scrollSection = 4;
    private double scrollRate = 0.5;
	/**
	 * ロングノート追加/削除モード
	 */
    private int longnoteMode = 0;
    private double longnoteRate = 1.0;
	/**
	 * アシストオプション:カスタムジャッジ
	 */
	private boolean customJudge = false;
	private int keyJudgeWindowRatePerfectGreat = 400;
	private int keyJudgeWindowRateGreat = 400;
	private int keyJudgeWindowRateGood = 100;
	private int scratchJudgeWindowRatePerfectGreat = 400;
	private int scratchJudgeWindowRateGreat = 400;
	private int scratchJudgeWindowRateGood = 100;

	/**
	 * 地雷モード
	 */
	private int mineMode = 0;
	/**
	 * アシストオプション:BPMガイド
	 */
	private boolean bpmguide = false;

	private int extranoteType = 0;
	private int extranoteDepth = 0;
	private boolean extranoteScratch = false;

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
	/**
	 * GASで遷移可能なゲージの下限  ASSIST EASY, EASY, NORMALから選択
	 */
	private int bottomShiftableGauge = GrooveGauge.ASSISTEASY;

	public static final int GAUGEAUTOSHIFT_NONE = 0;
	public static final int GAUGEAUTOSHIFT_CONTINUE = 1;
	public static final int GAUGEAUTOSHIFT_SURVIVAL_TO_GROOVE = 2;
	public static final int GAUGEAUTOSHIFT_BESTCLEAR = 3;
	public static final int GAUGEAUTOSHIFT_SELECT_TO_UNDER = 4;

	private int autosavereplay[];

	/**
	 * 7to9 スクラッチ鍵盤位置関係 0:OFF 1:SC1KEY2~8 2:SC1KEY3~9 3:SC2KEY3~9 4:SC8KEY1~7 5:SC9KEY1~7 6:SC9KEY2~8
	 */
	private int sevenToNinePattern = 0;

	/**
	 * 7to9 スクラッチ処理タイプ 0:そのまま 1:連打回避 2:交互
	 */
	private int sevenToNineType = 0;

	/**
	 * START+SELECTを押すと終了するまでの時間
	 */
	private int exitPressDuration = 1000;

	/**
	 * Guide SE
	 */
	private boolean isGuideSE = false;

	/**
	 * Window Hold
	 */
	private boolean isWindowHold = false;
	
	/**
	 * Enable folder random select bar
	 */
	private boolean isRandomSelect = false;

	private SkinConfig[] skin = new SkinConfig[SkinType.getMaxSkinTypeID() + 1];
	private SkinConfig[] skinHistory;

	private PlayModeConfig mode5 = new PlayModeConfig(Mode.BEAT_5K);

	private PlayModeConfig mode7 = new PlayModeConfig(Mode.BEAT_7K);

	private PlayModeConfig mode10 = new PlayModeConfig(Mode.BEAT_10K);

	private PlayModeConfig mode14 = new PlayModeConfig(Mode.BEAT_14K);

	private PlayModeConfig mode9 = new PlayModeConfig(Mode.POPN_9K);

	private PlayModeConfig mode24 = new PlayModeConfig(Mode.KEYBOARD_24K);

	private PlayModeConfig mode24double = new PlayModeConfig(Mode.KEYBOARD_24K_DOUBLE);
	/**
	 * HIDDENノートを表示するかどうか
	 */
	private boolean showhiddennote = false;
	/**
	 * 通過ノートを表示するかどうか
	 */
	private boolean showpastnote = false;
	
	/**
	 * チャートプレビューを使用するかどうか
	 */
	private boolean chartPreview = true;
	
	/**
	 * 選択中の選曲時ソート
	 */
	private int sort;
	/**
	 * 選択中の選曲時ソート
	 */
	private String sortid;

	/**
	 * 選曲時でのキー入力方式
	 */
	private int musicselectinput = 0;

	private IRConfig[] irconfig;
	
	private String twitterConsumerKey;

	private String twitterConsumerSecret;

	private String twitterAccessToken;

	private String twitterAccessTokenSecret;

	// -- Stream
	private boolean enableRequest = false;
	private boolean notifyRequest = false;
	private int maxRequestCount = 30;

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

	public boolean isNotesDisplayTimingAutoAdjust() {
		return notesDisplayTimingAutoAdjust;
	}

	public void setNotesDisplayTimingAutoAdjust(boolean notesDisplayTimingAutoAdjust) {
		this.notesDisplayTimingAutoAdjust = notesDisplayTimingAutoAdjust;
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

	public int getBottomShiftableGauge() {
		return bottomShiftableGauge;
	}

	public void setBottomShiftableGauge(int bottomShiftableGauge) {
		this.bottomShiftableGauge = bottomShiftableGauge;
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

	public int getExtranoteDepth() {
		return extranoteDepth;
	}

	public void setExtranoteDepth(int extranoteDepth) {
		this.extranoteDepth = extranoteDepth;
	}

	public int getExtranoteType() {
		return extranoteType;
	}

	public void setExtranoteType(int extranoteType) {
		this.extranoteType = extranoteType;
	}

	public boolean isExtranoteScratch() {
		return extranoteScratch;
	}

	public void setExtranoteScratch(boolean extranoteScratch) {
		this.extranoteScratch = extranoteScratch;
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
		switch (modeId != null ? modeId : Mode.BEAT_7K) {
		case BEAT_5K:
			return getMode5();
		case BEAT_7K:
			return getMode7();
		case BEAT_10K:
			return getMode10();
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
			return getMode7();
		case 5:
			return getMode5();
		case 14:
			return getMode14();
		case 10:
			return getMode10();
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

	public PlayModeConfig getMode5() {
		return mode5;
	}

	public void setMode5(PlayModeConfig mode5) {
		this.mode5 = mode5;
	}

	public PlayModeConfig getMode7() {
		return mode7;
	}

	public void setMode7(PlayModeConfig mode7) {
		this.mode7 = mode7;
	}

	public PlayModeConfig getMode10() {
		if(mode10 == null || mode10.getController().length < 2) {
			mode10 = new PlayModeConfig(Mode.BEAT_10K);
			Logger.getGlobal().warning("mode10のPlayConfigを再構成");
		}
		return mode10;
	}

	public void setMode10(PlayModeConfig mode10) {
		this.mode10 = mode10;
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
	
	public int getSort() {
		return this.sort ;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

	public String getSortid() {
		return sortid;
	}

	public void setSortid(String sortid) {
		this.sortid = sortid;
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

	public SkinConfig[] getSkinHistory() {
		return skinHistory;
	}

	public void setSkinHistory(SkinConfig[] skinHistory) {
		this.skinHistory = skinHistory;
	}

	public IRConfig[] getIrconfig() {
		return irconfig;
	}

	public void setIrconfig(IRConfig[] irconfig) {
		this.irconfig = irconfig;
	}

	public String getTargetid() {
		return targetid;
	}

	public void setTargetid(String targetid) {
		this.targetid = targetid;
	}

	public String[] getTargetlist() {
		return targetlist;
	}

	public void setTargetlist(String[] targetlist) {
		this.targetlist = targetlist;
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

	public boolean isCustomJudge() {
		return customJudge;
	}

	public void setCustomJudge(boolean customJudge) {
		this.customJudge = customJudge;
	}

	public int getKeyJudgeWindowRatePerfectGreat() {
		return keyJudgeWindowRatePerfectGreat;
	}

	public void setKeyJudgeWindowRatePerfectGreat(int judgeWindowRatePerfectGreat) {
		this.keyJudgeWindowRatePerfectGreat = judgeWindowRatePerfectGreat;
	}

	public int getKeyJudgeWindowRateGreat() {
		return keyJudgeWindowRateGreat;
	}

	public void setKeyJudgeWindowRateGreat(int judgeWindowRateGreat) {
		this.keyJudgeWindowRateGreat = judgeWindowRateGreat;
	}

	public int getKeyJudgeWindowRateGood() {
		return keyJudgeWindowRateGood;
	}

	public void setKeyJudgeWindowRateGood(int judgeWindowRateGood) {
		this.keyJudgeWindowRateGood = judgeWindowRateGood;
	}

	public int getScratchJudgeWindowRatePerfectGreat() {
		return scratchJudgeWindowRatePerfectGreat;
	}

	public void setScratchJudgeWindowRatePerfectGreat(int judgeWindowRatePerfectGreat) {
		this.scratchJudgeWindowRatePerfectGreat = judgeWindowRatePerfectGreat;
	}

	public int getScratchJudgeWindowRateGreat() {
		return scratchJudgeWindowRateGreat;
	}

	public void setScratchJudgeWindowRateGreat(int judgeWindowRateGreat) {
		this.scratchJudgeWindowRateGreat = judgeWindowRateGreat;
	}

	public int getScratchJudgeWindowRateGood() {
		return scratchJudgeWindowRateGood;
	}

	public void setScratchJudgeWindowRateGood(int judgeWindowRateGood) {
		this.scratchJudgeWindowRateGood = judgeWindowRateGood;
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

	public int getExitPressDuration() {
		return exitPressDuration;
	}

	public void setExitPressDuration(int exitPressDuration) {
		this.exitPressDuration = exitPressDuration;
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
	
	public boolean isRandomSelect() {
		return isRandomSelect;
	}
	
	public void setRandomSelect(boolean isRandomSelect) {
		this.isRandomSelect = isRandomSelect;
	}

	public boolean isChartPreview() {
		return chartPreview;
	}

	public void setChartPreview(boolean chartPreview) {
		this.chartPreview = chartPreview;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setAutoSaveReplay(int autoSaveReplay[]){
		this.autosavereplay = autoSaveReplay;
	}

	public int[] getAutoSaveReplay(){
		return autosavereplay;
	}

	public boolean isShowhiddennote() {
		return showhiddennote;
	}

	public void setShowhiddennote(boolean showhiddennote) {
		this.showhiddennote = showhiddennote;
	}

	public boolean isShowpastnote() {
		return showpastnote;
	}

	public void setShowpastnote(boolean showpastnote) {
		this.showpastnote = showpastnote;
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
	
	// --Stream
	public boolean getRequestEnable() {
        return enableRequest;
    }

	public boolean getRequestNotify() {
        return notifyRequest;
    }

    public void setRequestEnable(boolean requestEnable) {
        this.enableRequest = requestEnable;
    }

    public void setRequestNotify(boolean notifyEnable) {
        this.notifyRequest = notifyEnable;
    }

    public int getMaxRequestCount() {
        return maxRequestCount;
    }

    public void setMaxRequestCount(int maxRequestCount) {
        this.maxRequestCount = maxRequestCount;
    }

	public void validate() {
		if(skin == null) {
			skin = new SkinConfig[SkinType.getMaxSkinTypeID() + 1];
		}
		if(skinHistory == null) {
			skinHistory = new SkinConfig[0];
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

		if(mode5 == null) {
			mode5 = new PlayModeConfig(Mode.BEAT_5K);
		}

		if(mode7 == null) {
			mode7 = new PlayModeConfig(Mode.BEAT_7K);
		}
		if(mode14 == null) {
			mode14 = new PlayModeConfig(Mode.BEAT_14K);
		}
		if(mode10 == null) {
			mode10 = new PlayModeConfig(Mode.BEAT_10K);
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
		mode5.validate(7);
		mode7.validate(9);
		mode10.validate(14);
		mode14.validate(18);
		mode9.validate(9);
		mode24.validate(26);
		mode24double.validate(52);

		sort = MathUtils.clamp(sort, 0 , BarSorter.defaultSorter.length - 1);
		if(sortid == null) {
			sortid = BarSorter.defaultSorter[sort].name();
		}

		gauge = MathUtils.clamp(gauge, 0, 5);
		random = MathUtils.clamp(random, 0, 9);
		random2 = MathUtils.clamp(random2, 0, 9);
		doubleoption = MathUtils.clamp(doubleoption, 0, 3);
		chartReplicationMode = chartReplicationMode != null ? chartReplicationMode : "NONE";
		targetid = targetid!= null ? targetid : "MAX";
		targetlist = targetlist != null ? targetlist : new String[0];
		judgetiming = MathUtils.clamp(judgetiming, JUDGETIMING_MIN, JUDGETIMING_MAX);
		misslayerDuration = MathUtils.clamp(misslayerDuration, 0, 5000);
		lnmode = MathUtils.clamp(lnmode, 0, 2);
		keyJudgeWindowRatePerfectGreat = MathUtils.clamp(keyJudgeWindowRatePerfectGreat, 25, 400);
		keyJudgeWindowRateGreat = MathUtils.clamp(keyJudgeWindowRateGreat, 0, 400);
		keyJudgeWindowRateGood = MathUtils.clamp(keyJudgeWindowRateGood, 0, 400);
		scratchJudgeWindowRatePerfectGreat = MathUtils.clamp(scratchJudgeWindowRatePerfectGreat, 25, 400);
		scratchJudgeWindowRateGreat = MathUtils.clamp(scratchJudgeWindowRateGreat, 0, 400);
		scratchJudgeWindowRateGood = MathUtils.clamp(scratchJudgeWindowRateGood, 0, 400);
		hranThresholdBPM = MathUtils.clamp(hranThresholdBPM, 1, 1000);
		
		if(autosavereplay == null) {
			autosavereplay = new int[4];
		}
		if(autosavereplay.length != 4) {
			autosavereplay = Arrays.copyOf(autosavereplay, 4);
		}
		sevenToNinePattern = MathUtils.clamp(sevenToNinePattern, 0, 6);
		sevenToNineType = MathUtils.clamp(sevenToNineType, 0, 2);
		exitPressDuration = MathUtils.clamp(exitPressDuration, 0, 100000);

		scrollMode = MathUtils.clamp(scrollMode, 0, ScrollSpeedModifier.Mode.values().length);
		scrollSection = MathUtils.clamp(scrollSection, 1, 1024);
		scrollRate = MathUtils.clamp(scrollRate, 0, 1.0);
		longnoteMode = MathUtils.clamp(longnoteMode, 0, LongNoteModifier.Mode.values().length);
		longnoteRate = MathUtils.clamp(longnoteRate, 0.0, 1.0);
		mineMode = MathUtils.clamp(mineMode, 0, MineNoteModifier.Mode.values().length);
		extranoteDepth = MathUtils.clamp(extranoteDepth, 0, 100);

		if(irconfig == null || irconfig.length == 0) {
			String[] irnames = IRConnectionManager.getAllAvailableIRConnectionName();
			irconfig = new IRConfig[irnames.length];
			for(int i = 0;i < irnames.length;i++) {
				irconfig[i] = new IRConfig();
				irconfig[i].setIrname(irnames[i]);
			}
		}
		
		for(int i = 0;i < irconfig.length;i++) {
			if(irconfig[i] == null || irconfig[i].getIrname() == null) {
				continue;
			}
			for(int j = i + 1;j < irconfig.length;j++) {
				if(irconfig[j] != null && irconfig[i].getIrname().equals(irconfig[j].getIrname())) {
					irconfig[j].setIrname(null);
				}				
			}
		}
		irconfig = Validatable.removeInvalidElements(irconfig);

		// --Stream
		maxRequestCount = MathUtils.clamp(maxRequestCount, 0, 100);
	}

	public static void init(Config config) {
		// TODO プレイヤーアカウント検証
		try {
			if(!Files.exists(Paths.get(config.getPlayerpath()))) {
				Files.createDirectory(Paths.get(config.getPlayerpath()));
			}
			if(readAllPlayerID(config.getPlayerpath()).length == 0 || readPlayerConfig(config.getPlayerpath(), config.getPlayername()) == null) {
				PlayerConfig pc = new PlayerConfig();
				create(config.getPlayerpath(), "player1");
				// スコアデータコピー
				if(Files.exists(Paths.get("playerscore.db"))) {
					Files.copy(Paths.get("playerscore.db"), Paths.get(config.getPlayerpath() + "/player1/score.db"));
				}
				// リプレイデータコピー
				Files.createDirectory(Paths.get(config.getPlayerpath() + "/player1/replay"));
				if(Files.exists(Paths.get("replay"))) {
					try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("replay"))) {
						for (Path p : paths) {
							Files.copy(p, Paths.get(config.getPlayerpath() + "/player1/replay").resolve(p.getFileName()));
						}
					} catch(Throwable e) {
						e.printStackTrace();
					}
				}

				config.setPlayername("player1");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void create(String playerpath, String playerid) {
		try {
			Path p = Paths.get(playerpath + "/" + playerid);
			if(Files.exists(p)) {
				return;
			}
			Files.createDirectory(p);
			PlayerConfig player = new PlayerConfig();
			player.setId(playerid);
			write(playerpath, player);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] readAllPlayerID(String playerpath) {
		List<String> l = new ArrayList<>();
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(playerpath))) {
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

	public static PlayerConfig readPlayerConfig(String playerpath, String playerid) {
		PlayerConfig player = new PlayerConfig();
		final Path path = Paths.get(playerpath + "/" + playerid + "/" + configpath);
		final Path path_old = Paths.get(playerpath + "/" + playerid + "/" + configpath_old);
		if (Files.exists(path)) {
			try (Reader reader = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
				Json json = new Json();
				json.setIgnoreUnknownFields(true);
				player = json.fromJson(PlayerConfig.class, reader);
			} catch (SerializationException e) {
				Logger.getGlobal().warning("PlayerConfigの読み込み失敗 - Path : " + path.toString() + " , Log : " + e.getMessage());
				try {
					Files.copy(path, Paths.get(playerpath + "/" + playerid + "/config_backup.json"));
				} catch (IOException e1) {
//					e1.printStackTrace();
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}			
		} else if(Files.exists(path_old)) {
			// 旧コンフィグ読み込み。そのうち削除
			try (FileReader reader = new FileReader(path_old.toFile())) {
				Json json = new Json();
				json.setIgnoreUnknownFields(true);
				player = json.fromJson(PlayerConfig.class, reader);
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
		player.setId(playerid);
		player.validate();
		return player;
	}

	public static void write(String playerpath, PlayerConfig player) {
		try (Writer writer = new OutputStreamWriter(
				new FileOutputStream(Paths.get(playerpath + "/" + player.getId() + "/" + configpath).toFile()), StandardCharsets.UTF_8)) {
			Json json = new Json();
			json.setOutputType(JsonWriter.OutputType.json);
			json.setUsePrototypes(false);
			writer.write(json.prettyPrint(player));
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getMineMode() {
		return mineMode;
	}

	public void setMineMode(int mineMode) {
		this.mineMode = mineMode;
	}

    public int getScrollMode() {
        return scrollMode;
    }

    public void setScrollMode(int scrollMode) {
        this.scrollMode = scrollMode;
    }
    
	public int getScrollSection() {
		return scrollSection;
	}

	public void setScrollSection(int scrollSection) {
		this.scrollSection = scrollSection;
	}

	public double getScrollRate() {
		return scrollRate;
	}

	public void setScrollRate(double scrollRate) {
		this.scrollRate = scrollRate;
	}

    public int getLongnoteMode() {
        return longnoteMode;
    }

    public void setLongnoteMode(int longnoteMode) {
        this.longnoteMode = longnoteMode;
    }

    public double getLongnoteRate() {
        return longnoteRate;
    }

    public void setLongnoteRate(double longnoteRate) {
        this.longnoteRate = longnoteRate;
    }

    private boolean eventMode = false;

	public boolean isEventMode() {
		return eventMode;
	}

	public void setEventMode(boolean eventMode) {
		this.eventMode = eventMode;
	}

	public String getChartReplicationMode() {
		return chartReplicationMode;
	}

	public void setChartReplicationMode(String chartReplicationMode) {
		this.chartReplicationMode = chartReplicationMode;
	}
}
