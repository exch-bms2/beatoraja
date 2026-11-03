package bms.player.beatoraja.song.archive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import bms.player.beatoraja.song.SQLiteSongDatabaseAccessor;
import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongResources;
import bms.player.beatoraja.song.SongUtils;

/** Real ZIP/RAR4 scans must limit chart depth without restricting song assets. */
public final class SongArchiveDepthTest {
    private SongArchiveDepthTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyEntryDepth();
        Path root = Files.createTempDirectory("beatoraja-archive-depth-");
        try {
            for (String extension : List.of("ZIP", "RAR")) {
                for (boolean collapsed : new boolean[] {false, true}) {
                    verifyDatabase(Files.createDirectory(root.resolve(extension + "-" + collapsed)), extension, collapsed);
                }
            }
            verifyLocalDirectory(Files.createDirectory(root.resolve("local")));
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println("SongArchiveDepthTest passed (ZIP/RAR4 depth, collapsed roots, incremental cleanup, deep assets/local charts)");
    }

    private static void verifyEntryDepth() {
        for (String extension : List.of("bms", "BME", "bml", "PMS", "BMSON")) {
            check(SongArchives.isChartEntry("chart." + extension), "Root chart: " + extension);
            check(SongArchives.isChartEntry("song/chart." + extension), "One-level chart: " + extension);
            check(SongArchives.isChartEntry(".\\song\\chart." + extension), "Normalized separators: " + extension);
            check(!SongArchives.isChartEntry("pack/song/chart." + extension), "Deep chart: " + extension);
            check(!SongArchives.isChartEntry("pack\\song\\chart." + extension), "Deep Windows path: " + extension);
        }
        for (String name : List.of("song/readme.txt", "../chart.bms", "/chart.bms", "song//chart.bms")) {
            check(!SongArchives.isChartEntry(name), "Not a detected chart: " + name);
        }
    }

    private static void verifyDatabase(Path root, String extension, boolean collapsed) throws Exception {
        Path songs = Files.createDirectory(root.resolve("songs"));
        Path archive = songs.resolve("song_% ." + extension);
        String shallow = collapsed ? "set/chart.BMS" : "song/chart.BMS";
        String deep = collapsed ? "set/nested/ignored.bms" : "pack/song/ignored.bms";
        String assetRoot = collapsed ? "set/" : "song/";
        Map<String, byte[]> entries = new LinkedHashMap<>();
        if (!collapsed) entries.put("root.bms", chart("Root chart"));
        entries.put(shallow, chart("Shallow chart"));
        entries.put(deep, chart("Ignored chart"));
        for (String asset : List.of("assets/audio/key.ogg", "assets/image/banner.png", "assets/video/movie.mp4")) {
            entries.put(assetRoot + asset, asset.getBytes(StandardCharsets.UTF_8));
        }
        writeArchive(archive, entries);
        var contents = SongArchives.readContents(archive);
        check(collapsed ? "set".equals(contents.rootDirectory()) : contents.rootDirectory() == null,
                "Expected visible root");

        Path database = root.resolve("songdata.db");
        String[] roots = {songs.toString()};
        var db = new SQLiteSongDatabaseAccessor(database.toString(), roots, null,
                SQLiteSongDatabaseAccessor.SongUpdaterType.BATCHED, true);
        db.updateSongDatas(null, roots, true, null);
        Path chartPath = SongArchives.virtualPath(archive, shallow, contents.rootDirectory());
        check(db.getSongDatas("path", chartPath.toString()).length == 1, "Register shallow chart: " + archive);
        check(songCount(database) == (collapsed ? 1 : 2), "Only depth 0/1 charts registered: " + archive);

        long materialized = SongResources.getMaterializedStatistics().fileCount();
        var directory = SongResources.fromPath(chartPath).parent();
        for (String asset : List.of("assets/audio/key.ogg", "assets/image/banner.png", "assets/video/movie.mp4")) {
            try (var input = directory.resolve(asset).openStream()) {
                check(Arrays.equals(input.readAllBytes(), entries.get(assetRoot + asset)), "Read deep asset: " + asset);
            }
        }
        check(materialized == SongResources.getMaterializedStatistics().fileCount(), "No asset extraction");

        // Old versions registered these rows under the chart's actual parent, not the virtual archive root.
        Path deepPath = SongArchives.virtualPath(archive, deep, contents.rootDirectory());
        var stale = new SongData();
        stale.setTitle("Old deep chart");
        stale.setSha256("a".repeat(64));
        stale.setMd5("b".repeat(32));
        stale.setPath(deepPath.toString());
        stale.setFolder(SongUtils.crc32(deepPath.getParent().toString(), roots, Path.of(".").toString()));
        db.setSongDatas(new SongData[] {stale});
        check(songCount(database) == (collapsed ? 2 : 3), "Seed old deep record");
        List<Path> unrelated = List.of(
                SongArchives.virtualPath(songs.resolve("song_AX ." + extension), "other.bms"),
                SongArchives.virtualPath(songs.resolve("song_% ." + extension.toLowerCase(java.util.Locale.ROOT)), "other.bms"));
        for (Path path : unrelated) {
            var other = new SongData();
            other.setTitle("Unrelated archive");
            other.setSha256("c".repeat(64));
            other.setMd5("d".repeat(32));
            other.setPath(path.toString());
            db.setSongDatas(new SongData[] {other});
        }
        db.updateSongDatas(null, roots, false, null);
        check(db.getSongDatas("path", deepPath.toString()).length == 0, "Remove old deep record");
        for (Path path : unrelated) {
            check(db.getSongDatas("path", path.toString()).length == 1, "Preserve distinct archive prefix: " + path);
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                var statement = connection.prepareStatement("DELETE FROM song WHERE path = ?")) {
            for (Path path : unrelated) {
                statement.setString(1, path.toString());
                statement.executeUpdate();
            }
        }
        check(songCount(database) == (collapsed ? 1 : 2), "Retain eligible charts on incremental update");

        writeArchive(archive, Map.of("pack/song/deep.bms", chart("Only deep chart")));
        db.updateSongDatas(null, roots, false, null);
        check(songCount(database) == 0, "Remove archive records when all charts exceed depth limit");
        try {
            SongArchives.expandToSongDirectory(archive);
            throw new AssertionError("Deep-only archives must not be automatically expanded");
        } catch (IOException expected) {
            check(expected.getMessage().contains("within one directory level"), "Depth rejection message");
        }
        check(Files.exists(archive) && !Files.exists(songs.resolve("pack"))
                && !Files.exists(songs.resolve(".beatoraja-archives")), "Rejected archive remains untouched");
    }

    private static void verifyLocalDirectory(Path root) throws Exception {
        Path songs = Files.createDirectory(root.resolve("songs"));
        Path chart = songs.resolve("pack/song/deep/chart.bms");
        Files.createDirectories(chart.getParent());
        Files.write(chart, chart("Local deep chart"));
        Path database = root.resolve("songdata.db");
        String[] roots = {songs.toString()};
        var db = new SQLiteSongDatabaseAccessor(database.toString(), roots, null,
                SQLiteSongDatabaseAccessor.SongUpdaterType.BATCHED, true);
        db.updateSongDatas(null, roots, true, null);
        check(db.getSongDatas("path", chart.toString()).length == 1, "Local chart depth remains unlimited");
    }

    private static byte[] chart(String title) {
        return ("#PLAYER 1\n#TITLE " + title + "\n#ARTIST Test\n#BPM 120\n#PLAYLEVEL 1\n#TOTAL 100\n"
                + "#WAV01 assets/audio/key.ogg\n#00111:01\n").getBytes(StandardCharsets.UTF_8);
    }

    private static int songCount(Path database) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM song")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void writeArchive(Path archive, Map<String, byte[]> entries) throws IOException {
        if (archive.toString().endsWith(".ZIP")) {
            try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                for (var entry : entries.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            return;
        }
        var out = new ByteArrayOutputStream();
        out.write(new byte[] {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00});
        out.write(header(0x73, 0, 13).array());
        for (var entry : entries.entrySet()) {
            byte[] name = entry.getKey().replace('/', '\\').getBytes(StandardCharsets.US_ASCII);
            byte[] data = entry.getValue();
            var block = header(0x74, 0x8000, 32 + name.length);
            var crc = new CRC32();
            crc.update(data);
            block.position(7);
            block.putInt(data.length).putInt(data.length).put((byte) 3).putInt((int) crc.getValue());
            block.putInt(0).put((byte) 20).put((byte) 0x30).putShort((short) name.length).putInt(0100644).put(name);
            updateHeaderCrc(block);
            out.write(block.array());
            out.write(data);
        }
        out.write(header(0x7b, 0x4000, 7).array());
        Files.write(archive, out.toByteArray());
    }

    private static ByteBuffer header(int type, int flags, int size) {
        var block = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        block.position(2);
        block.put((byte) type).putShort((short) flags).putShort((short) size);
        updateHeaderCrc(block);
        return block;
    }

    private static void updateHeaderCrc(ByteBuffer block) {
        var crc = new CRC32();
        crc.update(block.array(), 2, block.capacity() - 2);
        block.putShort(0, (short) crc.getValue());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
