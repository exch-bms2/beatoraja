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

public class GdxSoundDriver implements AudioDriver {
	/**
	 * 効果音マップ
	 */
	private Map<String, Sound> soundmap = new HashMap<String, Sound>();
	/**
	 * キー音マップ(音切りなし)
	 */
	private Sound[] wavmap = new Sound[0];
	/**
	 * キー音マップ(音切りなし):再生状況
	 */
	private long[] playmap = new long[0];
	/**
	 * キー音マップ(音切りあり)
	 */
	private SliceWav[][] slicesound = new SliceWav[0][0];
	/**
	 * キー音読み込み進捗状況
	 */
	private float progress = 0;
	/**
	 * キー音ボリューム
	 */
	private float volume = 1.0f;

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

	/**
	 * BMSの音源データを読み込む
	 * 
	 * @param model
	 */
	public void setModel(BMSModel model) {
		final int wavcount = model.getWavList().length;
		for (Sound id : wavmap) {
			if (id != null) {
				id.dispose();
			}
		}
		wavmap = new Sound[wavcount];
		playmap = new long[wavmap.length];
		Arrays.fill(playmap, -1);

		for (SliceWav[] slices : slicesound) {
			for (SliceWav slice : slices) {
				slice.wav.dispose();
			}
		}
		slicesound = new SliceWav[wavcount][];
		
		progress = 0;
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		if (model.getVolwav() > 0 && model.getVolwav() < 100) {
			volume = model.getVolwav() / 100f;
		}

		final Map<Integer, PCM> orgwavmap = new HashMap<Integer, PCM>();
		List<SliceWav>[] slicesound = new List[wavcount];

		List<Note> notes = new ArrayList<Note>();
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null) {
					notes.add(tl.getNote(i));
					notes.addAll(tl.getNote(i).getLayeredNotes());
				}
				if (tl.getHiddenNote(i) != null) {
					notes.add(tl.getHiddenNote(i));
				}
			}
			notes.addAll(Arrays.asList(tl.getBackGroundNotes()));
		}

		for (Note note : notes) {
			if (note.getWav() < 0) {
				continue;
			}
			String name = model.getWavList()[note.getWav()];
			if (note.getStarttime() == 0 && note.getDuration() == 0) {
				// 音切りなしのケース
				if (note.getWav() >= 0 && wavmap[note.getWav()]  == null) {
					wavmap[note.getWav()] = getSound(dpath.resolve(name).toString());
				}

			} else {
				// 音切りありのケース
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
					// byte[] wav = null;
					PCM wav = null;
					if (orgwavmap.get(note.getWav()) != null) {
						wav = orgwavmap.get(note.getWav());
					} else {
						name = name.substring(0, name.lastIndexOf('.'));
						final Path wavfile = dpath.resolve(name + ".wav");
						final Path oggfile = dpath.resolve(name + ".ogg");
						final Path mp3file = dpath.resolve(name + ".mp3");
						if (wav == null && Files.exists(wavfile)) {
							try {
								wav = new PCM(wavfile);
								orgwavmap.put(note.getWav(), wav);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
						if (wav == null && Files.exists(oggfile)) {
							try {
								wav = new PCM(oggfile);
								orgwavmap.put(note.getWav(), wav);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
						if (wav == null && Files.exists(mp3file)) {
							try {
								wav = new PCM(mp3file);
								orgwavmap.put(note.getWav(), wav);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					}

					if (wav != null) {
						try {
							final PCM slicewav = wav.slice(note.getStarttime(), note.getDuration());
							Sound sound = Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
								@Override
								public InputStream read() {
									return slicewav.getInputStream();
								}

								@Override
								public OutputStream write(boolean overwrite) {
									return null;
								}
							});
							slicesound[note.getWav()].add(new SliceWav(note, sound));
							// System.out.println("WAV slicing - Name:"
							// + name + " ID:" + note.getWav() +
							// " start:" + note.getStarttime() +
							// " duration:" + note.getDuration());
						} catch (Throwable e) {
							Logger.getGlobal().warning("音源(wav)ファイルスライシング失敗。" + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			progress += 1f / notes.size();
		}

		Logger.getGlobal().info("音源ファイル読み込み完了。音源数:" + wavmap.length);
		for (int i = 0; i < wavmap.length; i++) {
			if (slicesound[i] != null) {
				this.slicesound[i] = slicesound[i].toArray(new SliceWav[slicesound[i].size()]);
			} else {
				this.slicesound[i] = new SliceWav[0];
			}
		}

		progress = 1;
	}

	public void play(Note n, float volume) {
		play0(n, volume);
		for(Note ln : n.getLayeredNotes()) {
			play0(ln, volume);			
		}
	}
	
	private final void play0(Note n, float volume) {
		try {
			final int id = n.getWav();
			if (id < 0) {
				return;
			}
			final int starttime = n.getStarttime();
			final int duration = n.getDuration();
			if (starttime == 0 && duration == 0) {
				final Sound sound = wavmap[id];
				if (sound != null) {
					synchronized (this) {
						if (playmap[id] != -1) {
							sound.stop(playmap[id]);
						}
						playmap[id] = sound.play(this.volume * volume);
					}
				}
			} else {
				for (SliceWav slice : slicesound[id]) {
					if (slice.starttime == starttime && slice.duration == duration) {
						synchronized (this) {
							if (slice.playid != -1) {
								slice.wav.stop(slice.playid);
							}
							slice.playid = slice.wav.play(this.volume * volume);
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
				for (Sound s : wavmap) {
					if (s != null) {
						s.stop();
					}
				}
				for (SliceWav[] slices : slicesound) {
					for (SliceWav slice : slices) {
						slice.wav.stop();
					}
				}

			} else {
				stop0(n);
				for(Note ln : n.getLayeredNotes()) {
					stop0(ln);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private final void stop0(Note n) {
		final int id = n.getWav();
		if (id < 0) {
			return;
		}
		final int starttime = n.getStarttime();
		final int duration = n.getDuration();
		if (starttime == 0 && duration == 0) {
			final Sound sound = wavmap[id];
			final long pid = playmap[id];
			if (sound != null && pid != -1) {
				sound.stop();
				playmap[id] = -1;
			}
		} else {
			for (SliceWav slice : slicesound[id]) {
				if (slice.starttime == starttime && slice.duration == duration) {
					slice.wav.stop(slice.playid);
					slice.playid = -1;
					break;
				}
			}
		}
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

	public float getProgress() {
		return progress;
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

class SliceWav {
	public final int starttime;
	public final int duration;
	public final Sound wav;

	public long playid = -1;

	public SliceWav(Note note, Sound wav) {
		this.starttime = note.getStarttime();
		this.duration = note.getDuration();
		this.wav = wav;
	}
}