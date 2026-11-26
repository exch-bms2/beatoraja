package bms.player.beatoraja.play;

import bms.player.beatoraja.PlayerConfig;

/** Save eligibility and assist labels belong to the rule that actually uses an option. */
public record ScoringEligibility(boolean updateScore, int assist) {
    public static ScoringEligibility resolve(SecondaryScoringRule rule, PlayerConfig config,
            boolean commonAllowed, int commonAssist, boolean applyCustomJudge) {
        boolean expanded = applyCustomJudge && rule == SecondaryScoringRule.BEATORAJA && config.isCustomJudge()
                && (config.getKeyJudgeWindowRatePerfectGreat() > 100 || config.getKeyJudgeWindowRateGreat() > 100
                || config.getKeyJudgeWindowRateGood() > 100 || config.getScratchJudgeWindowRatePerfectGreat() > 100
                || config.getScratchJudgeWindowRateGreat() > 100 || config.getScratchJudgeWindowRateGood() > 100
                || config.getLongnoteMarginRate() > 100);
        int assist = expanded ? Math.max(commonAssist, 2) : commonAssist;
        return new ScoringEligibility(commonAllowed && assist == 0, assist);
    }
}
