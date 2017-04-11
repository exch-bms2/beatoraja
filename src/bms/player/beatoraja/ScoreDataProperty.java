package bms.player.beatoraja;

/**
 * Created by exch on 2017/04/05.
 */
public class ScoreDataProperty {

    private int nowpoint;
    private int nowscore;
    private int bestscore;
    private float bestscorerate;
    private int nowbestscore;
    private float nowbestscorerate;
    private int rivalscore;
    private float rivalscorerate;
    private int nowrivalscore;
    private float nowrivalscorerate;

    private float nowrate;
    private int nowrateInt;
    private int nowrateAfterDot;
    private float rate;
    private int rateInt;
    private int rateAfterDot;
    private boolean[] rank = new boolean[27];
    private boolean[] nowrank = new boolean[27];
    private boolean[] bestrank = new boolean[27];

    public void update(IRScoreData score) {
        if(score != null) {
            this.update(score.getExscore(), score.getNotes(), score.getNotes());
        } else {
            this.update(0, 0, 0);
        }
    }

    public void update(int exscore, int totalnotes, int notes) {
        nowscore = exscore;
        rate = totalnotes == 0 ? 1.0f : ((float)exscore) / (totalnotes * 2);
        rateInt = (int)(rate * 100);
        rateAfterDot = ((int)(rate * 10000)) % 100;
        nowrate = notes == 0 ? 1.0f : ((float)exscore) / (notes * 2);
        nowrateInt = (int)(nowrate * 100);
        nowrateAfterDot = ((int)(nowrate * 10000)) % 100;
        for(int i = 0;i < rank.length;i++) {
            rank[i] = totalnotes != 0 && rate >= 1f * i / rank.length;
        }
        for(int i = 0;i < nowrank.length;i++) {
            nowrank[i] = totalnotes != 0 && nowrate >= 1f * i / nowrank.length;
        }

        nowbestscore = totalnotes == 0 ? 0 : bestscore * notes / totalnotes;
        nowbestscorerate= totalnotes == 0 ? 0 : (float) (bestscore) * notes / (totalnotes * totalnotes * 2);
        nowrivalscore = totalnotes == 0 ? 0 : rivalscore * notes / totalnotes;
        nowrivalscorerate= totalnotes == 0 ? 0 : (float) (rivalscore) * notes / (totalnotes * totalnotes * 2);
    }

    public void setTargetScore(int bestscore, int rivalscore, int totalnotes) {
        this.bestscore = bestscore;
        this.rivalscore = rivalscore;
        bestscorerate= ((float)bestscore)  / (totalnotes * 2);
        rivalscorerate= ((float)rivalscore)  / (totalnotes * 2);
        for(int i = 0;i < bestrank.length;i++) {
            bestrank[i] = bestscorerate >= 1f * i / bestrank.length;
        }
    }

    public int getNowEXScore() {
        return nowscore;
    }

    public int getNowBestScore() {
        return nowbestscore;
    }

    public int getNowRivalScore() {
        return nowrivalscore;
    }

    public boolean qualifyRank(int index) {
        return rank[index];
    }

    public boolean qualifyNowRank(int index) {
        return nowrank[index];
    }

    public boolean qualifyBestRank(int index) {
        return bestrank[index];
    }
    public float getNowRate() {
        return nowrate;
    }

    public int getNowRateInt() {
        return nowrateInt;
    }

    public int getNowRateAfterDot() {
        return nowrateAfterDot;
    }

    public float getRate() {
        return rate;
    }

    public int getRateInt() {
        return rateInt;
    }

    public int getRateAfterDot() {
        return rateAfterDot;
    }

    public int getBestScore() {
        return bestscore;
    }

    public float getBestScoreRate() {
        return bestscorerate;
    }

    public float getNowBestScoreRate() {
        return nowbestscorerate;
    }

    public int getRivalScore() {
        return rivalscore;
    }

    public float getRivalScoreRate() {
        return rivalscorerate;
    }

    public float getNowRivalScoreRate() {
        return nowrivalscorerate;
    }
}
