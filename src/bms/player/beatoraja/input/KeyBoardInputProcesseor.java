package bms.player.beatoraja.input;

import java.util.Arrays;

import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Rectangle;

/**
 * キーボード入力処理用クラス
 * 
 * @author exch
 */
public class KeyBoardInputProcesseor extends BMSPlayerInputDevice implements InputProcessor {

	/**
	 * 
	 */
	private final BMSPlayerInputProcessor bmsPlayerInputProcessor;
	private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.BACKSLASH, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	private int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5,
			Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9 };
	private int[] cover = new int[] { Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT };
	private int[] function = new int[] { Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8,
			Keys.F9, Keys.F10, Keys.F11, Keys.F12 };
	private int[] control = new int[] { Keys.Q, Keys.W };
	private int exit = Keys.ESCAPE;

	private int lastPressedKey = -1;

	private boolean enable = true;

	private Resolution resolution;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, int[] keys, Resolution resolution) {
		super(Type.KEYBOARD);
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.setKeyAssign(keys);
		this.resolution = resolution;
	}

	public void setKeyAssign(int[] keys) {
		if (keys.length < 20) {
			return;
		}
		this.keys = new int[] { keys[0], keys[1], keys[2], keys[3], keys[4], keys[5], keys[6], keys[7], keys[8],
				keys[9], keys[10], keys[11], keys[12], keys[13], keys[14], keys[15], keys[16], keys[17] };
		this.control = new int[] { keys[18], keys[19] };
	}

	public boolean keyDown(int keycode) {
		setLastPressedKey(keycode);
		return true;
	}

	public boolean keyTyped(char keycode) {
		return false;
	}

	public boolean keyUp(int keycode) {
		return true;
	}

	private boolean[] keystate = new boolean[256];
	private long[] keytime = new long[256];
	private int duration;

	public void clear() {
		// Arrays.fill(keystate, false);
		Arrays.fill(keytime, -duration);
		lastPressedKey = -1;
	}

	public void poll(final long presstime) {
		if (enable) {
			for (int i = 0; i < keys.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(keys[i]);
				if (pressed != keystate[keys[i]] && presstime >= keytime[keys[i]] + duration) {
					keystate[keys[i]] = pressed;
					keytime[keys[i]] = presstime;
					this.bmsPlayerInputProcessor.keyChanged(this, presstime, i, pressed);
				}
			}

			for (int i = 0; i < cover.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(cover[i]);
				if (pressed != keystate[cover[i]]) {
					keystate[cover[i]] = pressed;
					this.bmsPlayerInputProcessor.cursor[i] = pressed;
					this.bmsPlayerInputProcessor.cursortime[i] = presstime;
				}
			}

			for (int i = 0; i < numbers.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(numbers[i]);
				if (pressed != keystate[numbers[i]]) {
					keystate[numbers[i]] = pressed;
					this.bmsPlayerInputProcessor.numberstate[i] = pressed;
					this.bmsPlayerInputProcessor.numtime[i] = presstime;
				}
			}

			for (int i = 0; i < function.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(function[i]);
				if (pressed != keystate[function[i]]) {
					keystate[function[i]] = pressed;
					this.bmsPlayerInputProcessor.functionstate[i] = pressed;
					this.bmsPlayerInputProcessor.functiontime[i] = presstime;
				}
			}

			final boolean startpressed = Gdx.input.isKeyPressed(control[0]);
			if (startpressed != keystate[control[0]]) {
				keystate[control[0]] = startpressed;
				this.bmsPlayerInputProcessor.startChanged(startpressed);
			}
			final boolean selectpressed = Gdx.input.isKeyPressed(control[1]);
			if (selectpressed != keystate[control[1]]) {
				keystate[control[1]] = selectpressed;
				this.bmsPlayerInputProcessor.setSelectPressed(selectpressed);
			}
		}
		final boolean exitpressed = Gdx.input.isKeyPressed(exit);
		if (exitpressed != keystate[exit]) {
			keystate[exit] = exitpressed;
			this.bmsPlayerInputProcessor.setExitPressed(exitpressed);
		}
	}

	public boolean mouseMoved(int arg0, int arg1) {
		return false;
	}

	public boolean scrolled(int amount) {
		this.bmsPlayerInputProcessor.scroll += amount;
		return false;
	}

	public boolean touchDown(int x, int y, int point, int button) {
		this.bmsPlayerInputProcessor.mousebutton = button;
		this.bmsPlayerInputProcessor.mousex = (int) (x * resolution.width / Gdx.graphics.getWidth());
		this.bmsPlayerInputProcessor.mousey = (int) (resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight());
		this.bmsPlayerInputProcessor.mousepressed = true;
		return false;
	}

	public boolean touchDragged(int x, int y, int point) {
		this.bmsPlayerInputProcessor.mousex = (int) (x * resolution.width / Gdx.graphics.getWidth());
		this.bmsPlayerInputProcessor.mousey = (int) (resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight());
		this.bmsPlayerInputProcessor.mousedragged = true;
		return false;
	}

	public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
		return false;
	}

	public int getLastPressedKey() {
		return lastPressedKey;
	}

	public void setLastPressedKey(int lastPressedKey) {
		this.lastPressedKey = lastPressedKey;
	}

	public void setMinimumDuration(int duration) {
		this.duration = duration;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}
}