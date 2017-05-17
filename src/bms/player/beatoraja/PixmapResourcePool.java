package bms.player.beatoraja;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * Pixmapリソースプール
 * 
 * @author exch
 */
public class PixmapResourcePool extends ResourcePool<String, Pixmap> {

	public static final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };

	public PixmapResourcePool() {
		super(1);
	}
	
	public PixmapResourcePool(int maxgen) {
		super(maxgen);
	}
	
	@Override
	protected Pixmap load(String path) {
		return convert(loadPicture(path));
	}
	
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
	 * @return イメージ。読めなかった場合はnullを返す
	 */
	public static Pixmap loadPicture(String path) {
		Pixmap tex = null;
		for (String mov : pic_extension) {
			if (path.toLowerCase().endsWith(mov)) {
				try {
					tex = new Pixmap(Gdx.files.internal(path));
				} catch (Exception e) {
					e.printStackTrace();
				} catch (Error e) {
				}
				if (tex == null) {
					Logger.getGlobal().warning("BGAファイル読み込み再試行:" + path);
					try {
						BufferedImage bi = ImageIO.read(new File(path));
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
}