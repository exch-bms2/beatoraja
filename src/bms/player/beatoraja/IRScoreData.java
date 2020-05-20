package bms.player.beatoraja;

import bms.model.Mode;

/**
 * スコアデータ
 * LR2のスコアデータを元に拡張している
 *
 * @author ununique
 */
public class IRScoreData extends ScoreData {

	// TODO リファクタリング後削除
	
	public IRScoreData() {
		this(Mode.BEAT_7K);
	}

	public IRScoreData(Mode playmode) {
		super(playmode);
	}
}
