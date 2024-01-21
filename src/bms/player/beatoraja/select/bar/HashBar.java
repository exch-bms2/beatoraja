package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

import java.util.*;
import java.util.stream.Stream;

/**
 * ハッシュ集合を持ち、各ハッシュ値に該当する楽曲を含むフォルダバー
 *
 * @author exch
 */
public class HashBar extends DirectoryBar {
    private final String title;
    private SongData[] elements;
    private String[] elementsHash;

    public HashBar(MusicSelector selector, String title, SongData[] elements) {
        super(selector);
        this.title = title;
        setElements(elements);;
    }

    @Override
    public final String getTitle() {
        return title;
    }

    public final SongData[] getElements() {
        return elements;
    }

    public final void setElements(SongData[] elements) {
        this.elements = elements;
        elementsHash = Stream.of(elements).map(e -> e.getSha256().length() > 0 ? e.getSha256() : e.getMd5()).toArray(String[]::new);
    }

    @Override
    public final Bar[] getChildren() {
        return SongBar.toSongBarArray(selector.getSongDatabase().getSongDatas(elementsHash), elements);
    }

    public void updateFolderStatus() {
        updateFolderStatus(selector.getSongDatabase().getSongDatas(elementsHash));
    }
}
