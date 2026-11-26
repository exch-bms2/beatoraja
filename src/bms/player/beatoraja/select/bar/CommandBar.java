package bms.player.beatoraja.select.bar;

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
        return SongBar.toSongBarArray(querySongs());
    }

    public void updateFolderStatus() {
        updateFolderStatus(querySongs());
    }

    private bms.player.beatoraja.song.SongData[] querySongs() {
        var main = selector.main;
        String info = main.getInfoDatabase() != null ? "songinfo.db" : null;
        if (selector.getScoreDataCache() instanceof bms.player.beatoraja.select.PrimaryScoreDataCache cache)
            return cache.querySongs(main.getSongDatabase(), sql, info);
        var paths = main.getPlayDataAccessor().scoreQueryPaths(bms.player.beatoraja.play.SecondaryScoringRule.BEATORAJA);
        return main.getSongDatabase().getSongDatas(sql, paths.score(), paths.scorelog(), info);
    }
}
