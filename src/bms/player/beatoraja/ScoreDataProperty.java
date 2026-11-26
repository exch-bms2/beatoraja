package bms.player.beatoraja;

/**
 * スコアデータから各数値を算出するためのクラス
 * 
 * @author exch
 */
public class ScoreDataProperty {

    private ScoreData score;
    private ScoreData rival;

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
    
    private int totalnotes;
    private int maximumScore;

    public void update(ScoreData score) {
        this.update(score, score != null ? score.getNotes() : 0);
    }

    public void update(ScoreData score, ScoreData rival) {
        update(score);
        this.rival = rival;
        final int exscore = rival != null ? rival.getRuleScore() : 0;

        rivalscore = exscore;
        rivalscorerate = rival == null || rival.getRuleMaxScore() == 0 ? 0 : (float) exscore / rival.getRuleMaxScore();
        rivalrateInt = (int)(rivalscorerate * 100);
        rivalrateAfterDot = ((int)(rivalscorerate * 10000)) % 100;
    }

    public void update(ScoreData score, int notes) {
        this.score = score;
        final int exscore = score != null ? score.getRuleScore() : 0;
        final int totalnotes = score != null ? score.getNotes() : 0;
        final int maximum = score != null ? score.getRuleMaxScore() : 0;
        final double progressMaximum = totalnotes == 0 ? 0 : (double) maximum * notes / totalnotes;
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
        if (score != null && score.getNativeScore() >= 0) nowpoint = exscore;
        rate = maximum == 0 ? 1.0f : (float) exscore / maximum;
        rateInt = (int)(rate * 100);
        rateAfterDot = ((int)(rate * 10000)) % 100;
        nowrate = progressMaximum == 0 ? 1.0f : (float) (exscore / progressMaximum);
        nowrateInt = (int)(nowrate * 100);
        nowrateAfterDot = ((int)(nowrate * 10000)) % 100;
        nextrank = Integer.MIN_VALUE;
        for(int i = 0;i < rank.length;i++) {
            rank[i] = totalnotes != 0 && rate >= 1f * i / rank.length;
            if(i % 3 == 0 && !rank[i] && nextrank == Integer.MIN_VALUE) {
                nextrank = (int)Math.ceil((i / (double) rank.length - rate) * progressMaximum);
            }
        }
        if(nextrank == Integer.MIN_VALUE) {
            nextrank = (int) progressMaximum - exscore;
        }
        for(int i = 0;i < nowrank.length;i++) {
            nowrank[i] = totalnotes != 0 && nowrate >= 1f * i / nowrank.length;
        }

        if (useBestGhost) {
            for (int i=previousNotes; i<notes; i++) {
                nowbestscore += getExScore(bestGhost[i]);
            }
            nowbestscorerate = maximum == 0 ? 0 : (float) nowbestscore / maximum;
        } else {
            nowbestscore = totalnotes == 0 ? 0 : (int) ((long) bestscore * notes / totalnotes);
            nowbestscorerate = maximum == 0 ? 0 : (float) nowbestscore / maximum;
        }
        if (useRivalGhost) {
            for (int i=previousNotes; i<notes; i++) {
                nowrivalscore += getExScore(rivalGhost[i]);
            }
            nowrivalscorerate = maximum == 0 ? 0 : (float) nowrivalscore / maximum;
        } else {
            nowrivalscore = totalnotes == 0 ? 0 : (int) ((long) rivalscore * notes / totalnotes);
            nowrivalscorerate = maximum == 0 ? 0 : (float) nowrivalscore / maximum;
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
    
    public void updateTargetScore(int rivalscore) {
    	this.rivalscore = rivalscore;
        rivalscorerate = maximumScore == 0 ? 0 : (float) rivalscore / maximumScore;
        rivalrateInt = (int)(rivalscorerate * 100);
        rivalrateAfterDot = ((int)(rivalscorerate * 10000)) % 100;
    }

    public void setTargetScore(int bestscore, int rivalscore, int totalnotes) {
        setTargetScore(bestscore, null, rivalscore, null, totalnotes);
    }

    public void setTargetScore(int bestscore, int[] bestGhost, int rivalscore, int[] rivalGhost, int totalnotes) {
        setTargetScore(bestscore, bestGhost, rivalscore, rivalGhost, totalnotes, totalnotes * 2);
    }

    public void setTargetScore(int bestscore, int[] bestGhost, int rivalscore, int[] rivalGhost, int totalnotes, int maximumScore) {
        this.bestscore = bestscore;
        this.bestGhost = bestGhost;
        this.rivalscore = rivalscore;
        this.rivalGhost = rivalGhost;
        this.totalnotes = totalnotes;
        this.maximumScore = maximumScore;
        bestscorerate = maximumScore == 0 ? 0 : (float) bestscore / maximumScore;
        bestrateInt = (int)(bestscorerate * 100);
        bestrateAfterDot = ((int)(bestscorerate * 10000)) % 100;
        rivalscorerate = maximumScore == 0 ? 0 : (float) rivalscore / maximumScore;
        for(int i = 0;i < bestrank.length;i++) {
            bestrank[i] = bestscorerate >= 1f * i / bestrank.length;
        }
        rivalrateInt = (int)(rivalscorerate * 100);
        rivalrateAfterDot = ((int)(rivalscorerate * 10000)) % 100;

        // ゴーストとノーツ数が異なる場合（ランダム分岐でノーツ数が変わった場合）はゴーストを再生しない
        useBestGhost = bestGhost != null && bestGhost.length == totalnotes && maximumScore == totalnotes * 2;
        useRivalGhost = rivalGhost != null && rivalGhost.length == totalnotes && maximumScore == totalnotes * 2;
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

    public ScoreData getScoreData() {
        return score;
    }

    public ScoreData getRivalScoreData() {
        return rival;
    }
}
