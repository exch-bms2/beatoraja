package bms.player.beatoraja.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.BMSPlayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

/**
 * キーボードやコントローラからの入力を管理するクラス
 * 
 * @author exch
 */
public class BMSPlayerInputProcessor {

	private KeyBoardInputProcesseor kbinput;
	
	private List<BMSControllerListener> bminput = new ArrayList();
	
	public BMSPlayerInputProcessor() {
		kbinput = new KeyBoardInputProcesseor(new int[] {
				Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
				Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.COMMA, Keys.L,
				Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
				Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q,
				Keys.W });
		Gdx.input.setInputProcessor(kbinput);
		int player = 0;
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			BMSControllerListener bm = new BMSControllerListener(player, new int[] {
					BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2,
					BMKeys.BUTTON_7, BMKeys.BUTTON_1, BMKeys.BUTTON_4,
					BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8,
					BMKeys.BUTTON_9 });
			controller.addListener(bm);
			bminput.add(bm);
			if (player == 1) {
				break;
			} else {
				player++;
			}
		}
	}

	/**
	 * 各キーのON/OFF状態
	 */
	private boolean[] keystate = new boolean[18];
	/**
	 * 各キーの最終更新時間
	 * TODO これを他クラスから編集させない方がいいかも
	 */
	private long[] time = new long[18];
	/**
	 * 0-9キーのON/OFF状態
	 */
	private boolean[] numberstate = new boolean[10];
	/**
	 * 0-9キーの最終更新時間
	 */
	private long[] numtime = new long[10];
	/**
	 * F1-F12キーのON/OFF状態
	 */
	private boolean[] functionstate = new boolean[12];
	/**
	 * F1-F12キーの最終更新時間
	 */
	private long[] functiontime = new long[12];

	private long starttime;

	private boolean enableKeyInput = true;

	private List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();

	private boolean startPressed;
	private boolean selectPressed;

	private boolean exitPressed;

	private boolean[] cursor = new boolean[4];

	public void setKeyassign(int[] keyassign) {
		kbinput.setKeyAssign(keyassign);
	}
	
	public void setControllerassign(int[] buttons) {
		for(BMSControllerListener controller : bminput) {
			controller.setControllerKeyAssign(buttons);
		}
	}
	
	public void setStartTime(long starttime) {
		this.starttime = starttime;
		if(starttime != 0) {
			Arrays.fill(time, 0);
			keylog.clear();
		}
	}

	public long getStartTime() {
		return starttime;
	}

	public long[] getTime() {
		return time;
	}

	public void setTime(long[] l) {
		time = l;
	}

	public boolean[] getKeystate() {
		return keystate;
	}

	public void setKeystate(boolean[] b) {
		keystate = b;
	}

	public boolean[] getNumberState() {
		return numberstate;
	}

	public long[] getNumberTime() {
		return numtime;
	}

	public void keyChanged(int presstime, int i, boolean pressed) {
		if (enableKeyInput) {
			keystate[i] = pressed;
			time[i] = presstime;
			if (this.getStartTime() != 0) {
				keylog.add(new KeyInputLog(presstime, i, pressed));
			}
		}
	}

	public void setEnableKeyInput(boolean b) {
		enableKeyInput = b;
		if (b) {
			Arrays.fill(keystate, false);
			Arrays.fill(time, 0);
		}
	}

	public List<KeyInputLog> getKeyInputLog() {
		return keylog;
	}

	public void startChanged(boolean pressed) {
		startPressed = pressed;
	}

	public boolean startPressed() {
		return startPressed;
	}

	public boolean[] getCursorState() {
		return cursor;
	}

	public void setCursorState(boolean[] cursor) {
		this.cursor = cursor;
	}

	public boolean isExitPressed() {
		return exitPressed;
	}

	public void setExitPressed(boolean exitPressed) {
		this.exitPressed = exitPressed;
	}

	public boolean[] getFunctionstate() {
		return functionstate;
	}

	public void setFunctionstate(boolean[] functionstate) {
		this.functionstate = functionstate;
	}

	public long[] getFunctiontime() {
		return functiontime;
	}

	public void setFunctiontime(long[] functiontime) {
		this.functiontime = functiontime;
	}

	public boolean isSelectPressed() {
		return selectPressed;
	}

	public void setSelectPressed(boolean selectPressed) {
		this.selectPressed = selectPressed;
	}

	/**
	 * キーボード入力処理用クラス
	 * 
	 * @author exch
	 */
	class KeyBoardInputProcesseor implements InputProcessor {

		private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D,
				Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
				Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH,
				Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT,
				Keys.CONTROL_RIGHT };
		private int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2,
				Keys.NUM_3 };
		private int[] cover = new int[] { Keys.UP, Keys.DOWN, Keys.LEFT,
				Keys.RIGHT };
		private int[] function = new int[] { Keys.F1, Keys.F2, Keys.F3,
				Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8, Keys.F9, Keys.F10,
				Keys.F11, Keys.F12 };
		private int[] control = new int[] { Keys.Q, Keys.W };
		private int exit = Keys.ESCAPE;

		public KeyBoardInputProcesseor(int[] keys) {
			this.setKeyAssign(keys);
		}
		
		public void setKeyAssign(int[] keys) {
			this.keys = new int[] { keys[0], keys[1], keys[2], keys[3],
					keys[4], keys[5], keys[6], keys[7], keys[8], keys[9],
					keys[10], keys[11], keys[12], keys[13], keys[14], keys[15],
					keys[16], keys[17] };
			this.control = new int[] { keys[18], keys[19] };			
		}

		public boolean keyDown(int keycode) {
			System.out.println(keycode);
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] == keycode) {
					keyChanged(presstime, i, true);
					return true;
				}
			}

			// レーンカバー
			for (int i = 0; i < cursor.length; i++) {
				if (cover[i] == keycode) {
					cursor[i] = true;
				}
			}

			if (control[0] == keycode) {
				startChanged(true);
			}
			if (control[1] == keycode) {
				setSelectPressed(true);
			}
			if (exit == keycode) {
				setExitPressed(true);
			}

			for (int i = 0; i < numbers.length; i++) {
				if (keycode == numbers[i]) {
					presstime = (int) (System.currentTimeMillis() - starttime);
					numberstate[i] = true;
					numtime[i] = presstime;
				}
			}

			for (int i = 0; i < function.length; i++) {
				if (keycode == function[i]) {
					presstime = (int) (System.currentTimeMillis() - starttime);
					functionstate[i] = true;
					functiontime[i] = presstime;
				}
			}

			return true;
		}

		public boolean keyTyped(char keycode) {
			return false;
		}

		public boolean keyUp(int keycode) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] == keycode) {
					keyChanged(presstime, i, false);
					return true;
				}
			}
			if (control[0] == keycode) {
				startChanged(false);
			}
			if (control[1] == keycode) {
				setSelectPressed(false);
			}
			if (exit == keycode) {
				setExitPressed(false);
			}

			for (int i = 0; i < cursor.length; i++) {
				if (cover[i] == keycode) {
					cursor[i] = false;
				}
			}
			for (int i = 0; i < numbers.length; i++) {
				if (keycode == numbers[i]) {
					presstime = (int) (System.currentTimeMillis() - starttime);
					numberstate[i] = false;
					numtime[i] = presstime;
				}
			}
			return true;
		}

		public boolean mouseMoved(int arg0, int arg1) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean scrolled(int arg0) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean touchDown(int arg0, int arg1, int arg2, int arg3) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean touchDragged(int arg0, int arg1, int arg2) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}
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

	/**
	 * 専用コントローラー入力処理用クラス
	 * 
	 * @author exch
	 */
	class BMSControllerListener implements ControllerListener {

		// TODO 専コンのキーコンフィグ

		private int player = 0;

		private float[] axis = new float[4];

		public BMSControllerListener(int player, int[] buttons) {
			this.player = player;
			this.setControllerKeyAssign(buttons);
		}

		private int[] buttons = new int[] { BMKeys.BUTTON_3, BMKeys.BUTTON_6,
				BMKeys.BUTTON_2, BMKeys.BUTTON_7, BMKeys.BUTTON_1,
				BMKeys.BUTTON_4, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
		private int start = BMKeys.BUTTON_8;
		private int select = BMKeys.BUTTON_9;

		public void setControllerKeyAssign(int[] buttons) {
			this.buttons = new int[] { buttons[0], buttons[1], buttons[2],
					buttons[3], buttons[4], buttons[5], buttons[6], buttons[7],
					buttons[8] };
			this.start = buttons[9];
			this.select = buttons[10];			
		}
		
		public boolean accelerometerMoved(Controller arg0, int arg1,
				Vector3 arg2) {
			Logger.getGlobal().info("controller : " + player + " accelerometer moved :" + arg1 + " - " + arg2.x + " " + arg2.y + " " + arg2.z);
			return false;
		}

		public boolean axisMoved(Controller arg0, int arg1, float arg2) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
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
//			Logger.getGlobal().info("controller : " + player +"axis moved :" + arg1 + " - " + arg2);
			return false;
		}

		public boolean buttonDown(Controller arg0, int keycode) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] == keycode) {
					keyChanged(presstime, i + player * 9, true);
				}
			}

			if (start == keycode) {
				startChanged(true);
			}
			if (select == keycode) {
				setSelectPressed(true);
			}

//			Logger.getGlobal().info("controller : " + player +" button pressed : " + keycode + " time : " + presstime);
			return false;
		}

		public boolean buttonUp(Controller arg0, int keycode) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] == keycode) {
					keyChanged(presstime, i + player * 9, false);
				}
			}

			if (start == keycode) {
				startChanged(false);
			}
			if (select == keycode) {
				setSelectPressed(false);
			}
//			Logger.getGlobal().info("controller : " + player +" button released : " + keycode + " time : " + presstime);
			return false;
		}

		public void connected(Controller arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public void disconnected(Controller arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public boolean povMoved(Controller arg0, int arg1, PovDirection arg2) {
			Logger.getGlobal().info("controller : " + player +"pov moved : " + arg1 + " - " + arg2.ordinal());
			return false;
		}

		public boolean xSliderMoved(Controller arg0, int arg1, boolean arg2) {
			Logger.getGlobal().info("controller : " + player +"xslider moved : " + arg1 + " - " + arg2);
			return false;
		}

		public boolean ySliderMoved(Controller arg0, int arg1, boolean arg2) {
			Logger.getGlobal().info("controller : " + player +"yslider moved : " + arg1 + " - " + arg2);
			return false;
		}

	}

}
