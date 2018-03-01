package bms.player.beatoraja.play;

/**
 * 判定の設定値
 *
 * @author exch
 */
public enum JudgeProperty {

    FIVEKEYS(new int[][]{ {-20, 20}, {-50, 50}, {-100, 100}, {-150, 150}, {-150, 500} },
            new int[][]{ {-30, 30}, {-60, 60}, {-110, 110}, {-160, 160}, {-160, 500}},
            new int[][]{ {-120, 120}, {-150, 150}, {-200, 200}, {-250, 250}},
            new int[][]{ {-130, 130}, {-160, 160}, {-110, 110}, {-260, 260}},
            new boolean[]{true, true, true, false, false, false },
            MissCondition.ALWAYS,
            new boolean[]{true, true, true, true, true, false }
            ),
    SEVENKEYS(new int[][]{ {-20, 20}, {-60, 60}, {-150, 150}, {-280, 220}, {-150, 500} },
            new int[][]{ {-30, 30}, {-70, 70}, {-160, 160}, {-290, 230}, {-160, 500}},
            new int[][]{ {-120, 120}, {-160, 160}, {-200, 200}, {-280, 220}},
            new int[][]{ {-130, 130}, {-170, 170}, {-210, 210}, {-290, 230}},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS,
            new boolean[]{true, true, true, true, true, false }
            ),
    PMS(new int[][]{ {-20, 20}, {-50, 50}, {-117, 117}, {-183, 183}, {-175, 500} },
            new int[][]{},
            new int[][]{ {-120, 120}, {-150, 150}, {-217, 217}, {-283, 283}},
            new int[][]{},
            new boolean[]{true, true, true, false, false, false },
            MissCondition.ONE,
            new boolean[]{true, true, true, false, true, false }
            ),
	KEYBOARD(new int[][]{ {-30, 30}, {-90, 90}, {-200, 200}, {-320, 240}, {-200, 650} },
			new int[][]{},
			new int[][]{ {-160, 25}, {-200, 75}, {-260, 140}, {-320, 240}},
			new int[][]{},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS,
            new boolean[]{true, true, true, true, true, false }
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
    /**
     * 各判定毎のノートの判定を消失するかどうか。PG, GR, GD, BD, PR, MSの順
     */
    public final boolean[] judgeVanish;

    private JudgeProperty(int[][] note, int[][] scratch, int[][] longnote, int[][] longscratch, boolean[] combo, MissCondition miss, boolean[] judgeVanish) {
        this.note = note;
        this.scratch = scratch;
        this.longnote = longnote;
        this.longscratch = longscratch;
        this.combo = combo;
        this.miss = miss;
        this.judgeVanish = judgeVanish;
    }

    public int[][] getNoteJudge(int judgerank, int judgeWindowRate, int constraint, boolean pms) {
    	return create(note, judgerank, judgeWindowRate, constraint, pms);
    }

    public int[][] getLongNoteEndJudge(int judgerank, int judgeWindowRate, int constraint, boolean pms) {
    	return create(longnote, judgerank, judgeWindowRate, constraint, pms);
    }

    public int[][] getScratchJudge(int judgerank, int judgeWindowRate, int constraint) {
    	return create(scratch, judgerank, judgeWindowRate, constraint, false);
    }

    public int[][] getLongScratchEndJudge(int judgerank, int judgeWindowRate, int constraint) {
    	return create(longscratch, judgerank, judgeWindowRate, constraint, false);
    }

    private int[][] create(int[][] org, int judgerank, int judgeWindowRate, int constraint, boolean pms) {
		final int[][] judge = new int[org.length][2];
		for (int i = 0; i < judge.length; i++) {
			for(int j = 0;j < 2;j++) {
				if((judgeWindowRate <= 100 && i < 3) || (judgeWindowRate > 100 && i < 2)) {
					if(i > constraint) {
						judge[i][j] = judge[i - 1][j];
					} else {
						//PMS時はjudgerankによらずにPG幅固定
						judge[i][j] = org[i][j] * (pms && i == 0 ? 100 : judgerank) / 100 * judgeWindowRate / 100;
						if(Math.abs(judge[i][j]) > Math.abs(org[3][j]) && judgeWindowRate <= 100) {
							judge[i][j] = org[3][j];
						}
					}
				} else if(judgeWindowRate <= 100 || (i == 4 && j == 0)) {
					judge[i][j] = org[i][j];
				} else {
					int sign = org[i][j] >= 0 ? 1 : -1;
					int difference = Math.abs(org[i][j] * (i < 3 ? judgerank : 100) / 100) - Math.abs(org[i - 1][j] * (i - 1 < 3 ? judgerank : 100) / 100);
					judge[i][j] = sign * (Math.abs(judge[i - 1][j]) + difference);
				}
			}
		}
		return judge;
    }
    
    public enum MissCondition {
    	ONE, ALWAYS
    }
}
