package bms.player.beatoraja.input;


import java.io.File;
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
	 * ボタンキーアサイン
	 */
	private int[] buttons = new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
			BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
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
	private float[] axis = new float[4];
	/**
	 * 各ボタン状態
	 */
	private final boolean[] buttonstate = new boolean[BMKeys.MAXID];
	/**
	 * 各ボタン状態に変更があったかどうか
	 */
	private final boolean[] buttonchanged = new boolean[BMKeys.MAXID];
	/**
	 * 各ボタン状態の変更時間(ms)
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
	 * アナログ皿かどうか
	 */
	private boolean analogScratch;
	/**
	 * アナログ皿モード２かどうか
	 */
	private boolean analogScratchMode2;
	/**
	 * アナログ皿の閾値
	 */
	private int analogScratchThreshold;
	/**
	 * スクラッチ停止カウンタ
	 */
	private long counter = 1;
	/**
	 * （モード２）皿の閾値以内皿移動の数(２回->スクラッチ)
	 */
	private int analogScratchTickCounter = 0;
	/**
	 * アナログスクラッチ位置(-1<->0<->1)
	 */
	private float oldAnalogScratchX = 10;
	/**
	 * アナログスクラッチ 入力フラグ
	 */
	private boolean activeAnalogScratch = false;
	/**
	 * アナログスクラッチ 右回転フラグ
	 */
	private boolean rightMoveScratching = false;

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
		this.analogScratch = controllerConfig.isAnalogScratch();
		this.analogScratchMode2 = controllerConfig.isAnalogScratchMode2();
		this.analogScratchThreshold = controllerConfig.getAnalogScratchThreshold();
	}

	public String getName() {
		return name;
	}

	public void clear() {
		// Arrays.fill(buttonstate, false);
		// Arrays.fill(axis, 0);
		Arrays.fill(buttonchanged, false);
		Arrays.fill(buttontime, Long.MIN_VALUE);
		lastPressedButton = -1;
	}

	public void poll(final long presstime) {
		// AXISの更新
		for (int i = 0; i < 4; i++) {
			axis[i] = controller.getAxis(i);
		}

		for (int button = 0; button < buttonstate.length; button++) {
			if (presstime >= buttontime[button] + duration) {
				final boolean prev = buttonstate[button];
				if (button <= BMKeys.BUTTON_16) {
					buttonstate[button] = controller.getButton(button);
				} else if (button == BMKeys.UP && !jkoc) {
					// アナログ右回転をUPに割り当てる
					buttonstate[button] = scratchInput(BMKeys.UP);
				} else if (button == BMKeys.DOWN && !jkoc) {
					// アナログ左回転をDOWNに割り当てる
					buttonstate[button] = scratchInput(BMKeys.DOWN);
				} else if (button == BMKeys.LEFT) {
					buttonstate[button] = (axis[0] < -0.9) || (axis[3] < -0.9);
				} else if (button == BMKeys.RIGHT) {
					buttonstate[button] = (axis[0] > 0.9) || (axis[3] > 0.9);
				}
				if (buttonchanged[button] = (prev != buttonstate[button])) {
					buttontime[button] = presstime;
				}

				if (!prev && buttonstate[button]) {
					setLastPressedButton(button);
				}
			}
		}

		for (int i = 0; i < buttons.length; i++) {
			final int button = buttons[i];
			if (button >= 0 && button < BMKeys.MAXID && buttonchanged[button]) {
				this.bmsPlayerInputProcessor.keyChanged(this, presstime, i, buttonstate[button]);
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
	}

	private boolean analogScratchInput(int button) {
		// Linux : axis[0]
		// Windows : axis[1]
		//float analogScratchX = File.separatorChar == '\\' ? axis[1] :  axis[0];
		float analogScratchX = 0f;
		for (int i = 0; i < 4; i++) {
			if (axis[i] != 0f) {
				analogScratchX = axis[i];
				break;
			}
		}

		if (oldAnalogScratchX > 1) {
			oldAnalogScratchX = analogScratchX;
			activeAnalogScratch = false;
			return false;
		}

		if (oldAnalogScratchX != analogScratchX) {
			// アナログスクラッチ位置の移動が発生した場合
			boolean nowRight = false;
			if (oldAnalogScratchX < analogScratchX) {
				nowRight = true;
				if ((analogScratchX - oldAnalogScratchX) > (1 - analogScratchX + oldAnalogScratchX)) {
					nowRight = false;
				}
			} else if (oldAnalogScratchX > analogScratchX) {
				nowRight = false;
				if ((oldAnalogScratchX - analogScratchX) > ((analogScratchX + 1) - oldAnalogScratchX)) {
					nowRight = true;
				}
			}

			if (activeAnalogScratch && !(rightMoveScratching == nowRight)) {
				// 左回転→右回転の場合(右回転→左回転は値の変更がない)
				rightMoveScratching = nowRight;
			} else if (!activeAnalogScratch) {
				// 移動無し→回転の場合
				activeAnalogScratch = true;
				rightMoveScratching = nowRight;
			}

			counter = 0;
			oldAnalogScratchX = analogScratchX;
		}

		// counter > Threshold ... Stop Scratching.
		if (counter > this.analogScratchThreshold && activeAnalogScratch) {
			activeAnalogScratch = false;
			counter = 0;
		}

		if (counter == Long.MAX_VALUE) {
			counter = 0;
		}

		counter++;

		if(button == BMKeys.UP) {
			return activeAnalogScratch && rightMoveScratching;
		} else if(button == BMKeys.DOWN){
			return activeAnalogScratch && !rightMoveScratching;
		} else {
			return false;
		}
	}

	private boolean analogScratchInputMode2(int button) {
		// Linux : axis[0]
		// Windows : axis[1]
		//float analogScratchX = File.separatorChar == '\\' ? axis[1] :  axis[0];
		float analogScratchX = 0f;
		for (int i = 0; i < 4; i++) {
			if (axis[i] != 0f) {
				analogScratchX = axis[i];
				break;
			}
		}

		if (oldAnalogScratchX > 1) {
			oldAnalogScratchX = analogScratchX;
			activeAnalogScratch = false;
			return false;
		}

		if (oldAnalogScratchX != analogScratchX) {
			// アナログスクラッチ位置の移動が発生した場合
			boolean nowRight = false;
			if (oldAnalogScratchX < analogScratchX) {
				nowRight = true;
				if ((analogScratchX - oldAnalogScratchX) > (1 - analogScratchX + oldAnalogScratchX)) {
					nowRight = false;
				}
			} else if (oldAnalogScratchX > analogScratchX) {
				nowRight = false;
				if ((oldAnalogScratchX - analogScratchX) > ((analogScratchX + 1) - oldAnalogScratchX)) {
					nowRight = true;
				}
			}

			if (activeAnalogScratch && !(rightMoveScratching == nowRight)) {
				// 左回転→右回転の場合(右回転→左回転は値の変更がない)
				rightMoveScratching = nowRight;
				activeAnalogScratch = false;
				analogScratchTickCounter = 0;
			} else if (!activeAnalogScratch) {
				// 移動無し→回転の場合
				if (analogScratchTickCounter == 0 || counter <= this.analogScratchThreshold) {
					analogScratchTickCounter++;
				}
				if (analogScratchTickCounter >= 2) {
					activeAnalogScratch = true;
					rightMoveScratching = nowRight;
				}
			}

			counter = 0;
			oldAnalogScratchX = analogScratchX;
		}

		// counter > 2*Threshold ... Stop Scratching.
		if (counter > this.analogScratchThreshold*2) {
			activeAnalogScratch = false;
			analogScratchTickCounter = 0;
			counter = 0;
		}

		counter++;

		if(button == BMKeys.UP) {
			return activeAnalogScratch && rightMoveScratching;
		} else if(button == BMKeys.DOWN){
			return activeAnalogScratch && !rightMoveScratching;
		} else {
			return false;
		}
	}

	private boolean scratchInput(int button) {
		if (analogScratchMode2) {
			return analogScratchInputMode2(button);
		} else if (analogScratch) {
			return analogScratchInput(button);
		} else {
			if(button == BMKeys.UP) {
				return (axis[1] < -0.9) || (axis[2] < -0.9);
			} else if(button == BMKeys.DOWN){
				return (axis[1] > 0.9) || (axis[2] > 0.9);
			} else {
				return false;
			}
		}
	}

	public int getLastPressedButton() {
		return lastPressedButton;
	}

	public void setLastPressedButton(int lastPressedButton) {
		this.lastPressedButton = lastPressedButton;
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
		public static final int UP = 16;
		public static final int DOWN = 17;
		public static final int LEFT = 18;
		public static final int RIGHT = 19;
		
		public static final int MAXID = 20;

		/**
		 * 専コンのキーコードに対応したテキスト
		 */
		private static final String[] BMCODE = { "BUTTON 1", "BUTTON 2", "BUTTON 3", "BUTTON 4", "BUTTON 5", "BUTTON 6",
				"BUTTON 7", "BUTTON 8", "BUTTON 9", "BUTTON 10", "BUTTON 11", "BUTTON 12", "BUTTON 13", "BUTTON 14",
				"BUTTON 15", "BUTTON 16", "UP", "DOWN", "LEFT", "RIGHT" };

		public static final String toString(int keycode) {
			if (keycode >= 0 && keycode < BMCODE.length) {
				return BMCODE[keycode];
			}
			return "Unknown";
		}
	}

}