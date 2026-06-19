package bms.player.beatoraja.audio;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.logging.Logger;

import com.portaudio.*;
import bms.player.beatoraja.AudioConfig;
import bms.player.beatoraja.Config;

/**
 * PortAudioドライバ
 * 
 * @author exch
 */
public class PortAudioDriver extends AbstractAudioDriver<PCM> implements Runnable {

	private static DeviceInfo[] devices;
	
	private BlockingStream stream;

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
			Logger.getGlobal().info("PortAudio initialized : " + PortAudio.getVersionText());

			devices = new DeviceInfo[PortAudio.getDeviceCount()];
			for(int i = 0;i < devices.length;i++) {
				devices[i] = PortAudio.getDeviceInfo(i);
			}
			logDevices(devices);
		}
		return devices;
	}

	public static String getUnavailableMessage(Throwable e) {
		String message = e.getMessage() != null ? e.getMessage() : e.toString();
		if(e instanceof UnsatisfiedLinkError || message.contains("jportaudio")) {
			return message + "。macOSでPortAudioを使用するには natives/libjportaudio.dylib を配置し、"
					+ "-Djava.library.path=./natives を指定してください。";
		}
		return message;
	}

	public PortAudioDriver(Config config) {
		super(config.getSongResourceGen());
		DeviceInfo[] devices = getDevices();
		AudioConfig audioConfig = config.getAudioConfig();
		int deviceId = getDeviceId(audioConfig, devices);
		DeviceInfo deviceInfo = devices[ deviceId ];
		logSelectedDevice(deviceId, deviceInfo);

		setSampleRate(audioConfig.getSampleRate() <= 0 ? (int)deviceInfo.defaultSampleRate : audioConfig.getSampleRate());
		channels = 2;
//		System.out.println( "  deviceId    = " + deviceId );
//		System.out.println( "  sampleRate  = " + sampleRate );
//		System.out.println( "  device name = " + deviceInfo.name );

		StreamParameters streamParameters = new StreamParameters();
		streamParameters.channelCount = channels;
		streamParameters.device = deviceId;
		streamParameters.sampleFormat = PortAudio.FORMAT_FLOAT_32;
		int framesPerBuffer = audioConfig.getDeviceBufferSize();
		streamParameters.suggestedLatency = getSuggestedLatency(deviceInfo, framesPerBuffer);
//		System.out.println( "  suggestedLatency = " + streamParameters.suggestedLatency );

		int flags = PortAudio.FLAG_CLIP_OFF | PortAudio.FLAG_DITHER_OFF;

		// Open a stream for output.
		stream = openStream(streamParameters, deviceInfo, framesPerBuffer, flags);

		stream.start();

		mixer = new Thread(this, "PortAudio Mixer");
		mixer.setPriority(Thread.MAX_PRIORITY);
		buffer = new float[framesPerBuffer * channels];
		inputs = new MixerInput[audioConfig.getDeviceSimultaneousSources()];
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = new MixerInput();
		}
		mixer.start();
	}

	private static int getDeviceId(AudioConfig config, DeviceInfo[] devices) {
		String driverName = config.getDriverName();
		if(driverName != null && !driverName.isEmpty()) {
			for(int i = 0;i < devices.length;i++) {
				if(driverName.equals(devices[i].name) && devices[i].maxOutputChannels > 0) {
					return i;
				}
			}
		}

		if(isMacOS()) {
			int coreAudioDefault = getCoreAudioDefaultOutputDevice(devices);
			if(coreAudioDefault >= 0) {
				return coreAudioDefault;
			}
		}

		int defaultOutput = PortAudio.getDefaultOutputDevice();
		if(defaultOutput >= 0 && defaultOutput < devices.length && devices[defaultOutput].maxOutputChannels > 0) {
			return defaultOutput;
		}
		return getFirstOutputDevice(devices);
	}

	private BlockingStream openStream(StreamParameters streamParameters, DeviceInfo deviceInfo, int framesPerBuffer, int flags) {
		logStreamOpen(streamParameters, framesPerBuffer, flags, "primary");
		try {
			logFormatSupport(streamParameters, framesPerBuffer);
			return PortAudio.openStream( null, streamParameters, getSampleRate(), framesPerBuffer, flags );
		} catch(RuntimeException e) {
			Logger.getGlobal().warning("PortAudio primary openStream failed : " + e.getMessage());
		}

		StreamParameters highLatencyParameters = copyParameters(streamParameters);
		highLatencyParameters.suggestedLatency = deviceInfo.defaultHighOutputLatency > 0
				? deviceInfo.defaultHighOutputLatency
				: Math.max(streamParameters.suggestedLatency, 0.05);
		logStreamOpen(highLatencyParameters, framesPerBuffer, flags, "high-latency fallback");
		try {
			logFormatSupport(highLatencyParameters, framesPerBuffer);
			return PortAudio.openStream(null, highLatencyParameters, getSampleRate(), framesPerBuffer, flags);
		} catch(RuntimeException e) {
			Logger.getGlobal().warning("PortAudio high-latency fallback openStream failed : " + e.getMessage());
		}

		StreamParameters unspecifiedBufferParameters = copyParameters(highLatencyParameters);
		int unspecifiedFramesPerBuffer = 0;
		logStreamOpen(unspecifiedBufferParameters, unspecifiedFramesPerBuffer, flags, "unspecified-buffer fallback");
		logFormatSupport(unspecifiedBufferParameters, unspecifiedFramesPerBuffer);
		return PortAudio.openStream(null, unspecifiedBufferParameters, getSampleRate(), unspecifiedFramesPerBuffer, flags);
	}

	private double getSuggestedLatency(DeviceInfo deviceInfo, int framesPerBuffer) {
		double bufferLatency = ((double)framesPerBuffer) / getSampleRate();
		if(isCoreAudioLowLatency(deviceInfo) && deviceInfo.defaultLowOutputLatency > 0) {
			return Math.min(bufferLatency, deviceInfo.defaultLowOutputLatency);
		}
		return bufferLatency;
	}

	private static StreamParameters copyParameters(StreamParameters source) {
		StreamParameters copy = new StreamParameters();
		copy.device = source.device;
		copy.channelCount = source.channelCount;
		copy.sampleFormat = source.sampleFormat;
		copy.suggestedLatency = source.suggestedLatency;
		return copy;
	}

	private void logFormatSupport(StreamParameters streamParameters, int framesPerBuffer) {
		try {
			int result = PortAudio.isFormatSupported(null, streamParameters, getSampleRate());
			Logger.getGlobal().info("PortAudio format support"
					+ " : result=" + result
					+ ", device=" + streamParameters.device
					+ ", channels=" + streamParameters.channelCount
					+ ", sampleRate=" + getSampleRate()
					+ ", framesPerBuffer=" + framesPerBuffer
					+ ", suggestedLatency=" + streamParameters.suggestedLatency);
		} catch(Throwable e) {
			Logger.getGlobal().warning("PortAudio format support check failed : " + e.getMessage());
		}
	}

	private void logStreamOpen(StreamParameters streamParameters, int framesPerBuffer, int flags, String label) {
		Logger.getGlobal().info("PortAudio openStream " + label
				+ " : device=" + streamParameters.device
				+ ", channels=" + streamParameters.channelCount
				+ ", sampleFormat=" + streamParameters.sampleFormat
				+ ", sampleRate=" + getSampleRate()
				+ ", framesPerBuffer=" + framesPerBuffer
				+ ", suggestedLatency=" + streamParameters.suggestedLatency
				+ ", flags=" + flags);
	}

	private static int getCoreAudioDefaultOutputDevice(DeviceInfo[] devices) {
		try {
			int coreAudioIndex = PortAudio.hostApiTypeIdToHostApiIndex(PortAudio.HOST_API_TYPE_COREAUDIO);
			if(coreAudioIndex >= 0) {
				HostApiInfo coreAudio = PortAudio.getHostApiInfo(coreAudioIndex);
				if(coreAudio.defaultOutputDevice >= 0 && coreAudio.defaultOutputDevice < devices.length
						&& devices[coreAudio.defaultOutputDevice].maxOutputChannels > 0) {
					return coreAudio.defaultOutputDevice;
				}
			}
		} catch(Throwable e) {
			Logger.getGlobal().fine("Core Audio default output device is unavailable : " + e.getMessage());
		}

		for(int i = 0;i < devices.length;i++) {
			if(devices[i].maxOutputChannels > 0 && isCoreAudioDevice(devices[i])) {
				return i;
			}
		}
		return -1;
	}

	private static int getFirstOutputDevice(DeviceInfo[] devices) {
		for(int i = 0;i < devices.length;i++) {
			if(devices[i].maxOutputChannels > 0) {
				return i;
			}
		}
		throw new RuntimeException("PortAudio output device is not found");
	}

	private static boolean isCoreAudioDevice(DeviceInfo deviceInfo) {
		try {
			return PortAudio.getHostApiInfo(deviceInfo.hostApi).type == PortAudio.HOST_API_TYPE_COREAUDIO;
		} catch(Throwable e) {
			return false;
		}
	}

	private static boolean isCoreAudioLowLatency(DeviceInfo deviceInfo) {
		return isMacOS() && isCoreAudioDevice(deviceInfo);
	}

	private static boolean isMacOS() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac");
	}

	private static void logSelectedDevice(int deviceId, DeviceInfo deviceInfo) {
		String hostApiName = "unknown";
		int hostApiType = -1;
		try {
			HostApiInfo hostApi = PortAudio.getHostApiInfo(deviceInfo.hostApi);
			hostApiName = hostApi.name;
			hostApiType = hostApi.type;
		} catch(Throwable e) {
			Logger.getGlobal().fine("PortAudio host API information is unavailable : " + e.getMessage());
		}

		boolean coreAudio = hostApiType == PortAudio.HOST_API_TYPE_COREAUDIO;
		Logger.getGlobal().info("PortAudio output device selected"
				+ " : id=" + deviceId
				+ ", name=" + deviceInfo.name
				+ ", hostApi=" + hostApiName
				+ ", coreAudio=" + coreAudio
				+ ", lowLatencyMode=" + isCoreAudioLowLatency(deviceInfo)
				+ ", defaultLowOutputLatency=" + deviceInfo.defaultLowOutputLatency
				+ ", defaultHighOutputLatency=" + deviceInfo.defaultHighOutputLatency
				+ ", defaultSampleRate=" + deviceInfo.defaultSampleRate);
	}

	private static void logDevices(DeviceInfo[] devices) {
		for(int i = 0;i < devices.length;i++) {
			DeviceInfo device = devices[i];
			String hostApiName = "unknown";
			int hostApiType = -1;
			try {
				HostApiInfo hostApi = PortAudio.getHostApiInfo(device.hostApi);
				hostApiName = hostApi.name;
				hostApiType = hostApi.type;
			} catch(Throwable e) {
				Logger.getGlobal().fine("PortAudio host API information is unavailable : " + e.getMessage());
			}
			Logger.getGlobal().info("PortAudio device"
					+ " : id=" + i
					+ ", name=" + device.name
					+ ", hostApi=" + hostApiName
					+ ", coreAudio=" + (hostApiType == PortAudio.HOST_API_TYPE_COREAUDIO)
					+ ", maxInputChannels=" + device.maxInputChannels
					+ ", maxOutputChannels=" + device.maxOutputChannels
					+ ", defaultLowOutputLatency=" + device.defaultLowOutputLatency
					+ ", defaultHighOutputLatency=" + device.defaultHighOutputLatency
					+ ", defaultSampleRate=" + device.defaultSampleRate);
		}
	}

	@Override
	protected PCM getKeySound(Path p) {
		return PCM.load(p.toString(), this);
	}

	@Override
	protected PCM getKeySound(PCM pcm) {
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
	protected boolean isPlaying(PCM id) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == id) {
					return input.pos != -1;
				}
			}				
		}
		return false;
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

	@Override
	protected void setVolume(PCM id, int channel, float volume) {
		synchronized (inputs) {
			for (MixerInput input : inputs) {
				if (input.pcm == id && input.channel == channel) {
					input.volume = volume;
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
							if(input.pcm instanceof FloatPCM floatPCM) {
								final float[] sample = floatPCM.sample;
								wav_l += sample[input.pos + floatPCM.start] * input.volume;
								wav_r += sample[input.pos+1 + floatPCM.start] * input.volume;																
							} else if(input.pcm instanceof ShortDirectPCM shortPCM) {
								final ByteBuffer sample = shortPCM.sample;
								wav_l += ((float) sample.getShort((input.pos + shortPCM.start) * 2)) * input.volume / Short.MAX_VALUE;
								wav_r += ((float) sample.getShort((input.pos+1 + shortPCM.start) * 2)) * input.volume / Short.MAX_VALUE;																
							} else if(input.pcm instanceof ShortPCM shortPCM) {
								final short[] sample = shortPCM.sample;
								wav_l += ((float) sample[input.pos + shortPCM.start]) * input.volume / Short.MAX_VALUE;
								wav_r += ((float) sample[input.pos+1 + shortPCM.start]) * input.volume / Short.MAX_VALUE;																
							} else if(input.pcm instanceof BytePCM bytePCM) {
								final byte[] sample = bytePCM.sample;
								wav_l += ((float) sample[input.pos + bytePCM.start]) * input.volume / 128f;
								wav_r += ((float) sample[input.pos+1 + bytePCM.start]) * input.volume / 128f;
							}
							input.posf += gpitch * input.pitch;
							int inc = (int)input.posf;
							if (inc > 0) {
								input.pos += 2 * inc;
								input.posf -= (float)inc;
							}
							if (input.pos + 1 >= input.pcm.len) {
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
		public float volume;
		public float pitch;
		public int pos = -1;
		public float posf = 0.0f;
		public boolean loop;
		public long id;
		public int channel = -1;
	}
}
