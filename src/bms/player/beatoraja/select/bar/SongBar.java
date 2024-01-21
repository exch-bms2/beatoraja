package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.graphics.Pixmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * 楽曲バー
 *
 * @author exch
 */
public class SongBar extends SelectableBar {
    /**
     * 楽曲データ
     */
    private final SongData song;
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

    public final SongData getSongData() {
        return song;
    }

    public final boolean existsSong() {
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
    public final String getTitle() {
        return song.getFullTitle();
    }

    public int getLamp(boolean isPlayer) {
    	final ScoreData score = isPlayer ? getScore() : getRivalScore();
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
        // remove duplicates by sha256
        ArrayList<SongData> filteredSongs = new ArrayList<>(Arrays.stream(songs).collect(
                Collectors.toMap(SongData::getSha256, p -> p, (p, q) -> p, LinkedHashMap::new)).values());
        // remove null
        filteredSongs.removeAll(Collections.singleton(null));

        int count = filteredSongs.size();
        SongBar[] result = new SongBar[count--];
        for(SongData song : filteredSongs) {
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
