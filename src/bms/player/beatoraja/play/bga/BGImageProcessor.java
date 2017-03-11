package bms.player.beatoraja.play.bga;

import java.util.Arrays;
import java.util.logging.Logger;

import bms.model.TimeLine;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * BGIリソース管理用クラス
 *
 * @author exch
 */
public class BGImageProcessor {
	/**
	 * BGイメージ
	 */
	private Pixmap[] bgamap;
	/**
	 * BGイメージのキャッシュ
	 */
	private Texture[] bgacache;
	/**
	 * キャッシュされているBGイメージID
	 */
	private int[] bgacacheid;

	public BGImageProcessor(Pixmap[] pixmap, int size) {
		this.bgamap = pixmap;
		bgacache = new Texture[size];
		bgacacheid = new int[size];
		Arrays.fill(bgacacheid, -1);
	}

	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare(TimeLine[] timelines) {
		long l = System.currentTimeMillis();
		int count = 0;
		for (TimeLine tl : timelines) {
			int bga = tl.getBGA();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bgamap[bga] != null) {
				bgacache[bga % bgacache.length] = new Texture(createPixmap(bga));
				bgacacheid[bga % bgacache.length] = bga;
				count++;
			}

			bga = tl.getLayer();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bgamap[bga] != null) {
				bgacache[bga % bgacache.length] = new Texture(createPixmap(bga));
				bgacacheid[bga % bgacache.length] = bga;
				count++;
			}
		}
		Logger.getGlobal().info(
				"BGAデータの事前Texture化 - BGAデータ数:" + count + " time(ms):" + (System.currentTimeMillis() - l));
	}

	public Texture getTexture(int id) {
		final int cid = id % bgacache.length;
		// BGイメージキャッシュにTextureがある場合
		if (bgacacheid[cid] == id) {
			return bgacache[cid];
		}
		// BGイメージキャッシュにTextureがない場合
		if (bgacache[cid] != null) {
			bgacache[cid].dispose();
		}
		if (bgamap[id] != null){
			bgacache[cid] = new Texture(createPixmap(id));
			bgacacheid[cid] = id;
			return bgacache[cid];
		}
		return null;
	}


	/**
	 * Create a pixmap that is compatible with legacy BMS
	 */
	private Pixmap createPixmap(int id){
		int bgasize = bgamap[id].getHeight() > bgamap[id].getWidth() ?
				bgamap[id].getHeight() : bgamap[id].getWidth();
		Pixmap pix;
		if ( bgasize <=256 ){
			pix = new Pixmap(bgasize, bgasize,bgamap[id].getFormat());
		} else {
			pix = new Pixmap(bgamap[id].getWidth(), bgamap[id].getHeight(), bgamap[id].getFormat());
		}
		pix.drawPixmap(bgamap[id], 0, 0, bgamap[id].getWidth(), bgamap[id].getHeight(),
				0, 0, bgamap[id].getWidth(), bgamap[id].getHeight());
		return pix;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (Texture bga : bgacache) {
			if (bga != null) {
				bga.dispose();
			}
		}
		bgacache = new Texture[0];
		for (Pixmap id : bgamap) {
			if (id != null) {
				id.dispose();
			}
		}
		bgamap = new Pixmap[0];
	}
}
