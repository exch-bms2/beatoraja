package bms.player.beatoraja.play.bga;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import org.bytedeco.javacv.*;

public class FFmpegProcessor implements MovieProcessor {

	private List<Pixmap> frames = new ArrayList<Pixmap>();
	private int showingframe = -1;
	private Texture showingtex;

	private long starttime;
	
	private double fps;
	
	private int fpsd = 2;

	private FrameGrabber grabber;

	public FFmpegProcessor(int fpsd) {
		this.fpsd = fpsd;
	}

	@Override
	public void create(String filepath) {
		grabber = new FFmpegFrameGrabber(filepath);
		try {
			createFramePixmap();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Texture getBGAData(boolean cont) {
		if (starttime == 0 || !cont) {
			starttime = System.currentTimeMillis();
		}
		final long nowtime = System.currentTimeMillis() - starttime;

		final int nowframe = (int) (nowtime / (1000 / fps));
		if (showingframe != nowframe) {
			showingframe = nowframe;
			if (showingtex != null) {
				showingtex.dispose();
			}
			if (showingframe < frames.size()) {
				showingtex = new Texture(frames.get(showingframe));
//				System.out.println("FFmpegProcessor : showing frame - " + showingframe);
			} else {
				showingtex = null;
			}
		}
		return showingtex;
	}

	private void createFramePixmap() throws Exception {
		grabber.start();
		try {
			Logger.getGlobal().info(
					"decode開始 - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat() + " size : "
							+ grabber.getImageWidth() + " x " + grabber.getImageHeight());
			fps = grabber.getFrameRate() / fpsd;
			Java2DFrameConverter converter = new Java2DFrameConverter();
			long start = System.currentTimeMillis();
			Frame frame = grabber.grab();
			while (frame != null) {
				if (frame.image[0] != null) {
					BufferedImage image = converter.convert(frame);
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(image, "bmp", baos);
					// Pixmap pixmap = new Pixmap(new
					// FileHandleStream("tempimage.bmp") {
					// @Override
					// public InputStream read() {
					// return new ByteArrayInputStream(baos.toByteArray());
					// }
					//
					// @Override
					// public OutputStream write(boolean overwrite) {
					// return null;
					// }
					// });
					byte[] data = baos.toByteArray();
					Pixmap pixmap = new Pixmap(data, 0, data.length);
					frames.add(pixmap);
					// System.out.println("FFmpegProcessor : encoded frames - "
					// + grabber.getFrameNumber());
				}
				for(int i = 0;i < fpsd;i++) {
					frame = grabber.grab();					
				}
			}
			System.out.println("FFmpegProcessor : encoded time - " + (System.currentTimeMillis() - start));
		} catch (Exception e) {
			e.printStackTrace();
		}

		grabber.stop();
		grabber.release();

	}

	@Override
	public void dispose() {
		if (showingtex != null) {
			showingtex.dispose();
		}
		for (Pixmap p : frames) {
			p.dispose();
		}
	}

}
