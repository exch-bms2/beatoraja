package bms.player.beatoraja.input;

import bms.player.beatoraja.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Stream;

import bms.player.beatoraja.PlayModeConfig.*;
import bms.player.beatoraja.input.BMSPlayerInputDevice.Type;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;

/**
 * キーボードやコントローラからの入力を管理するクラス
 *
 * @author exch
 */
public class BMSPlayerInputProcessor {
	
	private volatile boolean enable = true;
	private volatile boolean closing;
	private InputPollingThread polling;
	private JInputControllerManager controllerManager;
	private final java.util.concurrent.atomic.AtomicBoolean devicesReleased = new java.util.concurrent.atomic.AtomicBoolean();
	private final KeyboardJudgeQueue keyboardJudgeQueue = new KeyboardJudgeQueue();
	private int[] keyboardReplayKeys = new int[0];
    private int[] controllerReplayKeys = new int[0];
    private int[] mouseScratchReplayKeys = new int[0];
	private boolean keyboardQueueFaultReported;

	private KeyBoardInputProcesseor kbinput;

	private BMControllerInputProcessor[] bminput;

	private MidiInputProcessor midiinput;
	
	private KeyLogger keylog = new KeyLogger();

	public BMSPlayerInputProcessor(Config config, PlayerConfig player) {
		this(config, player, Controllers.getControllers(), MidiInputProcessor::new);
	}

	BMSPlayerInputProcessor(Config config, PlayerConfig player, Array<Controller> controllers,
			java.util.function.Function<BMSPlayerInputProcessor, MidiInputProcessor> midiFactory) {
		Resolution resolution = config.getResolution();
		kbinput = new KeyBoardInputProcesseor(this, player.getMode14().getKeyboardConfig(), resolution);
		// Gdx.input.setInputProcessor(kbinput);
		Array<BMControllerInputProcessor> bminput = new Array<BMControllerInputProcessor>();
		controllerManager = JInputControllerManager.ownerOf(controllerPollRunner);
		try {
			for (Controller controller : controllers) {
				Logger.getGlobal().info("コントローラーを検出 : " + controller.getName());
				// FIXME:前回終了時のModeからコントローラ設定を復元
				ControllerConfig controllerConfig = Stream.of(player.getMode7().getController())
					.filter(m -> matchesControllerName(m.getName(), controller.getName())).findFirst()
					.orElse(new ControllerConfig());
				// デバイス名のユニーク化
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

			this.bminput = bminput.toArray(BMControllerInputProcessor.class);
			midiinput = midiFactory.apply(this);
			midiinput.open();
			midiinput.setConfig(new MidiConfig());

			devices = new Array<BMSPlayerInputDevice>();
			devices.add(kbinput);
			for (BMControllerInputProcessor bm : bminput) {
				devices.add(bm);
			}
			devices.add(midiinput);

			this.analogScroll = config.isAnalogScroll();
			polling = new InputPollingThread(this::poll, this::releaseDevices);
			if (controllerManager != null) controllerManager.setPollingShutdown(this::stopPolling);
		} catch (RuntimeException | Error e) {
			releaseDevices();
			throw e;
		}
	}

	public  static final int KEYSTATE_SIZE = 256;
	/**
	 * 各キーのON/OFF状態
	 * 全モードの入力が収まる大きさにしておく
	 */
	private boolean[] keystate = new boolean[KEYSTATE_SIZE];
	/**
	 * 各キーの最終更新時間
	 */
	private long[] time = new long[KEYSTATE_SIZE];

    /**
     * 選曲バーとレーンカバーのアナログスクロール
     */
    private boolean analogScroll = true;
	/**
	 * 選曲バーのアナログスクロール
	 * (各キーのアナログ状態)
	 */
	private boolean[] isAnalog = new boolean[KEYSTATE_SIZE];
	private float[] lastAnalogValue = new float[KEYSTATE_SIZE];
	private float[] currentAnalogValue = new float[KEYSTATE_SIZE];
	private long[] analogLastResetTime = new long[KEYSTATE_SIZE];

	private BMSPlayerInputDevice lastKeyDevice;
	private Array<BMSPlayerInputDevice> devices;

	private volatile long starttime;
	private long microMarginTime;

	int mousex;
	int mousey;
	int mousebutton;
	boolean mousepressed;
	boolean mousedragged;
	private boolean mouseMoved = false;

	float scrollX;
	float scrollY;

	private boolean startPressed;
	private boolean selectPressed;
	private final SkinMouseInput skinMouseInput = new SkinMouseInput();

	/** Observes mouse presses without consuming events handled by the skin or Stage. */
	public InputProcessor getSkinMouseInput() {
		return skinMouseInput;
	}

	/** Publishes mouse press edges once per render frame. */
	public void beginSkinInputFrame() {
		skinMouseInput.beginFrame();
	}

	public boolean isSkinMouseJustPressed(int button) {
		return skinMouseInput.isJustPressed(button);
	}

	static final class SkinMouseInput extends InputAdapter {
		private final boolean[] pending = new boolean[5];
		private final boolean[] justPressed = new boolean[5];

		@Override
		public boolean touchDown(int x, int y, int pointer, int button) {
			if (button >= 0 && button < pending.length) pending[button] = true;
			return false;
		}

		void beginFrame() {
			System.arraycopy(pending, 0, justPressed, 0, pending.length);
			Arrays.fill(pending, false);
		}

		boolean isJustPressed(int button) {
			return button >= 0 && button < justPressed.length && justPressed[button];
		}
	}

	private Type type = Type.KEYBOARD;

	public void setKeyboardConfig(KeyboardConfig config) {
		kbinput.setConfig(config);
	}

	public void setControllerConfig(ControllerConfig[] configs) {
		synchronized (kbinput) {
			boolean[] b = new boolean[configs.length];
			for (BMControllerInputProcessor controller : bminput) {
				controller.setEnable(false);
				for(int i = 0;i < configs.length;i++) {
					if(b[i]) {
						continue;
					}
					if(configs[i].getName() == null || configs[i].getName().length() == 0) {
						configs[i].setName(controller.getName());
					}
					if(matchesControllerName(configs[i].getName(), controller.getName())) {
						controller.setConfig(configs[i]);
						controller.setEnable(true);
						b[i] = true;
						break;
					}
				}
			}
		}
	}

	public void setMidiConfig(MidiConfig config) {
		midiinput.setConfig(config);
	}

	public void setStartTime(long starttime) {
		synchronized (kbinput) {
			this.starttime = starttime;
			keyboardJudgeQueue.synchronizeHeld(keystate);
			if (starttime != 0) {
				resetAllKeyChangedTime();
				keylog.clear();
				kbinput.clear();
				for (BMControllerInputProcessor bm : bminput) bm.clear();
			}
		}
		midiinput.setStartTime(starttime);
	}
	
	public void setKeyLogMarginTime(long milliMarginTime) {
		microMarginTime = milliMarginTime * 1000;
	}

	public long getStartTime() {
		return starttime;
	}

	/**
	 * 指定のキーIDのキー状態を返す
	 * @param id キーID
	 * @return 押されていればtrue
	 */
	public boolean getKeyState(int id) {
		return id >= 0 && id < keystate.length ? keystate[id] : false;
	}
	
	/**
	 * 指定のキーIDのキー状態を設定する
	 * @param id キーID
	 * @param pressed キー状態
	 * @param time キー状態の変更時間
	 */
	public void setKeyState(int id, boolean pressed, long time) {
		synchronized (keyboardJudgeQueue) {
			if(id >= 0 && id < keystate.length) {
				keystate[id] = pressed;
				this.time[id] = time;
				keyboardJudgeQueue.offer(id, pressed, time);
				checkKeyboardQueueOverflow();
			}
		}
	}
	
	/**
	 * 指定のキーIDのキー状態変更時間を返す
	 * @param id キーID
	 * @return キー状態の変更時間。変更されていない場合はLong.MIN_VALUE
	 */
	public long getKeyChangedTime(int id) {
		long result = id >= 0 && id < time.length ? time[id] : Long.MIN_VALUE;
		if (result != Long.MIN_VALUE && Gdx.input instanceof PollingLwjgl3Input input && input.getDiagnostics() != null)
			input.getDiagnostics().gameRead(id, System.nanoTime());
		return result;
	}

	/**
	 * 指定のキーIDのキー状態変更時間をリセットする
	 * @param id キーID
	 * @return キー状態の変更時間が設定されていればtrue
	 */
	public boolean resetKeyChangedTime(int id) {
		if(id >= 0 && id < time.length) {
			boolean result = time[id] != Long.MIN_VALUE;
			if (Gdx.input instanceof PollingLwjgl3Input input && input.getDiagnostics() != null)
				input.getDiagnostics().gameReset(id);
			time[id] = Long.MIN_VALUE;
			return result;
		}
		return false;
	}
	
	/**
	 * 全てのキー状態をリセットする
	 */
	public void resetAllKeyState() {
		Arrays.fill(keystate, false);
		Arrays.fill(time, Long.MIN_VALUE);
		keyboardJudgeQueue.clear();
		if (Gdx.input instanceof PollingLwjgl3Input input && input.getDiagnostics() != null)
			input.getDiagnostics().clearGameSlots();
	}

	/**
	 * 全てのキー状態変更時間をリセットする
	 */
	public void resetAllKeyChangedTime() {
		Arrays.fill(time, Long.MIN_VALUE);
		if (Gdx.input instanceof PollingLwjgl3Input input && input.getDiagnostics() != null)
			input.getDiagnostics().clearGameSlots();
	}

	public BMSPlayerInputDevice getLastKeyChangedDevice() {
		return lastKeyDevice;
	}

	public int getNumberOfDevice() {
		return bminput.length + 1;
	}

	public void setPlayConfig(PlayModeConfig playconfig) {
		// KB, コントローラー, Midiの各ボタンについて排他的処理を実施
		int[] kbkeys = playconfig.getKeyboardConfig().getKeyAssign();
		boolean[] exclusive = new boolean[kbkeys.length];
		for(int i = kbkeys.length;i < keystate.length;i++) {
			keystate[i] = false;
			time[i] = Long.MIN_VALUE;
		}
		
		int kbcount = setPlayConfig0(kbkeys,  exclusive);
		
		int[][] cokeys = new int[playconfig.getController().length][];
		int cocount = 0;
		for(int i = 0;i < cokeys.length;i++) {
			cokeys[i] = playconfig.getController()[i].getKeyAssign();
			cocount += setPlayConfig0(cokeys[i],  exclusive);
		}
				
		MidiConfig.Input[] mikeys  = playconfig.getMidiConfig().getKeys();
		int micount = 0;
		for(int i = 0;i < mikeys.length;i++) {
			if(exclusive[i]) {
				mikeys[i] = null;
			} else {
				exclusive[i] = true;
				micount++;
			}
		}
		
		// 各デバイスにキーコンフィグをセット
		kbinput.setConfig(playconfig.getKeyboardConfig());
		setControllerConfig(playconfig.getController());
		midiinput.setConfig(playconfig.getMidiConfig());
		
		if(kbcount >= cocount && kbcount >= micount) {
			type = Type.KEYBOARD;
		} else if(cocount >= kbcount && cocount >= micount) {
			type = Type.BM_CONTROLLER;
		} else {
			type = Type.MIDI;			
		}
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
		synchronized (kbinput) {
			this.enable = enable;
			if(!enable) {
				resetAllKeyState();
				for (BMSPlayerInputDevice device : devices) {
					device.clear();
				}
			}
		}
	}

	public void beginKeyboardJudge(int[] replayKeys, boolean replay) {
        beginInputJudge(replayKeys, null, replay);
    }

    public void beginInputJudge(int[] keyboardKeys, int[] controllerKeys, boolean replay) {
        beginInputJudge(keyboardKeys, controllerKeys, null, replay);
    }

    public void beginInputJudge(int[] keyboardKeys, int[] controllerKeys, int[] mouseScratchKeys, boolean replay) {
        int[] assignedKeyboard = replay ? keyboardKeys : enable ? kbinput.getQueuedGameKeys() : new int[0];
        int[] assignedController = replay ? controllerKeys : enable
                ? Arrays.stream(bminput).flatMapToInt(c -> Arrays.stream(c.getQueuedGameKeys())).toArray() : new int[0];
        int[] assignedMouse = replay ? mouseScratchKeys : enable ? kbinput.getQueuedMouseScratchGameKeys() : new int[0];
        int[] assigned = mergeEventKeys(mergeEventKeys(assignedKeyboard, assignedController), assignedMouse);
		synchronized (keyboardJudgeQueue) {
			boolean[] initial = keystate.clone();
			if (!replay && assigned != null)
				for (int key : assigned) if (time[key] != Long.MIN_VALUE) initial[key] = !keystate[key];
			keyboardJudgeQueue.begin(assigned, initial);
            keyboardReplayKeys = mergeEventKeys(assignedKeyboard, null);
            controllerReplayKeys = mergeEventKeys(assignedController, null);
            mouseScratchReplayKeys = mergeEventKeys(assignedMouse, null);
			if (!replay)
				for (int key : assigned) if (time[key] != Long.MIN_VALUE)
					keyboardJudgeQueue.offer(key, keystate[key], time[key]);
		}
	}

    private static int[] mergeEventKeys(int[] first, int[] second) {
        return java.util.stream.IntStream.concat(first == null ? java.util.stream.IntStream.empty() : Arrays.stream(first),
                second == null ? java.util.stream.IntStream.empty() : Arrays.stream(second))
                .filter(key -> key >= 0 && key < 256).distinct().sorted().toArray();
    }

	public void endKeyboardJudge() { endInputJudge(); }
    public void endInputJudge() { keyboardJudgeQueue.end(); }
	public KeyboardJudgeQueue getKeyboardJudgeQueue() { return keyboardJudgeQueue; }
    public JudgeEventQueue getJudgeEventQueue() { return keyboardJudgeQueue; }
	public int[] getKeyboardReplayKeys() { return keyboardReplayKeys.clone(); }
    public int[] getControllerReplayKeys() { return controllerReplayKeys.clone(); }
    public int[] getMouseScratchReplayKeys() { return mouseScratchReplayKeys.clone(); }

	public boolean getJudgeKeyState(int key) {
		return keyboardJudgeQueue.isQueued(key) ? keyboardJudgeQueue.held(key) : getKeyState(key);
	}

	void keyboardKeyChanged(long presstime, int key, boolean pressed) {
        queuedKeyChanged(kbinput, presstime, key, pressed);
    }

    void controllerKeyChanged(BMControllerInputProcessor device, long presstime, int key, boolean pressed) {
        queuedKeyChanged(device, presstime, key, pressed);
    }

    void mouseScratchKeyChanged(KeyBoardInputProcesseor device, long presstime, int key, boolean pressed) {
        queuedKeyChanged(device, presstime, key, pressed);
    }

    private void queuedKeyChanged(BMSPlayerInputDevice device, long presstime, int key, boolean pressed) {
		synchronized (keyboardJudgeQueue) {
			if (enable && keystate[key] != pressed) {
				keyChanged(device, presstime, key, pressed);
				keyboardJudgeQueue.offer(key, pressed, presstime);
				checkKeyboardQueueOverflow();
			}
		}
	}

	private void checkKeyboardQueueOverflow() {
		if (keyboardJudgeQueue.isOverflowed() && !keyboardQueueFaultReported) {
			keyboardQueueFaultReported = true;
			Logger.getGlobal().severe("Input judgment queue overflow; stopping to avoid invalid scores");
			if (Gdx.app != null) Gdx.app.exit();
		}
	}

	public void clearQueuedKeyboardTime(int key, long eventTime, boolean pressed) {
        clearQueuedInputTime(key, eventTime, pressed);
    }

    public void clearQueuedInputTime(int key, long eventTime, boolean pressed) {
		synchronized (keyboardJudgeQueue) {
			if (time[key] == eventTime && keystate[key] == pressed) resetKeyChangedTime(key);
		}
	}
	
	public boolean getControlKeyState(ControlKeys key) {
		return kbinput.getKeyState(key.keycode);
	}

	public boolean isControlKeyPressed(ControlKeys key) {
		return kbinput.isKeyPressed(key.keycode);
	}

	public boolean isControlKeyPressed(ControlKeys key, int heldModifiers, int... notHeldModifiers) {
		return kbinput.isKeyPressed(key.keycode, heldModifiers, notHeldModifiers);
	}
	
	protected void keyChanged(BMSPlayerInputDevice device, long presstime, int i, boolean pressed) {
		if (!enable) {
			return;
		}
		if (keystate[i] != pressed) {
			if (device instanceof BMControllerInputProcessor controller && controller.getDiagnostics() != null)
				controller.getDiagnostics().gameWrite(time[i] != Long.MIN_VALUE);
			if (device == kbinput && Gdx.input instanceof PollingLwjgl3Input input && input.getDiagnostics() != null)
				input.getDiagnostics().gameWrite(i, time[i] != Long.MIN_VALUE, System.nanoTime());
			keystate[i] = pressed;
			time[i] = presstime;
			lastKeyDevice = device;
			if (starttime != 0) {
				keylog.add(presstime - microMarginTime, i, pressed);
			}
		}
	}

	public void setAnalogState(int i, boolean _isAnalog, float _analogValue) {
		if (!enable) {
			return;
		}
		if (analogScroll) {
			isAnalog[i] = _isAnalog;
			currentAnalogValue[i] = _analogValue;
		} else {
			isAnalog[i] = false;
			currentAnalogValue[i] = 0;
		}
	}

	public void resetAnalogInput(int i) {
		lastAnalogValue[i] = currentAnalogValue[i];
		analogLastResetTime[i] = System.currentTimeMillis();
	}

	public long getTimeSinceLastAnalogReset(int i) {
		return System.currentTimeMillis() - analogLastResetTime[i];
	}

	public int getAnalogDiff(int i) {
		return BMControllerInputProcessor.computeAnalogDiff(lastAnalogValue[i], currentAnalogValue[i]);
	}

	public boolean isAnalogInput(int i) {
		return isAnalog[i];
	}

	public int getAnalogDiffAndReset(int i, int msTolerance) {
		int dTicks = 0;
        if (getTimeSinceLastAnalogReset(i) <= msTolerance) {
            dTicks = Math.max(0,getAnalogDiff(i));
        }
        resetAnalogInput(i);
        return dTicks;
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

	public boolean isActivated(KeyCommand key) {
		final int MASK_CTRL = KeyBoardInputProcesseor.MASK_CTRL;
		final int MASK_CTRL_SHIFT = KeyBoardInputProcesseor.MASK_CTRL|KeyBoardInputProcesseor.MASK_SHIFT;

		switch(key) {
		case SHOW_FPS:
			return isControlKeyPressed(ControlKeys.F1);
		case UPDATE_FOLDER:
			return isControlKeyPressed(ControlKeys.F2);
		case OPEN_EXPLORER:
			return isControlKeyPressed(ControlKeys.F3, 0, MASK_CTRL, MASK_CTRL_SHIFT);
		case COPY_SONG_MD5_HASH:
			return isControlKeyPressed(ControlKeys.F3, MASK_CTRL, MASK_CTRL_SHIFT);
		case COPY_SONG_SHA256_HASH:
			return isControlKeyPressed(ControlKeys.F3, MASK_CTRL_SHIFT);
		case SWITCH_SCREEN_MODE:
			return isControlKeyPressed(ControlKeys.F4);
		case SAVE_SCREENSHOT:
			return isControlKeyPressed(ControlKeys.F6);
		case ADD_FAVORITE_SONG:
			return isControlKeyPressed(ControlKeys.F8);
		case ADD_FAVORITE_CHART:
			return isControlKeyPressed(ControlKeys.F9);
		case AUTOPLAY_FOLDER:
			return isControlKeyPressed(ControlKeys.F10);
		case OPEN_IR:
			return isControlKeyPressed(ControlKeys.F11);
		case OPEN_SKIN_CONFIGURATION:
			return isControlKeyPressed(ControlKeys.F12);
		}
		return false;
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

	public boolean isMouseMoved() {
		return mouseMoved;
	}

	public void setMouseMoved(boolean mouseMoved) {
		this.mouseMoved = mouseMoved;
	}

	public int getScroll() {
		return (int) -scrollY;
	}

	public float getScrollX() {
		return scrollX;
	}

	public float getScrollY() {
		return scrollY;
	}

	public void resetScroll() {
		scrollX = scrollY = 0;
	}

	static public volatile Runnable controllerPollRunner = null;

	public void poll() {
		synchronized (kbinput) {
			if (closing) return;
			if (Gdx.input instanceof PollingLwjgl3Input input) {
				if (input.getDiagnostics() != null) input.getDiagnostics().poll(System.nanoTime());
				if (input.getFrameDiagnostics() != null) input.getFrameDiagnostics().workerPoll(System.nanoTime());
			}
			final long origin = starttime;
			final long now = System.nanoTime() / 1000 - origin;
			kbinput.poll(now, origin);

			Runnable controllerPoll = controllerPollRunner;
			if (controllerPoll != null) {
				controllerPoll.run();
			}

			for (BMControllerInputProcessor controller : bminput) {
				controller.poll(now);
			}
		}
	}

	public void startPolling() { polling.start(); }

	private boolean stopPolling() {
		closing = true;
		return polling == null || polling.stopAndJoin();
	}

	public void dispose() {
		closing = true;
		if (polling != null) polling.close();
		else releaseDevices();
	}

	private void releaseDevices() {
		if (!devicesReleased.compareAndSet(false, true)) return;
		closing = true;
		try {
			if (midiinput != null) midiinput.close();
		} finally {
			try {
				synchronized (kbinput) {
					enable = false;
					if (bminput != null) for (BMControllerInputProcessor controller : bminput) controller.setEnable(false);
					Arrays.fill(keystate, false);
					Arrays.fill(time, Long.MIN_VALUE);
					keyboardJudgeQueue.end();
				}
			} finally {
				if (controllerManager != null) controllerManager.dispose();
			}
		}
	}

	static boolean matchesControllerName(String configured, String detected) {
		if (configured == null || detected == null) return false;
		return configured.equals(detected) || configured.equals(new String(
				detected.getBytes(java.nio.charset.Charset.forName("EUC_JP")), StandardCharsets.UTF_8));
	}
	
	/**
	 * キーロガー
	 * 
	 * @author exch
	 */
	private static class KeyLogger {
		
		public static final int INITIAL_LOG_COUNT = 10000;
		
		private final Array<KeyInputLog> keylog;
		
		private final KeyInputLog[] logpool;
		private int poolindex;

		public KeyLogger() {
			keylog = new Array<KeyInputLog>(INITIAL_LOG_COUNT);
			logpool = new KeyInputLog[INITIAL_LOG_COUNT];
			clear();
		}
		
		/**
		 * キー入力ログを追加する
		 * 
		 * @param presstime キー入力時間(us)
		 * @param keycode キーコード
		 * @param pressed 押されたかどうか
		 */
		public synchronized void add(long presstime, int keycode, boolean pressed) {
			final KeyInputLog log = poolindex < logpool.length ? logpool[poolindex] : new KeyInputLog();
			poolindex++;
			log.setData(presstime, keycode, pressed);
			keylog.add(log);
		}
		
		/**
		 * キーログをクリアする
		 */
		public synchronized void clear() {
			keylog.clear();
			for(int i = 0;i < logpool.length;i++) {
				logpool[i] = new KeyInputLog();
			}
		}
		
		/**
		 * 
		 * @return
		 */
		public synchronized KeyInputLog[] toArray() {
			return keylog.toArray(KeyInputLog.class);
		}
	}
}
