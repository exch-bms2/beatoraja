package bms.player.beatoraja.play;

import static bms.player.beatoraja.play.GrooveGauge.GaugeModifier.*;

/**
 * ゲージの仕様
 * 
 * @author exch
 */
public enum GaugeProperty {

    FIVEKEYS(
    		GaugeElementProperty.ASSIST_EASY_5,
            GaugeElementProperty.EASY_5,
            GaugeElementProperty.NORMAL_5,
            GaugeElementProperty.HARD_5,
            GaugeElementProperty.EXHARD_5,
            GaugeElementProperty.HAZARD_5,
            GaugeElementProperty.CLASS_5,
            GaugeElementProperty.EXCLASS_5,
            GaugeElementProperty.EXHARDCLASS_5),
    SEVENKEYS(
    		GaugeElementProperty.ASSIST_EASY,
            GaugeElementProperty.EASY,
            GaugeElementProperty.NORMAL,
            GaugeElementProperty.HARD,
            GaugeElementProperty.EXHARD,
            GaugeElementProperty.HAZARD,
            GaugeElementProperty.CLASS,
            GaugeElementProperty.EXCLASS,
            GaugeElementProperty.EXHARDCLASS),
    PMS(
            GaugeElementProperty.ASSIST_EASY_PMS,
            GaugeElementProperty.EASY_PMS,
            GaugeElementProperty.NORMAL_PMS,
            GaugeElementProperty.HARD_PMS,
            GaugeElementProperty.EXHARD_PMS,
            GaugeElementProperty.HAZARD_PMS,
            GaugeElementProperty.CLASS_PMS,
            GaugeElementProperty.EXCLASS_PMS,
            GaugeElementProperty.EXHARDCLASS_PMS),
    KEYBOARD(
            GaugeElementProperty.ASSIST_EASY_KB,
            GaugeElementProperty.EASY_KB,
            GaugeElementProperty.NORMAL_KB,
            GaugeElementProperty.HARD_KB,
            GaugeElementProperty.EXHARD_KB,
            GaugeElementProperty.HAZARD_KB,
            GaugeElementProperty.CLASS_KB,
            GaugeElementProperty.EXCLASS_KB,
            GaugeElementProperty.EXHARDCLASS_KB),
    LR2(
            GaugeElementProperty.ASSIST_EASY_LR2,
            GaugeElementProperty.EASY_LR2,
            GaugeElementProperty.NORMAL_LR2,
            GaugeElementProperty.HARD_LR2,
            GaugeElementProperty.EXHARD_LR2,
            GaugeElementProperty.HAZARD_LR2,
            GaugeElementProperty.CLASS_LR2,
            GaugeElementProperty.EXCLASS_LR2,
            GaugeElementProperty.EXHARDCLASS_LR2),
    ;

    public final GaugeElementProperty[] values;

    private GaugeProperty(GaugeElementProperty... values) {
        this.values = values;
    }

    /**
     * 各ゲージの仕様
     * 
     * @author exch
     */
    public enum GaugeElementProperty {

        ASSIST_EASY_5(TOTAL ,2 ,100 ,20, 50, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -3.0f, -0.5f}, new float[][]{}),
        EASY_5(TOTAL ,2 ,100 ,20, 75, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -4.5f, -1.0f}, new float[][]{}),
        NORMAL_5(TOTAL ,2 ,100 ,20, 75, new float[]{1.0f, 1.0f, 0.5f, -3.0f, -6.0f, -2.0f}, new float[][]{}),
        HARD_5(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0f, 0f, 0f, -5.0f, -10.0f, -5.0f}, new float[][]{}),
        EXHARD_5(MODIFY_DAMAGE ,0 ,100 ,100, 0, new float[]{0f, 0f, 0f, -10.0f, -20.0f, -10.0f}, new float[][]{}),
        HAZARD_5(null ,0 ,100 ,100, 0, new float[]{0f, 0f, 0f, -100.0f, -100.0f, -100.0f}, new float[][]{}),
        CLASS_5(null ,0 ,100 ,100, 0, new float[]{0.01f, 0.01f, 0f, -0.5f, -1.0f, -0.5f}, new float[][]{}),
        EXCLASS_5(null ,0 ,100 ,100, 0, new float[]{0.01f, 0.01f, 0f, -1.0f, -2.0f, -1.0f}, new float[][]{}),
        EXHARDCLASS_5(null ,0 ,100 ,100, 0, new float[]{0.01f, 0.01f, 0f, -2.5f, -5.0f, -2.5f}, new float[][]{}),

        ASSIST_EASY(TOTAL ,2 ,100 ,20, 60, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -3.0f, -0.5f}, new float[][]{}),
        EASY(TOTAL ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -4.5f, -1.0f}, new float[][]{}),
        NORMAL(TOTAL ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -3.0f, -6.0f, -2.0f}, new float[][]{}),
        HARD(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -5.0f, -10.0f, -5.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -8.0f, -16.0f, -8.0f}, new float[][]{}),
        HAZARD(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -100.0f, -100.0f, -10.0f}, new float[][]{}),
        CLASS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.06f, -1.5f, -3f, -1.5f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -3.0f, -6.0f, -3.0f}, new float[][]{}),
        EXHARDCLASS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -5.0f, -10.0f, -5.0f}, new float[][]{}),

        ASSIST_EASY_PMS(TOTAL ,2, 120, 30, 65, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -2.0f, -2.0f}, new float[][]{}),
        EASY_PMS(TOTAL ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -3.0f}, new float[][]{}),
        NORMAL_PMS(TOTAL ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -2.0f, -6.0f, -6.0f}, new float[][]{}),
        HARD_PMS(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -5.0f, -10.0f, -10.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD_PMS(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -10.0f, -15.0f, -15.0f}, new float[][]{}),
        HAZARD_PMS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -100.0f, -100.0f, -100.0f}, new float[][]{}),
        CLASS_PMS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.06f, -1.5f, -3f, -3f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS_PMS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.12f, 0.03f, -3.0f, -6.0f, -6.0f}, new float[][]{}),
        EXHARDCLASS_PMS(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -5.0f, -10.0f, -10.0f}, new float[][]{}),

        ASSIST_EASY_KB(TOTAL ,2, 100, 30, 50, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -2.0f, -1.0f}, new float[][]{}),
        EASY_KB(TOTAL ,2, 100, 20, 70, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -1.0f}, new float[][]{}),
        NORMAL_KB(TOTAL ,2, 100, 20, 70, new float[]{1.0f, 1.0f, 0.5f, -2.0f, -4.0f, -2.0f}, new float[][]{}),
        HARD_KB(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -4.0f, -8.0f, -4.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD_KB(LIMIT_INCREMENT ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -6.0f, -12.0f, -6.0f}, new float[][]{}),
        HAZARD_KB(null ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -100.0f, -100.0f, -100.0f}, new float[][]{}),
        CLASS_KB(null ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -1.5f, -3f, -1.5f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS_KB(null ,0 ,100 ,100, 0, new float[]{0.2f, 0.2f, 0.1f, -3.0f, -6.0f, -3.0f}, new float[][]{}),
        EXHARDCLASS_KB(null ,0 ,100 ,100, 0, new float[]{0.2f, 0.1f, 0, -5.0f, -10.0f, -5.0f}, new float[][]{}),

        ASSIST_EASY_LR2(TOTAL ,2 ,100 ,20, 60, new float[]{1.2f, 1.2f, 0.6f, -3.2f, -4.8f, -1.6f}, new float[][]{}),
        EASY_LR2(TOTAL ,2 ,100 ,20, 80, new float[]{1.2f, 1.2f, 0.6f, -3.2f, -4.8f, -1.6f}, new float[][]{}),
        NORMAL_LR2(TOTAL ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -4.0f, -6.0f, -2.0f}, new float[][]{}),
        HARD_LR2(MODIFY_DAMAGE ,0 ,100 ,100, 0, new float[]{0.1f, 0.1f, 0.05f, -6.0f, -10.0f, -2.0f}, new float[][]{{30, 0.6f}}),
        EXHARD_LR2(MODIFY_DAMAGE ,0 ,100 ,100, 0, new float[]{0.1f, 0.1f, 0.05f, -12.0f, -20.0f, -2.0f}, new float[][]{}),
        HAZARD_LR2(null ,0 ,100 ,100, 0, new float[]{0.15f, 0.06f, 0, -100.0f, -100.0f, -10.0f}, new float[][]{}),
        CLASS_LR2(null ,0 ,100 ,100, 0, new float[]{0.10f, 0.10f, 0.05f, -2f, -3f, -2f}, new float[][]{{30, 0.6f}}),
        EXCLASS_LR2(null ,0 ,100 ,100, 0, new float[]{0.10f, 0.10f, 0.05f, -6.0f, -10.0f, -2.0f}, new float[][]{{30, 0.6f}}),
        EXHARDCLASS_LR2(null ,0 ,100 ,100, 0, new float[]{0.10f, 0.10f, 0.05f, -12.0f, -20.0f, -2.0f}, new float[][]{}),
        ;

        /**
         * ゲージ増減補正タイプ
         */
        public final GrooveGauge.GaugeModifier modifier;
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

        private GaugeElementProperty(GrooveGauge.GaugeModifier modifier, float min, float max, float init, float border, float[] value, float[][] guts) {
            this.modifier = modifier;
            this.min = min;
            this.max = max;
            this.init = init;
            this.border = border;
            this.value = value;
            this.guts = guts;
        }

    }
}
