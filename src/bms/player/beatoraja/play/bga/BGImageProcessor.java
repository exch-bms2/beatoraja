package bms.player.beatoraja.play.bga;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import bms.model.TimeLine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandleStream;
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

	private static final int MAX_GENERATION = 1;
	
	private Map<String, ImageCacheElement> image = new HashMap<String, ImageCacheElement> ();

	public BGImageProcessor(int size) {
		bgacache = new Texture[size];
		bgacacheid = new int[size];
	}

	public void put(int id, Path path) {
		if(!image.containsKey(path.toString())) {
			Pixmap pixmap = convertPixmap(loadPicture(path));
			image.put(path.toString(), new ImageCacheElement(pixmap));
		} else {
			System.out.println("ImageCache : リソース再利用 - " + path.toString());
			image.get(path.toString()).gen = 0;
		}
		if(id >= bgamap.length) {
			bgamap = Arrays.copyOf(bgamap, id + 1);
		}
		bgamap[id] = image.get(path.toString()).image;
	}
	
	public void clear() {
		Arrays.fill(bgamap,  null);
	}
	
	public void disposeOld() {
		String[] keyset = image.keySet().toArray(new String[image.size()]);
		for(String s : keyset) {
			ImageCacheElement ie = image.get(s);
			if(ie.gen == MAX_GENERATION) {
				System.out.println("ImageCache : リソース破棄 - " + s);
				ie.image.dispose();
				image.remove(s);
			} else {
				ie.gen++;
			}
		}
		Logger.getGlobal().info("現在のImageCache容量 : " + image.size());
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
	 * Create a pixmap that is compatible with legacy BMS
	 */
	private Pixmap convertPixmap(Pixmap pixmap){
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
		
		for (ImageCacheElement id : image.values()) {
			if (id != null) {
				id.image.dispose();
			}
		}
		image.clear();
	}
	
	public static Pixmap loadPicture(Path dir) {
		Pixmap tex = null;
		for (String mov : pic_extension) {
			if (dir.toString().toLowerCase().endsWith(mov)) {
				try {
					tex = new Pixmap(Gdx.files.internal(dir.toString()));
				} catch (Exception e) {
					e.printStackTrace();
				} catch (Error e) {
				}
				if (tex == null) {
					Logger.getGlobal().warning("BGAファイル読み込み再試行:" + dir.toString());
					try {
						BufferedImage bi = ImageIO.read(dir.toFile());
						final ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(bi, "gif", baos);
						tex = new Pixmap((new FileHandleStream("tempwav.gif") {
							@Override
							public InputStream read() {
								return new ByteArrayInputStream(baos.toByteArray());
							}

							@Override
							public OutputStream write(boolean overwrite) {
								return null;
							}
						}));
					} catch (Throwable e) {
						Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}

		return tex;
	}
		
	private static class ImageCacheElement {
		public Pixmap image;
		public int gen;
		
		public ImageCacheElement(Pixmap image) {
			this.image = image;
		}
	}
}
