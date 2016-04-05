package bms.player.beatoraja.input;

import java.util.logging.Logger;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

/**
 * 専用コントローラー入力処理用クラス
 * 
 * @author exch
 */
public class BMControllerInputProcessor implements ControllerListener {

	// TODO 専コンのキーコンフィグ

	/**
		 * 
		 */
	private final BMSPlayerInputProcessor bmsPlayerInputProcessor;

	private int player = 0;

	private float[] axis = new float[4];

	private int lastPressedButton = -1;

	public BMControllerInputProcessor(BMSPlayerInputProcessor bmsPlayerInputProcessor, int player, int[] buttons) {
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.player = player;
		this.setControllerKeyAssign(buttons);
	}

	private int[] buttons = new int[] { BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2, BMKeys.BUTTON_7,
			BMKeys.BUTTON_1, BMKeys.BUTTON_4, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
	private int start = BMKeys.BUTTON_8;
	private int select = BMKeys.BUTTON_9;

	public void setControllerKeyAssign(int[] buttons) {
		this.buttons = new int[] { buttons[0], buttons[1], buttons[2], buttons[3], buttons[4], buttons[5], buttons[6],
				buttons[7], buttons[8] };
		this.start = buttons[9];
		this.select = buttons[10];
	}

	public boolean accelerometerMoved(Controller arg0, int arg1, Vector3 arg2) {
		Logger.getGlobal().info(
				"controller : " + player + " accelerometer moved :" + arg1 + " - " + arg2.x + " " + arg2.y + " "
						+ arg2.z);
		return false;
	}

	public boolean axisMoved(Controller arg0, int arg1, float arg2) {
		int presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
		if (arg1 == 0 || arg1 == 3) {
			// 7ボタン目の処理、スクラッチ処理
			if (arg2 == -1.0) {
				// LEFT
				if (axis[arg1] == 1.0) {
					buttonUp(arg0, BMKeys.RIGHT);
				}
				buttonDown(arg0, BMKeys.LEFT);
			} else if (arg2 == 1.0) {
				// RIGHT
				if (axis[arg1] == -1.0) {
					buttonUp(arg0, BMKeys.LEFT);
				}
				buttonDown(arg0, BMKeys.RIGHT);
			} else {
				if (axis[arg1] == 1.0) {
					buttonUp(arg0, BMKeys.RIGHT);
				}
				if (axis[arg1] == -1.0) {
					buttonUp(arg0, BMKeys.LEFT);
				}
			}
			axis[arg1] = arg2;
		} else {
			if (arg2 == -1.0) {
				// UP
				if (axis[arg1] == 1.0) {
					buttonUp(arg0, BMKeys.DOWN);
				}
				buttonDown(arg0, BMKeys.UP);
			} else if (arg2 == 1.0) {
				// DOWN
				if (axis[arg1] == -1.0) {
					buttonUp(arg0, BMKeys.UP);
				}
				buttonDown(arg0, BMKeys.DOWN);
			} else {
				if (axis[arg1] == 1.0) {
					buttonUp(arg0, BMKeys.DOWN);
				}
				if (axis[arg1] == -1.0) {
					buttonUp(arg0, BMKeys.UP);
				}
			}
		}
		axis[arg1] = arg2;
		// Logger.getGlobal().info("controller : " + player +"axis moved :"
		// + arg1 + " - " + arg2);
		return false;
	}

	public boolean buttonDown(Controller arg0, int keycode) {
		int presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] == keycode) {
				this.bmsPlayerInputProcessor.keyChanged(presstime, i + player * 9, true);
			}
		}
		setLastPressedButton(keycode);

		if (start == keycode) {
			this.bmsPlayerInputProcessor.startChanged(true);
		}
		if (select == keycode) {
			this.bmsPlayerInputProcessor.setSelectPressed(true);
		}

		// Logger.getGlobal().info("controller : " + player
		// +" button pressed : " + keycode + " time : " + presstime);
		return false;
	}

	public boolean buttonUp(Controller arg0, int keycode) {
		int presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] == keycode) {
				this.bmsPlayerInputProcessor.keyChanged(presstime, i + player * 9, false);
			}
		}

		if (start == keycode) {
			this.bmsPlayerInputProcessor.startChanged(false);
		}
		if (select == keycode) {
			this.bmsPlayerInputProcessor.setSelectPressed(false);
		}
		// Logger.getGlobal().info("controller : " + player
		// +" button released : " + keycode + " time : " + presstime);
		return false;
	}

	public void connected(Controller arg0) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public void disconnected(Controller arg0) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public boolean povMoved(Controller arg0, int arg1, PovDirection arg2) {
		Logger.getGlobal().info("controller : " + player + "pov moved : " + arg1 + " - " + arg2.ordinal());
		return false;
	}

	public boolean xSliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + player + "xslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	public boolean ySliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + player + "yslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	public int getLastPressedButton() {
		return lastPressedButton;
	}

	public void setLastPressedButton(int lastPressedButton) {
		this.lastPressedButton = lastPressedButton;
	}

	public static class BMKeys {
		public static final int BUTTON_0 = 0;
		public static final int BUTTON_1 = 1;
		public static final int BUTTON_2 = 2;
		public static final int BUTTON_3 = 3;
		public static final int BUTTON_4 = 4;
		public static final int BUTTON_5 = 5;
		public static final int BUTTON_6 = 6;
		public static final int BUTTON_7 = 7;
		public static final int BUTTON_8 = 8;
		public static final int BUTTON_9 = 9;
		public static final int UP = 10;
		public static final int DOWN = 11;
		public static final int LEFT = 12;
		public static final int RIGHT = 13;
	}

}