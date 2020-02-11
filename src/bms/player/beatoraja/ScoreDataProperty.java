package bms.player.beatoraja;

/**
 * Created by exch on 2017/04/05.
 */
public class ScoreDataProperty {

    private IRScoreData score;
    private IRScoreData rival;

    private int nowpoint;
    private int nowscore;
    private int bestscore;
    private float bestscorerate;
    private int nowbestscore;
    private float nowbestscorerate;

    private float nowrate;
    private int nowrateInt;
    private int nowrateAfterDot;
    private float rate;
    private int rateInt;
    private int rateAfterDot;
    private int bestrateInt;
    private int bestrateAfterDot;

    private int rivalscore;
    private float rivalscorerate;
    private int nowrivalscore;
    private float nowrivalscorerate;
    private int rivalrateInt;
    private int rivalrateAfterDot;
    private boolean[] rank = new boolean[27];
    private int nextrank;
    private boolean[] nowrank = new boolean[27];
    private boolean[] bestrank = new boolean[27];

    private int previousNotes = 0;
    private int[] bestGhost;
    private int[] rivalGhost;
    private boolean useBestGhost = false;
    private boolean useRivalGhost = false;

    public void update(IRScoreData score) {
        this.update(score, score != null ? score.getNotes() : 0);
    }

    public void update(IRScoreData score, IRScoreData rival) {
        update(score);
        this.rival = rival;
        final int exscore = rival != null ? rival.getExscore() : 0;
        final int totalnotes = rival != null ? rival.getNotes() : 0;

        rivalscore = exscore;
        rivalscorerate = totalnotes == 0 ? 1.0f : ((float)exscore) / (totalnotes * 2);

    }

    public void update(IRScoreData score, int notes) {
        this.score = score;
        final int exscore = score != null ? score.getExscore() : 0;
        final int totalnotes = score != null ? score.getNotes() : 0;
        if(totalnotes > 0) {
            switch (score.getPlaymode()) {
                case BEAT_5K:
                case BEAT_10K:
                    nowpoint = (int)(((long)100000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 50000 * score.getJudgeCount(2))
                            / totalnotes);
                    break;
                case BEAT_7K:
                case BEAT_14K:
                    nowpoint = (int)(((long)150000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 20000 * score.getJudgeCount(2))
                            / totalnotes) + (int)((long)50000 * score.getCombo() / totalnotes);
                    break;
                case POPN_5K:
                case POPN_9K:
                    nowpoint = (int)(((long)100000 * score.getJudgeCount(0) + 70000 * score.getJudgeCount(1) + 40000 * score.getJudgeCount(2))
                            / totalnotes);
                    break;
                default:
                    nowpoint = (int)(((long)1000000 * score.getJudgeCount(0) + 700000 * score.getJudgeCount(1) + 400000 * score.getJudgeCount(2))
                            / totalnotes);
                    break;
            }
        } else {
            nowpoint = 0;
        }
        nowscore = exscore;
        rate = totalnotes == 0 ? 1.0f : ((float)exscore) / (totalnotes * 2);
        rateInt = (int)(rate * 100);
        rateAfterDot = ((int)(rate * 10000)) % 100;
        nowrate = notes == 0 ? 1.0f : ((float)exscore) / (notes * 2);
        nowrateInt = (int)(nowrate * 100);
        nowrateAfterDot = ((int)(nowrate * 10000)) % 100;
        nextrank = Integer.MIN_VALUE;
        for(int i = 0;i < rank.length;i++) {
            rank[i] = totalnotes != 0 && rate >= 1f * i / rank.length;
            if(i % 3 == 0 && !rank[i] && nextrank == Integer.MIN_VALUE) {
                nextrank = Math.round((i * (notes * 2) / rank.length) - rate * (notes * 2));
            }
        }
        if(nextrank == Integer.MIN_VALUE) {
            nextrank = Math.round((notes * 2) - rate * (notes * 2));
        }
        for(int i = 0;i < nowrank.length;i++) {
            nowrank[i] = totalnotes != 0 && nowrate >= 1f * i / nowrank.length;
        }

        if (useBestGhost) {
            for (int i=previousNotes; i<notes; i++) {
                nowbestscore += getExScore(bestGhost[i]);
            }
            nowbestscorerate = totalnotes == 0 ? 0 : (float) nowbestscore / (totalnotes * 2);
        } else {
            nowbestscore = totalnotes == 0 ? 0 : bestscore * notes / totalnotes;
            nowbestscorerate= totalnotes == 0 ? 0 : (float) (bestscore) * notes / (totalnotes * totalnotes * 2);
        }
        if (useRivalGhost) {
            for (int i=previousNotes; i<notes; i++) {
                nowrivalscore += getExScore(rivalGhost[i]);
            }
            nowrivalscorerate = totalnotes == 0 ? 0 : (float) nowrivalscore / (totalnotes * 2);
        } else {
            nowrivalscore = totalnotes == 0 ? 0 : rivalscore * notes / totalnotes;
            nowrivalscorerate= totalnotes == 0 ? 0 : (float) (rivalscore) * notes / (totalnotes * totalnotes * 2);
        }
        previousNotes = notes;
    }

    private int getExScore(int judge) {
        if (judge == 0) {
            return 2;
        } else if (judge == 1) {
            return 1;
        }
        return 0;
    }

    public void setTargetScore(int bestscore, int rivalscore, int totalnotes) {
        setTargetScore(bestscore, null, rivalscore, null, totalnotes);
    }

    public void setTargetScore(int bestscore, int[] bestGhost, int rivalscore, int[] rivalGhost, int totalnotes) {
        this.bestscore = bestscore;
        this.bestGhost = bestGhost;
        this.rivalscore = rivalscore;
        this.rivalGhost = rivalGhost;
        bestscorerate= ((float)bestscore)  / (totalnotes * 2);
        bestrateInt = (int)(bestscorerate * 100);
        bestrateAfterDot = ((int)(bestscorerate * 10000)) % 100;
        rivalscorerate= ((float)rivalscore)  / (totalnotes * 2);
        for(int i = 0;i < bestrank.length;i++) {
            bestrank[i] = bestscorerate >= 1f * i / bestrank.length;
        }
        rivalrateInt = (int)(rivalscorerate * 100);
        rivalrateAfterDot = ((int)(rivalscorerate * 10000)) % 100;

        // ゴーストとノーツ数が異なる場合（ランダム分岐でノーツ数が変わった場合）はゴーストを再生しない
        useBestGhost = bestGhost != null && bestGhost.length == totalnotes;
        useRivalGhost = rivalGhost != null && rivalGhost.length == totalnotes;
    }

    public int getNowScore() {
        return nowpoint;
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

    public int getRivalRateInt() {
        return rivalrateInt;
    }

    public int getRivalRateAfterDot() {
        return rivalrateAfterDot;
    }

    public float getRate() {
        return rate;
    }

    public int getNextRank() {
        return nextrank;
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

    public int getBestRateInt() {
        return bestrateInt;
    }

    public int getBestRateAfterDot() {
        return bestrateAfterDot;
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

    public IRScoreData getScoreData() {
        return score;
    }

    public IRScoreData getRivalScoreData() {
        return rival;
    }
}
