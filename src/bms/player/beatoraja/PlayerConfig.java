package bms.player.beatoraja;

import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.Input.Keys;

import bms.model.Mode;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;

/**
 * プレイヤー毎の設定項目
 * 
 * @author exch
 */
public class PlayerConfig {

    /**
     * プレイヤーネーム
     */
    private String name;
    
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

	private SkinConfig[] skin;

	private PlayConfig mode7 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
					Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN,
					Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			PlayConfig.MidiConfig.default7());

	private PlayConfig mode14 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
					Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN,
					Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 },
				{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
						BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			PlayConfig.MidiConfig.default14());

	private PlayConfig mode9 = new PlayConfig(
			new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.G, Keys.B, Keys.COMMA, Keys.L,
					Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT,
					Keys.CONTROL_RIGHT, Keys.Q, Keys.W },
			new int[][] {{ BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8, BMKeys.BUTTON_2,
					BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_9, BMKeys.BUTTON_10 }},
			PlayConfig.MidiConfig.default9());

	private int musicselectinput = 0;

	private String irname = "";

	private String userid = "";

	private String password = "";

	public PlayerConfig() {
	}
	
	public PlayerConfig(Config c) {
		this.gauge = c.getGauge();
		this.random = c.getRandom();
		this.random2 = c.getRandom2();
		this.doubleoption = c.getDoubleoption();
		this.fixhispeed = c.getFixhispeed();
		this.target = c.getTarget();
		this.judgetiming = c.getJudgetiming();
		this.mode = c.getMode();
		this.constant = c.isConstant();
		this.legacynote = c.isLegacynote();
		this.lnmode = c.getLnmode();
		this.expandjudge = c.isExpandjudge();
		this.nomine = c.isNomine();
		this.bpmguide = c.isBpmguide();
		this.showjudgearea = c.isShowjudgearea();
		this.markprocessednote = c.isMarkprocessednote();
		this.skin = c.getSkin();
		this.mode7 = c.getMode7();
		this.mode14 = c.getMode14();
		this.mode9 = c.getMode9();
		this.musicselectinput = c.getMusicselectinput();
		this.irname = c.getIrname();
		this.userid = c.getUserid();
		this.password = c.getPassword();		
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

	public int getLnmode() {
		return lnmode;
	}

	public void setLnmode(int lnmode) {
		this.lnmode = lnmode;
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
					PlayConfig.MidiConfig.default14());
			Logger.getGlobal().warning("mode14のPlayConfigを再構成");
		}
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}
}
