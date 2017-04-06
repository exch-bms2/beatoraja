package bms.player.beatoraja.audio;

import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;

import javazoom.jl.decoder.*;

import com.badlogic.gdx.backends.lwjgl3.audio.OggInputStream;
import com.badlogic.gdx.utils.StreamUtils;

/**
 * PCM音源処理用クラス
 * 
 * @author exch
 */
public class PCM {

	/**
	 * チャンネル数
	 */
	private int channels;
	/**
	 * 音源のサンプリングレート(Hz)
	 */
	private int sampleRate;
	/**
	 * PCMのタイプ
	 */
	private int type;
	/**
	 * 
	 */
	private int bitsPerSample;
	/**
	 * PCMデータ
	 */
	private short[] sample;

	private PCM() {

	}

	public PCM(Path p) throws IOException {
		// final long time = System.nanoTime();
		byte[] pcm = null;
		if (p.toString().toLowerCase().endsWith(".mp3")) {
			try {
				pcm = decodeMP3(new BufferedInputStream(new BufferedInputStream(Files.newInputStream(p))));
			} catch (Throwable ex) {
			}
		}
		if (p.toString().toLowerCase().endsWith(".ogg")) {
			try (OggInputStream input = new OggInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
				ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
				byte[] buff = new byte[2048];
				while (!input.atEnd()) {
					int length = input.read(buff);
					if (length == -1)
						break;
					output.write(buff, 0, length);
				}

				channels = input.getChannels();
				sampleRate = input.getSampleRate();
				bitsPerSample = 16;

				pcm = output.toByteArray();
			} catch (Throwable ex) {

			}
		}
		if (p.toString().toLowerCase().endsWith(".wav")) {
			try (WavInputStream input = new WavInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
				if (type == 85) {
					try {
						pcm = decodeMP3(new ByteArrayInputStream(
								StreamUtils.copyStreamToByteArray(input, input.dataRemaining)));
					} catch (BitstreamException e) {
						e.printStackTrace();
					}
				} else {
					pcm = StreamUtils.copyStreamToByteArray(input, input.dataRemaining);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (pcm != null) {
			// System.out.println(p.getFileName().toString() +
			// " - PCM generated : " + bitsPerSample + "bit " + sampleRate
			// + "Hz " + channels + "channel");
			int bytes = pcm.length - (pcm.length % (channels > 1 ? bitsPerSample / 4 : bitsPerSample / 8));

			if (bitsPerSample == 8) {
				this.sample = new short[pcm.length];
				for (int i = 0; i < pcm.length; i++) {
					this.sample[i] = (short) ((((short) pcm[i]) - 128) * 256);
				}
			}
			if (bitsPerSample == 16) {
				this.sample = new short[pcm.length / 2];
				for (int i = 0; i < this.sample.length; i++) {
					this.sample[i] = (short) (pcm[i * 2] + (pcm[i * 2 + 1] << 8));
				}
			}
			if (bitsPerSample == 24) {
				this.sample = new short[pcm.length / 3];
				for (int i = 0; i < this.sample.length; i++) {
					this.sample[i] = (short) (pcm[i * 3 + 1] + (pcm[i * 3 + 2] << 8));
				}
			}
			if (bitsPerSample == 32) {
				int pos = 0;
				this.sample = new short[pcm.length / 4];
				for (int i = 0; i < this.sample.length; i++) {
					this.sample[i] = (short) (Float.intBitsToFloat((pcm[pos] & 0xff) | ((pcm[pos + 1] & 0xff) << 8)
							| ((pcm[pos + 2] & 0xff) << 16) | ((pcm[pos + 3] & 0xff) << 24)) * Short.MAX_VALUE);
					pos += 4;
				}
			}
		} else if (sample == null) {
			throw new IOException("can't convert to PCM");
		}
		// System.out.println(p.toString() + " : " + (System.nanoTime() -
		// time));
	}

	private byte[] decodeMP3(InputStream is) throws BitstreamException {
		Bitstream bitstream = new Bitstream(is);
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
				bitsPerSample = 16;
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
		return output.toByteArray();

	}

	public int getChannels() {
		return channels;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getType() {
		return type;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public short[] getSample() {
		return sample;
	}

	/**
	 * サンプリングレートを変更したPCMを返す
	 * 
	 * @param sample
	 *            サンプリングレート
	 * @return サンプリングレート変更後のPCM
	 */
	public PCM changeSampleRate(int sample) {
		PCM pcm = new PCM();
		pcm.channels = channels;
		pcm.bitsPerSample = bitsPerSample;
		pcm.sampleRate = sample;

		pcm.sample = new short[(int) (((long) this.sample.length / channels) * sample / sampleRate) * channels];

		for (long i = 0; i < pcm.sample.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				if ((i * sampleRate) % sample != 0
						&& (int) ((i * sampleRate / sample + 1) * channels + j) < this.sample.length) {
					pcm.sample[(int) (i * channels
							+ j)] = (short) (this.sample[(int) ((i * sampleRate / sample) * channels + j)] / 2
									+ this.sample[(int) ((i * sampleRate / sample + 1) * channels + j)] / 2);
				} else {
					pcm.sample[(int) (i * channels + j)] = this.sample[(int) ((i * sampleRate / sample) * channels
							+ j)];
				}
			}
		}

		return pcm;
	}

	public PCM changeFrequency(float rate) {
		PCM pcm = changeSampleRate((int) (sampleRate / rate));
		pcm.sampleRate = sampleRate;
		return pcm;
	}

	public PCM changeChannels(int channels) {
		PCM pcm = new PCM();
		pcm.channels = channels;
		pcm.bitsPerSample = bitsPerSample;
		pcm.sampleRate = sampleRate;

		pcm.sample = new short[this.sample.length * channels / this.channels];

		for (long i = 0; i < pcm.sample.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				pcm.sample[(int) (i * channels + j)] = this.sample[(int) (i * this.channels)];
			}
		}

		return pcm;
	}

	public PCM slice(long starttime, long duration) {
		PCM pcm = new PCM();
		pcm.channels = channels;
		pcm.bitsPerSample = bitsPerSample;
		pcm.sampleRate = sampleRate;

		if (duration == 0 || starttime + duration > ((long) this.sample.length) * 1000 / (sampleRate * channels)) {
			duration = Math.max(((long) this.sample.length) * 1000 / (sampleRate * channels) - starttime, 0);
		}

		pcm.sample = new short[(int) ((duration * sampleRate / 1000) * channels)];
		System.arraycopy(this.sample, (int) ((starttime * sampleRate / 1000) * channels), pcm.sample, 0,
				pcm.sample.length);
		return pcm;
	}

	public InputStream getInputStream() {
		return new WavFileInputStream(this);
	}

	/** @author Nathan Sweet */
	private class WavInputStream extends FilterInputStream {
		private int dataRemaining;

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

class WavFileInputStream extends InputStream {

	private int pos = 0;
	private int mark = 0;
	private final byte[] header;
	private final short[] sample;

	public WavFileInputStream(PCM pcm) {
		header = new byte[44];

		final int sampleRate = pcm.getSampleRate();
		final int channels = pcm.getChannels();
		sample = pcm.getSample();
		final long totalDataLen = sample.length * 2 + 36;
		final long bitrate = sampleRate * channels * 16;

		header[0] = 'R';
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (sampleRate & 0xff);
		header[25] = (byte) ((sampleRate >> 8) & 0xff);
		header[26] = (byte) ((sampleRate >> 16) & 0xff);
		header[27] = (byte) ((sampleRate >> 24) & 0xff);
		header[28] = (byte) ((bitrate / 8) & 0xff);
		header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
		header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
		header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
		header[32] = (byte) ((channels * 16) / 8);
		header[33] = 0;
		header[34] = 16;
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) ((sample.length * 2) & 0xff);
		header[41] = (byte) (((sample.length * 2) >> 8) & 0xff);
		header[42] = (byte) (((sample.length * 2) >> 16) & 0xff);
		header[43] = (byte) (((sample.length * 2) >> 24) & 0xff);
	}

	@Override
	public int available() {
		return 44 + sample.length * 2 - pos;
	}

	@Override
	public synchronized void mark(int readlimit) {
		mark = pos;
	}

	@Override
	public synchronized void reset() {
		pos = mark;
	}

	@Override
	public long skip(long n) {
		if (n < 0) {
			return 0;
		}
		if (44 + sample.length * 2 - pos < n) {
			pos = 44 + sample.length * 2;
			return 44 + sample.length * 2 - pos;
		}
		pos += n;
		return n;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public int read() {
		int result = -1;
		if (pos < 44) {
			result = 0x00ff & header[pos];
			pos++;
		} else if (pos < 44 + sample.length * 2) {
			short s = sample[(pos - 44) / 2];
			if (pos % 2 == 0) {
				result = (s & 0x00ff);
			} else {
				result = ((s & 0xff00) >>> 8);
			}
			pos++;
		}
		// System.out.println("read : " + pos + " data : " + result);
		return result;
	}
}
