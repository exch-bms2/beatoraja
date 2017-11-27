package bms.player.beatoraja.select.bar;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.song.SongData;

/**
 * SQLで問い合わせた楽曲を表示するためのバー
 *
 * @author exch
 */
public class CommandBar extends DirectoryBar {

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
    public Bar[] getChildren() {
        SongData[] infos = main.getSongDatabase().getSongDatas(sql,"player/" + main.getConfig().getPlayername() + "/score.db"
        		,"player/" + main.getConfig().getPlayername() + "/scorelog.db",main.getInfoDatabase() != null ? "songinfo.db" : null);
       Bar[] l = new Bar[infos.length];
       for(int i = 0;i < infos.length;i++) {
           l[i] = new SongBar(infos[i]);    	   
       }
        return l;
    }

}
