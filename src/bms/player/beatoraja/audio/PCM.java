package bms.player.beatoraja.audio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.jflac.FLACDecoder;
import org.jflac.metadata.StreamInfo;

import com.badlogic.gdx.backends.lwjgl.audio.OggInputStream;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.StreamUtils.OptimizedByteArrayOutputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.MP3Decoder;
import javazoom.jl.decoder.OutputBuffer;

/**
 * PCM音源処理用クラス
 * 
 * @author exch
 */
public abstract class PCM<T> {

	protected static final boolean USE_UNSAFE = false;

	/**
	 * チャンネル数
	 */
	public final int channels;
	/**
	 * 音源のサンプリングレート(Hz)
	 */
	public final int sampleRate;
	/**
	 * PCMデータ
	 */
	public final T sample;
	/**
	 * PCMデータ開始位置
	 */
	public final int start;
	/**
	 * PCMデータ長
	 */	
	public final int len;

	PCM(int channels, int sampleRate, int start, int len, T sample) {
		this.channels = channels;
		this.sampleRate = sampleRate;
		this.start = start;
		this.len = len;
		this.sample = sample;
	}

	public static PCM load(Path p, AudioDriver driver) {
		try {
			PCMLoader loader = new PCMLoader(driver);
			loader.loadPCM(p);
			
			PCM pcm = null;
			if(loader.bitsPerSample > 16) {
//				System.out.println("FLOAT");
				pcm = FloatPCM.loadPCM(loader);				
			} else if(loader.bitsPerSample == 16) {
				if(loader.pcm.isDirect()) {
					pcm = ShortDirectPCM.loadPCM(loader);
				} else {
					pcm = ShortPCM.loadPCM(loader);					
				}
			} else {
				// TODO BytePCMのバグが解消されたら切替
//				pcm = BytePCM.loadPCM(loader);					
				pcm = ShortPCM.loadPCM(loader);					
			}
			
			// TODO PCMLoader側での逐次変換が実装されたら削除
			if(pcm != null && ((AbstractAudioDriver)driver).channels != 0 && pcm.channels != ((AbstractAudioDriver)driver).channels) {
				pcm = pcm.changeChannels(((AbstractAudioDriver)driver).channels);
			}
			if(pcm != null && ((AbstractAudioDriver)driver).sampleRate != 0 && pcm.sampleRate != ((AbstractAudioDriver)driver).sampleRate) {
				pcm = pcm.changeSampleRate(((AbstractAudioDriver)driver).sampleRate);
			}
			return pcm;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static PCM load(String name, AudioDriver driver) {
		for(Path path : AudioDriver.getPaths(name)) {
			PCM pcm = PCM.load(path,driver);
			if(pcm != null) {
				return pcm;
			}			
		}
		return null;
	}
	
	/**
	 * サンプリングレートを変更したPCMを返す
	 * 
	 * @param sample
	 *            サンプリングレート
	 * @return サンプリングレート変更後のPCM
	 */
	public abstract PCM<T> changeSampleRate(int sample);

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public abstract PCM<T> changeFrequency(float rate);
	
	/**
	 * チャンネル数を変更したPCMを返す
	 * 
	 * @param channels
	 *            チャンネル数
	 * @return チャンネル数を変更したPCM
	 */
	public abstract PCM<T> changeChannels(int channels);
	/**
	 * トリミングしたPCMを返す
	 * 
	 * @param starttime
	 *            開始時間(us)
	 * @param duration
	 *            再生時間(us)
	 * @return トリミングしたPCM
	 */
	public abstract PCM<T> slice(long starttime, long duration);
	
	protected static ByteBuffer getDirectByteBuffer(int capacity) {
		ByteBuffer result = USE_UNSAFE ? BufferUtils.newUnsafeByteBuffer(capacity) : ByteBuffer.allocateDirect(capacity);
		return result.order(ByteOrder.LITTLE_ENDIAN);
	}

	static class PCMLoader {
		
		ByteBuffer pcm;
		int channels = 0;
		int sampleRate = 0;
		int bitsPerSample = 0;
		
		private final AudioDriver driver;
		
		public PCMLoader(AudioDriver driver) {
			this.driver = driver;
		};
		
		public void loadPCM(Path p) throws IOException {
			// TODO prefferedSampleRate, prefferedChannelsを使って逐次変換し、メモリ確保のコストを減らす
			// final long time = System.nanoTime();
			pcm = null;

			final String name = p.toString().toLowerCase();
			if (name.endsWith(".wav")) {
				try (WavInputStream input = new WavInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
					switch(input.type) {
					case 1:
					case 3:
					{
						channels = input.channels;
						sampleRate = input.sampleRate;
						bitsPerSample = input.bitsPerSample;
						
						if(bitsPerSample == 16) {
							pcm = getDirectByteBuffer(input.dataRemaining);
							StreamUtils.copyStream(input, pcm);
						} else {
							OptimizedByteArrayOutputStream output = new OptimizedByteArrayOutputStream(input.dataRemaining);
							StreamUtils.copyStream(input, output);
							pcm = ByteBuffer.wrap(output.getBuffer()).order(ByteOrder.LITTLE_ENDIAN);
							pcm.limit(output.size());
						}
						
						break;					
					}
					case 85:
						// mp3
					{
						try {
							Bitstream bitstream = new Bitstream(new ByteArrayInputStream(
									StreamUtils.copyStreamToByteArray(input, input.dataRemaining)));
							ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
							MP3Decoder decoder = new MP3Decoder();
							OutputBuffer outputBuffer = null;
							while (true) {
								Header header = bitstream.readFrame();
								if (header == null)
									break;
								if (outputBuffer == null) {
									channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
									outputBuffer = new OutputBuffer(channels, false);
									decoder.setOutputBuffer(outputBuffer);
									sampleRate = header.getSampleRate();
								}
								try {
									decoder.decodeFrame(header, bitstream);
								} catch (Exception ignored) {
									// JLayer's decoder throws
									// ArrayIndexOutOfBoundsException
									// sometimes!?
								}
								bitstream.closeFrame();
								output.write(outputBuffer.getBuffer(), 0, outputBuffer.reset());
							}
							bitstream.close();
							byte[] bytes = output.toByteArray();
							pcm = getDirectByteBuffer(bytes.length).put(bytes);
							bitsPerSample = 16;
						} catch (BitstreamException e) {
							e.printStackTrace();
						}
						break;					
					}
					default:
						throw new IOException(p.toString() + " unsupported WAV format ID : " + input.type);					
					}
				} catch (Throwable e) {
					Logger.getGlobal().warning("WAV処理中の例外 - file : " + p + " error : "+ e.getMessage());
				}
			} else if (name.endsWith(".ogg")) {
				// ogg
				try (OggInputStream input = new OggInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
					// final long time = System.nanoTime();
					// OptimizedByteArrayOutputStream output = new
					// OptimizedByteArrayOutputStream(4096);
					OptimizedByteArrayOutputStream output = new OptimizedByteArrayOutputStream(input.getLength() * 16);
					byte[] buff = new byte[4096];
					while (!input.atEnd()) {
						int length = input.read(buff);
						if (length == -1)
							break;
						output.write(buff, 0, length);
					}
					
					channels = input.getChannels();
					sampleRate = input.getSampleRate();
					bitsPerSample = 16;
					
					pcm = getDirectByteBuffer(output.size()).put(output.getBuffer(), 0, output.size());
//					System.out.println(name + " - length : " + input.getLength() + " ( " + input.getLength() * 16 + " ) " + " , bytes : " + bytes);
				} catch (Throwable ex) {
				}
			} else if (name.endsWith(".mp3")) {
				// mp3
				try {
					Bitstream bitstream = new Bitstream(new BufferedInputStream(Files.newInputStream(p)));
					ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
					MP3Decoder decoder = new MP3Decoder();
					OutputBuffer outputBuffer = null;
					while (true) {
						Header header = bitstream.readFrame();
						if (header == null)
							break;
						if (outputBuffer == null) {
							channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
							outputBuffer = new OutputBuffer(channels, false);
							decoder.setOutputBuffer(outputBuffer);
							sampleRate = header.getSampleRate();
						}
						try {
							decoder.decodeFrame(header, bitstream);
						} catch (Exception ignored) {
							// JLayer's decoder throws
							// ArrayIndexOutOfBoundsException
							// sometimes!?
						}
						bitstream.closeFrame();
						output.write(outputBuffer.getBuffer(), 0, outputBuffer.reset());
					}
					bitstream.close();
					byte[] bytes = output.toByteArray();
					pcm = getDirectByteBuffer(bytes.length).put(bytes);
					bitsPerSample = 16;
				} catch (Throwable ex) {
				}
			} else if (name.endsWith(".flac")) {
				// flac
				try {
					FLACDecoder input = new FLACDecoder(new BufferedInputStream(Files.newInputStream(p)));
					input.readMetadata();
					StreamInfo info = input.getStreamInfo();
					
					channels = info.getChannels();
					sampleRate = info.getSampleRate();
					bitsPerSample = info.getBitsPerSample();
					
					OptimizedByteArrayOutputStream output = new OptimizedByteArrayOutputStream((int)info.getTotalSamples() * 16);
					input.addPCMProcessor(new FlacProcessor(output));
					
					input.decodeFrames();
					
					if(bitsPerSample == 16) {
						pcm = getDirectByteBuffer(output.size()).put(output.getBuffer(), 0, output.size());						
					} else {
						pcm = ByteBuffer.wrap(output.getBuffer()).order(ByteOrder.LITTLE_ENDIAN);
						pcm.limit(output.size());						
					}
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}

			if(pcm == null) {
				throw new IOException(p.toString() + " : can't convert to PCM");			
			}
			
			int bytes = pcm.limit();
			bytes -= bytes % (channels > 1 ? bitsPerSample / 4 : bitsPerSample / 8);
//			final int orgbytes = bytes;
			while(bytes > channels * bitsPerSample / 8) {
				boolean zero = true;
				for(int i = 0;i < channels * bitsPerSample / 8;i++){
					zero &= (pcm.get(bytes - i - 1) == 0x00);
				}
				if(zero) {
					bytes -= channels * bitsPerSample / 8;
				} else {
					break;
				}
			}
//			if(bytes != orgbytes) {
//				Logger.getGlobal().info("終端の無音データ除外 - " + p.getFileName().toString() + " : " + (orgbytes - bytes) + " bytes");
//			}
			if(bytes < channels * bitsPerSample / 8) {
				throw new IOException(p.toString() + " : 0 samples");			
			}
			if(sampleRate == 0) {
				throw new IOException(p.toString() + " : 0 sample rate");			
			}
			pcm.limit(bytes);
			
//			System.out.println(p.getFileName().toString() + " - " + sampleRate + " Hz, " + bitsPerSample + " bits, " + channels + " channels");
		}
		
	}
	
	/** @author Nathan Sweet */
	private static class WavInputStream extends FilterInputStream {
		private int dataRemaining;
		/**
		 * PCMのタイプ
		 */
		private final int type;
		private final int channels;
		private final int sampleRate;
		/**
		 * 1サンプル当たりのビット数
		 */
		private final int bitsPerSample;		

		WavInputStream(InputStream p) {
			super(p);
			try {
				if (read() != 'R' || read() != 'I' || read() != 'F' || read() != 'F')
					throw new RuntimeException("RIFF header not found: " + p.toString());

				skipFully(4);

				if (read() != 'W' || read() != 'A' || read() != 'V' || read() != 'E')
					throw new RuntimeException("Invalid wave file header: " + p.toString());

				int fmtChunkLength = seekToChunk('f', 'm', 't', ' ');

				type = read() & 0xff | (read() & 0xff) << 8;

				channels = read() & 0xff | (read() & 0xff) << 8;

				sampleRate = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;

				skipFully(6);

				bitsPerSample = read() & 0xff | (read() & 0xff) << 8;

				skipFully(fmtChunkLength - 16);

				dataRemaining = seekToChunk('d', 'a', 't', 'a');
			} catch (Throwable ex) {
				StreamUtils.closeQuietly(this);
				throw new RuntimeException("Error reading WAV file: " + p.toString(), ex);
			}
		}

		private int seekToChunk(char c1, char c2, char c3, char c4) throws IOException {
			while (true) {
				boolean found = read() == c1;
				found &= read() == c2;
				found &= read() == c3;
				found &= read() == c4;
				int chunkLength = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;
				if (chunkLength == -1)
					throw new IOException("Chunk not found: " + c1 + c2 + c3 + c4);
				if (found)
					return chunkLength;
				skipFully(chunkLength);
			}
		}

		private void skipFully(int count) throws IOException {
			while (count > 0) {
				long skipped = in.skip(count);
				if (skipped <= 0)
					throw new EOFException("Unable to skip.");
				count -= skipped;
			}
		}

		public int read(byte[] buffer) throws IOException {
			if (dataRemaining == 0)
				return -1;
			int length = Math.min(super.read(buffer), dataRemaining);
			if (length == -1)
				return -1;
			dataRemaining -= length;
			return length;
		}
	}
}
