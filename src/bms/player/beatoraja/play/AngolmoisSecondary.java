package bms.player.beatoraja.play;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.play.SecondaryJudgingGroup.Judgment;
import bms.player.beatoraja.play.SecondaryJudgingGroup.Outcome;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.LongArray;
import java.io.IOException;
import java.util.ArrayList;

/** Silent single-song C Angolmois scoring, clocked independently of render/judge callback cadence. */
final class AngolmoisSecondary implements SecondaryJudgingGroup.Run {
    private final AngolmoisEngine engine;
    private final long endUs, offsetUs;
    private final int[] lanes;
    private final FloatArray values = new FloatArray();
    private final LongArray times = new LongArray();
    private Outcome outcome = Outcome.RUNNING;
    private long lastUs;

    static AngolmoisSecondary create(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs) {
        if (model.getMode() != Mode.BEAT_7K || resource.getOriginalMode() != Mode.BEAT_7K
                || resource.getPlayMode() == null || resource.getPlayMode().mode != BMSPlayerMode.Mode.PLAY
                || resource.getCourseBMSModels() != null) return null;
        try {
            var engine = AngolmoisChart.create(model);
            if (engine.notes == 0) return null;
            return new AngolmoisSecondary(engine, endUs, engine.originDelayUs - resource.getMarginTime() * 1_000,
                    player.getLaneProperty().getKeyLaneAssign());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read Angolmois chart", e);
        }
    }

    AngolmoisSecondary(AngolmoisEngine engine, long endUs, long offsetUs, int[] lanes) {
        this.engine = engine;
        this.endUs = endUs;
        this.offsetUs = offsetUs;
        this.lanes = lanes.clone();
        sample(0);
    }

    @Override public boolean isRunning() { return outcome == Outcome.RUNNING; }

    @Override public void addLegacyEvent(int key, long time, boolean pressed) { offer(key, time, pressed); }

    private void offer(int key, long time, boolean pressed) {
        if (!isRunning() || key < 0 || key >= lanes.length || lanes[key] < 0) return;
        // Retain the two logical scratch directions as separate physical keys sharing lane 7.
        int mapped = lanes[key] == 7 ? (key == lanes.length - 1 ? 8 : 7) : lanes[key];
        engine.offer(mapped, pressed, time + offsetUs);
    }

    @Override public void advance(long nowUs, JudgeEventQueue.Batch batch, boolean overflow) {
        if (!isRunning()) return;
        if (overflow) { outcome = Outcome.INPUT_LOST; return; }
        for (int i = 0; i < batch.size; i++) offer(batch.keys[i], batch.times[i], batch.pressed[i]);
        long limit = Math.min(nowUs, endUs);
        engine.advance(limit + offsetUs);
        lastUs = Math.max(lastUs, limit);
        if (engine.failed) outcome = Outcome.FAILED;
        else if (nowUs >= endUs) outcome = engine.settled() ? Outcome.FINISHED : Outcome.INTERRUPTED;
        if (!isRunning() || lastUs - times.peek() >= 500_000) sample(lastUs);
    }

    private void sample(long us) {
        float value = engine.gauge * 100f / 512;
        if (times.size > 0 && times.peek() == us) values.set(values.size - 1, value);
        else { times.add(us); values.add(value); }
    }

    @Override public Judgment finish(boolean primaryFinished) {
        if (isRunning()) outcome = primaryFinished && engine.settled() ? Outcome.FINISHED : Outcome.INTERRUPTED;
        sample(lastUs);
        var counts = new ArrayList<Integer>(12);
        for (int count : engine.counts) counts.add(count);
        int misses = engine.counts[6] + engine.counts[7] + engine.counts[8] + engine.counts[9];
        var clear = outcome == Outcome.FINISHED && engine.gauge >= 150 ? ClearType.Normal : ClearType.Failed;
        var gauge = new DisplayGauge(engine.gauge, engine.failed);
        return new Judgment(outcome, engine.passed,
                (counts.get(0) + counts.get(1)) * 2 + counts.get(2) + counts.get(3), engine.maxCombo,
                counts, 0, engine.gauge * 100f / 512, engine.notes, misses + engine.notes - engine.passed, clear,
                new PlayerResource.GaugeHistory(gauge, new FloatArray[]{values}, times, endUs), engine.score, engine.maxScore);
    }

    private static final class DisplayGauge extends AbstractGrooveGauge {
        DisplayGauge(int raw, boolean failed) { super(0, new Gauge[]{new Value(raw, failed)}); }
        private static final class Value extends Gauge {
            private final boolean failed;
            Value(int raw, boolean failed) {
                super(GaugeProperty.GaugeElementProperty.ANGOLMOIS, ClearType.Normal);
                value = raw * 100.0 / 512;
                this.failed = failed;
            }
            @Override public void update(int judge, float rate) {}
            @Override boolean isFailed() { return failed; }
        }
    }
}
