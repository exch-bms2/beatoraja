package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLで問い合わせた楽曲を表示するためのバー
 *
 * @author exch
 */
public class CommandBar extends DirectoryBar {

    // TODO song.dbへの問い合わせの追加

    private MainController main;
    private String title;
    private String sql;

    public CommandBar(MainController main, String title, String sql) {
        this.main = main;
        this.title = title;
        this.sql = sql;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getLamp() {
        return 0;
    }

    @Override
    public Bar[] getChildren() {
        SongData[] infos = main.getSongDatabase().getSongDatas(sql,"player/" + main.getConfig().getPlayername() + "/score.db",main.getInfoDatabase() != null ? 
        		"songinfo.db" : null);
        List<Bar> l = new ArrayList<Bar>();
        for (SongData info : infos) {
            l.add(new SongBar(info));
//            System.out.println(info.getSha256() + " " + info.getFullTitle());
        }
        return l.toArray(new Bar[l.size()]);
    }

}
