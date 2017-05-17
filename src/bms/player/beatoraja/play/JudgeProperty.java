package bms.player.beatoraja.play;

/**
 * 判定の設定値
 */
public enum JudgeProperty {

    Default(new int[][]{ {-20, 20}, {-60, 60}, {-160, 140}, {-320, 240}, {-400, 600} },
            new int[][]{ {-160, 20}, {-200, 60}, {-260, 140}, {-320, 240}, {-400, 600 }},
            new int[][]{ {-25, 30}, {-75, 75}, {-200, 175}, {-320, 300}},
            new int[][]{ {-160, 30}, {-200, 75}, {-260, 175}, {-320, 300}},
            false
            ),
    PMS(new int[][]{ {-25, 25}, {-75, 75}, {-175, 175}, {-200, 200}, {-400, 600} },
            new int[][]{ {-160, 25}, {-200, 75}, {-260, 175}, {-320, 200}, {-400, 600 }},
            new int[][]{ {-25, 30}, {-75, 75}, {-200, 175}, {-320, 300}},
            new int[][]{ {-160, 30}, {-200, 75}, {-260, 175}, {-320, 300}},
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
