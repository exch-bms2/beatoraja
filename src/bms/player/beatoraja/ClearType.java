package bms.player.beatoraja;

/**
 * クリアタイプ
 *
 * @author exch
 */
public enum ClearType {

    NoPlay(0, new int[]{}),
    Failed(1, new int[]{}),
    AssistEasy(2, new int[]{}),
    LightAssistEasy(3, new int[]{0}),
    Easy(4, new int[]{1}),
    Normal(5, new int[]{2, 6}),
    Hard(6, new int[]{3, 7}),
    ExHard(7, new int[]{4, 8}),
    FullCombo(8, new int[]{5}),
    Perfect(9, new int[]{}),
    Max(10, new int[]{});

	/**
	 * クリアタイプID
	 */
    public final int id;
    /**
     * クリアタイプに対応したゲージタイプ
     */
    public final int[] gaugetype;

    private ClearType(int id, int[] gaugetype) {
        this.id = id;
        this.gaugetype = gaugetype;
    }

    /**
     * IDに対応するClearTypeを取得する
     * 
     * @param id ID
     * @return 対応するクリアタイプ。存在しない場合はNoPlay
     */
    public static ClearType getClearTypeByID(int id) {
        for(ClearType clear : ClearType.values()) {
        	if(clear.id == id) {
        		return clear;
        	}
        }
        return NoPlay;
    }

    public static ClearType getClearTypeByGauge(int gaugetype) {
        for(ClearType clear : ClearType.values()) {
            for(int type : clear.gaugetype) {
                if(gaugetype == type) {
                    return clear;
                }
            }
        }
        return null;
    }
}
