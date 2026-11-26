package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.select.PrimaryScoreDataCache;
import bms.player.beatoraja.song.SongData;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

public final class ChartScoringPlanTest {
    public static void main(String[] args) throws Exception {
        var file = Files.createTempFile("scoring-plan-", ".bme");
        Files.writeString(file, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n#RANDOM 2\n#IF 1\n#00119:01\n#ENDIF\n#IF 2\n#00119:0101\n#ENDIF\n#ENDRANDOM\n#00301:01\n");
        var original = decode(file);
        var song = new SongData(original, false);
        var load = PlayerResource.class.getDeclaredMethod("loadBMSModel", ChartInformation.class, boolean.class);
        load.setAccessible(true);
        var observed = EnumSet.noneOf(SecondaryScoringRule.class);
        for (long seed : new long[]{0, 4096, 8192, 12345}) {
            var config = new PlayerConfig(); config.setGauge(AbstractGrooveGauge.NORMAL);
            config.setPrimaryScoringRule(Mode.BEAT_7K, SecondaryScoringRule.LR2);
            config.setLongnoteMode(3); config.setLongnoteRate(.5);
            var plan = new ChartScoringPlan(seed);
            plan.adopt(ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, new int[]{1})));
            var plans = new ConcurrentHashMap<String, ChartScoringPlan>(); plans.put(original.getSHA256(), plan);
            set(config, PlayerConfig.class, "chartScoringPlans", plans);
            var cache = new PrimaryScoreDataCache(null, config);
            boolean standard = cache.usesStandardRule(song, 0);
            var expected = cache.scoringInfo(song, 0);
            var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
            set(resource, PlayerResource.class, "pconfig", config);
            set(resource, PlayerResource.class, "mode", BMSPlayerMode.PLAY);
            set(resource, PlayerResource.class, "orgmode", Mode.BEAT_7K);
            int[] branch = null;
            for (int repeat = 0; repeat < 4; repeat++) {
                var actual = (BMSModel) load.invoke(resource, new ChartInformation(file, 0, null), true);
                for (var modifier : plan.modifiers(config)) modifier.modify(actual);
                if (branch == null) branch = actual.getRandom().clone();
                check(Arrays.equals(branch, actual.getRandom()), "Preview and play share selected BMS branches");
                var selection = PrimaryScoringSelection.resolve(SecondaryScoringRule.LR2, actual, resource, AbstractGrooveGauge.NORMAL);
                observed.add(selection.effective());
                check(selection.isStandard() == standard, "Cached selection and actual play use the same LN lottery");
                check(PrimaryScoringSelection.scoringInfo(selection.effective(), actual).equals(expected), "Planned notes and maximum match the played chart");
            }
            config.refreshChartScoringPlan(song.getSha256());
            check(config.getChartScoringPlan(song.getSha256()) != plan, "Returning to selection can create the next lottery generation");
        }
        check(observed.contains(SecondaryScoringRule.LR2) && observed.contains(SecondaryScoringRule.BEATORAJA), "Both supported and fallback outcomes were tested");
        System.out.println("ChartScoringPlanTest passed (shared BMS branches, LN lotteries, metadata, fallback and refresh)");
    }
    private static BMSModel decode(Path file) { return ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, null)); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
