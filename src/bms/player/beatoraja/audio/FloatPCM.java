package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

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
		final int bytes = loader.pcm.limit();
		final ByteBuffer pcm = loader.pcm;
		pcm.rewind();

		switch(loader.bitsPerSample) {
		case 8:
			sample = new float[bytes];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (pcm.get() - 128) / 128.0f;
			}
			break;
		case 16:
			// final long time = System.nanoTime();
			sample = new float[bytes / 2];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (float) (pcm.getShort()) / Short.MAX_VALUE;
			}
			break;
		case 24:
			sample = new float[bytes / 3];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (float) (((pcm.get(i * 3) & 0xff) << 8) | ((pcm.get(i * 3 + 1) & 0xff) << 16) | ((pcm.get(i * 3 + 2) & 0xff) << 24)) / Integer.MAX_VALUE;
			}
			break;
		case 32:
			sample = new float[bytes / 4];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = pcm.getFloat();
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
		int start = (Math.min((int)((long)this.start * sample / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len * sample / this.sampleRate), samples.length - start) / channels) * channels;
		return new FloatPCM(channels, sample, start, len, samples);
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
		int start = (Math.min((int)((long)this.start / rate / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len / rate / this.sampleRate), samples.length - start) / channels) * channels;
		return new FloatPCM(channels, sampleRate, start, len, samples);
	}
	
	private float[] getSample(int sample) {
		float[] samples = new float[(int) (((long) this.sample.length / channels) * sample / sampleRate) * channels];

		for (long i = 0; i < samples.length / channels; i++) {
			long position = i * sampleRate / sample;
			long mod = (i * sampleRate) % sample;
			for (int j = 0; j < channels; j++) {
				if (mod != 0 && (int) ((position + 1) * channels + j) < this.sample.length) {
					float sample1 = this.sample[(int) (position * channels + j)];
					float sample2 = this.sample[(int) ((position + 1) * channels + j)];
					samples[(int) (i * channels + j)] = (sample1 * (sample - mod) + sample2 * mod) / sample;
				} else {
					samples[(int) (i * channels + j)] = this.sample[(int) (position * channels + j)];
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
		return new FloatPCM(channels, sampleRate, this.start * channels / this.channels , this.len  * channels / this.channels, samples);
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
		return length > 0 ? new FloatPCM(channels, sampleRate, this.start + start, length, this.sample) : null;
	}
	
	public boolean validate() {
		if(sample.length == 0) {
			return false;
		}
		return true;
	}
}
