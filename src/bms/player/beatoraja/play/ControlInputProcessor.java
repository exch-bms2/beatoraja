package bms.player.beatoraja.play;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;

public class ControlInputProcessor {
	
	private final BMSPlayer player;

	private boolean hschanged;
	private long startpressedtime;
	private boolean startpressed;
	private boolean cursorpressed;
	private long lanecovertiming;

	private boolean enableControl = true;
	
	private final int autoplay;

	public ControlInputProcessor(BMSPlayer player, int autoplay) {
		this.player = player;
		this.autoplay = autoplay;
	}
	
	public void setEnableControl(boolean b) {
		enableControl = b;
	}
	
	public void input() {
		final LaneRenderer lanerender = player.getLanerender();
		final BMSPlayerInputProcessor input = player.getMainController().getInputProcessor();
		// 各種コントロール入力判定
		if (enableControl) {
			if (input.getCursorState()[0]) {
				if (!cursorpressed) {
					lanerender.setLanecover(lanerender.getLanecover() - 0.01f);
					cursorpressed = true;
				}
			} else if (input.getCursorState()[1]) {
				if (!cursorpressed) {
					lanerender.setLanecover(lanerender.getLanecover() + 0.01f);
					cursorpressed = true;
				}
			} else {
				cursorpressed = false;
			}
			// move lane cover by mouse wheel
			if (input.getScroll() != 0) {
				lanerender.setLanecover(lanerender.getLanecover() - input.getScroll() * 0.005f);
				input.resetScroll();
			}
			if (input.startPressed() && !input.isSelectPressed()) {
				if (autoplay == 0) {
					// change hi speed by START + Keys
					boolean[] key = input.getKeystate();
					if (key[0] || key[2] || key[4] || key[6]) {
						if (!hschanged) {
							lanerender.changeHispeed(false);
							hschanged = true;
						}
					} else if (key[1] || key[3] || key[5]) {
						if (!hschanged) {
							lanerender.changeHispeed(true);
							hschanged = true;
						}
					} else {
						hschanged = false;
					}

					// move lane cover by START + Scratch
					if (key[7] | key[8]) {
						long l = System.currentTimeMillis();
						if (l - lanecovertiming > 50) {
							lanerender.setLanecover(lanerender.getLanecover() + (key[7] ? 0.001f : -0.001f));
							lanecovertiming = l;
						}
					}
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
				if (autoplay == 0) {
					boolean[] key = input.getKeystate();
					// change duration by SELECT + Scratch
					if (key[7] | key[8]) {
						long l = System.currentTimeMillis();
						if (l - lanecovertiming > 50) {
							lanerender.setGreenValue(lanerender.getGreenValue() + (key[7] ? 1 : -1));
							lanecovertiming = l;
						}
					}
					// change duration by SELECT + Keys
					if (key[0] || key[2] || key[4] || key[6]) {
						if (!hschanged) {
							lanerender.setGreenValue(lanerender.getGreenValue() -1);
							hschanged = true;
						}
					} else if (key[1] || key[3] || key[5]) {
						if (!hschanged) {
							lanerender.setGreenValue(lanerender.getGreenValue() +1);
							hschanged = true;
						}
					} else {
						hschanged = false;
					}
				}
			}
		}

		// stop playing
		if (input.isExitPressed()) {
			input.setExitPressed(false);
			player.stopPlay();
		}
		// play speed change (autoplay or replay only)
		if (autoplay == 1 || autoplay >= 3) {
			if (input.getNumberState()[1]) {
				player.setPlaySpeed(25);
			} else if (input.getNumberState()[2]) {
				player.setPlaySpeed(50);
			} else if (input.getNumberState()[3]) {
				player.setPlaySpeed(200);
			} else if (input.getNumberState()[4]) {
				player.setPlaySpeed(300);
			} else {
				player.setPlaySpeed(100);
			}
		}
	}

}
