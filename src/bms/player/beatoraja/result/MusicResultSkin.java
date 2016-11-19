package bms.player.beatoraja.result;

import bms.player.beatoraja.skin.*;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin {

	private int ranktime;

	public MusicResultSkin(float srcw, float srch, float dstw, float dsth) {
		super(srcw, srch, dstw, dsth);
	}

	public int getRankTime() {
		return ranktime;
	}

	public void setRankTime(int ranktime) {
		this.ranktime = ranktime;
	}

}
