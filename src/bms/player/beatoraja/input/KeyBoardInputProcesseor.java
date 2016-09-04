package bms.player.beatoraja.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

/**
 * キーボード入力処理用クラス
 * 
 * @author exch
 */
public class KeyBoardInputProcesseor implements InputProcessor {

	/**
	 * 
	 */
	private final BMSPlayerInputProcessor bmsPlayerInputProcessor;
	private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	private int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5,
			Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9 };
	private int[] cover = new int[] { Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT };
	private int[] function = new int[] { Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8,
			Keys.F9, Keys.F10, Keys.F11, Keys.F12 };
	private int[] control = new int[] { Keys.Q, Keys.W };
	private int exit = Keys.ESCAPE;
	
	private int lastPressedKey = -1;

	private Rectangle resolution;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, int[] keys, Rectangle resolution) {
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.setKeyAssign(keys);
		this.resolution = resolution;
	}

	public void setKeyAssign(int[] keys) {
		this.keys = new int[] { keys[0], keys[1], keys[2], keys[3], keys[4], keys[5], keys[6], keys[7], keys[8],
				keys[9], keys[10], keys[11], keys[12], keys[13], keys[14], keys[15], keys[16], keys[17] };
		this.control = new int[] { keys[18], keys[19] };
	}

	public boolean keyDown(int keycode) {
		setLastPressedKey(keycode);
		int presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == keycode) {
				this.bmsPlayerInputProcessor.keyChanged(presstime, i, true);
				return true;
			}
		}

		// レーンカバー
		for (int i = 0; i < this.bmsPlayerInputProcessor.cursor.length; i++) {
			if (cover[i] == keycode) {
				this.bmsPlayerInputProcessor.cursor[i] = true;
			}
		}

		if (control[0] == keycode) {
			this.bmsPlayerInputProcessor.startChanged(true);
		}
		if (control[1] == keycode) {
			this.bmsPlayerInputProcessor.setSelectPressed(true);
		}
		if (exit == keycode) {
			this.bmsPlayerInputProcessor.setExitPressed(true);
		}

		for (int i = 0; i < numbers.length; i++) {
			if (keycode == numbers[i]) {
				presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
				this.bmsPlayerInputProcessor.numberstate[i] = true;
				this.bmsPlayerInputProcessor.numtime[i] = presstime;
			}
		}

		for (int i = 0; i < function.length; i++) {
			if (keycode == function[i]) {
				presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
				this.bmsPlayerInputProcessor.functionstate[i] = true;
				this.bmsPlayerInputProcessor.functiontime[i] = presstime;
			}
		}

		return true;
	}

	public boolean keyTyped(char keycode) {
		return false;
	}

	public boolean keyUp(int keycode) {
		int presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == keycode) {
				this.bmsPlayerInputProcessor.keyChanged(presstime, i, false);
				return true;
			}
		}
		if (control[0] == keycode) {
			this.bmsPlayerInputProcessor.startChanged(false);
		}
		if (control[1] == keycode) {
			this.bmsPlayerInputProcessor.setSelectPressed(false);
		}
		if (exit == keycode) {
			this.bmsPlayerInputProcessor.setExitPressed(false);
		}

		for (int i = 0; i < this.bmsPlayerInputProcessor.cursor.length; i++) {
			if (cover[i] == keycode) {
				this.bmsPlayerInputProcessor.cursor[i] = false;
			}
		}
		for (int i = 0; i < numbers.length; i++) {
			if (keycode == numbers[i]) {
				presstime = (int) (System.currentTimeMillis() - this.bmsPlayerInputProcessor.starttime);
				this.bmsPlayerInputProcessor.numberstate[i] = false;
				this.bmsPlayerInputProcessor.numtime[i] = presstime;
			}
		}
		return true;
	}

	public boolean mouseMoved(int arg0, int arg1) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	public boolean scrolled(int amount) {
		this.bmsPlayerInputProcessor.scroll += amount;
		return false;
	}

	public boolean touchDown(int x, int y, int point, int button) {
		this.bmsPlayerInputProcessor.mousex = (int) (x * resolution.width / Gdx.graphics.getWidth());
		this.bmsPlayerInputProcessor.mousey = (int) (resolution.height - y * resolution.height / Gdx.graphics.getHeight());
		this.bmsPlayerInputProcessor.mouseconsumed = true;
		return false;
	}

	public boolean touchDragged(int x, int y, int point) {
		this.bmsPlayerInputProcessor.mousex = (int) (x * resolution.width / Gdx.graphics.getWidth());
		this.bmsPlayerInputProcessor.mousey = (int) (resolution.height - y * resolution.height / Gdx.graphics.getHeight());
		this.bmsPlayerInputProcessor.mouseconsumed = true;
		return false;
	}

	public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	public int getLastPressedKey() {
		return lastPressedKey;
	}

	public void setLastPressedKey(int lastPressedKey) {
		this.lastPressedKey = lastPressedKey;
	}
}