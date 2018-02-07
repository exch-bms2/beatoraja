package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

import java.util.*;

/**
 * ハッシュ集合を持ち、各ハッシュ値に該当する楽曲を含むフォルダバー
 *
 * @author exch
 */
public class HashBar extends DirectoryBar {
    private String title;
    private SongData[] elements;
    private String[] elementsHash;
    private SongData[] songs;

    public HashBar(MusicSelector selector, String title, SongData[] elements) {
        super(selector);
        this.title = title;
        setElements(elements);;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public SongData[] getElements() {
        return elements;
    }

    public void setElements(SongData[] elements) {
        this.elements = elements;
        elementsHash = new String[elements.length];
        for(int i = 0;i < elementsHash.length;i++) {
        	elementsHash[i] = elements[i].getSha256().length() > 0 ? elements[i].getSha256() : elements[i].getMd5();
        }
        songs = null;
    }

    @Override
    public Bar[] getChildren() {
        List<SongBar> songbars = new ArrayList<SongBar>(elements.length);
        String[] hashes = new String[elements.length];
        for(int i = 0;i < hashes.length;i++) {
            hashes[i] = elements[i].getSha256().length() > 0 ? elements[i].getSha256() : elements[i].getMd5();
        }
        if(songs == null) {
            songs = selector.getSongDatabase().getSongDatas(hashes);
        }
        for(SongData element : elements) {
            boolean exist = false;
            for (SongData song : songs) {
                if((element.getMd5().length() > 0 && element.getMd5().equals(song.getMd5()))
                        || (element.getSha256().length() > 0 && element.getSha256().equals(song.getSha256()))) {
                    songbars.add(new SongBar(song));
                    exist = true;
                    break;
                }
            }
            if(!exist && element.getTitle() != null) {
                songbars.add(new SongBar(element));
            }
        }

        return songbars.toArray(new Bar[songbars.size()]);
    }

    public void updateFolderStatus() {
        if(songs == null) {
            songs = selector.getSongDatabase().getSongDatas(elementsHash);        	
        }
        updateFolderStatus(songs);
    }
}
