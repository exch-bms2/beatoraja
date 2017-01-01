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
	/**
	 * 効果音マップ
	 */
	private Map<String, Sound> soundmap = new HashMap<String, Sound>();

	public GdxSoundDriver() {
		setWavmap(new Sound[0]);
	}

	public void play(String p, boolean loop) {
		Sound sound = soundmap.get(p);
		if (!soundmap.containsKey(p)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(p));
				soundmap.put(p, sound);
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源読み込み失敗。" + e.getMessage());
			}
		}

		if (sound != null) {
			if (loop) {
				sound.loop();
			} else {
				sound.play();
			}
		}
	}

	public void stop(String p) {
		Sound sound = soundmap.get(p);
		if (sound != null) {
			sound.stop();
		}
	}

	@Override
	protected void initKeySound(int count) {
		for (Sound id : getWavmap()) {
			if (id != null) {
				id.dispose();
			}
		}
		setWavmap(new Sound[count]);
		for (SliceWav[] slices : getSlicesound()) {
			for (SliceWav<Sound> slice : slices) {
				slice.wav.dispose();
			}
		}
		setSlicesound(new SliceWav[count][]);
	}

	@Override
	protected Sound getKeySound(Path p) {
		return getSound(p.toString());
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
	protected synchronized void play(int id, float volume) {
		if (getPlaymap()[id] != -1) {
			getWavmap()[id].stop(getPlaymap()[id]);
		}
		getPlaymap()[id] = getWavmap()[id].play(getVolume() * volume);
	}

	@Override
	protected synchronized void play(SliceWav<Sound> slice, float volume) {
		if (slice.playid != -1) {
			slice.wav.stop(slice.playid);
		}
		slice.playid = slice.wav.play(getVolume() * volume);
	}

	@Override
	protected void stop() {
		for (Sound s : getWavmap()) {
			if (s != null) {
				s.stop();
			}
		}
		for (SliceWav[] slices : getSlicesound()) {
			for (SliceWav<Sound> slice : slices) {
				slice.wav.stop();
			}
		}
	}

	@Override
	protected void stop(int id) {
		final Sound sound = getWavmap()[id];
		final long pid = getPlaymap()[id];
		if (sound != null && pid != -1) {
			sound.stop();
			getPlaymap()[id] = -1;
		}
	}

	@Override
	protected void stop(SliceWav<Sound> slice) {
		slice.wav.stop(slice.playid);
		slice.playid = -1;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for(Sound sound : soundmap.values()) {
			if(sound != null) {
				sound.dispose();
			}
		}
		soundmap.clear();
	}

	private Sound getSound(String name) {
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
}
