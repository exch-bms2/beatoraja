package bms.player.beatoraja.play;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.math.Rectangle;


/**
 * プレイスキン
 *
 * @author exch
 */
public class PlaySkin extends Skin {
	/**
	 * STATE_READYからSTATE_PLAYに移行するまでのマージン(ms)
	 */
	private int playstart;

	private SkinImage[] line = new SkinImage[0];
	private SkinImage[] time = new SkinImage[0];
	private SkinImage[] bpm = new SkinImage[0];
	private SkinImage[] stop = new SkinImage[0];

	private Rectangle[] laneregion;
	private Rectangle[] lanegroupregion;

	private int judgeregion;
	/**
	 * STATE_FAILEDからプレイヤーを終了するまでのマージン(ms)
	 */
	private int close;

	/**
	 * STATE_FINISHEDからフェードアウトを開始するまでのマージン(ms)
	 */
	private int finishMargin = 0;

	private int loadstart;
	private int loadend;
	
	/**
	 * 各レーンの判定タイマーを発動するときの判定条件。(0:PG, 1:GR, 2:GD, 3:BD)
	 */
	private int judgetimer = 1;

	/**
	 * PMSのリズムに合わせたノートの拡大の最大拡大率(%) w h
	 */
	private int[] noteExpansionRate = {100,100};

	public SkinSlider laneCover;

	public final PomyuCharaProcessor pomyu = new PomyuCharaProcessor();
	
	public PlaySkin(SkinHeader header) {
		super(header);
	}
	
	public Rectangle[] getLaneGroupRegion() {
		return lanegroupregion;
	}

	public void setLaneGroupRegion(Rectangle[] r) {
		lanegroupregion = r;
	}

	public Rectangle[] getLaneRegion() {
		return laneregion;
	}

	public void setLaneRegion(Rectangle[] r) {
		laneregion = r;
	}

	public void setJudgeregion(int jr) {
		judgeregion = jr;
	}

	public int getJudgeregion() {
		return judgeregion;
	}

	public int getClose() {
		return close;
	}

	public void setClose(int close) {
		this.close = close;
	}

	public int getFinishMargin() {
		return finishMargin;
	}

	public void setFinishMargin(int finishMargin) {
		this.finishMargin = finishMargin;
	}

	public int getPlaystart() {
		return playstart;
	}

	public void setPlaystart(int playstart) {
		this.playstart = playstart;
	}

	public SkinImage[] getLine() {
		return line;
	}

	public SkinImage[] getBPMLine() {
		return bpm;
	}

	public SkinImage[] getStopLine() {
		return stop;
	}

	public void setLine(SkinImage[] line) {
		this.line = line;
	}

	public void setBPMLine(SkinImage[] bpm) {
		this.bpm = bpm;
	}

	public void setStopLine(SkinImage[] stop) {
		this.stop = stop;
	}

	public SkinImage[] getTimeLine() {
		return time;
	}
	
	public void setTimeLine(SkinImage[] time) {
		this.time = time;
	}

	public int getLoadstart() {
		return loadstart;
	}

	public void setLoadstart(int loadstart) {
		this.loadstart = loadstart;
	}

	public int getLoadend() {
		return loadend;
	}

	public void setLoadend(int loadend) {
		this.loadend = loadend;
	}
	
	public int getJudgetimer() {
		return judgetimer;
	}
	
	public void setJudgetimer(int judgetimer) {
		this.judgetimer = judgetimer;
	}

	public int[] getNoteExpansionRate() {
		return noteExpansionRate;
	}

	public void setNoteExpansionRate(int[] rate) {
		this.noteExpansionRate = rate;
	}

}
