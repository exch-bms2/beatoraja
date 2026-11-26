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
        return modifiers(snapshot(config));
    }

    public bms.player.beatoraja.ReplayData.ChartModifiers snapshot(bms.player.beatoraja.PlayerConfig config) {
        var settings = new bms.player.beatoraja.ReplayData.ChartModifiers();
        settings.seed = seed;
        settings.scrollMode = config.getScrollMode(); settings.scrollSection = config.getScrollSection();
        settings.scrollRate = config.getScrollRate(); settings.longnoteMode = config.getLongnoteMode();
        settings.longnoteRate = config.getLongnoteRate(); settings.mineMode = config.getMineMode();
        settings.extranoteType = config.getExtranoteType(); settings.extranoteDepth = config.getExtranoteDepth();
        settings.extranoteScratch = config.isExtranoteScratch();
        return settings;
    }

    public static java.util.List<bms.player.beatoraja.pattern.PatternModifier> modifiers(bms.player.beatoraja.ReplayData.ChartModifiers settings) {
        var mods = new java.util.ArrayList<bms.player.beatoraja.pattern.PatternModifier>();
        if (settings == null) return mods;
        if (settings.scrollMode > 0) mods.add(new bms.player.beatoraja.pattern.ScrollSpeedModifier(
                settings.scrollMode - 1, settings.scrollSection, settings.scrollRate, settings.seed ^ 0x5343524f4c4cL));
        if (settings.longnoteMode > 0) mods.add(new bms.player.beatoraja.pattern.LongNoteModifier(
                settings.longnoteMode - 1, settings.longnoteRate, settings.seed));
        if (settings.mineMode > 0) mods.add(new bms.player.beatoraja.pattern.MineNoteModifier(settings.mineMode - 1, settings.seed ^ 0x4d494e45L));
        if (settings.extranoteDepth > 0) mods.add(new bms.player.beatoraja.pattern.ExtraNoteModifier(
                settings.extranoteType, settings.extranoteDepth, settings.extranoteScratch));
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
