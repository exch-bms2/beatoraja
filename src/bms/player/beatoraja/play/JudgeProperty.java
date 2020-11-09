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
            new boolean[]{true, true, true, true, true, false },
            JudgeWindowRule.NORMAL
            ),
    SEVENKEYS(new int[][]{ {-20, 20}, {-60, 60}, {-150, 150}, {-280, 220}, {-150, 500} },
            new int[][]{ {-30, 30}, {-70, 70}, {-160, 160}, {-290, 230}, {-160, 500}},
            new int[][]{ {-120, 120}, {-160, 160}, {-200, 200}, {-280, 220}},
            new int[][]{ {-130, 130}, {-170, 170}, {-210, 210}, {-290, 230}},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS,
            new boolean[]{true, true, true, true, true, false },
            JudgeWindowRule.NORMAL
            ),
    PMS(new int[][]{ {-20, 20}, {-50, 50}, {-117, 117}, {-183, 183}, {-175, 500} },
            new int[][]{},
            new int[][]{ {-120, 120}, {-150, 150}, {-217, 217}, {-283, 283}},
            new int[][]{},
            new boolean[]{true, true, true, false, false, false },
            MissCondition.ONE,
            new boolean[]{true, true, true, false, true, false },
            JudgeWindowRule.PMS
            ),
	KEYBOARD(new int[][]{ {-30, 30}, {-90, 90}, {-200, 200}, {-320, 240}, {-200, 650} },
			new int[][]{},
			new int[][]{ {-160, 25}, {-200, 75}, {-260, 140}, {-320, 240}},
			new int[][]{},
            new boolean[]{true, true, true, false, false, true },
            MissCondition.ALWAYS,
            new boolean[]{true, true, true, true, true, false },
            JudgeWindowRule.NORMAL
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
    
    public final JudgeWindowRule windowrule;

    private JudgeProperty(int[][] note, int[][] scratch, int[][] longnote, int[][] longscratch, boolean[] combo, MissCondition miss, boolean[] judgeVanish, JudgeWindowRule windowrule) {
        this.note = note;
        this.scratch = scratch;
        this.longnote = longnote;
        this.longscratch = longscratch;
        this.combo = combo;
        this.miss = miss;
        this.judgeVanish = judgeVanish;
        this.windowrule = windowrule;
    }

    public int[][] getNoteJudge(int judgerank, int[] judgeWindowRate) {
    	return windowrule.create(note, judgerank, judgeWindowRate);
    }

    public int[][] getLongNoteEndJudge(int judgerank, int[] judgeWindowRate) {
    	return windowrule.create(longnote, judgerank, judgeWindowRate);
    }

    public int[][] getScratchJudge(int judgerank, int[] judgeWindowRate) {
    	return windowrule.create(scratch, judgerank, judgeWindowRate);
    }

    public int[][] getLongScratchEndJudge(int judgerank, int[] judgeWindowRate) {
    	return windowrule.create(longscratch, judgerank, judgeWindowRate);
    }
    
    public enum MissCondition {
    	ONE, ALWAYS
    }
    
    public enum JudgeWindowRule {
    	NORMAL (new int[]{25, 50, 75, 100, 125}){

			@Override
			public int[][] create(int[][] org, int judgerank, int[] judgeWindowRate) {
				return JudgeWindowRule.create(org, judgerank,judgeWindowRate, false);
			}
    		
    	},
    	PMS (new int[]{33, 50, 70, 100, 133}) {

			@Override
			public int[][] create(int[][] org, int judgerank, int[] judgeWindowRate) {
				return JudgeWindowRule.create(org, judgerank,judgeWindowRate, true);
			}
    		
    	};
    	
    	/**
    	 * JUDGERANKの倍率(VERYHARD, HARD, NORMAL, EASY, VERYEASY)
    	 */
    	public final int[] judgerank;
    	
        private static int[][] create(int[][] org, int judgerank, int[] judgeWindowRate, boolean pms) {
    		final int[][] judge = new int[org.length][2];
    		final boolean[] fix = pms ? new boolean[]{true, false, false, true, true} : new boolean[]{false, false, false, false, true};
    		for (int i = 0; i < judge.length; i++) {
    			for(int j = 0;j < 2;j++) {
					judge[i][j] = fix[i] ? org[i][j] : org[i][j] * judgerank / 100;
    			}
    		}

    		int fixmin = -1;
    		for (int i = 0; i < Math.min(org.length, 4); i++) {
    			if(fix[i]) {
    				fixmin = i;
    				continue;
    			}
        		int fixmax = -1;
    			for(int j = i + 1;j < 4;j++) {
        			if(fix[j]) {
        				fixmax = j;
        				break;
        			}
    			}
        		
    			for(int j = 0;j < 2;j++) {
					if(fixmin != -1 && Math.abs(judge[i][j]) < Math.abs(judge[fixmin][j])) {
						judge[i][j] = judge[fixmin][j];
					}
					if(fixmax != -1 && Math.abs(judge[i][j]) > Math.abs(judge[fixmax][j])) {
						judge[i][j] = judge[fixmax][j];
					}
    			}
    		}

    		// judgeWindowRateによる補正
    		for (int i = 0; i < Math.min(org.length, 3); i++) {
    			for(int j = 0;j < 2;j++) {
					judge[i][j] = judge[i][j]*judgeWindowRate[i] / 100;
					if(Math.abs(judge[i][j]) > Math.abs(judge[3][j])) {
						judge[i][j] = judge[3][j];
					}
					if(i > 0 && Math.abs(judge[i][j]) < Math.abs(judge[i - 1][j])) {
						judge[i][j] = judge[i - 1][j];
					}
    			}
    		}
    		
    		return judge;
        }
        
        private JudgeWindowRule(int[] judgerank) {
        	this.judgerank = judgerank;
        }
        
    	public abstract int[][] create(int[][] org, int judgerank, int[] judgeWindowRate);
    }
}
