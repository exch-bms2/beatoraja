package bms.player.beatoraja.play;

import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * プレイスキン
 * 
 * @author exch
 */
public class PlaySkin extends Skin {

	private int playstart;

	private TextureRegion[][] gauge;

	private TextureRegion[][] judge;

	private TextureRegion[][][] judgenum;

	private SkinGraph[] graph;

	private SkinImage[] line = new SkinImage[0];

	private Rectangle[] lanegroupregion;

	private int judgeregion;

	private float dw;
	private float dh;

	private int close;
	
	private int loadstart;
	private int loadend;

	private final int[] judgecount = { NUMBER_EARLY_PERFECT, NUMBER_LATE_PERFECT,
			NUMBER_EARLY_GREAT, NUMBER_LATE_GREAT, NUMBER_EARLY_GOOD,
			NUMBER_LATE_GOOD, NUMBER_EARLY_BAD, NUMBER_LATE_BAD,
			NUMBER_EARLY_POOR, NUMBER_LATE_POOR, NUMBER_EARLY_MISS,
			NUMBER_LATE_MISS };

	protected BMSPlayer player;
	
	private static final int[] fixop = {OPTION_STAGEFILE, OPTION_NO_STAGEFILE, OPTION_BACKBMP, OPTION_NO_BACKBMP, 
		OPTION_AUTOPLAYON, OPTION_AUTOPLAYOFF, OPTION_BGAON, OPTION_BGAOFF, 
		OPTION_BGANORMAL, OPTION_BGAEXTEND, OPTION_GHOST_OFF, OPTION_GHOST_A, OPTION_GHOST_B, 
		OPTION_GHOST_C, OPTION_GAUGE_GROOVE, OPTION_GAUGE_HARD,
		OPTION_GAUGE_GROOVE_2P, OPTION_GAUGE_HARD_2P,OPTION_OFFLINE, OPTION_ONLINE, 
		OPTION_SCOREGRAPHOFF, OPTION_SCOREGRAPHON,OPTION_DIFFICULTY0,OPTION_DIFFICULTY1
		,OPTION_DIFFICULTY2,OPTION_DIFFICULTY3,OPTION_DIFFICULTY4,OPTION_DIFFICULTY5,
		OPTION_NO_BPMCHANGE,OPTION_BPMCHANGE,
		OPTION_COURSE_STAGE1,OPTION_COURSE_STAGE2,OPTION_COURSE_STAGE3,OPTION_COURSE_STAGE4,OPTION_COURSE_STAGE_FINAL,
		OPTION_MODE_COURSE,OPTION_MODE_NONSTOP,OPTION_MODE_EXPERT,OPTION_MODE_GRADE};

	public PlaySkin(float srcw, float srch, float dstw, float dsth) {
		super(srcw, srch, dstw, dsth, fixop);
	}

	public Rectangle[] getLaneGroupRegion() {
		return lanegroupregion;
	}

	public void setLaneGroupRegion(Rectangle[] r) {
		lanegroupregion = r;
	}

	public void setJudgeregion(int jr) {
		judgeregion = jr;
	}

	public int getJudgeregion() {
		return judgeregion;
	}

	private Rectangle rect(float x, float y, float width, float height) {
		return new Rectangle(x * dw, y * dh, width * dw, height * dh);
	}

	public int getClose() {
		return close;
	}

	public void setClose(int close) {
		this.close = close;
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

	public void setLine(SkinImage[] line) {
		this.line = line;
	}

	public static class JudgeRegion {

		public SkinImage[] judge;
		public SkinNumber[] count;
		public boolean shift;

		public JudgeRegion(SkinImage[] judge, SkinNumber[] count, boolean shift) {
			this.judge = judge;
			this.count = count;
			this.shift = shift;
		}
	}

	public void setBMSPlayer(BMSPlayer player) {
		this.player = player;
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
	
}
