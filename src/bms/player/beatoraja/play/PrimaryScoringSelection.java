package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.ReplayData;

/** Frozen before play. The effective rule owns note consumption, sound, gauges and closure for the whole song. */
public record PrimaryScoringSelection(SecondaryScoringRule requested, SecondaryScoringRule effective, String reason) {
    public static PrimaryScoringSelection standard() {
        return new PrimaryScoringSelection(SecondaryScoringRule.BEATORAJA, SecondaryScoringRule.BEATORAJA, "");
    }

    public boolean isStandard() { return effective == SecondaryScoringRule.BEATORAJA; }

    static PrimaryScoringSelection resolveReplay(ReplayData replay, BMSModel model, PlayerResource resource) {
        var rule = SecondaryScoringRule.byId(replay.primaryScoringRuleId);
        if (rule == null) return standard();
        if (replay.scoringRuleVersion != 1) return fallback(rule, "Unsupported replay rule version");
        if (rule == SecondaryScoringRule.ANGOLMOIS && replay.scoringFrameUs != AngolmoisEngine.FRAME_US)
            return fallback(rule, "Unsupported replay frame period");
        var selection = resolve(rule, model, resource, replay.gauge);
        var requested = SecondaryScoringRule.byId(replay.requestedPrimaryScoringRuleId);
        if (rule == SecondaryScoringRule.BEATORAJA && requested != null) {
            return new PrimaryScoringSelection(requested, SecondaryScoringRule.BEATORAJA,
                    replay.primaryScoringFallbackReason != null ? replay.primaryScoringFallbackReason : "");
        }
        return selection;
    }

    static PrimaryScoringSelection resolve(SecondaryScoringRule requested, BMSModel model, PlayerResource resource, int gauge) {
        return resolve(requested, model, resource.getOriginalMode(), resource.getPlayMode(),
                resource.getCourseBMSModels() != null, gauge, resource.getChartRuleParameters());
    }

    /** Selection preview uses the same eligibility checks as play, before chart normalization. */
    public static PrimaryScoringSelection preview(SecondaryScoringRule requested, BMSModel model, int gauge) {
        return resolve(requested, model, model.getMode(), BMSPlayerMode.PLAY, false, gauge,
                model.getPath() != null && model.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".bmson")
                        ? null : BMSPlayerRule.ChartParameters.fromBms(model));
    }

    private static PrimaryScoringSelection resolve(SecondaryScoringRule requested, BMSModel model, Mode originalMode,
            BMSPlayerMode playMode, boolean course, int gauge, BMSPlayerRule.ChartParameters parameters) {
        if (requested == null || requested == SecondaryScoringRule.BEATORAJA) return standard();
        if (playMode == null || course
                || (playMode.mode != BMSPlayerMode.Mode.PLAY && playMode.mode != BMSPlayerMode.Mode.REPLAY)) {
            return fallback(requested, "Single-song play only");
        }
        if (model.getMode() != originalMode) return fallback(requested, "Converted key mode");
        if (requested == SecondaryScoringRule.RAINDROP || requested == SecondaryScoringRule.BEMUSE) {
            return fallback(requested, "Timing-only profile; dedicated gauge system is not implemented");
        }
        if (requested == SecondaryScoringRule.LR2) {
            boolean eligible = BMSPlayerRule.select(BMSPlayerRule.LunaticRave2, model, originalMode,
                    BMSPlayerMode.PLAY, false, gauge, parameters) == BMSPlayerRule.LunaticRave2;
            return eligible ? selected(requested) : fallback(requested, "Unsupported gauge, key mode or LN format");
        }
        if (model.getMode() != Mode.BEAT_7K) return fallback(requested, "7 KEY only");
        if (requested == SecondaryScoringRule.PULSUS && gauge != AbstractGrooveGauge.NORMAL
                && gauge != AbstractGrooveGauge.EASY && gauge != AbstractGrooveGauge.HARD) {
            return fallback(requested, "Unsupported gauge");
        }
        if (requested == SecondaryScoringRule.ANGOLMOIS) {
            if (gauge != AbstractGrooveGauge.NORMAL) return fallback(requested, "Unsupported gauge");
            try {
                if (AngolmoisChart.create(model).notes == 0) return fallback(requested, "Empty chart");
            } catch (java.io.IOException | RuntimeException e) {
                return fallback(requested, "Unsupported chart: " + e.getMessage());
            }
        }
        if (requested == SecondaryScoringRule.PULSUS) {
            for (var timeline : model.getAllTimeLines()) for (int lane = 0; lane < model.getMode().key; lane++) {
                if (timeline.getNote(lane) instanceof LongNote ln && (ln.getPair() == null
                        || !(ln.getType() == LongNote.TYPE_LONGNOTE || ln.getType() == LongNote.TYPE_UNDEFINED
                        && model.getLntype() == BMSModel.LNTYPE_LONGNOTE))) {
                    return fallback(requested, "Unsupported LN format");
                }
            }
        }
        return selected(requested);
    }

    private static PrimaryScoringSelection selected(SecondaryScoringRule rule) { return new PrimaryScoringSelection(rule, rule, ""); }
    private static PrimaryScoringSelection fallback(SecondaryScoringRule rule, String reason) {
        return new PrimaryScoringSelection(rule, SecondaryScoringRule.BEATORAJA, reason);
    }
}
