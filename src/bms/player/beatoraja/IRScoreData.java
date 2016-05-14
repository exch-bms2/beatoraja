package bms.player.beatoraja;

/**
 * IRスコアデータ
 * LR2のスコアデータを元に拡張している
 *
 * @author ununique
 */
public class IRScoreData {
    private static final long serialVersionUID = 1L;

    private int irid;
    private String logdate;
    /**
     * BMSのハッシュ値
     */
    private String hash = "";
    /**
     * BMSのタイトル
     */
    private String title = "";
    /**
     * クリア状況(0-5)
     */
    private int clear = 0;

    private int exclear;

    private int rank;

    private int sync;

    private int notes = 0;
    /**
     * コンボ数
     */
    private int combo = 0;
    /**
     * PGREAT数
     */
    private int fpg = 0;
    private int spg = 0;
    /**
     * GREAT数
     */
    private int fgr = 0;
    private int sgr = 0;
    /**
     * GOOD数
     */
    private int fgd = 0;
    private int sgd = 0;
    /**
     * BAD数
     */
    private int fbd = 0;
    private int sbd = 0;
    /**
     * PORR数
     */
    private int fpr = 0;
    private int spr = 0;
    /**
     * MISS数。空POORに相当
     */
    private int fms = 0;
    private int sms = 0;
    /**
     * 最小ミスカウント数
     */
    private int minbp = Integer.MAX_VALUE;
    /**
     * 譜面、ゲージオプション
     */
    private int option = 0;
    /**
     * IRランキングの最終更新日時。プレイヤー自身の最終更新日時ではないことに注意。
     */
    private long lastupdate = 0;

    private int playcount;

    private int clearcount;

    /**
     * 過去のベストクリア状況
     */
    private int bestclear = 0;
    /**
     * 過去のベストスコア
     */
    private int bestexscore = 0;
    /**
     * 過去のベストコンボ数
     */
    private int bestcombo = 0;
    /**
     * 過去のベスト最小ミスカウント数
     */
    private int bestminbp = Integer.MAX_VALUE;

    public IRScoreData() {
    }

    public IRScoreData(String hash, String title, int clear, int notes, int combo, int pg,
                       int gr, int gd, int bd, int pr, int minbp, int option, int lastudate) {
        setHash(hash);
        setTitle(title);
        setClear(clear);
        setNotes(notes);
        setCombo(combo);
        setPg(pg);
        setGr(gr);
        setGd(gd);
        setBd(bd);
        setPr(pr);
        setMinbp(minbp);
        setOption(option);
        setLastupdate(lastudate);
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getClear() {
        return clear;
    }

    public void setClear(int clear) {
        this.clear = clear;
    }

    public int getNotes() {
        return notes;
    }

    public void setNotes(int notes) {
        this.notes = notes;
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        this.combo = combo;
    }

    public int getPg() {
        return fpg + spg;
    }

    public void setPg(int pg) {
        this.fpg = pg;
        spg = 0;
    }

    public int getGr() {
        return fgr + sgr;
    }

    public void setGr(int gr) {
        this.fgr = gr;
        sgr = 0;
    }

    public int getGd() {
        return fgd + sgd;
    }

    public void setGd(int gd) {
        this.fgd = gd;
        sgd = 0;
    }

    public int getBd() {
        return fbd + sbd;
    }

    public void setBd(int bd) {
        this.fbd = bd;
        sbd = 0;
    }

    public int getPr() {
        return fpr + spr + fms + sms;
    }

    public void setPr(int pr) {
        this.fpr = pr;
        spr = 0;
        fms = 0;
        sms = 0;
    }

    public int getMinbp() {
        return minbp;
    }

    public void setMinbp(int minbp) {
        this.minbp = minbp;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public long getLastupdate() {
        return lastupdate;
    }

    public void setLastupdate(long lastupdate) {
        this.lastupdate = lastupdate;
    }

    public int getExscore() {
        return getPg() * 2 + getGr();
    }

    public int getBestclear() {
        return bestclear;
    }

    public void setBestclear(int bestclear) {
        this.bestclear = bestclear;
    }

    public int getBestexscore() {
        return bestexscore;
    }

    public void setBestexscore(int bestnotes) {
        this.bestexscore = bestnotes;
    }

    public int getBestcombo() {
        return bestcombo;
    }

    public void setBestcombo(int bestcombo) {
        this.bestcombo = bestcombo;
    }

    public int getBestminbp() {
        return bestminbp;
    }

    public void setBestminbp(int bestminbp) {
        this.bestminbp = bestminbp;
    }

    public int getCleardetail() {
        return clear > bestclear ? clear : -clear;
    }

    public String getMinbpdetail() {
        if(bestminbp == Integer.MAX_VALUE || bestminbp <= minbp) {
            return String.valueOf(minbp);
        }
        return minbp + " (-" + (bestminbp - minbp) + ")";
    }

    public String getCombodetail() {
        if(bestcombo == 0 || bestcombo >= combo) {
            return String.valueOf(combo);
        }
        return combo + " (+" + (combo - bestcombo) + ")";
    }

    public String getExscoredetail() {
        if(bestexscore == 0 || bestexscore >= this.getExscore()) {
            return String.valueOf(this.getExscore());
        }
        return this.getExscore() + " (+" + (this.getExscore() - bestexscore) + ")";
    }

    public int getIrid() {
        return irid;
    }

    public void setIrid(int irid) {
        this.irid = irid;
    }

    public String getLogdate() {
        return logdate;
    }

    public void setLogdate(String logdate) {
        this.logdate = logdate;
    }

    public int getExclear() {
        return exclear;
    }

    public void setExclear(int exclear) {
        this.exclear = exclear;
    }

    public int getPlaycount() {
        return playcount;
    }

    public void setPlaycount(int playcount) {
        this.playcount = playcount;
    }

    public int getClearcount() {
        return clearcount;
    }

    public void setClearcount(int clearcount) {
        this.clearcount = clearcount;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getSync() {
        return sync;
    }

    public void setSync(int sync) {
        this.sync = sync;
    }

    public int getFpg() {
        return fpg;
    }

    public void setFpg(int fpg) {
        this.fpg = fpg;
    }

    public int getSpg() {
        return spg;
    }

    public void setSpg(int spg) {
        this.spg = spg;
    }

    public int getFgr() {
        return fgr;
    }

    public void setFgr(int fgr) {
        this.fgr = fgr;
    }

    public int getSgr() {
        return sgr;
    }

    public void setSgr(int sgr) {
        this.sgr = sgr;
    }

    public int getFgd() {
        return fgd;
    }

    public void setFgd(int fgd) {
        this.fgd = fgd;
    }

    public int getSgd() {
        return sgd;
    }

    public void setSgd(int sgd) {
        this.sgd = sgd;
    }

    public int getFbd() {
        return fbd;
    }

    public void setFbd(int fbd) {
        this.fbd = fbd;
    }

    public int getSbd() {
        return sbd;
    }

    public void setSbd(int sbd) {
        this.sbd = sbd;
    }

    public int getFpr() {
        return fpr;
    }

    public void setFpr(int fpr) {
        this.fpr = fpr;
    }

    public int getSpr() {
        return spr;
    }

    public void setSpr(int spr) {
        this.spr = spr;
    }

    public int getFms() {
        return fms;
    }

    public void setFms(int fms) {
        this.fms = fms;
    }

    public int getSms() {
        return sms;
    }

    public void setSms(int sms) {
        this.sms = sms;
    }
}
