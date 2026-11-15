package bms.player.beatoraja.result;

import java.lang.reflect.Proxy;

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
            for (var mode : new Mode[]{Mode.BEAT_5K, Mode.BEAT_7K, Mode.POPN_9K, Mode.BEAT_10K, Mode.BEAT_14K}) {
                verify(mode);
            }
        } finally {
            Gdx.input = previousInput;
        }
        System.out.println("MusicResultScoreDisplayTest passed (F7, graph switching and primary isolation in all supported modes)");
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
        resource.setAlternateResultScore(new PlayerResource.AlternateResultScore(secondary, secondaryBest, timedHistory));
        result.prepareScoreDisplay();
        var graph = new SkinGaugeGraphObject();
        var gaugeNumber = FloatPropertyFactory.getFloatProperty("groovegauge_1p");
        check(graph.updateGraphData(result) && gaugeNumber.get(result) == 80, "primary gauge data");
        check(score.get(result) == 190 && rank.get(result) && !rank.isStatic(result),
                "primary is initially shown, rank destinations remain dynamic");
        press(result, keyboard, states);
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
        check(result.getGaugeType() == NORMAL && graph.updateGraphData(result) && gaugeNumber.get(result) == 80
                        && resource.getGrooveGauge() == primaryGauge && resource.getGauge() == primaryHistory,
                "primary graph and gauge remain untouched");
        press(result, keyboard, states);
        check(result.getGaugeType() == HARD && graph.updateGraphData(result), "LR2 gauge selection is retained");

        set(resource, PlayerResource.class, "course", new BMSModel[]{model});
        result.prepareScoreDisplay();
        press(result, keyboard, states);
        check(!result.canSwitchScoreDisplay() && !result.isShowingLr2Score(), "a course is not switchable");
        set(resource, PlayerResource.class, "course", null);
        model.setSHA256("2".repeat(64));
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
