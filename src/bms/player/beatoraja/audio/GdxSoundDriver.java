package bms.player.beatoraja.audio;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * libGDX Sound(OpenAL)サウンドドライバ
 *
 * @author exch
 */
public class GdxSoundDriver extends AbstractAudioDriver<Sound> {

	private SoundMixer mixer;

	private final boolean soundthread = true;

	public GdxSoundDriver() {
		if(soundthread) {
			mixer = new SoundMixer();
			mixer.start();
		}
	}

	@Override
	protected Sound getKeySound(Path p) {
		String name = p.toString();
		final int index = name.lastIndexOf('.');
		if (index !=-1 ) {
			name = name.substring(0, index);
		}
		final Path wavfile = Paths.get(name + ".wav");

		if (Files.exists(wavfile)) {
			try {
				return Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
					@Override
					public InputStream read() {
						try {
							final PCM pcm = new PCM(wavfile);
							return pcm.getInputStream();
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}

					@Override
					public OutputStream write(boolean overwrite) {
						return null;
					}
				});
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(wav)ファイル読み込み失敗。" + e.getMessage());
//				e.printStackTrace();
			}
		}
		final Path oggfile = Paths.get(name + ".ogg");
		if (Files.exists(oggfile)) {
			try {
				return Gdx.audio.newSound(Gdx.files.internal(oggfile.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(ogg)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}
		final Path mp3file = Paths.get(name + ".mp3");
		if (Files.exists(mp3file)) {
			try {
				return Gdx.audio.newSound(Gdx.files.internal(mp3file.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(mp3)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected Sound getKeySound(final PCM pcm) {
		return Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
			@Override
			public InputStream read() {
				return pcm.getInputStream();
			}

			@Override
			public OutputStream write(boolean overwrite) {
				return null;
			}
		});
	}

	@Override
	protected synchronized void play(Sound id, float volume, boolean loop) {
		if(loop) {
			id.loop(getVolume() * volume);
		} else {
			if(soundthread) {
				mixer.put(id, getVolume() * volume);
			} else {
				id.play(getVolume() * volume);
			}
		}
	}

	@Override
	protected void stop(Sound id) {
		id.stop();
	}

	@Override
	protected void disposeKeySound(Sound pcm) {
		pcm.dispose();
	}

	class SoundMixer extends Thread {

		private Sound[] sound = new Sound[256];
		private float[] volume = new float[256];
		private int cpos;
		private int pos;

		public synchronized void put(Sound sound, float volume) {
			this.sound[cpos] = sound;
			this.volume[cpos] = volume;
			cpos = (cpos + 1) % this.sound.length;
		}

		public void run() {
			for(;;) {
				if(pos != cpos) {
					sound[pos].play(getVolume() * this.volume[pos]);
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
}
