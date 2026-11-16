package bms.player.beatoraja.play;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.LongArray;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.Mode;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.PlayerResource.GaugeHistory;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.play.SecondaryJudgingGroup.Judgment;
import bms.player.beatoraja.play.SecondaryJudgingGroup.Outcome;

/** In-memory, silent run. The judge thread owns it until it joins. */
final class SecondaryJudging implements SecondaryJudgingGroup.Run {
	private static final long GAUGE_INTERVAL_US = 500_000;

	private final AbstractJudgeManager judge;
	private final AbstractGrooveGauge gauge;
	private final PlayerConfig config;
	private final long endUs;
	private final FloatArray[] gaugeSamples;
	private final LongArray gaugeTimesUs = new LongArray();
	private long lastProcessedUs;
	private final int[] legacyKeys;
	private final long[] legacyTimes;
	private final boolean[] legacyPressed;
	private int legacySize;
	private Outcome outcome = Outcome.RUNNING;

	static SecondaryJudging create(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs) {
		var config = resource.getPlayerConfig();
		int startGauge = LunaticRave2GrooveGauge.mapBeatorajaGauge(config.getGauge());
		if (BMSPlayerRule.select(BMSPlayerRule.LunaticRave2, model,
				resource.getOriginalMode(), resource.getPlayMode(), resource.getCourseBMSModels() != null,
				startGauge, resource.getChartRuleParameters()) != BMSPlayerRule.LunaticRave2) {
			return null;
		}
		return new SecondaryJudging(player, model, resource, endUs, startGauge, BMSPlayerRule.LunaticRave2);
	}

	static SecondaryJudging createTimingProfile(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs,
			SecondaryScoringRule rule) {
		if (resource.getPlayMode() == null || resource.getPlayMode().mode != BMSPlayerMode.Mode.PLAY
				|| resource.getCourseBMSModels() != null) return null;
		return new SecondaryJudging(player, model, resource, endUs, resource.getPlayerConfig().getGauge(), rule.scoreRule());
	}

	private SecondaryJudging(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs, int startGauge,
			BMSPlayerRule rule) {
		config = resource.getPlayerConfig();
		this.endUs = endUs;
		var chart = copyChart(model);
		if (rule == BMSPlayerRule.LunaticRave2) {
			gauge = new LunaticRave2GrooveGauge(chart, startGauge, resource);
			judge = new LunaticRave2JudgeManager(player, gauge);
		} else {
			var pulsusRules = rule == BMSPlayerRule.Pulsus && model.getMode() == Mode.BEAT_7K
					&& resource.getOriginalMode() == Mode.BEAT_7K
					? PulsusNoteRules.forRank(model.getJudgeType().bmsRank()) : null;
			gauge = pulsusRules != null
					? new PulsusGrooveGauge(model, startGauge, resource.getChartRuleParameters())
					: new GrooveGauge(chart, startGauge, BMSPlayerRule.getBMSPlayerRule(chart.getMode()).gauge);
			judge = new JudgeManager(player, gauge, rule, pulsusRules);
		}
		gaugeSamples = new FloatArray[gauge.getGaugeTypeLength()];
		for (int i = 0; i < gaugeSamples.length; i++) gaugeSamples[i] = new FloatArray();
		judge.init(chart, resource);
		int keys = player.getLaneProperty().getKeyLaneAssign().length;
		legacyKeys = new int[keys];
		legacyTimes = new long[keys];
		legacyPressed = new boolean[keys];
	}

	public boolean isRunning() {
		return outcome == Outcome.RUNNING;
	}

	AbstractGrooveGauge gauge() {
		return gauge;
	}

	public void addLegacyEvent(int key, long time, boolean pressed) {
		legacyKeys[legacySize] = key;
		legacyTimes[legacySize] = time;
		legacyPressed[legacySize++] = pressed;
	}

	public void advance(long nowUs, JudgeEventQueue.Batch batch, boolean overflow) {
		try {
			if (overflow) {
				outcome = Outcome.INPUT_LOST;
				return;
			}
			if (gaugeTimesUs.size == 0) sampleGauge(0);
			long processedUs = nowUs;
			if (judge instanceof LunaticRave2JudgeManager lr2) {
				lr2.processCapturedInput(nowUs, batch, legacyKeys, legacyTimes, legacyPressed, legacySize);
				processedUs = lr2.processedThroughUs(nowUs);
			} else if (judge instanceof JudgeManager timing) {
				timing.processCapturedInput(nowUs, batch, legacyKeys, legacyTimes, legacyPressed, legacySize);
			}
			if (BMSPlayer.updateGaugeAutoShift(gauge, config, gauge.isFailed())) {
				outcome = Outcome.FAILED;
			} else if (nowUs >= endUs) {
				outcome = Outcome.FINISHED;
			}
			lastProcessedUs = Math.min(endUs, Math.max(lastProcessedUs, processedUs));
			if (!isRunning() || lastProcessedUs - gaugeTimesUs.peek() >= GAUGE_INTERVAL_US) {
				sampleGauge(lastProcessedUs);
			}
		} catch (RuntimeException e) {
			outcome = Outcome.ERROR;
			Logger.getGlobal().log(Level.WARNING,
					judge.getScoreData().getRule() + " secondary judging stopped", e);
		} finally {
			legacySize = 0;
		}
	}

	Judgment finish() {
		return finish(false);
	}

	public Judgment finish(boolean primaryFinished) {
		var score = judge.getScoreData();
		int passed = judge.getPastNotes();
		if (isRunning()) {
			// A manual exit can skip the post-song margin after all judgments are settled.
			outcome = primaryFinished && score.getNotes() > 0 && passed == score.getNotes()
					? Outcome.FINISHED : Outcome.INTERRUPTED;
		}
		sampleGauge(lastProcessedUs);
		var counts = new ArrayList<Integer>(12);
		for (int i = 0; i < 6; i++) {
			counts.add(judge.getJudgeCount(i, true));
			counts.add(judge.getJudgeCount(i, false));
		}
		int minbp = judge.getJudgeCount(3) + judge.getJudgeCount(4) + judge.getJudgeCount(5)
				+ score.getNotes() - passed;
		return new Judgment(outcome, passed, score.getExscore(), score.getCombo(),
				List.copyOf(counts), gauge.getType(), gauge.getValue(), score.getNotes(), minbp, clearType(),
				new GaugeHistory(gauge, gaugeSamples, gaugeTimesUs, endUs));
	}

	private void sampleGauge(long timeUs) {
		if (gaugeTimesUs.size > 0 && gaugeTimesUs.peek() == timeUs) {
			for (int i = 0; i < gaugeSamples.length; i++) {
				gaugeSamples[i].set(gaugeTimesUs.size - 1, gauge.getValue(i));
			}
		} else {
			gaugeTimesUs.add(timeUs);
			for (int i = 0; i < gaugeSamples.length; i++) gaugeSamples[i].add(gauge.getValue(i));
		}
	}

	private ClearType clearType() {
		if (outcome != Outcome.FINISHED) return ClearType.Failed;
		var comboClear = judge.getCompletedComboClear();
		if (comboClear != null) return comboClear;
		if (!gauge.isQualified()) return ClearType.Failed;
		if (judge instanceof JudgeManager && judge.getPastNotes() > 0 && judge.getPastNotes() == judge.getCombo()
				&& judge.getPastNotes() == judge.getScoreData().getNotes()) {
			if (judge.getJudgeCount(2) != 0) return ClearType.FullCombo;
			return judge.getJudgeCount(1) == 0 ? ClearType.Max : ClearType.Perfect;
		}
		return gauge.getClearType();
	}

	private static BMSModel copyChart(BMSModel source) {
		var model = new BMSModel();
		model.setMode(source.getMode());
		model.setChartInformation(source.getChartInformation());
		model.setJudgerank(source.getJudgerank());
		model.setTotal(source.getTotal());
		model.setSHA256(source.getSHA256());
		var paired = new IdentityHashMap<Note, Note>();
		var timelines = new ArrayList<TimeLine>();
		for (var original : source.getAllTimeLines()) {
			TimeLine copy = null;
			for (int lane = 0; lane < source.getMode().key; lane++) {
				Note note = original.getNote(lane);
				if (note == null) continue;
				if (copy == null) copy = new TimeLine(original.getSection(), original.getMicroTime(), source.getMode().key);
				var replica = paired.remove(note);
				if (replica == null) {
					replica = (Note) note.clone();
					if (note instanceof LongNote ln) paired.put(ln.getPair(), ((LongNote) replica).getPair());
				}
				copy.setNote(lane, replica);
			}
			if (copy != null) timelines.add(copy);
		}
		model.setAllTimeLine(timelines.toArray(TimeLine[]::new));
		return model;
	}
}
