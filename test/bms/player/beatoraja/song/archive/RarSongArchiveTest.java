package bms.player.beatoraja.song.archive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import com.github.junrar.Archive;
import com.github.junrar.exception.UnsupportedRarV5Exception;

/** Exercise real RAR4 I/O without external tools, downloads or player files. */
public final class RarSongArchiveTest {
    // Unmodified Junrar v7.6.0 fixtures; provenance and license are recorded in fixtures/.
    private static final String COMPRESSED =
            "UmFyIRoHAM+QcwAADQAAAAAAAAB8zXQgkC0ADQAAAAQAAAAD4Tl7zCeTJEEdMwsAtIEAAGZvb1xiYXIudHh0AMAACL8IrvLDGH6f/ZLdiiN04IAjAAAAAAAAAAAAAwAAAAAnkyRBFDADAP1BAABmb2/EPXsAQAcA";
    private static final String SOLID =
            "UmFyIRoHADvQcwgADQAAAAAAAACylHSAkCsAGgAAAAYAAAADBPcp4veq81AdMwkApIEAAGZpbGUxLnR4dADADQwM/hAMt2G79EFqVSh/2gEYP7Diz78doSBa4XSQkCsAAwAAAAYAAAADx6QEyfeq81AdMwkApIEAAGZpbGUyLnR4dADAepIAyjN0kJArAAMAAAAGAAAAA4aVH9D3qvNQHTMJAKSBAABmaWxlMy50eHQAwHsSAPkEdJCQKwADAAAABgAAAANBA16f96rzUB0zCQCkgQAAZmlsZTQudHh0AMB7kgBp1nSQkCsAAwAAAAYAAAADADJFhveq81AdMwkApIEAAGZpbGU1LnR4dADAfBIAmKd0kJArAAMAAAAGAAAAA8NhaK33qvNQHTMJAKSBAABmaWxlNi50eHQAwHySAAh1dJCQKwADAAAABgAAAAOCUHO096rzUB0zCQCkgQAAZmlsZTcudHh0AMB9EgD+yXSQkCsAAwAAAAYAAAADTUzrM/eq81AdMwkApIEAAGZpbGU4LnR4dADAfZIAbht0kJArAAMAAAAGAAAAAwx98Cr3qvNQHTMJAKSBAABmaWxlOS50eHQAwH4SgMQ9ewBABwA=";
    private static final String RAR5 =
            "UmFyIRoHAQDz4YLrCwEFBwAGAQGAgIAATS800SUCAwuHAASHACC6fRl6gAAACUZJTEUxLlRYVAoDAgDwWYPlessBZmlsZTENCqOo3u8lAgMLhwAEhwAg48NfeIAAAAlGSUxFMi5UWFQKAwIAd+2G5XrLAWZpbGUyDQodd1ZRAwUEAA==";

    private RarSongArchiveTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("beatoraja-rar-test-");
        try {
            Path archive = root.resolve("song.RAR");
            byte[] chart = "#TITLE RAR test\n#BPM 120\n#PLAYER 1\n#00111:0100\n".getBytes(StandardCharsets.UTF_8);
            byte[] text = "second entry\n".getBytes(StandardCharsets.UTF_8);
            Files.write(archive, storedRar(chart, text));
            var rar = new RarSongArchive();
            check(rar.supports(archive), "RAR extension detection");
            check(rar.listEntries(archive).equals(List.of("set/chart.bms", "set/readme.txt")), "RAR entry list");
            check(SongArchives.listEntries(archive).equals(rar.listEntries(archive)), "SongArchives RAR routing");
            check(rar.entrySize(archive, "set/chart.bms") == chart.length, "RAR entry size");
            check(Arrays.equals(chart, rar.readEntry(archive, "set/chart.bms")), "RAR chart bytes");
            Path virtual = SongArchives.virtualPath(archive, "set/chart.bms");
            check(Arrays.equals(chart, SongArchives.readEntry(virtual)), "Virtual RAR chart reading");
            try (var in = rar.openEntry(archive, "set/readme.txt")) {
                check(Arrays.equals(text, in.readAllBytes()), "RAR stream bytes");
            }
            Path extracted = Files.createDirectory(root.resolve("extracted"));
            rar.extract(archive, extracted);
            check(Arrays.equals(chart, Files.readAllBytes(extracted.resolve("set/chart.bms"))), "Extracted chart");
            check(Arrays.equals(text, Files.readAllBytes(extracted.resolve("set/readme.txt"))), "Extracted second entry");
            expectIOException(() -> rar.entrySize(archive, "missing.bms"));
            expectIOException(() -> rar.readEntry(archive, "missing.bms"));
            expectIOException(() -> {
                try (var ignored = rar.openEntry(archive, "missing.bms")) {
                    throw new AssertionError("Missing entry opened");
                }
            });
            verifyCompressed(root, "compressed.rar", COMPRESSED, Map.of("foo/bar.txt", "baz\n"), false);
            Map<String, String> solidFiles = new LinkedHashMap<>();
            for (int i = 1; i <= 9; i++) solidFiles.put("file" + i + ".txt", "file" + i + "\n");
            verifyCompressed(root, "solid.rar", SOLID, solidFiles, true);
            verifyRar5Rejected(root);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("RarSongArchiveTest passed (stored/compressed/solid RAR4, reverse reads, extraction, RAR5 rejection)");
    }

    private static void verifyCompressed(Path root, String filename, String fixture,
            Map<String, String> expected, boolean solid) throws Exception {
        Path archive = root.resolve(filename);
        Files.write(archive, Base64.getDecoder().decode(fixture));
        try (var metadata = new Archive(archive.toFile())) {
            check(metadata.getMainHeader().isSolid() == solid, "Fixture solid flag: " + filename);
            check(metadata.getFileHeaders().stream().filter(h -> !h.isDirectory())
                    .allMatch(h -> h.getUnpMethod() != 0x30), "Fixture must be compressed: " + filename);
        }
        var rar = new RarSongArchive();
        var names = new ArrayList<>(expected.keySet());
        check(rar.listEntries(archive).equals(names), "Compressed entry list: " + filename);
        check(SongArchives.listEntries(archive).equals(names), "Compressed RAR routing");
        Collections.reverse(names);
        for (String name : names) {
            byte[] bytes = expected.get(name).getBytes(StandardCharsets.UTF_8);
            check(rar.entrySize(archive, name) == bytes.length, "Compressed entry size: " + name);
            check(Arrays.equals(bytes, rar.readEntry(archive, name)), "Reverse readEntry: " + name);
            try (var in = rar.openEntry(archive, name)) {
                check(Arrays.equals(bytes, in.readAllBytes()), "Reverse openEntry: " + name);
            }
            check(Arrays.equals(bytes, SongArchives.readEntry(SongArchives.virtualPath(archive, name))),
                    "Compressed virtual path: " + name);
        }
        Path extracted = Files.createDirectory(root.resolve(filename + "-extracted"));
        rar.extract(archive, extracted);
        for (var entry : expected.entrySet()) {
            check(Files.readString(extracted.resolve(entry.getKey())).equals(entry.getValue()),
                    "Compressed extraction: " + entry.getKey());
        }
    }

    private static void verifyRar5Rejected(Path root) throws Exception {
        Path archive = root.resolve("rar5.rar");
        Files.write(archive, Base64.getDecoder().decode(RAR5));
        var rar = new RarSongArchive();
        expectUnsupportedRar5(() -> rar.listEntries(archive));
        expectUnsupportedRar5(() -> rar.entrySize(archive, "FILE1.TXT"));
        expectUnsupportedRar5(() -> rar.readEntry(archive, "FILE1.TXT"));
        expectUnsupportedRar5(() -> {
            try (var ignored = rar.openEntry(archive, "FILE1.TXT")) {
                throw new AssertionError("RAR5 stream opened");
            }
        });
        Path extracted = Files.createDirectory(root.resolve("rar5-extracted"));
        expectUnsupportedRar5(() -> rar.extract(archive, extracted));
    }

    private static void expectUnsupportedRar5(IoAction action) throws Exception {
        try {
            action.run();
            throw new AssertionError("Junrar 7.6.0 must reject RAR5");
        } catch (IOException expected) {
            check(expected.getCause() instanceof UnsupportedRarV5Exception, "RAR5 rejection cause");
        }
    }

    private static byte[] storedRar(byte[] chart, byte[] text) throws IOException {
        var out = new ByteArrayOutputStream();
        out.write(new byte[] {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00});
        out.write(header(0x73, 0, 13).array());
        addFile(out, "set\\chart.bms", chart);
        addFile(out, "set\\readme.txt", text);
        out.write(header(0x7b, 0x4000, 7).array());
        return out.toByteArray();
    }

    private static void addFile(ByteArrayOutputStream out, String name, byte[] data) throws IOException {
        byte[] filename = name.getBytes(StandardCharsets.US_ASCII);
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

    // RAR4 header CRC is the low 16 bits of CRC32, excluding the CRC field itself.
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

    private static void expectIOException(IoAction action) throws Exception {
        try {
            action.run();
            throw new AssertionError("Expected IOException for missing entry");
        } catch (IOException expected) {
            check(expected.getMessage().contains("does not exist"), "Unexpected missing entry failure");
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
