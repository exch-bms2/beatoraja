package bms.player.beatoraja.play;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.util.Arrays;

import bms.model.Mode;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;

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
	
	private final int autoplay;

	public ControlInputProcessor(BMSPlayer player, int autoplay) {
		this.player = player;
		this.autoplay = autoplay;
		hschanged = new boolean[player.getMainController().getInputProcessor().getKeystate().length];
		Arrays.fill(hschanged, true);
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
				if (autoplay == 0 && startpressed) {
					// change hi speed by START + Keys
					boolean[] key = input.getKeystate();
					for(int i = 0; i <= 15; i++) {
						if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && key[i]) {
							if(!hschanged[i]) {
								lanerender.changeHispeed(false);
								hschanged[i] = true;
							}
						} else if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && key[i]) {
							if(!hschanged[i]) {
								lanerender.changeHispeed(true);
								hschanged[i] = true;
							}
						} else {
							hschanged[i] = false;
						}
					}

					// move lane cover by START + Scratch
					if (key[7] || key[8] || key[16] || key[17]) {
						long l = System.currentTimeMillis();
						if (l - lanecovertiming > 50) {
							lanerender.setLanecover(lanerender.getLanecover() + (key[7] || key[16] ? 0.001f : -0.001f));
							lanecovertiming = l;
						}
					}
				} else if (autoplay == 0 && !startpressed) {
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
				if (autoplay == 0 && selectpressed) {
					boolean[] key = input.getKeystate();
					// change duration by SELECT + Keys
					if(player.getMode() == Mode.POPN_9K) {
						for(int i = 0; i <= 8; i++) {
							if (i % 2 == 1 && key[i]) {
								if(!hschanged[i]) {
									lanerender.setGreenValue(lanerender.getGreenValue() -1);
									hschanged[i] = true;
								}
							} else if (i % 2 == 0 && key[i])  {
								if(!hschanged[i]) {
									lanerender.setGreenValue(lanerender.getGreenValue() +1);
									hschanged[i] = true;
								}
							} else {
								hschanged[i] = false;
							}
						}
					} else {
						// change duration by SELECT + Scratch
						if (key[7] || key[8] || key[16] || key[17]) {
							long l = System.currentTimeMillis();
							if (l - lanecovertiming > 50) {
								lanerender.setGreenValue(lanerender.getGreenValue() + (key[7] || key[16] ? 1 : -1));
								lanecovertiming = l;
							}
						}
						// change duration by SELECT + Keys
						for(int i = 0; i <= 15; i++) {
							if ((i == 1 || i == 3 || i == 5 || i == 10 || i == 12 || i == 14) && key[i]) {
								if(!hschanged[i]) {
									lanerender.setGreenValue(lanerender.getGreenValue() -1);
									hschanged[i] = true;
								}
							} else if ((i == 0 || i == 2 || i == 4 || i == 6 || i == 9 || i == 11 || i == 13 || i == 15) && key[i])  {
								if(!hschanged[i]) {
									lanerender.setGreenValue(lanerender.getGreenValue() +1);
									hschanged[i] = true;
								}
							} else {
								hschanged[i] = false;
							}
						}
					}
				} else if (autoplay == 0 && !selectpressed) {
					Arrays.fill(hschanged, true);
				}
				selectpressed = true;
			} else {
				selectpressed = false;
			}
		}
		long now = System.currentTimeMillis();
		if((input.startPressed() && input.isSelectPressed() && now - exitpressedtime > 1000 )||
				(player.getTimer()[TIMER_ENDOFNOTE_1P] != Long.MIN_VALUE &&
				now > player.getTimer()[TIMER_ENDOFNOTE_1P] && (input.startPressed() || input.isSelectPressed()))){
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
