package bms.player.beatoraja.audio;

import bms.model.*;
import bms.player.beatoraja.ResourcePool;

import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
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
	private Map<String, AudioElement<T>> soundmap = new HashMap<String, AudioElement<T>>();
	/**
	 * キー音マップ(音切りなし)
	 */
	private T[] wavmap = (T[]) new Object[0];
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
	 * 
	 */
	private AudioCache cache = new AudioCache();

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
	 * キー音を再生する
	 * 
	 * @param id
	 *            音源データ
	 * @param volume
	 *            ボリューム(0.0-1.0)
	 */
	protected abstract void play(T id, float volume);

	/**
	 * 効果音を再生する
	 * 
	 * @param id
	 *            音源データ
	 * @param volume
	 *            ボリューム(0.0-1.0)
	 * @param loop
	 *            ループ再生するかどうか
	 */
	protected abstract void play(AudioElement<T> id, float volume, boolean loop);
	
	/**
	 * 効果音のボリュームを設定する。再生中も可能
	 * 
	 * @param id
	 *            音源データ
	 * @param volume
	 *            ボリューム(0.0-1.0)
	 */
	protected abstract void setVolume(AudioElement<T> id, float volume);

	/**
	 * 音源データが再生されていれば停止する
	 * 
	 * @param id
	 *            音源データ
	 */
	protected abstract void stop(T id);

	public void play(String p, float volume, boolean loop) {
		if (p == null || p.length() == 0) {
			return;
		}
		AudioElement<T> sound = soundmap.get(p);
		if (!soundmap.containsKey(p)) {
			try {
				sound = new AudioElement(getKeySound(Paths.get(p)));
				soundmap.put(p, sound.audio != null ? sound : null);
			} catch (GdxRuntimeException e) {
				Logger.getGlobal().warning("音源読み込み失敗。" + e.getMessage());
			}
		}

		if (sound != null) {
			play(sound, volume, loop);
		}
	}

	public void setVolume(String p, float volume) {
		if (p == null || p.length() == 0) {
			return;
		}
		AudioElement<T> sound = soundmap.get(p);
		if (sound != null) {
			setVolume(sound, volume);
		}
	}
	
	public void stop(String p) {
		if (p == null || p.length() == 0) {
			return;
		}
		AudioElement<T> sound = soundmap.get(p);
		if (sound != null) {
			stop(sound.audio);
		}
	}

	public void dispose(String p) {
		if (p == null || p.length() == 0) {
			return;
		}
		AudioElement<T> sound = soundmap.get(p);
		if (sound != null) {
			soundmap.remove(p);
			disposeKeySound(sound.audio);
		}
	}

	/**
	 * BMSの音源データを読み込む
	 *
	 * @param model
	 */
	public synchronized void setModel(BMSModel model) {
		final int wavcount = model.getWavList().length;
		wavmap = (T[]) new Object[wavcount];
		slicesound = new SliceWav[wavcount][];

		progress = 0;
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		if (model.getVolwav() > 0 && model.getVolwav() < 100) {
			volume = model.getVolwav() / 100f;
		}

		List<SliceWav<T>>[] slicesound = new List[wavcount];

		Map<Integer, List<Note>> notemap = new HashMap<Integer, List<Note>>();
		final int lanes = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				final Note n = tl.getNote(i);
				if (n != null) {
					addNoteList(notemap, n);
					for (Note ln : n.getLayeredNotes()) {
						addNoteList(notemap, ln);
					}
				}
				if (tl.getHiddenNote(i) != null) {
					addNoteList(notemap, tl.getHiddenNote(i));
				}
			}
			for (Note n : tl.getBackGroundNotes()) {
				addNoteList(notemap, n);
			}
		}

		for (Entry<Integer, List<Note>> waventry : notemap.entrySet()) {
			final int wavid = waventry.getKey();
			if (progress >= 1) {
				break;
			}
			if (wavid < 0) {
				continue;
			}
			String name = model.getWavList()[wavid];
			for (Note note : waventry.getValue()) {
				if (note.getMicroStarttime() == 0 && note.getMicroDuration() == 0) {
					// 音切りなしのケース
					Path p = dpath.resolve(name);
					wavmap[wavid] = cache.get(new AudioKey(p.toString(), note));
					if (wavmap[wavid] == null) {
						break;
					}
				} else {
					// 音切りありのケース
					boolean b = true;
					if (slicesound[note.getWav()] == null) {
						slicesound[note.getWav()] = new ArrayList<SliceWav<T>>();
					}
					for (SliceWav<T> slice : slicesound[note.getWav()]) {
						if (slice.starttime == note.getMicroStarttime() && slice.duration == note.getMicroDuration()) {
							b = false;
							break;
						}
					}
					if (b) {
						Path p = dpath.resolve(name);
						T sliceaudio = cache.get(new AudioKey(p.toString(), note));
						if (sliceaudio != null) {
							slicesound[note.getWav()].add(new SliceWav<T>(note, sliceaudio));
						} else {
							break;
						}
					}
				}
			}
			progress += 1f / notemap.keySet().size();
		}

		Logger.getGlobal().info("音源ファイル読み込み完了。音源数:" + wavmap.length);
		for (int i = 0; i < wavmap.length; i++) {
			if (slicesound[i] != null) {
				this.slicesound[i] = slicesound[i].toArray(new SliceWav[slicesound[i].size()]);
			} else {
				this.slicesound[i] = new SliceWav[0];
			}
		}

		final int prevsize = cache.size();
		cache.disposeOld();
		Logger.getGlobal().info("AudioCache容量 : " + cache.size() + " 開放 : " + (prevsize - cache.size()));

		progress = 1;
	}

	private void addNoteList(Map<Integer, List<Note>> notemap, Note n) {
		if (n.getWav() < 0) {
			return;
		}
		List<Note> notes = notemap.get(n.getWav());
		if (notes == null) {
			notes = new ArrayList<Note>();
			notemap.put(n.getWav(), notes);
		}

		for (Note note : notes) {
			if (n.getMicroStarttime() == note.getMicroStarttime() && n.getMicroDuration() == note.getMicroDuration()) {
				return;
			}
		}
		notes.add(n);
	}

	public void abort() {
		progress = 1;
	}

	public void play(Note n, float volume) {
		play0(n, this.volume * volume);
		for (Note ln : n.getLayeredNotes()) {
			play0(ln, this.volume * volume);
		}
	}

	private final void play0(Note n, float volume) {
		try {
			final int id = n.getWav();
			if (id < 0) {
				return;
			}
			final long starttime = n.getMicroStarttime();
			final long duration = n.getMicroDuration();
			if (starttime == 0 && duration == 0) {
				final T wav = (T) wavmap[id];
				if (wav != null) {
					stop(wav);
					play(wav, volume);
				}
			} else {
				for (SliceWav<T> slice : slicesound[id]) {
					if (slice.starttime == starttime && slice.duration == duration) {
						play(slice.wav, volume);
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
				for (T s : wavmap) {
					if (s != null) {
						stop(s);
					}
				}
				for (SliceWav<T>[] slices : slicesound) {
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
		final long starttime = n.getMicroStarttime();
		final long duration = n.getMicroDuration();
		if (starttime == 0 && duration == 0) {
			final T sound = (T) wavmap[id];
			if (sound != null) {
				stop(sound);
			}
		} else {
			for (SliceWav<T> slice : slicesound[id]) {
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
		for (AudioElement<T> sound : soundmap.values()) {
			if (sound != null) {
				disposeKeySound(sound.audio);
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
	static class SliceWav<T> {
		public final long starttime;
		public final long duration;
		public final T wav;

		public long playid = -1;

		public SliceWav(Note note, T wav) {
			this.starttime = note.getMicroStarttime();
			this.duration = note.getMicroDuration();
			this.wav = wav;
		}
	}

	class AudioCache extends ResourcePool<AudioKey, T> {

		public AudioCache() {
			super(1);
		}

		private String path;
		private PCM wav;

		@Override
		protected T load(AudioKey key) {
			if (!key.path.equals(path)) {
				wav = null;
			}
			if (key.start == 0 && key.duration == 0) {
				// 音切りなしのケース
				return getKeySound(Paths.get(key.path));
			} else {
				if (wav == null) {
					String name = key.path.substring(0, key.path.lastIndexOf('.'));
					final Path wavfile = Paths.get(name + ".wav");
					final Path oggfile = Paths.get(name + ".ogg");
					final Path mp3file = Paths.get(name + ".mp3");
					if (wav == null && Files.exists(wavfile)) {
						try {
							wav = new PCM(wavfile);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					if (wav == null && Files.exists(oggfile)) {
						try {
							wav = new PCM(oggfile);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					if (wav == null && Files.exists(mp3file)) {
						try {
							wav = new PCM(mp3file);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}

				if (wav != null) {
					path = key.path;
					try {
						final PCM slicewav = wav.slice(key.start, key.duration);
						return getKeySound(slicewav);
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
			return null;
		}

		@Override
		protected void dispose(T resource) {
			disposeKeySound(resource);
		}
	}
	
	static class AudioElement<T> {
		public long id;
		public final T audio;
		
		public AudioElement(T audio) {
			this.audio = audio;
		}		
	}
	
	/**
	 * AudioCache Key
	 * 
	 * @author exch
	 */
	private static class AudioKey {
		/**
		 * Audio File path
		 */
		public final String path;
		/**
		 * Audio start time(us)
		 */
		public final long start;
		/**
		 * Audio duration(us)
		 */
		public final long duration;

		public AudioKey(String path, Note n) {
			this.path = path;
			this.start = n.getMicroStarttime();
			this.duration = n.getMicroDuration();
		}

		public boolean equals(Object o) {
			if (o instanceof AudioKey) {
				final AudioKey key = (AudioKey) o;
				return path.equals(key.path) && start == key.start && duration == key.duration;
			}
			return false;
		}
		
		public int hashCode() {
			return Objects.hash(path, start, duration);
		}
	}
}
