package bms.player.beatoraja.launcher;

import bms.player.beatoraja.song.SongData;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.sql.SQLException;
import java.util.logging.Logger;

/** The editor can query charts without migrating or updating either game database. */
final class TableChartLookup {
    private final QueryRunner query;
    private final BeanListHandler<SongData> handler = new BeanListHandler<>(SongData.class);

    TableChartLookup(String path) {
        var config = new SQLiteConfig();
        config.setReadOnly(true);
        config.setBusyTimeout(250);
        var source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + path);
        query = new QueryRunner(source);
    }

    SongData[] byHash(String hash) {
        if (hash == null || !TableEditorView.isMd5OrSha256Hash(hash)) return SongData.EMPTY;
        return find(hash.length() == 32 ? "SELECT * FROM song WHERE md5 = ?" : "SELECT * FROM song WHERE sha256 = ?", hash);
    }

    SongData[] byText(String text) {
        return find("SELECT * FROM song WHERE rtrim(title||' '||subtitle||' '||artist||' '||subartist||' '||genre) LIKE ?"
                + " GROUP BY sha256", "%" + text + "%");
    }

    private SongData[] find(String sql, String parameter) {
        try {
            return query.query(sql, handler, parameter).toArray(SongData[]::new);
        } catch (SQLException e) {
            Logger.getGlobal().warning("Table editor chart search failed: " + e.getClass().getSimpleName());
            return SongData.EMPTY;
        }
    }
}
