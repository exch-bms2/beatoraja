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
        SongBar[] result = new SongBar[elements.length];
        for (int i = 0; i < elements.length; i++) {
            SongData element = elements[i];
            element.setPath(null);

            SongData song = findSongData(songs, element);
            if (song != null) {
                element.setPath(song.getPath());
                song.merge(element);
                result[i] = new SongBar(song);
            } else {
                result[i] = new SongBar(element);
            }
        }
        return result;
    }

    private static SongData findSongData(SongData[] songs, SongData element) {
        for (SongData song : songs) {
            if (song != null && matches(song, element)) {
                return song;
            }
        }
        return null;
    }

    private static boolean matches(SongData song, SongData element) {
        return (element.getMd5().length() > 0 && element.getMd5().equals(song.getMd5()))
                || (element.getSha256().length() > 0 && element.getSha256().equals(song.getSha256()));
    }
}
