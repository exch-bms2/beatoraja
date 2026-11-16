package bms.player.beatoraja.play;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.player.beatoraja.ClearType;
import bms.player.beatoraja.PlayDataAccessor.ScoreAttempt;
import bms.player.beatoraja.PlayDataAccessor.ScoreOutcome;
import bms.player.beatoraja.PlayerResource;
import bms.player.beatoraja.PlayerResource.GaugeHistory;
import bms.player.beatoraja.input.JudgeEventQueue;

/** Fan out a single input snapshot without sharing mutable charts, gauges or scores between rules. */
final class SecondaryJudgingGroup {
	enum Outcome { RUNNING, FINISHED, FAILED, INTERRUPTED, INPUT_LOST, ERROR }

	record Judgment(Outcome outcome, int passed, int exScore, int maxCombo,
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

	interface Run {
		boolean isRunning();
		void addLegacyEvent(int key, long time, boolean pressed);
		void advance(long nowUs, JudgeEventQueue.Batch batch, boolean overflow);
		Judgment finish(boolean primaryFinished);
	}

	record Entry(SecondaryScoringRule rule, Run run) {}
	record Result(SecondaryScoringRule rule, Judgment judgment) {}

	private final Entry[] entries;
	private final boolean[] broken;

	SecondaryJudgingGroup(List<Entry> entries) {
		this.entries = entries.toArray(Entry[]::new);
		this.broken = new boolean[this.entries.length];
	}

	static SecondaryJudgingGroup create(BMSPlayer player, BMSModel model, PlayerResource resource, long endUs) {
		var ids = resource.getPlayerConfig().getSecondaryScoringRuleIds();
		if (ids.length == 0) return null;
		var entries = new ArrayList<Entry>(ids.length);
		for (String id : ids) {
			var rule = SecondaryScoringRule.byId(id);
			if (rule == null) continue;
			try {
				Run run = switch (rule) {
					case LR2 -> SecondaryJudging.create(player, model, resource, endUs);
					case PULSUS, RAINDROP, BEMUSE -> SecondaryJudging.createTimingProfile(player, model, resource, endUs, rule);
				};
				if (run != null) entries.add(new Entry(rule, run));
			} catch (RuntimeException e) {
				logFailure(rule, "initialization", e);
			}
		}
		return entries.isEmpty() ? null : new SecondaryJudgingGroup(entries);
	}

	boolean isRunning() {
		for (int i = 0; i < entries.length; i++) if (isRunning(i)) return true;
		return false;
	}

	private boolean isRunning(int i) {
		return !broken[i] && entries[i].run().isRunning();
	}

	void addLegacyEvent(int key, long time, boolean pressed) {
		for (int i = 0; i < entries.length; i++) {
			if (!isRunning(i)) continue;
			try {
				entries[i].run().addLegacyEvent(key, time, pressed);
			} catch (RuntimeException e) {
				broken[i] = true;
				logFailure(entries[i].rule(), "input capture", e);
			}
		}
	}

	void advance(long nowUs, JudgeEventQueue.Batch batch, boolean overflow) {
		for (int i = 0; i < entries.length; i++) {
			if (!isRunning(i)) continue;
			try {
				entries[i].run().advance(nowUs, batch, overflow);
			} catch (RuntimeException e) {
				broken[i] = true;
				logFailure(entries[i].rule(), "judgment", e);
			}
		}
	}

	List<Result> finish(boolean primaryFinished) {
		var results = new ArrayList<Result>(entries.length);
		for (int i = 0; i < entries.length; i++) {
			if (broken[i]) continue;
			var entry = entries[i];
			try {
				results.add(new Result(entry.rule(), entry.run().finish(primaryFinished)));
			} catch (RuntimeException e) {
				logFailure(entry.rule(), "result finalization", e);
			}
		}
		return results;
	}

	private static void logFailure(SecondaryScoringRule rule, String phase, RuntimeException e) {
		Logger.getGlobal().log(Level.WARNING, rule.displayName() + " secondary scoring failed during " + phase, e);
	}
}
