package bms.player.beatoraja.audio;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import com.synthbot.jasiohost.*;

import bms.model.*;
import bms.player.beatoraja.Config;
import bms.player.beatoraja.play.audio.PCM;

public class ASIODriver implements AudioDriver, AsioDriverListener {

	private Map<String, PCM> soundmap = new HashMap<String, PCM>();
	private Map<String, Integer> soundplaymap = new HashMap<String, Integer>();

	private PCM[] wavmap = new PCM[0];
	private int[] playmap = new int[0];
	/**
	 * 
	 */
	private SliceWav[][] slicesound = new SliceWav[0][];

	private AsioDriver asioDriver;
	private Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
	private int bufferSize;
	private double sampleRate;
	private float[][] outputbuffer;

	private float progress = 0;
	private float volume = 1.0f;

	private AudioMixer mixer;

	public ASIODriver(Config config) {
		List<String> drivers = AsioDriver.getDriverNames();
//		System.out.println(Arrays.toString(drivers.toArray()));
		asioDriver = AsioDriver.getDriver(drivers.get(0));
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

	public void setModel(BMSModel model) {
		progress = 0;
		Path dpath = Paths.get(model.getPath()).getParent();

		Map<Integer, PCM> soundmap = new HashMap<Integer, PCM>();

		TimeLine[] timelines = model.getAllTimeLines();
		if (model.getVolwav() > 0 && model.getVolwav() < 100) {
			volume = model.getVolwav() / 100f;
		}
		int wavcount = model.getWavList().length;

		List<SliceWav>[] slicesound = new List[wavcount];

		for (TimeLine tl : timelines) {
			if (progress == 1) {
				break;
			}
			List<Note> notes = new ArrayList<Note>();
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null) {
					notes.add(tl.getNote(i));
				}
				if (tl.getHiddenNote(i) != null) {
					notes.add(tl.getHiddenNote(i));
				}
			}
			notes.addAll(Arrays.asList(tl.getBackGroundNotes()));

			for (Note note : notes) {
				if (note.getWav() >= 0) {
					String name = model.getWavList()[note.getWav()];
					if (note.getStarttime() == 0 && note.getDuration() == 0) {
						// 音切りなし
						if (soundmap.get(note.getWav()) == null) {
							name = name.substring(0, name.lastIndexOf('.'));
							final Path wavfile = dpath.resolve(name + ".wav");
							final Path oggfile = dpath.resolve(name + ".ogg");
							final Path mp3file = dpath.resolve(name + ".mp3");

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
							soundmap.put(note.getWav(), wav);
						}
					} else {
						// 音切りあり
						boolean b = true;
						if (slicesound[note.getWav()] == null) {
							slicesound[note.getWav()] = new ArrayList<SliceWav>();
						}
						for (SliceWav slice : slicesound[note.getWav()]) {
							if (slice.starttime == note.getStarttime() && slice.duration == note.getDuration()) {
								b = false;
								break;
							}
						}
						if (b) {
							name = name.substring(0, name.lastIndexOf('.'));
							final Path wavfile = dpath.resolve(name + ".wav");
							final Path oggfile = dpath.resolve(name + ".ogg");
							final Path mp3file = dpath.resolve(name + ".mp3");

							PCM wav = null;
							if (soundmap.get(note.getWav()) != null) {
								wav = soundmap.get(note.getWav());
							}
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

							if (wav != null) {
								slicesound[note.getWav()]
										.add(new SliceWav(note, wav.slice(note.getStarttime(), note.getDuration())));
								soundmap.put(note.getWav(), wav);
							}
						}
					}
				}
			}
			progress += 1f / timelines.length;
		}

		Logger.getGlobal().info("髻ｳ貅舌ヵ繧｡繧､繝ｫ隱ｭ縺ｿ霎ｼ縺ｿ螳御ｺ�縲る浹貅先焚:" + soundmap.keySet().size());
		wavmap = new PCM[wavcount];
		this.slicesound = new SliceWav[wavcount][];
		for (int i = 0; i < wavmap.length; i++) {
			wavmap[i] = soundmap.get(i);
			if (wavmap[i] != null && wavmap[i].getSampleRate() != (int) asioDriver.getSampleRate()) {
				wavmap[i] = wavmap[i].changeSampleRate((int) asioDriver.getSampleRate());
			}
			if (wavmap[i] != null && wavmap[i].getChannels() != 2) {
				wavmap[i] = wavmap[i].changeChannels(2);
			}

			if (slicesound[i] != null) {
				this.slicesound[i] = slicesound[i].toArray(new SliceWav[slicesound[i].size()]);
				for (SliceWav slice : this.slicesound[i]) {
					if (slice.wav.getSampleRate() != (int) asioDriver.getSampleRate()) {
						slice.wav = slice.wav.changeSampleRate((int) asioDriver.getSampleRate());
					}
					if (slice.wav.getChannels() != 2) {
						slice.wav = slice.wav.changeChannels(2);
					}
				}
			} else {
				this.slicesound[i] = new SliceWav[0];
			}
		}
		playmap = new int[wavmap.length];
		Arrays.fill(playmap, -1);

		progress = 1;
	}

	@Override
	public void play(String path, boolean loop) {
		PCM sound = soundmap.get(path);
		if (!soundmap.containsKey(path)) {
			try {
				sound = new PCM(Paths.get(path));
				soundmap.put(path, sound);
			} catch (IOException e) {
				Logger.getGlobal().warning("音源読み込み失敗。" + e.getMessage());
			}
		}

		if (sound != null) {
			soundplaymap.put(path, mixer.put(sound, loop));
		}
	}

	public void stop(String p) {
		Integer sound = soundplaymap.get(p);
		if (sound != null) {
			mixer.stop(sound);
		}
	}

	public void play(Note n, float volume) {
		try {
			final int id = n.getWav();
			if (id < 0) {
				return;
			}
			final int starttime = n.getStarttime();
			final int duration = n.getDuration();
			if (starttime == 0 && duration == 0) {
				final PCM sound = wavmap[id];
				if (sound != null) {
					synchronized (this) {
						playmap[id] = mixer.put(sound, false);
					}
				}
			} else {
				for (SliceWav slice : slicesound[id]) {
					if (slice.starttime == starttime && slice.duration == duration) {
						synchronized (this) {
							slice.playid = mixer.put(slice.wav, false);
						}
						// System.out.println("slice WAV play - ID:" + id +
						// " start:" + starttime + " duration:" + duration);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(Note n) {
		try {
			if (n == null) {
				for (int id : playmap) {
					mixer.stop(id);
				}
				for (SliceWav[] slices : slicesound) {
					for (SliceWav slice : slices) {
						mixer.stop(slice.playid);
					}
				}

			} else {
				final int id = n.getWav();
				if (id < 0) {
					return;
				}
				final int starttime = n.getStarttime();
				final int duration = n.getDuration();
				if (starttime == 0 && duration == 0) {
					mixer.stop(playmap[id]);
				} else {
					for (SliceWav slice : slicesound[id]) {
						if (slice.starttime == starttime && slice.duration == duration) {
							mixer.stop(slice.playid);
							break;
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		if (asioDriver != null) {
			asioDriver.shutdownAndUnloadDriver();
			activeChannels.clear();
			asioDriver = null;
		}
	}

	public float getProgress() {
		return progress;
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

	class SliceWav {
		public final int starttime;
		public final int duration;
		private PCM wav;

		public int playid = -1;

		public SliceWav(Note note, PCM wav) {
			this.starttime = note.getStarttime();
			this.duration = note.getDuration();
			this.wav = wav;
		}
	}

	class AudioMixer {

		private MixerInput[] inputs;

		public AudioMixer(int channels) {
			inputs = new MixerInput[channels];
			for (int i = 0; i < inputs.length; i++) {
				inputs[i] = new MixerInput();
			}
		}

		public int put(PCM pcm, boolean loop) {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i].pos == -1) {
					inputs[i].sample = pcm.getSample();
					inputs[i].pos = 0;
					inputs[i].loop = loop;
					return i;
				}
			}
			return -1;
		}

		public void stop(int id) {
			if (id >= 0 && id < inputs.length) {
				inputs[(int) id].pos = -1;
			}
		}

		public void fillBuffer(float[][] buffer) {
			final int size = buffer[0].length;
			final int channel = buffer.length;

			for (int i = 0; i < size; i++) {
				for (int j = 0; j < channel; j++) {
					float wav = 0;
					for (int k = 0; k < inputs.length; k++) {
						if (inputs[k].pos != -1) {
							wav += ((float) inputs[k].sample[inputs[k].pos]) / Short.MAX_VALUE;
							inputs[k].pos++;
							if (inputs[k].pos == inputs[k].sample.length) {
								inputs[k].pos = inputs[k].loop ? 0 : -1;
							}
						}
					}
					buffer[j][i] = wav;
				}
			}
		}
	}

	class MixerInput {
		public short[] sample = new short[0];
		public int pos = -1;
		public boolean loop;
	}
}
