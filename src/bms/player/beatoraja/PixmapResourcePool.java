package bms.player.beatoraja;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * Pixmapリソースプール
 * 
 * @author exch
 */
public class PixmapResourcePool extends ResourcePool<String, PixmapResourcePool.PixmapResource> {

	public static final class PixmapResource {
		private final Pixmap pixmap;
		private final int frameCount;
		private final int frameWidth;
		private final int frameHeight;
		private final int columns;
		private final int rows;

		public PixmapResource(Pixmap pixmap) {
			this(pixmap, 1, pixmap.getWidth(), pixmap.getHeight(), 1, 1);
		}

		public PixmapResource(Pixmap pixmap, int frameCount, int frameWidth, int frameHeight, int columns, int rows) {
			this.pixmap = pixmap;
			this.frameCount = frameCount;
			this.frameWidth = frameWidth;
			this.frameHeight = frameHeight;
			this.columns = columns;
			this.rows = rows;
		}

		public Pixmap getPixmap() {
			return pixmap;
		}

		public int getFrameCount() {
			return frameCount;
		}

		public int getFrameWidth() {
			return frameWidth;
		}

		public int getFrameHeight() {
			return frameHeight;
		}

		public int getColumns() {
			return columns;
		}

		public int getRows() {
			return rows;
		}

		public void dispose() {
			pixmap.dispose();
		}
	}

	public PixmapResourcePool() {
		super(1);
	}
	
	public PixmapResourcePool(int maxgen) {
		super(maxgen);
	}
	
	@Override
	protected PixmapResource load(String path) {
		final PixmapResource resource = loadPictureResource(path);
		return resource != null ? convert(resource) : null;
	}

	public Pixmap getPixmap(String path) {
		PixmapResource resource = get(path);
		return resource != null ? resource.getPixmap() : null;
	}

	public PixmapResource getPixmapResource(String path) {
		return get(path);
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

	protected PixmapResource convert(PixmapResource resource) {
		Pixmap pixmap = convert(resource.getPixmap());
		if (pixmap == resource.getPixmap()) {
			return resource;
		}
		return new PixmapResource(pixmap);
	}

	@Override
	protected void dispose(PixmapResource resource) {
		resource.dispose();
	}

	/**
	 * 指定のパスで表現されるファイルを読み込む
	 * @param path イメージファイルのパス
	 * @return イメージ。読めなかった場合またはpathがファイルでない場合はnullを返す
	 */
	public static Pixmap loadPicture(String path) {
		PixmapResource resource = loadPictureResource(path);
		return resource != null ? resource.getPixmap() : null;
	}

	/**
	 * 指定のパスで表現されるファイルを読み込む
	 * @param path イメージファイルのパス
	 * @return イメージリソース。読めなかった場合またはpathがファイルでない場合はnullを返す
	 */
	public static PixmapResource loadPictureResource(String path) {
		PixmapResource resource = null;
		Pixmap tex = null;
		File f = new File(path);
		if(!f.isFile()) {
			return null;
		}

		// JPEGファイルはlibGDXのネイティブデコーダー(jpgd)を使用しない。
		// 一部のJPEGファイルでネイティブコードのassert()が発動してプロセスごとクラッシュするため。
		String lower = path.toLowerCase(Locale.ROOT);
		boolean isJpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
		boolean isWebp = lower.endsWith(".webp");

		if (!isJpeg && !isWebp) {
			try {
				if(lower.endsWith(".cim")) {
					tex = PixmapIO.readCIM(Gdx.files.internal(path));
				} else {
					tex = new Pixmap(Gdx.files.internal(path));
				}
			} catch (Throwable e) {
				Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
			}
		}
		if (tex == null) {
			if (!isWebp && !isJpeg) {
				Logger.getGlobal().warning("BGAファイル読み込み再試行:" + path);
			}
			try {
				// TODO 一部のbmsはImageIO.readで失敗する(e.g. past glow)。別の画像デコーダーが必要
				if (isWebp) {
					resource = loadWebpWithFFmpeg(path);
					if (resource == null) {
						BufferedImage bi = hasImageReader("webp") ? ImageIO.read(f) : null;
						if (bi != null) {
							tex = toPixmap(bi);
						} else {
							Logger.getGlobal().warning("WebPファイルを読み込めません : " + path);
						}
					}
				} else {
					BufferedImage bi = ImageIO.read(f);
					if (bi != null) {
						tex = toPixmap(bi);
					}
				}
			} catch (Throwable e) {
				Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
				e.printStackTrace();
			}
		}

		return resource != null ? resource : tex != null ? new PixmapResource(tex) : null;
	}

	private static boolean hasImageReader(String formatName) {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
		return readers.hasNext();
	}

	private static PixmapResource loadWebpWithFFmpeg(String path) {
		List<Pixmap> frames = new ArrayList<>();
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path)) {
			grabber.start();
			Java2DFrameConverter converter = new Java2DFrameConverter();
			Frame frame;
			while ((frame = grabber.grabImage()) != null) {
				BufferedImage image = converter.convert(frame);
				if (image != null) {
					frames.add(toPixmap(image));
				}
			}
			return toSpriteSheetPixmap(frames);
		} catch (Throwable e) {
			frames.forEach(Pixmap::dispose);
			Logger.getGlobal().warning("WebPファイル読み込み失敗。" + e.getMessage());
			return null;
		}
	}

	private static PixmapResource toSpriteSheetPixmap(List<Pixmap> frames) {
		if (frames.isEmpty()) {
			return null;
		}

		int frameWidth = 0;
		int frameHeight = 0;
		for (Pixmap frame : frames) {
			frameWidth = Math.max(frameWidth, frame.getWidth());
			frameHeight = Math.max(frameHeight, frame.getHeight());
		}

		int maxTextureSize = getMaxTextureSize();
		int maxColumns = Math.max(1, maxTextureSize / Math.max(1, frameWidth));
		int maxRows = Math.max(1, maxTextureSize / Math.max(1, frameHeight));
		int[] grid = getSpriteSheetGrid(frames.size(), frameWidth, frameHeight, maxColumns, maxRows);
		int columns = grid[0];
		int rows = grid[1];

		int width = frameWidth * columns;
		int height = frameHeight * rows;
		if (width > maxTextureSize || height > maxTextureSize) {
			Logger.getGlobal().warning("WebPのスプライトシートサイズが最大テクスチャサイズを超えています : "
					+ width + "x" + height + " max=" + maxTextureSize);
		}
		if (frames.size() > 1) {
			Logger.getGlobal().info("WebPをスプライトシートとして読み込みました : frames=" + frames.size()
					+ " grid=" + columns + "x" + rows + " size=" + width + "x" + height);
		}

		Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
		for (int i = 0; i < frames.size(); i++) {
			Pixmap frame = frames.get(i);
			int x = i % columns * frameWidth;
			int y = i / columns * frameHeight;
			pixmap.drawPixmap(frame, x, y);
		}
		frames.forEach(Pixmap::dispose);
		return new PixmapResource(pixmap, frames.size(), frameWidth, frameHeight, columns, rows);
	}

	private static int[] getSpriteSheetGrid(int frames, int frameWidth, int frameHeight, int maxColumns, int maxRows) {
		int bestColumns = 0;
		int bestRows = 0;
		long bestScore = Long.MAX_VALUE;
		for (int columns = 1; columns <= Math.min(frames, maxColumns); columns++) {
			if (frames % columns != 0) {
				continue;
			}
			int rows = frames / columns;
			if (rows > maxRows) {
				continue;
			}
			long width = (long) frameWidth * columns;
			long height = (long) frameHeight * rows;
			long score = Math.abs(width - height);
			if (score < bestScore) {
				bestColumns = columns;
				bestRows = rows;
				bestScore = score;
			}
		}
		if (bestColumns > 0) {
			return new int[] { bestColumns, bestRows };
		}

		int columns = Math.min(frames, maxColumns);
		int rows = (frames + columns - 1) / columns;
		while (rows > maxRows && columns < Math.min(frames, maxColumns)) {
			columns++;
			rows = (frames + columns - 1) / columns;
		}
		while (columns > 1 && (long) (columns - 1) * frameWidth >= (long) rows * frameHeight
				&& (frames + columns - 2) / (columns - 1) <= maxRows) {
			columns--;
			rows = (frames + columns - 1) / columns;
		}
		return new int[] { columns, rows };
	}

	private static int getMaxTextureSize() {
		try {
			IntBuffer buffer = BufferUtils.newIntBuffer(1);
			Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buffer);
			int size = buffer.get(0);
			if (size > 0) {
				return size;
			}
		} catch (Throwable e) {
			Logger.getGlobal().warning("最大テクスチャサイズを取得できません。" + e.getMessage());
		}
		return 4096;
	}

	private static Pixmap toPixmap(BufferedImage image) {
		Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
		for(int x = 0;x < image.getWidth();x++) {
			for(int y = 0;y < image.getHeight();y++) {
				int argb = image.getRGB(x, y);
				pixmap.drawPixel(x, y, argb << 8 | (argb >>> 24 & 0x000000ff));
			}
		}
		return pixmap;
	}
}
