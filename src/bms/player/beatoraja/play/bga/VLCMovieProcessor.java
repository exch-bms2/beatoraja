package bms.player.beatoraja.play.bga;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallbackAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

public class VLCMovieProcessor implements MovieProcessor {

	private OrthographicCamera camera;

	int w = 512;
	int h = 384;

	private BufferedImage image;

	private MediaPlayerFactory factory;
	private DirectMediaPlayer mediaPlayer;
	private Pixmap pixmap;

	private boolean play = false;
	
	private String vlcpath;
	
	public VLCMovieProcessor(String vlcpath) {
		this.vlcpath = vlcpath;
	}

	public void create(String filepath) throws Error {

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
	
	private Pixmap showing;
	private Texture showingtex;

	public Texture getBGAData() {
		if (!play) {
			mediaPlayer.start();
			play = true;
		}
		if(showing != pixmap) {
			showing = pixmap;
			if(showingtex != null) {
				showingtex.dispose();
			}
			if(pixmap != null) {
				showingtex = new Texture(pixmap);				
			} else {
				showingtex = null;
			}
		}
		return showingtex;
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