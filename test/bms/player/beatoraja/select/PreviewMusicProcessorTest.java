package bms.player.beatoraja.select;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import bms.player.beatoraja.song.SongData;
import bms.player.beatoraja.song.SongResources;
import bms.player.beatoraja.song.archive.SongArchives;

/** Preview resolution tests using real local, ZIP and RAR4 resources, without audio hardware. */
public final class PreviewMusicProcessorTest {
    private PreviewMusicProcessorTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("beatoraja-preview-test-");
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            for (String name : new String[] {"chart.bms", "exact.wav", "exact.ogg", "ogg/preview.ogg",
                    "mp3/preview.mp3", "flac/preview.flac", "wav/preview.wav", "order/preview.flac",
                    "order/preview.ogg", "order/preview.mp3", "extensionless/preview.ogg",
                    "sounds.v1/preview space.ogg", "PREVIEW.OGG"}) {
                entries.put("set/" + name, name.getBytes(StandardCharsets.UTF_8));
            }
            for (var entry : entries.entrySet()) {
                Path path = root.resolve("local").resolve(entry.getKey());
                Files.createDirectories(path.getParent());
                Files.write(path, entry.getValue());
            }
            verify(root.resolve("local/set/chart.bms"), entries, false);
            Path zip = root.resolve("song.zip");
            writeZip(zip, entries);
            Path rar = root.resolve("song.rar");
            writeRar(rar, entries);
            for (Path archive : new Path[] {zip, rar}) {
                verify(SongArchives.virtualPath(archive, "set/chart.bms"), entries, true);
                verify(SongArchives.virtualPath(archive, "set/chart.bms", "set"), entries, true);
            }
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println("PreviewMusicProcessorTest passed (local/ZIP/RAR4, exact/fallback/missing, no extraction)");
    }

    private static void verify(Path chart, Map<String, byte[]> entries, boolean archived) throws Exception {
        assertPreview(chart, "exact.wav", "exact.wav", entries, archived);
        assertPreview(chart, "exact.ogg", "exact.ogg", entries, archived);
        assertPreview(chart, "ogg/preview.wav", "ogg/preview.ogg", entries, archived);
        assertPreview(chart, "mp3/preview.wav", "mp3/preview.mp3", entries, archived);
        assertPreview(chart, "flac/preview.wav", "flac/preview.flac", entries, archived);
        assertPreview(chart, "wav/preview.ogg", "wav/preview.wav", entries, archived);
        assertPreview(chart, "order/preview.wav", "order/preview.flac", entries, archived);
        assertPreview(chart, "extensionless/preview", "extensionless/preview.ogg", entries, archived);
        assertPreview(chart, "sounds.v1/preview space.wav", "sounds.v1/preview space.ogg", entries, archived);
        assertPreview(chart, "PREVIEW.OGG", "PREVIEW.OGG", entries, archived);
        for (String missing : new String[] {"missing.wav", "", null}) {
            SongData song = song(chart, missing);
            check(PreviewMusicProcessor.resolvePreviewResource(song) == null, "No preview: " + missing);
        }
        check(PreviewMusicProcessor.resolvePreviewResource(null) == null, "No selected song");
    }

    private static void assertPreview(Path chart, String requested, String expected,
            Map<String, byte[]> entries, boolean archived) throws Exception {
        long materialized = SongResources.getMaterializedStatistics().fileCount();
        SongData song = song(chart, requested);
        var resource = PreviewMusicProcessor.resolvePreviewResource(song);
        check(resource != null, "Preview missing: " + chart + " -> " + requested);
        var expectedResource = SongResources.fromPath(chart).parent().resolve(expected);
        check(resource.cacheKey().equals(expectedResource.cacheKey()), "Preview priority/path: " + requested);
        check(resource.localPath().isEmpty() == archived, "Preserve resource kind: " + requested);
        try (var input = resource.openStream()) {
            check(Arrays.equals(input.readAllBytes(), entries.get("set/" + expected)), "Preview bytes: " + requested);
        }
        check(materialized == SongResources.getMaterializedStatistics().fileCount(), "No materialization");
        check(requested.equals(song.getPreview()), "Do not modify song metadata");
    }

    private static SongData song(Path chart, String preview) {
        var song = new SongData();
        song.setPath(chart.toString());
        song.setPreview(preview);
        return song;
    }

    private static void writeZip(Path path, Map<String, byte[]> entries) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static void writeRar(Path path, Map<String, byte[]> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        out.write(new byte[] {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00});
        out.write(header(0x73, 0, 13).array());
        for (var entry : entries.entrySet()) {
            byte[] filename = entry.getKey().replace('/', '\\').getBytes(StandardCharsets.US_ASCII);
            byte[] data = entry.getValue();
            var block = header(0x74, 0x8000, 32 + filename.length);
            block.position(7);
            block.putInt(data.length).putInt(data.length).put((byte) 3);
            var crc = new CRC32();
            crc.update(data);
            block.putInt((int) crc.getValue()).putInt(0).put((byte) 20).put((byte) 0x30);
            block.putShort((short) filename.length).putInt(0100644).put(filename);
            updateHeaderCrc(block);
            out.write(block.array());
            out.write(data);
        }
        out.write(header(0x7b, 0x4000, 7).array());
        Files.write(path, out.toByteArray());
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
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
