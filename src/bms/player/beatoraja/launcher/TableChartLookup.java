package bms.player.beatoraja.launcher;

import bms.player.beatoraja.song.SongData;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    static String chartHash(SongData song) {
        String sha256 = song.getSha256();
        if (sha256 != null && !sha256.isEmpty()) return normalizedHash(sha256, 64);
        return normalizedHash(song.getMd5(), 32);
    }

    private static String normalizedHash(String hash, int length) {
        return hash != null && hash.length() == length && TableEditorView.isMd5OrSha256Hash(hash)
                ? hash.toLowerCase(Locale.ROOT) : null;
    }

    Map<String, SongData> byHashes(Set<String> hashes) {
        var found = new HashMap<String, SongData>();
        for (int length : new int[]{64, 32}) {
            List<String> keys = hashes.stream().filter(hash -> hash.length() == length).toList();
            String column = length == 64 ? "sha256" : "md5";
            for (int start = 0; start < keys.size(); start += 500) {
                if (Thread.currentThread().isInterrupted()) return found;
                var batch = keys.subList(start, Math.min(start + 500, keys.size()));
                var placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
                for (var song : find("SELECT * FROM song WHERE " + column + " IN (" + placeholders + ")", batch.toArray())) {
                    var key = normalizedHash(length == 64 ? song.getSha256() : song.getMd5(), length);
                    if (key != null) found.putIfAbsent(key, song);
                }
            }
        }
        return found;
    }

    private SongData[] find(String sql, Object... parameters) {
        try {
            return query.query(sql, handler, parameters).toArray(SongData[]::new);
        } catch (SQLException e) {
            Logger.getGlobal().warning("Table editor chart search failed: " + e.getClass().getSimpleName());
            return SongData.EMPTY;
        }
    }
}
