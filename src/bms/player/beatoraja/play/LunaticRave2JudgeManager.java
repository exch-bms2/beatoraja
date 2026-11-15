package bms.player.beatoraja.play;

import java.util.Arrays;
import java.util.Comparator;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.input.JudgeEventQueue.Batch;

/** The experimental LunaticRave2 judgment engine for eligible single-song play. */
final class LunaticRave2JudgeManager extends AbstractJudgeManager {
	private static final int DP_EVENT_BITS = 14;
	private static final int DP_EVENT_MASK = (1 << DP_EVENT_BITS) - 1;
	private Lr2Lane[] lr2DpLanes;
	private long[] lr2DpEvents;
	private long[] lr2LegacyTimes;
	private boolean[] lr2LegacyPressed;
	private volatile long lr2DpProcessedMs;
	private volatile boolean lr2DpFailed;
	private Lr2Lane[] states;
	private LunaticRave2Rules rules;

	LunaticRave2JudgeManager(BMSPlayer main) {
		super(main);
	}

	LunaticRave2JudgeManager(BMSPlayer main, AbstractGrooveGauge scoringGauge) {
		super(main, scoringGauge);
	}

	@Override
	BMSPlayerRule ruleFor(Mode mode) {
		return BMSPlayerRule.LunaticRave2;
	}

	@Override
	JudgeAlgorithm judgeAlgorithm(PlayerResource resource) {
		return JudgeAlgorithm.Lowest;
	}

	@Override
	LaneState[] initLanes(LaneProperty property, Lane[] lanes) {
		states = new Lr2Lane[lanes.length];
		for (int i = 0; i < lanes.length; i++) states[i] = new Lr2Lane(i, property, lanes[i]);
		return states;
	}

	@Override
	void initRule(BMSModel model, PlayerResource resource, JudgeProperty rule,
			int judgerank, int[] keyRates, int[] scratchRates) {
		rules = new LunaticRave2Rules(resource.getChartRuleParameters(), model.getTotalNotes());
		lr2DpLanes = null;
		lr2DpProcessedMs = -1;
		lr2DpFailed = false;
		if (model.getMode().player == 2) {
			lr2DpLanes = states.clone();
			Arrays.sort(lr2DpLanes, Comparator.comparingInt(lane -> lane.player * 10 + lane.offset));
			lr2DpEvents = new long[eventBatch.keys.length + keyassign.length];
			lr2LegacyTimes = new long[keyassign.length];
			lr2LegacyPressed = new boolean[keyassign.length];
		}
	}

	@Override
	void processInput(long mtime, BMSPlayerInputProcessor input, JudgeEventQueue queue) {
		if (lr2DpLanes == null) {
			super.processInput(mtime, input, queue);
			return;
		}
		if (lr2DpFailed) return;
		long nowMs = mtime / 1_000;
		int count = packDpQueue(nowMs, eventBatch);
		for (int key = 0; key < keyassign.length; key++) {
			if (queue.isQueued(key) || keyassign[key] < 0) continue;
			long time = input.getKeyChangedTime(key);
			if (time == Long.MIN_VALUE) continue;
			lr2DpEvents[count++] = packDpLegacy(nowMs, key, time, input.getKeyState(key));
		}
		processDpBatch(nowMs, eventBatch, count, input, queue);
	}

	void processCapturedInput(long mtime, Batch batch, int[] keys, long[] times, boolean[] pressed, int size) {
		if (lr2DpLanes == null) {
			processFifoEvents(mtime, batch, null, null);
			for (int i = 0; i < size; i++) processKeyEvent(keys[i], times[i], pressed[i], mtime);
			finishTime(mtime);
			return;
		}
		if (lr2DpFailed) return;
		long nowMs = mtime / 1_000;
		int count = packDpQueue(nowMs, batch);
		for (int i = 0; i < size; i++) {
			int key = keys[i];
			if (keyassign[key] >= 0) lr2DpEvents[count++] = packDpLegacy(nowMs, key, times[i], pressed[i]);
		}
		processDpBatch(nowMs, batch, count, null, null);
	}

	long processedThroughUs(long mtime) {
		return lr2DpFailed ? lr2DpProcessedMs * 1_000 : mtime;
	}

	@Override
	ClearType getCompletedComboClear() {
		var score = getScoreData();
		if (score.getNotes() <= 0 || score.getCombo() != score.getNotes()) return null;
		if (!gauge().isQualified() || score.getJudgeCount(2) > 0) return ClearType.FullCombo;
		return score.getJudgeCount(1) == 0 ? ClearType.Max : ClearType.Perfect;
	}

	@Override
	void beforeFifoCheckpoint(long checkpoint) {
		if (checkpoint / 1_000 > prevmtime / 1_000) {
			// A later release must not erase a mine already crossed while held.
			finishLunaticRave2Time(checkpoint / 1_000 - 1, checkpoint);
		}
	}

	@Override
	void processKeyEvent(int key, long pmtime, boolean pressed, long mtime) {
		if (key >= 0 && key < keyassign.length && keyassign[key] >= 0) {
			processLunaticRave2Event(states[keyassign[key]], key, pressed, pmtime, mtime,
					main.resource.getConfig().getAudioConfig().getKeyvolume());
		}
	}

	@Override
	void finishTime(long mtime) {
		finishLunaticRave2Time(mtime / 1_000, mtime);
	}

	@Override
	PlayEnd playEnd(long playMs, long endMs) {
		if (lr2DpLanes == null) return PlayEnd.UNSYNCHRONIZED;
		long processed = lr2DpProcessedMs;
		if (lr2DpFailed) return PlayEnd.FAILED;
		return playMs > endMs && processed > endMs ? PlayEnd.FINISHED : PlayEnd.PLAYING;
	}

	private int packDpQueue(long nowMs, Batch batch) {
		int count = 0;
		// Pack the chart millisecond and source index to retain FIFO order within each lane.
		for (int i = 0; i < batch.size; i++) {
			int key = batch.keys[i];
			if (key >= keyassign.length || keyassign[key] < 0) continue;
			lr2DpEvents[count++] = (Math.min(nowMs, batch.times[i] / 1_000) << DP_EVENT_BITS) | i;
		}
		return count;
	}

	private long packDpLegacy(long nowMs, int key, long time, boolean pressed) {
		lr2LegacyTimes[key] = time;
		lr2LegacyPressed[key] = pressed;
		return (Math.min(nowMs, time / 1_000) << DP_EVENT_BITS) | (eventBatch.keys.length + key);
	}

	private void processDpBatch(long nowMs, Batch batch, int count, BMSPlayerInputProcessor input, JudgeEventQueue queue) {
		Arrays.sort(lr2DpEvents, 0, count);
		float volume = main.resource.getConfig().getAudioConfig().getKeyvolume();
		for (int from = 0; from < count && !lr2DpFailed;) {
			long tick = lr2DpEvents[from] >> DP_EVENT_BITS;
			int to = from + 1;
			while (to < count && (lr2DpEvents[to] >> DP_EVENT_BITS) == tick) to++;
			advanceLunaticRave2Dp(tick - 1, volume);
			if (lr2DpFailed) return;
			for (Lr2Lane lane : lr2DpLanes) {
				for (int i = from; i < to; i++) {
					int index = (int) lr2DpEvents[i] & DP_EVENT_MASK;
					boolean queued = index < batch.keys.length;
					int key = queued ? batch.keys[index] : index - batch.keys.length;
					if (keyassign[key] != lane.lane) continue;
					boolean pressed = queued ? batch.pressed[index] : lr2LegacyPressed[key];
					if (queued && queue != null) queue.apply(key, pressed);
					processLunaticRave2Event(lane, key, pressed, tick * 1_000, tick * 1_000, volume);
					if (input != null) input.clearQueuedInputTime(key, queued ? batch.times[index] : lr2LegacyTimes[key], pressed);
				}
				finishLunaticRave2Lane(lane, tick, tick * 1_000, volume);
			}
			completeLunaticRave2DpTick(tick);
			from = to;
		}
		advanceLunaticRave2Dp(nowMs, volume);
	}

	private void advanceLunaticRave2Dp(long throughMs, float volume) {
		while (!lr2DpFailed && lr2DpProcessedMs < throughMs) {
			long nextMs = Long.MAX_VALUE;
			for (Lr2Lane lane : lr2DpLanes) {
				if (lane.processing != null) {
					nextMs = Math.min(nextMs, lane.processing.getMicroTime() / 1_000 + 1);
					continue;
				}
				Note[] notes = lane.lanemodel.getNotes();
				if (lane.nextLunaticRave2Note < notes.length) {
					Note note = notes[lane.nextLunaticRave2Note];
					long due = note instanceof MineNote ? note.getMicroTime() / 1_000 + 1
							: rules.missAtMs(note.getMicroTime(), note instanceof LongNote);
					nextMs = Math.min(nextMs, due);
				}
			}
			if (nextMs > throughMs) {
				lr2DpProcessedMs = throughMs;
				return;
			}
			for (Lr2Lane lane : lr2DpLanes) finishLunaticRave2Lane(lane, nextMs, nextMs * 1_000, volume);
			completeLunaticRave2DpTick(nextMs);
		}
	}

	private void completeLunaticRave2DpTick(long tick) {
		// Only latch a terminal failure when auto shift cannot keep the song running.
		lr2DpFailed = main.resource.getPlayerConfig().getGaugeAutoShift() == PlayerConfig.GAUGEAUTOSHIFT_NONE
				&& gauge().isFailed();
		lr2DpProcessedMs = Math.max(lr2DpProcessedMs, tick);
	}
	private void processLunaticRave2Event(Lr2Lane state, int key, boolean pressed, long inputUs, long nowUs, float volume) {
		int direction = key == state.laneassign[0] ? 1 : 2;
		boolean wasHeld = (state.lr2Held & direction) != 0;
		if (pressed) state.lr2Held |= direction;
		else state.lr2Held &= ~direction;
		if (state.sckey >= 0) {
			// Both scratch directions feed one lane. Resolve a same-timestamp handoff before releasing the LN.
			if (state.releasetime != Long.MIN_VALUE && state.releasetime != inputUs) {
				releaseLunaticRave2LongNote(state, state.releasetime / 1_000, nowUs);
			}
			if (pressed) {
				state.releasetime = Long.MIN_VALUE;
			} else {
				if (wasHeld && state.lr2Held == 0 && state.processing != null) state.releasetime = inputUs;
			}
		}
		long inputMs = inputUs / 1_000;
		if (pressed) {
			boolean wasProcessing = state.processing != null;
			if (wasProcessing && inputMs > state.processing.getMicroTime() / 1_000) {
				finishLunaticRave2LongNote(state, nowUs, false);
			}
			if (state.processing == null) {
				if (!wasProcessing || state.nextLunaticRave2Note < state.lanemodel.getNotes().length) {
					processLunaticRave2Press(state, inputMs, nowUs, volume);
				}
			}
			if (!isHeadless()) main.getKeyinput().inputKeyOn(state.lane);
		} else if (state.sckey < 0 && state.processing != null) {
			releaseLunaticRave2LongNote(state, inputMs, nowUs);
		}
	}

	private void processLunaticRave2Press(Lr2Lane state, long inputMs, long nowUs, float volume) {
		Note[] notes = state.lanemodel.getNotes();
		if (state.nextLunaticRave2Note < notes.length) {
			do {
				Note note = notes[state.nextLunaticRave2Note];
				long noteUs = note.getMicroTime();
				if (note instanceof MineNote mine) {
					if (rules.isMinePress(inputMs, noteUs)) hitLunaticRave2Mine(state, mine, volume);
					return;
				}
				long earlyMs = noteUs / 1_000 - inputMs;
				if (rules.isLate(inputMs, noteUs, note instanceof LongNote)) {
					advanceLunaticRave2Note(state);
					state.lastLunaticRave2Keysound = note;
					updateMicro(state, note, nowUs, 4, noteUs - inputMs * 1_000, true);
					return;
				}
				int result = rules.judge(earlyMs);
				if (result < 0) {
					playKeySound(state.lastLunaticRave2Keysound, volume);
					return;
				}
				boolean consumed = result < 4;
				state.lastLunaticRave2Keysound = note;
				playKeySound(note, volume);
				if (consumed && note instanceof LongNote ln) {
					state.processing = ln.getPair();
					state.lnstartJudge = result;
					state.lnstartDuration = earlyMs * 1_000;
					setLaneJudgement(state, 8);
					if (!isHeadless()) main.timer.setTimerOn(state.timerHold);
					return;
				}
				if (consumed) advanceLunaticRave2Note(state);
				updateMicro(state, note, nowUs, result, earlyMs * 1_000, consumed);
				if (result != 3) return;
			} while (state.nextLunaticRave2Note < notes.length && LunaticRave2Rules.canChainBad(
					notes[state.nextLunaticRave2Note].getMicroTime() / 1_000 - inputMs));
			return;
		}
		playKeySound(state.lastLunaticRave2Keysound, volume);
	}

	private void playKeySound(Note note, float volume) {
		if (!isHeadless() && note != null) keysound.play(note, volume, 0);
	}

	private static void advanceLunaticRave2Note(Lr2Lane state) {
		Note[] notes = state.lanemodel.getNotes();
		do {
			state.nextLunaticRave2Note++;
		} while (state.nextLunaticRave2Note < notes.length
				&& notes[state.nextLunaticRave2Note] instanceof LongNote ln && ln.isEnd());
	}

	private void releaseLunaticRave2LongNote(Lr2Lane state, long releaseMs, long nowUs) {
		finishLunaticRave2LongNote(state, nowUs,
				rules.isEarlyLongNoteRelease(state.processing.getMicroTime() / 1_000, releaseMs));
	}

	private void finishLunaticRave2LongNote(Lr2Lane state, long nowUs, boolean earlyRelease) {
		LongNote head = state.processing.getPair();
		state.processing = null;
		state.releasetime = Long.MIN_VALUE;
		if (!isHeadless()) main.timer.setTimerOff(state.timerHold);
		advanceLunaticRave2Note(state);
		if (earlyRelease && !isHeadless()) keysound.stop(head);
		updateMicro(state, head, nowUs, earlyRelease ? 3 : state.lnstartJudge, state.lnstartDuration, true);
	}

	private void hitLunaticRave2Mine(Lr2Lane state, MineNote mine, float volume) {
		advanceLunaticRave2Note(state);
		mine.setState(1);
		// One POOR without consuming a scorable note, breaking combo, or applying the empty-POOR gauge delta.
		getScoreData().addJudgeCount(5, true, 1);
		gauge().applyMine(mine.getDamage());
		playKeySound(mine, volume);
	}

	private void finishLunaticRave2Time(long nowMs, long mtime) {
		float volume = main.resource.getConfig().getAudioConfig().getKeyvolume();
		for (Lr2Lane state : states) {
			finishLunaticRave2Lane(state, nowMs, mtime, volume);
		}
	}

	private void finishLunaticRave2Lane(Lr2Lane state, long nowMs, long mtime, float volume) {
		if (state.processing != null) {
			if (state.releasetime != Long.MIN_VALUE) {
				releaseLunaticRave2LongNote(state, state.releasetime / 1_000, mtime);
			} else if (nowMs > state.processing.getMicroTime() / 1_000) {
				finishLunaticRave2LongNote(state, mtime, false);
			} else {
				return;
			}
		}
		Note[] notes = state.lanemodel.getNotes();
		while (state.nextLunaticRave2Note < notes.length) {
			Note note = notes[state.nextLunaticRave2Note];
			long noteUs = note.getMicroTime();
			if (note instanceof MineNote mine) {
				if (nowMs * 1_000 <= noteUs) break;
				if (state.lr2Held != 0) hitLunaticRave2Mine(state, mine, volume);
				else advanceLunaticRave2Note(state);
				continue;
			}
			if (!rules.isLate(nowMs, noteUs, note instanceof LongNote)) break;
			advanceLunaticRave2Note(state);
			state.lastLunaticRave2Keysound = note;
			updateMicro(state, note, mtime, 4, noteUs - nowMs * 1_000, true);
		}
	}
	private static final class Lr2Lane extends LaneState {
		int nextLunaticRave2Note;
		int lr2Held;
		Note lastLunaticRave2Keysound;

		Lr2Lane(int lane, LaneProperty property, Lane lanemodel) {
			super(lane, property, lanemodel);
		}
	}
}
