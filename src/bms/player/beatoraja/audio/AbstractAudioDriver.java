package bms.player.beatoraja.audio;

import bms.model.*;

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
		if(p == null || p.length() == 0) {
			return;
		}
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
		if(p == null || p.length() == 0) {
			return;
		}
		T sound = soundmap.get(p);
		if (sound != null) {
			stop(sound);
		}
	}

	public void dispose(String p) {
		if(p == null || p.length() == 0) {
			return;
		}
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
		wavmap = new Object[wavcount];
		slicesound = new SliceWav[wavcount][];

		progress = 0;
		// BMS格納ディレクトリ
		Path dpath = Paths.get(model.getPath()).getParent();

		if (model.getVolwav() > 0 && model.getVolwav() < 100) {
			volume = model.getVolwav() / 100f;
		}

		List<SliceWav>[] slicesound = new List[wavcount];

		Map<Integer, List<Note>> notemap = new HashMap();
		final int lanes = model.getMode().key;
		for (TimeLine tl : model.getAllTimeLines()) {
			for (int i = 0; i < lanes; i++) {
				final Note n = tl.getNote(i);
				if (n != null) {
					addNoteList(notemap, n);
					for(Note ln : n.getLayeredNotes()) {
						addNoteList(notemap, ln);
					}
				}
				if (tl.getHiddenNote(i) != null) {
					addNoteList(notemap, tl.getHiddenNote(i));
				}
			}
			for(Note n : tl.getBackGroundNotes()) {
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
			PCM wav = null;

			for(Note note : waventry.getValue()) {
				if (note.getStarttime() == 0 && note.getDuration() == 0) {
					// 音切りなしのケース
					Path p = dpath.resolve(name);
					wavmap[wavid] = cache.get(p.toString(), note);
					if (wavmap[wavid] == null) {
						wavmap[wavid] = getKeySound(p);
						if(wavmap[wavid] == null) {
							break;
						}
						cache.put(p.toString(), note, (T)wavmap[wavid]);
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
						Path p = dpath.resolve(name);
						T sliceaudio = cache.get(p.toString(), note);
						if (sliceaudio != null) {
							slicesound[note.getWav()].add(new SliceWav(note, sliceaudio));
						} else {
							if(wav == null) {
								name = name.substring(0, name.lastIndexOf('.'));
								final Path wavfile = dpath.resolve(name + ".wav");
								final Path oggfile = dpath.resolve(name + ".ogg");
								final Path mp3file = dpath.resolve(name + ".mp3");
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
								try {
									final PCM slicewav = wav.slice(note.getStarttime(), note.getDuration());
									T sound = getKeySound(slicewav);
									cache.put(p.toString(), note, sound);
									slicesound[note.getWav()].add(new SliceWav(note, sound));
									// System.out.println("WAV slicing - Name:"
									// + name + " ID:" + note.getWav() +
									// " start:" + note.getStarttime() +
									// " duration:" + note.getDuration());
								} catch (Throwable e) {
									Logger.getGlobal().warning("音源(wav)ファイルスライシング失敗。" + e.getMessage());
									e.printStackTrace();
								}
							} else {
								break;
							}							
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
		
		cache.disposeOld();

		progress = 1;
	}
	
	private void addNoteList(Map<Integer, List<Note>> notemap, Note n) {
		if(n.getWav() < 0) {
			return;
		}
		List<Note> notes = notemap.get(n.getWav());
		if(notes == null) {
			notes = new ArrayList<Note>();
			notemap.put(n.getWav(), notes);
		}
		
		for(Note note : notes) {
			if(n.getStarttime() == note.getStarttime() && n.getDuration() == note.getDuration()) {
				return;
			}
		}
		notes.add(n);
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
	
	
	public class AudioCache {
		
		private static final int MAX_GENERATION = 1;
		
		private Map<String, Set<SliceWav<T>>> audio = new HashMap<String, Set<SliceWav<T>>> ();

		public T get(String s, Note n) {
			if(!audio.containsKey(s)) {
				return null;
			}
			for(SliceWav<T> entry : audio.get(s)) {
				if(n.getStarttime() == entry.starttime && n.getDuration() == entry.duration) {
//					System.out.println("AudioCache : リソース再利用 - " + s);
					entry.playid = 0;
					return entry.wav;
				}
			}
			return null;
		}
		
		public void put(String s, Note n, T sound) {
			if(!audio.containsKey(s)) {
				audio.put(s, new HashSet());
			}
			for(SliceWav<T> entry : audio.get(s)) {
				if(n.getStarttime() == entry.starttime && n.getDuration() == entry.duration) {
					entry.playid = 0;
					return;
				}
			}
			SliceWav<T> entry = new SliceWav(n, sound);
			entry.playid = 0;
			audio.get(s).add(entry);
		}
		
		public void disposeOld() {
			String[] keyset = audio.keySet().toArray(new String[audio.size()]);
			for(String s : keyset) {
				Set<SliceWav<T>> set = audio.get(s);
				for(SliceWav<T> wav : set.toArray(new SliceWav[set.size()])) {
					if(wav.playid == MAX_GENERATION) {
						disposeKeySound(wav.wav);
//						System.out.println("AudioCache : リソース開放 - " + s);
						set.remove(wav);
					} else {
						wav.playid++;
					}					
				}
				if(set.isEmpty()) {
					audio.remove(s);
				}
			}
			Logger.getGlobal().info("現在のAudioCache容量 : " + audio.size());
		}
	}	
}
