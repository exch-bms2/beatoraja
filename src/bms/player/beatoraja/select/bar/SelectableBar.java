package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;

/**
 * 選択可能なバー
 *
 * @author exch
 */
public abstract class SelectableBar extends Bar {

	/**
	 * リプレイデータが存在するか
	 */
	private final boolean[] existsReplay = new boolean[MusicSelector.REPLAY];

	public final boolean existsReplayData() {
		for (boolean b : existsReplay) {
			if (b) {
				return b;
			}
		}
		return false;
	}

	/**
	 * 指定のリプレイが存在するかを返す
	 * 
	 * @param index
	 *            リプレイ番号
	 * @return リプレイが存在すればtrue
	 */
	public final boolean existsReplay(int index) {
		return index >= 0 && index < existsReplay.length ? existsReplay[index] : false;
	}

	/**
	 * 指定のリプレイが存在するかを設定する
	 * 
	 * @param index
	 *            リプレイ番号
	 * @param existsReplay
	 *            リプレイが存在する場合はtrue
	 */
	public final void setExistsReplay(int index, boolean existsReplay) {
		if (index >= 0 && index < this.existsReplay.length) {
			this.existsReplay[index] = existsReplay;
		}
	}
}
