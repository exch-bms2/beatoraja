package bms.player.beatoraja.play.bga;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.logging.Logger;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.play.BMSPlayer;

/**
 * ffmpegを使用した動画表示用クラス
 *
 * @author exch
 */
public class FFmpegProcessor implements MovieProcessor {

	// TODO フレームレートが違う場合がある

	/**
	 * 現在表示中のフレームのTexture
	 */
	private Texture showingtex;
	/**
	 * 動画のフレーム表示率(1/n)
	 */
	private int fpsd = 1;
	/**
	 * ffmpegアクセサ
	 */
	private FFmpegFrameGrabber grabber;
	/**
	 * 動画再生用スレッド
	 */
	private MovieSeekThread movieseek;

	private BMSPlayer player;
	/**
	 * 動画色補正用シェーダ
	 */
	private ShaderProgram shader;

	public FFmpegProcessor(int fpsd) {
		this.fpsd = fpsd;
	}

	public void setBMSPlayer(BMSPlayer player) {
		this.player = player;
	}

	@Override
	public void create(String filepath) {
		try {
			RandomAccessFile file = new RandomAccessFile(filepath, "r");
			file.getChannel().map(MapMode.READ_ONLY, 0, file.length()).load();
			file.close();

			grabber = new FFmpegFrameGrabber(filepath);
			grabber.start();
			while(grabber.getVideoBitrate() < 10) {
				grabber.setVideoStream(grabber.getVideoStream() + 1);
				grabber.restart();
			}
			Logger.getGlobal().info(
					"movie decode - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat() + " size : "
							+ grabber.getImageWidth() + " x " + grabber.getImageHeight()
							+ " length (frame / time) : " + grabber.getLengthInFrames() + " / "
							+ grabber.getLengthInTime());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public Texture getFrame() {
		return showingtex;
	}

	/**
	 * 動画再生用スレッド
	 *
	 * @author exch
	 */
	class MovieSeekThread extends Thread {

		public boolean stop = false;
		public boolean restart = false;
		public boolean loop = false;

		private Pixmap pixmap;

		private final long[] nativeData = {0, grabber.getImageWidth(), grabber.getImageHeight(),
				Gdx2DPixmap.GDX2D_FORMAT_RGB888 };

		private final Runnable updateTexture = new Runnable() {
			@Override
			public void run() {
				if(!stop){
					if (showingtex != null) {
						showingtex.draw(pixmap, 0, 0);
					} else {
						showingtex = new Texture(pixmap);
					}
				}
			}
		};

		public void run() {
			try {
				double fps = grabber.getFrameRate();
				if (fps > 240) {
					// フレームレートが大きすぎる場合は手動で修正(暫定処置)
					fps = 30;
				}
				long start = player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000);
				int framecount = 0;
				Frame frame = null;
				while (!stop) {
					final long time = (player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000)) - start;
					if (time >= framecount * 1000 / fps) {
						while (time >= framecount * 1000 / fps || framecount % fpsd != 0) {
							frame = grabber.grabImage();
							framecount++;
						}
						if (frame == null) {
							if(loop) {
								restart = true;
							} else {
								try {
									sleep(3600000);
								} catch (InterruptedException e) {

								}
							}
						} else if (frame.image != null && frame.image[0] != null) {
							try {
								Gdx2DPixmap pixmapData = new Gdx2DPixmap((ByteBuffer) frame.image[0], nativeData);
								if(pixmap == null) {
									pixmap = new Pixmap(pixmapData);
								}
								Gdx.app.postRunnable(updateTexture);
								// System.out.println("movie pixmap created : "
								// + time);
							} catch (Throwable e) {
								throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
							}
						}
					} else {
						final long sleeptime = (long) (framecount * 1000 / fps - time + 1);
						if (sleeptime > 0) {
							try {
								sleep(sleeptime);
							} catch (InterruptedException e) {

							}
						}
					}
					if (restart) {
						restart = false;
						pixmap = null;
						grabber.start();
//						grabber.restart();
						start = player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000);
						framecount = 0;
//						System.out.println("movie restart - starttime : " + start);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				try {
					grabber.stop();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		}
	}

	public ShaderProgram getShader() {
		if(shader == null) {
			String vertex = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
					+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
					+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
					+ "uniform mat4 u_projTrans;\n" //
					+ "varying vec4 v_color;\n" //
					+ "varying vec2 v_texCoords;\n" //
					+ "\n" //
					+ "void main()\n" //
					+ "{\n" //
					+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
					+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
					+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
					+ "}\n";

			String fragment = "#ifdef GL_ES\n" //
					+ "#define LOWP lowp\n" //
					+ "precision mediump float;\n" //
					+ "#else\n" //
					+ "#define LOWP \n" //
					+ "#endif\n" //
					+ "varying LOWP vec4 v_color;\n" //
					+ "varying vec2 v_texCoords;\n" //
					+ "uniform sampler2D u_texture;\n" //
					+ "void main()\n"//
					+ "{\n" //
					+ "    vec4 c4 = texture2D(u_texture, v_texCoords);\n"
					+ "gl_FragColor = v_color * vec4(c4.b, c4.g, c4.r, c4.a);\n" + "}";
			shader = new ShaderProgram(vertex, fragment);
		}
		return shader;
	}

	@Override
	public void dispose() {
		stop();
		try {
			long l = System.currentTimeMillis();
			while(movieseek.isAlive() && System.currentTimeMillis() - l < 2000);
			grabber.release();
		} catch (Throwable e) {

		}
		if (showingtex != null) {
			showingtex.dispose();
		}

		if(shader != null) {
			try {
				shader.dispose();
			} catch(Throwable e) {

			}
			shader = null;
		}
	}

	public void play(boolean loop) {
		if (movieseek != null) {
			synchronized (movieseek) {
				// 再生中
				if(movieseek.isAlive()) {
					movieseek.loop = loop;
					movieseek.restart = true;
					movieseek.interrupt();
				} else {
					// 再生停止時
					try {
						grabber.start();
						movieseek = new MovieSeekThread();
						movieseek.loop = loop;
						movieseek.start();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			// 新規再生
			movieseek = new MovieSeekThread();
			movieseek.loop = loop;
			movieseek.start();
		}
	}

	public void stop() {
		if (movieseek != null && movieseek.isAlive()) {
			synchronized (movieseek) {
				movieseek.stop = true;
				movieseek.interrupt();
			}
		}
	}

}
