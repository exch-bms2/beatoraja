package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.IRScoreData;

/**
 * Created by exch on 2017/09/03.
 */
public abstract class Bar {

    private IRScoreData score;
    private IRScoreData rscore;

    public abstract String getTitle();
    public abstract String getArtist();

    public IRScoreData getScore() {
        return score;
    }

    public void setScore(IRScoreData score) {
        this.score = score;
    }

    public IRScoreData getRivalScore() {
        return rscore;
    }

    public void setRivalScore(IRScoreData score) {
        this.rscore = score;
    }

    public abstract int getLamp(boolean isPlayer);
}

