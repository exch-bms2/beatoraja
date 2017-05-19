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
	
	public static final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };
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

	private PixmapResourcePool cache = new PixmapResourcePool() {
		
		protected Pixmap convert(Pixmap pixmap) {
			int bgasize = Math.max(pixmap.getHeight(), pixmap.getWidth());
			if ( bgasize <=256 ){
				final int fixx = (bgasize -  pixmap.getWidth()) / 2;
				Pixmap fixpixmap = new Pixmap(bgasize, bgasize, pixmap.getFormat());
				fixpixmap.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(),
						fixx, 0, pixmap.getWidth(), pixmap.getHeight());
				pixmap.dispose();
				return fixpixmap;
			}
			return pixmap;
		}
	};

	public BGImageProcessor(int size) {
		bgacache = new Texture[size];
		bgacacheid = new int[size];
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
		int count = 0;
		for (TimeLine tl : timelines) {
			int bga = tl.getBGA();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bgamap[bga] != null) {
				getTexture(bga);
				count++;
			}

			bga = tl.getLayer();
			if (bga >= 0 && bgacache[bga % bgacache.length] == null && bgamap[bga] != null) {
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
		if (bgacache[cid] != null) {
			bgacache[cid].dispose();
		}
		if (bgamap[id] != null){
			bgacache[cid] = new Texture(bgamap[id]);
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
