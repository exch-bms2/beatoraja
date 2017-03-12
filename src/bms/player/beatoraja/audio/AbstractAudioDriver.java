package bms.player.beatoraja.audio;

import bms.model.*;

import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * 抽象オーディオドライバー
 * 
 * @author exch
 *
 * @param <T>
 *            音源データ
 */
public abstract class AbstractAudioDriver<T> implements AudioDriver {

	/**
	 * 効果音マップ
	 */
	private Map<String, T> soundmap = new HashMap<String, T>();
	/**
	 * キー音マップ(音切りなし)
	 */
	private Object[] wavmap = new Object[0];
	/**
	 * キー音マップ(音切りあり)
	 */
	private SliceWav<T>[][] slicesound = new SliceWav[0][0];
	/**
	 * キー音読み込み進捗状況
	 */
	private float progress = 0;
	/**
	 * キー音ボリューム
	 */
	private float volume = 1.0f;

	/**
	 * パスで指定された効果音ファイルの音源データを取得する
	 * 
	 * @param p
	 *            音源データのパス
	 * @return 音源データ
	 */
	protected abstract T getKeySound(Path p);

	/**
	 * PCMオブジェクトで指定されたキー音の音源データを取得する
	 * 
	 * @param pcm
	 * @return
	 */
	protected abstract T getKeySound(PCM pcm);

	/**
	 * 音源データを開放する
	 * 
	 * @param pcm
	 *            開放する音源データ
	 */
	protected abstract void disposeKeySound(T pcm);

	/**
	 * 音源データを再生する
	 * 
	 * @param id
	 *            音源データ
	 * @param volume
	 *            ボリューム(0.0-1.0)
	 * @param loop
	 *            ループ再生するかどうか
	 */
	protected abstract void play(T id, float volume, boolean loop);

	/**
	 * 音源データが再生されていれば停止する
	 * 
	 * @param id
	 *            音源データ
	 */
	protected abstract void stop(T id);

	protected float getVolume() {
		return volume;
	}

	public void play(String p, boolean loop) {
		T sound = soundmap.get(p);
		if (!soundmap.containsKey(p)) {
			try {
				sound = getKeySound(Paths.get(p));
				soundmap.put(p, sound);
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源読み込み失敗。" + e.getMessage());
			}
		}

		if (sound != null) {
			play(sound, 1.0f, loop);
		}
	}

	public void stop(String p) {
		T sound = soundmap.get(p);
		if (sound != null) {
			stop(sound);
		}
	}

	public void dispose(String p) {
		T sound = soundmap.get(p);
		if (sound != null) {
			soundmap.remove(p);
			disposeKeySound(sound);			
		}
	}
	
	/**
	 * BMSの音源データを読み込む
	 *
	 * @param model
	 */
	public synchronized void setModel(BMSModel model) {
		final int wavcount = model.getWavList().length;
		for (Object id : wavmap) {
			if (id != null) {
				disposeKeySound((T) id);
			}
		}
		wavmap = new Object[wavcount];
		for (SliceWav[] slices : slicesound) {
			for (SliceWav<T> slice : slices) {
				disposeKeySound(slice.wav);
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
			if (progress >= 1) {
				break;
			}
			if (note.getWav() < 0) {
				continue;
			}
			String name = model.getWavList()[note.getWav()];
			if (note.getStarttime() == 0 && note.getDuration() == 0) {
				// 音切りなしのケース
				if (note.getWav() >= 0 && wavmap[note.getWav()] == null) {
					wavmap[note.getWav()] = getKeySound(dpath.resolve(name));
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
							T sound = getKeySound(slicewav);
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

	public void abort() {
		progress = 1;
	}

	public void play(Note n, float volume) {
		play0(n, volume);
		for (Note ln : n.getLayeredNotes()) {
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
				final T wav = (T) wavmap[id];
				if (wav != null) {
					stop(wav);
					play(wav, volume, false);
				}
			} else {
				for (SliceWav slice : slicesound[id]) {
					if (slice.starttime == starttime && slice.duration == duration) {
						play((T) slice.wav, volume, false);
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
				for (Object s : wavmap) {
					if (s != null) {
						stop((T) s);
					}
				}
				for (SliceWav[] slices : slicesound) {
					for (SliceWav<T> slice : slices) {
						stop(slice.wav);
					}
				}
			} else {
				stop0(n);
				for (Note ln : n.getLayeredNotes()) {
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
			final T sound = (T) wavmap[id];
			if (sound != null) {
				stop(sound);
			}
		} else {
			for (SliceWav slice : slicesound[id]) {
				if (slice.starttime == starttime && slice.duration == duration) {
					stop((T) slice.wav);
					break;
				}
			}
		}
	}

	public float getProgress() {
		return progress;
	}

	/**
	 * リソースを開放する
	 */
	public void dispose() {
		for (T sound : soundmap.values()) {
			if (sound != null) {
				disposeKeySound(sound);
			}
		}
		soundmap.clear();
	}

	/**
	 * 音切りデータ
	 * 
	 * @author exch
	 *
	 * @param <T>
	 */
	class SliceWav<T> {
		public final int starttime;
		public final int duration;
		public final T wav;

		public long playid = -1;

		public SliceWav(Note note, T wav) {
			this.starttime = note.getStarttime();
			this.duration = note.getDuration();
			this.wav = wav;
		}
	}
}
