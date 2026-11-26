package bms.player.beatoraja.select;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.song.SongData;
import java.nio.file.*;
import java.util.List;

public final class PrimaryScoreDataCacheTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("primary-select-");
        var file = directory.resolve("song.bme");
        Files.writeString(file, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 tap.wav\n#00119:01\n#00219:01\n");
        var model = ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, null));
        var song = new SongData(model, false);
        var config = new Config(); config.setPlayerpath(directory.toString()); config.setPlayername("test");
        Files.createDirectories(directory.resolve("test"));
        var data = new PlayDataAccessor(config);
        var standard = new ScoreData(model.getMode());
        standard.setSha256(model.getSHA256()); standard.setNotes(2); standard.setPassnotes(2);
        standard.setEgr(2); standard.setClear(ClearType.Easy.id);
        data.writeScoreData(standard, model, 0, true);
        data.writeRuleAttempt(SecondaryScoringRule.LR2, new PlayDataAccessor.ScoreAttempt(model.getSHA256(), 0,
                PlayDataAccessor.ScoreOutcome.FINISHED, 2, 2, 2, 0, ClearType.Perfect, AbstractGrooveGauge.NORMAL,
                100, List.of(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)), true);
        var player = new PlayerConfig(); player.setGauge(AbstractGrooveGauge.NORMAL);
        var cache = new PrimaryScoreDataCache(data, player);
        check(cache.readScoreData(song, 0).getExscore() == 2, "Default selection shows canonical beatoraja score");
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
        var nativeBest = cache.readScoreData(song, 0);
        check(nativeBest.getExscore() == 4 && nativeBest.getClear() == ClearType.Perfect.id, "Selected rule owns lamp and best score");
        int context = cache.selectionContext();
        player.setSevenToNinePattern(1);
        check(cache.selectionContext() != context && cache.readScoreData(song, 0).getExscore() == 2,
                "Key-mode conversion invalidates selector context and uses beatoraja fallback");
        player.setSevenToNinePattern(0);
        cache.clear();
        cache.readScoreDatas((s, score) -> check(score.getExscore() == 4, "Bulk song loading uses selected rule"), new SongData[]{song}, 0);
        player.setGauge(AbstractGrooveGauge.EXHARD);
        check(cache.readScoreData(song, 0).getExscore() == 2, "Unsupported gauge changes cache identity and falls back to beatoraja");
        player.setGauge(AbstractGrooveGauge.NORMAL);
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.BEMUSE);
        check(cache.readScoreData(song, 0).getClear() == ClearType.Easy.id, "Timing-only primary falls back in selection");
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.PULSUS);
        check(cache.readScoreData(song, 0) == null, "Unplayed selected rule never borrows beatoraja best");
        var holdFile = directory.resolve("hold.bme");
        Files.writeString(holdFile, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 tap.wav\n#00159:0101\n#00219:01\n");
        var hold = new SongData(ChartDecoder.getDecoder(holdFile).decode(new ChartInformation(holdFile, 0, null)), false);
        check(cache.readScoreData(hold, 0) == null && cache.scoringInfo(hold, 0).notes() == 3
                && cache.scoringInfo(hold, 0).maximumScore() == 6, "Unplayed Pulsus LN chart has scoring notes and maximum without a best");
        var selector = bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate(MusicSelector.class);
        var manager = bms.player.beatoraja.input.KeyboardQueueTestSupport.allocate(BarManager.class);
        manager.currentsongs = new bms.player.beatoraja.select.bar.Bar[]{new bms.player.beatoraja.select.bar.SongBar(hold)};
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(selector, MusicSelector.class, "manager", manager);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(selector, MusicSelector.class, "config", player);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(selector, MusicSelector.class, "scorecache", cache);
        bms.player.beatoraja.input.KeyboardQueueTestSupport.set(selector, MainState.class, "score", new ScoreDataProperty());
        var holdBar = manager.currentsongs[0];
        var holdBest = new ScoreData(Mode.BEAT_7K); holdBest.setNotes(3); holdBest.setEpg(3); holdBar.setScore(holdBest);
        check(bms.player.beatoraja.skin.property.FloatPropertyFactory.getFloatProperty(140).get(selector) == 1,
                "Selection PG rate uses Pulsus scoring endpoints instead of the indexed chart count");
        holdBest.setNotes(0);
        check(bms.player.beatoraja.skin.property.FloatPropertyFactory.getFloatProperty(140).get(selector) == 0,
                "Empty score produces a finite zero judge rate");
        holdBar.setScore(null);
        check(!selector.canCompareRival(holdBar), "Native-rule bar cannot show a beatoraja rival lamp");
        player.setGauge(AbstractGrooveGauge.EXHARD);
        check(selector.canCompareRival(holdBar), "Fallback bar can show the compatible beatoraja rival lamp");
        player.setGauge(AbstractGrooveGauge.NORMAL);
        check(bms.player.beatoraja.skin.property.IntegerPropertyFactory.getIntegerProperty(bms.player.beatoraja.skin.SkinProperty.NUMBER_TOTALNOTES).get(selector) == 3
                && bms.player.beatoraja.skin.property.IntegerPropertyFactory.getIntegerProperty(bms.player.beatoraja.skin.SkinProperty.NUMBER_MAXSCORE).get(selector) == 6,
                "Selection skin properties use chart scoring metadata when no best exists");
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.ANGOLMOIS);
        check(cache.scoringInfo(hold, 0).notes() == 2 && cache.scoringInfo(hold, 0).maximumScore() == 750,
                "Unplayed Angolmois maximum follows native scoring");
        player.setGauge(AbstractGrooveGauge.EXHARD);
        check(cache.scoringInfo(hold, 0).maximumScore() == 4, "Unplayed scoring metadata follows beatoraja fallback");
        System.out.println("PrimaryScoreDataCacheTest passed (lamps, personal bests, bulk/cache switching and fallback)");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
