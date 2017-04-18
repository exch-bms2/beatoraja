package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSModel;
import bms.model.Mode;
import bms.model.TimeLine;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyInputLog;

/**
 * キー入力処理用スレッド
 *
 * @author exch
 */
class KeyInputProccessor {

	private final BMSPlayer player;
	private final Mode mode;
	private final boolean has_scratch;

	private final int[] key_offset = { 1, 2, 3, 4, 5, 6, 7, 0, 11, 12, 13, 14, 15, 16, 17, 10 };

	private JudgeThread judge;

	private int prevtime = -1;
	private int scratch1;
	private int scratch2;

	public KeyInputProccessor(BMSPlayer player, Mode mode) {
		this.player = player;
		this.mode = mode;
		has_scratch = mode == Mode.BEAT_5K || mode == Mode.BEAT_7K || mode == Mode.BEAT_10K || mode == Mode.BEAT_14K;
	}

	public void startJudge(BMSModel model, List<KeyInputLog> keylog) {
		judge = new JudgeThread(model.getAllTimeLines(),
				keylog != null ? keylog.toArray(new KeyInputLog[keylog.size()]) : null);
		judge.start();
	}

	public void input() {
		final int now = player.getNowTime();
		final long[] timer = player.getTimer();
		final JudgeManager judge = player.getJudgeManager();
		final boolean[] keystate = player.getMainController().getInputProcessor().getKeystate();

		for (int lane = 0; lane < mode.key; lane++) {
			// キービームフラグON/OFF
			if (has_scratch) {
				final int key = lane >= 8 ? lane + 1 : lane;
				final int offset = key_offset[lane];
				if (keystate[key] || (key == 7 && keystate[8]) || (key == 16 && keystate[17])) {
					if (timer[TIMER_KEYON_1P_SCRATCH + offset] == Long.MIN_VALUE) {
						timer[TIMER_KEYON_1P_SCRATCH + offset] = now;
						timer[TIMER_KEYOFF_1P_SCRATCH + offset] = Long.MIN_VALUE;
					}
				} else {
					if (timer[TIMER_KEYOFF_1P_SCRATCH + offset] == Long.MIN_VALUE) {
						timer[TIMER_KEYOFF_1P_SCRATCH + offset] = now;
						timer[TIMER_KEYON_1P_SCRATCH + offset] = Long.MIN_VALUE;
					}
				}
			} else {
				if (lane < 9) {
					if (keystate[lane]) {
						if (timer[TIMER_KEYON_1P_KEY1 + lane] == Long.MIN_VALUE) {
							timer[TIMER_KEYON_1P_KEY1 + lane] = now;
							timer[TIMER_KEYOFF_1P_KEY1 + lane] = Long.MIN_VALUE;
						}
					} else {
						if (timer[TIMER_KEYOFF_1P_KEY1 + lane] == Long.MIN_VALUE) {
							timer[TIMER_KEYOFF_1P_KEY1 + lane] = now;
							timer[TIMER_KEYON_1P_KEY1 + lane] = Long.MIN_VALUE;
						}
					}
				} else {
					if (keystate[lane]) {
						if (timer[TIMER_KEYON_1P_KEY10 + lane - 9] == Long.MIN_VALUE) {
							timer[TIMER_KEYON_1P_KEY10 + lane - 9] = now;
							timer[TIMER_KEYOFF_1P_KEY10 + lane - 9] = Long.MIN_VALUE;
						}
					} else {
						if (timer[TIMER_KEYOFF_1P_KEY10 + lane - 9] == Long.MIN_VALUE) {
							timer[TIMER_KEYOFF_1P_KEY10 + lane - 9] = now;
							timer[TIMER_KEYON_1P_KEY10 + lane - 9] = Long.MIN_VALUE;
						}
					}
				}
			}
		}
		
		if(prevtime >= 0) {
			final int deltatime = now - prevtime;
			scratch1 += 2160 - deltatime;
			scratch2 += deltatime;
			if (has_scratch) {
				if (keystate[7]) {
					scratch1 += deltatime * 2;
				} else if (keystate[8]) {
					scratch1 += 2160 - deltatime * 2;
				}
				if (keystate[16]) {
					scratch2 += deltatime * 2;
				} else if (keystate[17]) {
					scratch2 += 2160 - deltatime * 2;
				}
			}
			scratch1 %= 2160;
			scratch2 %= 2160;			
		}
		prevtime = now;
	}
	
	public int getScratchState(int i) {
		if(i == 0) {
			return scratch1 / 6;
		}
		return scratch2 / 6;
	}

	public void stopJudge() {
		if (judge != null) {
			judge.stop = true;
			judge = null;
		}
	}

	class JudgeThread extends Thread {

		private final TimeLine[] timelines;
		private boolean stop = false;
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
			final long[] timer = player.getTimer();

			int prevtime = -1;
			while (!stop) {
				final int now = player.getNowTime();
				final int time = (int) (now - timer[TIMER_PLAY]);
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