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

    public boolean usesStandardRule(SongData song, int lnmode) { return effectiveRule(song, lnmode) == SecondaryScoringRule.BEATORAJA; }

    @Override public void clear() { super.clear(); effectiveRules.clear(); }

    public PrimaryScoreDataCache(PlayDataAccessor data, PlayerConfig config) { this.data = data; this.config = config; }

    private Mode mode(SongData song) {
        return Arrays.stream(Mode.values()).filter(mode -> mode.id == song.getMode()).findFirst().orElse(null);
    }

    int selectionContext() {
        int key = Objects.hash(config.getGauge(), config.getLnmode(), config.getSevenToNinePattern(), config.getDoubleoption(),
                config.getLongnoteMode(), config.getLongnoteRate());
        for (var mode : Mode.values()) key = 31 * key + config.getPrimaryScoringRule(mode).ordinal();
        return key;
    }

    @Override protected String cacheKey(SongData song) {
        return song.getSha256() + ":" + config.getPrimaryScoringRule(mode(song)).id() + ":" + config.getGauge()
                + ":" + config.getSevenToNinePattern() + ":" + config.getDoubleoption() + ":" + config.getLongnoteMode()
                + ":" + config.getLongnoteRate();
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
        if (config.getLongnoteMode() == 0 && (requested == SecondaryScoringRule.LR2 || requested == SecondaryScoringRule.PULSUS)) {
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
            BMSModel model;
            if (song.getPath() == null) model = song.getBMSModel();
            else {
                var file = SongResources.fromPath(Path.of(song.getPath()));
                var source = new ChartSource() {
                    public String location() { return file.displayPath(); }
                    public java.io.InputStream openStream() throws java.io.IOException { return file.openStream(); }
                };
                var decoder = ChartDecoder.getDecoder(source);
                model = decoder != null ? decoder.decode(new ChartInformation(source, lnmode, null)) : null;
            }
            if (model != null && config.getLongnoteMode() > 0)
                new bms.player.beatoraja.pattern.LongNoteModifier(config.getLongnoteMode() - 1, config.getLongnoteRate()).modify(model);
            return model != null ? PrimaryScoringSelection.preview(requested, model, config.getGauge()).effective()
                    : SecondaryScoringRule.BEATORAJA;
        } catch (RuntimeException e) {
            return SecondaryScoringRule.BEATORAJA;
        }
    }

    private ScoreData read(SongData song, int lnmode, SecondaryScoringRule rule) {
        return rule == SecondaryScoringRule.BEATORAJA
                ? data.readScoreData(song.getSha256(), song.hasUndefinedLongNote(), lnmode)
                : data.readRuleBest(rule, song.getSha256(), song.hasUndefinedLongNote() ? lnmode : 0);
    }

    @Override protected ScoreData readScoreDatasFromSource(SongData song, int lnmode) {
        return read(song, lnmode, effectiveRule(song, lnmode));
    }

    @Override protected void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode) {
        var standard = new ArrayList<SongData>();
        for (var song : songs) {
            var rule = effectiveRule(song, lnmode);
            if (rule == SecondaryScoringRule.BEATORAJA) standard.add(song);
            else collector.collect(song, read(song, lnmode, rule));
        }
        if (!standard.isEmpty()) data.readScoreDatas(collector, standard.toArray(SongData[]::new), lnmode);
    }
}
