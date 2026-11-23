package bms.player.beatoraja.video;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.ImageMode;
import org.bytedeco.javacv.FrameGrabber.Exception;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;

import bms.player.beatoraja.song.SongResource;
import bms.player.beatoraja.song.SongResources;

/**
 * ffmpegを使用した動画表示用クラス
 *
 * @author exch
 */
public class FFmpegProcessor implements VideoProcessor {
	/** JavaCV 1.5.14 exposes the borrowed AVFrame through processed RAW frames without owning their planes. */
	static Frame grabDecodedFrame(FFmpegFrameGrabber grabber) throws Exception {
		return grabber.grabImage();
	}
	private enum ProcessorStatus {
		TEXTURE_INACTIVE,
		TEXTURE_ACTIVE,
		DISPOSED,
	}

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
	private final Object pixmapLock = new Object();

	private volatile long time;
	/**
	 * dispose()を呼び出した後にprocessorDisposedはtrueになる
	 */
	private volatile ProcessorStatus processorStatus = ProcessorStatus.TEXTURE_INACTIVE;

	public FFmpegProcessor(int fpsd) {
		this.fpsd = fpsd;
	}

	public void create(String filepath) {
		create(SongResources.fromPath(java.nio.file.Path.of(filepath)));
	}

	/** Starts decoding from a resource without extracting its containing archive. */
	public void create(SongResource resource) {
		movieseek = new MovieSeekThread(resource);
		movieseek.start();
	}

	@Override
	public Texture getFrame(long time) {
		this.time = time;
		MovieSeekThread worker = movieseek;
		if (worker != null && time * 1000 >= worker.wakeAtMicros) {
			worker.signalPlaybackUpdate();
		}
		if (processorStatus == ProcessorStatus.TEXTURE_ACTIVE) {
			return showingtex;
		} else {
			return null;
		}
	}
	
	public void play(long time, boolean loop) {
		if (processorStatus == ProcessorStatus.DISPOSED) return;
		this.time = time;
		movieseek.exec(loop ? Command.LOOP : Command.PLAY);
	}

	public void stop() {
		if (processorStatus == ProcessorStatus.DISPOSED) return;
		movieseek.exec(Command.STOP);
	}

	@Override
	public void dispose() {
		synchronized (pixmapLock) {
			if (processorStatus == ProcessorStatus.DISPOSED) return;
			processorStatus = ProcessorStatus.DISPOSED;
			if (movieseek != null) {
				movieseek.exec(Command.HALT);
				movieseek = null;
			}

			if (showingtex != null) {
				showingtex.dispose();
				showingtex = null;
			}
		}
	}

	/** Returns the compressed movie bytes currently retained for stream-backed decoding. */
	public long getRetainedMovieBytes() {
		MovieSeekThread seekThread = movieseek;
		return seekThread != null ? seekThread.getRetainedMovieBytes() : 0;
	}

	/**
	 * 動画再生用スレッド
	 *
	 * @author exch
	 */
	class MovieSeekThread extends Thread {
		/**
		 * FFmpegFrameGrabber::setVideoFrameNumber
		 * 1.4.1以前のJavaCVには存在しない
		 */
		private static final Method setVideoFrameNumber;
		static {
			Method method = null;
			try {
				method = FFmpegFrameGrabber.class.getMethod("setVideoFrameNumber", int.class);
			} catch (NoSuchMethodException | SecurityException ignored) {}
			setVideoFrameNumber = method;
		}

		/**
		 * ffmpegアクセサ
		 */
		private FFmpegFrameGrabber grabber;
		private BgrFrameConverter converter = new BgrFrameConverter();
		/**
		 * コマンドキュー
		 */
		private LinkedBlockingDeque<Command> commands = new LinkedBlockingDeque<>(4);

		private boolean eof = true;
		private volatile boolean haltRequested;
		private volatile boolean finished;
		private volatile Throwable uploadFailure;
		private volatile String phase = "resource-read";
		private volatile Command lastCommand;
		private volatile long uploadCount;
		private volatile long restartCount;
		private volatile long waitMillis;
		private volatile long wakeAtMicros = Long.MAX_VALUE;

		private Pixmap pixmap;
		private boolean uploadPending;
		private volatile byte[] movieBytes;
		private File movieFile;

		private final SongResource resource;
		
		private long offset;
		private long framecount;

		public MovieSeekThread(SongResource resource) {
			this.resource = resource;
		}

		public void run() {
			String exitReason = "halt";
			boolean cleanupFailed = false;
			try {
				if (haltRequested) return;
				movieFile = resource.localPath().map(Path::toFile).orElse(null);
				if (movieFile == null) {
					try (InputStream input = resource.openStream()) {
						movieBytes = input.readAllBytes();
					}
				}
				if (haltRequested) return;
				phase = "initialize";
				openGrabber();
				if (haltRequested) return;
				Logger.getGlobal()
						.info("movie decode - fps : " + grabber.getFrameRate() + " format : " + grabber.getFormat()
								+ " size : " + grabber.getImageWidth() + " x " + grabber.getImageHeight()
								+ " length (frame / time) : " + grabber.getLengthInFrames() + " / "
								+ grabber.getLengthInTime() + " memory : " + getRetainedMovieBytes() + " bytes");

				offset = grabber.getTimestamp();
				Frame frame = null;
				boolean loop = false;
				while (!haltRequested) {
					Command command = null;
					final long microtime = time * 1000 + offset;
					if (eof) {
						synchronized (pixmapLock) {
							if (processorStatus != ProcessorStatus.DISPOSED) {
								processorStatus = ProcessorStatus.TEXTURE_INACTIVE;
							}
						}
						waitMillis = -1;
						phase = "command-wait";
						command = commands.takeFirst();
					} else if (microtime >= grabber.getTimestamp()) {
						phase = "decode";
						while (!haltRequested && (microtime >= grabber.getTimestamp() || framecount % fpsd != 0)) {
							frame = grabDecodedFrame(grabber);
							if (frame == null) {
								break;
							}
							framecount++;
							// System.out.println("time : " + grabber.getTimestamp() + " --- " + time);
						}
						if (frame == null) {
							eof = true;
							if (loop) {
								commands.offerLast(Command.LOOP);
							}
						} else {
							phase = "convert";
							Frame converted = converter.convert(frame);
							try {
								phase = "pixmap-copy";
								final boolean queueUpload;
								synchronized (pixmapLock) {
									if (pixmap == null || pixmap.getWidth() != converted.imageWidth || pixmap.getHeight() != converted.imageHeight) {
										if (pixmap != null) {
											pixmap.dispose();
										}
										pixmap = new Pixmap(converted.imageWidth, converted.imageHeight, Pixmap.Format.RGB888);
									}
									copyFrameToPixmap(converted, pixmap);
									queueUpload = !uploadPending;
									uploadPending = true;
								}
								if (queueUpload) {
									Gdx.app.postRunnable(this::uploadLatestFrame);
								}
							} catch (Throwable e) {
								throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
							}
						}
					} else {
						awaitFrameTime(grabber.getTimestamp() - offset);
					}

					if (command == null) {
						command = commands.pollFirst();
					}
					if (!haltRequested && command != null) {
						lastCommand = command;
						switch (command) {
							case PLAY -> {
								loop = false;
								phase = "seek";
								restart();
							}
							case LOOP -> {
								loop = true;
								phase = "seek";
								restart();
							}
							case STOP -> eof = true;
							case HALT -> haltRequested = true;
						}
					}
				}
			} catch (Throwable e) {
				exitReason = e instanceof InterruptedException ? "interrupted" : "failure";
				// The worker is exiting; clear external interruption before native cleanup.
				Thread.interrupted();
				logFailure(phase, e);
			} finally {
				try {
					synchronized (pixmapLock) {
						if (pixmap != null) {
							pixmap.dispose();
							pixmap = null;
						}
						if (processorStatus != ProcessorStatus.DISPOSED) processorStatus = ProcessorStatus.TEXTURE_INACTIVE;
					}
				} catch (Throwable e) {
					cleanupFailed = true;
					logFailure("pixmap-cleanup", e);
				} finally {
					try {
						closeGrabber();
					} catch (Throwable e) {
						cleanupFailed = true;
						logFailure("grabber-cleanup", e);
					} finally {
						try { if (converter != null) converter.close(); }
						catch (Throwable e) { cleanupFailed = true; logFailure("converter-cleanup", e); }
						converter = null;
						movieBytes = null;
						synchronized (this) {
							finished = true;
							commands.clear();
						}
						if (uploadFailure != null) exitReason = "upload-failure";
						else if (exitReason.equals("halt") && processorStatus == ProcessorStatus.DISPOSED) exitReason = "disposed";
						Logger.getGlobal().info("動画リソースの開放 : " + resource.displayPath()
								+ " reason=" + exitReason + " phase=" + phase + " lastCommand=" + lastCommand
								+ " frameCount=" + framecount + " uploads=" + uploadCount + " restarts=" + restartCount
								+ " waitMillis=" + waitMillis + " haltRequested=" + haltRequested + " cleanupFailed=" + cleanupFailed);
					}
				}
			}
		}

		private synchronized void awaitFrameTime(long frameTimeMicros) throws InterruptedException {
			phase = "frame-wait";
			waitMillis = (frameTimeMicros - time * 1000) / 1000;
			wakeAtMicros = frameTimeMicros;
			try {
				// Only getFrame or a playback command can advance the externally supplied clock.
				while (!haltRequested && commands.isEmpty() && time * 1000 < frameTimeMicros) {
					wait();
				}
			} finally {
				wakeAtMicros = Long.MAX_VALUE;
			}
		}

		private synchronized void signalPlaybackUpdate() {
			notifyAll();
		}

		private void uploadLatestFrame() {
			try {
				synchronized (pixmapLock) {
					uploadPending = false;
					final Pixmap p = pixmap;
					if (p == null || processorStatus == ProcessorStatus.DISPOSED || uploadFailure != null) {
						return;
					}
					preparePixmapForDraw(p);
					if (showingtex != null && showingtex.getWidth() == p.getWidth() && showingtex.getHeight() == p.getHeight()) {
						showingtex.draw(p, 0, 0);
					} else {
						if (showingtex != null) {
							showingtex.dispose();
						}
						showingtex = new Texture(p);
					}
					processorStatus = ProcessorStatus.TEXTURE_ACTIVE;
					uploadCount++;
				}
			} catch (Throwable e) {
				uploadFailure = e;
				logFailure("texture-upload", e);
				exec(Command.HALT);
				throw new GdxRuntimeException("Movie texture upload failed: " + resource.displayPath(), e);
			}
		}

		private void logFailure(String failurePhase, Throwable error) {
			// XMLFormatter records only the outer exception; retain causes and suppressed failures too.
			StringWriter details = new StringWriter();
			error.printStackTrace(new PrintWriter(details));
			Logger.getGlobal().log(Level.WARNING, "Movie processing failed : file=" + resource.displayPath()
					+ " phase=" + failurePhase + " lastCommand=" + lastCommand + " frameCount=" + framecount
					+ System.lineSeparator() + details, error);
		}

		private void openGrabber() throws Exception {
			grabber = movieFile != null ? new FFmpegFrameGrabber(movieFile)
					: new FFmpegFrameGrabber(new ByteArrayInputStream(movieBytes));
			grabber.setImageMode(ImageMode.RAW);
			grabber.start();
			while (!haltRequested && grabber.getVideoBitrate() < 10) {
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
					logFailure("stream-probe", e);
				}
			}
		}

		private void reopenGrabber() throws Exception {
			closeGrabber();
			openGrabber();
		}

		private void closeGrabber() throws Exception {
			if (grabber != null) {
				FFmpegFrameGrabber closing = grabber;
				grabber = null;
				try {
					closing.stop();
				} finally {
					closing.close();
				}
			}
		}
		
		private void restart() throws Exception {
			if (setVideoFrameNumber != null) {
				try {
					setVideoFrameNumber.invoke(grabber, 0);
				} catch (IllegalAccessException | InvocationTargetException e) {
					logFailure("seek-reopen", e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e);
					reopenGrabber();
				}
			} else {
				try {
					grabber.restart();
					grabDecodedFrame(grabber);
				} catch (Throwable e) {
					logFailure("restart-reopen", e);
					reopenGrabber();
				}
			}
			eof = false;
			offset = grabber.getTimestamp() - time * 1000;
			framecount = 1;
			restartCount++;
			// System.out.println("movie restart - starttime : " + start);
		}

		private void copyFrameToPixmap(Frame frame, Pixmap pixmap) {
			final ByteBuffer source = (ByteBuffer) frame.image[0];
			final ByteBuffer pixels = pixmap.getPixels();
			// BgrFrameConverter produces BGR24 with an aligned stride.
			final int rowBytes = frame.imageWidth * 3;
			final int sourceStride = frame.imageStride;
			final int byteCount = rowBytes * frame.imageHeight;

			pixels.clear();
			if (sourceStride == rowBytes) {
				pixels.put(0, source, 0, byteCount);
			} else {
				for (int y = 0; y < frame.imageHeight; y++) {
					pixels.put(y * rowBytes, source, y * sourceStride, rowBytes);
				}
			}
			pixels.limit(byteCount);
		}

		private long getRetainedMovieBytes() {
			byte[] bytes = movieBytes;
			return bytes != null ? bytes.length : 0;
		}

		private void preparePixmapForDraw(Pixmap pixmap) {
			ByteBuffer pixels = pixmap.getPixels();
			pixels.position(0);
			pixels.limit(pixmap.getWidth() * pixmap.getHeight() * 3);
		}

		public synchronized void exec(Command com) {
			if (haltRequested || finished) return;
			if (com == Command.HALT) {
				// Wake both idle and frame-clock waits without interrupting JavaCPP/file I/O.
				haltRequested = true;
				commands.clear();
				commands.offerFirst(Command.HALT);
			} else {
				commands.offerLast(com);
			}
			notifyAll();
		}
	}

	enum Command {
		PLAY, LOOP, STOP, HALT;
	}
	
	public interface TimerObserver {
		
		public long getMicroTime();
	}
}
