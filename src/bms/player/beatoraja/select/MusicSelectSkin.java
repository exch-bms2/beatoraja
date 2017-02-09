package bms.player.beatoraja.select;

import bms.player.beatoraja.skin.*;
import com.badlogic.gdx.math.Rectangle;

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

	private Rectangle search;

	public MusicSelectSkin(float srcw, float srch, float dstw, float dsth) {
		super(srcw, srch, dstw, dsth);
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
	
	public Rectangle getSearchTextRegion() {
		return search;
	}

	public void setSearchTextRegion(Rectangle r) {
		search = r;
	}

}
