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
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * キー入力処理用スレッド
 *
 * @author exch
 */
class KeyInputProccessor {

	private final BMSPlayer player;
	private final boolean ispms;

	private JudgeThread judge;

	private int prevtime = -1;
	private int[] scratch;

	public KeyInputProccessor(BMSPlayer player, Mode mode) {
		this.player = player;
		ispms = mode == Mode.POPN_5K || mode == Mode.POPN_9K;
		this.scratch = new int[2];
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

		for (int lane = 0; lane < player.getLaneProperty().getSkinOffset().length; lane++) {
			// キービームフラグON/OFF
			final int offset = player.getLaneProperty().getSkinOffset()[lane];
			boolean pressed = false;
			for (int i = 0; i < player.getLaneProperty().getLaneAssign()[lane].length; i++) {
				int key = player.getLaneProperty().getLaneAssign()[lane][i];
				if (key >= 0 && keystate[key]) {
					pressed = true;
					break;
				}
			}
			final int timerOn = SkinPropertyMapper.keyOnTimerId(player.getLaneProperty().getPlayer()[lane], offset);
			final int timerOff = SkinPropertyMapper.keyOffTimerId(player.getLaneProperty().getPlayer()[lane], offset);
			if (pressed) {
				if (timer[timerOn] == Long.MIN_VALUE) {
					timer[timerOn] = now;
					timer[timerOff] = Long.MIN_VALUE;
				}
			} else {
				if (timer[timerOff] == Long.MIN_VALUE) {
					timer[timerOff] = now;
					timer[timerOn] = Long.MIN_VALUE;
				}
			}
		}
		
		if(prevtime >= 0) {
			final int deltatime = now - prevtime;
			for (int s = 0; s < scratch.length; s++) {
				scratch[s] += s % 2 == 0 ? 2160 - deltatime : deltatime;
				if (s < player.getLaneProperty().getScratchToKey().length) {
					if (keystate[player.getLaneProperty().getScratchToKey()[s][0]]) {
						scratch[s] += deltatime * 2;
					} else if (keystate[player.getLaneProperty().getScratchToKey()[s][1]]) {
						scratch[s] += 2160 - deltatime * 2;
					}
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