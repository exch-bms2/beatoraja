package bms.player.beatoraja.skin.lua;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.result.*;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.skin.property.IntegerPropertyFactory;
import com.badlogic.gdx.utils.FloatArray;
import org.luaj.vm2.LuaTable;
import java.util.List;
import static bms.player.beatoraja.input.KeyboardQueueTestSupport.set;

public final class ScoringLuaApiTest {
    public static void main(String[] args) throws Exception {
        var model = new BMSModel(); model.setMode(Mode.BEAT_7K); model.setTotal(120); model.setSHA256("1".repeat(64));
        var nativeScore = new ScoreData(Mode.BEAT_7K); nativeScore.setSha256(model.getSHA256()); nativeScore.setNotes(2);
        nativeScore.setNativeScore(700); nativeScore.setNativeMaxScore(750);
        var standard = new ScoreData(Mode.BEAT_7K); standard.setSha256(model.getSHA256()); standard.setNotes(2); standard.setEpg(1);
        var primaryGauge = new GrooveGauge(model, AbstractGrooveGauge.NORMAL, GaugeProperty.SEVENKEYS);
        var alternateGauge = new GrooveGauge(model, AbstractGrooveGauge.HARD, GaugeProperty.SEVENKEYS);
        var primaryHistory = samples(primaryGauge, 30);
        var resource = KeyboardQueueTestSupport.allocate(PlayerResource.class);
        set(resource, PlayerResource.class, "model", model);
        resource.setScoreData(nativeScore); resource.setPlayMode(BMSPlayerMode.PLAY);
        resource.setPrimaryScoringSelection(new PrimaryScoringSelection(SecondaryScoringRule.ANGOLMOIS, SecondaryScoringRule.ANGOLMOIS, ""));
        resource.setGrooveGauge(primaryGauge); resource.setGauge(primaryHistory);
        var target = new ScoreData(); target.setOption(3); resource.setTargetScoreData(target);
        resource.setAlternateResultScores(List.of(new PlayerResource.AlternateResultScore("beatoraja", "beatoraja", standard, null,
                new PlayerResource.GaugeHistory(alternateGauge, samples(alternateGauge, 90), null, 0))));
        var main = KeyboardQueueTestSupport.allocate(MainController.class); set(main, MainController.class, "resource", resource);
        var result = new MusicResult(main);
        set(result, AbstractResult.class, "gaugeType", AbstractGrooveGauge.NORMAL);
        result.getPrimaryScoreDataProperty().setTargetScore(600, null, 750, null, 2, 750);
        result.getPrimaryScoreDataProperty().update(nativeScore);
        var prepare = MusicResult.class.getDeclaredMethod("prepareScoreDisplay"); prepare.setAccessible(true); prepare.invoke(result);
        var api = new LuaTable(); new MainStatePropertyLuaApiExporter(result).export(api);
        check(api.get("gauge").call().todouble() == 30 && api.get("gauge_type").call().toint() == 2, "Lua result gauge follows native primary");
        check(api.get("exscore").call().toint() == 700 && api.get("exscore_rival").call().toint() == 750
                && IntegerPropertyFactory.getImageIndexProperty(61).get(result) == 3, "Compatible primary target remains visible");
        set(result, MusicResult.class, "scoreDisplayIndex", 1);
        check(api.get("gauge").call().todouble() == 90 && api.get("gauge_type").call().toint() == 3
                && api.get("exscore").call().toint() == 2, "Lua result gauge and score follow F7");
        check(api.get("rate_rival").call().isnil() && api.get("exscore_rival").call().isnil()
                && IntegerPropertyFactory.getImageIndexProperty(61).get(result) == Integer.MIN_VALUE
                && IntegerPropertyFactory.getImageIndexProperty(62).get(result) == Integer.MIN_VALUE
                && IntegerPropertyFactory.getImageIndexProperty(63).get(result) == Integer.MIN_VALUE,
                "Incompatible target is absent from Lua and all target option images");
        set(result, MusicResult.class, "scoreDisplayIndex", 0);
        check(api.get("gauge").call().todouble() == 30 && api.get("exscore_rival").call().toint() == 750, "Returning to primary restores Lua values");
        System.out.println("ScoringLuaApiTest passed (native/F7 gauge, score and incompatible target visibility)");
    }
    private static FloatArray[] samples(AbstractGrooveGauge gauge, float value) {
        var values = new FloatArray[gauge.getGaugeTypeLength()];
        for (int i = 0; i < values.length; i++) values[i] = FloatArray.with(value);
        return values;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
