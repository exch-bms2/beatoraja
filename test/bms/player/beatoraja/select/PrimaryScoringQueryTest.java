package bms.player.beatoraja.select;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.song.*;
import java.nio.file.*;
import java.util.*;

public final class PrimaryScoringQueryTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("primary-query-");
        var config = new Config(); config.setPlayerpath(directory.toString()); config.setPlayername("test");
        Files.createDirectories(directory.resolve("test"));
        var data = new PlayDataAccessor(config);
        var a = chart(directory, "native", "19");
        var b = chart(directory, "standard", "11");
        var c = chart(directory, "unplayed-native", "19");
        var songs = new SQLiteSongDatabaseAccessor(directory.resolve("song.db").toString(), new String[0]);
        songs.setSongDatas(new SongData[]{new SongData(a, false), new SongData(b, false), new SongData(c, false)});
        writeCanonical(data, a, 0, 0, ClearType.Failed);
        writeCanonical(data, b, 1, 1, ClearType.Hard);
        writeCanonical(data, c, 2, 0, ClearType.Normal);
        var player = new PlayerConfig(); player.setGauge(AbstractGrooveGauge.NORMAL);
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
        var cache = new PrimaryScoreDataCache(data, player);
        data.writeRuleAttempt(SecondaryScoringRule.LR2, attempt(a, ClearType.Normal, 0, 2, -1, -1), true);
        check(hashes(cache.querySongs(songs, "score.clear = 5", null)).equals(Set.of(a.getSHA256())), "Clear filter uses each song's effective rule");
        check(hashes(cache.querySongs(songs, "song.sha256 = '" + a.getSHA256() + "' AND scorelog.score > scorelog.oldscore", null)).equals(Set.of(a.getSHA256())), "Update folders use native update logs");
        check(hashes(cache.querySongs(songs, "score.sha256 IS NULL", null)).equals(Set.of(c.getSHA256())), "Unplayed filter does not borrow canonical scores");
        var top = cache.querySongs(songs, "score.sha256 IS NOT NULL ORDER BY score.rule_score DESC LIMIT 1", null);
        check(top.length == 1 && top[0].getSha256().equals(b.getSHA256()), "ORDER BY and LIMIT apply globally across scoring rules");
        var best = data.readRuleBest(SecondaryScoringRule.LR2, a.getSHA256(), 0);
        check(best.getTrophy().indexOf(ScoreData.SongTrophy.GROOVE.character) >= 0
                && best.getTrophy().indexOf(ScoreData.SongTrophy.NORMAL.character) >= 0, "Native best preserves gauge and option trophies");
        var mirror = new ScoreData(Mode.BEAT_7K); mirror.setClear(ClearType.Hard.id); mirror.setOption(1);
        data.writeRuleAttempt(SecondaryScoringRule.LR2, attempt(a, ClearType.Hard, 1, 1, -1, -1).withMetadata(mirror), true);
        best = data.readRuleBest(SecondaryScoringRule.LR2, a.getSHA256(), 0);
        check(best.getTrophy().indexOf(ScoreData.SongTrophy.GROOVE.character) >= 0
                && best.getTrophy().indexOf(ScoreData.SongTrophy.HARD.character) >= 0
                && best.getTrophy().indexOf(ScoreData.SongTrophy.MIRROR.character) >= 0, "Native trophy accumulation survives later attempts");
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.ANGOLMOIS);
        data.writeRuleAttempt(SecondaryScoringRule.ANGOLMOIS, attempt(a, ClearType.Normal, 0, 0, 900, 1000), true);
        check(hashes(cache.querySongs(songs, "score.rule_score * 100.0 / NULLIF(score.rule_max_score, 0) >= 88.88", null)).equals(Set.of(a.getSHA256())), "Native rank SQL uses native score and maximum");
        player.setGauge(AbstractGrooveGauge.EXHARD);
        check(hashes(cache.querySongs(songs, "score.clear = 5", null)).equals(Set.of(c.getSHA256())), "Query eligibility follows unsupported-gauge fallback");
        var ln = chart(directory, "ln-mode", "59");
        songs.setSongDatas(new SongData[]{new SongData(ln, false)});
        writeCanonical(data, ln, 1, 0, ClearType.Easy);
        var cnScore = new ScoreData(Mode.BEAT_7K); cnScore.setSha256(ln.getSHA256()); cnScore.setNotes(1);
        cnScore.setEpg(1); cnScore.setClear(ClearType.Hard.id); cnScore.setGauge(AbstractGrooveGauge.HARD);
        data.writeScoreData(cnScore, ln, 1, true);
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
        player.setGauge(AbstractGrooveGauge.NORMAL); player.setLnmode(1);
        check(hashes(cache.querySongs(songs, "song.sha256 = '" + ln.getSHA256() + "' AND score.clear = 6", null)).equals(Set.of(ln.getSHA256())),
                "Undefined LN query selects the current LN mode and its beatoraja fallback");
        player.setLnmode(0);
        check(cache.querySongs(songs, "song.sha256 = '" + ln.getSHA256() + "' AND score.clear = 6", null).length == 0,
                "Other LN-mode records do not leak into native queries");
        System.out.println("PrimaryScoringQueryTest passed (effective SQL views, global ordering, updates, unplayed, native ranks and trophies)");
    }
    private static BMSModel chart(Path directory, String title, String lane) throws Exception {
        var file = directory.resolve(title + ".bme");
        Files.writeString(file, "#TITLE " + title + "\n#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#001" + lane + ":01\n#002" + lane + ":01\n");
        return ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, null));
    }
    private static void writeCanonical(PlayDataAccessor data, BMSModel model, int pg, int gr, ClearType clear) {
        var score = new ScoreData(model.getMode()); score.setSha256(model.getSHA256()); score.setNotes(2);
        score.setEpg(pg); score.setEgr(gr); score.setClear(clear.id); score.setMinbp(0); score.setGauge(AbstractGrooveGauge.NORMAL);
        data.writeScoreData(score, model, 0, true);
    }
    private static PlayDataAccessor.ScoreAttempt attempt(BMSModel model, ClearType clear, int pg, int gr, int nativeScore, int max) {
        return new PlayDataAccessor.ScoreAttempt(model.getSHA256(), 0, PlayDataAccessor.ScoreOutcome.FINISHED, 2, 2, 1, 0,
                clear, AbstractGrooveGauge.NORMAL, 90, List.of(pg, 0, gr, 0, 2 - pg - gr, 0, 0, 0, 0, 0, 0, 0), nativeScore, max);
    }
    private static Set<String> hashes(SongData[] songs) { return new HashSet<>(Arrays.stream(songs).map(SongData::getSha256).toList()); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
