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

/** Exercise real RAR4/RAR5 I/O without external tools, downloads or player files. */
public final class RarSongArchiveTest {
    // Unmodified Junrar v7.6.0 fixtures; provenance and license are recorded in fixtures/.
    private static final String COMPRESSED =
            "UmFyIRoHAM+QcwAADQAAAAAAAAB8zXQgkC0ADQAAAAQAAAAD4Tl7zCeTJEEdMwsAtIEAAGZvb1xiYXIudHh0AMAACL8IrvLDGH6f/ZLdiiN04IAjAAAAAAAAAAAAAwAAAAAnkyRBFDADAP1BAABmb2/EPXsAQAcA";
    private static final String SOLID =
            "UmFyIRoHADvQcwgADQAAAAAAAACylHSAkCsAGgAAAAYAAAADBPcp4veq81AdMwkApIEAAGZpbGUxLnR4dADADQwM/hAMt2G79EFqVSh/2gEYP7Diz78doSBa4XSQkCsAAwAAAAYAAAADx6QEyfeq81AdMwkApIEAAGZpbGUyLnR4dADAepIAyjN0kJArAAMAAAAGAAAAA4aVH9D3qvNQHTMJAKSBAABmaWxlMy50eHQAwHsSAPkEdJCQKwADAAAABgAAAANBA16f96rzUB0zCQCkgQAAZmlsZTQudHh0AMB7kgBp1nSQkCsAAwAAAAYAAAADADJFhveq81AdMwkApIEAAGZpbGU1LnR4dADAfBIAmKd0kJArAAMAAAAGAAAAA8NhaK33qvNQHTMJAKSBAABmaWxlNi50eHQAwHySAAh1dJCQKwADAAAABgAAAAOCUHO096rzUB0zCQCkgQAAZmlsZTcudHh0AMB9EgD+yXSQkCsAAwAAAAYAAAADTUzrM/eq81AdMwkApIEAAGZpbGU4LnR4dADAfZIAbht0kJArAAMAAAAGAAAAAwx98Cr3qvNQHTMJAKSBAABmaWxlOS50eHQAwH4SgMQ9ewBABwA=";
    private static final String RAR5 =
            "UmFyIRoHAQDz4YLrCwEFBwAGAQGAgIAATS800SUCAwuHAASHACC6fRl6gAAACUZJTEUxLlRYVAoDAgDwWYPlessBZmlsZTENCqOo3u8lAgMLhwAEhwAg48NfeIAAAAlGSUxFMi5UWFQKAwIAd+2G5XrLAWZpbGUyDQodd1ZRAwUEAA==";
    // Junrar v8.1.2 fixtures; pinned source paths and hashes are recorded in fixtures/.
    private static final String RAR5_SOLID =
            "UmFyIRoHAQAJ78hvCwEFBwQGAQGAgIAA5PLueR8CApsABoYApIMCY0kUXwT3KeKAGwEJZmlsZTEudHh0wYMYNDAz+EAy3Ybv0QWJRKH8DMPdhxsia/YACAfgmR8CAoUABoYApIMCY0kUX8ekBMnAGwEJZmlsZTIudHh0QhoCe4DgrmJzHwIChQAGhgCkgwJjSRRfhpUf0MAbAQlmaWxlMy50eHRCGgJ8ALv5fIsfAgKFAAaGAKSDAmNJFF9BA16fwBsBCWZpbGU0LnR4dEIaAnyAU1D+YR8CAoUABoYApIMCY0kUXwAyRYbAGwEJZmlsZTUudHh0QhoCfQAqrAiFHwIChQAGhgCkgwJjSRRfw2ForcAbAQlmaWxlNi50eHRCGgJ9gMIFim8fAgKFAAaGAKSDAmNJFF+CUHO0wBsBCWZpbGU3LnR4dEIaAn4A3QRFrh8CAoUABoYApIMCY0kUX01M6zPAGwEJZmlsZTgudHh0QhoCfoA1rcdEHwIChQAGhgCkgwJjSRRfDH3wKsAbAQlmaWxlOS50eHRCGgJ/AB13VlEDBQQA";
    private static final String RAR5_PASSWORD =
            "UmFyIRoHAQAzkrXlCgEFBgAFAQGAgABfIc6zUQIDMaAABoYApIMCY0kUXxnn0+uAAwEJZmlsZTEudHh0MAEAAw+rgP7jvy7bm3QlOdS3YZwFYG45NDcQ1tsYPE/SbYZEVJmc4Qhprj8yi5S4gj56t2Cqe0X/M28NnKG0ccGCC/yKLzwOLmAr44eAnrJUHXdWUQMFBAA=";

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
            verifyCompressed(root, "rar5-solid.rar", RAR5_SOLID, solidFiles, true);
            Path rar5Stored = root.resolve("rar5.rar");
            Files.write(rar5Stored, Base64.getDecoder().decode(RAR5));
            verifyEntries(rar5Stored, Map.of("FILE1.TXT", "file1\r\n", "FILE2.TXT", "file2\r\n"));
            Path password = root.resolve("rar5-password.rar");
            Files.write(password, Base64.getDecoder().decode(RAR5_PASSWORD));
            verifyRejectedArchive(password, "file1.txt", "Password protected");
            verifyRar5Guards(root);
            verifyCorruptData(root, chart, text);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("RarSongArchiveTest passed (RAR4/RAR5, reverse reads, extraction, corrupt data and RAR5 resource/link/split guards)");
    }

    private static void verifyCompressed(Path root, String filename, String fixture,
            Map<String, String> expected, boolean solid) throws Exception {
        Path archive = root.resolve(filename);
        Files.write(archive, Base64.getDecoder().decode(fixture));
        try (var metadata = new Archive(archive.toFile())) {
            boolean actualSolid = metadata.getMainHeader() != null ? metadata.getMainHeader().isSolid()
                    : metadata.getFileHeaders().stream().anyMatch(h -> h.isSolid());
            check(actualSolid == solid, "Fixture solid flag: " + filename);
            check(metadata.getFileHeaders().stream().filter(h -> !h.isDirectory())
                    .allMatch(h -> h.getUnpMethod() != (h.isRar5Container() ? 0 : 0x30)),
                    "Fixture must be compressed: " + filename);
        }
        verifyEntries(archive, expected);
    }

    private static void verifyEntries(Path archive, Map<String, String> expected) throws Exception {
        String filename = archive.getFileName().toString();
        var rar = new RarSongArchive();
        var names = new ArrayList<>(expected.keySet());
        var actual = rar.listEntries(archive);
        check(actual.size() == names.size() && actual.containsAll(names), "Entry list: " + filename);
        check(SongArchives.listEntries(archive).equals(actual), "RAR routing");
        names = new ArrayList<>(actual);
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
        Path extracted = Files.createDirectory(archive.resolveSibling(filename + "-extracted"));
        rar.extract(archive, extracted);
        for (var entry : expected.entrySet()) {
            check(Files.readString(extracted.resolve(entry.getKey())).equals(entry.getValue()),
                    "Compressed extraction: " + entry.getKey());
        }
    }

    private static void verifyRar5Guards(Path root) throws Exception {
        String name = "set/\u697d\u66f2.bms";
        Path stored = root.resolve("rar5-stored.rar");
        Files.write(stored, storedRar5(name, "#TITLE RAR5\n".getBytes(StandardCharsets.UTF_8), 0, 0, false));
        verifyEntries(stored, Map.of(name, "#TITLE RAR5\n"));
        Path largeDictionary = root.resolve("rar5-large-dictionary.rar");
        Files.write(largeDictionary, storedRar5(name, new byte[0], 12L << 10, 0, false));
        verifyRejectedArchive(largeDictionary, name, "dictionary is too large");
        Path link = root.resolve("rar5-link.rar");
        Files.write(link, storedRar5(name, new byte[0], 0, 0, true));
        verifyRejectedArchive(link, name, "links are not supported");
        Path split = root.resolve("rar5-split.rar");
        Files.write(split, storedRar5(name, new byte[0], 0, 8, false));
        verifyRejectedArchive(split, name, "Split RAR archives are not supported");
    }

    private static void verifyRejectedArchive(Path archive, String name, String message) throws Exception {
        var rar = new RarSongArchive();
        expectFailure(() -> rar.listEntries(archive), message);
        expectFailure(() -> rar.entrySize(archive, name), message);
        expectFailure(() -> rar.readEntry(archive, name), message);
        expectFailure(() -> {
            try (var ignored = rar.openEntry(archive, name)) {
                throw new AssertionError("Rejected RAR stream opened");
            }
        }, message);
        Path extracted = Files.createDirectory(archive.resolveSibling(archive.getFileName() + "-rejected"));
        expectFailure(() -> rar.extract(archive, extracted), message);
        try (var files = Files.list(extracted)) {
            check(files.findAny().isEmpty(), "Rejected archive must not write files");
        }
    }

    private static void expectFailure(IoAction action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError("Expected RAR failure: " + message);
        } catch (IOException expected) {
            check(expected.getMessage().contains(message), "Unexpected RAR failure: " + expected);
        }
    }

    private static void verifyCorruptData(Path root, byte[] chart, byte[] text) throws Exception {
        byte[] data = storedRar(chart, text);
        // First payload follows the signature, main header and first file header.
        int fileHeader = 7 + 13;
        int payload = fileHeader + 32 + "set\\chart.bms".length();
        data[payload] ^= 1;
        Path corrupt = root.resolve("corrupt-data.rar");
        Files.write(corrupt, data);
        var rar = new RarSongArchive();
        for (boolean stream : new boolean[] {false, true}) {
            try {
                if (stream) {
                    try (var input = rar.openEntry(corrupt, "set/chart.bms")) { input.readAllBytes(); }
                } else {
                    rar.readEntry(corrupt, "set/chart.bms");
                }
                throw new AssertionError("Corrupt RAR data must not become a successful read/EOF");
            } catch (IOException expected) {
                check(expected.getCause() != null, "Preserve RAR CRC failure cause");
            }
        }
        data = storedRar(chart, text);
        data[fileHeader] ^= 1;
        Path badHeader = root.resolve("corrupt-header.rar");
        Files.write(badHeader, data);
        verifyRejectedArchive(badHeader, "set/chart.bms", "broken headers");
    }

    private static byte[] storedRar5(String name, byte[] data, long compression, int flags, boolean link) throws IOException {
        var archive = new ByteArrayOutputStream();
        archive.write(new byte[] {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x01, 0x00});
        rar5Block(archive, new byte[] {1, 0, 0});
        var extra = new ByteArrayOutputStream();
        if (link) {
            var record = new ByteArrayOutputStream();
            byte[] target = "../../outside".getBytes(StandardCharsets.UTF_8);
            vint(record, 5); // REDIR extra record, Unix symlink, no flags.
            vint(record, 1);
            vint(record, 0);
            vint(record, target.length);
            record.write(target);
            vint(extra, record.size());
            record.writeTo(extra);
        }
        var file = new ByteArrayOutputStream();
        vint(file, 2); // FILE block, data area, optional extra area.
        vint(file, 2 | flags | (link ? 1 : 0));
        if (link) vint(file, extra.size());
        vint(file, data.length);
        vint(file, 4); // File CRC present.
        vint(file, data.length);
        vint(file, 0100644);
        var crc = new CRC32();
        crc.update(data);
        file.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue()).array());
        vint(file, compression);
        vint(file, 1); // Unix host, UTF-8 filename.
        byte[] filename = name.getBytes(StandardCharsets.UTF_8);
        vint(file, filename.length);
        file.write(filename);
        extra.writeTo(file);
        rar5Block(archive, file.toByteArray());
        archive.write(data);
        rar5Block(archive, new byte[] {5, 0, 0});
        return archive.toByteArray();
    }

    private static void rar5Block(ByteArrayOutputStream archive, byte[] body) throws IOException {
        var header = new ByteArrayOutputStream();
        vint(header, body.length);
        header.write(body);
        var crc = new CRC32();
        crc.update(header.toByteArray());
        archive.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) crc.getValue()).array());
        header.writeTo(archive);
    }

    private static void vint(ByteArrayOutputStream output, long value) {
        do {
            int next = (int) (value & 0x7f);
            value >>>= 7;
            output.write(next | (value == 0 ? 0 : 0x80));
        } while (value != 0);
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
