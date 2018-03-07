package bms.player.beatoraja.audio;

import java.nio.file.*;

import com.portaudio.*;

import bms.player.beatoraja.Config;

/**
 * PortAudioドライバ
 * 
 * @author exch
 */
public class PortAudioDriver extends AbstractAudioDriver<PCM> implements Runnable {

	private static DeviceInfo[] devices;
	
	private BlockingStream stream;
	private int sampleRate;
	
	/**
	 * ミキサー入力
	 */
	private final MixerInput[] inputs;

	private long idcount;
	
	private boolean stop = false;
	
	private final float[] buffer;
	
	private final Thread mixer;

	public static DeviceInfo[] getDevices() {
		if(devices == null) {
			PortAudio.initialize();
			
			devices = new DeviceInfo[PortAudio.getDeviceCount()];
			for(int i = 0;i < devices.length;i++) {
				devices[i] = PortAudio.getDeviceInfo(i);
			}
		}
		return devices;
	}

	public PortAudioDriver(Config config) {

		DeviceInfo[] devices = getDevices();
		// Get the default device and setup the stream parameters.
		int deviceId = 0;
		for(int i = 0;i < devices.length;i++) {
			if(devices[i].name.equals(config.getAudioDriverName())) {
				deviceId = i;
				break;
			}
		}
		DeviceInfo deviceInfo = devices[ deviceId ];
		sampleRate = (int)deviceInfo.defaultSampleRate;
//		System.out.println( "  deviceId    = " + deviceId );
//		System.out.println( "  sampleRate  = " + sampleRate );
//		System.out.println( "  device name = " + deviceInfo.name );

		StreamParameters streamParameters = new StreamParameters();
		streamParameters.channelCount = 2;
		streamParameters.device = deviceId;
		int framesPerBuffer = config.getAudioDeviceBufferSize();
		streamParameters.suggestedLatency = ((double)framesPerBuffer) / sampleRate;
//		System.out.println( "  suggestedLatency = " + streamParameters.suggestedLatency );

		int flags = 0;
		
		// Open a stream for output.
		stream = PortAudio.openStream( null, streamParameters,
				(int) sampleRate, framesPerBuffer, flags );

		stream.start();

		mixer = new Thread(this);
		buffer = new float[framesPerBuffer * 2];
		inputs = new MixerInput[config.getAudioDeviceSimultaneousSources()];
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = new MixerInput();
		}
		mixer.start();
	}

	@Override
	protected PCM getKeySound(Path p) {
		PCM wav = PCM.load(p.toString());
		
		if (wav != null && wav.getSampleRate() != sampleRate) {
			wav = wav.changeSampleRate(sampleRate);
		}
		if (wav != null && wav.getChannels() != 2) {
			wav = wav.changeChannels(2);
		}

		return wav;
	}

	@Override
	protected PCM getKeySound(PCM pcm) {
		if (pcm.getSampleRate() != sampleRate) {
			pcm = pcm.changeSampleRate(sampleRate);
		}
		if (pcm.getChannels() != 2) {
			pcm = pcm.changeChannels(2);
		}
		return pcm;
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
		for (MixerInput input : inputs) {
			if (input.id == id.id) {
				input.volume = volume;
				break;
			}
		}
	}

	@Override
	protected void disposeKeySound(PCM pcm) {
	}

	private long put(PCM pcm, int channel, float volume, float pitch, boolean loop) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pos == -1) {
					input.pcm = pcm;
					input.sample = pcm.getSample();						
					input.volume = volume;
					input.pitch = pitch;
					input.loop = loop;
					input.id = idcount++;
					input.channel = channel;
					input.pos = 0;
					input.start = pcm.getStart();
					input.len = pcm.getLength();
					return input.id;
				}
			}
		}
		return -1;
	}

	@Override
	protected void stop(PCM id) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == id) {
					input.pos = -1;
				}
			}				
		}
	}

	@Override
	protected void stop(PCM id, int channel) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == id && input.channel == channel) {
					input.pos = -1;
				}
			}
		}
	}

	public void run() {
		while(!stop) {
			final float gpitch = getGlobalPitch();
			synchronized (inputs) {
				for (int i = 0; i < buffer.length; i+=2) {
					float wav_l = 0;
					float wav_r = 0;
					for (MixerInput input : inputs) {
						if (input.pos != -1) {
							wav_l += ((float) input.sample[input.pos + input.start]) * input.volume / Short.MAX_VALUE;
							wav_r += ((float) input.sample[input.pos+1 + input.start]) * input.volume / Short.MAX_VALUE;								
							input.posf += gpitch * input.pitch;
							int inc = (int)input.posf;
							if (inc > 0) {
								input.pos += 2 * inc;
								input.posf -= (float)inc;
							}
							if (input.pos >= input.len) {
								input.pos = input.loop ? 0 : -1;
							}
						}
					}
					buffer[i] = wav_l;
					buffer[i+1] = wav_r;
				}						
			}
			
			try {
				stream.write( buffer, buffer.length / 2);
			} catch(Throwable e) {
				e.printStackTrace();
			}
			
		}
	}		

	public void dispose() {
		super.dispose();
		if(stream != null) {
			stop = true;
			long l = System.currentTimeMillis();
			while(mixer.isAlive() && System.currentTimeMillis() - l < 1000);
			stream.stop();
			stream.close();
			
			stream = null;

			PortAudio.terminate();
//			System.out.println( "JPortAudio test complete." );			
		}
	}

	static class MixerInput {
		public PCM pcm;
		public short[] sample = new short[0];
		public int start;
		public int len;
		public float volume;
		public float pitch;
		public int pos = -1;
		public float posf = 0.0f;
		public boolean loop;
		public long id;
		public int channel = -1;
	}
}
