package bms.player.beatoraja.play.audio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import javax.sound.sampled.*;

import bms.model.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandleStream;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * キー音リソースの管理、及びキー音を鳴らすためのクラス
 * 
 * @author exch
 */
public class SoundProcessor implements AudioProcessor {

	private Sound[] wavmap = new Sound[0];

	private SliceWav[][] slicesound = new SliceWav[0][];

	private long[] playmap = new long[0];

	private float progress = 0;

	private float volume = 1.0f;

	/**
	 * BMSの音源データを読み込む
	 * 
	 * @param model
	 */
	public void setModel(BMSModel model) {
		dispose();
		progress = 0;
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		Map<Integer, byte[]> orgwavmap = new HashMap<Integer, byte[]>();
		Map<Integer, Sound> soundmap = new HashMap<Integer, Sound>();

		TimeLine[] timelines = model.getAllTimeLines();
		if(model.getVolwav() > 0 && model.getVolwav() < 100) {
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
						// BMSのケース(音切りなし)
						if (soundmap.get(note.getWav()) == null) {
							Sound sound = getSound(dpath.resolve(name).toString());
							soundmap.put(note.getWav(), sound);
						}

					} else {
						// BMSONのケース(音切りあり)
						boolean b = true;
						if(slicesound[note.getWav()] == null) {
							slicesound[note.getWav()] = new ArrayList<SliceWav>();
						}
						for (SliceWav slice : slicesound[note.getWav()]) {
							if (slice.starttime == note.getStarttime()
									&& slice.duration == note.getDuration()) {
								b = false;
								break;
							}
						}
						if (b) {
							name = name.substring(0, name.lastIndexOf('.'));
							final Path wavfile = dpath.resolve(name + ".wav");
							final Path oggfile = dpath.resolve(name + ".ogg");
							final Path mp3file = dpath.resolve(name + ".mp3");

							byte[] wav = null;
							if (orgwavmap.get(note.getWav()) != null) {
								wav = orgwavmap.get(note.getWav());
							}
							if (wav == null && Files.exists(wavfile)) {
								try {
									wav = AudioUtils.convertWav(wavfile);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (wav == null && Files.exists(oggfile)) {
								try {
									wav = AudioUtils.convertWav(oggfile);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (wav == null && Files.exists(mp3file)) {
								try {
									wav = AudioUtils.convertWav(mp3file);
									orgwavmap.put(note.getWav(), wav);
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							if (wav != null) {
								// スライシング、wavid振り直し
								ByteArrayInputStream bais = new ByteArrayInputStream(wav);
								try {
									final byte[] slicewav = AudioUtils.sliceWav(bais, note.getStarttime(), note.getDuration());
									Sound sound = Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
										@Override
										public InputStream read() {
											return new ByteArrayInputStream(slicewav);
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
								} catch (UnsupportedAudioFileException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								} catch (GdxRuntimeException e) {
									Logger.getGlobal().warning("音源(wav)ファイルスライシング失敗。" + e.getMessage());
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			progress += 1f / timelines.length;
		}

		Logger.getGlobal().info("音源ファイル読み込み完了。音源数:" + soundmap.keySet().size());
		wavmap = new Sound[wavcount];
		this.slicesound = new SliceWav[wavcount][];
		for (int i = 0; i < wavmap.length; i++) {
			wavmap[i] = soundmap.get(i);
			
			if(slicesound[i] != null) {
				this.slicesound[i] = slicesound[i].toArray(new SliceWav[slicesound[i].size()]);
			} else {
				this.slicesound[i] = new SliceWav[0];
			}
		}
		playmap = new long[wavmap.length];
		Arrays.fill(playmap, -1);
		
		
		progress = 1;
	}

	synchronized public void play(Note n, float volume) {
		try {
			final int id = n.getWav();
			if(id < 0) {
				return;
			}
			final int starttime = n.getStarttime();
			final int duration = n.getDuration();
			if (starttime == 0 && duration == 0) {
				final Sound sound = wavmap[id];
				final long pid = playmap[id];
				if (sound != null) {
					if (pid != -1) {
						sound.stop(pid);
					}
					playmap[id] = wavmap[id].play(this.volume * volume);
				}
			} else {
				for (SliceWav slice : slicesound[id]) {
					if (slice.starttime == starttime && slice.duration == duration) {
						if (slice.playid != -1) {
							slice.wav.stop(slice.playid);
						}
						slice.playid = slice.wav.play(this.volume * volume);
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
				final int id = n.getWav();
				if(id < 0) {
					return;
				}
				final int starttime = n.getStarttime();
				final int duration = n.getDuration();			
				if (starttime == 0 && duration == 0) {
					final Sound sound = wavmap[id];
					final long pid = playmap[id];
					if(sound != null && pid != -1) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (Sound id : wavmap) {
			if (id != null) {
				id.dispose();
			}
		}
		for (SliceWav[] slices : slicesound) {
			for (SliceWav slice : slices) {
				slice.wav.dispose();
			}
		}
		slicesound = new SliceWav[0][];
	}

	public float getProgress() {
		return progress;
	}

	public void forceFinish() {
		progress = 1;
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

	public static Sound getSound(String name) {
		final int index = name.lastIndexOf('.');
		if(index != -1) {
			name = name.substring(0, index);					
		}
		final Path wavfile = Paths.get(name + ".wav");
		final Path oggfile = Paths.get(name + ".ogg");
		final Path mp3file = Paths.get(name + ".mp3");

		Sound sound = null;
		try {
			if (Files.exists(wavfile)) {
				RandomAccessFile f;
				try {
					f = new RandomAccessFile(wavfile.toFile(), "r");
					byte[] header = new byte[44];
					f.read(header, 0, 44);
					f.close();
					if (header[20] == 85) {
						// WAVの中身がmp3の場合
						sound = Gdx.audio.newSound(new FileHandleStream("tempwav.mp3") {
							@Override
							public InputStream read() {
								try {
									BufferedInputStream input = new BufferedInputStream(Files.newInputStream(wavfile));
									input.skip(44);
									return input;
								} catch (IOException e) {
									e.printStackTrace();
								}
								return null;
							}

							@Override
							public OutputStream write(boolean overwrite) {
								return null;
							}
						});
					} else {
						sound = Gdx.audio.newSound(new FileHandleStream("tempwav.wav") {
							@Override
							public InputStream read() {
								try {
									return new ByteArrayInputStream(AudioUtils.convertWav(wavfile));
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
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} catch (GdxRuntimeException e) {
			Logger.getGlobal().warning("音源(wav)ファイル読み込み失敗。" + e.getMessage());
			e.printStackTrace();
		}
		if (sound == null && Files.exists(oggfile)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(oggfile.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(ogg)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}
		if (sound == null && Files.exists(mp3file)) {
			try {
				sound = Gdx.audio.newSound(Gdx.files.internal(mp3file.toString()));
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源(mp3)ファイル読み込み失敗。" + e.getMessage());
				// e.printStackTrace();
			}
		}

		return sound;
	}
}
