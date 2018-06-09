package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;

import bms.model.Mode;
import bms.player.beatoraja.PlayerResource.PlayMode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.keyData;

public class ControlInputProcessor {
	
	private final BMSPlayer player;

	private boolean[] hschanged;
	private long startpressedtime;
	private boolean startpressed = false;
	private boolean selectpressed = false;
	private boolean cursorpressed;
	private long lanecovertiming;
	private long exitpressedtime;

	private boolean enableControl = true;
	private boolean enableCursor = true;
	
	private final PlayMode autoplay;

	private Runnable processStart;
	private Runnable processSelect;

	public ControlInputProcessor(BMSPlayer player, PlayMode autoplay) {
		this.player = player;
		this.autoplay = autoplay;
		hschanged = new boolean[keyData.getKeyLength()];
		Arrays.fill(hschanged, true);

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
		// �뜝�럥�봻占쎈첎�뜝�룞�삕亦낆쥜�삕�댆�엪�삕繹먮맮�삕�댆�냵�삕�댆戮녹삕�댆洹⑥삕占쏙옙�뜝�럥裕볟뜝�럥�뼋�븨�뙋�삕
		if (enableControl) {
			if (enableCursor) {
				if (input.getCursorState(0)) {
					if (!cursorpressed) {
						lanerender.setLanecover(lanerender.getLanecover() - 0.01f);
						cursorpressed = true;
					}
				} else if (input.getCursorState(1)) {
					if (!cursorpressed) {
						lanerender.setLanecover(lanerender.getLanecover() + 0.01f);
						cursorpressed = true;
					}
				} else {
					cursorpressed = false;
				}
			}
			// move lane cover by mouse wheel
			if (input.getScroll() != 0) {
				lanerender.setLanecover(lanerender.getLanecover() - input.getScroll() * 0.005f);
				input.resetScroll();
			}
			if ((input.startPressed() && !input.isSelectPressed())
					|| (player.main.getPlayerResource().getPlayerConfig().isWindowHold() && player.main.isTimerOn(TIMER_PLAY) && !player.isNoteEnd())) {
				if ((autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) && startpressed) {
					processStart.run();
				} else if ((autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) && !startpressed) {
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
				if ((autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) && selectpressed) {
					processSelect.run();
				} else if ((autoplay == PlayMode.PLAY || autoplay == PlayMode.PRACTICE) && !selectpressed) {
					Arrays.fill(hschanged, true);
				}
				selectpressed = true;
			} else {
				selectpressed = false;
			}
		}
		long now = System.currentTimeMillis();
		if((input.startPressed() && input.isSelectPressed() && now - exitpressedtime > 1000 )||
				(player.isNoteEnd() && (input.startPressed() || input.isSelectPressed()))){
			input.startChanged(false);
			input.setSelectPressed(false);
			player.stopPlay();
		}else if(!(input.startPressed() && input.isSelectPressed())){
			exitpressedtime = now;
		}
		// stop playing
		if (input.isExitPressed()) {
			input.setExitPressed(false);
			player.stopPlay();
		}
		// play speed change (autoplay or replay only)
		if (autoplay.isAutoPlayMode() || autoplay.isReplayMode()) {
			if (keyData.getNumberState(1)) {
				player.setPlaySpeed(25);
			} else if (keyData.getNumberState(2)) {
				player.setPlaySpeed(50);
			} else if (keyData.getNumberState(3)) {
				player.setPlaySpeed(200);
			} else if (keyData.getNumberState(4)) {
				player.setPlaySpeed(300);
			} else {
				player.setPlaySpeed(100);
			}
		}
	}

	void processStart7key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change hi speed by START + Keys
		for(int i = 0; i <= 15; i++) {
			if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && keyData.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(false);
					hschanged[i] = true;
				}
			} else if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && keyData.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(true);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}

		// move lane cover by START + Scratch
		boolean start2Scratch = keyData.getKeyState(7) || keyData.getKeyState(8) || keyData.getKeyState(16) || keyData.getKeyState(17);
		if (start2Scratch) {
			long l = System.currentTimeMillis();
			if (l - lanecovertiming > 50) {
				lanerender.setLanecover(lanerender.getLanecover() + (keyData.getKeyState(7) || keyData.getKeyState(16) ? 0.001f : -0.001f));
				lanecovertiming = l;
			}
		}
	}

	void processSelect7key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Scratch
		boolean start2Scratch = keyData.getKeyState(7) || keyData.getKeyState(8) ||keyData.getKeyState(16) || keyData.getKeyState(17);
		if (start2Scratch) {
			long l = System.currentTimeMillis();
			if (l - lanecovertiming > 50) {
				lanerender.setGreenValue(lanerender.getGreenValue() + (keyData.getKeyState(7) || keyData.getKeyState(16) ? 1 : -1));
				lanecovertiming = l;
			}
		}

		// change duration by SELECT + Keys
		for(int i = 0; i <= 15; i++) {
			if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && keyData.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() -1);
					hschanged[i] = true;
				}
			} else if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && keyData.getKeyState(i))  {
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
			if (i % 2 == 1 && keyData.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.changeHispeed(true);
					hschanged[i] = true;
				}
			} else if (i % 2 == 0 && keyData.getKeyState(i))  {
				if(!hschanged[i]) {
					lanerender.changeHispeed(false);
					hschanged[i] = true;
				}
			} else {
				hschanged[i] = false;
			}
		}

		// move lane cover by START + Keys(7-8)
		if (keyData.getKeyState(7) || keyData.getKeyState(8)) {
			long l = System.currentTimeMillis();
			if (l - lanecovertiming > 50) {
				lanerender.setLanecover(lanerender.getLanecover() + (keyData.getKeyState(7) || keyData.getKeyState(16) ? 0.001f : -0.001f));
				lanecovertiming = l;
			}
		}
	}

	void processSelect9key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Keys
		for(int i = 0; i <= 8; i++) {
			if (i % 2 == 1 && keyData.getKeyState(i)) {
				if(!hschanged[i]) {
					lanerender.setGreenValue(lanerender.getGreenValue() -1);
					hschanged[i] = true;
				}
			} else if (i % 2 == 0 && keyData.getKeyState(i))  {
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

		// change duration by SELECT + Keys/Wheel
		for(int i = 0; i < 52; i++) {
			int j = i % 26;
			if (keyData.getKeyState(i) && j < 24) {
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
			} else if (keyData.getKeyState(i) && j >= 24) {
				long l = System.currentTimeMillis();
				if (l - lanecovertiming > 50) {
					lanerender.setLanecover(lanerender.getLanecover() + (j == 25 ? 0.001f : -0.001f));
					lanecovertiming = l;
				}
				hschanged[i] = false;
			} else {
				hschanged[i] = false;
			}
		}
	}

	void processSelect24key() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.main.getInputProcessor();

		// change duration by SELECT + Keys/Wheel
		for(int i = 0; i < 52; i++) {
			int j = i % 26;
			if (keyData.getKeyState(i) && j < 24) {
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
			} else if (keyData.getKeyState(i) && j >= 24) {
				long l = System.currentTimeMillis();
				if (l - lanecovertiming > 50) {
					lanerender.setGreenValue(lanerender.getGreenValue() + (j == 24 ? 1 : -1));
					lanecovertiming = l;
				}
				hschanged[i] = false;
			} else {
				hschanged[i] = false;
			}
		}
	}
}
