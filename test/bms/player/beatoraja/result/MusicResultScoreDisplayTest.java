package bms.player.beatoraja.result;

import java.lang.reflect.Proxy;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.LongArray;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor;
import bms.player.beatoraja.play.BMSPlayerRule;
import bms.player.beatoraja.play.GaugeProperty;
import bms.player.beatoraja.play.GrooveGauge;
import bms.player.beatoraja.skin.property.FloatPropertyFactory;
import bms.player.beatoraja.skin.property.BooleanPropertyFactory;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;

import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;
import static bms.player.beatoraja.skin.SkinProperty.*;
import static bms.player.beatoraja.play.AbstractGrooveGauge.HARD;
import static bms.player.beatoraja.play.AbstractGrooveGauge.NORMAL;

public final class MusicResultScoreDisplayTest {
    public static void main(String[] args) throws Exception {
        var previousInput = Gdx.input;
        try {
            verifyGaugeCycles();
            for (var mode : new Mode[]{Mode.BEAT_5K, Mode.BEAT_7K, Mode.POPN_9K, Mode.BEAT_10K, Mode.BEAT_14K}) {
                verify(mode);
            }
        } finally {
            Gdx.input = previousInput;
        }
        System.out.println("MusicResultScoreDisplayTest passed (F7, graph switching and primary isolation in all supported modes)");
    }

    private static void verifyGaugeCycles() throws Exception {
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        var model = new BMSModel(); model.setMode(Mode.BEAT_7K); model.setTotal(120);
        var gauge = new GrooveGauge(model, NORMAL, GaugeProperty.SEVENKEYS);
        resource.setGrooveGauge(gauge); resource.setGauge(gaugeSamples(gauge, 80));
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        set(main, MainController.class, "resource", resource);
        var result = new MusicResult(main);
        var change = MusicResult.class.getDeclaredMethod("changeGaugeGraph"); change.setAccessible(true);
        var rules = new bms.player.beatoraja.play.SecondaryScoringRule[]{
                bms.player.beatoraja.play.SecondaryScoringRule.LR2,
                bms.player.beatoraja.play.SecondaryScoringRule.PULSUS,
                bms.player.beatoraja.play.SecondaryScoringRule.ANGOLMOIS,
                bms.player.beatoraja.play.SecondaryScoringRule.BEATORAJA};
        int[][] expected = {{3, 5, 1, 2}, {3, 1, 2}, {2}, {3, 4, 5, 0, 1, 2}};
        for (int i = 0; i < rules.length; i++) {
            var rule = rules[i];
            resource.setPrimaryScoringSelection(new bms.player.beatoraja.play.PrimaryScoringSelection(rule, rule, ""));
            result.gaugeType = NORMAL;
            for (int type : expected[i]) {
                change.invoke(result);
                check(result.getGaugeType() == type, "Gauge cycle stays within " + rule + " graphs");
            }
        }
        result.gaugeType = 6;
        change.invoke(result); check(result.getGaugeType() == 7, "beatoraja course gauge cycle is preserved");
        change.invoke(result); check(result.getGaugeType() == 8, "beatoraja course EX-HARD is preserved");
        change.invoke(result); check(result.getGaugeType() == 6, "beatoraja course gauges wrap");
    }

    private static void verify(Mode mode) throws Exception {
        var rule = BMSPlayerRule.getBMSPlayerRule(mode);
        var primary = score(mode, 95, ClearType.Normal, rule);
        var primaryBest = score(mode, 60, ClearType.Easy, rule);
        var secondary = score(mode, 30, ClearType.Failed, BMSPlayerRule.LunaticRave2);
        var secondaryBest = score(mode, 40, ClearType.Normal, BMSPlayerRule.LunaticRave2);
        var replay = new ReplayData();
        var model = new BMSModel();
        model.setSHA256(primary.getSha256());
        model.setMode(mode);
        model.setTotal(120);
        var primaryGauge = new GrooveGauge(model, NORMAL, GaugeProperty.SEVENKEYS);
        var secondaryGauge = new GrooveGauge(model, NORMAL, GaugeProperty.LR2);
        var primaryHistory = gaugeSamples(primaryGauge, 80);
        var secondaryHistory = gaugeSamples(secondaryGauge, 40);
        var timedHistory = new PlayerResource.GaugeHistory(secondaryGauge, secondaryHistory,
                new LongArray(new long[]{0, 1_000_000}), 4_000_000);
        secondary.setGauge(NORMAL);
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        resource.setPlayMode(BMSPlayerMode.PLAY);
        resource.setScoreData(primary);
        resource.setUpdateScore(true);
        resource.setGauge(primaryHistory);
        resource.setGrooveGauge(primaryGauge);
        set(resource, PlayerResource.class, "model", model);
        set(resource, PlayerResource.class, "replay", replay);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        var input = KeyboardQueueTestSupport.input(new PlayerConfig().getPlayConfig(mode).getKeyboardConfig());
        var timer = new TimerManager();
        set(timer, TimerManager.class, "nowmicrotime", 1_000_000L);
        timer.setTimerOn(TIMER_STARTINPUT);
        timer.setTimerOff(TIMER_FADEOUT);
        set(main, MainController.class, "timer", timer);
        set(main, MainController.class, "resource", resource);
        set(main, MainController.class, "input", input);
        var result = new MusicResult(main);
        var skin = KeyboardQueueTestSupport.allocate(TestSkin.class);
        set(result, MainState.class, "skin", skin);
        set(result, MusicResult.class, "property", ResultKeyProperty.get(mode));
        result.oldscore = primaryBest;
        result.gaugeType = NORMAL;
        result.getPrimaryScoreDataProperty().setTargetScore(primaryBest.getExscore(), 170, 100);
        result.getPrimaryScoreDataProperty().update(primary);
        var states = new boolean[256];
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (proxy, method, args) -> "isKeyPressed".equals(method.getName()) && states[(int) args[0]]);
        var keyboard = input.getKeyBoardInputProcesseor();
        keyboard.setShortcutContext(new PlayerConfig(), MainState.MainStateType.RESULT, false);

        var rank = BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_AAA_1P);
        var score = IntegerPropertyFactory.getIntegerProperty(NUMBER_SCORE);
        var clear = IntegerPropertyFactory.getIntegerProperty(NUMBER_CLEAR);
        var fail = BooleanPropertyFactory.getBooleanProperty(OPTION_RESULT_FAIL);
        result.prepareScoreDisplay();
        press(result, keyboard, states);
        check(!result.isShowingLr2Score() && rank.isStatic(result), "unavailable without a secondary result");
        var lr2 = new PlayerResource.AlternateResultScore("lr2", "Lunatic Rave 2", secondary, secondaryBest, timedHistory);
        int[] standardArea = {-15, 15, -30, 30, -60, 60, -100, 100, -150, 150};
        int[] lr2Area = {-18, 18, -40, 40, -100, 100, -200, 200, -200, 1000};
        resource.setScoringTiming("beatoraja", new PlayerResource.TimingData(new int[]{-12, -12, 6}, standardArea));
        resource.setScoringTiming("lr2", new PlayerResource.TimingData(new int[]{20, 40}, lr2Area));
        resource.setAlternateResultScores(List.of(lr2));
        result.prepareScoreDisplay();
        var graph = new SkinGaugeGraphObject();
        var timingGraph = new bms.player.beatoraja.skin.SkinTimingDistributionGraph(301, 1,
                "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", "FFFFFFFF", 1, 1);
        timingGraph.setDestination(0, 0, 0, 301, 100, 0, 255, 255, 255, 255, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0);
        timingGraph.prepare(0, result);
        check(result.getTimingDistribution().getAverage() == -6 && java.util.Arrays.equals(result.getDisplayTimingJudgeArea(), standardArea),
                "Initial timing distribution and judge area belong to primary");
        check(FloatPropertyFactory.getFloatProperty(374).get(result) == -6, "Float timing mean uses signed primary samples in ms");
        var timingField = timingGraph.getClass().getDeclaredField("distribution"); timingField.setAccessible(true);
        var initialDistribution = timingField.get(timingGraph);
        var gaugeNumber = FloatPropertyFactory.getFloatProperty("groovegauge_1p");
        check(graph.updateGraphData(result) && gaugeNumber.get(result) == 80, "primary gauge data");
        check(score.get(result) == 190 && rank.get(result) && !rank.isStatic(result),
                "primary is initially shown, rank destinations remain dynamic");
        press(result, keyboard, states);
        timingGraph.prepare(0, result);
        check(result.getTimingDistribution().getAverage() == 30 && java.util.Arrays.equals(result.getDisplayTimingJudgeArea(), lr2Area)
                        && timingField.get(timingGraph) != initialDistribution,
                "F7 switches signed distribution, statistics and judge-area graph cache");
        check(FloatPropertyFactory.getFloatProperty("timing_average").get(result) == 30, "Float timing mean follows F7 samples in ms");
        check(result.getDisplayGaugeHistory() == timedHistory && result.getGaugeType() == NORMAL
                        && gaugeNumber.get(result) == 40 && graph.updateGraphData(result) && !graph.updateGraphData(result),
                "F7 invalidates the graph even when both views use NORMAL");
        checkPartialPixels(graph);
        input.setKeyState(5, true, 5);
        result.input();
        input.setKeyState(5, false, 6);
        check(result.getGaugeType() == HARD && graph.updateGraphData(result) && gaugeNumber.get(result) == 41,
                "existing CHANGE_GRAPH key cycles the LR2 gauge histories");
        check(result.isShowingLr2Score() && score.get(result) == 60 && !rank.get(result)
                        && result.getJudgeCount(0, true) == 30 && clear.get(result) == ClearType.Failed.id
                        && fail.get(result) && result.getDisplayOldScore() == secondaryBest
                        && result.getScoreDataProperty().getBestScore() == 80
                        && result.getDisplayScore().getPlaymode() == mode,
                "F7 switches skin score, rank, judgments, clear and previous best");
        check(result.getNewScore() == primary && result.getOldScore() == primaryBest
                        && resource.getReplayData() == replay && resource.getScoreData() == primary && resource.isUpdateScore(),
                "IR, replay and primary score state are not swapped");
        result.input();
        check(result.isShowingLr2Score(), "holding F7 does not toggle twice");
        skin.editing = true;
        press(result, keyboard, states);
        skin.editing = false;
        result.input();
        check(result.isShowingLr2Score(), "text input blocks F7 and discards its pending edge");
        timer.setTimerOn(TIMER_FADEOUT);
        press(result, keyboard, states);
        timer.setTimerOff(TIMER_FADEOUT);
        result.input();
        check(result.isShowingLr2Score(), "fadeout blocks F7 and discards its pending edge");
        press(result, keyboard, states);
        check(!result.isShowingLr2Score() && score.get(result) == 190 && rank.get(result),
                "a second press restores the primary view");
        timingGraph.prepare(0, result);
        check(timingField.get(timingGraph) == initialDistribution && result.getTimingDistribution().getAverage() == -6,
                "F7 restores primary timing graph without mixing samples");
        check(result.getGaugeType() == NORMAL && graph.updateGraphData(result) && gaugeNumber.get(result) == 80
                        && resource.getGrooveGauge() == primaryGauge && resource.getGauge() == primaryHistory,
                "primary graph and gauge remain untouched");
        press(result, keyboard, states);
        check(result.getGaugeType() == HARD && graph.updateGraphData(result), "LR2 gauge selection is retained");

        var third = score(mode, 42, ClearType.Easy, rule);
        third.setGauge(NORMAL);
        var thirdHistory = new PlayerResource.GaugeHistory(secondaryGauge, gaugeSamples(secondaryGauge, 50),
                new LongArray(new long[]{0, 500_000}), 4_000_000);
        var stale = score(mode, 60, ClearType.Normal, rule);
        stale.setSha256("2".repeat(64));
        resource.setAlternateResultScores(List.of(lr2,
                new PlayerResource.AlternateResultScore("stale", "Stale", stale, null, null),
                new PlayerResource.AlternateResultScore("fixture", "Fixture", third, null, thirdHistory)));
        result.prepareScoreDisplay();
        press(result, keyboard, states);
        check(result.isShowingLr2Score() && result.getDisplayGaugeHistory() == timedHistory, "First alternate is LR2");
        input.setKeyState(5, true, 7);
        result.input();
        input.setKeyState(5, false, 8);
        press(result, keyboard, states);
        check(result.isShowingSecondaryScore() && !result.isShowingLr2Score()
                && result.getDisplayScore() == third && result.getGaugeType() == NORMAL
                && result.getDisplayGaugeHistory() == thirdHistory, "Skip stale scores, and use the next rule's gauge and score");
        press(result, keyboard, states);
        check(!result.isShowingSecondaryScore() && result.getDisplayScore() == primary, "Wrap to primary");
        press(result, keyboard, states);
        check(result.getGaugeType() == HARD, "Alternate gauge selection stays independent across multiple rules");

        var selectedPrimary = score(mode, 35, ClearType.Normal, BMSPlayerRule.LunaticRave2);
        selectedPrimary.setAvgjudge(12000);
        primary.setAvgjudge(23000);
        primary.setGauge(NORMAL);
        resource.setPrimaryScoringSelection(new bms.player.beatoraja.play.PrimaryScoringSelection(
                bms.player.beatoraja.play.SecondaryScoringRule.LR2, bms.player.beatoraja.play.SecondaryScoringRule.LR2, ""));
        resource.setScoreData(selectedPrimary);
        third.setNativeScore(1234); third.setNativeMaxScore(2000);
        third.setAvgjudge(6000); third.setNotes(120);
        third.setGauge(0);
        var nativeBest = score(mode, 80, ClearType.Easy, BMSPlayerRule.Angolmois);
        nativeBest.setNativeScore(1200); nativeBest.setNativeMaxScore(2000);
        resource.setAlternateResultScores(List.of(lr2,
                new PlayerResource.AlternateResultScore("beatoraja", "beatoraja", primary, primaryBest,
                        new PlayerResource.GaugeHistory(primaryGauge, primaryHistory, new LongArray(new long[]{0, 1_000_000}), 4_000_000)),
                new PlayerResource.AlternateResultScore("angolmois", "Angolmois", third, nativeBest, thirdHistory)));
        result.prepareScoreDisplay();
        check(result.isShowingLr2Score() && result.getDisplayScore() == selectedPrimary
                        && result.getDisplayScoringSummary().startsWith("Primary: Lunatic Rave 2"), "Configured primary is the initial result");
        check(result.getTimingDistribution().getAverage() == 30 && java.util.Arrays.equals(result.getDisplayTimingJudgeArea(), lr2Area),
                "Non-beatoraja primary receives its timing distribution despite the separate save path");
        check(result.getAverageDuration() == 12000, "Absolute duration follows native primary");
        check(bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1011).get(result).equals("Lunatic Rave 2")
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1012).get(result).equals("Lunatic Rave 2"),
                "Initial result strings expose its actual primary rule");
        press(result, keyboard, states);
        check(result.getDisplayScore() == primary && result.getDisplayScoringRuleName().equals("beatoraja")
                        && result.getDisplayOldScore() == primaryBest && result.getDisplayGaugeValue() == 80,
                "F7 exposes independently calculated beatoraja with its own best and gauge");
        check(result.getTimingDistribution().getAverage() == -6 && java.util.Arrays.equals(result.getDisplayTimingJudgeArea(), standardArea),
                "F7 from a native primary shows the independent beatoraja timing distribution and windows");
        check(result.getAverageDuration() == 23000, "F7 absolute duration uses independent beatoraja statistics");
        check(bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1011).get(result).equals("Lunatic Rave 2")
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1012).get(result).equals("beatoraja")
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1013).get(result).equals("Lunatic Rave 2"),
                "F7 changes only the displayed-rule name and leaves primary/configured names fixed");
        press(result, keyboard, states);
        check(result.getDisplayScoringSummary().contains("SCORE 1234 / 2000"), "Native score and maximum remain visible");
        check(result.getDisplayScoringSummary().contains(" / NORMAL") && !result.getDisplayScoringSummary().contains("ASSIST EASY"),
                "Angolmois internal gauge slot zero is labeled as native NORMAL");
        check(IntegerPropertyFactory.getImageIndexProperty(40).get(result) == NORMAL
                && !BooleanPropertyFactory.getBooleanProperty(1046).get(result), "Native gauge skin properties do not signal ASSIST EASY or EX gauge");
        check(score.get(result) == 1234 && IntegerPropertyFactory.getIntegerProperty(NUMBER_MAXSCORE).get(result) == 2000
                && result.getScoreDataProperty().getRateInt() == 61
                && BooleanPropertyFactory.getBooleanProperty(OPTION_UPDATE_SCORE).get(result)
                && IntegerPropertyFactory.getIntegerProperty(NUMBER_TOTALNOTES).get(result) == 120
                && IntegerPropertyFactory.getIntegerProperty(372).get(result) == 6
                && IntegerPropertyFactory.getIntegerProperty(NUMBER_TARGET_SCORE).get(result) == Integer.MIN_VALUE,
                "F7 native display uses native score/rate/update, scoring notes, own duration and hides incompatible targets");
        press(result, keyboard, states);
        check(result.getDisplayScore() == selectedPrimary, "No duplicate selected primary in F7 cycle");
        var constructor = Class.forName("bms.player.beatoraja.play.AngolmoisGrooveGauge").getDeclaredConstructor();
        constructor.setAccessible(true);
        var angGauge = (bms.player.beatoraja.play.AbstractGrooveGauge) constructor.newInstance();
        angGauge.setValue(35);
        resource.setGrooveGauge(angGauge);
        resource.setGauge(new FloatArray[]{FloatArray.with(35), FloatArray.with(35), FloatArray.with(35)});
        resource.setScoreData(third);
        resource.setPrimaryScoringSelection(new bms.player.beatoraja.play.PrimaryScoringSelection(
                bms.player.beatoraja.play.SecondaryScoringRule.ANGOLMOIS, bms.player.beatoraja.play.SecondaryScoringRule.ANGOLMOIS, ""));
        primaryGauge.setType(HARD);
        resource.setAlternateResultScores(List.of(new PlayerResource.AlternateResultScore("beatoraja", "beatoraja", primary, primaryBest,
                new PlayerResource.GaugeHistory(primaryGauge, primaryHistory, new LongArray(new long[]{0, 1}), 1))));
        result.gaugeType = NORMAL;
        result.prepareScoreDisplay();
        var particles = new bms.player.beatoraja.play.SkinGauge(new com.badlogic.gdx.graphics.g2d.TextureRegion[][]{
                new com.badlogic.gdx.graphics.g2d.TextureRegion[36]}, 0, 0, 50, 0, 0, 100);
        particles.setDestination(0, 0, 0, 100, 20, 0, 255, 255, 255, 255, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0);
        particles.prepare(1000, result);
        var valueField = particles.getClass().getDeclaredField("value"); valueField.setAccessible(true);
        var borderField = particles.getClass().getDeclaredField("border"); borderField.setAccessible(true);
        check(valueField.getFloat(particles) == 35, "Gauge particles initially use Angolmois samples");
        input.setKeyState(5, true, 10); result.input(); input.setKeyState(5, false, 11);
        check(result.getGaugeType() == NORMAL, "Angolmois exposes only its actual NORMAL gauge");
        press(result, keyboard, states);
        particles.prepare(1000, result);
        check(result.getGaugeType() == HARD && valueField.getFloat(particles) == 81 && borderField.getFloat(particles) == 0,
                "F7 gauge particles switch to beatoraja HARD history and border without indexing the Angolmois bank");
        press(result, keyboard, states);
        particles.prepare(1000, result);
        check(valueField.getFloat(particles) == 35 && result.getGaugeType() == NORMAL, "Gauge particles restore native primary");
        resource.setGrooveGauge(primaryGauge); primaryGauge.setType(NORMAL); resource.setGauge(primaryHistory);
        resource.setPrimaryScoringSelection(new bms.player.beatoraja.play.PrimaryScoringSelection(
                bms.player.beatoraja.play.SecondaryScoringRule.LR2, bms.player.beatoraja.play.SecondaryScoringRule.BEATORAJA, "Unsupported gauge"));
        resource.setScoreData(primary);
        result.prepareScoreDisplay();
        check(result.getDisplayScoringSummary().startsWith("Primary: beatoraja")
                        && result.getPrimaryScoringDetails().contains("Unsupported gauge"), "Show effective fallback and retain requested-rule details");
        check(bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty("scoring_rule").get(result)
                        .equals(result.getDisplayScoringSummary()), "Result skin scoring label");
        check(bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1011).get(result).equals("beatoraja")
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1013).get(result).equals("Lunatic Rave 2")
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1014).get(result).equals(result.getDisplayScoringSummary())
                && bms.player.beatoraja.skin.property.StringPropertyFactory.getStringProperty(1015).get(result).equals(result.getPrimaryScoringDetails()),
                "Fallback and relocated summary/details strings preserve their distinct meanings");
        resource.setPrimaryScoringSelection(bms.player.beatoraja.play.PrimaryScoringSelection.standard());

        set(resource, PlayerResource.class, "course", new BMSModel[]{model});
        result.prepareScoreDisplay();
        press(result, keyboard, states);
        check(!result.canSwitchScoreDisplay() && !result.isShowingLr2Score(), "a course is not switchable");
        set(resource, PlayerResource.class, "course", null);
        model.setSHA256("3".repeat(64));
        result.prepareScoreDisplay();
        check(!result.canSwitchScoreDisplay(), "a result belonging to another chart is ignored");
    }

    private static FloatArray[] gaugeSamples(GrooveGauge gauge, float normalValue) {
        var samples = new FloatArray[gauge.getGaugeTypeLength()];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = new FloatArray(new float[]{20, normalValue + i - NORMAL});
        }
        return samples;
    }

    private static void checkPartialPixels(SkinGaugeGraphObject graph) {
        GdxNativesLoader.load();
        var shape = new Pixmap(201, 100, Pixmap.Format.RGBA8888);
        try {
            graph.drawTimedHistory(shape);
            check((shape.getPixel(50, 38) & 0xff) != 0, "the terminal sample is drawn at one quarter of the song");
            for (int x = 51; x < shape.getWidth(); x++) {
                for (int y = 0; y < shape.getHeight(); y++) {
                    check((shape.getPixel(x, y) & 0xff) == 0, "no graph line beyond the terminal sample");
                }
            }
        } finally {
            shape.dispose();
        }
    }

    private static void press(MusicResult result, KeyBoardInputProcesseor keyboard, boolean[] states) {
        states[Keys.F7] = false;
        keyboard.poll(1);
        states[Keys.F7] = true;
        keyboard.poll(2);
        result.input();
    }

    private static ScoreData score(Mode mode, int pg, ClearType clear, BMSPlayerRule rule) {
        var score = new ScoreData(mode);
        score.setSha256("1".repeat(64));
        score.setNotes(100);
        score.setEpg(pg);
        score.setClear(clear.id);
        score.setRule(rule);
        return score;
    }

    private static final class TestSkin extends MusicResultSkin {
        boolean editing;
        TestSkin() { super(null); }
        @Override public boolean isTextInputActive() { return editing; }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
