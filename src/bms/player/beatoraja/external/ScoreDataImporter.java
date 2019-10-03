package bms.player.beatoraja.external;

import bms.player.beatoraja.IRScoreData;
import bms.player.beatoraja.ScoreDatabaseAccessor;
import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongDatabaseAccessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ScoreDataImporter {

    private ScoreDatabaseAccessor scoredb;

    public ScoreDataImporter(ScoreDatabaseAccessor scoredb) {
        this.scoredb = scoredb;
    }

    public void importFromLR2ScoreDatabase(String path, SongDatabaseAccessor songdb) {
        final int[] clears = { 0, 1, 4, 5, 6, 8, 9 };
        scoredb.createTable();

        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            QueryRunner qr = new QueryRunner();
            MapListHandler rh = new MapListHandler();
            List<Map<String, Object>> scores = qr.query(con, "SELECT * FROM score", rh);

            List<IRScoreData> result = new ArrayList<IRScoreData>();
            for (Map<String, Object> score : scores) {
                final String md5 = (String) score.get("hash");
                SongData[] song = songdb.getSongDatas(new String[] { md5 });
                if (song.length > 0) {
                    IRScoreData sd = new IRScoreData();
                    sd.setEpg((int) score.get("perfect"));
                    sd.setEgr((int) score.get("great"));
                    sd.setEgd((int) score.get("good"));
                    sd.setEbd((int) score.get("bad"));
                    sd.setEpr((int) score.get("poor"));
                    sd.setMinbp((int) score.get("minbp"));
                    sd.setClear(clears[(int) score.get("clear")]);
                    sd.setPlaycount((int) score.get("playcount"));
                    sd.setClearcount((int) score.get("clearcount"));
                    sd.setSha256(song[0].getSha256());
                    sd.setNotes(song[0].getNotes());
                    result.add(sd);
                }
            }
            
            this.importScores(result.toArray(new IRScoreData[result.size()]), "LR2");
        } catch (Exception e) {
            Logger.getGlobal().severe("スコア移行時の例外:" + e.getMessage());
        }
    }

    public void importScores(IRScoreData[] scores, String scorehash) {
        List<IRScoreData> result = new ArrayList<IRScoreData>();

        for(IRScoreData score : scores) {
            IRScoreData oldsd = scoredb.getScoreData(score.getSha256(), score.getMode());
            if(oldsd == null) {
                oldsd = new IRScoreData();
                oldsd.setPlaycount(score.getPlaycount());
                oldsd.setClearcount(score.getClearcount());
                oldsd.setSha256(score.getSha256());
                oldsd.setMode(score.getMode());
                oldsd.setNotes(score.getNotes());
            }
            oldsd.setScorehash(scorehash);
            if (oldsd.update(score)) {
                result.add(oldsd);
            }
        }
        
        scoredb.setScoreData(result.toArray(new IRScoreData[result.size()]));
		Logger.getGlobal().info("スコアインポート完了 - インポート数 : " + result.size());
    }
}
