package bms.player.beatoraja.input;

import bms.player.beatoraja.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import bms.player.beatoraja.PlayConfig.ControllerConfig;
import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.input.BMControllerInputProcessor.BMKeys;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.Rectangle;

/**
 * キーボードやコントローラからの入力を管理するクラス
 * 
 * @author exch
 */
public class BMSPlayerInputProcessor {

	private KeyBoardInputProcesseor kbinput;

	private BMControllerInputProcessor[] bminput;

	public BMSPlayerInputProcessor(Resolution resolution) {
		kbinput = new KeyBoardInputProcesseor(this, new int[] { Keys.Z, Keys.S, Keys.X, Keys.D, Keys.C, Keys.F, Keys.V,
				Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.COMMA, Keys.L, Keys.PERIOD, Keys.SEMICOLON, Keys.SLASH,
				Keys.APOSTROPHE, Keys.UNKNOWN, Keys.SHIFT_RIGHT, Keys.CONTROL_RIGHT, Keys.Q, Keys.W }, resolution);
		// Gdx.input.setInputProcessor(kbinput);
		List<BMControllerInputProcessor> bminput = new ArrayList<BMControllerInputProcessor>();
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
			BMControllerInputProcessor bm = new BMControllerInputProcessor(this, controller, new int[] {
					BMKeys.BUTTON_3, BMKeys.BUTTON_6, BMKeys.BUTTON_2, BMKeys.BUTTON_7, BMKeys.BUTTON_1,
					BMKeys.BUTTON_4, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN, BMKeys.BUTTON_8, BMKeys.BUTTON_9 });
			// controller.addListener(bm);
			bminput.add(bm);
		}
                
		this.bminput = bminput.toArray(new BMControllerInputProcessor[0]);
	}

	/**
	 * 各キーのON/OFF状態
	 */
	private boolean[] keystate = new boolean[18];
	/**
	 * 各キーの最終更新時間 TODO これを他クラスから編集させない方がいいかも
	 */
	private long[] time = new long[18];

	private int lastKeyDevice;
	private int[] disableDevice = new int[0];
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

	int mousex;
	int mousey;
	int mousebutton;
	boolean mousepressed;
	boolean mousedragged;

	int scroll;

	private List<KeyInputLog> keylog = new ArrayList<KeyInputLog>(10000);

	private boolean startPressed;
	private boolean selectPressed;

	private boolean exitPressed;
        
	boolean[] cursor = new boolean[4];
	long[] cursortime = new long[4];

	public void setMinimumInputDutration(int minduration) {
		kbinput.setMinimumDuration(minduration);
		for (BMControllerInputProcessor bm : bminput) {
			bm.setMinimumDuration(minduration);
		}
	}

	public void setKeyassign(int[] keyassign) {
		kbinput.setKeyAssign(keyassign);
	}
        
	public void setControllerConfig(ControllerConfig[] configs) {
		boolean[] b = new boolean[configs.length];
		for (BMControllerInputProcessor controller : bminput) {
			int player = -1;
			for(int i = 0;i < configs.length;i++) {
				if(b[i]) {
					continue;
				}
				if(configs[i].getName() == null || configs[i].getName().length() == 0) {
					configs[i].setName(controller.getController().getName());
				}
				if(controller.getController().getName().equals(configs[i].getName())) {
					player = i;
					controller.setControllerKeyAssign(configs[i].getAssign());
					b[i] = true;
					break;
				}
			}
			if(player != -1) {
				controller.setPlayer(player);
			} else {
				controller.setPlayer(0);
				
			}
		}
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
		if (starttime != 0) {
			Arrays.fill(time, 0);
			keylog.clear();
			kbinput.clear();
			for (BMControllerInputProcessor bm : bminput) {
				bm.clear();
			}
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

	public int getLastKeyChangedDevice() {
		return lastKeyDevice;
	}

	public int getNumberOfDevice() {
		return bminput.length + 1;
	}

	public void setDisableDevice(int[] devices) {
		if(devices == null) {
			devices = new int[3];
			for(int i = 0;i < devices.length;i++) {
				devices[i] = i;
			}
			Arrays.fill(keystate, false);
			Arrays.fill(time, 0);
		}
		this.disableDevice = devices;
		for(int device : devices) {
			if(device == 0) {
				kbinput.clear();
			} else {
				for(BMControllerInputProcessor controller : bminput) {
					if(controller.getPlayer() == device - 1) {
						controller.clear();
					}
				}
			}
		}

	}

	public boolean[] getNumberState() {
		return numberstate;
	}

	public long[] getNumberTime() {
		return numtime;
	}

	public void keyChanged(int device, long presstime, int i, boolean pressed) {
		for(int disable : disableDevice) {
			if(device == disable) {
				return;
			}
		}
		if (keystate[i] != pressed) {
			keystate[i] = pressed;
			time[i] = presstime;
			lastKeyDevice = device;
			if (this.getStartTime() != 0) {
				keylog.add(new KeyInputLog((int) presstime, i, pressed));
			}
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

	public long[] getCursorTime() {
		return cursortime;
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
		return bminput;
	}

	public int getMouseX() {
		return mousex;
	}

	public int getMouseY() {
		return mousey;
	}

	public int getMouseButton() {
		return mousebutton;
	}

	public boolean isMousePressed() {
		return mousepressed;
	}

	public void setMousePressed() {
		mousepressed = false;
	}

	public boolean isMouseDragged() {
		return mousedragged;
	}

	public void setMouseDragged() {
		mousedragged = false;
	}

	public int getScroll() {
		return scroll;
	}

	public void resetScroll() {
		scroll = 0;
	}

	public void poll() {
		final long now = System.nanoTime() / 1000000 - starttime;
		kbinput.poll(now);
		for (BMControllerInputProcessor controller : bminput) {
			controller.poll(now);
		}		
	}	
}
