package bms.player.beatoraja.input;

import java.util.Arrays;

import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.Resolution;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.Input.Keys;

/**
 * キーボード入力処理用クラス
 * 
 * @author exch
 */
public class KeyBoardInputProcesseor extends BMSPlayerInputDevice implements InputProcessor {

	private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.BACKSLASH, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	private int[] control = new int[] { Keys.Q, Keys.W };
	/**
	 * 数字
	 */
	private final int[] numbers = { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5,
			Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9 };
	/**
	 * カーソル
	 */
	private final int[] cover = { Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT };
	/**
	 * 機能
	 */
	private final int[] function = { Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8,
			Keys.F9, Keys.F10, Keys.F11, Keys.F12 };
	/**
	 * 終了キー
	 */
	private final int exit = Keys.ESCAPE;
	/**
	 * ENTERキー
	 */
	private final int enter = Keys.ENTER;
	/**
	 * DELキー
	 */
	private final int delete = Keys.FORWARD_DEL;

	private final IntArray reserved;
	/**
	 * 最後に押されたキー
	 */
	private int lastPressedKey = -1;

	private boolean enable = true;

	/**
	 * 画面の解像度。マウスの入力イベント処理で使用
	 */
	private Resolution resolution;

	/**
	 * 各キーのon/off状態
	 */
	private boolean[] keystate = new boolean[256];
	/**
	 * 各キーの状態変化時間
	 */
	private long[] keytime = new long[256];
	/**
	 * キーの最少入力感覚
	 */
	private int duration;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, KeyboardConfig config, Resolution resolution) {
		super(bmsPlayerInputProcessor, Type.KEYBOARD);
		this.setConfig(config);
		this.resolution = resolution;
		
		reserved = new IntArray();
		reserved.addAll(cover);
		reserved.addAll(function);
		reserved.addAll(numbers);
		reserved.addAll(exit);
		reserved.addAll(enter);
	}

	public void setConfig(KeyboardConfig config) {
		this.keys = config.getKeyAssign().clone();
		this.duration = config.getDuration();
		this.control = new int[] { config.getStart(), config.getSelect() };
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

	public void clear() {
		// Arrays.fill(keystate, false);
		Arrays.fill(keytime, -duration);
		lastPressedKey = -1;
	}

	public void poll(final long presstime) {
		if (enable) {
			for (int i = 0; i < keys.length; i++) {
				if(keys[i] < 0) {
					continue;
				}
				final boolean pressed = Gdx.input.isKeyPressed(keys[i]);
				if (pressed != keystate[keys[i]] && presstime >= keytime[keys[i]] + duration) {
					keystate[keys[i]] = pressed;
					keytime[keys[i]] = presstime;
					this.bmsPlayerInputProcessor.keyChanged(this, presstime, i, pressed);
					this.bmsPlayerInputProcessor.setAnalogState(i, false, 0);
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
		final boolean enterpressed = Gdx.input.isKeyPressed(enter);
		if (enterpressed != keystate[enter]) {
			keystate[enter] = enterpressed;
			this.bmsPlayerInputProcessor.setEnterPressed(enterpressed);
		}
		final boolean deletepressed = Gdx.input.isKeyPressed(delete);
		if (deletepressed != keystate[delete]) {
			keystate[delete] = deletepressed;
			this.bmsPlayerInputProcessor.setDeletePressed(deletepressed);
		}
	}

	public boolean mouseMoved(int x, int y) {
		this.bmsPlayerInputProcessor.setMouseMoved(true);
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height / Gdx.graphics.getHeight();
		return false;
	}

	/**
	 * 旧InputProcessorのメソッド
	 * libGDX更新時に削除
	 */
	public boolean scrolled(int amount) {
		return scrolled(0, amount);
	}

	public boolean scrolled(float amountX, float amountY) {
		this.bmsPlayerInputProcessor.scrollX += amountX;
		this.bmsPlayerInputProcessor.scrollY += amountY;
		return false;
	}

	public boolean touchDown(int x, int y, int point, int button) {
		this.bmsPlayerInputProcessor.mousebutton = button;
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
		this.bmsPlayerInputProcessor.mousepressed = true;
		return false;
	}

	public boolean touchDragged(int x, int y, int point) {
		this.bmsPlayerInputProcessor.mousex = x * resolution.width / Gdx.graphics.getWidth();
		this.bmsPlayerInputProcessor.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
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

	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public boolean isReservedKey(int key) {
		return reserved.contains(key);
	}
}