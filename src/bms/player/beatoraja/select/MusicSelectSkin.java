package bms.player.beatoraja.select;

import bms.player.beatoraja.skin.*;

/**
 * 選曲スキン
 *
 * @author exch
 */
public class MusicSelectSkin extends Skin {

	/**
	 * カーソルが合っているBarのindex
	 */
	private int centerBar;
	/**
	 * クリック可能なBarのindex
	 */
	private int[] clickableBar = new int[0];

	public MusicSelectSkin(SkinHeader header) {
		super(header);
	}

	public int[] getClickableBar() {
		return clickableBar;
	}

	public void setClickableBar(int[] clickableBar) {
		this.clickableBar = clickableBar;
	}

	public int getCenterBar() {
		return centerBar;
	}

	public void setCenterBar(int centerBar) {
		this.centerBar = centerBar;
	}

}
