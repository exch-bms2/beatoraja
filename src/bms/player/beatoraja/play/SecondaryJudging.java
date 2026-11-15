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
import bms.player.beatoraja.PlayDataAccessor.ScoreAttempt;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.PlayerResource.GaugeHistory;
import bms.player.beatoraja.input.JudgeEventQueue;

/** In-memory, silent 7KEY LR2 run. The judge thread owns it until it joins. */
final class SecondaryJudging {
	private static final long GAUGE_INTERVAL_US = 500_000;
	enum Outcome { RUNNING, FINISHED, FAILED, INTERRUPTED, INPUT_LOST, ERROR }

	record Result(Outcome outcome, int passed, int exScore, int maxCombo,
			List<Integer> judgeCounts, int gaugeType, float gaugeValue, int notes, int minbp, ClearType clear,
			GaugeHistory gaugeHistory) {
		ScoreAttempt toLocalAttempt(BMSModel model) {
			int mode = model.containsUndefinedLongNote() ? model.getLntype() : 0;
			var localOutcome = switch (outcome) {
				case FINISHED -> ScoreOutcome.FINISHED;
				case FAILED -> ScoreOutcome.FAILED;
				case INTERRUPTED -> ScoreOutcome.INTERRUPTED;
				case INPUT_LOST -> ScoreOutcome.INPUT_LOST;
				case ERROR -> ScoreOutcome.ERROR;
				case RUNNING -> throw new IllegalStateException("Cannot save a running judgment");
			};
			return new ScoreAttempt(model.getSHA256(), mode, localOutcome,
					notes, passed, maxCombo, minbp, clear, gaugeType, gaugeValue, judgeCounts);
		}
	}

	private final LunaticRave2JudgeManager judge;
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
		if (model.getMode() != Mode.BEAT_7K || BMSPlayerRule.select(BMSPlayerRule.LunaticRave2, model,
				resource.getOriginalMode(), resource.getPlayMode(), resource.getCourseBMSModels() != null,
				config.getGauge(), resource.getChartRuleParameters()) != BMSPlayerRule.LunaticRave2) {
			return null;
		}
		return new SecondaryJudging(player, model, resource, endUs);
	}

	private SecondaryJudging(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs) {
		config = resource.getPlayerConfig();
		this.endUs = endUs;
		var chart = copyChart(model);
		gauge = new LunaticRave2GrooveGauge(chart, config.getGauge(), resource);
		gaugeSamples = new FloatArray[gauge.getGaugeTypeLength()];
		for (int i = 0; i < gaugeSamples.length; i++) gaugeSamples[i] = new FloatArray();
		judge = new LunaticRave2JudgeManager(player, gauge);
		judge.init(chart, resource);
		int keys = player.getLaneProperty().getKeyLaneAssign().length;
		legacyKeys = new int[keys];
		legacyTimes = new long[keys];
		legacyPressed = new boolean[keys];
	}

	boolean isRunning() {
		return outcome == Outcome.RUNNING;
	}

	AbstractGrooveGauge gauge() {
		return gauge;
	}

	void addLegacyEvent(int key, long time, boolean pressed) {
		legacyKeys[legacySize] = key;
		legacyTimes[legacySize] = time;
		legacyPressed[legacySize++] = pressed;
	}

	void advance(long nowUs, JudgeEventQueue.Batch batch, boolean overflow) {
		try {
			if (overflow) {
				outcome = Outcome.INPUT_LOST;
				return;
			}
			if (gaugeTimesUs.size == 0) sampleGauge(0);
			judge.processFifoEvents(nowUs, batch, null, null);
			for (int i = 0; i < legacySize; i++) {
				judge.processKeyEvent(legacyKeys[i], legacyTimes[i], legacyPressed[i], nowUs);
			}
			judge.finishTime(nowUs);
			if (BMSPlayer.updateGaugeAutoShift(gauge, config, gauge.isFailed())) {
				outcome = Outcome.FAILED;
			} else if (nowUs >= endUs) {
				outcome = Outcome.FINISHED;
			}
			lastProcessedUs = Math.min(endUs, Math.max(lastProcessedUs, nowUs));
			if (!isRunning() || lastProcessedUs - gaugeTimesUs.peek() >= GAUGE_INTERVAL_US) {
				sampleGauge(lastProcessedUs);
			}
		} catch (RuntimeException e) {
			outcome = Outcome.ERROR;
			Logger.getGlobal().log(Level.WARNING, "LR2 secondary judging stopped", e);
		} finally {
			legacySize = 0;
		}
	}

	Result finish() {
		if (isRunning()) outcome = Outcome.INTERRUPTED;
		sampleGauge(lastProcessedUs);
		var counts = new ArrayList<Integer>(12);
		for (int i = 0; i < 6; i++) {
			counts.add(judge.getJudgeCount(i, true));
			counts.add(judge.getJudgeCount(i, false));
		}
		var score = judge.getScoreData();
		int passed = judge.getPastNotes();
		int minbp = judge.getJudgeCount(3) + judge.getJudgeCount(4) + judge.getJudgeCount(5)
				+ score.getNotes() - passed;
		return new Result(outcome, passed, score.getExscore(), score.getCombo(),
				List.copyOf(counts), gauge.getType(), gauge.getValue(), score.getNotes(), minbp, clearType(passed),
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

	private ClearType clearType(int passed) {
		if (outcome != Outcome.FINISHED || !gauge.isQualified()) return ClearType.Failed;
		if (passed > 0 && passed == judge.getCombo()) {
			if (judge.getJudgeCount(2) > 0) return ClearType.FullCombo;
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
