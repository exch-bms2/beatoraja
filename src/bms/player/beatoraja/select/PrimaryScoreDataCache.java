package bms.player.beatoraja.select;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.song.*;
import java.nio.file.Path;
import java.util.*;

/** Song lamps and personal bests follow the same rule eligibility as single-song play. */
public final class PrimaryScoreDataCache extends ScoreDataCache {
    private final PlayDataAccessor data;
    private final PlayerConfig config;
    private final Map<String, SecondaryScoringRule> effectiveRules = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, PrimaryScoringSelection.ScoringInfo> scoringInfos = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> scoringModes = new java.util.concurrent.ConcurrentHashMap<>();

    /** One effective score/log view preserves global SQL ordering, limits and per-song fallback. */
    public SongData[] querySongs(SongDatabaseAccessor database, String sql, String info) {
        var rules = java.util.EnumSet.of(SecondaryScoringRule.BEATORAJA);
        for (var mode : bms.model.Mode.values()) rules.add(config.getPrimaryScoringRule(mode));
        var sources = new java.util.ArrayList<SongDatabaseAccessor.ScoringSource>();
        var ordered = new java.util.ArrayList<SecondaryScoringRule>();
        ordered.add(SecondaryScoringRule.BEATORAJA);
        rules.remove(SecondaryScoringRule.BEATORAJA);
        ordered.addAll(rules);
        for (var rule : ordered) {
            if (rule == SecondaryScoringRule.RAINDROP || rule == SecondaryScoringRule.BEMUSE) continue;
            var paths = data.scoreQueryPaths(rule);
            if (paths != null) sources.add(new SongDatabaseAccessor.ScoringSource(rule.id(), paths.score(), paths.scorelog()));
        }
        var applied = new java.util.HashMap<String, String>();
        var modes = new java.util.HashMap<String, Integer>();
        for (var song : database.getAllSongDatas()) {
            applied.put(song.getSha256(), effectiveRule(song, config.getLnmode()).id());
            modes.put(song.getSha256(), scoringMode(song, config.getLnmode()));
        }
        return database.getSongDatasForScoring(sql, sources, applied, modes, info, config.getLnmode());
    }

    public boolean usesStandardRule(SongData song, int lnmode) { return effectiveRule(song, lnmode) == SecondaryScoringRule.BEATORAJA; }

    @Override public void clear() { super.clear(); effectiveRules.clear(); scoringInfos.clear(); scoringModes.clear(); }

    private boolean needsPlannedMode(SongData song) { return song.hasRandomSequence() || config.getLongnoteMode() > 0; }

    int scoringMode(SongData song, int lnmode) {
        if (!needsPlannedMode(song)) return song.hasUndefinedLongNote() ? lnmode : 0;
        return scoringModes.computeIfAbsent(cacheKey(song, lnmode), key -> {
            try {
                var model = loadModel(song, lnmode);
                if (model != null) return model.containsUndefinedLongNote() ? model.getLntype() : 0;
            } catch (RuntimeException ignored) { /* Keep indexed metadata when the chart cannot be previewed. */ }
            return song.hasUndefinedLongNote() ? lnmode : 0;
        });
    }

    @Override protected String cacheKey(SongData song, int lnmode) { return cacheKey(song) + ":" + lnmode; }

    public PrimaryScoringSelection.ScoringInfo scoringInfo(SongData song, int lnmode) {
        return scoringInfos.computeIfAbsent(cacheKey(song) + ":" + lnmode, key -> {
            var rule = effectiveRule(song, lnmode);
            if ((rule == SecondaryScoringRule.BEATORAJA || rule == SecondaryScoringRule.LR2)
                    && config.getLongnoteMode() == 0 && config.getExtranoteDepth() == 0 && !song.hasRandomSequence())
                return new PrimaryScoringSelection.ScoringInfo(song.getNotes(), song.getNotes() * 2);
            try {
                var model = loadModel(song, lnmode);
                return model != null ? PrimaryScoringSelection.scoringInfo(rule, model) : null;
            } catch (java.io.IOException | RuntimeException e) { return null; }
        });
    }

    private BMSModel loadModel(SongData song, int lnmode) {
        BMSModel model;
        if (song.getPath() == null) {
            var original = song.getBMSModel();
            var info = original != null ? original.getChartInformation() : null;
            if (info == null) return config.getLongnoteMode() == 0 ? original : null;
            var decoder = info.source != null ? ChartDecoder.getDecoder(info.source) : ChartDecoder.getDecoder(info.path);
            var input = info.source != null ? new ChartInformation(info.source, lnmode, null) : new ChartInformation(info.path, lnmode, null);
            model = decoder != null ? config.getChartScoringPlan(song.getSha256()).decode(decoder, input) : null;
        }
        else {
            var file = SongResources.fromPath(Path.of(song.getPath()));
            var source = new ChartSource() {
                public String location() { return file.displayPath(); }
                public java.io.InputStream openStream() throws java.io.IOException { return file.openStream(); }
            };
            var decoder = ChartDecoder.getDecoder(source);
            model = decoder != null ? config.getChartScoringPlan(song.getSha256()).decode(decoder, new ChartInformation(source, lnmode, null)) : null;
        }
        if (model != null) {
            bms.model.BMSModelUtils.setStartNoteTime(model, 1000);
            BMSPlayerRule.validate(model);
            for (var modifier : config.getChartScoringPlan(song.getSha256()).modifiers(config)) modifier.modify(model);
        }
        return model;
    }

    public PrimaryScoreDataCache(PlayDataAccessor data, PlayerConfig config) { this.data = data; this.config = config; }

    private Mode mode(SongData song) {
        return Arrays.stream(Mode.values()).filter(mode -> mode.id == song.getMode()).findFirst().orElse(null);
    }

    int selectionContext() {
        int key = Objects.hash(config.getGauge(), config.getLnmode(), config.getSevenToNinePattern(), config.getDoubleoption(),
                config.getLongnoteMode(), config.getLongnoteRate(), modifierContext());
        for (var mode : Mode.values()) key = 31 * key + config.getPrimaryScoringRule(mode).ordinal();
        return key;
    }

    @Override protected String cacheKey(SongData song) {
        return song.getSha256() + ":" + config.getPrimaryScoringRule(mode(song)).id() + ":" + config.getGauge()
                + ":" + config.getSevenToNinePattern() + ":" + config.getDoubleoption() + ":" + config.getLongnoteMode()
                + ":" + config.getLongnoteRate() + ":" + modifierContext() + ":" + config.getChartScoringPlan(song.getSha256()).seed();
    }

    private int modifierContext() {
        return Objects.hash(config.getScrollMode(), config.getScrollSection(), config.getScrollRate(), config.getMineMode(),
                config.getExtranoteType(), config.getExtranoteDepth(), config.isExtranoteScratch());
    }

    SecondaryScoringRule effectiveRule(SongData song, int lnmode) {
        return effectiveRules.computeIfAbsent(cacheKey(song) + ":" + lnmode, key -> computeEffectiveRule(song, lnmode));
    }

    private SecondaryScoringRule computeEffectiveRule(SongData song, int lnmode) {
        var requested = config.getPrimaryScoringRule(mode(song));
        if (requested == SecondaryScoringRule.BEATORAJA) return requested;
        var mode = mode(song);
        if (requested == SecondaryScoringRule.RAINDROP || requested == SecondaryScoringRule.BEMUSE
                || mode == Mode.BEAT_7K && config.getSevenToNinePattern() > 0
                || config.getDoubleoption() >= 2 && (mode == Mode.BEAT_5K || mode == Mode.BEAT_7K || mode == Mode.KEYBOARD_24K))
            return SecondaryScoringRule.BEATORAJA;
        // Ordinary LR2/Pulsus eligibility is fully described by indexed chart features. Avoid decoding every song in a folder.
        if (config.getLongnoteMode() == 0 && !song.hasRandomSequence() && (requested == SecondaryScoringRule.LR2 || requested == SecondaryScoringRule.PULSUS)) {
            boolean keys = requested == SecondaryScoringRule.PULSUS ? mode == Mode.BEAT_7K
                    : mode == Mode.BEAT_5K || mode == Mode.BEAT_7K || mode == Mode.POPN_9K || mode == Mode.BEAT_10K || mode == Mode.BEAT_14K;
            int gauge = config.getGauge();
            boolean supportedGauge = gauge == AbstractGrooveGauge.NORMAL || gauge == AbstractGrooveGauge.EASY
                    || gauge == AbstractGrooveGauge.HARD || requested == SecondaryScoringRule.LR2 && gauge == AbstractGrooveGauge.HAZARD;
            boolean ordinaryLn = !song.hasChargeNote() && !song.hasHellChargeNote() && (!song.hasUndefinedLongNote() || lnmode == 0);
            boolean bms = requested != SecondaryScoringRule.LR2 || song.getPath() == null
                    || !song.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".bmson");
            return keys && supportedGauge && ordinaryLn && bms ? requested : SecondaryScoringRule.BEATORAJA;
        }
        try {
            BMSModel model = loadModel(song, lnmode);
            return model != null ? PrimaryScoringSelection.preview(requested, model, config.getGauge()).effective()
                    : SecondaryScoringRule.BEATORAJA;
        } catch (RuntimeException e) {
            return SecondaryScoringRule.BEATORAJA;
        }
    }

    private ScoreData read(SongData song, int lnmode, SecondaryScoringRule rule) {
        int mode = scoringMode(song, lnmode);
        return rule == SecondaryScoringRule.BEATORAJA
                ? data.readScoreData(song.getSha256(), true, mode)
                : data.readRuleBest(rule, song.getSha256(), mode);
    }

    @Override protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
        return read(song, lnmode, effectiveRule(song, lnmode));
    }

    @Override protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
        var standard = new ArrayList<SongData>();
        for (var song : songs) {
            var rule = effectiveRule(song, lnmode);
            if (rule == SecondaryScoringRule.BEATORAJA && !needsPlannedMode(song)) standard.add(song);
            else collector.collect(song, read(song, lnmode, rule));
        }
        if (!standard.isEmpty()) data.readScoreDatas(collector, standard.toArray(SongData[]::new), lnmode);
    }
}
