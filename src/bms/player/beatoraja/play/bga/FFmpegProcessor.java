package bms.player.beatoraja.play.bga;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

import org.bytedeco.javacv.*;

/**
 * ffmpegを使用した動画表示用クラス
 * 
 * @author exch
 */
public class FFmpegProcessor implements MovieProcessor {

	// TODO リスタートでエラーが出る場合がある(Ex.KRAKEN)
	// TODO フレームレートが違う場合がある

	/**
	 * 現在表示中のフレームのTexture
	 */
	private Texture showingtex;
	/**
	 * 動画のfps
	 */
	private double fps;

	private int fpsd = 1;

	private FFmpegFrameGrabber grabber;

	private Pixmap pixmap;
	private Pixmap showing;

	private MovieSeekThread movieseek;

	public FFmpegProcessor(int fpsd) {
		this.fpsd = fpsd;
	}

	@Override
	public void create(String filepath) {
		grabber = new FFmpegFrameGrabber(filepath);
		try {
			grabber.start();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Texture getBGAData(boolean cont) {
		if (movieseek == null || !cont) {
			if (movieseek != null) {
				movieseek.stop = true;
			}
			movieseek = new MovieSeekThread();
			movieseek.start();
		}
		if (showing != pixmap) {
			showing = pixmap;
			if (showingtex != null) {
				showingtex.dispose();
			}
			if (pixmap != null) {
				showingtex = new Texture(pixmap);
			} else {
				showingtex = null;
			}
		}
		return showingtex;
	}

	class MovieSeekThread extends Thread {

		public boolean stop = false;

		public void run() {
			try {
				grabber.restart();
				Logger.getGlobal().info(
						"decode開始 - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat() + " size : "
								+ grabber.getImageWidth() + " x " + grabber.getImageHeight()
								+ " length (frame / time) : " + grabber.getLengthInFrames() + " / "
								+ grabber.getLengthInTime());
				fps = grabber.getFrameRate();
				if(fps > 240) {
					// フレームレートが大きすぎる場合は手動で修正(暫定処置)
					fps = 30;
				}
				final long[] nativeData = new long[] { 0, grabber.getImageWidth(), grabber.getImageHeight(),
						Gdx2DPixmap.GDX2D_FORMAT_RGB888 };
				final long start = System.currentTimeMillis();
				int framecount = 0;
				Frame frame = null;
				while (!stop) {
					final long time = System.currentTimeMillis() - start;
					if (time >= framecount * 1000 / fps) {
						while (time >= framecount * 1000 / fps || framecount % fpsd != 0) {
							frame = grabber.grabImage();
							framecount++;
						}
						if (frame == null) {
							// System.out.println("decode end");
							break;
						}
						if (frame.image != null && frame.image[0] != null) {
							try {
								Gdx2DPixmap pixmapData = new Gdx2DPixmap((ByteBuffer) frame.image[0], nativeData);
								pixmap = new Pixmap(pixmapData);
								// System.out.println("movie pixmap created : "
								// + time);
							} catch (Exception e) {
								pixmap = null;
								throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
							}
						}
					} else {
						final long sleeptime = (long) (framecount * 1000 / fps - time + 1);
						if (sleeptime > 0) {
							sleep(sleeptime);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					grabber.stop();
				} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public void dispose() {
		stop();
		try {
			grabber.release();
		} catch (Exception e) {

		}
		if (showingtex != null) {
			showingtex.dispose();
		}
	}

	public void stop() {
		if (movieseek != null) {
			movieseek.stop = true;
		}
	}

}
