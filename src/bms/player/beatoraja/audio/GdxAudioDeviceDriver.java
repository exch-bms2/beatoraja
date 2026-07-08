package bms.player.beatoraja.audio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;

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
	private boolean stop;

	public GdxAudioDeviceDriver(Config config) {
		super(config.getSongResourceGen());
		AudioConfig audioConfig = config.getAudioConfig();
		setSampleRate(audioConfig.getSampleRate() > 0 ? audioConfig.getSampleRate() : 44100);
		channels = 2;

		if (Gdx.audio == null) {
			throw new IllegalStateException("GdxAudioDeviceDriver requires libGDX audio to be enabled.");
		}
		device = Gdx.audio.newAudioDevice(getSampleRate(), channels == 1);
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

		mixer = new Thread(this, "GdxAudioDevice Mixer");
		mixer.setPriority(Thread.MAX_PRIORITY);
		mixer.start();
	}

	private static int getFramesPerBuffer(int configuredFrames) {
		return Math.max(configuredFrames, 1024);
	}

	@Override
	protected PCM getKeySound(Path p) {
		return PCM.load(p.toString(), this);
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
		while (!stop) {
			mix();
			try {
				device.writeSamples(buffer, 0, buffer.length);
			} catch (Throwable e) {
				Logger.getGlobal().warning("GdxAudioDevice writeSamples failed : " + e.getMessage());
			}
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
		super.dispose();
		stop = true;
		long start = System.currentTimeMillis();
		while (mixer.isAlive() && System.currentTimeMillis() - start < 1000) {
			Thread.yield();
		}
		if (device != null) {
			device.dispose();
			device = null;
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
