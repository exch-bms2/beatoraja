package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import com.badlogic.gdx.utils.Json;
import java.nio.file.*;
import java.util.*;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

/** Exercises the play/replay modifier hook, JSON persistence and the real judge engines. */
public final class ChartModifierReplayTest {
    public static void main(String[] args) throws Exception {
        var file = Files.createTempFile("chart-modifier-replay-", ".bme");
        try {
            Files.writeString(file, "#PLAYER 1\n#BPM 120\n#RANK 2\n#TOTAL 400\n#WAV01 a.wav\n"
                    + "#RANDOM 2\n#IF 1\n#00119:01000100\n#ENDIF\n#IF 2\n#00119:00010001\n#ENDIF\n#ENDRANDOM\n"
                    + "#00219:01000100\n#00319:01\n#00101:00010001\n#00401:01\n");
            for (int lnMode : new int[]{2, 3, 5}) for (long seed : new long[]{0, 4096}) {
                var config = new PlayerConfig(); config.setLongnoteMode(lnMode); config.setLongnoteRate(.5);
                config.setScrollMode(2); config.setScrollSection(1); config.setScrollRate(.7);
                config.setMineMode(2); config.setExtranoteDepth(1); config.setExtranoteScratch(true);
                var plan = new ChartScoringPlan(seed);
                var original = decode(file, null);
                var parameters = BMSPlayerRule.ChartParameters.fromBms(original);
                var plans = new java.util.concurrent.ConcurrentHashMap<String, ChartScoringPlan>();
                plans.put(original.getSHA256(), plan); set(config, PlayerConfig.class, "chartScoringPlans", plans);
                var info = apply(original, config, null, BMSPlayerMode.PLAY);
                check(info.chartModifiers.seed == seed, "Play records the shared plan seed");
                info.rand = original.getRandom();
                var json = new Json();
                var saved = json.fromJson(ReplayData.class, json.toJson(info));
                check(saved.chartModifiers != null && saved.chartModifiers.seed == seed, "Replay JSON retains modifier settings and seed");
                config.setLongnoteMode(0); config.setScrollMode(0); config.setMineMode(0); config.setExtranoteDepth(0);
                var reproduced = decode(file, saved.rand);
                apply(reproduced, config, saved, BMSPlayerMode.REPLAY_1);
                check(fingerprint(original).equals(fingerprint(reproduced)), "Replay restores notes, LN types, mines and scroll without current options");
                check(config.getLongnoteMode() == 0 && config.getMineMode() == 0 && config.getScrollMode() == 0
                        && config.getExtranoteDepth() == 0, "Playback does not overwrite the user profile");
                for (var requested : new SecondaryScoringRule[]{SecondaryScoringRule.LR2, SecondaryScoringRule.PULSUS, SecondaryScoringRule.ANGOLMOIS}) {
                    var context = KeyboardQueueTestSupport.allocate(PlayerResource.class);
                    set(context, PlayerResource.class, "orgmode", Mode.BEAT_7K);
                    set(context, PlayerResource.class, "chartRuleParameters", parameters);
                    context.setPlayMode(BMSPlayerMode.PLAY);
                    var selection = PrimaryScoringSelection.resolve(requested, original, context, AbstractGrooveGauge.NORMAL);
                    saved.primaryScoringRuleId = selection.effective().id(); saved.requestedPrimaryScoringRuleId = requested.id();
                    saved.primaryScoringFallbackReason = selection.reason(); saved.gauge = AbstractGrooveGauge.NORMAL;
                    saved.scoringFrameUs = selection.effective() == SecondaryScoringRule.ANGOLMOIS ? AngolmoisEngine.FRAME_US : 0;
                    context.setPlayMode(BMSPlayerMode.REPLAY_1);
                    check(PrimaryScoringSelection.resolveReplay(saved, reproduced, context).equals(selection), "Replay restores effective rule and fallback");
                    var a = decode(file, saved.rand);
                    var b = decode(file, saved.rand); apply(b, config, saved, BMSPlayerMode.REPLAY_1);
                    // Judge the original modifier settings independently of the replay path.
                    for (var modifier : ChartScoringPlan.modifiers(info.chartModifiers)) modifier.modify(a);
                    check(judge(a, selection, parameters).equals(judge(b, selection, parameters)),
                            "Recorded inputs reproduce judgment counts, native/EX score, combo and all gauges for " + requested);
                }
                var legacy = json.fromJson(ReplayData.class, "{}");
                var unchanged = decode(file, saved.rand); var before = fingerprint(unchanged);
                config.setLongnoteMode(3); config.setLongnoteRate(1);
                apply(unchanged, config, legacy, BMSPlayerMode.REPLAY_1);
                check(before.equals(fingerprint(unchanged)), "Legacy replays keep their original modifier behavior");
            }
        } finally { Files.deleteIfExists(file); }
        System.out.println("ChartModifierReplayTest passed (JSON, branch/modifier restoration, profile isolation, real judge scores/gauges and legacy replays)");
    }

    private static ReplayData apply(BMSModel model, PlayerConfig config, ReplayData replay, BMSPlayerMode mode) throws Exception {
        var player = KeyboardQueueTestSupport.allocate(BMSPlayer.class); var info = new ReplayData();
        set(player, BMSPlayer.class, "model", model); set(player, BMSPlayer.class, "playinfo", info);
        set(player, BMSPlayer.class, "replay", replay);
        var apply = BMSPlayer.class.getDeclaredMethod("applyChartModifiers", BMSPlayerMode.class, PlayerConfig.class);
        apply.setAccessible(true); apply.invoke(player, mode, config);
        return info;
    }

    private static BMSModel decode(Path file, int[] randoms) {
        var model = ChartDecoder.getDecoder(file).decode(new ChartInformation(file, 0, randoms));
        BMSModelUtils.setStartNoteTime(model, 1000); BMSPlayerRule.validate(model); return model;
    }

    private static String fingerprint(BMSModel model) {
        var value = new StringBuilder();
        for (var tl : model.getAllTimeLines()) {
            value.append(tl.getMicroTime()).append(':').append(tl.getScroll()).append(':');
            for (int lane = 0; lane < model.getMode().key; lane++) {
                var note = tl.getNote(lane);
                value.append(note == null ? "-" : note.getClass().getSimpleName() + ":" + note.getWav());
                if (note instanceof LongNote ln) value.append(':').append(ln.getType()).append(':').append(ln.isEnd()).append(':').append(ln.getPair().getMicroTime());
                if (note instanceof MineNote mine) value.append(':').append(mine.getDamage());
                value.append(',');
            }
        }
        return value.toString();
    }

    private static List<Number> judge(BMSModel model, PrimaryScoringSelection selection, BMSPlayerRule.ChartParameters parameters) throws Exception {
        var fixture = new KeyboardJudgeCompatibilityTest.Fixture(model, true, selection.isStandard()
                ? BMSPlayerRule.Beatoraja_7 : selection.effective().scoreRule(), parameters, AbstractGrooveGauge.NORMAL);
        for (long time = 1_000_000; time <= 10_000_000; time += 100_000) {
            if (time % 500_000 == 0) fixture.input.setKeyState(6, time % 1_000_000 == 0, time);
            fixture.judge.update(time + 1);
        }
        var score = fixture.judge.getScoreData(); var values = new ArrayList<Number>();
        values.add(score.getRuleScore()); values.add(score.getRuleMaxScore()); values.add(score.getCombo()); values.add(score.getNotes());
        for (int j = 0; j < 6; j++) { values.add(score.getJudgeCount(j, true)); values.add(score.getJudgeCount(j, false)); }
        for (int i = 0; i < fixture.gauge.getGaugeTypeLength(); i++) values.add(fixture.gauge.getValue(i));
        return values;
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
