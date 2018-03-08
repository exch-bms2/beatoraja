package bms.player.beatoraja.audio;

import java.io.IOException;

/**
 * 32bit float PCM
 * 
 * @author exch
 */
public class FloatPCM extends PCM<float[]> {

	FloatPCM(int channels, int sampleRate, int start, int len, float[] sample) {
		super(channels, sampleRate, start, len, sample);
	}

	protected static FloatPCM loadPCM(PCMLoader loader) throws IOException {
		float[] sample = null;
		final int bytes = loader.bytes;
		final byte[] pcm = loader.pcm;

		switch(loader.bitsPerSample) {
		case 8:
			sample = new float[bytes];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (pcm[i] - 128) / 128.0f;
			}
			break;
		case 16:
			// final long time = System.nanoTime();
			sample = new float[bytes / 2];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (float) ((pcm[i * 2] & 0xff) | (pcm[i * 2 + 1] << 8)) / Short.MAX_VALUE;
			}

			// ShortBuffer shortbuf =
			// ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			// shortbuf.get(sample);
			// System.out.println(p.toString() + " : " + (System.nanoTime()
			// - time));
			break;
		case 24:
			sample = new float[bytes / 3];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (float) (((pcm[i * 3] & 0xff) << 8) | ((pcm[i * 3 + 1] & 0xff) << 16) | ((pcm[i * 3 + 2] & 0xff) << 24)) / Integer.MAX_VALUE;
			}
			break;
		case 32:
			int pos = 0;
			sample = new float[bytes / 4];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = Float.intBitsToFloat((pcm[pos] & 0xff) | ((pcm[pos + 1] & 0xff) << 8)
						| ((pcm[pos + 2] & 0xff) << 16) | ((pcm[pos + 3] & 0xff) << 24));
				pos += 4;
			}
			break;
		default:
			throw new IOException(loader.bitsPerSample + " bits per samples isn't supported");			
		}
		
		return new FloatPCM(loader.channels, loader.sampleRate, 0, sample.length, sample);
	}

	/**
	 * サンプリングレートを変更したPCMを返す
	 * 
	 * @param sample
	 *            サンプリングレート
	 * @return サンプリングレート変更後のPCM
	 */
	public FloatPCM changeSampleRate(int sample) {
		float[] samples = getSample(sample);
		return new FloatPCM(channels, sample, 0, samples.length, samples);
	}

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public FloatPCM changeFrequency(float rate) {
		float[] samples = getSample((int) (sampleRate / rate));
		return new FloatPCM(channels, sampleRate, 0, samples.length, samples);
	}
	
	private float[] getSample(int sample) {
		float[] samples = new float[(int) (((long) this.sample.length / channels) * sample / sampleRate) * channels];

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
	public FloatPCM changeChannels(int channels) {
		float[] samples = new float[this.sample.length * channels / this.channels];

		for (long i = 0; i < samples.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				samples[(int) (i * channels + j)] = this.sample[(int) (i * this.channels)];
			}
		}
		return new FloatPCM(channels, sampleRate, 0, samples.length, samples);
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
	public FloatPCM slice(long starttime, long duration) {
		if (duration == 0 || starttime + duration > ((long) this.sample.length) * 1000000 / (sampleRate * channels)) {
			duration = Math.max(((long) this.sample.length) * 1000000 / (sampleRate * channels) - starttime, 0);
		}

		final int start = (int) ((starttime * sampleRate / 1000000) * channels);
		int length = (int) ((duration * sampleRate / 1000000) * channels);
//		final int orglength = length;
		while(length > channels) {
			boolean zero = true;
			for(int i = 0;i < channels;i++){
				zero &= (this.sample[start + length - i - 1] == 0);
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
		return length > 0 ? new FloatPCM(channels, sampleRate, start, length, this.sample) : null;
	}
}
