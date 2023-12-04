package bms.player.beatoraja.result;

import bms.player.beatoraja.skin.*;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin {

	private int ranktime;

	public MusicResultSkin(SkinHeader header) {
		super(header);
	}

	public int getRankTime() {
		return ranktime;
	}

	public void setRankTime(int ranktime) {
		this.ranktime = ranktime;
	}

}
