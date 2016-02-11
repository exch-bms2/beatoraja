package bms.player.beatoraja.input;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

public class AbstractInputProcessor {

	private boolean[] keystate = new boolean[9];
	private long[] time = new long[9];
	
	private boolean[] numberstate = new boolean[10];
	private long[] numtime = new long[10];

	private long starttime;

	private boolean startPressed;

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
		keystate[i] = pressed;
		time[i] = presstime;
	}

	public void startChanged(boolean pressed) {
		startPressed = pressed;
	}

	public boolean startPressed() {
		return startPressed;
	}

	public void lanecoverChanged(float f) {
	}

	public void stopPlay() {
	}

	/**
	 * キーボード入力処理用クラス
	 * 
	 * @author exch
	 */
	class KeyBoardInputProcesseor implements InputProcessor {

		private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D,
				Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT };
		private int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3};
		private int[] cover = new int[] { Keys.DOWN, Keys.UP };
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
			if (cover[0] == keycode) {
				lanecoverChanged(0.01f);
			}
			if (cover[1] == keycode) {
				lanecoverChanged(-0.01f);
			}

			if (control[0] == keycode) {
				startChanged(true);
			}
			if (exit == keycode) {
				stopPlay();
			}
			
			for(int i = 0;i < numbers.length;i++) {
				if(keycode == numbers[i]) {
					presstime = (int) (System.currentTimeMillis() - starttime);
					numberstate[i] = true;
					numtime[i] = presstime;
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
			for(int i = 0;i < numbers.length;i++) {
				if(keycode == numbers[i]) {
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
				// TODO 正回転、逆回転を分ける
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
