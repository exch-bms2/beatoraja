package bms.player.beatoraja.play;

/**
 * 判定の設定値
 */
public enum JudgeProperty {

    Default(new int[][]{ {-20, 20}, {-60, 60}, {-160, 160}, {-250, 250}, {-350, 650} },
            new int[][]{ {-150, 20}, {-200, 60}, {-250, 160}, {-350, 250}, {-350, 650 }},
            new int[][]{ {-30, 30}, {-75, 75}, {-200, 200}, {-300, 300}},
            new int[][]{ {-150, 30}, {-200, 75}, {-250, 200}, {-350, 300}},
            false
            ),
    PMS(new int[][]{ {-25, 25}, {-75, 75}, {-175, 175}, {-200, 200}, {-350, 650} },
            new int[][]{ {-150, 125}, {-200, 75}, {-250, 175}, {-350, 200}, {-350, 650 }},
            new int[][]{ {-30, 30}, {-75, 75}, {-200, 200}, {-300, 300}},
            new int[][]{ {-100, 100}, {-150, 150}, {-200, 200}, {-250, 250}},
            true
            );

    /**
     * 通常ノートの格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    public final int[][] note;
    /**
     * スクラッチノートの格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    public final int[][] scratch;
    /**
     * 通常ロングノート終端の格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    public final int[][] longnote;
    /**
     * スクラッチロングノート終端の格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    public final int[][] longscratch;
    /**
     * PMSシステムを使用するかどうか(1note当たりmissは最大1回まで、missでコンボが切れる)
     */
    public final boolean pms;

    private JudgeProperty(int[][] note, int[][] scratch, int[][] longnote, int[][] longscratch, boolean pms) {
        this.note = note;
        this.scratch = scratch;
        this.longnote = longnote;
        this.longscratch = longscratch;
        this.pms = pms;
    }

}
