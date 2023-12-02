package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 16bit short PCM (using diect buffer)
 * 
 * @author exch
 */
public class ShortDirectPCM extends PCM<ByteBuffer> {

	ShortDirectPCM(int channels, int sampleRate, int start, int len, ByteBuffer sample) {
		super(channels, sampleRate, start, len, sample);
	}

	protected static ShortDirectPCM loadPCM(PCMLoader loader) throws IOException {
		ByteBuffer sample = null;
		final int bytes = loader.pcm.limit();
		final ByteBuffer pcm = loader.pcm;
		pcm.rewind();
		
		switch(loader.bitsPerSample) {
		case 8:
			sample = getDirectByteBuffer(bytes * 2);
			for (int i = 0; i < bytes; i++) {
				sample.putShort((short) ((((short) pcm.get()) - 128) * 256));
			}
			break;
		case 16:
			// final long time = System.nanoTime();
			if(pcm.isDirect()) {
				sample = pcm;
			} else {
				sample = getDirectByteBuffer(bytes).put(pcm.array(), 0, bytes);				
			}
			break;
		case 24:
			sample = getDirectByteBuffer(bytes * 2 / 3);
			for (int i = 0, len = bytes / 3; i < len; i++) {
				sample.putShort(pcm.getShort(i * 3 + 1));
			}
			break;
		case 32:
			sample = getDirectByteBuffer(bytes / 2);
			for (int i = 0, len = bytes / 4; i < len; i++) {
				sample.putShort((short) (pcm.getFloat() * Short.MAX_VALUE));
			}
			break;
		default:
			throw new IOException(loader.bitsPerSample + " bits per samples isn't supported");			
		}
		
		return new ShortDirectPCM(loader.channels, loader.sampleRate, 0, bytes * 8 / (loader.bitsPerSample), sample);
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
	public ShortDirectPCM changeSampleRate(int sample) {
		ByteBuffer samples = getSample(sample);
		int start = (Math.min((int)((long)this.start * sample / this.sampleRate), samples.limit() / 2 - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len * sample / this.sampleRate), samples.limit() / 2 - start) / channels) * channels;
		return new ShortDirectPCM(channels, sample, start, len, samples);
	}

	/**
	 * 再生速度を変更したPCMを返す
	 * 
	 * @param rate
	 *            再生速度。基準は1.0
	 * @return 再生速度を変更したPCM
	 */
	public ShortDirectPCM changeFrequency(float rate) {
		ByteBuffer samples = getSample((int) (sampleRate / rate));
		int start = (Math.min((int)((long)this.start / rate / this.sampleRate), samples.limit()  / 2 - 1) / channels) * channels;
		int len = (Math.min((int)((long)this.len / rate / this.sampleRate), samples.limit() / 2 - start) / channels) * channels;
		return new ShortDirectPCM(channels, sampleRate, start, len, samples);
	}
	
	private ByteBuffer getSample(int sample) {
		ByteBuffer samples = getDirectByteBuffer((int) (((long) this.sample.limit() / 2 / channels) * sample / sampleRate) * channels * 2);

		for (long i = 0; i < samples.limit() / 2 / channels; i++) {
			long position = i * sampleRate / sample;
			long mod = (i * sampleRate) % sample;
			for (int j = 0; j < channels; j++) {
				if (mod != 0 && (int) ((position + 1) * channels + j) < this.sample.limit() / 2) {
					short sample1 = this.sample.getShort((int) (position * channels + j) * 2);
					short sample2 = this.sample.getShort((int) ((position + 1) * channels + j) * 2);
					samples.putShort((int) (i * channels + j) * 2,  (short)(((long)sample1 * (sample - mod) + (long)sample2 * mod) / sample));
				} else {
					samples.putShort((int) (i * channels + j) * 2, this.sample.getShort((int) (position * channels+ j) * 2));
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
	public ShortDirectPCM changeChannels(int channels) {
		ByteBuffer samples = getDirectByteBuffer((this.sample.limit() / 2) * channels / this.channels * 2);

		for (long i = 0; i < samples.limit() / 2 / channels; i++) {
			for (int j = 0; j < channels; j++) {
				samples.putShort((int) (i * channels + j) * 2, this.sample.getShort((int) (i * this.channels) * 2));
			}
		}
		return new ShortDirectPCM(channels, sampleRate, this.start * channels / this.channels , this.len  * channels / this.channels, samples);
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
	public ShortDirectPCM slice(long starttime, long duration) {
		if (duration == 0 || starttime + duration > ((long) this.len) * 1000000 / (sampleRate * channels)) {
			duration = Math.max(((long) this.len) * 1000000 / (sampleRate * channels) - starttime, 0);
		}

		final int start = (int) ((starttime * sampleRate / 1000000) * channels);
		int length = (int) ((duration * sampleRate / 1000000) * channels);
//		final int orglength = length;
		while(length > channels) {
			boolean zero = true;
			for(int i = 0;i < channels;i++){
				zero &= (this.sample.getShort((this.start + start + length - i - 1) * 2) == 0);
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
		return length > 0 ? new ShortDirectPCM(channels, sampleRate, this.start + start, length, this.sample) : null;
	}
	
	public boolean validate() {
		if(sample.limit() == 0) {
			return false;
		}
		return true;
	}
}
