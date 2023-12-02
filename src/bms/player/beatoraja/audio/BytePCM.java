package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BytePCM extends PCM<byte[]> {

	BytePCM(int channels, int sampleRate, int start, int len, byte[] sample) {
		super(channels, sampleRate, start, len, sample);
	}

	protected static BytePCM loadPCM(PCMLoader loader) throws IOException {
		byte[] sample = null;
		final int bytes = loader.pcm.limit();
		final ByteBuffer pcm = loader.pcm;
		pcm.rewind();
		
		switch(loader.bitsPerSample) {
		case 8:
			sample = pcm.array();
			break;
		case 16:
			sample = new byte[bytes / 2];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = pcm.get(i * 2 + 1);
			}
			break;
		case 24:
			sample = new byte[bytes / 3];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = pcm.get(i * 3 + 2);
			}
			break;
		case 32:
			sample = new byte[bytes / 4];
			for (int i = 0; i < sample.length; i++) {
				sample[i] = (byte) (pcm.getFloat() * Byte.MAX_VALUE);
			}
			break;
		default:
			throw new IOException(loader.bitsPerSample + " bits per samples isn't supported");			
		}
		
		return new BytePCM(loader.channels, loader.sampleRate, 0, sample.length, sample);
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
	public BytePCM changeSampleRate(int sample) {
		byte[] samples = getSample(sample);
		int start = (Math.min((int)((long)this.start * sample / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len * sample / this.sampleRate), samples.length - start) / channels) * channels;
		return new BytePCM(channels, sample, start, len, samples);
	}

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public BytePCM changeFrequency(float rate) {
		byte[] samples = getSample((int) (sampleRate / rate));
		int start = (Math.min((int)((long)this.start / rate / this.sampleRate), samples.length - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len / rate / this.sampleRate), samples.length - start) / channels) * channels;
		return new BytePCM(channels, sampleRate, start, len, samples);
	}
	
	private byte[] getSample(int sample) {
		byte[] samples = new byte[(int) (((long) this.sample.length / channels) * sample / sampleRate) * channels];

		for (long i = 0; i < samples.length / channels; i++) {
			long position = i * sampleRate / sample;
			long mod = (i * sampleRate) % sample;
			for (int j = 0; j < channels; j++) {
				if (mod  != 0 && (int) ((position + 1) * channels + j) < this.sample.length) {
					short sample1 = this.sample[(int) (position * channels + j)];
					short sample2 = this.sample[(int) ((position + 1) * channels + j)];
					samples[(int) (i * channels + j)] = (byte) (((long)sample1 * (sample - mod) + (long)sample2 * mod) / sample);
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
	public BytePCM changeChannels(int channels) {
		byte[] samples = new byte[this.sample.length * channels / this.channels];

		for (long i = 0; i < samples.length / channels; i++) {
			for (int j = 0; j < channels; j++) {
				samples[(int) (i * channels + j)] = this.sample[(int) (i * this.channels)];
			}
		}
		return new BytePCM(channels, sampleRate, this.start * channels / this.channels , this.len  * channels / this.channels, samples);
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
	public BytePCM slice(long starttime, long duration) {
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
		return length > 0 ? new BytePCM(channels, sampleRate, this.start + start, length, this.sample) : null;
	}
	
	public boolean validate() {
		if(sample.length == 0) {
			return false;
		}
		return true;
	}
}
