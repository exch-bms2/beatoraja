package bms.player.beatoraja.select;

import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.song.SongData;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * プレビュー再生管理用クラス
 */
public class PreviewMusicProcessor {
    /**
     * 音源読み込みタスク
     */
    private Deque<String> commands = new ConcurrentLinkedDeque<String>();

    private PreviewThread preview;

    private String defaultMusic = "";

    private SongData current;

    private final AudioDriver audio;

    public PreviewMusicProcessor(AudioDriver audio) {
        this.audio = audio;
    }

    public void setDefault(String path) {
        defaultMusic = (path != null ? path : "");
    }

    public void start(SongData song) {
        if(preview == null) {
            preview = new PreviewThread();
            preview.start();
        }
        current = song;
        commands.add(song != null && song.getPreview() != null && song.getPreview().length() > 0 ?
                Paths.get(song.getPath()).getParent().resolve(song.getPreview()).toString() : "");
    }

    public SongData getSongData() {
        return current;
    }

    public void stop() {
        preview.stop = true;
        preview = null;
    }

    class PreviewThread extends Thread {

        private boolean stop;
        private String playing;

        public void run() {
            audio.play(defaultMusic, true);
            while(!stop) {
                if(!commands.isEmpty()) {
                    String path = commands.removeFirst();
                    if(path.length() == 0) {
                        path = defaultMusic;
                    }
                    if(!path.equals(playing)) {
                        stopPreview(true);
                        if(path.length() > 0) {
                            audio.play(path, false);
                        } else {
                            // TODO 選曲BGMは再開
                            audio.play(defaultMusic, true);
                            path = defaultMusic;
                        }
                        playing = path;
                    }
                } else {
                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            }
            this.stopPreview(false);
        }

        private void stopPreview(boolean pause) {
            if(playing != null && playing.length() > 0) {
                if(!playing.equals(defaultMusic)) {
                    audio.stop(playing);
                    audio.dispose(playing);
                } else if(pause) {
                    // TODO 選曲BGMの場合は一時停止
                    audio.stop(playing);
                } else {
                    audio.stop(playing);
                }
            }
        }
    }
}
