package bms.player.beatoraja;

import lombok.Data;

/**
 * Created by exch on 2017/04/05.
 */

@Data
public class ScoreDataProperty {

    private IRScoreData score;
    private IRScoreData rival;

    private int nowScore;
    private int nowExScore;
    private int bestScore;
    private float bestScoreRate;
    private int nowBestScore;
    private float nowBestScoreRate;

    private float nowRate;
    private int nowRateInt;
    private int nowRateAfterDot;
    private float rate;
    private int rateInt;
    private int rateAfterDot;
    private int bestRateInt;
    private int bestRateAfterDot;

    private int rivalScore;
    private float rivalScoreRate;
    private int nowRivalScore;
    private float nowRivalScoreRate;
    private int rivalRateInt;
    private int rivalRateAfterDot;
    private boolean[] rank = new boolean[27];
    private int nextRank;
    private boolean[] nowRank = new boolean[27];
    private boolean[] bestRank = new boolean[27];

    public void update(IRScoreData score) {
        this.update(score, score != null ? score.getNotes() : 0);
    }

    public void update(IRScoreData score, IRScoreData rival) {
        update(score);
        this.rival = rival;
        final int exscore = rival != null ? rival.getExscore() : 0;
        final int totalnotes = rival != null ? rival.getNotes() : 0;

        rivalScore = exscore;
        rivalScoreRate = totalnotes == 0 ? 1.0f : ((float)exscore) / (totalnotes * 2);

    }

    public void update(IRScoreData score, int notes) {
        this.score = score;
        final int exscore = score != null ? score.getExscore() : 0;
        final int totalnotes = score != null ? score.getNotes() : 0;
        if(score != null) {
            switch (score.getPlaymode()) {
                case BEAT_5K:
                case BEAT_10K:
                    nowScore = (int)((long)100000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 50000 * score.getJudgeCount(2))
                            / score.getNotes();
                    break;
                case BEAT_7K:
                case BEAT_14K:
                    nowScore = (int)((long)150000 * score.getJudgeCount(0) + 100000 * score.getJudgeCount(1) + 20000 * score.getJudgeCount(2))
                            / score.getNotes() + (int)((long)50000 * score.getMaxCombo() / score.getNotes());
                    break;
                case POPN_5K:
                case POPN_9K:
                    nowScore = (int)((long)100000 * score.getJudgeCount(0) + 70000 * score.getJudgeCount(1) + 40000 * score.getJudgeCount(2))
                            / score.getNotes();
                    break;
                default:
                    nowScore = (int)((long)1000000 * score.getJudgeCount(0) + 700000 * score.getJudgeCount(1) + 400000 * score.getJudgeCount(2))
                            / score.getNotes();
                    break;
            }
        } else {
            nowScore = 0;
        }
        nowExScore = exscore;
        rate = totalnotes == 0 ? 1.0f : ((float)exscore) / (totalnotes * 2);
        rateInt = (int)(rate * 100);
        rateAfterDot = ((int)(rate * 10000)) % 100;
        nowRate = notes == 0 ? 1.0f : ((float)exscore) / (notes * 2);
        nowRateInt = (int)(nowRate * 100);
        nowRateAfterDot = ((int)(nowRate * 10000)) % 100;
        nextRank = Integer.MIN_VALUE;
        for(int i = 0;i < rank.length;i++) {
            rank[i] = totalnotes != 0 && rate >= 1f * i / rank.length;
            if(i % 3 == 0 && !rank[i] && nextRank == Integer.MIN_VALUE) {
                nextRank = Math.round((i * (notes * 2) / rank.length) - rate * (notes * 2));
            }
        }
        if(nextRank == Integer.MIN_VALUE) {
            nextRank = Math.round((notes * 2) - rate * (notes * 2));
        }
        for(int i = 0; i < nowRank.length; i++) {
            nowRank[i] = totalnotes != 0 && nowRate >= 1f * i / nowRank.length;
        }

        nowBestScore = totalnotes == 0 ? 0 : bestScore * notes / totalnotes;
        nowBestScoreRate = totalnotes == 0 ? 0 : (float) (bestScore) * notes / (totalnotes * totalnotes * 2);
        nowRivalScore = totalnotes == 0 ? 0 : rivalScore * notes / totalnotes;
        nowRivalScoreRate = totalnotes == 0 ? 0 : (float) (rivalScore) * notes / (totalnotes * totalnotes * 2);
    }

    public void setTargetScore(int bestscore, int rivalscore, int totalnotes) {
        this.bestScore = bestscore;
        this.rivalScore = rivalscore;
        bestScoreRate = ((float)bestscore)  / (totalnotes * 2);
        bestRateInt = (int)(bestScoreRate * 100);
        bestRateAfterDot = ((int)(bestScoreRate * 10000)) % 100;
        rivalScoreRate = ((float)rivalscore)  / (totalnotes * 2);
        for(int i = 0; i < bestRank.length; i++) {
            bestRank[i] = bestScoreRate >= 1f * i / bestRank.length;
        }
        rivalRateInt = (int)(rivalScoreRate * 100);
        rivalRateAfterDot = ((int)(rivalScoreRate * 10000)) % 100;
    }

    public boolean qualifyRank(int index) {
        return rank[index];
    }

    public boolean qualifyNowRank(int index) {
        return nowRank[index];
    }

    public boolean qualifyBestRank(int index) {
        return bestRank[index];
    }

}
