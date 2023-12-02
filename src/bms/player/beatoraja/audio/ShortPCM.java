package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

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
		final int bytes = loader.pcm.limit();
		final ByteBuffer pcm = loader.pcm;
		pcm.rewind();
		
		switch(loader.bitsPerSample) {
		case 8:
			sample = new short[bytes];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) ((((short) pcm.get()) - 128) * 256);
			}
			break;
		case 16:
			sample = new short[bytes / 2];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = pcm.getShort();
			}
			break;
		case 24:
			sample = new short[bytes / 3];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = pcm.getShort(i * 3 + 1);
			}
			break;
		case 32:
			sample = new short[bytes / 4];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (short) (pcm.getFloat() * Short.MAX_VALUE);
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
			long position = i * sampleRate / sample;
			long mod = (i * sampleRate) % sample;
			for (int j = 0; j < channels; j++) {
				if (mod  != 0 && (int) ((position + 1) * channels + j) < this.sample.length) {
					short sample1 = this.sample[(int) (position * channels + j)];
					short sample2 = this.sample[(int) ((position + 1) * channels + j)];
					samples[(int) (i * channels + j)] = (short) (((long)sample1 * (sample - mod) + (long)sample2 * mod) / sample);
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
	
	public boolean validate() {
		if(sample.length == 0) {
			return false;
		}
		return true;
	}
}
