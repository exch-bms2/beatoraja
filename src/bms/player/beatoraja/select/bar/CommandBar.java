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
    private MusicSelector selector;
    private String title;
    private String sql;
    private int type;

    public CommandBar(MainController main, MusicSelector selector, String title, String sql) {
    	this(main, selector, title, sql, 0);
    }

    public CommandBar(MainController main, MusicSelector selector, String title, String sql, int type) {
        this.main = main;
        this.selector = selector;
        this.title = title;
        this.sql = sql;
        this.type = type;
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
    	if(type == 2) {
    		if(main.getInfoDatabase() == null) {
    			return new Bar[0];
    		}
    		SongInformation[] infos = main.getInfoDatabase().getInformations(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (SongInformation info : infos) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", info.getSha256());
                if(song.length > 0) {
                    l.add(new SongBar(song[0]));
                }
            }
            return l.toArray(new Bar[l.size()]);
    	} else if(type == 1) {
            SongData[] infos = main.getSongDatabase().getSongDatas(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (SongData info : infos) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", info.getSha256());
                if(song.length > 0) {
                    l.add(new SongBar(song[0]));
                }
            }
            return l.toArray(new Bar[l.size()]);
        } else{
            List<IRScoreData> scores = main.getPlayDataAccessor().readScoreDatas(sql);
            List<Bar> l = new ArrayList<Bar>();
            for (IRScoreData score : scores) {
                SongData[] song = selector.getSongDatabase().getSongDatas("sha256", score.getSha256());
                if (song.length > 0 && (!song[0].hasUndefinedLongNote() || selector.getMainController().getPlayerResource().getPlayerConfig().getLnmode() == score.getMode())) {
                    l.add(new SongBar(song[0]));
                }
            }
            return l.toArray(new Bar[l.size()]);
        }
    }

}
