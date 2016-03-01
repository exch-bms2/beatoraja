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

public class BMSPlayerInputProcessor {

	// TODO キーコンフィグの実装

	public BMSPlayerInputProcessor() {
		Gdx.input.setInputProcessor(new KeyBoardInputProcesseor());
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			controller.addListener(new BMSControllerListener());
		}
	}

	private boolean[] keystate = new boolean[18];
	private long[] time = new long[18];

	private boolean[] numberstate = new boolean[10];
	private long[] numtime = new long[10];
	private boolean[] functionstate = new boolean[12];
	private long[] functiontime = new long[12];

	private long starttime;

	private boolean enableKeyInput = true;

	private List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();

	private boolean startPressed;

	private boolean exitPressed;

	private boolean[] cursor = new boolean[4];

	public void setStartTime(long starttime) {
		this.starttime = starttime;
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

	/**
	 * キーボード入力処理用クラス
	 * 
	 * @author exch
	 */
	class KeyBoardInputProcesseor implements InputProcessor {

		private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D,
				Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
				Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH,
				Keys.COLON, Keys.BACKSLASH, Keys.SHIFT_RIGHT,
				Keys.CONTROL_RIGHT };
		private int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2,
				Keys.NUM_3 };
		private int[] cover = new int[] { Keys.UP, Keys.DOWN, Keys.LEFT,
				Keys.RIGHT };
		private int[] function = new int[] { Keys.F1, Keys.F2, Keys.F3,
				Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8, Keys.F9, Keys.F10,
				Keys.F11, Keys.F12 };
		private int[] control = new int[] { Keys.Q };
		private int exit = Keys.ESCAPE;

		public boolean keyDown(int keycode) {
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

	/**
	 * 専用コントローラー入力処理用クラス
	 * 
	 * @author exch
	 */
	class BMSControllerListener implements ControllerListener {

		private int[] buttons = new int[] { 3, 6, 2, 7, 1, 4 };
		private int start = 8;

		public boolean accelerometerMoved(Controller arg0, int arg1,
				Vector3 arg2) {
			System.out.println("accelerometer moved");
			return false;
		}

		public boolean axisMoved(Controller arg0, int arg1, float arg2) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			if (arg1 == 0 || arg1 == 3) {
				// 7ボタン目の処理、スクラッチ処理
				if (arg2 == -1.0) {
					keyChanged(presstime, 6, true);
				} else {
					keyChanged(presstime, 6, false);
				}
			} else {
				if (arg2 == -1.0) {
					keyChanged(presstime, 7, true);
					if (keystate[8]) {
						keyChanged(presstime, 8, false);
					}
				} else if (arg2 == 1.0) {
					keyChanged(presstime, 8, true);
					if (keystate[7]) {
						keyChanged(presstime, 7, false);
					}
				} else {
					if (keystate[7]) {
						keyChanged(presstime, 7, false);
					}
					if (keystate[8]) {
						keyChanged(presstime, 8, false);
					}
				}
			}
			// System.out.println("axis moved :" + arg1 + " - " + arg2);
			return false;
		}

		public boolean buttonDown(Controller arg0, int keycode) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] == keycode) {
					keyChanged(presstime, i, true);
				}
			}

			if (start == keycode) {
				startChanged(true);
			}

			System.out.println("button : " + keycode);
			return false;
		}

		public boolean buttonUp(Controller arg0, int keycode) {
			int presstime = (int) (System.currentTimeMillis() - starttime);
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] == keycode) {
					keyChanged(presstime, i, false);
				}
			}

			if (start == keycode) {
				startChanged(false);
			}

			return false;
		}

		public void connected(Controller arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public void disconnected(Controller arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public boolean povMoved(Controller arg0, int arg1, PovDirection arg2) {
			System.out.println("pov moved");
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean xSliderMoved(Controller arg0, int arg1, boolean arg2) {
			System.out.println("xslider moved");
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		public boolean ySliderMoved(Controller arg0, int arg1, boolean arg2) {
			System.out.println("yslider moved");
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

	}

}
