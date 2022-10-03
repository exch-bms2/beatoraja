package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.skin.property.TimerProperty;

/**
 * IDで参照可能なユーザー定義タイマー
 * 挙動をスクリプトで記述した能動的なものと、外部からON/OFFを切り替える想定の受動的なものが存在する
 */
public class CustomTimer {
	private final int id;
	private final TimerProperty timerFunc;
	private long time = Long.MIN_VALUE;

	/**
	 * カスタムタイマーを生成する
	 * @param id 外部から参照する際のID
	 * @param timerFunc タイマーの挙動 ({@null} を与えて受動的なタイマーとすることも可能)
	 */
	public CustomTimer(int id, TimerProperty timerFunc) {
		this.id = id;
		this.timerFunc = timerFunc;
	}

	public boolean isPassive() {
		return timerFunc == null;
	}

	public int getId() {
		return id;
	}

	public long getMicroTimer() {
		return time;
	}

	public void setMicroTimer(long time) {
		if (timerFunc != null)
			return;
		this.time = time;
	}

	public void update(MainState state) {
		if (timerFunc != null) {
			time = timerFunc.getMicro(state);
		}
	}
}
