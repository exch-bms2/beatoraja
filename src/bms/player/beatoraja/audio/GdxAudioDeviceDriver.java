package bms.player.beatoraja.audio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.song.SongResource;

/**
 * libGDX AudioDeviceドライバ
 *
 * @author exch
 */
public class GdxAudioDeviceDriver extends AbstractAudioDriver<PCM> implements Runnable {

	private final MixerInput[] inputs;
	private final short[] buffer;
	private final Thread mixer;
	private AudioDevice device;
	private long idcount;
	private volatile boolean stop;
	private AudioDiagnostics diagnostics;
	private final AtomicBoolean resourcesReleased = new AtomicBoolean();

	public GdxAudioDeviceDriver(Config config) {
		this(config, Gdx.audio, task -> new Thread(task, "GdxAudioDevice Mixer"));
	}

	GdxAudioDeviceDriver(Config config, Audio audio, ThreadFactory threads) {
		super(config.getSongResourceGen());
		AudioConfig audioConfig = config.getAudioConfig();
		setSampleRate(audioConfig.getSampleRate() > 0 ? audioConfig.getSampleRate() : 44100);
		channels = 2;

		if (audio == null) {
			throw new IllegalStateException("GdxAudioDeviceDriver requires libGDX audio to be enabled.");
		}
		device = audio.newAudioDevice(getSampleRate(), channels == 1);
		try {
			channels = device.isMono() ? 1 : 2;
			int framesPerBuffer = getFramesPerBuffer(audioConfig.getDeviceBufferSize());
			buffer = new short[framesPerBuffer * channels];
			inputs = new MixerInput[audioConfig.getDeviceSimultaneousSources()];
			for (int i = 0; i < inputs.length; i++) {
				inputs[i] = new MixerInput();
			}

			Logger.getGlobal().info("GdxAudioDevice opened"
					+ " : sampleRate=" + getSampleRate()
					+ ", channels=" + channels
					+ ", deviceClass=" + device.getClass().getName()
					+ ", framesPerBuffer=" + framesPerBuffer
					+ ", simultaneousSources=" + inputs.length
					+ ", latency=" + device.getLatency());
			diagnostics = AudioDiagnostics.createIfEnabled("GdxAudioDevice/" + device.getClass().getSimpleName(),
					audioConfig, getSampleRate(), channels, framesPerBuffer);
			if (diagnostics != null) {
				AudioDiagnostics.openALInfo();
				int latencyFrames = device.getLatency();
				Logger.getGlobal().info("Audio diagnostics AudioDevice : apiLatencyFrames=" + latencyFrames
						+ ", nominalQueueMs=" + AudioDiagnostics.latencyMillis(latencyFrames, getSampleRate())
						+ ", hardwareLatencyMeasured=false");
			}

			mixer = threads.newThread(this);
			mixer.setPriority(Thread.MAX_PRIORITY);
			if (diagnostics != null) diagnostics.start();
			mixer.start();
		} catch (RuntimeException | Error e) {
			stop = true;
			if (diagnostics != null) diagnostics.close();
			try { device.dispose(); }
			catch (Throwable cleanupFailure) { e.addSuppressed(cleanupFailure); }
			throw e;
		}
	}

	private static int getFramesPerBuffer(int configuredFrames) {
		return Math.max(configuredFrames, 1024);
	}

	@Override
	protected PCM getKeySound(Path p) {
		return PCM.load(p.toString(), this);
	}

	@Override
	protected PCM getKeySound(SongResource resource) {
		return PCM.load(resource, this);
	}

	@Override
	protected PCM getKeySound(PCM pcm) {
		return convertToDeviceFormat(pcm);
	}

	@Override
	protected void disposeKeySound(PCM pcm) {
	}

	@Override
	protected void play(PCM pcm, int channel, float volume, float pitch) {
		put(pcm, channel, volume, pitch, false);
	}

	@Override
	protected void play(AudioElement<PCM> id, float volume, boolean loop) {
		id.id = put(id.audio, -1, volume, 1.0f, loop);
	}

	@Override
	protected void setVolume(AudioElement<PCM> id, float volume) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.id == id.id) {
					input.volume = volume;
					break;
				}
			}
		}
	}

	private long put(PCM pcm, int channel, float volume, float pitch, boolean loop) {
		if (pcm == null) {
			return -1;
		}
		pcm = convertToDeviceFormat(pcm);
		synchronized (inputs) {
			if (stop) return -1;
			for (MixerInput input : inputs) {
				if (input.pos == -1) {
					input.pcm = pcm;
					input.volume = volume;
					input.pitch = pitch;
					input.loop = loop;
					input.id = idcount++;
					input.channel = channel;
					input.pos = 0;
					input.posf = 0;
					return input.id;
				}
			}
		}
		return -1;
	}

	@Override
	protected boolean isPlaying(PCM pcm) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == pcm) {
					return input.pos != -1;
				}
			}
		}
		return false;
	}

	@Override
	protected void stop(PCM pcm) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == pcm) {
					input.pos = -1;
				}
			}
		}
	}

	@Override
	protected void stop(PCM pcm, int channel) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == pcm && input.channel == channel) {
					input.pos = -1;
				}
			}
		}
	}

	@Override
	protected void setVolume(PCM pcm, int channel, float volume) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == pcm && input.channel == channel) {
					input.volume = volume;
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			while (!stop) {
				long started = diagnostics != null ? System.nanoTime() : 0;
				mix();
				if (stop) break;
				long writeStarted = diagnostics != null ? System.nanoTime() : 0;
				boolean returned = false;
				try {
					device.writeSamples(buffer, 0, buffer.length);
					returned = true;
				} catch (Throwable e) {
					if (!stop) Logger.getGlobal().log(java.util.logging.Level.WARNING,
							"GdxAudioDevice writeSamples failed; stopping mixer", e);
					stop = true;
				} finally {
					if (diagnostics != null) diagnostics.stream(started, writeStarted, System.nanoTime(), buffer.length / channels,
							returned ? AudioDiagnostics.RETURNED : AudioDiagnostics.FAILED, getGlobalPitch(), getTimeStretchRate());
				}
			}
		} catch (Throwable e) {
			if (!stop) Logger.getGlobal().log(java.util.logging.Level.WARNING, "GdxAudioDevice mixer failed", e);
		} finally {
			stop = true;
			releaseResources();
		}
	}

	private void mix() {
		final float gpitch = getGlobalPitch();
		synchronized (inputs) {
			for (int i = 0; i < buffer.length; i += channels) {
				float wav_l = 0;
				float wav_r = 0;
				for (MixerInput input : inputs) {
					if (input.pos != -1) {
						float sample_l = getSample(input.pcm, input.pos);
						float sample_r = input.pcm.channels > 1 ? getSample(input.pcm, input.pos + 1) : sample_l;
						wav_l += sample_l * input.volume;
						wav_r += sample_r * input.volume;

						input.posf += gpitch * input.pitch;
						int inc = (int) input.posf;
						if (inc > 0) {
							input.pos += input.pcm.channels * inc;
							input.posf -= (float) inc;
						}
						if (input.pos + input.pcm.channels - 1 >= input.pcm.len) {
							input.pos = input.loop ? 0 : -1;
						}
					}
				}
				if (channels == 1) {
					buffer[i] = toShort((wav_l + wav_r) * 0.5f);
				} else {
					buffer[i] = toShort(wav_l);
					buffer[i + 1] = toShort(wav_r);
				}
			}
		}
	}

	private PCM convertToDeviceFormat(PCM pcm) {
		if (pcm != null && pcm.channels != channels) {
			pcm = pcm.changeChannels(channels);
		}
		if (pcm != null && pcm.sampleRate != getSampleRate()) {
			pcm = pcm.changeSampleRate(getSampleRate());
		}
		return pcm;
	}

	private static float getSample(PCM pcm, int index) {
		if (pcm instanceof FloatPCM floatPCM) {
			return floatPCM.sample[floatPCM.start + index];
		}
		if (pcm instanceof ShortDirectPCM shortPCM) {
			ByteBuffer sample = shortPCM.sample;
			return sample.getShort((shortPCM.start + index) * 2) / (float) Short.MAX_VALUE;
		}
		if (pcm instanceof ShortPCM shortPCM) {
			return shortPCM.sample[shortPCM.start + index] / (float) Short.MAX_VALUE;
		}
		if (pcm instanceof BytePCM bytePCM) {
			return bytePCM.sample[bytePCM.start + index] / 128f;
		}
		return 0;
	}

	private static short toShort(float value) {
		if (value > 1f) {
			return Short.MAX_VALUE;
		}
		if (value < -1f) {
			return Short.MIN_VALUE;
		}
		return (short) (value * Short.MAX_VALUE);
	}

	@Override
	public void dispose() {
		stop = true;
		mixer.interrupt();
		try {
			if (mixer != Thread.currentThread()) mixer.join(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (!mixer.isAlive()) releaseResources();
		else Logger.getGlobal().warning("GdxAudioDevice mixer is still stopping; cleanup deferred until it exits");
	}

	private void releaseResources() {
		if (!resourcesReleased.compareAndSet(false, true)) return;
		// AudioDevice has no abort API. Never dispose it concurrently with writeSamples.
		try { device.dispose(); }
		catch (Throwable e) { Logger.getGlobal().log(java.util.logging.Level.WARNING, "GdxAudioDevice dispose failed", e); }
		synchronized (inputs) {
			for (MixerInput input : inputs) { input.pcm = null; input.pos = -1; }
		}
		try { super.dispose(); }
		finally {
			if (diagnostics != null) diagnostics.close();
		}
	}

	private static class MixerInput {
		public PCM pcm;
		public float volume;
		public float pitch;
		public int pos = -1;
		public float posf;
		public boolean loop;
		public long id;
		public int channel = -1;
	}
}
