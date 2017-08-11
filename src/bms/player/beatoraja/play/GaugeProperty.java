package bms.player.beatoraja.play;

/**
 * ゲージの仕様
 * 
 * @author exch
 */
public enum GaugeProperty {

    Default(new GaugeElementProperty[]{
        GaugeElementProperty.ASSIST_EASY,
            GaugeElementProperty.EASY,
            GaugeElementProperty.NORMAL,
            GaugeElementProperty.HARD,
            GaugeElementProperty.EXHARD,
            GaugeElementProperty.HAZARD,
            GaugeElementProperty.CLASS,
            GaugeElementProperty.EXCLASS,
            GaugeElementProperty.EXHARDCLASS,
    }),
    PMS(new GaugeElementProperty[]{
            GaugeElementProperty.ASSIST_EASY_PMS,
            GaugeElementProperty.EASY_PMS,
            GaugeElementProperty.NORMAL_PMS,
            GaugeElementProperty.HARD_PMS,
            GaugeElementProperty.EXHARD_PMS,
            GaugeElementProperty.HAZARD_PMS,
            GaugeElementProperty.CLASS_PMS,
            GaugeElementProperty.EXCLASS_PMS,
            GaugeElementProperty.EXHARDCLASS_PMS,
    }),
    KEYBOARD(new GaugeElementProperty[]{
            GaugeElementProperty.ASSIST_EASY_KB,
            GaugeElementProperty.EASY_KB,
            GaugeElementProperty.NORMAL_KB,
            GaugeElementProperty.HARD_KB,
            GaugeElementProperty.EXHARD_KB,
            GaugeElementProperty.HAZARD_KB,
            GaugeElementProperty.CLASS_KB,
            GaugeElementProperty.EXCLASS_KB,
            GaugeElementProperty.EXHARDCLASS_KB,
    }),
    ;

    public final GaugeElementProperty[] values;

    private GaugeProperty(GaugeElementProperty[] values) {
        this.values = values;
    }

    /**
     * 各ゲージの仕様
     * 
     * @author exch
     */
    public enum GaugeElementProperty {

        ASSIST_EASY(0 ,2 ,100 ,20, 60, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -3.0f, -0.5f}, new float[][]{}),
        EASY(0 ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -4.5f, -1.0f}, new float[][]{}),
        NORMAL(0 ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -3.0f, -6.0f, -2.0f}, new float[][]{}),
        HARD(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -5.0f, -10.0f, -5.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD(2 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -8.0f, -16.0f, -8.0f}, new float[][]{}),
        HAZARD(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -100.0f, -100.0f, -10.0f}, new float[][]{}),
        CLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.06f, -1.5f, -3f, -1.5f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -3.0f, -6.0f, -3.0f}, new float[][]{}),
        EXHARDCLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -5.0f, -10.0f, -5.0f}, new float[][]{}),
        
        ASSIST_EASY_PMS(0 ,2, 120, 30, 65, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -2.0f, -2.0f}, new float[][]{}),
        EASY_PMS(0 ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -3.0f}, new float[][]{}),
        NORMAL_PMS(0 ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -2.0f, -6.0f, -6.0f}, new float[][]{}),
        HARD_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -5.0f, -10.0f, -10.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD_PMS(2 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -10.0f, -15.0f, -15.0f}, new float[][]{}),
        HAZARD_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -100.0f, -100.0f, -100.0f}, new float[][]{}),
        CLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.06f, -1.5f, -3f, -3f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -3.0f, -6.0f, -6.0f}, new float[][]{}),
        EXHARDCLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -5.0f, -10.0f, -10.0f}, new float[][]{}),

        ASSIST_EASY_KB(0 ,2, 100, 30, 50, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -2.0f, -1.0f}, new float[][]{}),
        EASY_KB(0 ,2, 100, 20, 70, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -1.0f}, new float[][]{}),
        NORMAL_KB(0 ,2, 100, 20, 70, new float[]{1.0f, 1.0f, 0.5f, -2.0f, -4.0f, -2.0f}, new float[][]{}),
        HARD_KB(1 ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -4.0f, -8.0f, -4.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD_KB(2 ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -6.0f, -12.0f, -6.0f}, new float[][]{}),
        HAZARD_KB(1 ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -100.0f, -100.0f, -100.0f}, new float[][]{}),
        CLASS_KB(1 ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -1.5f, -3f, -1.5f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS_KB(1 ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -3.0f, -6.0f, -3.0f}, new float[][]{}),
        EXHARDCLASS_KB(1 ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -5.0f, -10.0f, -5.0f}, new float[][]{}),
        ;

        /**
         * 0:回復量としてTOTALを使用する。trueの場合はTOTAL/notesに対する倍率で指定する
         * 1:回復量としてTOTALを使用しない。
         * 2:回復量としてTOTALを使用しないが、低TOTALに対して減少補正をかける
         */
        public final int type;
        /**
         * 格判定のゲージ変化量。PG, GR, GD, BD, PR, MSの順
         */
        public final float[] value;
        /**
         * ゲージの最小値
         */
        public final float min;
        /**
         * ゲージの最大値
         */
        public final float max;
        /**
         * ゲージの初期値
         */
        public final float init;
        /**
         * ゲージのボーダー値
         */
        public final float border;
        /**
         * ゲージの現象補正テーブル
         */
        public final float[][] guts;

        private GaugeElementProperty(int type, float min, float max, float init, float border, float[] value, float[][] guts) {
            this.type = type;
            this.min = min;
            this.max = max;
            this.init = init;
            this.border = border;
            this.value = value;
            this.guts = guts;
        }

    }
}
