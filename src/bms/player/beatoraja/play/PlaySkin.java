package bms.player.beatoraja.play;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.*;

import com.badlogic.gdx.math.Rectangle;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static bms.player.beatoraja.skin.SkinProperty.*;

/**
 * プレイスキン
 *
 * @author exch
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PlaySkin extends Skin {
	/**
	 * STATE_READYからSTATE_PLAYに移行するまでのマージン(ms)
	 */
	private int playStart;

	private SkinImage[] line = new SkinImage[0];
	private SkinImage[] timeLine = new SkinImage[0];
	private SkinImage[] bpmLine = new SkinImage[0];
	private SkinImage[] stopLine = new SkinImage[0];

	private Rectangle[] laneRegion;
	private Rectangle[] laneGroupRegion;

	private int judgeRegion;
	/**
	 * STATE_FAILEDからプレイヤーを終了するまでのマージン(ms)
	 */
	private int close;

	/**
	 * STATE_FINISHEDからフェードアウトを開始するまでのマージン(ms)
	 */
	private int finishMargin = 0;

	private int loadStart;
	private int loadEnd;
	
	/**
	 * 各レーンの判定タイマーを発動するときの判定条件。(0:PG, 1:GR, 2:GD, 3:BD)
	 */
	private int judgeTimer = 1;

	/**
	 * PMSのリズムに合わせたノートの拡大の最大拡大率(%) w h
	 */
	private int[] noteExpansionRate = {100,100};

	private static final int[] fixop = {OPTION_STAGEFILE, OPTION_NO_STAGEFILE, OPTION_BACKBMP, OPTION_NO_BACKBMP,
		OPTION_AUTOPLAYON, OPTION_AUTOPLAYOFF, OPTION_BGAON, OPTION_BGAOFF,
		OPTION_BGANORMAL, OPTION_BGAEXTEND, OPTION_GHOST_OFF, OPTION_GHOST_A, OPTION_GHOST_B,
		OPTION_GHOST_C, OPTION_GAUGE_GROOVE, OPTION_GAUGE_HARD,OPTION_GAUGE_EX,
		OPTION_GAUGE_GROOVE_2P, OPTION_GAUGE_HARD_2P, OPTION_GAUGE_EX_2P,OPTION_OFFLINE, OPTION_ONLINE,
		OPTION_SCOREGRAPHOFF, OPTION_SCOREGRAPHON,OPTION_DIFFICULTY0,OPTION_DIFFICULTY1
		,OPTION_DIFFICULTY2,OPTION_DIFFICULTY3,OPTION_DIFFICULTY4,OPTION_DIFFICULTY5,
		OPTION_NO_BPMCHANGE,OPTION_BPMCHANGE,
		OPTION_COURSE_STAGE1,OPTION_COURSE_STAGE2,OPTION_COURSE_STAGE3,OPTION_COURSE_STAGE4,OPTION_COURSE_STAGE_FINAL,
		OPTION_MODE_COURSE,OPTION_MODE_NONSTOP,OPTION_MODE_EXPERT,OPTION_MODE_GRADE};

	public PlaySkin(Resolution src, Resolution dst) {
		super(src, dst, fixop);
	}

}
