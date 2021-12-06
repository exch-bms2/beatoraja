package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;

import bms.player.beatoraja.PlayConfig;
import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;

/**
 * BMSPlayerの制御系の入力処理を行うクラス
 * 
 * @author exch
 */
public class ControlInputProcessor {

	private final BMSPlayer player;

	private boolean[] hschanged;
	private long startpressedtime;
	private boolean startpressed = false;
	private boolean selectpressed = false;
	private boolean startAndSelectPressed = false;
	private boolean cursorpressed;
	private long lanecovertiming;
	private long laneCoverStartTiming = Long.MIN_VALUE;
	private long exitpressedtime;

	private final long exitPressDuration;

	private boolean enableControl = true;
	private boolean enableCursor = true;

	private final BMSPlayerMode autoplay;

	private Runnable processStart;
	private Runnable processSelect;

	private boolean isChangeLift = true;

	private float coverChangeMarginLow = 0.001f;
	private float coverChangeMarginHigh = 0.01f;
	private long coverSpeedSwitchDuration = 500;

	private boolean hispeedAutoAdjust;

	public ControlInputProcessor(BMSPlayer player, BMSPlayerMode autoplay) {
		this.player = player;
		this.autoplay = autoplay;
		hschanged = new boolean[BMSPlayerInputProcessor.KEYSTATE_SIZE];
		Arrays.fill(hschanged, true);

		final PlayConfig playConfig = player.main.getPlayerResource().getPlayerConfig().getPlayConfig(player.getMode()).getPlayconfig();
		coverChangeMarginLow = playConfig.getLanecovermarginlow();
		coverChangeMarginHigh = playConfig.getLanecovermarginhigh();
		coverSpeedSwitchDuration = playConfig.getLanecoverswitchduration();
		hispeedAutoAdjust = playConfig.isEnableHispeedAutoAdjust();

		exitPressDuration = player.main.getPlayerConfig().getExitPressDuration();

		switch (this.player.getMode()) {
		case POPN_9K:
			processStart = () -> processStart9key();
			processSelect = () -> processSelect9key();
			break;
		case KEYBOARD_24K:
		case KEYBOARD_24K_DOUBLE:
			processStart = () -> processStart24key();
			processSelect = () -> processSelect24key();
			break;
		case BEAT_5K:
		case BEAT_10K:
			processStart = () -> processStart5key();
			processSelect = () -> processSelect5key();
			break;
		default:
			processStart = () -> processStart7key();
			processSelect = () -> processSelect7key();
		}
	}

	public void setEnableControl(boolean b) {
		enableControl = b;
	}

	public void setEnableCursor(boolean b) {
		enableCursor = b;
	}

	public void input() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();
		// 各種コントロール入力判定
		if (enableControl) {
			if (enableCursor) {
				if (input.getControlKeyState(ControlKeys.UP)) {
					if (!cursorpressed) {
						setCoverValue(-0.01f);
						cursorpressed = true;
					}
				} else if (input.getControlKeyState(ControlKeys.DOWN)) {
					if (!cursorpressed) {
						setCoverValue(0.01f);
						cursorpressed = true;
					}
				} else {
					cursorpressed = false;
				}
			}
			// move lane cover by mouse wheel
			if (input.getScroll() != 0) {
				setCoverValue(- input.getScroll() * 0.005f);
				input.resetScroll();
			}
			if ((input.startPressed() && !input.isSelectPressed())
					|| (player.main.getPlayerResource().getPlayerConfig().isWindowHold() && player.main.isTimerOn(TIMER_PLAY) && !player.isNoteEnd())) {
				if ((autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) && startpressed) {
					processStart.run();
				} else if ((autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) && !startpressed) {
					Arrays.fill(hschanged, true);
				}
				// show-hide lane cover by double-press START
				if (!startpressed) {
					long stime = System.currentTimeMillis();
					if (stime < startpressedtime + 500) {
						lanerender.setEnableLanecover(!lanerender.isEnableLanecover());
						startpressedtime = 0;
					} else {
						startpressedtime = stime;
					}
				}
				startpressed = true;
			} else {
				startpressed = false;
			}
			if(input.isSelectPressed() && !input.startPressed()){
				if ((autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) && selectpressed) {
					processSelect.run();
				} else if ((autoplay.mode == BMSPlayerMode.Mode.PLAY || autoplay.mode == BMSPlayerMode.Mode.PRACTICE) && !selectpressed) {
					Arrays.fill(hschanged, true);
				}
				selectpressed = true;
			} else {
				selectpressed = false;
			}
			if ((input.startPressed() && input.isSelectPressed())) {
				if(!startAndSelectPressed) {
					isChangeLift = !isChangeLift;
				}
				startAndSelectPressed = true;
			} else {
				startAndSelectPressed = false;
			}
		}
		long now = System.currentTimeMillis();
		if((input.startPressed() && input.isSelectPressed() && now - exitpressedtime > exitPressDuration )||
				(player.isNoteEnd() && (input.startPressed() || input.isSelectPressed()))){
			input.startChanged(false);
			input.setSelectPressed(false);
			player.stopPlay();
		}else if(!(input.startPressed() && input.isSelectPressed())){
			exitpressedtime = now;
		}
		// stop playing
		if (input.isControlKeyPressed(ControlKeys.ESCAPE)) {
			player.stopPlay();
		}
		// play speed change (autoplay or replay only)
		if (autoplay.mode == BMSPlayerMode.Mode.AUTOPLAY || autoplay.mode == BMSPlayerMode.Mode.REPLAY) {
			if (input.getControlKeyState(ControlKeys.NUM1)) {
				player.setPlaySpeed(25);
			} else if (input.getControlKeyState(ControlKeys.NUM2)) {
				player.setPlaySpeed(50);
			} else if (input.getControlKeyState(ControlKeys.NUM3)) {
				player.setPlaySpeed(200);
			} else if (input.getControlKeyState(ControlKeys.NUM4)) {
				player.setPlaySpeed(300);
			} else {
				player.setPlaySpeed(100);
			}
		}
	}

	/*
	 * 状況に応じてレーンカバー/リフト/HIDDENの表示量を変える
	 * レーンカバー: 「レーンカバーがオン」もしくは「リフトとHIDDENが共にオフ」
	 * リフト: 「レーンカバーがオフ」
	 * HIDDEN: 「レーンカバーがオフ」かつ「リフトがオフ」
	 * 「レーンカバーがオフ」で「リフトとHIDDENが共にオン」の時は「START+SELECT短押し」で切り替え
	 */
	private void setCoverValue(float value) {
		final LaneRenderer lanerender = player.getLanerender();
		if(lanerender.isEnableLanecover() || (!lanerender.isEnableLift() && !lanerender.isEnableHidden())) {
			lanerender.setLanecover(lanerender.getLanecover() + value);
		} else if(lanerender.isEnableLift() && (!lanerender.isEnableHidden() || isChangeLift)) {
			lanerender.setLiftRegion(lanerender.getLiftRegion() - value);
		} else {
			lanerender.setHiddenCover(lanerender.getHiddenCover() - value);
		}

		if (hispeedAutoAdjust && lanerender.getNowBPM() > 0) {
			lanerender.resetHispeed(lanerender.getNowBPM());
		}
	}

	private void coverValueChange(int up1, int up2, int down1, int down2) {
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// move lane cover by START + Scratch
		// analog
		int dTicks = 0;
		if (up1 != -1 && input.isAnalogInput(up1)) dTicks += input.getAnalogDiffAndReset(up1, 200);
		if (up2 != -1 && input.isAnalogInput(up2)) dTicks += input.getAnalogDiffAndReset(up2, 200);
		if (down1 != -1 && input.isAnalogInput(down1)) dTicks -= input.getAnalogDiffAndReset(down1, 200);
		if (down2 != -1 && input.isAnalogInput(down2)) dTicks -= input.getAnalogDiffAndReset(down2, 200);
		if (dTicks != 0) {
			setCoverValue(dTicks*coverChangeMarginLow);
		}

		// non-analog
		boolean nonAnalogUp = (up1 != -1 && input.getKeyState(up1) && !input.isAnalogInput(up1)) || (up2 != -1 && input.getKeyState(up2) && !input.isAnalogInput(up2));
		boolean nonAnalogDown = (down1 != -1 && input.getKeyState(down1) && !input.isAnalogInput(down1)) || (down2 != -1 && input.getKeyState(down2) && !input.isAnalogInput(down2));
		if (nonAnalogUp || nonAnalogDown) {
			long l = System.currentTimeMillis();
			if(laneCoverStartTiming == Long.MIN_VALUE) laneCoverStartTiming = l;
			if (l - lanecovertiming > 50) {
				setCoverValue((nonAnalogUp ? 1 : -1) * (l - laneCoverStartTiming > coverSpeedSwitchDuration ? coverChangeMarginHigh : coverChangeMarginLow));
				lanecovertiming = l;
			}
		} else if(laneCoverStartTiming != Long.MIN_VALUE) {
			laneCoverStartTiming = Long.MIN_VALUE;
		}
	}

	private void greenNumberChange(int up1, int up2, int down1, int down2) {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Scratch
		// analog
		int dTicks = 0;
		if (up1 != -1 && input.isAnalogInput(up1)) dTicks += input.getAnalogDiffAndReset(up1, 200);
		if (up2 != -1 && input.isAnalogInput(up2)) dTicks += input.getAnalogDiffAndReset(up2, 200);
		if (down1 != -1 && input.isAnalogInput(down1)) dTicks -= input.getAnalogDiffAndReset(down1, 200);
		if (down2 != -1 && input.isAnalogInput(down2)) dTicks -= input.getAnalogDiffAndReset(down2, 200);
		if (dTicks != 0) {
			lanerender.setGreenValue(lanerender.getGreenValue() + dTicks);
		}

		// non-analog
		boolean nonAnalogUp = (up1 != -1 && input.getKeyState(up1) && !input.isAnalogInput(up1)) || (up2 != -1 && input.getKeyState(up2) && !input.isAnalogInput(up2));
		boolean nonAnalogDown = (down1 != -1 && input.getKeyState(down1) && !input.isAnalogInput(down1)) || (down2 != -1 && input.getKeyState(down2) && !input.isAnalogInput(down2));
		if (nonAnalogUp || nonAnalogDown) {
			long l = System.currentTimeMillis();
			if (l - lanecovertiming > 50) {
				lanerender.setGreenValue(lanerender.getGreenValue() + (nonAnalogUp ? 1 : -1));
				lanecovertiming = l;
			}
		}
	}

	void processStart7key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change hi speed by START + Keys
		for(int i = 0; i <= 15; i++) {
			if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(false);
					hschanged[i] = true;
				}
			} else if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(true);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}

		// move lane cover by START + Scratch
		coverValueChange(7, 16, 8, 17);
	}

	void processSelect7key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Scratch
		greenNumberChange(7, 16, 8, 17);

		// change duration by SELECT + Keys
		for(int i = 0; i <= 15; i++) {
			if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() -1);
					hschanged[i] = true;
				}
			} else if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && input.getKeyState(i))  {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() +1);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}
	}

	void processStart5key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change hi speed by START + Keys
		for(int i = 0; i <= 11; i++) {
			if ((i == 0 || i == 2 || i == 4 || i == 7 || i == 9 || i == 11) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(false);
					hschanged[i] = true;
				}
			} else if ((i == 1 || i == 3 || i == 8 || i == 10) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(true);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}

		// move lane cover by START + Scratch
		coverValueChange(5, 12, 6, 13);
	}

	void processSelect5key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Scratch
		greenNumberChange(5, 12, 6, 13);

		// change duration by SELECT + Keys
		for(int i = 0; i <= 11; i++) {
			if ((i == 1 || i == 3 || i == 8 || i == 10) && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() -1);
					hschanged[i] = true;
				}
			} else if ((i == 0 || i == 2 || i == 4 || i == 7 || i == 9 || i == 11) && input.getKeyState(i))  {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() +1);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}
	}

	void processStart9key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change hi speed by START + Keys(0-6)
		for(int i = 0; i <= 6; i++) {
			if (i % 2 == 1 && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(true);
					hschanged[i] = true;
				}
			} else if (i % 2 == 0 && input.getKeyState(i))  {
				if(!hschanged[i]) {
					lanerender.changeHispeed(false);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}

		// move lane cover by START + Keys(7-8)
		coverValueChange(8, -1, 7, -1);
	}

	void processSelect9key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Keys
		for(int i = 0; i <= 8; i++) {
			if (i % 2 == 1 && input.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() -1);
					hschanged[i] = true;
				}
			} else if (i % 2 == 0 && input.getKeyState(i))  {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() +1);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}
	}

	void processStart24key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by START + Keys/Wheel
		for(int i = 0; i < 52; i++) {
			int j = i % 26;
			if (input.getKeyState(i) && j < 24) {
				int k = j % 12;
				if (k <= 4 ? k % 2 == 1 : k % 2 == 0) {
					if (!hschanged[i]) {
						lanerender.changeHispeed(true);
						hschanged[i] = true;
					}
				} else {
					if (!hschanged[i]) {
						lanerender.changeHispeed(false);
						hschanged[i] = true;
					}
				}
			} else {
				hschanged[i] = false;
			}
		}

		coverValueChange(25, 51, 24, 50);
	}

	void processSelect24key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Keys/Wheel
		for(int i = 0; i < 52; i++) {
			int j = i % 26;
			if (input.getKeyState(i) && j < 24) {
				int k = j % 12;
				if (k <= 4 ? k % 2 == 1 : k % 2 == 0) {
					if (!hschanged[i]) {
						lanerender.setGreenValue(lanerender.getGreenValue() - 1);
						hschanged[i] = true;
					}
				} else {
					if (!hschanged[i]) {
						lanerender.setGreenValue(lanerender.getGreenValue() + 1);
						hschanged[i] = true;
					}
				}
			} else {
				hschanged[i] = false;
			}
		}
		greenNumberChange(25, 51, 24, 50);
	}
}
