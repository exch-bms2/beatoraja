package bms.player.beatoraja.audio;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd;
import be.tarsos.dsp.WaveformSimilarityBasedOverlapAdd.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline time-stretch for PCM using TarsosDSP WSOLA.
 * Only used for Practice SPEED (PortAudio) to keep pitch while changing tempo.
 */
public final class TimeStretchProcessor {

	private TimeStretchProcessor() {}

	public static PCM<?> stretch(PCM<?> pcm, float rate) {
		if (pcm == null || rate <= 0f || rate == 1f) {
			return pcm;
		}
		final float[] samples = toFloatArray(pcm);
		if (samples.length == 0) {
			return pcm;
		}

		WaveformSimilarityBasedOverlapAdd wsola = new WaveformSimilarityBasedOverlapAdd(
				Parameters.musicDefaults(rate, pcm.sampleRate));
		AudioDispatcher dispatcher;
		try {
			dispatcher = AudioDispatcherFactory.fromFloatArray(
					samples, pcm.sampleRate, wsola.getInputBufferSize(), wsola.getOverlap());
		} catch (Exception e) {
			return pcm; // dispatcher生成不可時はフォールバック
		}
		dispatcher.addAudioProcessor(wsola);

		List<Float> out = new ArrayList<>((int) (samples.length / rate) + 1024);
		AudioProcessor collector = new AudioProcessor() {
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] buffer = audioEvent.getFloatBuffer();
				for (float v : buffer) {
					out.add(v);
				}
				return true;
			}

			@Override
			public void processingFinished() {
			}
		};
		dispatcher.addAudioProcessor(collector);
		dispatcher.run();

		float[] stretched = new float[out.size()];
		for (int i = 0; i < out.size(); i++) {
			stretched[i] = out.get(i);
		}

		return toPCM(pcm, stretched);
	}

	private static float[] toFloatArray(PCM<?> pcm) {
		if (pcm instanceof FloatPCM f) {
			float[] src = f.sample;
			float[] out = new float[f.len];
			System.arraycopy(src, f.start, out, 0, f.len);
			return out;
		} else if (pcm instanceof ShortPCM s) {
			short[] src = s.sample;
			float[] out = new float[s.len];
			for (int i = 0; i < s.len; i++) {
				out[i] = src[s.start + i] / (float) Short.MAX_VALUE;
			}
			return out;
		} else if (pcm instanceof ShortDirectPCM s) {
			java.nio.ByteBuffer src = (java.nio.ByteBuffer) s.sample;
			float[] out = new float[s.len];
			for (int i = 0; i < s.len; i++) {
				out[i] = src.getShort((s.start + i) * 2) / (float) Short.MAX_VALUE;
			}
			return out;
		} else if (pcm instanceof BytePCM b) {
			byte[] src = b.sample;
			float[] out = new float[b.len];
			for (int i = 0; i < b.len; i++) {
				out[i] = (src[b.start + i] - 128) / 128f;
			}
			return out;
		}
		return new float[0];
	}

	private static PCM<?> toPCM(PCM<?> src, float[] stretched) {
		// Keep channels/sampleRate; store as FloatPCM to avoid clipping.
		int channels = src.channels;
		int len = (stretched.length / channels) * channels;
		float[] data = new float[len];
		System.arraycopy(stretched, 0, data, 0, len);
		return new FloatPCM(channels, src.sampleRate, 0, len, data);
	}
}