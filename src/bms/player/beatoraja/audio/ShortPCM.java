package bms.player.beatoraja.audio;

import java.io.IOException;

/**
 * 16bit short PCM
 * 
 * @author exch
 */
public class ShortPCM extends PCM<short[]> {

	ShortPCM(int channels, int sampleRate, int start, int len, short[] sample) {
		super(channels, sampleRate, start, len, sample);
	}

	protected static ShortPCM loadPCM(PCMLoader loader) throws IOException {
		short[] sample = null;
		final int bytes = loader.bytes;
		final byte[] pcm = loader.pcm;
		
		switch(loader.bitsPerSample) {
		case 8:
			sample = new short[bytes];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) ((((short) pcm[i]) - 128) * 256);
			}
			break;
		case 16:
			// final long time = System.nanoTime();
			sample = new short[bytes / 2];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) ((pcm[i * 2] & 0xff) | (pcm[i * 2 + 1] << 8));
			}

			// ShortBuffer shortbuf =
			// ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			// shortbuf.get(sample);
			// System.out.println(p.toString() + " : " + (System.nanoTime()
			// - time));
			break;
		case 24:
			sample = new short[bytes / 3];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) ((pcm[i * 3 + 1] & 0xff) | (pcm[i * 3 + 2] << 8));
			}
			break;
		case 32:
			int pos = 0;
			sample = new short[bytes / 4];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) (Float.intBitsToFloat((pcm[pos] & 0xff) | ((pcm[pos + 1] & 0xff) << 8)
						| ((pcm[pos + 2] & 0xff) << 16) | ((pcm[pos + 3] & 0xff) << 24)) * Short.MAX_VALUE);
				pos += 4;
			}
			break;
		default:
			throw new IOException(loader.bitsPerSample + " bits per samples isn't supported");			
		}
		
		return new ShortPCM(loader.channels, loader.sampleRate, 0, sample.length, sample);
		// System.out.println(p.toString() + " : " + (System.nanoTime() -
		// time));
	}

	/**
	 * サンプリングレートを変更したPCMを返す
	 * 
	 * @param sample
	 *            サンプリングレート
	 * @return サンプリングレート変更後のPCM
	 */
	public ShortPCM changeSampleRate(int sample) {
		short[] samples = getSample(sample);
		int start = (Math.min((int)((long)this.start * sample / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len * sample / this.sampleRate), samples.length - start) / channels) * channels;
		return new ShortPCM(channels, sample, start, len, samples);
	}

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public ShortPCM changeFrequency(float rate) {
		short[] samples = getSample((int) (sampleRate / rate));
		int start = (Math.min((int)((long)this.start / rate / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len / rate / this.sampleRate), samples.length - start) / channels) * channels;
		return new ShortPCM(channels, sampleRate, start, len, samples);
	}
	
	private short[] getSample(int sample) {
		short[] samples = new short[(int) (((long) this.sample.length / channels) * sample / sampleRate) * channels];

		for (long i = 0; i < samples.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				if ((i * sampleRate) % sample != 0
						&& (int) ((i * sampleRate / sample + 1) * channels + j) < this.sample.length) {
					samples[(int) (i * channels
							+ j)] = (short) (this.sample[(int) ((i * sampleRate / sample) * channels + j)] / 2
									+ this.sample[(int) ((i * sampleRate / sample + 1) * channels + j)] / 2);
				} else {
					samples[(int) (i * channels + j)] = this.sample[(int) ((i * sampleRate / sample) * channels
							+ j)];
				}
			}
		}
		
		return samples;
	}

	/**
	 * チャンネル数を変更したPCMを返す
	 * 
	 * @param channels
	 *            チャンネル数
	 * @return チャンネル数を変更したPCM
	 */
	public ShortPCM changeChannels(int channels) {
		short[] samples = new short[this.sample.length * channels / this.channels];

		for (long i = 0; i < samples.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				samples[(int) (i * channels + j)] = this.sample[(int) (i * this.channels)];
			}
		}
		return new ShortPCM(channels, sampleRate, this.start * channels / this.channels , this.len  * channels / this.channels, samples);
	}

	/**
	 * トリミングしたPCMを返す
	 * 
	 * @param starttime
	 *            開始時間(us)
	 * @param duration
	 *            再生時間(us)
	 * @return トリミングしたPCM
	 */
	public ShortPCM slice(long starttime, long duration) {
		if (duration == 0 || starttime + duration > ((long) this.len) * 1000000 / (sampleRate * channels)) {
			duration = Math.max(((long) this.len) * 1000000 / (sampleRate * channels) - starttime, 0);
		}

		final int start = (int) ((starttime * sampleRate / 1000000) * channels);
		int length = (int) ((duration * sampleRate / 1000000) * channels);
//		final int orglength = length;
		while(length > channels) {
			boolean zero = true;
			for(int i = 0;i < channels;i++){
				zero &= (this.sample[this.start + start + length - i - 1] == 0);
			}
			if(zero) {
				length -= channels;
			} else {
				break;
			}
		}
//		if(length != orglength) {
//			Logger.getGlobal().info("終端の無音データ除外 - " + (orglength - length) + " samples");
//		}
		return length > 0 ? new ShortPCM(channels, sampleRate, this.start + start, length, this.sample) : null;
	}
}
