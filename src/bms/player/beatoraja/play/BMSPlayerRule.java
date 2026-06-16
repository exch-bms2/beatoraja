package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;

/**
 * プレイヤールール
 * 
 * @author exch
 */
public enum BMSPlayerRule {

	Beatoraja_5(GaugeProperty.FIVEKEYS, JudgeProperty.FIVEKEYS, Mode.BEAT_5K, Mode.BEAT_10K),
	Beatoraja_7(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS, Mode.BEAT_7K, Mode.BEAT_14K),
	Beatoraja_9(GaugeProperty.PMS, JudgeProperty.PMS, Mode.POPN_5K, Mode.POPN_9K),
	Beatoraja_24(GaugeProperty.KEYBOARD, JudgeProperty.KEYBOARD, Mode.KEYBOARD_24K, Mode.KEYBOARD_24K_DOUBLE),
	Beatoraja_Other(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS),

	LR2(GaugeProperty.LR2, JudgeProperty.SEVENKEYS),

	Default(GaugeProperty.SEVENKEYS, JudgeProperty.SEVENKEYS),
;

	/**
	 * ゲージ仕様
	 */
    public final GaugeProperty gauge;
	/**
	 * 判定仕様
	 */
    public final JudgeProperty judge;
	/**
	 * 対象モード。全モード対象の場合は空列
	 */
	public final Mode[] mode;

    private BMSPlayerRule(GaugeProperty gauge, JudgeProperty judge, Mode... mode) {
        this.gauge = gauge;
        this.judge = judge;
        this.mode = mode;
    }

    public static BMSPlayerRule getBMSPlayerRule(Mode mode) {
        for(BMSPlayerRule bmsrule : BMSPlayerRuleSet.Beatoraja.ruleset) {
        	if(bmsrule.mode.length == 0) {
    			return bmsrule; 
        	}
        	for(Mode m : bmsrule.mode) {
        		if(mode == m) {
        			return bmsrule;
        		}
        	}
        }
        return Default;
    }
    
    public static void validate(BMSModel model) {
    	BMSPlayerRule rule = getBMSPlayerRule(model.getMode());
    	final int judgerank = model.getJudgerank();
		// TODO ここでjudgerank変換をせず、JudgeWindow側で実施する
    	model.setJudgerank(switch(model.getJudgerankType()) {
			case BMS_RANK -> judgerank >= 0 && model.getJudgerank() < 5 ? rule.judge.windowrule.judgerank[judgerank][1] : rule.judge.windowrule.judgerank[2][1];
			case BMS_DEFEXRANK -> judgerank > 0 ? judgerank * rule.judge.windowrule.judgerank[2][1] / 100 : rule.judge.windowrule.judgerank[2][1];
			case BMSON_JUDGERANK -> judgerank > 0 ? judgerank : 100;
    	});
    	model.setJudgerankType(BMSModel.JudgeRankType.BMSON_JUDGERANK);
		
    	switch(model.getTotalType()) {
			case BMS -> {
				// TOTAL未定義の場合
				if (model.getTotal() <= 0.0) {
					model.setTotal(calculateDefaultTotal(model.getMode(), model.getTotalNotes()));
				}
			}
			case BMSON -> {
				final double total = calculateDefaultTotal(model.getMode(), model.getTotalNotes());
				model.setTotal(model.getTotal() > 0 ? model.getTotal() / 100.0 * total : total);
			}
    	};
    	model.setTotalType(BMSModel.TotalType.BMS);
    }
    
	private static double calculateDefaultTotal(Mode mode, int totalnotes) {
		return switch (mode) {
			case BEAT_5K, BEAT_7K, BEAT_10K, BEAT_14K, POPN_5K, POPN_9K -> Math.max(260.0, 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
			case KEYBOARD_24K, KEYBOARD_24K_DOUBLE -> Math.max(300.0, 7.605 * (totalnotes + 100) / (0.01 * totalnotes + 6.5));
		};
	}
}

enum BMSPlayerRuleSet {
	
	Beatoraja(BMSPlayerRule.Beatoraja_5, BMSPlayerRule.Beatoraja_7, BMSPlayerRule.Beatoraja_9, BMSPlayerRule.Beatoraja_24,  BMSPlayerRule.Beatoraja_Other),
	LR2(BMSPlayerRule.LR2);
	
	public final BMSPlayerRule[] ruleset;
	
    private BMSPlayerRuleSet(BMSPlayerRule... ruleset) {
    	this.ruleset = ruleset;
    }
}
