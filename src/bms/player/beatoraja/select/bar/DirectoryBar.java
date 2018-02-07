package bms.player.beatoraja.select.bar;

import java.util.Arrays;
import java.util.Map;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.song.SongData;

/**
 * ディレクトリの抽象バー。
 * 
 * @author exch
 */
public abstract class DirectoryBar extends Bar {

	protected final MusicSelector selector;
	/**
	 * プレイヤーのクリアランプ数
	 */
    private int[] lamps = new int[11];
	/**
	 * ライバルのクリアランプ数
	 */
    private int[] rlamps = new int[11];
	/**
	 * プレイヤーのランク数
	 */
    private int[] ranks = new int[28];

    public DirectoryBar(MusicSelector selector) {
    	this.selector = selector;
    }
    
    public int[] getLamps() {
        return lamps;
    }

    public void setLamps(int[] lamps) {
        this.lamps = lamps;
    }

    public int[] getRivalLamps() {
        return rlamps;
    }

    public void setRivalLamps(int[] lamps) {
        this.rlamps = lamps;
    }

    public int[] getRanks() {
        return ranks;
    }

    public void setRanks(int[] ranks) {
        this.ranks = ranks;
    }

    public int getLamp(boolean isPlayer) {
    	final int[] lamps = isPlayer ? this.lamps : rlamps;
        for (int i = 0; i < lamps.length; i++) {
            if (lamps[i] > 0) {
                return i;
            }
        }
        return 0;
    }
    
    public void clear() {
    	Arrays.fill(lamps, 0);
    	Arrays.fill(rlamps, 0);
    	Arrays.fill(ranks, 0);
    }

    /**
     * ディレクトリ内のバーを返す
     * 
     * @return ディレクトリ内のバー
     */
    public abstract Bar[] getChildren();
    
    public void updateFolderStatus() {
    	
    }
    
    protected void updateFolderStatus(SongData[] songs) {
        clear();
        final Map<String, IRScoreData> scores = selector.getScoreDataCache()
                .readScoreDatas(songs, selector.getMainController().getPlayerResource().getPlayerConfig().getLnmode());
        for (SongData song : songs) {
            final IRScoreData score = scores.get(song.getSha256());
            if (score != null) {
                lamps[score.getClear()]++;
                if (score.getNotes() != 0) {
                    ranks[(score.getExscore() * 27 / (score.getNotes() * 2))]++;
                } else {
                    ranks[0]++;
                }
            } else {
                ranks[0]++;
                lamps[0]++;
            }
        }
    }
}
