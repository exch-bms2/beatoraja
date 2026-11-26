package bms.player.beatoraja.play;

import bms.model.*;
import java.util.concurrent.ThreadLocalRandom;

/** One preview/play generation of BMS branches and probabilistic LN changes. */
public final class ChartScoringPlan {
    private final long seed;
    private int[] selectedRandoms;

    public ChartScoringPlan() { this(ThreadLocalRandom.current().nextLong()); }
    public ChartScoringPlan(long seed) { this.seed = seed; }
    public long seed() { return seed; }

    public java.util.List<bms.player.beatoraja.pattern.PatternModifier> modifiers(bms.player.beatoraja.PlayerConfig config) {
        var mods = new java.util.ArrayList<bms.player.beatoraja.pattern.PatternModifier>();
        if (config.getScrollMode() > 0) mods.add(new bms.player.beatoraja.pattern.ScrollSpeedModifier(
                config.getScrollMode() - 1, config.getScrollSection(), config.getScrollRate(), seed ^ 0x5343524f4c4cL));
        if (config.getLongnoteMode() > 0) mods.add(new bms.player.beatoraja.pattern.LongNoteModifier(
                config.getLongnoteMode() - 1, config.getLongnoteRate(), seed));
        if (config.getMineMode() > 0) mods.add(new bms.player.beatoraja.pattern.MineNoteModifier(config.getMineMode() - 1, seed ^ 0x4d494e45L));
        if (config.getExtranoteDepth() > 0) mods.add(new bms.player.beatoraja.pattern.ExtraNoteModifier(
                config.getExtranoteType(), config.getExtranoteDepth(), config.isExtranoteScratch()));
        return mods;
    }

    public synchronized BMSModel decode(ChartDecoder decoder, ChartInformation info) {
        if (info.selectedRandoms != null) return decoder.decode(info);
        BMSModel model;
        if (selectedRandoms == null) {
            model = decoder.decode(info);
            if (model != null) selectedRandoms = model.getRandom() != null ? model.getRandom().clone() : new int[0];
        } else {
            model = decoder.decode(info.source != null ? new ChartInformation(info.source, info.lntype, selectedRandoms.clone())
                    : new ChartInformation(info.path, info.lntype, selectedRandoms.clone()));
        }
        return model;
    }

    public synchronized void adopt(BMSModel model) {
        if (selectedRandoms == null) selectedRandoms = model.getRandom() != null ? model.getRandom().clone() : new int[0];
    }
}
