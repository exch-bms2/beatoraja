package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.TimeLine;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * キー入力処理用スレッド
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

	//キービーム停止用
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
	}

	public void input() {
		final long now = player.getNowTime();
		final boolean[] keystate = player.getMainController().getInputProcessor().getKeystate();
		final long[] auto_presstime = player.getJudgeManager().getAutoPresstime();

		final int[] laneoffset = laneProperty.getLaneSkinOffset();
		for (int lane = 0; lane < laneoffset.length; lane++) {
			// キービームフラグON/OFF
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
				if (!player.isTimerOn(timerOn) || scratch) {
					player.setTimerOn(timerOn);
					player.setTimerOff(timerOff);
				}
			} else {
				if (player.isTimerOn(timerOn)) {
					player.setTimerOn(timerOff);
					player.setTimerOff(timerOn);
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

	public int getScratchState(int i) {
		return scratch[i] / 6;
	}

	public void stopJudge() {
		if (judge != null) {
			keyBeamStop = true;
			judge.stop = true;
			judge = null;
		}
	}

	public void setKeyBeamStop(boolean inputStop) {
		this.keyBeamStop = inputStop;
	}

	/**
	 * プレイログからのキー自動入力、判定処理用スレッド
	 */
	class JudgeThread extends Thread {

		// TODO 判定処理スレッドはJudgeManagerに渡した方がいいかも

		private final TimeLine[] timelines;
		private boolean stop = false;
		/**
		 * 自動入力するキー入力ログ
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
			final BMSPlayerInputProcessor input = player.getMainController().getInputProcessor();
			final JudgeManager judge = player.getJudgeManager();
			final int lasttime = timelines[timelines.length - 1].getTime() + BMSPlayer.TIME_MARGIN;

			long prevtime = -1;
			while (!stop) {
				final long time = player.getNowTime(TIMER_PLAY);
				if (time != prevtime) {
					// リプレイデータ再生
					if (keylog != null) {
						while (index < keylog.length && keylog[index].time <= time) {
							final KeyInputLog key = keylog[index];
							// if(input.getKeystate()[key.keycode] ==
							// key.pressed) {
							// System.out.println("押し離しが行われていません : key - " +
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

			Logger.getGlobal().info("入力パフォーマンス(max ms) : " + frametime);
		}
	}
}