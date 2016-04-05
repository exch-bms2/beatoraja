package bms.player.beatoraja.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;
import bms.player.beatoraja.play.BMSPlayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;

/**
 * キーボードやコントローラからの入力を管理するクラス
 * 
 * @author exch
 */
public class BMSPlayerInputProcessor {

	private KeyBoardInputProcesseor kbinput;

	private List<BMControllerInputProcessor> bminput = new ArrayList();

	public BMSPlayerInputProcessor() {
		kbinput = new KeyBoardInputProcesseor(this, new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
				Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH,
				Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W });
		Gdx.input.setInputProcessor(kbinput);
		int player = 0;
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			BMControllerInputProcessor bm = new BMControllerInputProcessor(this, player,
					new int[] { BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2, BMKeys.BUTTON_7, BMKeys.BUTTON_1,
							BMKeys.BUTTON_4, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8, BMKeys.BUTTON_9 });
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
	 * 各キーの最終更新時間 TODO これを他クラスから編集させない方がいいかも
	 */
	private long[] time = new long[18];
	/**
	 * 0-9キーのON/OFF状態
	 */
	boolean[] numberstate = new boolean[10];
	/**
	 * 0-9キーの最終更新時間
	 */
	long[] numtime = new long[10];
	/**
	 * F1-F12キーのON/OFF状態
	 */
	boolean[] functionstate = new boolean[12];
	/**
	 * F1-F12キーの最終更新時間
	 */
	long[] functiontime = new long[12];

	long starttime;

	private boolean enableKeyInput = true;

	private List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();

	private boolean startPressed;
	private boolean selectPressed;

	private boolean exitPressed;

	boolean[] cursor = new boolean[4];

	public void setKeyassign(int[] keyassign) {
		kbinput.setKeyAssign(keyassign);
	}

	public void setControllerassign(int[] buttons) {
		for (BMControllerInputProcessor controller : bminput) {
			controller.setControllerKeyAssign(buttons);
		}
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
		if (starttime != 0) {
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
	
	public KeyBoardInputProcesseor getKeyBoardInputProcesseor() {
		return kbinput;
	}
	
	public BMControllerInputProcessor[] getBMInputProcessor() {
		return bminput.toArray(new BMControllerInputProcessor[0]);
	}

}
