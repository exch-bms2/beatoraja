package bms.player.beatoraja.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import bms.player.beatoraja.ScoreData;
import bms.player.beatoraja.ScoreDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongReview;
import bms.player.beatoraja.song.SongReviewAccessor;

/** Exercise Commons archive APIs and the application's JDBC bean mapping without user data. */
public final class CommonsCompatibilityTest {
    public static void main(String[] args) throws Exception {
        verifyTarGzip();
        verifyQueryRunner();
        verifySongReviews();
        System.out.println("CommonsCompatibilityTest passed");
    }

    private static void verifyTarGzip() throws Exception {
        String name = "long-directory/".repeat(10) + "\u697d\u66f2/chart.bms";
        byte[] content = "#TITLE \u697d\u66f2\n#BPM 150\n".repeat(200).getBytes(StandardCharsets.UTF_8);
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed);
                var tar = new TarArchiveOutputStream(gzip, "UTF-8")) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            var entry = new TarArchiveEntry(name);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        try (var gzip = new GzipCompressorInputStream(new ByteArrayInputStream(compressed.toByteArray()));
                var tar = new TarArchiveInputStream(gzip, "UTF-8")) {
            var entry = tar.getNextEntry();
            check(entry != null && entry.getName().equals(name), "TAR long/unicode entry name");
            check(entry.getSize() == content.length, "TAR entry size");
            var restored = new ByteArrayOutputStream();
            IOUtils.copy(tar, restored);
            check(Arrays.equals(content, restored.toByteArray()), "GZIP/TAR copy used by music downloads");
            check(tar.getNextEntry() == null, "TAR end of archive");
        }
    }

    private static void verifyQueryRunner() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            var runner = new QueryRunner();
            runner.update(connection, "CREATE TABLE review (sha256 TEXT, tag TEXT, favorite INTEGER, levelreview REAL, comment TEXT)");
            int[] counts = runner.batch(connection, "INSERT INTO review VALUES (?, ?, ?, ?, ?)", new Object[][] {
                    {"a", "\u65e5\u672c\u8a9e ' tag", SongReview.FAVORITE_CHART, 12.5, null},
                    {"b", null, null, null, "comment"}
            });
            check(counts.length == 2 && counts[0] == 1 && counts[1] == 1, "QueryRunner batch insert");
            var beans = runner.query(connection, "SELECT * FROM review ORDER BY sha256", new BeanListHandler<>(SongReview.class));
            check(beans.size() == 2, "BeanListHandler row count");
            check(beans.get(0).getTag().equals("\u65e5\u672c\u8a9e ' tag")
                    && beans.get(0).getFavorite() == SongReview.FAVORITE_CHART
                    && beans.get(0).getLevelreview() == 12.5 && beans.get(0).getComment() == null,
                    "Bean mapping of text, numbers and nullable reference");
            check(beans.get(1).getFavorite() == 0 && beans.get(1).getLevelreview() == 0,
                    "SQL NULL maps to primitive defaults");
            check(runner.update(connection, "UPDATE review SET comment = ? WHERE sha256 = ?", null, "b") == 1,
                    "Nullable prepared statement parameter");
            var maps = runner.query(connection, "SELECT sha256, favorite, comment FROM review WHERE sha256 = ?",
                    new MapListHandler(), "a");
            check(maps.size() == 1 && maps.get(0).get("sha256").equals("a")
                    && ((Number) maps.get(0).get("favorite")).intValue() == SongReview.FAVORITE_CHART,
                    "MapListHandler and prepared query");
            try {
                runner.update(connection, "INSERT INTO missing_table VALUES (?)", 1);
                throw new AssertionError("Expected SQL failure");
            } catch (SQLException expected) { }
            check(connection.isValid(1), "QueryRunner must not close caller-owned connection after failure");
        }
    }

    private static void verifySongReviews() throws Exception {
        Path root = Files.createTempDirectory("commons-dbutils-compatibility-");
        try {
            Path file = root.resolve("songreview.db");
            // Exercise the shared SQLiteDatabaseAccessor's schema migration as well as new writes.
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file)) {
                new QueryRunner().update(connection, "CREATE TABLE review (sha256 TEXT PRIMARY KEY NOT NULL, tag TEXT, favorite INTEGER)");
            }
            var accessor = new SongReviewAccessor(file.toString());
            var review = new SongReview();
            review.setSha256("a".repeat(64));
            review.setTag("\u697d\u66f2 ' tag");
            review.setFavorite(SongReview.FAVORITE_SONG | SongReview.FAVORITE_CHART);
            review.setLevelreview(12.75);
            review.setComment(null);
            accessor.setSongReview(review);
            var loaded = accessor.getSongReview(review.getSha256());
            check(loaded != null && loaded.getTag().equals(review.getTag())
                    && loaded.getFavorite() == review.getFavorite() && loaded.getLevelreview() == 12.75
                    && loaded.getComment() == null, "Application review write/read after schema migration");
            var song = new SongData();
            song.setSha256(review.getSha256());
            song.getSongReview().setTag("updated");
            song.getSongReview().setFavorite(SongReview.INVISIBLE_CHART);
            accessor.setSongReviews(new SongData[] {song});
            var reopened = new SongReviewAccessor(file.toString());
            loaded = reopened.getSongReviews().get(review.getSha256());
            check(loaded != null && loaded.getTag().equals("updated") && loaded.getFavorite() == SongReview.INVISIBLE_CHART
                    && loaded.getLevelreview() == 12.75, "Transactional review update/reopen retains other values");
            check(reopened.getSongReview("missing") == null, "Missing review");
            verifyScores(root.resolve("score.db"));
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void verifyScores(Path file) throws Exception {
        var accessor = new ScoreDatabaseAccessor(file.toString());
        accessor.createTable();
        check(accessor.getPlayerDatas(1).length == 1, "Player database initialization");
        var score = new ScoreData();
        score.setSha256("b".repeat(64));
        score.setNotes(1000);
        score.setEpg(500);
        score.setDate(4_000_000_000L);
        score.setSeed(8_000_000_000L);
        score.setAvgjudge(4_294_967_419L);
        score.setTrophy("gH");
        accessor.setScoreData(score);
        var loaded = accessor.getScoreData(score.getSha256(), score.getMode());
        check(loaded != null && loaded.getNotes() == 1000 && loaded.getEpg() == 500
                && loaded.getDate() == score.getDate() && loaded.getSeed() == score.getSeed()
                && loaded.getAvgjudge() == score.getAvgjudge() && loaded.getTrophy().equals("gH"),
                "Score persistence preserves long/integer/string properties");
        score.setEpg(600);
        accessor.setScoreData(score);
        var reopened = new ScoreDatabaseAccessor(file.toString());
        loaded = reopened.getScoreData(score.getSha256(), score.getMode());
        check(loaded != null && loaded.getEpg() == 600 && loaded.getAvgjudge() == score.getAvgjudge(),
                "Score replace/reopen");
        check(reopened.getScoreData("a".repeat(64), 0) == null, "Missing score");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
