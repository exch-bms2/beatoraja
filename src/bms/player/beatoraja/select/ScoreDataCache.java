package bms.player.beatoraja.select;

import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ScoreDatabaseAccessor.ScoreDataCollector;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.*;

/**
 * スコアデータのキャッシュ
 *
 * @author exch
 */
public abstract class ScoreDataCache {

    protected String cacheKey(SongData song) { return song.getSha256(); }

    // TODO ResourcePoolベースに移行する

    /**
     * スコアデータのキャッシュ
     */
    private ObjectMap<String, ScoreData>[] scorecache;

    public ScoreDataCache() {
        scorecache = new ObjectMap[4];
        for (int i = 0; i < scorecache.length; i++) {
            scorecache[i] = new ObjectMap(2000);
        }
    }

    /**
     * 指定した楽曲データ、LN MODEに対するスコアデータを返す
     * @param song 楽曲データ
     * @param lnmode LN MODE
     * @return スコアデータ。存在しない場合はnull
     */
    public ScoreData readScoreData(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        if (scorecache[cacheindex].containsKey(cacheKey(song))) {
            return scorecache[cacheindex].get(cacheKey(song));
        }
        ScoreData score = readScoreDatasFromSource(song, lnmode);
        scorecache[cacheindex].put(cacheKey(song), score);
        return score;
    }

    /**
     *
     * @param collector
     * @param songs
     * @param lnmode
     */
    public void readScoreDatas(ScoreDataCollector collector, SongData[] songs, int lnmode) {
        // キャッシュからの抽出
        Array<SongData> noscore = null;
        for (SongData song : songs) {
            final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;

            if (scorecache[cacheindex].containsKey(cacheKey(song))) {
                collector.collect(song, scorecache[cacheindex].get(cacheKey(song)));
            } else {
            	if(noscore == null) {
            		noscore = new Array<SongData>();
            	}
                noscore.add(song);
            }
        }

        if(noscore == null) {
            return;
        }
        // キャッシュに存在しなかったスコアデータをキャッシュに登録
        final SongData[] noscores = noscore.toArray(SongData.class);

        final ScoreDataCollector cachecollector = (song, score) -> {
            final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
            scorecache[cacheindex].put(cacheKey(song), score);
        	collector.collect(song, score);
        };
        readScoreDatasFromSource(cachecollector, noscores, lnmode);
    }

    boolean existsScoreDataCache(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        return scorecache[cacheindex].containsKey(cacheKey(song));
    }

    public void clear() {
        for (ObjectMap<?, ?> cache : scorecache) {
            cache.clear();
        }
    }

    public void update(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        ScoreData score = readScoreDatasFromSource(song, lnmode);
        scorecache[cacheindex].put(cacheKey(song), score);
    }

    protected abstract ScoreData readScoreDatasFromSource(SongData songs, int lnmode);

    protected abstract void readScoreDatasFromSource(ScoreDataCollector collector, SongData[] songs, int lnmode);
}
