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
}
