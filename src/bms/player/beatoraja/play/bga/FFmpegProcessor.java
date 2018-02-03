package bms.player.beatoraja.play.bga;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * ffmpegを使用した動画表示用クラス
 *
 * @author exch
 */
public class FFmpegProcessor implements MovieProcessor {

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

	private TimerObserver timerObserver = () -> {
		return System.nanoTime() / 1000;
	};

	public FFmpegProcessor(int fpsd) {
		this.fpsd = fpsd;
	}

	public void setTimerObserver(TimerObserver timerObserver) {
		this.timerObserver = timerObserver;
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
	
	public void play(boolean loop) {
		movieseek.exec(loop ? Command.LOOP : Command.PLAY);
	}

	public void stop() {
		movieseek.exec(Command.STOP);
	}

	@Override
	public void dispose() {
		if (movieseek != null) {
			movieseek.exec(Command.HALT);
			movieseek = null;
		}

		if (showingtex != null) {
			showingtex.dispose();
		}
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
		
		private long offset;
		private long framecount;

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
				while (grabber.getVideoBitrate() < 10) {
					final int videoStream = grabber.getVideoStream();
					try {
						if (videoStream < 5) {
							grabber.setVideoStream(videoStream + 1);
							grabber.restart();
						} else {
							grabber.setVideoStream(-1);
							grabber.restart();
							break;
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				Logger.getGlobal()
						.info("movie decode - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat()
								+ " size : " + grabber.getImageWidth() + " x " + grabber.getImageHeight()
								+ " length (frame / time) : " + grabber.getLengthInFrames() + " / "
								+ grabber.getLengthInTime());

				final long[] nativeData = { 0, grabber.getImageWidth(), grabber.getImageHeight(),
						Gdx2DPixmap.GDX2D_FORMAT_RGB888 };

				offset = grabber.getTimestamp();
				Frame frame = null;
				boolean halt = false;
				boolean loop = false;
				while (!halt) {
					final long microtime = timerObserver.getMicroTime() + offset;
					if (eof) {
						try {
							sleep(3600000);
						} catch (InterruptedException e) {

						}
					} else if (microtime >= grabber.getTimestamp()) {
						while (microtime >= grabber.getTimestamp() || framecount % fpsd != 0) {
							frame = grabber.grabImage();
							if (frame == null) {
								break;
							}
							framecount++;
							// System.out.println("time : " + grabber.getTimestamp() + " --- " + time);
						}
						if (frame == null) {
							eof = true;
							if (loop) {
								commands.addLast(Command.PLAY);
							}
						} else if (frame.image != null && frame.image[0] != null) {
							try {
								Gdx2DPixmap pixmapData = new Gdx2DPixmap((ByteBuffer) frame.image[0], nativeData);
								if (pixmap == null) {
									pixmap = new Pixmap(pixmapData);
								}
								Gdx.app.postRunnable(() -> {
									final Pixmap p = pixmap;
									if (p == null) {
										return;
									}
									if (showingtex != null) {
										showingtex.draw(p, 0, 0);
									} else {
										showingtex = new Texture(p);
									}
								});
								// System.out.println("movie pixmap created : " + time);
							} catch (Throwable e) {
								throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
							}
						}
					} else {
						final long sleeptime = (grabber.getTimestamp() - microtime) / 1000 - 1;
						if (sleeptime > 0) {
							try {
								sleep(sleeptime);
							} catch (InterruptedException e) {

							}
						}
					}

					if (!commands.isEmpty()) {
						switch (commands.pollFirst()) {
						case PLAY:
							loop = false;
							restart();
							break;
						case LOOP:
							loop = true;
							restart();
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
		
		private void restart() throws Exception {
			pixmap = null;
			grabber.restart();
			grabber.grabFrame();
			eof = false;
			offset = grabber.getTimestamp() - timerObserver.getMicroTime();
			framecount = 1;
			// System.out.println("movie restart - starttime : " + start);
		}

		public void exec(Command com) {
			commands.addLast(com);
			interrupt();
		}
	}

	enum Command {
		PLAY, LOOP, STOP, HALT;
	}
	
	public interface TimerObserver {
		
		public long getMicroTime();
	}
}
