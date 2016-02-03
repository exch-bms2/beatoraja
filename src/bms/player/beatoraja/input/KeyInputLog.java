package bms.player.beatoraja.input;

/**
 * キー入力ログ
 * 
 * @author exch
 */
public class KeyInputLog {

	/**
	 * キー入力時間
	 */
	public int time;
	/**
	 * キーコード
	 */
	public int keycode;
	/**
	 * キー押し離し
	 */
	public boolean pressed;

	public KeyInputLog() {	
	}
	
	public KeyInputLog(int time, int keycode, boolean pressed) {
		this.time = time;
		this.keycode = keycode;
		this.pressed = pressed;
	}

}
