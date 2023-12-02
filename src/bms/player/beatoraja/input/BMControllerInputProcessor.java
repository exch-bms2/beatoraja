package bms.player.beatoraja.input;


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

	public void setConfig(ControllerConfig controllerConfig) {
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
	}

	public String getName() {
		return name;
	}

	public void clear() {
		Arrays.fill(buttonchanged, false);
		Arrays.fill(buttontime, Long.MIN_VALUE);
		lastPressedButton = -1;
	}

	public void poll(final long microtime) {
		if (!enabled) return;
	
		// AXISの更新
		for (int i = 0; i < AXIS_LENGTH ; i++) {
			axis[i] = controller.getAxis(i);
		}

		for (int button = 0; button < buttonstate.length; button++) {
			if (microtime >= buttontime[button] + duration * 1000) {
				final boolean prev = buttonstate[button];
				if (button <= BMKeys.BUTTON_32) {
					buttonstate[button] = controller.getButton(button);
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
					buttontime[button] = microtime;
				}

				if (!prev && buttonstate[button]) {
					setLastPressedButton(button);
				}
			}
		}

		for (int i = 0; i < buttons.length; i++) {
			final int button = buttons[i];
			if (button >= 0 && button < BMKeys.MAXID && buttonchanged[button]) {
				this.bmsPlayerInputProcessor.keyChanged(this, microtime, i, buttonstate[button]);
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
            } else {
                this.bmsPlayerInputProcessor.setAnalogState(i, false, 0);
            }
        }
	}

    private float getAnalogValue(int button) {
        // assume isAnalog(button) == true.
        int axis_index = (button - BMKeys.AXIS1_PLUS)/2;
        boolean plus = (button - BMKeys.AXIS1_PLUS)%2 == 0;
        float value = controller.getAxis(axis_index);
        return plus ? value : -value;
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

	public int getLastPressedButton() {
		return lastPressedButton;
	}

	public void setLastPressedButton(int lastPressedButton) {
		this.lastPressedButton = lastPressedButton;
	}

	public void setEnable(boolean enabled) {
		this.enabled = enabled;
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
		public static final int BUTTON_29 = 28;
		public static final int BUTTON_30 = 29;
		public static final int BUTTON_31 = 30;
		public static final int BUTTON_32 = 31;
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
                "BUTTON 23", "BUTTON 24", "BUTTON 25", "BUTTON 26", "BUTTON 27", "BUTTON 28", "BUTTON 29", "BUTTON 30",
                "BUTTON 31", "BUTTON 32", "UP (AXIS 1 +)", "DOWN (AXIS 1 -)", "RIGHT (AXIS 2 +)", "LEFT (AXIS 2 -)",
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
    }

    private static final class AnalogScratchAlgorithmVersion1 implements AnalogScratchAlgorithm {
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