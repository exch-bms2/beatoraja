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
                    IRScoreData oldsd = scoredb.getScoreData(sd.getSha256(), 0);
                    if(oldsd == null) {
                        oldsd = new IRScoreData();
                        oldsd.setPlaycount((int) score.get("playcount"));
                        oldsd.setClearcount((int) score.get("clearcount"));
                    }
                    oldsd.setSha256(song[0].getSha256());
                    oldsd.setNotes(song[0].getNotes());
                    oldsd.setScorehash("LR2");
                    if (oldsd.update(sd)) {
                        result.add(oldsd);
                    }
                }
            }
            scoredb.setScoreData(result.toArray(new IRScoreData[result.size()]));
        } catch (Exception e) {
            Logger.getGlobal().severe("スコア移行時の例外:" + e.getMessage());
        }
    }

    private void update(IRScoreData[] scores) {
        List<IRScoreData> result = new ArrayList<IRScoreData>();

        for(IRScoreData sd : scores) {
            IRScoreData oldsd = scoredb.getScoreData(sd.getSha256(), 0);
            sd.setScorehash("LR2");
            if (oldsd == null || oldsd.getClear() <= sd.getClear()) {
                result.add(sd);
            }
        }

    }
}
