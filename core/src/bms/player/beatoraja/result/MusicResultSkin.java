package bms.player.beatoraja.result;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.skin.*;

/**
 * リサルトスキン
 */
public class MusicResultSkin extends Skin {

	private int ranktime;

	public MusicResultSkin(Resolution src, Resolution dst) {
		super(src, dst);
	}

	public int getRankTime() {
		return ranktime;
	}

	public void setRankTime(int ranktime) {
		this.ranktime = ranktime;
	}

}
