package bms.player.beatoraja.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bms.model.BMSModel;
import bms.model.LongNote;
import bms.model.NormalNote;
import bms.model.Note;
import bms.model.TimeLine;
import bms.player.beatoraja.Validatable;

/**
 * キー入力ログ
 * 
 * @author exch
 */
public final class KeyInputLog implements Validatable {
	
	public static final KeyInputLog[] EMPTYARRAY = new KeyInputLog[0];

	/**
	 * キー入力時間(us)
	 */	
	private long presstime;
	/**
	 * キーコード
	 */
	private int keycode;
	/**
	 * キー押し離し
	 */
	private boolean pressed;

	/**
	 * キー入力時間(ms)。旧データとの互換性維持用
	 */
	long time;

	public KeyInputLog() {
	}

	public KeyInputLog(long presstime, int keycode, boolean pressed) {
		setData(presstime, keycode, pressed);
	}
	
	public void setData(long presstime, int keycode, boolean pressed) {
		this.presstime = presstime;
		this.keycode = keycode;
		this.pressed = pressed;		
	}
	
	public long getTime() {
		return presstime != 0 ? presstime : time * 1000;
	}
	
	public int getKeycode() {
		return keycode;
	}
	
	public boolean isPressed() {
		return pressed;
	}

	/**
	 * AUTOPLAY用のKeyInputLogを生成する
	 * 
	 * @return AUTOPLAY用のKeyInputLog
	 */
	public static final List<KeyInputLog> createAutoplayLog(BMSModel model) {
		// TODO 地雷を確実に回避するアルゴリズム
		List<KeyInputLog> keylog = new ArrayList<KeyInputLog>();
		int keys = model.getMode().key;
		int[] sc = model.getMode().scratchKey;
		Note[] ln = new Note[keys];
		for (TimeLine tl : model.getAllTimeLines()) {
			long i = tl.getTime();
			for (int lane = 0; lane < keys; lane++) {
				Note note = tl.getNote(lane);
				if (note != null) {
					if (note instanceof LongNote) {
						if (((LongNote) note).isEnd()) {
							keylog.add(new KeyInputLog(i, lane, false));
							if (model.getLntype() != 0 && Arrays.asList(sc).contains(lane)) {
								// BSS処理
								keylog.add(new KeyInputLog(i, lane + 1, true));
							}
							ln[lane] = null;
						} else {
							keylog.add(new KeyInputLog(i, lane, true));
							ln[lane] = note;
						}
					} else if (note instanceof NormalNote) {
						keylog.add(new KeyInputLog(i, lane, true));
					}
				} else {
					if (ln[lane] == null) {
						keylog.add(new KeyInputLog(i, lane, false));
						if (Arrays.asList(sc).contains(lane)) {
							keylog.add(new KeyInputLog(i, lane + 1, false));
						}
					}
				}
			}
		}
		return keylog;
	}

	@Override
	public boolean validate() {
		if(time > 0) {
			presstime = time * 1000;
			time = 0;
		}
		return presstime >= 0 && keycode >= 0;
	}
}
