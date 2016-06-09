package bms.player.beatoraja.play;

import java.util.Arrays;

import bms.model.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

/**
 * ノーツ判定管理用クラス
 *
 * @author exch
 */
public class JudgeManager {

    // TODO bug:稀にノーツカウント漏れがある(BSS絡み？)

    private BMSPlayer main;
    private BMSModel model;

    private final JudgeAlgorithm[] judgeAlgorithms = {new JudgeAlgorithmLR2(), new JudgeAlgorithm2DX(), new JudgeAlgorithmLowestNote()};
    /**
     * 判定アルゴリズム:LR2風
     */
    public static final int JUDGE_ALGORITHM_LR2 = 0;
    /**
     * 判定アルゴリズム:本家風
     */
    public static final int JUDGE_ALGORITHM_IIDX = 1;
    /**
     * 判定アルゴリズム:最下ノーツ優先判定
     */
    public static final int JUDGE_ALGORITHM_LOWEST_NOTE = 2;
    /**
     * 現在の判定カウント内訳
     */
    private int[][] count = new int[6][2];

    /**
     * 現在のコンボ数
     */
    private int combo;
    /**
     * 最大コンボ数
     */
    private int maxcombo;

    private int coursecombo;

    private int coursemaxcombo;
    /**
     * 現在表示中の判定
     */
    private int[] judgenow;
    /**
     * 判定の最終更新時間
     */
    private int[] judgenowt;

    private int[] judgecombo;
    /**
     * 判定差時間(ms , +は早押しで-は遅押し)
     */
    private int judgefast;
    /**
     * ボムの表示開始時間
     */
    private long[] bomb = new long[8];
    /**
     * 処理中のLN
     */
    private LongNote[] processing = new LongNote[8];
    /**
     * 通過中のHCN
     */
    private LongNote[] passing = new LongNote[8];
    /**
     * HCN増加判定
     */
    private boolean[] inclease = new boolean[8];
    private boolean[] next_inclease = new boolean[8];
    private int[] passingcount;

    private int[] keyassign;
    private int[] noteassign;

    private int[] sckeyassign;
    private int[] sckey;
    /**
     * ミスレイヤー表示開始時間
     */
    private int misslayer;
    /**
     * 各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
     */
//    private static final int[] judgetable = {20, 60, 165, 315, 0, 1000};
    private static final int[] judgetable = {20, 60, 160, 250, 0, 1000};
    /**
     * PMSの各判定の範囲(+-ms)。PGREAT, GREAT, GOOD, BAD, POOR, MISS空POORの順
     */
    private static final int[] pjudgetable = {25, 75, 175, 200, 0, 1000};
    /**
     * スクラッチレーンの判定拡大幅
     */
    private static final int sjudgerate = 110;
    /**
     * HCNの増減間隔(ms)
     */
    private static final int hcnduration = 100;
    /**
     * ノーツ判定テーブル
     */
    private int[] njudge;
    /**
     * スクラッチ判定テーブル
     */
    private int[] sjudge;
    
    private boolean pmsjudge = false;

    private int pos = 0;
    private int judgetype = 0;

    private int prevtime;

    private void prepareAttr(int judges) {
        bomb = new long[noteassign.length];
        processing = new LongNote[noteassign.length];
        passing = new LongNote[noteassign.length];
        passingcount = new int[noteassign.length];
        inclease = new boolean[noteassign.length];
        next_inclease = new boolean[noteassign.length];
        sckey = new int[sckeyassign.length];
        judgenow = new int[judges];
        judgenowt = new int[judges];
        judgecombo = new int[judges];
    }

    public JudgeManager(BMSPlayer main, BMSModel model) {
        this.main = main;
        this.model = model;
        switch (model.getUseKeys()) {
            case 5:
            case 7:
                keyassign = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 7};
                noteassign = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
                sckeyassign = new int[]{7};
                prepareAttr(1);
                pmsjudge = false;
                break;
            case 10:
            case 14:
                keyassign = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11, 12, 13, 14, 15, 15};
                noteassign = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16};
                sckeyassign = new int[]{7, 15};
                prepareAttr(2);
                pmsjudge = false;
                break;
            case 9:
                keyassign = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
                noteassign = new int[]{0, 1, 2, 3, 4, 10, 11, 12, 13};
                sckeyassign = new int[]{};
                prepareAttr(1);
                pmsjudge = true;
                break;
        }
        Arrays.fill(bomb, -1000);

        njudge = new int[6];
        sjudge = new int[6];
        for (int i = 0; i < judgetable.length; i++) {
            if (i < 4) {
                njudge[i] = (model.getUseKeys() == 9 ? pjudgetable[i] : judgetable[i]) * model.getJudgerank() / 100;
                sjudge[i] = njudge[i] * sjudgerate / 100;
            } else {
                sjudge[i] = njudge[i] = (model.getUseKeys() == 9 ? pjudgetable[i] : judgetable[i]);
                
            }
        }
    }

    public void update(TimeLine[] timelines, int time) {
        BMSPlayerInputProcessor input = main.getBMSPlayerInputProcessor();
        long[] keytime = input.getTime();
        boolean[] keystate = input.getKeystate();

        for (int i = pos; i < timelines.length && timelines[i].getTime() <= time; i++) {
            if (timelines[i].getTime() > prevtime) {
                for (int key = 0; key < keyassign.length; key++) {
                    final Note note = timelines[i].getNote(noteassign[keyassign[key]]);
                    if (note != null) {
                        if (note instanceof MineNote && keystate[key]) {
                            final MineNote mnote = (MineNote) note;
                            // 地雷ノート判定
                            main.getGauge().addValue(-mnote.getDamage());
                            System.out.println("Mine Damage : " + mnote.getWav());
                        }
                        if (model.getLntype() == BMSModel.LNTYPE_HELLCHARGENOTE && note instanceof LongNote) {
                            // HCN判定
                            final LongNote lnote = (LongNote) note;
                            if (lnote.getStart() == timelines[i]) {
                                passing[keyassign[key]] = lnote;
                            }
                            if (lnote.getEnd() == timelines[i]) {
                                passing[keyassign[key]] = null;
                                passingcount[keyassign[key]] = 0;
                            }
                        }
                    }
                }
            }
            if (pos < i && timelines[i].getTime() < prevtime - njudge[5]) {
                pos = i;
//				System.out.println("judge first position : " + timelines[i].getTime() + " time : " + time);
            }
        }
        // HCNゲージ増減判定
        Arrays.fill(next_inclease, false);
        for (int key = 0; key < keyassign.length; key++) {
            if (passing[keyassign[key]] != null) {
                if (keystate[key]) {
                    next_inclease[keyassign[key]] = true;
                }
            }
        }
        final boolean[] b = inclease;
        inclease = next_inclease;
        next_inclease = b;

        for (int key = 0; key < keyassign.length; key++) {
            if (passing[keyassign[key]] != null) {
                if (inclease[keyassign[key]]) {
                    passingcount[keyassign[key]] += (time - prevtime);
                    if (passingcount[keyassign[key]] > hcnduration) {
                        main.getGauge().update(1);
//                        System.out.println("HCN : Gauge increase");
                        passingcount[keyassign[key]] -= hcnduration;
                    }
                } else {
                    passingcount[keyassign[key]] -= (time - prevtime);
                    if (passingcount[keyassign[key]] < -hcnduration) {
                        main.getGauge().update(4, 0.2f);
//                        System.out.println("HCN : Gauge decrease");
                        passingcount[keyassign[key]] += hcnduration;
                    }
                }
            }
        }

        for (int key = 0; key < keyassign.length; key++) {
            if (keytime[key] != 0) {
                long ptime = keytime[key];
                int lane = keyassign[key];
                int sc = Arrays.binarySearch(sckeyassign, lane);
            	int[] judge = sc >= 0 ? sjudge : njudge;
                if (keystate[key]) {
                    // キーが押されたときの処理
                    if (processing[lane] != null) {
                        if (model.getLntype() != BMSModel.LNTYPE_LONGNOTE && sc >= 0 && key != sckey[sc]) {
                            for (int j = 0; j < judge.length; j++) {
                                if (j > 3) {
                                    j = 4;
                                }
                                if (j == 4
                                        || (ptime > processing[lane].getEnd().getTime() - judge[j] && ptime < processing[lane]
                                        .getEnd().getTime() + judge[j])) {
                                    if (j < 2) {
                                        bomb[lane] = ptime;
                                    }
                                    final int dtime = (int) (processing[lane].getEnd().getTime() - ptime);
                                    this.update(lane, j < 4 ? j : 4, time, dtime);
                                    processing[lane].setEndstate(j + 1);
                                    processing[lane].setEndtime(dtime);
                                    System.out.println("BSS終端判定 - Time : " + ptime + " Judge : " + j + " LN : "
                                            + processing[lane].hashCode());
                                    processing[lane] = null;
                                    sckey[sc] = 0;
                                    break;
                                }
                            }

                        } else {
                            // ここに来るのはマルチキーアサイン以外ありえないはず
                        }
                    } else {
                        // 対象ノーツの抽出
                        TimeLine tl = judgeAlgorithms[judgetype].getNote(pos,timelines, ptime, judge, noteassign[lane], pmsjudge);
                        int j = judgeAlgorithms[judgetype].getJudge();

                        if (tl != null) {
                            Note note = tl.getNote(noteassign[lane]);
                            // TODO この時点で空POOR処理を分岐させるべきか
                            if (note instanceof LongNote) {
                                // ロングノート処理
                                LongNote ln = (LongNote) note;
                                if (ln.getStart() == tl) {
                                    main.play(note.getWav(), note.getStarttime());
                                    if (j < 2) {
                                        bomb[lane] = ptime;
                                    }
                                    if (model.getLntype() == BMSModel.LNTYPE_LONGNOTE) {
                                        passingcount[lane] = (int) (tl.getTime() - ptime);
                                    } else {
                                        final int dtime = (int) (tl.getTime() - ptime);
                                        this.update(lane, j, time, dtime);
                                        if(ln.getState() == 0) {
                                            ln.setTime(dtime);
                                        }
                                        if (j < 4) {
                                        	ln.setState(j + 1);
                                        }
                                    }
                                    if (j < 4) {
                                        processing[lane] = ln;
                                        if (sc >= 0) {
                                            // BSS処理開始
                                            System.out.println("BSS開始判定 - Time : " + ptime + " Judge : " + j
                                                    + " KEY : " + key + " LN : " + note.hashCode());
                                            sckey[sc] = key;

                                        }
                                    }
                                }
                            } else {
                                main.play(note.getWav(), note.getStarttime());
                                // 通常ノート処理
                                if (j < 2) {
                                    bomb[lane] = ptime;
                                }
                                final int dtime = (int) (tl.getTime() - ptime);
                                this.update(lane, j, time, dtime);
                                if(note.getState() == 0) {
                                    note.setTime(dtime);
                                }
                                if (j < 4) {
                                	note.setState(j + 1);
                                }
                            }
                        } else {
                            Note n = null;
                            boolean sound = false;
                            for (TimeLine tl2 : timelines) {
                                if (tl2.getNote(lane) != null) {
                                    n = tl2.getNote(lane);
                                }
                                if (tl2.getHiddenNote(lane) != null) {
                                    n = tl2.getHiddenNote(lane);
                                }
                                if (n != null && tl2.getTime() >= ptime) {
                                    main.play(n.getWav(), n.getStarttime());
                                    sound = true;
                                    break;
                                }
                            }
                            if (!sound && n != null) {
                                main.play(n.getWav(), n.getStarttime());
                            }
                        }
                    }
                } else {
                    // キーが離されたときの処理
                    if (processing[lane] != null) {
                        for (int j = 0; j < judge.length; j++) {
                            if (j > 3) {
                                j = 4;
                            }
                            if (j == 4
                                    || (ptime > processing[lane].getEnd().getTime() - judge[j] && ptime < processing[lane]
                                    .getEnd().getTime() + judge[j])) {
                                int dtime = (int) (processing[lane].getEnd().getTime() - ptime);
                                if (model.getLntype() != BMSModel.LNTYPE_LONGNOTE) {
                                    if (sc >= 0) {
                                        if (j != 4 || key != sckey[sc]) {
                                            break;
                                        }
                                        System.out.println("BSS途中離し判定 - Time : " + ptime + " Judge : " + j + " LN : "
                                                + processing[lane]);
                                        sckey[sc] = 0;
                                    }
                                    if (j < 2) {
                                        bomb[lane] = ptime;
                                    }
                                    this.update(lane, j, time, dtime);
                                    processing[lane].setEndstate(j + 1);
                                    processing[lane].setEndtime(dtime);
                                    processing[lane] = null;
                                } else {
                                	if(Math.abs(passingcount[lane]) > Math.abs(dtime)) {
                                		dtime = passingcount[lane];
                                        for (;j < 4; j++) {
                                            if (Math.abs(passingcount[lane]) <= judge[j]) {
                                                break;
                                            }
                                        }
                                	}
                                    this.update(lane, j, time, dtime);
                                    processing[lane].setState(j + 1);
                                    processing[lane].setTime(dtime);
                                    processing[lane] = null;                                	
                                }
                                j = judge.length;
                                break;
                            }
                        }
                    }
                }
                keytime[key] = 0;
            }
            prevtime = time;
        }

        for (int lane = 0; lane < noteassign.length; lane++) {
            int sc = Arrays.binarySearch(sckeyassign, lane);
        	int[] judge = sc >= 0 ? sjudge : njudge;

            // LN終端判定
            if (model.getLntype() == BMSModel.LNTYPE_LONGNOTE && processing[lane] != null
                    && processing[lane].getEnd().getTime() < time) {
                int j = 0;
                for (; j < judge.length; j++) {
                    if (Math.abs(passingcount[lane]) <= judge[j]) {
                        break;
                    }
                }
                this.update(lane, j, time, passingcount[lane]);
                processing[lane].setState(j + 1);
                processing[lane].setTime(passingcount[lane]);
                processing[lane] = null;
            }
            // 見逃しPOOR判定
            for (int i = 0; i < timelines.length && timelines[i].getTime() < time - judge[3]; i++) {
                if (timelines[i].getTime() >= time - judge[3] - 500) {
                    Note note = timelines[i].getNote(noteassign[lane]);
                    if (note != null) {
                        int jud = timelines[i].getTime() - time;
                        if(note instanceof NormalNote && note.getState() == 0) {
                            this.update(lane, 4, time, jud);
                            note.setState(5);
                            note.setTime(jud);
                        }
                        if(note instanceof LongNote && ((LongNote) note).getStart() == timelines[i] && note.getState() == 0) {
                        	if(model.getLntype() != BMSModel.LNTYPE_LONGNOTE) {
                                // System.out.println("CN start poor");
                                this.update(lane, 4, time, jud);
                                note.setState(5);
                                note.setTime(jud);
                                this.update(lane, 4, time, jud);
                                ((LongNote) note).setEndstate(5);
                                ((LongNote) note).setEndtime(jud);                        		
                        	}
                            if(model.getLntype() == BMSModel.LNTYPE_LONGNOTE && processing[lane] != note) {
                                // System.out.println("LN start poor");
                                this.update(lane, 4, time, jud);
                                note.setState(5);
                                note.setTime(jud);
                            }
                        	
                        }
                        if(model.getLntype() != BMSModel.LNTYPE_LONGNOTE && note instanceof LongNote && ((LongNote) note).getEnd() == timelines[i] && ((LongNote) note).getEndstate() == 0) {
                            // System.out.println("CN end poor");
                            this.update(lane, 4, time, jud);
                            ((LongNote) note).setEndstate(5);
                            ((LongNote) note).setEndtime(jud);                        		
                            processing[lane] = null;
                            if (sc >= 0) {
                                sckey[sc] = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private void update(int lane, int j, int time, int fast) {
        judgenow[lane / (noteassign.length / judgenow.length)] = j + 1;
        judgenowt[lane / (noteassign.length / judgenow.length)] = time;
        count[j][fast >= 0 ? 0 : 1]++;
        judgefast = fast;
        if (j < 3) {
            combo++;
            maxcombo = maxcombo > combo ? maxcombo : combo;
            coursecombo++;
            coursemaxcombo = coursemaxcombo > coursecombo ? coursemaxcombo : coursecombo;
        } else if ((j >= 3 && j < 5) || (pmsjudge && j >= 3)) {
            combo = 0;
            coursecombo = 0;
        }
        judgecombo[lane / (noteassign.length / judgenow.length)] = coursecombo;
        main.update(lane, j, time, fast);
    }

    public int[] getJudgeNow() {
        return judgenow;
    }

    public int[] getJudgeTime() {
        return judgenowt;
    }

    public int[] getJudgeCombo() {
        return judgecombo;
    }

    public int getRecentJudgeTiming() {
        return judgefast;
    }

    public long[] getBomb() {
        return bomb;
    }

    public LongNote[] getProcessingLongNotes() {
        return processing;
    }

    public LongNote[] getPassingLongNotes() {
        return passing;
    }

    public boolean[] getHellChargeJudges() {
        return inclease;
    }

    public int getCombo() {
        return combo;
    }

    public int getCourseCombo() {
        return coursecombo;
    }

    public void setCourseCombo(int combo) {
        this.coursecombo = combo;
    }

    public int getMaxcombo() {
        return maxcombo;
    }

    public int getCourseMaxcombo() {
        return coursemaxcombo;
    }

    public void setCourseMaxcombo(int combo) {
        this.coursemaxcombo = combo;
    }

    public int getJudgeCount() {
        return count[0][0] + count[0][1] + count[1][0] + count[1][1] + count[2][0] + count[2][1] + count[3][0]
                + count[3][1] + count[4][0] + count[4][1] + count[5][0] + count[5][1];

    }

    public int[] getJudgeTimeRegion() {
        return njudge;
    }

    public int[] getScratchJudgeTimeRegion() {
        return sjudge;
    }

    /**
     * 指定の判定のカウント数雨返す
     * @param judge 0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
     * @return 判定のカウント数
     */
    public int getJudgeCount(int judge) {
        return count[judge][0] + count[judge][1];
    }

    /**
     * 指定の判定のカウント数雨返す
     * @param judge 0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
     * @param fast true:FAST, flase:SLOW
     * @return 判定のカウント数
     */
    public int getJudgeCount(int judge, boolean fast) {
        return fast ? count[judge][0] : count[judge][1];
    }

    public void setExpandJudge() {
        njudge[0] = njudge[1];
        njudge[1] = njudge[2];
        njudge[2] = njudge[3];
        sjudge[0] = sjudge[1];
        sjudge[1] = sjudge[2];
        sjudge[2] = sjudge[3];
        
    }

}

/**
 * 判定アルゴリズム
 *                             if(judgenote.getState() != 0) {
                                j = 5;
                            } else {
                                for (j = 0; j < judge.length
                                        && !(ptime >= timelines[i].getTime() - judge[j] && ptime <= timelines[i]
                                        .getTime() + judge[j]); j++) {
                                }
                            }

 * @author exch
 */
abstract class JudgeAlgorithm {

    private int j;

    public TimeLine getNote(int pos, TimeLine[] timelines, long ptime, int[] judge, int lane, boolean pmsjudge) {
        TimeLine tl = null;
        for (int i = pos; i < timelines.length && timelines[i].getTime() < ptime + judge[5]; i++) {
            if (timelines[i].getTime() >= ptime - judge[5]) {
                Note judgenote = timelines[i].getNote(lane);
                if (judgenote != null
                        && !(judgenote instanceof MineNote)
                        && !(judgenote instanceof LongNote && ((LongNote) judgenote).getEnd() == timelines[i])
                        ) {
                    if (tl == null) {
                        if(!(pmsjudge && (judgenote.getState() != 0 ||
                                (judgenote.getState() == 0 && judgenote.getTime() != 0 && ptime <= timelines[i].getTime() - judge[2])))) {
                            tl = timelines[i];
                            if(judgenote.getState() != 0) {
                                j = 5;
                            } else {
                                for (j = 0; j < judge.length
                                        && !(ptime >= timelines[i].getTime() - judge[j] && ptime <= timelines[i]
                                        .getTime() + judge[j]); j++) {
                                }
                            }
                        }
                    } else {
                        if(compare(tl, timelines[i], lane, ptime) == timelines[i]) {
                            if(!(pmsjudge && (judgenote.getState() != 0 ||
                                    (judgenote.getState() == 0 && judgenote.getTime() != 0 && ptime <= timelines[i].getTime() - judge[2])))) {
                                tl = timelines[i];
                                if(judgenote.getState() != 0) {
                                    j = 5;
                                } else {
                                    for (j = 0; j < judge.length
                                            && !(ptime >= timelines[i].getTime() - judge[j] && ptime <= timelines[i]
                                            .getTime() + judge[j]); j++) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return tl;
    }

    public int getJudge() {
        return j;
    }

    public abstract TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime);
}

/**
 * 判定アルゴリズム:LR2
 * 
 * @author exch
 */
class JudgeAlgorithmLR2 extends JudgeAlgorithm {

    @Override
    public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
        if(t1.getNote(lane).getState() != 0) {
            return t2;
        }
        if (t1.getTime() < t2.getTime() && t2.getNote(lane).getState() == 0 && t2.getTime() <= ptime) {
            return t2;
        }
        return t1;
    }
}

/**
 * 判定アルゴリズム:2DX
 * 
 * @author exch
 */
class JudgeAlgorithm2DX extends JudgeAlgorithm {

    @Override
    public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
        if(t1.getNote(lane).getState() != 0) {
            return t2;
        }
        if (Math.abs(t1.getTime() - ptime) < Math.abs(t2.getTime() - ptime) && t2.getNote(lane).getState() == 0) {
            return t2;
        }
        return t1;
    }
}

/**
 * 判定アルゴリズム:最下ノーツ優先
 * 
 * @author exch
 */
class JudgeAlgorithmLowestNote extends JudgeAlgorithm {

    @Override
    public TimeLine compare(TimeLine t1, TimeLine t2, int lane, long ptime) {
        if(t1.getNote(lane).getState() != 0) {
            return t2;
        }
        if (t1.getNote(lane).getState() != 0 && t2.getNote(lane).getState() == 0) {
            return t2;
        }
        return t1;
    }
}