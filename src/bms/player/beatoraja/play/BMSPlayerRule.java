package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * Created by exch on 2017/05/12.
 */
public enum BMSPlayerRule {

    BEAT_5K(GaugeProperty.Default, JudgeProperty.Default),
    BEAT_7K(GaugeProperty.Default, JudgeProperty.Default),
    BEAT_10K(GaugeProperty.Default, JudgeProperty.Default),
    BEAT_14K(GaugeProperty.Default, JudgeProperty.Default),
    POPN_5K(GaugeProperty.PMS, JudgeProperty.PMS),
    POPN_9K(GaugeProperty.PMS, JudgeProperty.PMS),
    KEYBOARD_24K(GaugeProperty.KEYBOARD, JudgeProperty.KEYBOARD),
    Default(GaugeProperty.Default, JudgeProperty.Default),
    ;

    public final GaugeProperty gauge;
    public final JudgeProperty judge;

    private BMSPlayerRule(GaugeProperty gauge, JudgeProperty judge) {
        this.gauge = gauge;
        this.judge = judge;
    }

    public static BMSPlayerRule getBMSPlayerRule(Mode mode) {
        for(BMSPlayerRule bmsrule : BMSPlayerRule.values()) {
            if(bmsrule.name().equals(mode.name())) {
                return bmsrule;
            }
        }
        return Default;
    }
}

