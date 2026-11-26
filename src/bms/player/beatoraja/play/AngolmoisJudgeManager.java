package bms.player.beatoraja.play;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;

/** Live presentation of the same fixed-frame engine used by the Angolmois secondary. */
final class AngolmoisJudgeManager extends AbstractJudgeManager implements AngolmoisEngine.Listener {
    private AngolmoisEngine engine;
    private LaneState[] lanes;
    private long offsetUs;

    AngolmoisJudgeManager(BMSPlayer player) { super(player); }
    @Override public int[] getTimingJudgeArea() { return AngolmoisEngine.judgeArea(); }
    @Override BMSPlayerRule ruleFor(Mode mode) { return BMSPlayerRule.Angolmois; }
    @Override JudgeAlgorithm judgeAlgorithm(PlayerResource resource) { return JudgeAlgorithm.Lowest; }
    @Override int scoringNoteCount(BMSModel model) {
        try { engine = AngolmoisChart.create(model); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Angolmois chart", e); }
        return engine.notes;
    }
    @Override LaneState[] initLanes(LaneProperty property, Lane[] models) {
        lanes = new LaneState[models.length];
        for (int i = 0; i < lanes.length; i++) lanes[i] = new LaneState(i, property, models[i]);
        return lanes;
    }
    @Override void initRule(BMSModel model, PlayerResource resource, JudgeProperty rule, int rank, int[] keys, int[] scratch) {
        getScoreData().setNativeScore(0);
        getScoreData().setNativeMaxScore(engine.maxScore);
        getScoreData().setRuleProgressMaxima(engine.progressMaxima);
        offsetUs = engine.originDelayUs - resource.getMarginTime() * 1000;
        engine.listener = this;
    }
    @Override void processInput(long time, BMSPlayerInputProcessor input, JudgeEventQueue queue) {
        for (int i = 0; i < eventBatch.size; i++) {
            int key = eventBatch.keys[i];
            queue.apply(key, eventBatch.pressed[i]);
            processKeyEvent(key, eventBatch.times[i], eventBatch.pressed[i], time);
            input.clearQueuedInputTime(key, eventBatch.times[i], eventBatch.pressed[i]);
        }
        for (int key = 0; key < keyassign.length; key++) {
            if (queue.isQueued(key) || input.getKeyChangedTime(key) == Long.MIN_VALUE) continue;
            processKeyEvent(key, input.getKeyChangedTime(key), input.getKeyState(key), time);
            input.resetKeyChangedTime(key);
        }
        finishTime(time);
    }
    @Override void processKeyEvent(int key, long inputUs, boolean down, long time) {
        if (key >= 0 && key < keyassign.length && keyassign[key] >= 0) {
            int lane = keyassign[key];
            int mapped = lane == 7 && key == keyassign.length - 1 ? 8 : lane;
            engine.offer(mapped, down, inputUs + offsetUs);
        }
    }
    @Override void finishTime(long time) {
        engine.advance(time + offsetUs);
        ((AngolmoisGrooveGauge) gauge()).sync(engine);
        getScoreData().setNativeScore(engine.score);
        getScoreData().setNativeMaxScore(engine.maxScore);
        getScoreData().setCombo(engine.maxCombo);
        prevmtime = time;
    }
    @Override boolean keepsCombo(Note note, int judge) { return judge <= 2; }
    @Override boolean incrementsCombo(Note note, int judge) { return judge < 2; }
    @Override ClearType getCompletedComboClear() { return gauge().isQualified() ? ClearType.Normal : ClearType.Failed; }
    @Override public void judge(AngolmoisEngine.ChartObject object, int grade, double signedMs, long frameUs, boolean vanish) {
        if (object.note == null) return;
        getScoreData().setNativeScore(engine.score);
        var lane = lanes[object.lane];
        if (object.type == AngolmoisEngine.HEAD && grade != 0) lane.processing = (LongNote) object.note;
        if (grade == 0) lane.processing = null;
        updateMicro(lane, object.note, Math.max(0, frameUs - offsetUs), 4 - grade, (long) (signedMs * 1000), vanish);
    }
    @Override public void release(AngolmoisEngine.ChartObject object) {
        if (object.note != null) object.note.setState(1);
        lanes[object.lane].processing = null;
    }
    @Override public void sound(AngolmoisEngine.ChartObject object) {
        if (object.note != null) keysound.play(object.note, main.resource.getConfig().getAudioConfig().getKeyvolume(), 0);
    }
}
