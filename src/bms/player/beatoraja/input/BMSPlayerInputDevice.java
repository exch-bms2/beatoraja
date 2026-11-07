package bms.player.beatoraja.input;

/**
 * 入力デバイス
 *
 * @author excln
 */
public abstract class BMSPlayerInputDevice {

	protected final BMSPlayerInputProcessor bmsPlayerInputProcessor;
	/**
	 * 入力デバイスの種類
	 */
	public final Type type;

	/**
	 * デバイスの入力状態をクリアする
	 */
	public abstract void clear();

	protected BMSPlayerInputDevice(BMSPlayerInputProcessor bmsPlayerInputProcessor, Type type) {
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.type = type;
	}

	protected final void keyChanged(long time, int key, boolean pressed) {
		bmsPlayerInputProcessor.keyChanged(this, time, key, pressed);
	}

	/**
	 * 入力デバイスのタイプ
	 *
	 * @author excln
	 */
	public enum Type {
		KEYBOARD,
		BM_CONTROLLER,
		MIDI
	}
}
