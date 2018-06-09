package bms.player.beatoraja.input;

import java.util.Arrays;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.playmode.KeyboardConfig;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.Input.Keys;

/**
 * �궘�꺖�깭�꺖�깋�뀯�뒟�눇�릤�뵪�궚�꺀�궧
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
	private int[] control = new int[] { Keys.Q, Keys.W };
	/**
	 * �빊耶�
	 */
	private final int[] numbers = new int[] { Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5,
			Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9 };
	/**
	 * �궖�꺖�궫�꺂
	 */
	private final int[] cover = new int[] { Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT };
	/**
	 * 艅잒꺗
	 */
	private final int[] function = new int[] { Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6, Keys.F7, Keys.F8,
			Keys.F9, Keys.F10, Keys.F11, Keys.F12 };
	/**
	 * 永귚틙�궘�꺖
	 */
	private int exit = Keys.ESCAPE;

	private int enter = Keys.ENTER;

	private int delete = Keys.FORWARD_DEL;

	private final IntArray reserved;
	/**
	 * ��孃뚣겓�듉�걬�굦�걼�궘�꺖
	 */
	private int lastPressedKey = -1;

	private boolean enable = true;

	/**
	 * �뵽�씊�겗鰲ｅ깗佯╉�귙깯�궑�궧�겗�뀯�뒟�궎�깧�꺍�깉�눇�릤�겎鵝욜뵪
	 */
	private Resolution resolution;

	/**
	 * �릢�궘�꺖�겗on/off�듁�뀑
	 */
	private boolean[] keystate = new boolean[256];
	/**
	 * �릢�궘�꺖�겗�듁�뀑鸚됧뙑�셽�뼋
	 */
	private long[] keytime = new long[256];
	/**
	 * �궘�꺖�겗��弱묈뀯�뒟�꽏誤�
	 */
	private int duration;

	public KeyBoardInputProcesseor(BMSPlayerInputProcessor bmsPlayerInputProcessor, KeyboardConfig config, Resolution resolution) {
		super(Type.KEYBOARD);
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
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
		this.keys = config.getKeys().clone();
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
				}
			}

			for (int i = 0; i < cover.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(cover[i]);
				if (pressed != keystate[cover[i]]) {
					keystate[cover[i]] = pressed;
					this.bmsPlayerInputProcessor.setCursor(i, pressed, presstime);
				}
			}

			for (int i = 0; i < numbers.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(numbers[i]);
				if (pressed != keystate[numbers[i]]) {
					keystate[numbers[i]] = pressed;
					keyData.setNumberState(i, pressed, presstime);
				}
			}

			for (int i = 0; i < function.length; i++) {
				final boolean pressed = Gdx.input.isKeyPressed(function[i]);
				if (pressed != keystate[function[i]]) {
					keystate[function[i]] = pressed;
					keyData.setFunction(i, pressed, presstime);
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
		mouseData.setMouseMoved(true);
		mouseData.mousex = x * resolution.width / Gdx.graphics.getWidth();
		mouseData.mousey = resolution.height - y * resolution.height / Gdx.graphics.getHeight();
		return false;
	}

	public boolean scrolled(int amount) {
		this.bmsPlayerInputProcessor.scroll -= amount;
		return false;
	}

	public boolean touchDown(int x, int y, int point, int button) {
		mouseData.mousebutton = button;
		mouseData.mousex = x * resolution.width / Gdx.graphics.getWidth();
		mouseData.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
		mouseData.mousepressed = true;
		return false;
	}

	public boolean touchDragged(int x, int y, int point) {
		mouseData.mousex = x * resolution.width / Gdx.graphics.getWidth();
		mouseData.mousey = resolution.height - y * resolution.height
				/ Gdx.graphics.getHeight();
		mouseData.mousedragged = true;
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
	
	public boolean isReservedKey(int key) {
		return reserved.contains(key);
	}
}