package bms.player.beatoraja.input;

import bms.player.beatoraja.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import bms.player.beatoraja.input.BMSPlayerInputDevice.Type;
import bms.player.beatoraja.playmode.*;
import bms.player.beatoraja.PlayModeConfig;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/**
 * �궘�꺖�깭�꺖�깋�굜�궠�꺍�깉�꺆�꺖�꺀�걢�굢�겗�뀯�뒟�굮嶸←릤�걲�굥�궚�꺀�궧
 *
 * @author exch
 */
public class BMSPlayerInputProcessor {
	
	private boolean enable = true;

	private KeyBoardInputProcesseor kbinput;

	private BMControllerInputProcessor[] bminput;

	private MidiInputProcessor midiinput;
	
	private KeyLogger keylog = new KeyLogger();

	public BMSPlayerInputProcessor(Config config, PlayerConfig player) {
		Resolution resolution = config.getResolution();
		kbinput = new KeyBoardInputProcesseor(this, player.getMode14().getKeyboardConfig(), resolution);
		// Gdx.input.setInputProcessor(kbinput);
		List<BMControllerInputProcessor> bminput = new ArrayList<BMControllerInputProcessor>();
		for (Controller controller : Controllers.getControllers()) {
			Logger.getGlobal().info("�궠�꺍�깉�꺆�꺖�꺀�꺖�굮濾쒎눣 : " + controller.getName());
			// FIXME:�뎺�썮永귚틙�셽�겗Mode�걢�굢�궠�꺍�깉�꺆�꺖�꺀鼇�若싥굮孃⒴뀇
			ControllerConfig controllerConfig = Stream.of(player.getMode7().getController())
				.filter(m -> {
				    try {
					return m.getName().equals(new String(controller.getName().getBytes("EUC_JP"), "UTF-8"));
				    } catch (UnsupportedEncodingException e) {
					return false;
				    }
				}).findFirst()
				.orElse(new ControllerConfig());
			// �깈�깘�궎�궧�릫�겗�깺�깑�꺖�궚�뙑
			int index = 1;
			String name = controller.getName();
			for(BMControllerInputProcessor bm : bminput) {
				if(bm.getName().equals(name)) {
					index++;
					name = controller.getName() + "-" + index;
				}
			}
			BMControllerInputProcessor bm = new BMControllerInputProcessor(this, name, controller, controllerConfig);
			// controller.addListener(bm);
			bminput.add(bm);
		}

		this.bminput = bminput.toArray(new BMControllerInputProcessor[0]);
		midiinput = new MidiInputProcessor(this);
		midiinput.open();
		midiinput.setConfig(new MidiConfig());

		devices = new ArrayList<BMSPlayerInputDevice>();
		devices.add(kbinput);
		for (BMControllerInputProcessor bm : bminput) {
			devices.add(bm);
		}
		devices.add(midiinput);
	}



	private BMSPlayerInputDevice lastKeyDevice;
	private ArrayList<BMSPlayerInputDevice> devices;
	


	long starttime;

	int scroll;

	private boolean startPressed;
	private boolean selectPressed;

	private boolean exitPressed;
	private boolean enterPressed;
	private boolean deletePressed;

	private Key[] cursor = new Key[4];

	private Type type = Type.KEYBOARD;
	
	public void setMinimumInputDutration(int minduration) {
		kbinput.setMinimumDuration(minduration);
		for (BMControllerInputProcessor bm : bminput) {
			bm.setMinimumDuration(minduration);
		}
	}

	public void setKeyboardConfig(KeyboardConfig config) {
		kbinput.setConfig(config);
	}

	public void setControllerConfig(ControllerConfig[] configs) {
		boolean[] b = new boolean[configs.length];
		for (BMControllerInputProcessor controller : bminput) {
			for(int i = 0;i < configs.length;i++) {
				if(b[i]) {
					continue;
				}
				if(configs[i].getName() == null || configs[i].getName().length() == 0) {
					configs[i].setName(controller.getName());
				}
				if(controller.getName().equals(configs[i].getName())) {
					controller.setConfig(configs[i]);
					b[i] = true;
					break;
				}
			}
		}
	}

	public void setMidiConfig(MidiConfig config) {
		midiinput.setConfig(config);
	}

	public void setStartTime(long starttime) {
		this.starttime = starttime;
		if (starttime != 0) {
			keyData.resetKeyTime();
			keylog.clear();
			kbinput.clear();
			for (BMControllerInputProcessor bm : bminput) {
				bm.clear();
			}
		}
		midiinput.setStartTime(starttime);
	}

	// get set methods for key
	public long getStartTime() {
		return starttime;
	}

	public BMSPlayerInputDevice getLastKeyChangedDevice() {
		return lastKeyDevice;
	}

	public int getNumberOfDevice() {
		return bminput.length + 1;
	}

	public void setPlayConfig(PlayModeConfig playconfig) {
	      boolean[] exclusive = new boolean[playconfig.getKeyboardConfig().getKeyLength()];
	      
	      // KB, �궠�꺍�깉�꺆�꺖�꺀�꺖, Midi�겗�릢�깭�궭�꺍�겓�겇�걚�겍�럲餓뽫쉪�눇�릤�굮若잍뼺
	      int kbcount = countKeyboard(playconfig, exclusive);
	      int cocount = countController(playconfig, exclusive);
	      int micount = countMidi(playconfig, exclusive);
	      
	      // �릢�깈�깘�궎�궧�겓�궘�꺖�궠�꺍�깢�궍�궛�굮�궩�긿�깉
	      kbinput.setConfig(playconfig.getKeyboardConfig());
	      for(int i = 0;i < bminput.length;i++) {
	         for(ControllerConfig controller : playconfig.getController()) {
	            if(bminput[i].getName().equals(controller.getName())) {
	               bminput[i].setConfig(controller);
	               break;
	            }
	         }
	      }
	      midiinput.setConfig(playconfig.getMidiConfig());
	      
	      if(kbcount >= cocount && kbcount >= micount) {
	         type = Type.KEYBOARD;
	      } else if(cocount >= kbcount && cocount >= micount) {
	         type = Type.BM_CONTROLLER;
	      } else {
	         type = Type.MIDI;         
	      }
	   }	
	private int countMidi(PlayModeConfig playconfig, boolean[] exclusive) {
		Input[] mikeys  = playconfig.getMidiConfig().getKeys();
		int micount = 0;
		for(int i = 0;i < mikeys.length;i++) {
			if(exclusive[i]) {
				mikeys[i] = null;
			} else {
				exclusive[i] = true;
				micount++;
			}
		}
		return micount;
	}

	private int countController(PlayModeConfig playconfig, boolean[] exclusive) {
		int[][] cokeys = new int[playconfig.getController().length][];
		int cocount = 0;
		for(int i = 0;i < cokeys.length;i++) {
			cokeys[i] = playconfig.getController()[i].getKeys();
			cocount += setPlayConfig0(cokeys[i],  exclusive);
		}
		return cocount;
	}

	private int countKeyboard(PlayModeConfig playconfig, boolean[] exclusive) {
		int[] kbkeys = playconfig.getKeyboardConfig().getKeys();
		keyData.resetKeyState();
		keyData.resetKeyTime();
		
		int kbcount = setPlayConfig0(kbkeys,  exclusive);
		return kbcount;
	}
	
	public BMSPlayerInputDevice.Type getDeviceType() {
		return type;
	}
	
	private int setPlayConfig0(int[] keys, boolean[] exclusive) {
		int count = 0;
		for(int i = 0;i < keys.length;i++) {
			if(exclusive[i]) {
				keys[i] = -1;
			} else if(keys[i] != -1){
				exclusive[i] = true;
				count++;
			}
		}
		return count;
	}
	
	public void setEnable(boolean enable) {
		this.enable = enable;
		if(!enable) {
			keyData.resetKeyState();
			keyData.resetKeyTime();
			for (BMSPlayerInputDevice device : devices) {
				device.clear();
			}
		}
	}

	public void keyChanged(BMSPlayerInputDevice device, long presstime, int i, boolean pressed) {
		if (!enable) {
			return;
		}
		if (keyData.getKeyState(i) != pressed) {
			keyData.setKeyState(i, pressed);
			keyData.setKeyTime(i, presstime);
			lastKeyDevice = device;
			if (this.getStartTime() != 0) {
				keylog.add((int) presstime, i, pressed);
			}
		}
	}

	public KeyInputLog[] getKeyInputLog() {
		return keylog.toArray();
	}

	public void startChanged(boolean pressed) {
		startPressed = pressed;
	}

	public boolean startPressed() {
		return startPressed;
	}

	// methods for cursor
	public boolean getCursorState(int i) {
		return cursor[i].getIsPressed();
	}

	public long getCursorTime(int i) {
		return cursor[i].getPressTime();
	}

	public void setCursorState(int i, boolean state) {
		this.cursor[i].setState(state);
	}
	
	public void setCursorTime(int i, long state) {
		this.cursor[i].setTime(state);
	}
	
	public void resetCursorTime(int i) {
		this.cursor[i].resetTime();
	}
	
	public void setCursor(int i, boolean state, long time) {
		this.cursor[i].setState(state);
		this.cursor[i].setTime(time);
	}
	
	public boolean checkIfCursorPressed(int i) {
		return cursor[i].checkIfPressed();
	}
	

	public boolean isExitPressed() {
		return exitPressed;
	}

	public void setExitPressed(boolean exitPressed) {
		this.exitPressed = exitPressed;
	}

	public boolean isEnterPressed() {
		return enterPressed;
	}

	public void setEnterPressed(boolean enterPressed) {
		this.enterPressed = enterPressed;
	}

	public boolean isDeletePressed() {
		return deletePressed;
	}

	public void setDeletePressed(boolean deletePressed) {
		this.deletePressed = deletePressed;
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

	public MidiInputProcessor getMidiInputProcessor() {
		return midiinput;
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

	public void dispose() {
		midiinput.close();
	}
	
	static class KeyLogger {
		
		public static final int INITIAL_LOG_COUNT = 10000;
		
		public final Array<KeyInputLog> keylog;
		
		public final KeyInputLog[] logpool;
		private int poolindex;

		public KeyLogger() {
			keylog = new Array<KeyInputLog>(INITIAL_LOG_COUNT);
			logpool = new KeyInputLog[INITIAL_LOG_COUNT];
			clear();
		}
		
		public void add(int time, int keycode, boolean pressed) {
			final KeyInputLog log = poolindex < logpool.length ? logpool[poolindex] : new KeyInputLog();
			poolindex++;
			log.time = time;
			log.keycode = keycode;
			log.pressed = pressed;
			keylog.add(log);
		}
		
		public void clear() {
			keylog.clear();
			for(int i = 0;i < logpool.length;i++) {
				logpool[i] = new KeyInputLog();
			}
		}
		
		public KeyInputLog[] toArray() {
			return keylog.toArray(KeyInputLog.class);
		}
	}


}