package bms.player.beatoraja.select;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.song.SongData;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * スコアデータのキャッシュ
 *
 * @author exch
 */
public abstract class ScoreDataCache {

    // TODO ResourcePoolベースに移行する

    /**
     * スコアデータのキャッシュ
     */
    private ObjectMap<String, IRScoreData>[] scorecache;

    public ScoreDataCache() {
        scorecache = new ObjectMap[4];
        for (int i = 0; i < scorecache.length; i++) {
            scorecache[i] = new ObjectMap(2000);
        }
    }

    public IRScoreData readScoreData(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        if (scorecache[cacheindex].containsKey(song.getSha256())) {
            return scorecache[cacheindex].get(song.getSha256());
        }
        IRScoreData score = readScoreDatasFromSource(song, lnmode);
        scorecache[cacheindex].put(song.getSha256(), score);
        return score;
    }

    public Map<String, IRScoreData> readScoreDatas(SongData[] songs, int lnmode) {
        Map<String, IRScoreData> result = new HashMap<String, IRScoreData>(songs.length);
        // キャッシュからの抽出
        List<SongData> noscore = new ArrayList<SongData>();
        for (SongData song : songs) {
            final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;

            if (scorecache[cacheindex].containsKey(song.getSha256())) {
                result.put(song.getSha256(), scorecache[cacheindex].get(song.getSha256()));
            } else {
                noscore.add(song);
            }
        }

        if(noscore.size() == 0) {
            return result;
        }

        // データベースから抽出し、キャッシュに登録
        Map<String, IRScoreData> scores = readScoreDatasFromSource(noscore.toArray(new SongData[noscore.size()]), lnmode);
        for (SongData song : noscore) {
            final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
            IRScoreData score = scores.get(song.getSha256());
            scorecache[cacheindex].put(song.getSha256(), score);
            result.put(song.getSha256(), score);
        }
        return result;
    }

    boolean existsScoreDataCache(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        return scorecache[cacheindex].containsKey(song.getSha256());
    }

    public void clear() {
        for (ObjectMap<?, ?> cache : scorecache) {
            cache.clear();
        }
    }

    public void update(SongData song, int lnmode) {
        final int cacheindex = song.hasUndefinedLongNote() ? lnmode : 3;
        IRScoreData score = readScoreDatasFromSource(song, lnmode);
        scorecache[cacheindex].put(song.getSha256(), score);
    }

    protected abstract IRScoreData readScoreDatasFromSource(SongData songs, int lnmode);

    protected abstract Map<String, IRScoreData> readScoreDatasFromSource(SongData[] songs, int lnmode);
}