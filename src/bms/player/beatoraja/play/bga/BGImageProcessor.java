package bms.player.beatoraja.play.bga;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import bms.model.TimeLine;
import bms.player.beatoraja.PixmapResourcePool;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * BGIリソース管理用クラス
 *
 * @author exch
 */
public class BGImageProcessor {
	
	public static final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png", "tga" };
	/**
	 * BGイメージ
	 */
	private Pixmap[] bgamap = new Pixmap[1000];
	/**
	 * BGイメージのキャッシュ
	 */
	private Texture[] bgacache;
	/**
	 * キャッシュされているBGイメージID
	 */
	private int[] bgacacheid;

	private final PixmapResourcePool cache;

	public BGImageProcessor(int size, int maxgen) {
		bgacache = new Texture[size];
		bgacacheid = new int[size];
		cache = new PixmapResourcePool(maxgen) {

			protected Pixmap convert(Pixmap pixmap) {
				int bgasize = Math.max(pixmap.getHeight(), pixmap.getWidth());
				if ( bgasize <=256 ){
					final int fixx = (256 - pixmap.getWidth()) / 2;
					Pixmap fixpixmap = new Pixmap(256, 256, pixmap.getFormat());
					fixpixmap.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(),
							fixx, 0, pixmap.getWidth(), pixmap.getHeight());
					pixmap.dispose();
					return fixpixmap;
				}
				return pixmap;
			}
		};
	}

	public void put(int id, Path path) {
		Pixmap pixmap = cache.get(path.toString());
		if(id >= bgamap.length) {
			bgamap = Arrays.copyOf(bgamap, id + 1);
		}
		bgamap[id] = pixmap;
	}
	
	public void clear() {
		Arrays.fill(bgamap,  null);
	}
	
	public void disposeOld() {
		cache.disposeOld();
	}

	/**
	 * BGAの初期データをあらかじめキャッシュする
	 */
	public void prepare(TimeLine[] timelines) {
		long l = System.currentTimeMillis();
		Arrays.fill(bgacacheid, -1);
		for (Texture bga : bgacache) {
			if (bga != null) {
				bga.dispose();
			}
		}
		Arrays.fill(bgacache, null);

		int count = 0;
		for (TimeLine tl : timelines) {
			int bga = tl.getBGA();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bga < bgamap.length && bgamap[bga] != null) {
				getTexture(bga);
				count++;
			}

			bga = tl.getLayer();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bga < bgamap.length && bgamap[bga] != null) {
				getTexture(bga);
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
		if (id < bgamap.length && bgamap[id] != null){
			if(bgacache[cid] == null) {
				bgacache[cid] = new Texture(bgamap[id]);				
			} else if(bgacache[cid].getWidth() != bgamap[id].getWidth() || bgacache[cid].getHeight() != bgamap[id].getHeight()){
				bgacache[cid].dispose();
				bgacache[cid] = new Texture(bgamap[id]);				
			} else {
				bgacache[cid].draw(bgamap[id], 0, 0);
			}
			bgacacheid[cid] = id;
			return bgacache[cid];
		}
		return null;
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

		cache.dispose();
	}	
}
