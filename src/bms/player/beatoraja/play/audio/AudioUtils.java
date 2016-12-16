package bms.player.beatoraja.play.audio;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import sun.misc.IOUtils;

import com.badlogic.gdx.backends.lwjgl.audio.OggInputStream;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;

import javazoom.jl.decoder.*;

/**
 * audio関係のメソッド群
 * 
 * @author exch
 */
public class AudioUtils {

	/**
	 * 音源ファイルをlibGDXの読み込める形に変換する
	 * 
	 * @param sourceFile
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public static byte[] convertWav(Path sourceFile) throws UnsupportedAudioFileException, IOException {

		byte[] result = null;
		AudioInputStream sourceStream = null;
		sourceStream = AudioSystem.getAudioInputStream(sourceFile.toFile());
		AudioFormat sourceFormat = sourceStream.getFormat();
		// System.out.println(sourceFormat + " length : " +
		// sourceStream.getFrameLength());
		if (sourceFormat.getEncoding().toString().equals("VORBIS")) {
			AudioFormat targetFormat = new AudioFormat(Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
			// System.out.println(sourceFormat + " : "
			// +sourceFormat.getFrameSize());
			// System.out.println(targetFormat + " : "
			// +targetFormat.getFrameSize());
			AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
			Path tmp = Files.createTempFile("wav", "tmp");
			AudioSystem.write(targetStream, Type.WAVE, tmp.toFile());
			result = Files.readAllBytes(tmp);
			// System.out.println(result.length);
			Files.delete(tmp);
		} else if ((sourceFormat.getEncoding() == Encoding.PCM_SIGNED && sourceFormat.getSampleSizeInBits() == 16)
				|| (sourceFormat.getEncoding() == Encoding.PCM_FLOAT && sourceFormat.getSampleSizeInBits() == 32)) {
			return Files.readAllBytes(sourceFile);
		} else {
			AudioFormat targetFormat = new AudioFormat(Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16,
					sourceFormat.getChannels(), sourceFormat.getFrameSize() * 16 / sourceFormat.getSampleSizeInBits(),
					sourceFormat.getSampleRate(), sourceFormat.isBigEndian());
			// System.out.println(sourceFormat + " : "
			// +sourceFormat.getFrameSize());
			// System.out.println(targetFormat + " : "
			// +targetFormat.getFrameSize());
			AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			AudioSystem.write(targetStream, Type.WAVE, bos);
			result = bos.toByteArray();
		}

		return result;
	}

	/**
	 * WAVデータを指定の開始時間、間隔で切り出す
	 * 
	 * @param is
	 * @param starttime
	 * @param duration
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public static byte[] sliceWav(InputStream is, long starttime, long duration) throws UnsupportedAudioFileException,
			IOException {
		AudioInputStream sourceStream = AudioSystem.getAudioInputStream(is);
		AudioFormat format = sourceStream.getFormat();

		int bytesPerSecond = format.getFrameSize() * (int) format.getFrameRate();
		sourceStream.skip(starttime * bytesPerSecond / 1000);
		long framesOfAudioToCopy = duration * (int) format.getFrameRate() / 1000;
		if (duration == 0) {
			framesOfAudioToCopy = sourceStream.getFrameLength() * format.getFrameSize() - starttime * bytesPerSecond
					/ 1000;
		}
		AudioInputStream shortenedStream = new AudioInputStream(is, format, framesOfAudioToCopy);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		AudioSystem.write(shortenedStream, Type.WAVE, bos);
		// System.out.println("sliced WAV status - offset : " + (starttime *
		// bytesPerSecond / 1000) + " len : " + framesOfAudioToCopy);
		return bos.toByteArray();
	}

	public static  short[] convertToPCM(Path p) {
		
		byte[] result = null;

		if (p.toString().toLowerCase().endsWith(".mp3")) {
			try {
				Bitstream bitstream = new Bitstream(Files.newInputStream(p));
				ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
				MP3Decoder decoder = new MP3Decoder();
				OutputBuffer outputBuffer = null;
				int sampleRate = -1, channels = -1;
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
				result = output.toByteArray();
				int bytes = result.length;
//				int samples = bytes / (2 * input.channels);
	//
				ShortBuffer buf = ByteBuffer.wrap(result, 0, bytes).asShortBuffer();
				short[] shortArray = new short[buf.limit()];
		        buf.get(shortArray);
				return shortArray;
			} catch (Throwable ex) {
			}
		}
		if (p.toString().toLowerCase().endsWith(".ogg")) {
			ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
			try (OggInputStream input = new OggInputStream(Files.newInputStream(p))){
				byte[] buffer = new byte[2048];
				while (!input.atEnd()) {
					int length = input.read(buffer);
					if (length == -1)
						break;
					output.write(buffer, 0, length);
				}
				result = output.toByteArray();
				int bytes = result.length;
//				int samples = bytes / (2 * input.channels);
	//
				ShortBuffer buf = ByteBuffer.wrap(result, 0, bytes).asShortBuffer();
				short[] shortArray = new short[buf.limit()];
		        buf.get(shortArray);
				return shortArray;
			} catch (Throwable ex) {

			}
		}
		if (p.toString().toLowerCase().endsWith(".wav")) {
			try (WavInputStream input = new WavInputStream(Files.newInputStream(p))) {
				result = StreamUtils.copyStreamToByteArray(input, input.dataRemaining);
				int bytes = result.length - (result.length % (input.channels > 1 ? 4 : 2));
//				int samples = bytes / (2 * input.channels);
	//
				ByteBuffer buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).put(result, 0, bytes);
				buffer.flip();

				ShortBuffer buf = buffer.asShortBuffer();
				System.out.println(bytes + " - " + buf.limit());
				short[] shortArray = new short[buf.limit()];
		        buf.get(shortArray);
				return shortArray;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		return null;
	}
	
	/** @author Nathan Sweet */
	static private class WavInputStream extends FilterInputStream {
		int channels, sampleRate, dataRemaining;

		WavInputStream (InputStream p) {
			super(p);
			try {
				if (read() != 'R' || read() != 'I' || read() != 'F' || read() != 'F')
					throw new RuntimeException("RIFF header not found: " + p.toString());

				skipFully(4);

				if (read() != 'W' || read() != 'A' || read() != 'V' || read() != 'E')
					throw new RuntimeException("Invalid wave file header: " + p.toString());

				int fmtChunkLength = seekToChunk('f', 'm', 't', ' ');

				int type = read() & 0xff | (read() & 0xff) << 8;
				if (type != 1) throw new RuntimeException("WAV files must be PCM: " + type);

				channels = read() & 0xff | (read() & 0xff) << 8;
				if (channels != 1 && channels != 2)
					throw new RuntimeException("WAV files must have 1 or 2 channels: " + channels);

				sampleRate = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;

				skipFully(6);

				int bitsPerSample = read() & 0xff | (read() & 0xff) << 8;
				if (bitsPerSample != 16) throw new RuntimeException("WAV files must have 16 bits per sample: " + bitsPerSample);

				skipFully(fmtChunkLength - 16);

				dataRemaining = seekToChunk('d', 'a', 't', 'a');
			} catch (Throwable ex) {
				StreamUtils.closeQuietly(this);
				throw new RuntimeException("Error reading WAV file: " + p.toString(), ex);
			}
		}

		private int seekToChunk (char c1, char c2, char c3, char c4) throws IOException {
			while (true) {
				boolean found = read() == c1;
				found &= read() == c2;
				found &= read() == c3;
				found &= read() == c4;
				int chunkLength = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;
				if (chunkLength == -1) throw new IOException("Chunk not found: " + c1 + c2 + c3 + c4);
				if (found) return chunkLength;
				skipFully(chunkLength);
			}
		}

		private void skipFully (int count) throws IOException {
			while (count > 0) {
				long skipped = in.skip(count);
				if (skipped <= 0) throw new EOFException("Unable to skip.");
				count -= skipped;
			}
		}

		public int read (byte[] buffer) throws IOException {
			if (dataRemaining == 0) return -1;
			int length = Math.min(super.read(buffer), dataRemaining);
			if (length == -1) return -1;
			dataRemaining -= length;
			return length;
		}
	}
}
