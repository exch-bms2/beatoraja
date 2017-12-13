package bms.player.beatoraja.play;

/**
 * 判定の設定値
 *
 * @author exch
 */
public enum JudgeProperty {

    Default(new int[][]{ {-20, 20}, {-60, 60}, {-150, 150}, {-280, 220}, {-150, 500} },
            new int[][]{ {-30, 30}, {-70, 70}, {-160, 160}, {-290, 230}, {-160, 500}},
            new int[][]{ {-120, 120}, {-160, 160}, {-200, 200}, {-280, 220}},
            new int[][]{ {-130, 130}, {-170, 170}, {-210, 210}, {-290, 230}},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS
            ),
    PMS(new int[][]{ {-25, 25}, {-75, 75}, {-175, 175}, {-200, 200}, {-175, 500} },
            new int[][]{},
            new int[][]{ {-125, 125}, {-175, 175}, {-225, 225}, {-250, 250}},
            new int[][]{},
            new boolean[]{true, true, true, false, false, false },
            MissCondition.ONE
            ),
	KEYBOARD(new int[][]{ {-30, 30}, {-90, 90}, {-200, 200}, {-320, 240}, {-200, 650} },
			new int[][]{},
			new int[][]{ {-160, 25}, {-200, 75}, {-260, 140}, {-320, 240}},
			new int[][]{},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS
			),
	;

    /**
     * 通常ノートの格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    private final int[][] note;
    /**
     * スクラッチノートの格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    private final int[][] scratch;
    /**
     * 通常ロングノート終端の格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    private final int[][] longnote;
    /**
     * スクラッチロングノート終端の格判定幅。PG, GR, GD, BD, MSの順で{LATE下限, EARLY上限}のセットで表現する。
     */
    private final int[][] longscratch;
    /**
     * 各判定毎のコンボ継続
     */
    public final boolean[] combo;
    /**
     * MISSの発生回数
     */
    public final MissCondition miss;

    private JudgeProperty(int[][] note, int[][] scratch, int[][] longnote, int[][] longscratch, boolean[] combo, MissCondition miss) {
        this.note = note;
        this.scratch = scratch;
        this.longnote = longnote;
        this.longscratch = longscratch;
        this.combo = combo;
        this.miss = miss;
    }
    
    public int[][] getNoteJudge(int judgerank, int constraint) {
    	return create(note, judgerank, constraint);
    }
    
    public int[][] getLongNoteEndJudge(int judgerank, int constraint) {
    	return create(longnote, judgerank, constraint);
    }
    
    public int[][] getScratchJudge(int judgerank, int constraint) {
    	return create(scratch, judgerank, constraint);
    }
    
    public int[][] getLongScratchEndJudge(int judgerank, int constraint) {
    	return create(longscratch, judgerank, constraint);
    }
    
    private int[][] create(int[][] org, int judgerank, int constraint) {
		final int[][] judge = new int[org.length][2];
		for (int i = 0; i < judge.length; i++) {
			for(int j = 0;j < 2;j++) {
				if(i < 3) {
					if(i > constraint) {
						judge[i][j] = judge[i - 1][j];
					} else {
						judge[i][j] = org[i][j] * judgerank / 100;
						if(Math.abs(judge[i][j]) > Math.abs(org[3][j])) {
							judge[i][j] = org[3][j];
						}						
					}
				} else {
					judge[i][j] = org[i][j];
				}
			}
		}
		return judge;
    }
    
    public enum MissCondition {
    	ONE, ALWAYS
    }
}
