package bms.player.beatoraja.select;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.Config.SongPreview;
import bms.player.beatoraja.audio.AudioDriver;
import bms.player.beatoraja.song.SongData;

/**
 * プレビュー再生管理用クラス
 *
 * @author exch
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

    private final Config config;

    public PreviewMusicProcessor(AudioDriver audio, Config config) {
        this.audio = audio;
        this.config = config;
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

        String previewPath = "";
        if (song != null && song.getPreview() != null && song.getPreview().length() > 0) {
            try {
                previewPath = Paths.get(song.getPath()).getParent().resolve(song.getPreview()).toString();
            } catch (InvalidPathException e) {
                Logger.getGlobal().warning(e.getMessage());
            }
        }
        commands.add(previewPath);
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
        private float currentVolume;

        public void run() {
            audio.play(defaultMusic, config.getAudioConfig().getSystemvolume(), true);
            playing = defaultMusic;
            currentVolume = config.getAudioConfig().getSystemvolume();
            while(!stop) {
                if(!commands.isEmpty()) {
                    String path = commands.removeFirst();
                    if(path.length() == 0) {
                        path = defaultMusic;
                    }
                    if(!path.equals(playing)) {
                        stopPreview(true);
                        if(!path.equals(defaultMusic)) {
                            audio.play(path, config.getAudioConfig().getSystemvolume(), config.getSongPreview() == SongPreview.LOOP);
                        } else {
                            audio.setVolume(defaultMusic, config.getAudioConfig().getSystemvolume());
                        }
                        playing = path;
                    }
                } else if(!Objects.equals(playing, defaultMusic) && !audio.isPlaying(playing)){
                	// プレビュー演奏終了後に選曲BGMに戻す
                    stopPreview(true);
                    audio.setVolume(defaultMusic, config.getAudioConfig().getSystemvolume());
                    playing = defaultMusic;
                } else if(currentVolume != config.getAudioConfig().getSystemvolume()){
                    audio.setVolume(playing, config.getAudioConfig().getSystemvolume());
                    currentVolume = config.getAudioConfig().getSystemvolume();
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
                	for(int i = 10;i >= 0;i--) {
                		float vol = i * 0.1f * config.getAudioConfig().getSystemvolume();
                        audio.setVolume(playing, vol);
                        // TODO フェードアウトはAudioDriver側で実装したい
                        try {
							sleep(15);
						} catch (InterruptedException e) {
						}
                	}
                } else {
                    audio.stop(playing);
                }
            }
        }
    }
}
