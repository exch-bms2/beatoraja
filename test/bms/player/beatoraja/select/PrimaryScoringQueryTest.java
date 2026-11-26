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
        verifyBranchModes(directory, data, songs, player);
        System.out.println("PrimaryScoringQueryTest passed (effective SQL views, global ordering, updates, unplayed, native ranks and trophies)");
    }
    private static void verifyBranchModes(Path directory, PlayDataAccessor data, SQLiteSongDatabaseAccessor songs, PlayerConfig player) throws Exception {
        var file = directory.resolve("branch-mode.bme");
        Files.writeString(file, "#TITLE Branch mode\n#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#RANDOM 2\n#IF 1\n#00159:0101\n#ENDIF\n#IF 2\n#00119:01\n#ENDIF\n#ENDRANDOM\n#00219:01\n");
        var hold = ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 1, new int[]{1}));
        var taps = ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 1, new int[]{2}));
        writeCanonical(data, taps, 1, 0, ClearType.Easy);
        var standardHold = new ScoreData(Mode.BEAT_7K); standardHold.setSha256(hold.getSHA256()); standardHold.setNotes(3);
        standardHold.setEpg(3); standardHold.setClear(ClearType.Hard.id); standardHold.setGauge(AbstractGrooveGauge.HARD);
        data.writeScoreData(standardHold, hold, 1, true);
        data.writeRuleAttempt(SecondaryScoringRule.LR2, attempt(taps, ClearType.Normal, 0, 2, -1, -1), true);
        player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2); player.setGauge(AbstractGrooveGauge.NORMAL); player.setLnmode(1);
        for (int indexedBranch : new int[]{1, 2}) {
            var indexed = ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, new int[]{indexedBranch}));
            var song = new SongData(indexed, false); songs.setSongDatas(new SongData[]{song});
            player.refreshChartScoringPlan(song.getSha256());
            player.getChartScoringPlan(song.getSha256()).adopt(indexedBranch == 1 ? taps : hold);
            var cache = new PrimaryScoreDataCache(data, player);
            int expected = indexedBranch == 1 ? ClearType.Normal.id : ClearType.Hard.id;
            check(cache.readScoreData(song, 1).getClear() == expected, "Single read follows planned branch LN identity");
            cache.clear();
            cache.readScoreDatas((s, score) -> check(score != null && score.getClear() == expected,
                    "Bulk read follows branch LN identity for native and beatoraja fallback"), new SongData[]{song}, 1);
            var condition = "song.sha256 = '" + song.getSha256() + "' AND ";
            var matches = cache.querySongs(songs, condition + "score.clear = " + expected, null);
            var registered = Arrays.stream(songs.getAllSongDatas()).filter(s -> s.getSha256().equals(song.getSha256())).findFirst().orElseThrow();
            check(matches.length == 1, "SQL score view follows planned LN identity: indexed=" + indexedBranch
                    + ", expected clear=" + expected + ", mode=" + cache.scoringMode(registered, 1)
                    + ", rule=" + cache.effectiveRule(registered, 1) + ", matches=" + matches.length);
            check(cache.querySongs(songs, condition + "scorelog.clear = " + expected, null).length == 1,
                    "SQL update history follows the same planned LN identity");
            if (indexedBranch == 2) {
                check(cache.readScoreData(song, 0).getClear() == ClearType.Normal.id
                        && cache.readScoreData(song, 1).getClear() == ClearType.Hard.id,
                        "Explicit LN modes do not alias in the cache when the indexed branch had no LN");
                player.setLongnoteMode(1); player.setLongnoteRate(1);
                check(cache.readScoreData(song, 1).getClear() == ClearType.Normal.id,
                        "LN removal uses the modified chart's mode-zero native record");
                player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.BEATORAJA);
                check(cache.readScoreData(song, 1).getClear() == ClearType.Easy.id,
                        "LN removal also uses the modified chart's beatoraja record");
                player.setLongnoteMode(0); player.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
            }
        }
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
