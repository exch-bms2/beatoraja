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

	public static final int MASK_SHIFT = 1 << 0;
	public static final int MASK_CTRL = 1 << 1;
	public static final int MASK_ALT = 1 << 2;

	private int[] keys = new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V, Keys.SHIFT_LEFT,
			Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH, Keys.APOSTROPHE,
			Keys.BACKSLASH, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT };
	private int[] control = new int[] { Keys.Q, Keys.W };

	private MouseScratchInput mouseScratchInput;

	private final IntArray reserved;
	/**
	 * 最後に押されたキー
	 */
	private int lastPressedKey = -1;

	private boolean textmode = false;

	/**
	 * 画面の解像度。マウスの入力イベント処理で使用
	 */
	private Resolution resolution;

	/**
	 * 各キーのon/off状態
	 */
	private final boolean[] keystate = new boolean[256];
	/**
	 * 各キーの状態変化時間
	 */
	private final long[] keytime = new long[256];
	/**
	 * 各キーが最後に押されたときで押されてる修飾キー
	 */
	private final int[] keymodifiers = new int[256];
	/**
	 * キーの最少入力間隔(ms)
	 */
	private int duration;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, KeyboardConfig config, Resolution resolution) {
		super(bmsPlayerInputProcessor, Type.KEYBOARD);
		this.mouseScratchInput = new MouseScratchInput(bmsPlayerInputProcessor, this, config);
		this.setConfig(config);
		this.resolution = resolution;
		
		reserved = new IntArray();
		Arrays.stream(ControlKeys.values()).forEach(keys -> reserved.add(keys.keycode));
		
		Arrays.fill(keytime, Long.MIN_VALUE);
	}

	public void setConfig(KeyboardConfig config) {
		this.keys = config.getKeyAssign().clone();
		this.duration = config.getDuration();
		this.control = new int[] { config.getStart(), config.getSelect() };
		mouseScratchInput.setConfig(config);
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
		Arrays.fill(keytime, Long.MIN_VALUE);
		lastPressedKey = -1;
		mouseScratchInput.clear();
	}

	public void poll(final long microtime) {
		if (!textmode) {
			for (int i = 0; i < keys.length; i++) {
				if(keys[i] < 0) {
					continue;
				}
				final boolean pressed = Gdx.input.isKeyPressed(keys[i]);
				if (pressed != keystate[keys[i]] && microtime >= keytime[keys[i]] + duration * 1000) {
					keystate[keys[i]] = pressed;
					keytime[keys[i]] = microtime;
					this.bmsPlayerInputProcessor.keyChanged(this, microtime, i, pressed);
					this.bmsPlayerInputProcessor.setAnalogState(i, false, 0);
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
		
		for (ControlKeys key : ControlKeys.values()) {
			final boolean pressed = Gdx.input.isKeyPressed(key.keycode);
			if (!(textmode && key.text) && pressed != keystate[key.keycode]) {
				keystate[key.keycode] = pressed;
				keytime[key.keycode] = microtime;
				keymodifiers[key.keycode] = pressed ? currentlyHeldModifiers() : 0;
			}
		}
		
		mouseScratchInput.poll(microtime);
	}

	private int currentlyHeldModifiers() {
		boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
		boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
		boolean alt = Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT);
		return (shift ? MASK_SHIFT : 0) | (ctrl ? MASK_CTRL : 0) | (alt ? MASK_ALT : 0);
	}


	public boolean getKeyState(int keycode) {
		return keystate[keycode];
	}
	
	protected void setKeyState(int keycode, boolean pressed) {
		keystate[keycode] = pressed;
	}

	public boolean isKeyPressed(int keycode) {
		if(keystate[keycode] && keytime[keycode] != Long.MIN_VALUE) {
			keytime[keycode] = Long.MIN_VALUE;
			return true;
		}
		return false;
	}

	public boolean isKeyPressed(int keycode, int heldModifiers, int... notHeldModifiers) {
		if(keystate[keycode] && keytime[keycode] != Long.MIN_VALUE) {
			int modifiers = keymodifiers[keycode];
			if ((modifiers & heldModifiers) != heldModifiers) return false;
			for (int i = 0; i < notHeldModifiers.length; i++) {
				if ((modifiers & notHeldModifiers[i]) == notHeldModifiers[i]) return false;
			}
			keytime[keycode] = Long.MIN_VALUE;
			return true;
		}
		return false;
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

	public MouseScratchInput getMouseScratchInput() {
		return mouseScratchInput;
	}

	public void setTextInputMode(boolean textmode) {
		this.textmode = textmode;
	}
	
	public boolean isReservedKey(int key) {
		return reserved.contains(key);
	}
	
	public enum ControlKeys {
		NUM0(0, Keys.NUM_0, true),
		NUM1(1, Keys.NUM_1, true),
		NUM2(2, Keys.NUM_2, true),
		NUM3(3, Keys.NUM_3, true),
		NUM4(4, Keys.NUM_4, true),
		NUM5(5, Keys.NUM_5, true),
		NUM6(6, Keys.NUM_6, true),
		NUM7(7, Keys.NUM_7, true),
		NUM8(8, Keys.NUM_8, true),
		NUM9(9, Keys.NUM_9, true),
		
		F1(10, Keys.F1, false),
		F2(11, Keys.F2, false),
		F3(12, Keys.F3, false),
		F4(13, Keys.F4, false),
		F5(14, Keys.F5, false),
		F6(15, Keys.F6, false),
		F7(16, Keys.F7, false),
		F8(17, Keys.F8, false),
		F9(18, Keys.F9, false),
		F10(19, Keys.F10, false),
		F11(20, Keys.F11, false),
		F12(21, Keys.F12, false),
		
		UP(22, Keys.UP, false),
		DOWN(23, Keys.DOWN, false),
		LEFT(24, Keys.LEFT, false),
		RIGHT(25, Keys.RIGHT, false),
		
		ENTER(26, Keys.ENTER, false),
		DEL(27, Keys.FORWARD_DEL, false),
		ESCAPE(28, Keys.ESCAPE, false),
		;
		
		public final int id;
		
		public final int keycode;
		
		public final boolean text;
		
		private ControlKeys(int id, int keycode, boolean text) {
			this.id = id;
			this.keycode = keycode;
			this.text = text;
		}
	}

}