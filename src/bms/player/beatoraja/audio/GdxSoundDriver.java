package bms.player.beatoraja.audio;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Locale;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * libGDX Sound(OpenAL)サウンドドライバ
 *
 * @author exch
 */
public class GdxSoundDriver extends AbstractAudioDriver<Sound> {

	private SoundMixer mixer;

	private final boolean soundthread = false;

	private SoundInstance[] sounds = new SoundInstance[256];
	private int soundPos = 0;

	public GdxSoundDriver(Config config) {
		super(config.getSongResourceGen());
		channels = 2;
		for (int i = 0; i < sounds.length; i++) {
			sounds[i] = new SoundInstance();
		}

		if(soundthread) {
			mixer = new SoundMixer();
			mixer.start();
		}
	}

	@Override
	protected Sound getKeySound(Path p) {
		for(Path path : AudioDriver.getPaths(p.toString())) {
			final String filename = path.toString();
			final String name = filename.substring(0, filename.lastIndexOf('.'));
			final String ext = filename.substring(filename.lastIndexOf('.'));
			final Sound sound = getKeySound(name, ext);
			if(sound != null) {
				return sound;
			}
		}
		return null;		
	}

	private Sound getKeySound(String name, String ext) {
		switch (ext.toLowerCase(Locale.ROOT)) {
			case ".wav":
			case ".flac":
				return getKeySound(new PCMHandleStream(name + ext));
			case ".ogg":
			case ".mp3":
				return getKeySound(Gdx.files.internal(name + ext));
		}
		return null;

	}

	private Sound getKeySound(FileHandle handle) {
		try {
			return Gdx.audio.newSound(handle);
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("音源ファイル読み込み失敗" + e.getMessage());
		}
		return null;
	}

	@Override
	protected Sound getKeySound(final PCM pcm) {
		return Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
			@Override
			public InputStream read() {
				return new WavFileInputStream(pcm);
			}

			@Override
			public OutputStream write(boolean overwrite) {
				return null;
			}
		});
	}

	private Object lock = new Object();
	
	@Override
	protected void play(Sound pcm, int channel, float volume, float pitch) {
		if(soundthread) {
			mixer.put(pcm, channel, volume, getGlobalPitch() * pitch);
		} else {
			synchronized (lock) {
				sounds[soundPos].sound = pcm;
				sounds[soundPos].id = pcm.play(volume, getGlobalPitch() * pitch, 0);
				sounds[soundPos].channel = channel;
				soundPos = (soundPos + 1) % sounds.length;
			}
		}
	}

	@Override
	protected void play(AudioElement<Sound> id, float volume, boolean loop) {
		if(soundthread) {
			mixer.put(id.audio, volume, loop);
		} else {
			synchronized (lock) {
				if(loop) {
					id.id = id.audio.loop(volume);
				} else {
					id.id = id.audio.play(volume);
				}				
			}
		}
	}
	
	@Override
	protected void setVolume(AudioElement<Sound> id, float volume) {
		id.audio.setVolume(id.id, volume);
	}
	
	@Override
	protected void stop(Sound id) {
		if (soundthread) {
			mixer.stop(id, 0);
		} else {
			synchronized (lock) {
				id.stop();			
			}			
		}
	}

	@Override
	protected void stop(Sound id, int channel) {
		if (soundthread) {
			mixer.stop(id, channel);
		} else {
			for (int i = 0; i < sounds.length; i++) {
				if (sounds[i].sound == id && sounds[i].channel == channel) {
					synchronized (lock) {
						sounds[i].sound.stop(sounds[i].id);						
						sounds[i].sound = null;
					}
				}
			}
		}
	}

	@Override
	protected void disposeKeySound(Sound pcm) {
		pcm.dispose();
	}

	class SoundMixer extends Thread {

		private Sound[] sound = new Sound[256];
		private float[] volume = new float[256];
		private float[] pitch = new float[256];
		private int[] channels = new int[256];
		private long[] ids = new long[256];
		private boolean[] loops = new boolean[256];
		private int cpos;
		private int pos;

		public synchronized void put(Sound sound, int channel, float volume, float pitch) {
			this.sound[cpos] = sound;
			this.volume[cpos] = volume;
			this.pitch[cpos] = pitch;
			this.channels[cpos] = channel;
			this.loops[cpos] = false;
			cpos = (cpos + 1) % this.sound.length;
		}

		public synchronized void put(Sound sound, float volume, boolean loop) {
			this.sound[cpos] = sound;
			this.volume[cpos] = volume;
			this.pitch[cpos] = 0;
			this.channels[cpos] = 0;
			this.loops[cpos] = loop;
			cpos = (cpos + 1) % this.sound.length;
		}

		public synchronized void stop(Sound snd, int channel) {
			for (int i = 0; i < sound.length; i++) {
				if (sound[i] == snd && this.channels[i] == channel) {
					sound[i].stop(ids[i]);
					sound[i] = null;
				}
			}
		}

		public void run() {
			for(;;) {
				if(pos != cpos) {
					if(loops[pos]) {
						ids[pos] = sound[pos].loop(this.volume[pos], getGlobalPitch() * this.pitch[pos], 0);												
					} else {
						ids[pos] = sound[pos].play(this.volume[pos], getGlobalPitch() * this.pitch[pos], 0);						
					}
					pos = (pos + 1) % this.sound.length;
				} else {
					try {
						sleep(1);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	private static class SoundInstance {
		public Sound sound;
		public long id = -1;
		public int channel = -1;
	}
	
	private class PCMHandleStream extends FileHandleStream {
		
		private final Path p;
		
		public PCMHandleStream(Path p) {
			super("tempwav.wav");
			this.p = p;
		}
		
		public PCMHandleStream(String path) {
			this(Paths.get(path));
		}

		@Override
		public InputStream read() {
			try {
				return new WavFileInputStream(PCM.load(p,GdxSoundDriver.this));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public OutputStream write(boolean overwrite) {
			return null;
		}
	}
	
	static class WavFileInputStream extends InputStream {

		private int pos = 0;
		private int mark = 0;
		private final byte[] header;
		private final PCM pcm;

		public WavFileInputStream(PCM pcm) {
			header = new byte[44];

			final int sampleRate = pcm.sampleRate;
			final int channels = pcm.channels;
			this.pcm = pcm;
			final long totalDataLen = pcm.len * 2 + 36;
			final long bitrate = sampleRate * channels * 16;

			header[0] = 'R';
			header[1] = 'I';
			header[2] = 'F';
			header[3] = 'F';
			header[4] = (byte) (totalDataLen & 0xff);
			header[5] = (byte) ((totalDataLen >> 8) & 0xff);
			header[6] = (byte) ((totalDataLen >> 16) & 0xff);
			header[7] = (byte) ((totalDataLen >> 24) & 0xff);
			header[8] = 'W';
			header[9] = 'A';
			header[10] = 'V';
			header[11] = 'E';
			header[12] = 'f';
			header[13] = 'm';
			header[14] = 't';
			header[15] = ' ';
			header[16] = 16;
			header[17] = 0;
			header[18] = 0;
			header[19] = 0;
			header[20] = 1;
			header[21] = 0;
			header[22] = (byte) channels;
			header[23] = 0;
			header[24] = (byte) (sampleRate & 0xff);
			header[25] = (byte) ((sampleRate >> 8) & 0xff);
			header[26] = (byte) ((sampleRate >> 16) & 0xff);
			header[27] = (byte) ((sampleRate >> 24) & 0xff);
			header[28] = (byte) ((bitrate / 8) & 0xff);
			header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
			header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
			header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
			header[32] = (byte) ((channels * 16) / 8);
			header[33] = 0;
			header[34] = 16;
			header[35] = 0;
			header[36] = 'd';
			header[37] = 'a';
			header[38] = 't';
			header[39] = 'a';
			header[40] = (byte) ((pcm.len * 2) & 0xff);
			header[41] = (byte) (((pcm.len * 2) >> 8) & 0xff);
			header[42] = (byte) (((pcm.len * 2) >> 16) & 0xff);
			header[43] = (byte) (((pcm.len * 2) >> 24) & 0xff);
		}

		@Override
		public int available() {
			return 44 + pcm.len * 2 - pos;
		}

		@Override
		public synchronized void mark(int readlimit) {
			mark = pos;
		}

		@Override
		public synchronized void reset() {
			pos = mark;
		}

		@Override
		public long skip(long n) {
			if (n < 0) {
				return 0;
			}
			if (44 + pcm.len * 2 - pos < n) {
				pos = 44 + pcm.len * 2;
				return 44 + pcm.len * 2 - pos;
			}
			pos += n;
			return n;
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public int read() {
			int result = -1;
			if (pos < 44) {
				result = 0x00ff & header[pos];
				pos++;
			} else if (pos < 44 + pcm.len * 2) {
				if(pcm instanceof ShortPCM) {
					short s = ((short[])pcm.sample)[(pos - 44) / 2 + pcm.start];
					if (pos % 2 == 0) {
						result = (s & 0x00ff);
					} else {
						result = ((s & 0xff00) >>> 8);
					}
				} else if(pcm instanceof ShortDirectPCM) {
					result = ((ByteBuffer)pcm.sample).get(pos - 44 + pcm.start * 2) & 0xff;
				} else if(pcm instanceof FloatPCM) {
					short s = (short) (((float[])pcm.sample)[(pos - 44) / 2 + pcm.start] * Short.MAX_VALUE);					
					if (pos % 2 == 0) {
						result = (s & 0x00ff);
					} else {
						result = ((s & 0xff00) >>> 8);
					}
				} else if(pcm instanceof BytePCM) {
					result = pos % 2 != 0 ? (((byte[])pcm.sample)[(pos - 44) / 2 + pcm.start]) & 0x000000ff : 0;
				}
				pos++;
			}
			// System.out.println("read : " + pos + " data : " + result);
			return result;
		}
	}
}
