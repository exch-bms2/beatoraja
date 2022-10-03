package bms.player.beatoraja;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

/**
 * Pixmapリソースプール
 * 
 * @author exch
 */
public class PixmapResourcePool extends ResourcePool<String, Pixmap> {

	public PixmapResourcePool() {
		super(1);
	}
	
	public PixmapResourcePool(int maxgen) {
		super(maxgen);
	}
	
	@Override
	protected Pixmap load(String path) {
		final Pixmap pixmap = loadPicture(path);
		return pixmap != null ? convert(pixmap) : null;
	}

	/**
	 * Pixmapをload時に変換する。
	 *
	 * @param pixmap
	 * @return
	 */
	protected Pixmap convert(Pixmap pixmap) {
		return pixmap;
	}

	@Override
	protected void dispose(Pixmap resource) {
		resource.dispose();
	}

	/**
	 * 指定のパスで表現されるファイルを読み込む
	 * @param path イメージファイルのパス
	 * @return イメージ。読めなかった場合またはpathがファイルでない場合はnullを返す
	 */
	public static Pixmap loadPicture(String path) {
		Pixmap tex = null;
		File f = new File(path);
		if(!f.isFile()) {
			return tex;
		}

		try {
			if(path.endsWith(".cim")) {
				tex = PixmapIO.readCIM(Gdx.files.internal(path));
			} else {
				tex = new Pixmap(Gdx.files.internal(path));				
			}
		} catch (Throwable e) {
			Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
		}
		if (tex == null) {
			Logger.getGlobal().warning("BGAファイル読み込み再試行:" + path);
			try {
				// TODO 一部のbmsはImageIO.readで失敗する(e.g. past glow)。別の画像デコーダーが必要
				BufferedImage bi = ImageIO.read(f);
//						System.out.println("width : " + bi.getWidth() + " height : " + bi.getHeight() + " type : " + bi.getType());
				tex = new Pixmap(bi.getWidth(), bi.getHeight(), Pixmap.Format.RGBA8888);
				for(int x = 0;x < bi.getWidth();x++) {
					for(int y = 0;y < bi.getHeight();y++) {
						tex.drawPixel(x, y, (bi.getRGB(x, y) << 8 | 0x000000ff));
					}
				}
			} catch (Throwable e) {
				Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
				e.printStackTrace();
			}
		}

		return tex;
	}
}