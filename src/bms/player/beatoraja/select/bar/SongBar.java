package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Pixmap;

import java.util.ArrayList;
import java.util.List;

/**
 * 楽曲バー
 *
 * @author exch
 */
public class SongBar extends SelectableBar {
    /**
     * 楽曲データ
     */
    private SongData song;
    /**
     * バナーデータ
     */
    private Pixmap banner;
    /**
     * ステージファイルデータ
     */
    private Pixmap stagefile;

    public SongBar(SongData song) {
        this.song = song;
    }

    public SongData getSongData() {
        return song;
    }

    public boolean existsSong() {
    	return song.getPath() != null;
    }

    public Pixmap getBanner() {
        return banner;
    }

    public void setBanner(Pixmap banner) {
    	this.banner = banner;
    }

    public Pixmap getStagefile() {
        return stagefile;
    }

    public void setStagefile(Pixmap stagefile) {
    	this.stagefile = stagefile;
    }

    @Override
    public String getTitle() {
        return song.getFullTitle();
    }

    @Override
    public String getArtist() {
        return song.getFullArtist();
    }

    public int getLamp(boolean isPlayer) {
    	final IRScoreData score = isPlayer ? getScore() : getRivalScore();
        if (score != null) {
            return score.getClear();
        }
        return 0;
    }

    /**
     * SongData配列をSongBar配列に変換する
     * @param songs SongData配列
     * @return SongBar配列
     */
    public static SongBar[] toSongBarArray(SongData[] songs) {
        // 重複除外
        int count = songs.length;
        for(int i = 0;i < songs.length;i++) {
            if(songs[i] == null) {
                continue;
            }
            for(int j = i + 1;j < songs.length;j++) {
                if(songs[j] != null && songs[i].getSha256().equals(songs[j].getSha256())) {
                    songs[j] = null;
                    count--;
                }
            }
        }
        SongBar[] result = new SongBar[count--];
        for(SongData song : songs) {
            if(song != null) {
                result[count--] = new SongBar(song);
            }
        }
        return result;
    }

    protected static SongBar[] toSongBarArray(SongData[] songs, SongData[] elements) {
        // 重複除外
        int count = songs.length;
        int noexistscount = elements.length;
        for(SongData element : elements) {
            element.setPath(null);
        }

        for(int i = 0;i < songs.length;i++) {
            if(songs[i] == null) {
                continue;
            }
            for(int j = i + 1;j < songs.length;j++) {
                if(songs[j] != null && songs[i].getSha256().equals(songs[j].getSha256())) {
                    songs[j] = null;
                    count--;
                }
            }
            for(int j = 0;j < elements.length;j++) {
                final SongData element = elements[j];
                if(element.getPath() == null && (element.getMd5().length() > 0 && element.getMd5().equals(songs[i].getMd5()))
                        || (element.getSha256().length() > 0 && element.getSha256().equals(songs[i].getSha256()))) {
                    element.setPath(songs[i].getPath());
                    songs[i].merge(element);
                    noexistscount--;
                    break;
                }
            }
        }
        SongBar[] result = new SongBar[count + noexistscount];
        noexistscount--;
        for(int i = 0;i < elements.length;i++) {
            if(elements[i].getPath() == null) {
                result[count + (noexistscount--)] = new SongBar(elements[i]);
            }
        }
        count--;
        for(SongData song : songs) {
            if(song != null) {
                result[count--] = new SongBar(song);
            }
        }
        return result;
    }
}
