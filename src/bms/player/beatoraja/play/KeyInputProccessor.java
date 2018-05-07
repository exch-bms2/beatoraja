package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * �궘�꺖�뀯�뒟�눇�릤�뵪�궧�꺃�긿�깋
 *
 * @author exch
 */
class KeyInputProccessor {

	private final BMSPlayer player;

	private JudgeThread judge;

	private long prevtime = -1;
	private int[] scratch;
	private int[] scratchKey;

	private final LaneProperty laneProperty;

	//�궘�꺖�깛�꺖�깲�닩若싧릪�쐿�뵪
	private boolean isJudgeStarted = false;

	//�궘�꺖�깛�꺖�깲�걶閭®뵪
	private boolean keyBeamStop = false;

	public KeyInputProccessor(BMSPlayer player, LaneProperty laneProperty) {
		this.player = player;
		this.laneProperty = laneProperty;
		this.scratch = new int[laneProperty.getScratchKeyAssign().length];
		this.scratchKey = new int[laneProperty.getScratchKeyAssign().length];
	}

	public void startJudge(BMSModel model, List<KeyInputLog> keylog) {
		judge = new JudgeThread(model.getAllTimeLines(),
				keylog != null ? keylog.toArray(new KeyInputLog[keylog.size()]) : null);
		judge.start();
		isJudgeStarted = true;
	}

	public void input() {
		final MainController main = player.main;
		final long now = main.getNowTime();
		final boolean[] keystate = main.getInputProcessor().getKeystate();
		final long[] auto_presstime = player.getJudgeManager().getAutoPresstime();

		final int[] laneoffset = laneProperty.getLaneSkinOffset();
		for (int lane = 0; lane < laneoffset.length; lane++) {
			// �궘�꺖�깛�꺖�깲�깢�꺀�궛ON/OFF			
			final int offset = laneoffset[lane];
			boolean pressed = false;
			boolean scratch = false;
			if(!keyBeamStop) {
				for (int key : laneProperty.getLaneKeyAssign()[lane]) {
					if (keystate[key] || auto_presstime[key] != Long.MIN_VALUE) {
						pressed = true;
						if(laneProperty.getLaneScratchAssign()[lane] != -1
								&& scratchKey[laneProperty.getLaneScratchAssign()[lane]] != key) {
							scratch = true;
							scratchKey[laneProperty.getLaneScratchAssign()[lane]] = key;
						}
					}
				}
			}
			final int timerOn = SkinPropertyMapper.keyOnTimerId(laneProperty.getLanePlayer()[lane], offset);
			final int timerOff = SkinPropertyMapper.keyOffTimerId(laneProperty.getLanePlayer()[lane], offset);
			if (pressed) {
				if(!isJudgeStarted || main.getPlayerResource().getPlayMode().isAutoPlayMode()) {
					if (!main.isTimerOn(timerOn) || scratch) {
						main.setTimerOn(timerOn);
						main.setTimerOff(timerOff);
					}
				}
			} else {
				if (main.isTimerOn(timerOn)) {
					main.setTimerOn(timerOff);
					main.setTimerOff(timerOn);
				}
			}
		}

		if(prevtime >= 0) {
			final long deltatime = now - prevtime;
			for (int s = 0; s < scratch.length; s++) {
				scratch[s] += s % 2 == 0 ? 2160 - deltatime : deltatime;
				final int key0 = laneProperty.getScratchKeyAssign()[s][1];
				final int key1 = laneProperty.getScratchKeyAssign()[s][0];
				if (keystate[key0] || auto_presstime[key0] != Long.MIN_VALUE) {
					scratch[s] += deltatime * 2;
				} else if (keystate[key1] || auto_presstime[key1] != Long.MIN_VALUE) {
					scratch[s] += 2160 - deltatime * 2;
				}
				scratch[s] %= 2160;
			}
		}
		prevtime = now;
	}

	// �궘�꺖�깛�꺖�깲�깢�꺀�궛ON �닩若싧릪�쐿�뵪
	public void inputKeyOn(int lane) {
		final MainController main = player.main;
		final int offset = laneProperty.getLaneSkinOffset()[lane];
		if(!keyBeamStop) {
			final int timerOn = SkinPropertyMapper.keyOnTimerId(laneProperty.getLanePlayer()[lane], offset);
			final int timerOff = SkinPropertyMapper.keyOffTimerId(laneProperty.getLanePlayer()[lane], offset);
			if (!main.isTimerOn(timerOn) || laneProperty.getLaneScratchAssign()[lane] != -1) {
				main.setTimerOn(timerOn);
				main.setTimerOff(timerOff);
			}
		}
	}

	public int getScratchState(int i) {
		return scratch[i] / 6;
	}

	public void stopJudge() {
		if (judge != null) {
			keyBeamStop = true;
			isJudgeStarted = false;
			judge.stop = true;
			judge = null;
		}
	}

	public void setKeyBeamStop(boolean inputStop) {
		this.keyBeamStop = inputStop;
	}

	/**
	 * �깤�꺃�궎�꺆�궛�걢�굢�겗�궘�꺖�눎�땿�뀯�뒟�곩닩若싧눇�릤�뵪�궧�꺃�긿�깋
	 */
	class JudgeThread extends Thread {

		// TODO �닩若싧눇�릤�궧�꺃�긿�깋�겘JudgeManager�겓歷▲걮�걼�뼶�걣�걚�걚�걢�굚

		private final TimeLine[] timelines;
		private boolean stop = false;
		/**
		 * �눎�땿�뀯�뒟�걲�굥�궘�꺖�뀯�뒟�꺆�궛
		 */
		private final KeyInputLog[] keylog;

		public JudgeThread(TimeLine[] timelines, KeyInputLog[] keylog) {
			this.timelines = timelines;
			this.keylog = keylog;
		}

		@Override
		public void run() {
			int index = 0;

			long frametime = 1;
			final BMSPlayerInputProcessor input = player.main.getInputProcessor();
			final JudgeManager judge = player.getJudgeManager();
			final int lasttime = timelines[timelines.length - 1].getTime() + BMSPlayer.TIME_MARGIN;

			long prevtime = -1;
			while (!stop) {
				final long time = player.main.getNowTime(TIMER_PLAY);
				if (time != prevtime) {
					// �꺁�깤�꺃�궎�깈�꺖�궭�냽�뵟
					if (keylog != null) {
						while (index < keylog.length && keylog[index].time <= time) {
							final KeyInputLog key = keylog[index];
							// if(input.getKeystate()[key.keycode] ==
							// key.pressed) {
							// System.out.println("�듉�걮�썴�걮�걣烏뚣굩�굦�겍�걚�겲�걵�굯 : key - " +
							// key.keycode + " pressed - " + key.pressed +
							// " time - " + key.time);
							// }
							input.getKeystate()[key.keycode] = key.pressed;
							input.getTime()[key.keycode] = key.time;
							index++;
						}
					}

					judge.update(time);

					if (prevtime != -1) {
						final long nowtime = time - prevtime;
						frametime = nowtime < frametime ? frametime : nowtime;
					}

					prevtime = time;
				} else {
					try {
						sleep(0, 500000);
					} catch (InterruptedException e) {
					}
				}

				if (time >= lasttime) {
					break;
				}
			}

			if (keylog != null) {
				Arrays.fill(input.getKeystate(), false);
				Arrays.fill(input.getTime(), 0);
			}

			Logger.getGlobal().info("�뀯�뒟�깙�깢�궔�꺖�깯�꺍�궧(max ms) : " + frametime);
		}
	}
}