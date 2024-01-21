package bms.player.beatoraja.select.bar;

import java.io.File;

import bms.player.beatoraja.MainController;
import bms.player.beatoraja.select.MusicSelector;

/**
 * SQLで問い合わせた楽曲を表示するためのバー
 *
 * @author exch
 */
public class CommandBar extends DirectoryBar {

	/**
	 * バータイトル
	 */
    private final String title;
    /**
     * DBに対するSQL
     */
    private final String sql;

    public CommandBar(MusicSelector selector, String title, String sql) {
    	this(selector, title, sql, false);
    }

    public CommandBar(MusicSelector selector, String title, String sql, boolean showInvisibleChart) {
    	super(selector, showInvisibleChart);
        this.title = title;
        this.sql = sql;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Bar[] getChildren() {
    	final MainController main = selector.main;
        return SongBar.toSongBarArray(main.getSongDatabase().getSongDatas(sql,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/score.db"
        		,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/scorelog.db",main.getInfoDatabase() != null ? "songinfo.db" : null));
    }

    public void updateFolderStatus() {
    	final MainController main = selector.main;
        updateFolderStatus(main.getSongDatabase().getSongDatas(sql,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/score.db"
        		,main.getConfig().getPlayerpath() + File.separatorChar + main.getConfig().getPlayername() + "/scorelog.db",main.getInfoDatabase() != null ? "songinfo.db" : null));
    }
}
