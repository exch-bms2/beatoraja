package bms.player.beatoraja.play.bga;

import static bms.player.beatoraja.skin.SkinProperty.*;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.LinkedBlockingDeque;
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
		movieseek = new MovieSeekThread(filepath);
		movieseek.start();
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
		/**
		 * ffmpegアクセサ
		 */
		private FFmpegFrameGrabber grabber;
		/**
		 * コマンドキュー
		 */
		private LinkedBlockingDeque<Command> commands = new LinkedBlockingDeque<>(4);
		
		private boolean eof = true;

		private Pixmap pixmap;

		private String filepath;

		private final Runnable updateTexture = new Runnable() {
			@Override
			public void run() {
				final Pixmap p = pixmap;
				if(p == null) {
					return;
				}
				if (showingtex != null) {
					showingtex.draw(p, 0, 0);
				} else {
					showingtex = new Texture(p);
				}
			}
		};
		
		public MovieSeekThread(String filepath) {
			this.filepath = filepath;
			try {
				RandomAccessFile file = new RandomAccessFile(filepath, "r");
				file.getChannel().map(MapMode.READ_ONLY, 0, file.length()).load();
				file.close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				grabber = new FFmpegFrameGrabber(filepath);
				grabber.start();
				while(grabber.getVideoBitrate() < 10) {
					final int videoStream = grabber.getVideoStream();
					if(videoStream < 5) {
						grabber.setVideoStream(videoStream + 1);
						grabber.restart();						
					} else {
						grabber.setVideoStream(-1);
						grabber.restart();
						break;
					}
				}
				Logger.getGlobal().info(
						"movie decode - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat() + " size : "
								+ grabber.getImageWidth() + " x " + grabber.getImageHeight()
								+ " length (frame / time) : " + grabber.getLengthInFrames() + " / "
								+ grabber.getLengthInTime());

				double fps = grabber.getFrameRate();
				long[] nativeData = {0, grabber.getImageWidth(), grabber.getImageHeight(),
						Gdx2DPixmap.GDX2D_FORMAT_RGB888 };

				if (fps > 240) {
					// フレームレートが大きすぎる場合は手動で修正(暫定処置)
					fps = 30;
				}
				long start = 0;
				int framecount = 0;
				Frame frame = null;
				boolean halt = false;
				boolean loop = false;
				while (!halt) {
					final long time = (player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000)) - start;
					if(eof) {
						try {
							sleep(3600000);
						} catch (InterruptedException e) {

						}						
					} else if (time >= framecount * 1000 / fps) {
						while (time >= framecount * 1000 / fps || framecount % fpsd != 0) {
							frame = grabber.grabImage();
							framecount++;
						}
						if (frame == null) {
							eof = true;
							if(loop) {
								commands.addLast(Command.PLAY);
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
					
					if(!commands.isEmpty()) {
						switch(commands.pollFirst()) {
						case PLAY:
							loop = false;
							pixmap = null;
//							grabber.start();
							grabber.restart();
							eof = false;
							start = player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000);
							framecount = 0;
//							System.out.println("movie restart - starttime : " + start);
							break;
						case LOOP:
							loop = true;
							pixmap = null;
//							grabber.start();
							grabber.restart();
							eof = false;
							start = player != null ? player.getNowTime() - player.getTimer()[TIMER_PLAY] : (System.nanoTime() / 1000000);
							framecount = 0;
//							System.out.println("movie restart - starttime : " + start);
							break;
						case STOP:
							eof = true;
							break;
						case HALT:
							halt = true;
						}						
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				try {
					grabber.stop();
					grabber.release();
					Logger.getGlobal().info("動画リソースの開放 : " + filepath);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		}
		
		public void exec(Command com) {
			commands.addLast(com);
			interrupt();
		}
	}
	
	enum Command {
		PLAY,LOOP,STOP,HALT;
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
		if(movieseek != null) {
			movieseek.exec(Command.HALT);
			movieseek = null;
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
		movieseek.exec(loop ? Command.LOOP : Command.PLAY);		
	}

	public void stop() {
		movieseek.exec(Command.STOP);
	}

}
