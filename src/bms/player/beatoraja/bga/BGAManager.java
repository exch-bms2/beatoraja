package bms.player.beatoraja.bga;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;
import bms.model.BMSModel;
import bms.player.beatoraja.Config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class BGAManager {

	private BMSModel model;
	private Config config;
	private float progress = 0;

	private Pixmap[] bgamap = new Pixmap[0];
	private Map<Integer, MovieManager> mpgmap = new HashMap<Integer, MovieManager>();
	private Pixmap backbmp;

	private final String[] mov_extension = { "mpg", "mpeg", "avi", "wmv" };
	private final String[] pic_extension = { "jpg", "jpeg", "gif", "bmp", "png" };

	public BGAManager(Config config) {
		this.config = config;
	}

	public void setModel(BMSModel model, String filepath) {
		this.model = model;
		// BMS格納ディレクトリ
		String directorypath = filepath.substring(0, filepath.lastIndexOf(File.separatorChar) + 1);

		dispose();
		progress = 0;

		String back = model.getBackbmp();
		if(back != null && back.length() > 0) {
			back = back.substring(0, back.lastIndexOf('.'));
			for (String mov : pic_extension) {
				File mpgfile = new File(directorypath + back + "." + mov);
				if (mpgfile.exists()) {
					try {
						backbmp = this.loadPicture(-1, mpgfile);						
						break;
					} catch (Exception e) {
						Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					} catch (Error e) {
						Logger.getGlobal().severe("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					}
				}
			}			
		}

		bgamap = new Pixmap[model.getBgaList().length];
		int id = 0;
		for (String name : model.getBgaList()) {
			name = name.substring(0, name.lastIndexOf('.'));

			for (String mov : mov_extension) {
				File mpgfile = new File(directorypath + name + "." + mov);
				if (mpgfile.exists()) {
					try {
						MovieManager mm = this.loadMovie(id, mpgfile);
						mpgmap.put(id, mm);
						break;
					} catch (Exception e) {
						Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					} catch (Error e) {
						Logger.getGlobal().severe("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			for (String mov : pic_extension) {
				File mpgfile = new File(directorypath + name + "." + mov);
				if (mpgfile.exists()) {
					try {
						Pixmap pix = this.loadPicture(id, mpgfile);
						bgamap[id] = pix;
						break;
					} catch (Exception e) {
						Logger.getGlobal().warning("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					} catch (Error e) {
						Logger.getGlobal().severe("BGAファイル読み込み失敗。" + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			progress += 1f / model.getBgaList().length;
			id++;
		}
		Logger.getGlobal().info("BGAファイル読み込み完了。BGA数:" + model.getBgaList().length);
		progress = 1;
	}

	private MovieManager loadMovie(int id, File f) throws Exception {
		if (config.getVlcpath().length() > 0) {
			MovieManager mm = new MovieManager();
			mm.create(config.getVlcpath(), f.getPath());
			return mm;
		}
		return null;
	}

	private Pixmap loadPicture(int id, File f) throws Exception {
		Pixmap tex = new Pixmap(Gdx.files.internal(f.getPath()));
		System.out.println("BGA ID:" + id + "  path;" + f.getPath());
		return tex;
	}
	
	public Pixmap getBackbmpData() {
		return backbmp;
	}

	public Pixmap getBGAData(int id) {
		if (progress != 1 || id == -1) {
			return null;
		}

		Pixmap pix = bgamap[id];
		if(pix != null) {
			return pix;
		}
		if (mpgmap.get(id) != null) {
			return mpgmap.get(id).getBGAData();
		}
		return null;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (Pixmap id : bgamap) {
			if (id != null) {
				id.dispose();
			}
		}
		for (int id : mpgmap.keySet()) {
			if (mpgmap.get(id) != null) {
				mpgmap.get(id).dispose();
			}
		}
		mpgmap.clear();
	}

	public float getProgress() {
		return progress;
	}
}

class MovieManager {

	private OrthographicCamera camera;

	int w = 512;
	int h = 384;

	private BufferedImage image;

	private MediaPlayerFactory factory;
	private DirectMediaPlayer mediaPlayer;
	private Pixmap pixmap;

	private boolean play = false;

	public void create(String vlcpath, String filepath) throws Error {

		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		camera.update();

		LibXUtil.initialise();
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcpath);

		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
				.createCompatibleImage((int) w, (int) h);
		image.setAccelerationPriority(1.0f);

		String[] args = null;
		if ((System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)) {
			String pluginpath = vlcpath.substring(0, vlcpath.lastIndexOf('/')) + "/plugins";
			System.out.println(pluginpath);
			uk.co.caprica.vlcj.binding.LibC.INSTANCE.setenv("VLC_PLUGIN_PATH", pluginpath, 1);
			args = new String[] { "--no-video-title-show", "--verbose=3", "--vout=macosx" };
		} else {
			args = new String[] { "--no-video-title-show", "--verbose=3" };
		}

		factory = new MediaPlayerFactory(args);
		mediaPlayer = factory.newDirectMediaPlayer(new TestBufferFormatCallback(), new TestRenderCallback());
		mediaPlayer.prepareMedia(filepath);
		mediaPlayer.start();
		mediaPlayer.pause();

		System.out.println(LibVlc.INSTANCE.libvlc_get_version());
	}

	public Pixmap getBGAData() {
		if (!play) {
			mediaPlayer.start();
			play = true;
		}
		return pixmap;
	}

	public void dispose() {
		mediaPlayer.release();
	}

	private final class TestRenderCallback extends RenderCallbackAdapter {

		final long[] nativeData = new long[] { 0, w, h, Gdx2DPixmap.GDX2D_FORMAT_RGBA8888 };

		private ByteBuffer byteBuffer;
		private int size;

		public TestRenderCallback() {
			super(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
		}

		@Override
		public void onDisplay(DirectMediaPlayer mediaPlayer, int[] data) {
			if (size != data.length) {
				byteBuffer = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder());
				size = data.length;
			}
			IntBuffer intBuffer = byteBuffer.asIntBuffer();
			intBuffer.put(data);

			try {
				Gdx2DPixmap pixmapData = new Gdx2DPixmap(byteBuffer, nativeData);
				pixmap = new Pixmap(pixmapData);
//				System.out.println("movie pixmap created : " + mediaPlayer.getTime() + " / " + mediaPlayer.getLength()
//						+ "   data size : " + data.length * 4);
			} catch (Exception e) {
				pixmap = null;
				throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
			}

		}
	}

	private final class TestBufferFormatCallback implements BufferFormatCallback {

		@Override
		public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
			sourceWidth = w;
			sourceHeight = h;

			System.out.println("Got VideoFormat: " + sourceWidth + "x" + sourceHeight);

			BufferFormat format = new BufferFormat("RGBA", sourceWidth, sourceHeight, new int[] { sourceWidth * 4 },
					new int[] { sourceHeight });

			return format;
		}

	}

	private final class FrameBuffer {
		public int time;
		public Pixmap frame;
	}
}
