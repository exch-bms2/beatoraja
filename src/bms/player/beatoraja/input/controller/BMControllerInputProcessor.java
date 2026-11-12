package bms.player.beatoraja.input.controller;

import bms.player.beatoraja.input.BMSPlayerInputDevice;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;


import java.util.Arrays;

import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import com.badlogic.gdx.controllers.Controller;

/**
 * 専用コントローラー入力処理用クラス
 *
 * @author exch
 */
public class BMControllerInputProcessor extends BMSPlayerInputDevice {

	// TODO アナログデバイスと設計(クラス)を分けたい

	private final Controller controller;
	/**
	 * デバイス名称
	 */
	private final String name;
	/**
	 * コントローラー使用してる
	 */
	private boolean enabled = false;
    private boolean windowActive = true, windowBaseline;
	/**
	 * ボタンキーアサイン
	 */
	private int[] buttons = new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
			BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.AXIS2_PLUS, BMKeys.AXIS1_PLUS, BMKeys.AXIS2_MINUS };
	/**
	 * スタートキーアサイン
	 */
	private int start = BMKeys.BUTTON_9;
	/**
	 * セレクトキーアサイン
	 */
	private int select = BMKeys.BUTTON_10;
	/**
	 * 各AXIS値(-1.0 - 1.0)
	 */
	private float[] axis = new float[AXIS_LENGTH];
	private static final int AXIS_LENGTH = 8;
	/**
	 * 各ボタン状態
	 */
	private final boolean[] buttonstate = new boolean[BMKeys.MAXID];
	/**
	 * 各ボタン状態に変更があったかどうか
	 */
	private final boolean[] buttonchanged = new boolean[BMKeys.MAXID];
	/**
	 * 各ボタン状態の変更時間(us)
	 */
	private final long[] buttontime = new long[BMKeys.MAXID];
	/**
	 * ボタン状態変更後の再度変更を受け付ける時間(ms)
	 */
	private int duration = 16;

	/**
	 * 最後に押したボタン
	 */
	private int lastPressedButton = -1;
	private DeviceButtonCapture deviceButtonCapture;
	private boolean deviceSelectionActive;
	private long selectionReleasedAt = Long.MIN_VALUE;
    private long inputGeneration = -1;
    private int gatedSamples, acceptedEdges, forwardedEdges;
	/**
	 * JKOC_HACK (UP,DOWNの誤反応対策用？)
 	 */
	private boolean jkoc;
	/**
	 * アナログ皿のアルゴリズム (null=アナログ皿を通用しない)
	 */
	AnalogScratchAlgorithm[] analogScratchAlgorithm = null;

    /**
     * tick: 皿の最小の動き
     * INFINITAS, DAO, YuanCon -> 0.00787
     * arcin board -> 0.00784
     */
    private static final float TICK_MAX_SIZE = 0.009f;

	public BMControllerInputProcessor(BMSPlayerInputProcessor bmsPlayerInputProcessor, String name, Controller controller,
									  ControllerConfig controllerConfig) {
		super(bmsPlayerInputProcessor, Type.BM_CONTROLLER);
		this.name = name;
		this.controller = controller;
		this.setConfig(controllerConfig);
	}

	public synchronized void setConfig(ControllerConfig controllerConfig) {
        if (controller instanceof QueuedController queued) queued.configure(controllerConfig);
		this.buttons = controllerConfig.getKeyAssign().clone();
		this.start = controllerConfig.getStart();
		this.select = controllerConfig.getSelect();
		this.duration = controllerConfig.getDuration();
		this.jkoc = controllerConfig.getJKOC();

		if (controllerConfig.isAnalogScratch()) {
			final AnalogScratchAlgorithm[] analogScratchAlgorithm = new AnalogScratchAlgorithm[AXIS_LENGTH];
			int analogScratchThreshold = controllerConfig.getAnalogScratchThreshold();
            for (int i = 0; i < AXIS_LENGTH; i++) {
    			switch (controllerConfig.getAnalogScratchMode()) {
    				case ControllerConfig.ANALOG_SCRATCH_VER_1:
    					analogScratchAlgorithm[i] = new AnalogScratchAlgorithmVersion1(analogScratchThreshold);
    					break;
    				case ControllerConfig.ANALOG_SCRATCH_VER_2:
    					analogScratchAlgorithm[i] = new AnalogScratchAlgorithmVersion2(analogScratchThreshold);
    					break;
    			}
            }
			this.analogScratchAlgorithm = analogScratchAlgorithm;
		} else {
			this.analogScratchAlgorithm = null;
		}
        resetEvents();
	}

	public String getName() {
		return name;
	}

    public Controller getController() { return controller; }

	public synchronized void clear() {
		Arrays.fill(buttonchanged, false);
		Arrays.fill(buttontime, Long.MIN_VALUE);
		lastPressedButton = -1;
        resetEvents();
	}

	public synchronized void poll(final long microtime) {
		if (!enabled || !windowActive) return;
        var queued = rawController();
        boolean rebaseline = queued != null && connectionGeneration != queued.connectionGeneration();
        if (deviceButtonCapture != null && queued != null) {
            if (windowBaseline || rebaseline) deviceButtonCapture.reset();
            deviceButtonCapture.sample(queued);
            if (deviceButtonCapture.anyDown()) selectionReleasedAt = Long.MIN_VALUE;
            else if (selectionReleasedAt == Long.MIN_VALUE) selectionReleasedAt = microtime;
            else if (!deviceSelectionActive && microtime >= selectionReleasedAt + duration * 1000L)
                deviceButtonCapture = null;
        }
        if (rebaseline) {
            connectionGeneration = queued.connectionGeneration();
            inputGeneration = queued.inputGeneration();
            releaseDisconnected(microtime);
            int event;
            while ((event = queued.events().pollNext(buttonstate, buttontime, microtime, 0)) >= 0)
                forwardButtonEvent(event / 2, event % 2 != 0, microtime);
        }
		ControllerInputDiagnostics diagnostics = getDiagnostics();
        gatedSamples = acceptedEdges = forwardedEdges = 0;
		if (diagnostics != null) diagnostics.beginBm(System.nanoTime());
	
		// AXISの更新
		for (int i = 0; i < AXIS_LENGTH ; i++) {
			axis[i] = i < controller.getAxisCount() ? controller.getAxis(i) : 0;
		}
        if (rebaseline && analogScratchAlgorithm != null)
            for (int i = 0; i < AXIS_LENGTH; i++) analogScratchAlgorithm[i].reset(axis[i]);

        if (windowBaseline) {
            windowBaseline = false;
            if (analogScratchAlgorithm != null)
                for (int i = 0; i < AXIS_LENGTH; i++) analogScratchAlgorithm[i].reset(axis[i]);
            for (int button = 0; button < buttonstate.length; button++) {
                if (button <= BMKeys.HAT1_BOTTOM) buttonstate[button] = controller.getButton(button);
                else if (jkoc) buttonstate[button] = switch (button) {
                    case BMKeys.AXIS1_PLUS -> axis[0] > 0.9 || axis[3] > 0.9;
                    case BMKeys.AXIS1_MINUS -> axis[0] < -0.9 || axis[3] < -0.9;
                    default -> false;
                };
                else buttonstate[button] = scratchInput((button - BMKeys.AXIS1_PLUS) / 2, button % 2 == 0);
            }
            Arrays.fill(buttonchanged, false);
            resetEvents();
            for (int i = 0; i < buttons.length; i++) {
                int button = buttons[i];
                if (button >= BMKeys.AXIS1_PLUS && button < BMKeys.MAXID && !jkoc && analogScratchAlgorithm != null) {
                    bmsPlayerInputProcessor.setAnalogState(i, true, getAnalogValue(button));
                    bmsPlayerInputProcessor.resetAnalogInput(i);
                }
            }
            if (diagnostics != null) diagnostics.endBm(System.nanoTime(), 0, 0, 0);
            return;
        }

        var raw = rawController();
        if (raw != null) {
            long generation = raw.inputGeneration();
            if (inputGeneration != generation) {
                inputGeneration = generation;
                Arrays.fill(buttontime, Long.MIN_VALUE);
            }
            if (raw.events().isOverflowed()) return;
            Arrays.fill(buttonchanged, 0, 32, false);
            int event;
            while ((event = raw.events().pollNext(buttonstate, buttontime, microtime, duration)) >= 0)
                forwardButtonEvent(event / 2, event % 2 != 0, microtime);
            gatedSamples += raw.events().deferredCount(buttonstate, buttontime, microtime, duration);
        }

		for (int button = 0; button < buttonstate.length; button++) {
            if (raw != null && button < 32) continue;
			if (microtime >= buttontime[button] + duration * 1000) {
				final boolean prev = buttonstate[button];
				if (button <= BMKeys.HAT1_BOTTOM) {
					buttonstate[button] = button >= controller.getMinButtonIndex()
							&& button <= controller.getMaxButtonIndex() && controller.getButton(button);
				} else {
					if (jkoc) {
						if (button == BMKeys.AXIS1_PLUS) {
							buttonstate[button] = (axis[0] > 0.9) || (axis[3] > 0.9);
						} else if (button == BMKeys.AXIS1_MINUS) {
							buttonstate[button] = (axis[0] < -0.9) || (axis[3] < -0.9);
						} else {
							buttonstate[button] = false;
						}
					} else {
						buttonstate[button] = scratchInput((button - BMKeys.AXIS1_PLUS) / 2, (button - BMKeys.AXIS1_PLUS) % 2 == 0);
					}
				}

				if (buttonchanged[button] = (prev != buttonstate[button])) {
					acceptedEdges++;
					buttontime[button] = microtime;
				}

				if (!prev && buttonstate[button]) {
					setLastPressedButton(button);
				}
			} else {
				gatedSamples++;
			}
		}

		for (int i = 0; i < buttons.length; i++) {
			final int button = buttons[i];
			if (button >= 0 && button < BMKeys.MAXID && buttonchanged[button]) {
				this.bmsPlayerInputProcessor.controllerKeyChanged(this, microtime, i, buttonstate[button]);
				forwardedEdges++;
				buttonchanged[button] = false;
			}
		}

		if (start >= 0 && start < BMKeys.MAXID && buttonchanged[start]) {
			this.bmsPlayerInputProcessor.startChanged(buttonstate[start]);
			buttonchanged[start] = false;
		}
        if (select >= 0 && select < BMKeys.MAXID && buttonchanged[select]) {
            this.bmsPlayerInputProcessor.setSelectPressed(buttonstate[select]);
            buttonchanged[select] = false;
        }

        boolean isAnalog = !jkoc && analogScratchAlgorithm != null;
        for (int i = 0; i < buttons.length; i++) {
            final int button = buttons[i];
            if (button < 0 || button >= BMKeys.MAXID) continue;
            if (isAnalog && button >= BMKeys.AXIS1_PLUS) {
                this.bmsPlayerInputProcessor.setAnalogState(i, true, getAnalogValue(button));
                if (rebaseline) this.bmsPlayerInputProcessor.resetAnalogInput(i);
            } else {
                this.bmsPlayerInputProcessor.setAnalogState(i, false, 0);
            }
        }
		if (diagnostics != null) diagnostics.endBm(System.nanoTime(), gatedSamples, acceptedEdges, forwardedEdges);
	}

    private void forwardButtonEvent(int button, boolean down, long time) {
        buttonstate[button] = down;
        buttontime[button] = time;
        acceptedEdges++;
        if (down) setLastPressedButton(button);
        for (int i = 0; i < buttons.length; i++) if (buttons[i] == button) {
            bmsPlayerInputProcessor.controllerKeyChanged(this, time, i, down);
            forwardedEdges++;
            return;
        }
        if (start == button) bmsPlayerInputProcessor.startChanged(down);
        else if (select == button) bmsPlayerInputProcessor.setSelectPressed(down);
    }

    private QueuedController rawController() {
        return controller instanceof QueuedController raw ? raw : null;
    }

    private long connectionGeneration;

    /** Unplug releases bypass debounce and enter the same judgment/replay stream as physical edges. */
    private void releaseDisconnected(long time) {
        for (int i = 0; i < buttons.length; i++) {
            int button = buttons[i];
            if (button >= 0 && button < buttonstate.length && buttonstate[button]) {
                bmsPlayerInputProcessor.controllerKeyChanged(this, time, i, false);
                bmsPlayerInputProcessor.setAnalogState(i, false, 0);
            }
        }
        if (start >= 0 && start < buttonstate.length && buttonstate[start]) bmsPlayerInputProcessor.startChanged(false);
        if (select >= 0 && select < buttonstate.length && buttonstate[select]) bmsPlayerInputProcessor.setSelectPressed(false);
        Arrays.fill(buttonstate, false);
        Arrays.fill(buttonchanged, false);
        Arrays.fill(buttontime, Long.MIN_VALUE);
        Arrays.fill(axis, 0);
        if (analogScratchAlgorithm != null) for (var algorithm : analogScratchAlgorithm) algorithm.reset(0);
        lastPressedButton = -1;
    }

    private void resetEvents() {
        var raw = rawController();
        if (raw != null) raw.resetEvents(enabled && windowActive && !windowBaseline);
    }

    public synchronized void setWindowActive(boolean active) {
        if (controller instanceof QueuedController queued) queued.windowActivityChanged();
        windowActive = active;
        windowBaseline = true;
        Arrays.fill(buttonstate, false);
        clear();
    }

    public synchronized int[] getQueuedGameKeys() {
        if (!enabled || rawController() == null) return new int[0];
        return java.util.stream.IntStream.range(0, buttons.length)
                .filter(i -> buttons[i] >= 0 && buttons[i] < BMKeys.MAXID).toArray();
    }

    private float getAnalogValue(int button) {
        // assume isAnalog(button) == true.
        int axis_index = (button - BMKeys.AXIS1_PLUS)/2;
        boolean plus = (button - BMKeys.AXIS1_PLUS)%2 == 0;
        float value = axis[axis_index];
        return plus ? value : -value;
    }

    public void recordGameWrite(boolean pending) {
        var diagnostics = getDiagnostics();
        if (diagnostics != null) diagnostics.gameWrite(pending);
    }

    private ControllerInputDiagnostics getDiagnostics() {
        return controller instanceof QueuedController raw ? raw.diagnostics() : null;
    }

    public static int computeAnalogDiff(float oldValue, float newValue) {
        float analogDiff = newValue - oldValue;
        if (analogDiff > 1.0f) {
            analogDiff -= (2 + TICK_MAX_SIZE/2);
        } else if (analogDiff < -1.0f) {
            analogDiff += (2 + TICK_MAX_SIZE/2);
        }
        analogDiff /= TICK_MAX_SIZE;
        return (int)(analogDiff > 0 ? Math.ceil(analogDiff) : Math.floor(analogDiff));
    }

	private boolean scratchInput(int axisIndex, boolean plus) { //int button) {
		final AnalogScratchAlgorithm[] analogScratchAlgorithm = this.analogScratchAlgorithm;
		if (analogScratchAlgorithm == null) {
			// アナログ皿を使わない
            if (plus) {
                return axis[axisIndex] > 0.9;
            } else {
                return axis[axisIndex] < -0.9;
            }
		} else {
			// アナログ皿
            return analogScratchAlgorithm[axisIndex].analogScratchInput(axis[axisIndex], plus);
		}
	}

	public synchronized int getLastPressedButton() {
		return lastPressedButton;
	}

	public synchronized void setLastPressedButton(int lastPressedButton) {
		if (lastPressedButton >= 0 && deviceButtonCapture != null) return;
		this.lastPressedButton = lastPressedButton;
		var diagnostics = getDiagnostics();
		if (lastPressedButton >= 0 && diagnostics != null) diagnostics.traceLogicalPress(lastPressedButton);
	}

	public synchronized int takeLastPressedButton() {
		int button = lastPressedButton;
		lastPressedButton = -1;
		return button;
	}

	public synchronized void setDeviceSelectionCapture(boolean enabled) {
		deviceSelectionActive = enabled && rawController() != null;
		if (deviceSelectionActive) {
			deviceButtonCapture = new DeviceButtonCapture();
			selectionReleasedAt = Long.MIN_VALUE;
		}
		lastPressedButton = -1;
	}

	public synchronized int takeDeviceSelectionButton() {
		return !deviceSelectionActive ? -1 : deviceButtonCapture.take();
	}

	public void setAssignmentTrace(boolean enabled) {
		var diagnostics = getDiagnostics();
		if (diagnostics != null) diagnostics.setAssignmentTrace(enabled);
	}

	public String getAssignmentTraceId() {
		var diagnostics = getDiagnostics();
		return diagnostics != null ? diagnostics.traceId() : controller.getUniqueId();
	}

	public synchronized void setEnable(boolean enabled) {
		this.enabled = enabled;
        resetEvents();
	}

	public static class BMKeys {

		public static final int BUTTON_1 = 0;
		public static final int BUTTON_2 = 1;
		public static final int BUTTON_3 = 2;
		public static final int BUTTON_4 = 3;
		public static final int BUTTON_5 = 4;
		public static final int BUTTON_6 = 5;
		public static final int BUTTON_7 = 6;
		public static final int BUTTON_8 = 7;
		public static final int BUTTON_9 = 8;
		public static final int BUTTON_10 = 9;
		public static final int BUTTON_11 = 10;
		public static final int BUTTON_12 = 11;
		public static final int BUTTON_13 = 12;
		public static final int BUTTON_14 = 13;
		public static final int BUTTON_15 = 14;
		public static final int BUTTON_16 = 15;
		public static final int BUTTON_17 = 16;
		public static final int BUTTON_18 = 17;
		public static final int BUTTON_19 = 18;
		public static final int BUTTON_20 = 19;
		public static final int BUTTON_21 = 20;
		public static final int BUTTON_22 = 21;
		public static final int BUTTON_23 = 22;
		public static final int BUTTON_24 = 23;
		public static final int BUTTON_25 = 24;
		public static final int BUTTON_26 = 25;
		public static final int BUTTON_27 = 26;
		public static final int BUTTON_28 = 27;
        public static final int HAT1_LEFT = 28;
        public static final int HAT1_TOP = 29;
        public static final int HAT1_RIGHT = 30;
        public static final int HAT1_BOTTOM = 31;
        public static final int AXIS1_PLUS = 32;
        public static final int AXIS1_MINUS = 33;
        public static final int AXIS2_PLUS = 34;
        public static final int AXIS2_MINUS = 35;
        public static final int AXIS3_PLUS = 36;
        public static final int AXIS3_MINUS = 37;
        public static final int AXIS4_PLUS = 38;
        public static final int AXIS4_MINUS = 39;
        public static final int AXIS5_PLUS = 40;
        public static final int AXIS5_MINUS = 41;
        public static final int AXIS6_PLUS = 42;
        public static final int AXIS6_MINUS = 43;
        public static final int AXIS7_PLUS = 44;
        public static final int AXIS7_MINUS = 45;
        public static final int AXIS8_PLUS = 46;
        public static final int AXIS8_MINUS = 47;
		
		public static final int MAXID = 48;

		/**
		 * 専コンのキーコードに対応したテキスト
		 */
        private static final String[] BMCODE = { "BUTTON 1", "BUTTON 2", "BUTTON 3", "BUTTON 4", "BUTTON 5", "BUTTON 6",
                "BUTTON 7", "BUTTON 8", "BUTTON 9", "BUTTON 10", "BUTTON 11", "BUTTON 12", "BUTTON 13", "BUTTON 14",
                "BUTTON 15", "BUTTON 16", "BUTTON 17", "BUTTON 18", "BUTTON 19", "BUTTON 20", "BUTTON 21", "BUTTON 22",
                "BUTTON 23", "BUTTON 24", "BUTTON 25", "BUTTON 26", "BUTTON 27", "BUTTON 28",
                "HAT 1 LEFT", "HAT 1 TOP", "HAT 1 RIGHT", "HAT 1 BOTTOM",
                "UP (AXIS 1 +)", "DOWN (AXIS 1 -)", "RIGHT (AXIS 2 +)", "LEFT (AXIS 2 -)",
                "AXIS 3 +", "AXIS 3 -", "AXIS 4 +", "AXIS 4 -", "AXIS 5 +", "AXIS 5 -", "AXIS 6 +", "AXIS 6 -", "AXIS 7 +", "AXIS 7 -", "AXIS 8 +", "AXIS 8 -" };

		public static final String toString(int keycode) {
			if (keycode >= 0 && keycode < BMCODE.length) {
				return BMCODE[keycode];
			}
			return "Unknown";
		}
	}


    private static interface AnalogScratchAlgorithm {
        public boolean analogScratchInput(float currentScratchX, boolean plus);
        void reset(float position);
    }

    private static final class AnalogScratchAlgorithmVersion1 implements AnalogScratchAlgorithm {
        @Override public void reset(float position) {
            oldScratchX = position; counter = 1; scratchActive = rightMoveScratching = false;
        }
        /**
         * アナログ皿の閾値
         */
        private int analogScratchThreshold;
        /**
         * スクラッチ停止カウンタ
         */
        private long counter = 1;
        /**
         * アナログスクラッチ位置(-1<->0<->1)
         */
        private float oldScratchX = 10;
        /**
         * アナログスクラッチ 入力フラグ
         */
        private boolean scratchActive = false;
        /**
         * アナログスクラッチ 右回転フラグ
         */
        private boolean rightMoveScratching = false;

        public AnalogScratchAlgorithmVersion1(int analogScratchThreshold) {
            this.analogScratchThreshold = analogScratchThreshold;
        }

        public boolean analogScratchInput(float currentScratchX, boolean plus) {
            if (oldScratchX > 1) {
                oldScratchX = currentScratchX;
                scratchActive = false;
                return false;
            }

            if (oldScratchX != currentScratchX) {
                // アナログスクラッチ位置の移動が発生した場合
                boolean nowRight = false;
                if (oldScratchX < currentScratchX) {
                    nowRight = true;
                    if ((currentScratchX - oldScratchX) > (1 - currentScratchX + oldScratchX)) {
                        nowRight = false;
                    }
                } else if (oldScratchX > currentScratchX) {
                    nowRight = false;
                    if ((oldScratchX - currentScratchX) > ((currentScratchX + 1) - oldScratchX)) {
                        nowRight = true;
                    }
                }

                if (scratchActive && !(rightMoveScratching == nowRight)) {
                    // 左回転→右回転の場合(右回転→左回転は値の変更がない)
                    rightMoveScratching = nowRight;
                } else if (!scratchActive) {
                    // 移動無し→回転の場合
                    scratchActive = true;
                    rightMoveScratching = nowRight;
                }

                counter = 0;
                oldScratchX = currentScratchX;
            }

            // counter > Threshold ... Stop Scratching.
            if (counter > this.analogScratchThreshold && scratchActive) {
                scratchActive = false;
                counter = 0;
            }

            if (counter == Long.MAX_VALUE) {
                counter = 0;
            }

            counter++;

            if(plus) {
                return scratchActive && rightMoveScratching;
            } else {
                return scratchActive && !rightMoveScratching;
            }
        }
    }

    private static final class AnalogScratchAlgorithmVersion2 implements AnalogScratchAlgorithm {
        @Override public void reset(float position) {
            oldScratchX = position; counter = 1; analogScratchTickCounter = 0;
            scratchActive = rightMoveScratching = false;
        }
        /**
         * アナログ皿の閾値
         */
        private int analogScratchThreshold;
        /**
         * スクラッチ停止カウンタ
         */
        private long counter = 1;
        /**
         * （モード２）閾値以内の皿の移動数(２回->スクラッチ)
         */
        private int analogScratchTickCounter = 0;
        /**
         * アナログスクラッチ位置(-1<->0<->1)
         */
        private float oldScratchX = 10;
        /**
         * アナログスクラッチ 入力フラグ
         */
        private boolean scratchActive = false;
        /**
         * アナログスクラッチ 右回転フラグ
         */
        private boolean rightMoveScratching = false;

        public AnalogScratchAlgorithmVersion2(int analogScratchThreshold) {
            this.analogScratchThreshold = analogScratchThreshold;
        }

        public boolean analogScratchInput(float currentScratchX, boolean plus) {
            if (oldScratchX > 1) {
                oldScratchX = currentScratchX;
                scratchActive = false;
                return false;
            }

            if (oldScratchX != currentScratchX) {
                // アナログスクラッチ位置の移動が発生した場合
                int ticks = computeAnalogDiff(oldScratchX, currentScratchX);
                boolean nowRight = (ticks >= 0);

                if (scratchActive && !(rightMoveScratching == nowRight)) {
                    // 左回転→右回転の場合(右回転→左回転は値の変更がない)
                    rightMoveScratching = nowRight;
                    scratchActive = false;
                    analogScratchTickCounter = 0;
                } else if (!scratchActive) {
                    // 移動無し→回転の場合
                    if (analogScratchTickCounter == 0 || counter <= this.analogScratchThreshold) {
                        analogScratchTickCounter += Math.abs(ticks);
                    }
                    // scratchActive=true
                    if (analogScratchTickCounter >= 2) {
                        scratchActive = true;
                        rightMoveScratching = nowRight;
                    }
                }

                counter = 0;
                oldScratchX = currentScratchX;
            }

            // counter > 2*Threshold ... Stop Scratching.
            if (counter > this.analogScratchThreshold*2) {
                scratchActive = false;
                analogScratchTickCounter = 0;
                counter = 0;
            }

            counter++;

            if(plus) {
                return scratchActive && rightMoveScratching;
            } else {
                return scratchActive && !rightMoveScratching;
            }
        }
    }
}
