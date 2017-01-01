package bms.player.beatoraja.audio;

import bms.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by exch on 2016/12/30.
 */
public abstract class AbstractAudioDriver<T> implements AudioDriver {

    /**
     * キー音マップ(音切りなし)
     */
    private T[] wavmap;
    /**
     * キー音マップ(音切りなし):再生状況
     */
    private long[] playmap = new long[0];
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

    protected abstract void initKeySound(int count);

    protected abstract T getKeySound(Path p);

    protected abstract T getKeySound(PCM pcm);

    protected abstract void play(int id, float volume);

    protected abstract void play(SliceWav<T> id, float volume);

    protected abstract void stop();

    protected abstract void stop(int id);

    protected abstract void stop(SliceWav<T> id);

    protected void setWavmap(T[] wavmap) {
        this.wavmap = wavmap;
    }

    protected T[] getWavmap() {
        return wavmap;
    }

    protected long[] getPlaymap() {
        return playmap;
    }

    protected void setSlicesound(SliceWav<T>[][] slice) {
        this.slicesound = slice;
    }

    protected SliceWav<T>[][] getSlicesound() {
        return slicesound;
    }

    protected float getVolume() {
        return volume;
    }

    /**
     * BMSの音源データを読み込む
     *
     * @param model
     */
    public void setModel(BMSModel model) {
        final int wavcount = model.getWavList().length;
        initKeySound(wavcount);
        playmap = new long[wavmap.length];
        Arrays.fill(playmap, -1);

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
                play(id, volume);
            } else {
                for (SliceWav slice : slicesound[id]) {
                    if (slice.starttime == starttime && slice.duration == duration) {
                        play(slice, volume);
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
                stop();
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
            stop(id);
        } else {
            for (SliceWav slice : slicesound[id]) {
                if (slice.starttime == starttime && slice.duration == duration) {
                    stop(slice);
                    break;
                }
            }
        }
    }

    public float getProgress() {
        return progress;
    }

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

