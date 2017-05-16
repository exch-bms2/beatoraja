package bms.player.beatoraja.play;

import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.ClearType.*;

/**
 * Created by exch on 2017/05/13.
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
    })

    ;

    public final GaugeElementProperty[] values;

    private GaugeProperty(GaugeElementProperty[] values) {
        this.values = values;
    }

    public enum GaugeElementProperty {

        ASSIST_EASY(0 ,2 ,100 ,20, 60, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -3.0f, 0}, new float[][]{}),
        ASSIST_EASY_PMS(0 ,2, 120, 30, 65, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -1.0f}, new float[][]{}),
        EASY(0 ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -1.5f, -4.5f, -0.5f}, new float[][]{}),
        EASY_PMS(0 ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -1.0f, -3.0f, -3.0f}, new float[][]{}),
        NORMAL(0 ,2 ,100 ,20, 80, new float[]{1.0f, 1.0f, 0.5f, -3.0f, -6.0f, -2.0f}, new float[][]{}),
        NORMAL_PMS(0 ,2, 120, 30, 85, new float[]{1.0f, 1.0f, 0.5f, -2.0f, -6.0f, -6.0f}, new float[][]{}),
        HARD(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0.05f, -5.0f, -10.0f, -5.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        HARD_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0.05f, -5.0f, -10.0f, -10.0f}, new float[][]{{10, 0.4f},{20, 0.5f},{30, 0.6f},{40, 0.7f},{50, 0.8f}}),
        EXHARD(2 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -10.0f, -15.0f, -10.0f}, new float[][]{}),
        EXHARD_PMS(2 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -10.0f, -15.0f, -15.0f}, new float[][]{}),
        HAZARD(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -100.0f, -100.0f, -10.0f}, new float[][]{}),
        HAZARD_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -100.0f, -100.0f, -100.0f}, new float[][]{}),

        CLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0.05f, -1.5f, -3f, -1.5f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        CLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0.05f, -1.5f, -3f, -3f}, new float[][]{{5, 0.4f},{10, 0.5f},{15, 0.6f},{20, 0.7f},{25, 0.8f}}),
        EXCLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0, -3.0f, -6.0f, -3.0f}, new float[][]{}),
        EXCLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.10f, 0, -3.0f, -6.0f, -6.0f}, new float[][]{}),
        EXHARDCLASS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -5.0f, -10.0f, -5.0f}, new float[][]{}),
        EXHARDCLASS_PMS(1 ,0 ,100 ,100, 0, new float[]{0.15f, 0.5f, 0, -5.0f, -10.0f, -10.0f}, new float[][]{}),
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
         * ゲージの現象補正テーブル。
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
