package bms.player.beatoraja.play;

import bms.model.Mode;

/**
 * プレイヤールール
 * 
 * @author exch
 */
public enum BMSPlayerRule {

    BEAT_5K(GaugeProperty.FIVEKEYS, JudgeProperty.FIVEKEYS),
    BEAT_7K(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS),
    BEAT_10K(GaugeProperty.FIVEKEYS, JudgeProperty.FIVEKEYS),
    BEAT_14K(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS),
    POPN_5K(GaugeProperty.PMS, JudgeProperty.PMS),
    POPN_9K(GaugeProperty.PMS, JudgeProperty.PMS),
    KEYBOARD_24K(GaugeProperty.KEYBOARD, JudgeProperty.KEYBOARD),
    KEYBOARD_24K_DOUBLE(GaugeProperty.KEYBOARD, JudgeProperty.KEYBOARD),
    LR2(GaugeProperty.LR2, JudgeProperty.SEVENKEYS),
    Default(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS),
    ;

    public final GaugeProperty gauge;
    public final JudgeProperty judge;

    private static boolean isSevenToNine = false;

    public static void setSevenToNine(boolean sevenToNine) {
        BMSPlayerRule.isSevenToNine = sevenToNine;
    }
    public static boolean isSevenToNine() {
        return BMSPlayerRule.isSevenToNine;
    }

    private BMSPlayerRule(GaugeProperty gauge, JudgeProperty judge) {
        this.gauge = gauge;
        this.judge = judge;
    }

    public static BMSPlayerRule getBMSPlayerRule(Mode mode) {
        for(BMSPlayerRule bmsrule : BMSPlayerRule.values()) {
            if(bmsrule.name().equals(mode.name())) {
                if(mode == Mode.POPN_9K && isSevenToNine) return BEAT_7K;
                else return bmsrule;
            }
        }
        return Default;
    }
}

