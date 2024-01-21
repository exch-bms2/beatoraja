package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.ScoreData;

/**
 * 選曲用バーの抽象クラス
 * 
 * @author exch
 */
public abstract class Bar {

	/**
	 * プレイヤースコア
	 */
    private ScoreData score;
	/**
	 * ライバルスコア
	 */
    private ScoreData rscore;

    /**
     * バーのタイトルを取得する
     * 
     * @return バーのタイトル
     */
    public abstract String getTitle();

    public final ScoreData getScore() {
        return score;
    }

    public final void setScore(ScoreData score) {
        this.score = score;
    }

    public final ScoreData getRivalScore() {
        return rscore;
    }

    public final void setRivalScore(ScoreData score) {
        this.rscore = score;
    }

    public abstract int getLamp(boolean isPlayer);
}

