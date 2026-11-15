package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import com.badlogic.gdx.utils.FloatArray;

import bms.model.*;
import bms.player.beatoraja.*;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.JudgeEventQueue;
import bms.player.beatoraja.play.JudgeProperty.JudgeWindow;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * ノーツ判定管理用クラス
 *
 * @author exch
 */
public abstract class AbstractJudgeManager {

	final BMSPlayer main;
	final AudioDriver keysound;
	private final AbstractGrooveGauge scoringGauge;
	/**
	 * 現在の判定カウント内訳
	 */
	private ScoreData score = new ScoreData();

	/**
	 * 現在のコンボ数
	 */
	private int combo;
	/**
	 * コース時の現在のコンボ数
	 */
	private int coursecombo;
	/**
	 * コース時の最大コンボ数
	 */
	private int coursemaxcombo;
	/**
	 * 判定連動レーザーの色
	 */
	private int[][] judge;
	/**
	 * 現在表示中の判定
	 */
	private int[] judgenow;
	private int[] judgecombo;
	private static final int SPLIT_JUDGE_REGIONS = 3;
	private final DisplayJudgement[] splitJudgements = new DisplayJudgement[SPLIT_JUDGE_REGIONS * 2];

	public record DisplayJudgement(int rank, int timingMs) {}
	/**
	 * ゴースト記録用の判定ログ
	 */
	private int[] ghostJudge;
	private int[] ghostDuration;
	/**
	 * 判定差時間(ms , +は早押しで-は遅押し)
	 */
	private long[] judgefast;

	private long[] mjudgefast;

	private LaneState[] states;
	int[] keyassign;
	private boolean[] combocond;
	private JudgeWindow window;
	long prevmtime;
	final JudgeEventQueue.Batch eventBatch = new JudgeEventQueue.Batch();
	private long[] auto_presstime;
	/**
	 * 直近100ノーツの判定差時間
	 */
	private long[] recentJudges = new long[100];
	private long[] microrecentJudges = new long[100];
	/**
	 * 判定差時間のヘッド
	 */
	private int recentJudgesIndex = 0;
	AbstractJudgeManager(BMSPlayer main) {
		this(main, null);
	}

	AbstractJudgeManager(BMSPlayer main, AbstractGrooveGauge scoringGauge) {
		this.main = main;
		this.scoringGauge = scoringGauge;
		this.keysound = scoringGauge == null ? main.main.getAudioProcessor() : null;
	}

	final boolean isHeadless() {
		return scoringGauge != null;
	}

	final AbstractGrooveGauge gauge() {
		return isHeadless() ? scoringGauge : main.getGauge();
	}

	static AbstractJudgeManager create(BMSPlayer main, BMSPlayerRule rule) {
		return rule == BMSPlayerRule.LunaticRave2 ? new LunaticRave2JudgeManager(main) : new JudgeManager(main);
	}

	public final void init(BMSModel model, PlayerResource resource) {
		final Mode orgmode = resource.getOriginalMode();
		final BMSPlayerRule playerRule = ruleFor(orgmode);
		prevmtime = 0;
		final int judgeregion = !isHeadless() && main.getSkin() instanceof PlaySkin skin ? skin.getJudgeregion() : 0;
		judgenow = new int[judgeregion];
		judgecombo = new int[judgeregion];
		judgefast = new long[judgeregion];
		mjudgefast = new long[judgeregion];
		Arrays.fill(splitJudgements, null);
		if (!isHeadless()) {
			for (int id = TIMER_JUDGE_1P_KEYS; id <= TIMER_JUDGE_3P_SCRATCH; id++) {
				main.timer.setTimerOff(id);
			}
		}
		score = new ScoreData(orgmode);
		score.setNotes(model.getTotalNotes());
		score.setSha256(model.getSHA256());
		ghostJudge = new int[model.getTotalNotes()];
		ghostDuration = new int[model.getTotalNotes()];
		Arrays.fill(ghostJudge, 4);
		Arrays.fill(ghostDuration, 1000000);

		final JudgeProperty rule = playerRule.judge;
		score.setJudgeAlgorithm(judgeAlgorithm(resource));
		score.setRule(playerRule);
		combocond = rule.combo;
		final LaneProperty lanes = main.getLaneProperty();
		keyassign = lanes.getKeyLaneAssign();
		judge = new int[model.getMode().player][model.getMode().key / model.getMode().player + 1];
		auto_presstime = new long[keyassign.length];
		Arrays.fill(auto_presstime, Long.MIN_VALUE);
		states = initLanes(lanes, model.getLanes());
		final int judgerank = model.getJudgerank();
		final PlayerConfig config = resource.getPlayerConfig();
		final int[] keyJudgeWindowRate = config.isCustomJudge()
				? new int[]{config.getKeyJudgeWindowRatePerfectGreat(), config.getKeyJudgeWindowRateGreat(), config.getKeyJudgeWindowRateGood()}
				: new int[]{100, 100, 100};
		final int[] scratchJudgeWindowRate = config.isCustomJudge()
				? new int[]{config.getScratchJudgeWindowRatePerfectGreat(), config.getScratchJudgeWindowRateGreat(), config.getScratchJudgeWindowRateGood()}
				: new int[]{100, 100, 100};
		for (CourseData.CourseDataConstraint mode : resource.getConstraint()) {
			if (mode == CourseData.CourseDataConstraint.NO_GREAT) {
				keyJudgeWindowRate[1] = keyJudgeWindowRate[2] = 0;
				scratchJudgeWindowRate[1] = scratchJudgeWindowRate[2] = 0;
			} else if (mode == CourseData.CourseDataConstraint.NO_GOOD) {
				keyJudgeWindowRate[2] = 0;
				scratchJudgeWindowRate[2] = 0;
			}
		}

		window = new JudgeWindow(rule, judgerank, keyJudgeWindowRate);
		initRule(model, resource, rule, judgerank, keyJudgeWindowRate, scratchJudgeWindowRate);

		FloatArray[] f = resource.getGauge();
		if (f != null) {
			setCourseCombo(resource.getCombo());
			setCourseMaxcombo(resource.getMaxcombo());
		}

		Arrays.fill(recentJudges, Long.MIN_VALUE);
		this.recentJudgesIndex = 0;
	}
	abstract BMSPlayerRule ruleFor(Mode mode);
	abstract JudgeAlgorithm judgeAlgorithm(PlayerResource resource);
	abstract LaneState[] initLanes(LaneProperty property, Lane[] lanes);
	abstract void initRule(BMSModel model, PlayerResource resource, JudgeProperty rule,
			int judgerank, int[] keyRates, int[] scratchRates);

	public final void update(long mtime) {
		update(mtime, null);
	}

	final void update(long mtime, SecondaryJudging secondary) {
		final BMSPlayerInputProcessor input = main.main.getInputProcessor();
		final JudgeEventQueue queue = input.getJudgeEventQueue();
		queue.drain(eventBatch);
		if (secondary != null && secondary.isRunning() && this instanceof JudgeManager) {
			updateFifo(mtime, input, queue, secondary);
			secondary.advance(mtime, eventBatch, queue.isOverflowed());
		} else {
			processInput(mtime, input, queue);
		}
	}

	void processInput(long mtime, BMSPlayerInputProcessor input, JudgeEventQueue queue) {
		updateFifo(mtime, input, queue);
	}

	final void updateFifo(long mtime, BMSPlayerInputProcessor input, JudgeEventQueue queue) {
		updateFifo(mtime, input, queue, null);
	}

	private void updateFifo(long mtime, BMSPlayerInputProcessor input, JudgeEventQueue queue, SecondaryJudging secondary) {
		processFifoEvents(mtime, eventBatch, input, queue);
		for (int key = 0; key < keyassign.length; key++) {
			if (queue.isQueued(key)) continue;
			long pmtime = input.getKeyChangedTime(key);
			if (pmtime == Long.MIN_VALUE || keyassign[key] == -1) continue;
			boolean pressed = input.getKeyState(key);
			processKeyEvent(key, pmtime, pressed, mtime);
			if (secondary != null) secondary.addLegacyEvent(key, pmtime, pressed);
			input.resetKeyChangedTime(key);
		}
		finishTime(mtime);
	}

	final void processFifoEvents(long mtime, JudgeEventQueue.Batch batch,
			BMSPlayerInputProcessor input, JudgeEventQueue queue) {
		for (int from = 0; from < batch.size;) {
			int to = from + 1;
			while (to < batch.size && batch.times[to] == batch.times[from]) to++;
			if (queue != null) {
				for (int i = from; i < to; i++) queue.apply(batch.keys[i], batch.pressed[i]);
			}
			long checkpoint = Math.min(mtime, Math.max(prevmtime, batch.times[from]));
			beforeFifoCheckpoint(checkpoint);
			advancePassing(checkpoint);
			for (int i = from; i < to; i++) {
				processKeyEvent(batch.keys[i], batch.times[i], batch.pressed[i], checkpoint);
				if (input != null) input.clearQueuedInputTime(batch.keys[i], batch.times[i], batch.pressed[i]);
			}
			finishTime(checkpoint);
			from = to;
		}
		advancePassing(mtime);
	}
	void beforeFifoCheckpoint(long checkpoint) {}

	void advancePassing(long mtime) {
		prevmtime = mtime;
	}

	abstract void processKeyEvent(int key, long pmtime, boolean pressed, long mtime);
	abstract void finishTime(long mtime);

	enum PlayEnd { UNSYNCHRONIZED, PLAYING, FAILED, FINISHED }

	PlayEnd playEnd(long playMs, long endMs) {
		return PlayEnd.UNSYNCHRONIZED;
	}

	final void setLaneJudgement(LaneState state, int value) {
		judge[state.player][state.offset] = value;
	}

	final void setAutoPresstime(int key, long time) {
		auto_presstime[key] = time;
	}

	boolean suppressMiss(Note note, int judgement) {
		return false;
	}

	private final int[] JUDGE_TIMER = { TIMER_JUDGE_1P, TIMER_JUDGE_2P, TIMER_JUDGE_3P };
	private final int[] COMBO_TIMER = { TIMER_COMBO_1P, TIMER_COMBO_2P, TIMER_COMBO_3P };

	final void updateMicro(LaneState state, Note n, long mtime, int judge, long mfast, boolean judgeVanish) {
		if (judgeVanish) {
			if (score.getPassnotes() < ghostJudge.length) {
				ghostJudge[score.getPassnotes()] = judge;
				ghostDuration[score.getPassnotes()] = (int) Math.abs(mfast);
			}
			n.setState(judge + 1);
			score.setPassnotes(score.getPassnotes() + 1);
		}
		if (suppressMiss(n, judge)) {
			return;
		}
		n.setMicroPlayTime(mfast);
		score.addJudgeCount(judge, mfast >= 0, 1);

		if (judge < 4) {
			recentJudgesIndex = (recentJudgesIndex + 1) % recentJudges.length;
			recentJudges[recentJudgesIndex] = mfast / 1000;
			microrecentJudges[recentJudgesIndex] = mfast;
		}

		if (combocond[judge] && judge < 5) {
			combo++;
			score.setCombo(Math.max(score.getCombo(), combo));
			coursecombo++;
			coursemaxcombo = coursemaxcombo > coursecombo ? coursemaxcombo : coursecombo;
		}
		if (!combocond[judge]) {
			combo = 0;
			coursecombo = 0;
		}

		if (isHeadless()) {
			gauge().update(judge);
			return;
		}

		if (judge != 4)
			this.judge[state.player][state.offset] = judge == 0 ? 1 : judge * 2 + (mfast > 0 ? 0 : 1);
		if (judge <= ((PlaySkin) main.getSkin()).getJudgetimer()) {
			main.timer.setTimerOn(state.timerBomb);
		}

		final int lanelength = states.length;
		int splitRegion = 0;
		if (judgenow.length > 0) {
			final int judgeindex = state.lane / (lanelength / judgenow.length);
			splitRegion = judgeindex;
			main.timer.setTimerOn(JUDGE_TIMER[judgeindex]);
			if (judgenow.length >= 3) {
				for (int i = 0; i < COMBO_TIMER.length; i++) {
					if (i != judgeindex)
						main.timer.setTimerOff(COMBO_TIMER[i]);
				}
			}
			main.timer.setTimerOn(COMBO_TIMER[judgeindex]);
			judgenow[judgeindex] = judge + 1;
			judgecombo[judgeindex] = main.getJudgeManager().getCourseCombo();
			judgefast[judgeindex] = mfast / 1000;
			mjudgefast[judgeindex] = mfast;
		}
		// Without a judge image, split properties still have one shared region.
		if (splitRegion < SPLIT_JUDGE_REGIONS) {
			final int slot = splitJudgeSlot(splitRegion, state.sckey >= 0);
			splitJudgements[slot] = new DisplayJudgement(judge + 1, (int) (mfast / 1000));
			main.timer.setTimerOn(TIMER_JUDGE_1P_KEYS + slot);
		}
		main.update(judge, mtime / 1000);
		keysound.play(judge, mfast >= 0);

		final PlayerConfig player = main.main.getPlayerConfig();
		if(player.isNotesDisplayTimingAutoAdjust()) {
			final BMSPlayerMode autoplay = main.resource.getPlayMode();
			if(autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) {
				if (judge <= 2 && mfast >= -150000 && mfast <= 150000) {
					player.setJudgetiming(player.getJudgetiming() - (int)((mfast >= 0 ? mfast + 15000 : mfast - 15000) / 30000));
				}
			}
		}
	}
	public long[] getRecentJudges() {
		return recentJudges;
	}

	public long[] getMicroRecentJudges() {
		return microrecentJudges;
	}

	public int getRecentJudgesIndex() {
		return recentJudgesIndex;
	}

	public long getRecentJudgeTiming(int player) {
		return player >= 0 && player < judgefast.length ? judgefast[player] : 0;
	}

	public long getRecentJudgeMicroTiming(int player) {
		return player >= 0 && player < mjudgefast.length ? mjudgefast[player] : 0;
	}

	private static int splitJudgeSlot(int region, boolean scratch) {
		return region + (scratch ? SPLIT_JUDGE_REGIONS : 0);
	}

	public DisplayJudgement getSplitJudgement(int region, boolean scratch) {
		return region >= 0 && region < SPLIT_JUDGE_REGIONS
				? splitJudgements[splitJudgeSlot(region, scratch)] : null;
	}

	public LongNote getProcessingLongNote(int lane) {
		return states[lane].processing;
	}
	public LongNote getPassingLongNote(int lane) {
		return null;
	}

	public boolean getHellChargeJudge(int lane) {
		return false;
	}

	public long[] getAutoPresstime() {
		return auto_presstime;
	}

	/**
	 * 現在の1曲内のコンボ数を取得する
	 *
	 * @return 現在のコンボ数
	 */
	public int getCombo() {
		return combo;
	}

	/**
	 * 現在のコース内のコンボ数を取得する
	 *
	 * @return 現在のコンボ数
	 */
	public int getCourseCombo() {
		return coursecombo;
	}

	public void setCourseCombo(int combo) {
		this.coursecombo = combo;
	}

	public int getCourseMaxcombo() {
		return coursemaxcombo;
	}

	public void setCourseMaxcombo(int combo) {
		this.coursemaxcombo = combo;
	}

	public boolean isScratch(int lane) {
		return states[lane].sckey >= 0;
	}

	public ScoreData getScoreData() {
		return score;
	}

	/**
	 * 指定の判定のカウント数を返す
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge) {
		return score.getJudgeCount(judge);
	}

	/**
	 * 指定の判定のカウント数を返す
	 *
	 * @param judge
	 *            0:PG, 1:GR, 2:GD, 3:BD, 4:PR, 5:MS
	 * @param fast
	 *            true:FAST, flase:SLOW
	 * @return 判定のカウント数
	 */
	public int getJudgeCount(int judge, boolean fast) {
		return score.getJudgeCount(judge, fast);
	}

	public int getJudge(int timerId) {
		int player = SkinPropertyMapper.getKeyJudgeValuePlayer(timerId);
		int offset = SkinPropertyMapper.getKeyJudgeValueOffset(timerId);
		if (player >= judge.length || offset >= judge[player].length)
			return -1;
		return judge[player][offset];
	}

	public int getNowJudge(int player) {
		return player >= 0 && player < judgenow.length ? judgenow[player] : 0;
	}

	public int getNowCombo(int player) {
		return player >= 0 && player < judgecombo.length ? judgecombo[player] : 0;
	}

	public final JudgeWindow getJudgeWindow() {
		return window;
	}

	public int getPastNotes() {
		return score.getPassnotes();
	}

	public int[] getGhost() {
		return ghostJudge;
	}
	static class LaneState {
		final int lane;
		final int player;
		final int offset;
		final int sckey;
		final int[] laneassign;
		final int timerBomb;
		final int timerHold;
		final Lane lanemodel;
		LongNote processing;
		int lnstartJudge;
		long lnstartDuration;
		long releasetime = Long.MIN_VALUE;

		public LaneState(int lane, LaneProperty property, Lane lanemodel) {
			this.lane = lane;
			this.lanemodel = lanemodel;
			this.player = property.getLanePlayer()[lane];
			this.offset = property.getLaneSkinOffset()[lane];
			this.sckey = property.getLaneScratchAssign()[lane];
			this.laneassign = property.getLaneKeyAssign()[lane];
			this.timerBomb = SkinPropertyMapper.bombTimerId(player, offset);
			this.timerHold = SkinPropertyMapper.holdTimerId(player, offset);
		}
	}
}
