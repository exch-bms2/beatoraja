package bms.player.beatoraja.audio;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import bms.model.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GdxSoundDriver extends AbstractAudioDriver<Sound> {

	public GdxSoundDriver() {
		setWavmap(new Sound[0]);
	}

	@Override
	protected Sound getKeySound(Path p) {
		String name = p.toString();
		final int index = name.lastIndexOf('.');
		if (index != -1) {
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
	protected Sound getKeySound(PCM pcm) {
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
	protected synchronized long play(Sound id, float volume, boolean loop) {
		if(loop) {
			return id.loop(getVolume() * volume);			
		}
		return id.play(getVolume() * volume);
	}

	@Override
	protected synchronized void play(SliceWav<Sound> slice, float volume) {
		if (slice.playid != -1) {
			slice.wav.stop(slice.playid);
		}
		slice.playid = slice.wav.play(getVolume() * volume);
	}

	@Override
	protected void stop(Sound id) {
		id.stop();
	}

	@Override
	protected void stop(SliceWav<Sound> slice) {
		slice.wav.stop(slice.playid);
		slice.playid = -1;
	}
	
	@Override
	protected void disposeKeySound(Sound pcm) {
		pcm.dispose();
	}
}
