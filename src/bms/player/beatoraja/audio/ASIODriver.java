package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;
import com.synthbot.jasiohost.AsioDriverListener;
import com.synthbot.jasiohost.AsioDriverState;

import bms.player.beatoraja.Config;

/**
 * ASIOサウンドドライバ
 *
 * @author exch
 */
public class ASIODriver extends AbstractAudioDriver<PCM> implements AsioDriverListener {

	/**
	 * ASIOドライバー
	 */
	private AsioDriver asioDriver;
	private Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
	private int bufferSize;
	private double sampleRate;
	private float[][] outputbuffer;

	/**
	 * オーディオミキサー
	 */
	private AudioMixer mixer;

	public ASIODriver(Config config) {
		// System.out.println(Arrays.toString(drivers.toArray()));
		asioDriver = AsioDriver.getDriver(config.getAudioDriverName());
		asioDriver.addAsioDriverListener(this);
		activeChannels.add(asioDriver.getChannelOutput(0));
		activeChannels.add(asioDriver.getChannelOutput(1));
		bufferSize = asioDriver.getBufferPreferredSize();
		sampleRate = asioDriver.getSampleRate();
		outputbuffer = new float[2][bufferSize];
		asioDriver.createBuffers(activeChannels);
		asioDriver.start();

		mixer = new AudioMixer(config.getAudioDeviceSimultaneousSources());
	}

	@Override
	protected PCM getKeySound(Path p) {
		String name = p.toString();
		name = name.substring(0, name.lastIndexOf('.'));
		final Path wavfile = Paths.get(name + ".wav");
		final Path oggfile = Paths.get(name + ".ogg");
		final Path mp3file = Paths.get(name + ".mp3");

		PCM wav = null;
		if (wav == null && Files.exists(wavfile)) {
			try {
				wav = new PCM(wavfile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (wav == null && Files.exists(oggfile)) {
			try {
				wav = new PCM(oggfile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (wav == null && Files.exists(mp3file)) {
			try {
				wav = new PCM(mp3file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (wav != null && wav.getSampleRate() != (int) asioDriver.getSampleRate()) {
			wav = wav.changeSampleRate((int) asioDriver.getSampleRate());
		}
		if (wav != null && wav.getChannels() != 2) {
			wav = wav.changeChannels(2);
		}

		return wav;
	}

	@Override
	protected PCM getKeySound(PCM pcm) {
		if (pcm.getSampleRate() != (int) asioDriver.getSampleRate()) {
			pcm = pcm.changeSampleRate((int) asioDriver.getSampleRate());
		}
		if (pcm.getChannels() != 2) {
			pcm = pcm.changeChannels(2);
		}
		return pcm;
	}

	@Override
	protected synchronized void play(PCM id, float volume) {
		mixer.put(id, volume, false);
	}

	@Override
	protected void play(AudioElement<PCM> id, float volume, boolean loop) {
		id.id = mixer.put(id.audio, volume, loop);
	}

	@Override
	protected void setVolume(AudioElement<PCM> id, float volume) {
		mixer.setVolume(id.id, volume);
	}

	@Override
	protected void stop(PCM id) {
		mixer.stop(id);
	}

	@Override
	protected void disposeKeySound(PCM pcm) {
	}

	public void dispose() {
		super.dispose();
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
			activeChannels.clear();
			asioDriver = null;
		}
	}

	public void resetRequest() {
		/*
		 * This thread will attempt to shut down the ASIO driver. However, it
		 * will block on the AsioDriver object at least until the current method
		 * has returned.
		 */
		new Thread() {
			@Override
			public void run() {
				System.out.println("resetRequest() callback received. Returning driver to INITIALIZED state.");
				asioDriver.returnToState(AsioDriverState.INITIALIZED);
			}
		}.start();
	}

	public void resyncRequest() {
		System.out.println("resyncRequest() callback received.");
	}

	public void sampleRateDidChange(double sampleRate) {
		System.out.println("sampleRateDidChange() callback received.");
	}

	public void bufferSizeChanged(int bufferSize) {
		System.out.println("bufferSizeChanged() callback received.");
	}

	public void latenciesChanged(int inputLatency, int outputLatency) {
		System.out.println("latenciesChanged() callback received.");
	}

	public void bufferSwitch(long systemTime, long samplePosition, Set<AsioChannel> channels) {
		mixer.fillBuffer(outputbuffer);
		for (AsioChannel channelInfo : channels) {
			channelInfo.write(outputbuffer[channelInfo.getChannelIndex()]);
		}
	}

	/**
	 * オーディオミキサー
	 *
	 * @author exch
	 */
	class AudioMixer {

		/**
		 * ミキサー入力
		 */
		private MixerInput[] inputs;

		private long idcount;

		public AudioMixer(int channels) {
			inputs = new MixerInput[channels];
			for (int i = 0; i < inputs.length; i++) {
				inputs[i] = new MixerInput();
			}
		}

		public long put(PCM pcm, float volume, boolean loop) {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i].pos == -1) {
					inputs[i].pcm = pcm;
					inputs[i].sample = pcm.getSample();
					inputs[i].volume = volume;
					inputs[i].pos = 0;
					inputs[i].loop = loop;
					inputs[i].id = idcount++;
					return inputs[i].id;
				}
			}
			return -1;
		}

		public void setVolume(long id, float volume) {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i].id == id) {
					inputs[i].volume = volume;
					break;
				}
			}
		}

		public void stop(PCM id) {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i].pcm == id) {
					inputs[i].pos = -1;
				}
			}
		}

		public void fillBuffer(float[][] buffer) {
			final int size = buffer[0].length;
			final int channel = buffer.length;

			for (int i = 0; i < size; i++) {
				for (int j = 0; j < channel; j++) {
					float wav = 0;
					for (MixerInput input : inputs) {
						if (input.pos != -1) {
							wav += ((float) input.sample[input.pos]) * input.volume / Short.MAX_VALUE;
							input.pos++;
							if (input.pos == input.sample.length) {
								input.pos = input.loop ? 0 : -1;
							}
						}
					}
					buffer[j][i] = wav;
				}
			}
		}
	}

	class MixerInput {
		public PCM pcm;
		public short[] sample = new short[0];
		public float volume;
		public int pos = -1;
		public boolean loop;
		public long id;
	}
}
