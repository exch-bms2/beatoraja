package bms.player.beatoraja.input;


import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

import bms.player.beatoraja.playmode.ControllerConfig;

/**
 * 弱귞뵪�궠�꺍�깉�꺆�꺖�꺀�꺖�뀯�뒟�눇�릤�뵪�궚�꺀�궧
 *
 * @author exch
 */
public class BMControllerInputProcessor extends BMSPlayerInputDevice implements ControllerListener {

	private final BMSPlayerInputProcessor bmsPlayerInputProcessor;

	private final Controller controller;
	/**
	 * �깈�깘�궎�궧�릫燁�
	 */
	private final String name;
	/**
	 * �깭�궭�꺍�궘�꺖�궋�궢�궎�꺍
	 */
	private int[] buttons = new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
			BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
	/**
	 * �궧�궭�꺖�깉�궘�꺖�궋�궢�궎�꺍
	 */
	private int start = BMKeys.BUTTON_9;
	/**
	 * �궩�꺃�궚�깉�궘�꺖�궋�궢�궎�꺍
	 */
	private int select = BMKeys.BUTTON_10;

	private float[] axis = new float[4];

	private int lastPressedButton = -1;
	
	private boolean jkoc;
	private boolean analogScratch;

	public BMControllerInputProcessor(BMSPlayerInputProcessor bmsPlayerInputProcessor, String name, Controller controller,
									  ControllerConfig controllerConfig) {
		super(Type.BM_CONTROLLER);
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.name = name;
		this.controller = controller;
		this.setConfig(controllerConfig);
	}

	public void setConfig(ControllerConfig controllerConfig) {
		this.buttons = controllerConfig.getKeys().clone();
		this.start = controllerConfig.getStart();
		this.select = controllerConfig.getSelect();
		this.jkoc = controllerConfig.getJKOC();
		this.analogScratch = controllerConfig.isAnalogScratch();
	}

	public String getName() {
		return name;
	}

	public boolean accelerometerMoved(Controller arg0, int arg1, Vector3 arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + " accelerometer moved :" + arg1 + " - "
				+ arg2.x + " " + arg2.y + " " + arg2.z);
		return false;
	}

	public boolean axisMoved(Controller arg0, int arg1, float arg2) {
		return false;
	}

	public boolean buttonDown(Controller arg0, int keycode) {
		return false;
	}

	public boolean buttonUp(Controller arg0, int keycode) {
		return false;
	}

	public void connected(Controller arg0) {
	}

	public void disconnected(Controller arg0) {
	}

	public boolean povMoved(Controller arg0, int arg1, PovDirection arg2) {
		Logger.getGlobal()
				.info("controller : " + controller.getName() + "pov moved : " + arg1 + " - " + arg2.ordinal());
		return false;
	}

	public boolean xSliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + "xslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	public boolean ySliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + "yslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	private final boolean[] buttonstate = new boolean[BMKeys.MAXID];
	private final boolean[] buttonchanged = new boolean[BMKeys.MAXID];
	private final long[] buttontime = new long[BMKeys.MAXID];

	private int duration = 16;

	public void clear() {
		// Arrays.fill(buttonstate, false);
		// Arrays.fill(axis, 0);
		Arrays.fill(buttonchanged, false);
		Arrays.fill(buttontime, Long.MIN_VALUE);
		lastPressedButton = -1;
	}

	/**
	 * �궧�궚�꺀�긿�긽�걶閭㏂궖�궑�꺍�궭
	 */
	private long counter = 1;
	/**
	 * �궋�깏�꺆�궛�궧�궚�꺀�긿�긽鵝띸쉰(-1<->0<->1)
	 */
	private float oldAnalogScratchX = 10;
	/**
	 * �궋�깏�꺆�궛�궧�궚�꺀�긿�긽 �뀯�뒟�깢�꺀�궛
	 */
	private boolean activeAnalogScratch = false;
	/**
	 * �궋�깏�꺆�궛�궧�궚�꺀�긿�긽 �뤂�썮邕㏂깢�꺀�궛
	 */
	private boolean rightMoveScratching = false;

	public void poll(final long presstime) {
		for (int i = 0; i < 4; i++) {
			axis[i] = controller.getAxis(i);
		}

		for (int button = 0; button < buttonstate.length; button++) {
			if (presstime >= buttontime[button] + duration) {
				final boolean prev = buttonstate[button];
				if (button <= BMKeys.BUTTON_16) {
					buttonstate[button] = controller.getButton(button);
				} else if (button == BMKeys.UP && !jkoc) {
					// �궋�깏�꺆�궛�뤂�썮邕㏂굮UP�겓�돯�굤壤볝겍�굥
					buttonstate[button] = scratchInput(BMKeys.UP);
				} else if (button == BMKeys.DOWN && !jkoc) {
					// �궋�깏�꺆�궛藥��썮邕㏂굮DOWN�겓�돯�굤壤볝겍�굥
					buttonstate[button] = scratchInput(BMKeys.DOWN);
				} else if (button == BMKeys.LEFT) {
					buttonstate[button] = (axis[0] < -0.9) || (axis[3] < -0.9);
				} else if (button == BMKeys.RIGHT) {
					buttonstate[button] = (axis[0] > 0.9) || (axis[3] > 0.9);
				}
				if (buttonchanged[button] = (prev != buttonstate[button])) {
					buttontime[button] = presstime;
				}

				if (!prev && buttonstate[button]) {
					setLastPressedButton(button);
				}
			}
		}

		for (int i = 0; i < buttons.length; i++) {
			final int button = buttons[i];
			if (button >= 0 && button < BMKeys.MAXID && buttonchanged[button]) {
				this.bmsPlayerInputProcessor.keyChanged(this, presstime, i, buttonstate[button]);
				buttonchanged[button] = false;
			}
		}

		if (start >= 0 && start < BMKeys.MAXID && buttonchanged[start]) {
			this.bmsPlayerInputProcessor.startChanged(buttonstate[start]);
			buttonchanged[start] = false;
		}
		if (select >= 0 && select < BMKeys.MAXID && buttonchanged[select]) {
			this.bmsPlayerInputProcessor.setSelectPressed(buttonstate[select]);
			buttonchanged[select] = false;
		}
	}

	private boolean scratchInput(int button) {
		if(!analogScratch) {
			if(button == BMKeys.UP) {
				return (axis[1] < -0.9) || (axis[2] < -0.9);
			} else if(button == BMKeys.DOWN){
				return (axis[1] > 0.9) || (axis[2] > 0.9);
			}
		}

		// Linux : axis[0]
		// Windows : axis[1]
		float analogScratchX = File.separatorChar == '\\' ? axis[1] :  axis[0];
		if (oldAnalogScratchX > 1) {
			oldAnalogScratchX = analogScratchX;
			activeAnalogScratch = false;
			return false;
		}

		if (oldAnalogScratchX != analogScratchX) {
			// �궋�깏�꺆�궛�궧�궚�꺀�긿�긽鵝띸쉰�겗燁삣땿�걣�쇇�뵟�걮�걼�졃�릦
			boolean nowRight = false;
			if (oldAnalogScratchX < analogScratchX) {
				nowRight = true;
				if ((analogScratchX - oldAnalogScratchX) > (1 - analogScratchX + oldAnalogScratchX)) {
					nowRight = false;
				}
			} else if (oldAnalogScratchX > analogScratchX) {
				nowRight = false;
				if ((oldAnalogScratchX - analogScratchX) > ((analogScratchX + 1) - oldAnalogScratchX)) {
					nowRight = true;
				}
			}

			if (activeAnalogScratch && !(rightMoveScratching == nowRight)) {
				// 藥��썮邕™넂�뤂�썮邕㏂겗�졃�릦(�뤂�썮邕™넂藥��썮邕㏂겘�ㅳ겗鸚됪쎍�걣�겒�걚)
				rightMoveScratching = nowRight;
			} else if (!activeAnalogScratch) {
				// 燁삣땿�꽒�걮�넂�썮邕㏂겗�졃�릦
				activeAnalogScratch = true;
				rightMoveScratching = nowRight;
			}

			counter = 0;
			oldAnalogScratchX = analogScratchX;
		}

		// counter > 100 ... Stop Scratching.
		if (counter > 100 && activeAnalogScratch) {
			activeAnalogScratch = false;
			counter = 0;
		}

		if (counter == Long.MAX_VALUE) {
			counter = 0;
		}

		counter++;

		if(button == BMKeys.UP) {
			return activeAnalogScratch && rightMoveScratching;
		} else if(button == BMKeys.DOWN){
			return activeAnalogScratch && !rightMoveScratching;
		} else {
			return false;
		}
	}

	public int getLastPressedButton() {
		return lastPressedButton;
	}

	public void setLastPressedButton(int lastPressedButton) {
		this.lastPressedButton = lastPressedButton;
	}

	public void setMinimumDuration(int duration) {
		this.duration = duration;
	}

	public static class BMKeys {

		public static final int BUTTON_1 = 0;
		public static final int BUTTON_2 = 1;
		public static final int BUTTON_3 = 2;
		public static final int BUTTON_4 = 3;
		public static final int BUTTON_5 = 4;
		public static final int BUTTON_6 = 5;
		public static final int BUTTON_7 = 6;
		public static final int BUTTON_8 = 7;
		public static final int BUTTON_9 = 8;
		public static final int BUTTON_10 = 9;
		public static final int BUTTON_11 = 10;
		public static final int BUTTON_12 = 11;
		public static final int BUTTON_13 = 12;
		public static final int BUTTON_14 = 13;
		public static final int BUTTON_15 = 14;
		public static final int BUTTON_16 = 15;
		public static final int UP = 16;
		public static final int DOWN = 17;
		public static final int LEFT = 18;
		public static final int RIGHT = 19;
		
		public static final int MAXID = 20;

		/**
		 * 弱귙궠�꺍�겗�궘�꺖�궠�꺖�깋�겓野얍퓶�걮�걼�깇�궘�궧�깉
		 */
		private static final String[] BMCODE = { "BUTTON 1", "BUTTON 2", "BUTTON 3", "BUTTON 4", "BUTTON 5", "BUTTON 6",
				"BUTTON 7", "BUTTON 8", "BUTTON 9", "BUTTON 10", "BUTTON 11", "BUTTON 12", "BUTTON 13", "BUTTON 14",
				"BUTTON 15", "BUTTON 16", "UP", "DOWN", "LEFT", "RIGHT" };

		public static final String toString(int keycode) {
			if (keycode >= 0 && keycode < BMCODE.length) {
				return BMCODE[keycode];
			}
			return "Unknown";
		}
	}

}