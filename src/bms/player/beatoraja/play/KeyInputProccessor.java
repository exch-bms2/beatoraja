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
import bms.player.beatoraja.input.keyData;
import bms.player.beatoraja.skin.SkinPropertyMapper;

/**
 * 占쎄텣占쎄틬占쎈��占쎈뮓占쎈늾占쎈┐占쎈뎁占쎄때占쎄틕占쎄맙占쎄퉳
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

	//占쎄텣占쎄틬占쎄튆占쎄틬占쎄묾占쎈떓畑댁떑由わ옙�맾占쎈뎁
	private boolean isJudgeStarted = false;

	//占쎄텣占쎄틬占쎄튆占쎄틬占쎄묾占쎄굡癲녌�逾�
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
		final BMSPlayerInputProcessor input = main.getInputProcessor();
		final long[] auto_presstime = player.getJudgeManager().getAutoPresstime();

		final int[] laneoffset = laneProperty.getLaneSkinOffset();
		for (int lane = 0; lane < laneoffset.length; lane++) {
			// 占쎄텣占쎄틬占쎄튆占쎄틬占쎄묾占쎄묄占쎄�占쎄텦ON/OFF			
			final int offset = laneoffset[lane];
			boolean pressed = false;
			boolean scratch = false;
			if(!keyBeamStop) {
				for (int key : laneProperty.getLaneKeyAssign()[lane]) {
					if (keyData.getKeyState(key) || auto_presstime[key] != Long.MIN_VALUE) {
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
				if (keyData.getKeyState(key0) || auto_presstime[key0] != Long.MIN_VALUE) {
					scratch[s] += deltatime * 2;
				} else if (keyData.getKeyState(key1) || auto_presstime[key1] != Long.MIN_VALUE) {
					scratch[s] += 2160 - deltatime * 2;
				}
				scratch[s] %= 2160;
			}
		}
		prevtime = now;
	}

	// 占쎄텣占쎄틬占쎄튆占쎄틬占쎄묾占쎄묄占쎄�占쎄텦ON 占쎈떓畑댁떑由わ옙�맾占쎈뎁
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
	 * 占쎄묏占쎄틕占쎄텕占쎄틙占쎄텦占쎄괍占쎄덩占쎄쿁占쎄텣占쎄틬占쎈닅占쎈빣占쎈��占쎈뮓占쎄낑�떓畑댁떑�늾占쎈┐占쎈뎁占쎄때占쎄틕占쎄맙占쎄퉳
	 */
	class JudgeThread extends Thread {

		// TODO 占쎈떓畑댁떑�늾占쎈┐占쎄때占쎄틕占쎄맙占쎄퉳占쎄쿂JudgeManager占쎄쾽癲뚢뼯嫄�占쎄굴占쎈섬占쎄괏占쎄콢占쎄콢占쎄괍占쎄탾

		private final TimeLine[] timelines;
		private boolean stop = false;
		/**
		 * 占쎈닅占쎈빣占쎈��占쎈뮓占쎄굉占쎄데占쎄텣占쎄틬占쎈��占쎈뮓占쎄틙占쎄텦
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
					// 占쎄틒占쎄묏占쎄틕占쎄텕占쎄퉰占쎄틬占쎄땟占쎈꺗占쎈턃
					if (keylog != null) {
						while (index < keylog.length && keylog[index].time <= time) {
							final KeyInputLog key = keylog[index];
							// if(input.getKeystate()[key.keycode] ==
							// key.pressed) {
							// System.out.println("占쎈뱣占쎄괼占쎌뜶占쎄괼占쎄괏�깗�슔援⑼옙援�占쎄쾷占쎄콢占쎄께占쎄굘占쎄뎐 : key - " +
							// key.keycode + " pressed - " + key.pressed +
							// " time - " + key.time);
							// }
							keyData.setKeyState(key.keycode, key.pressed);
							keyData.setKeyTime(key.keycode, key.time);
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
				keyData.resetKeyState();
				keyData.resetKeyTime();
			}

			Logger.getGlobal().info("占쎈��占쎈뮓占쎄튃占쎄묄占쎄텛占쎄틬占쎄묻占쎄틡占쎄때(max ms) : " + frametime);
		}
	}
}