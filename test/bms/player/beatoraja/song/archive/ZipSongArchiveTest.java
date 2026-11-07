package bms.player.beatoraja.song.archive;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** ZIP compatibility through both the JDK UTF-8 reader and Commons CP932 fallback. */
public final class ZipSongArchiveTest {
    private static final String DIRECTORY = "\u697d\u66f2/";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("zip-song-compatibility-");
        try {
            verifyArchive(root, "utf8", StandardCharsets.UTF_8);
            verifyArchive(root, "cp932", Charset.forName("MS932"));
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("ZipSongArchiveTest passed");
    }

    private static void verifyArchive(Path root, String label, Charset charset) throws Exception {
        var content = new LinkedHashMap<String, byte[]>();
        content.put(DIRECTORY + "\u8b5c\u9762.bms", "#TITLE ZIP compatibility\n#BPM 120\n".getBytes(StandardCharsets.UTF_8));
        content.put(DIRECTORY + "preview.wav", new byte[] {0, 1, 2, 3, 127, -128, -1});
        content.put(DIRECTORY + "stage.png", new byte[4096]);
        Path archive = root.resolve(label + ".zip");
        try (var output = new ZipOutputStream(Files.newOutputStream(archive), charset)) {
            output.putNextEntry(new ZipEntry(DIRECTORY));
            output.closeEntry();
            for (var file : content.entrySet()) {
                var entry = new ZipEntry(file.getKey());
                if (file.getKey().endsWith(".wav")) {
                    var crc = new CRC32();
                    crc.update(file.getValue());
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(file.getValue().length);
                    entry.setCrc(crc.getValue());
                }
                output.putNextEntry(entry);
                output.write(file.getValue());
                output.closeEntry();
            }
        }
        if (!charset.equals(StandardCharsets.UTF_8)) {
            expectIOException(() -> {
                try (var ignored = new ZipFile(archive.toFile())) { }
            });
        }
        var reader = new ZipSongArchive();
        check(reader.listEntries(archive).equals(List.copyOf(content.keySet())), label + " entry names/order");
        for (var file : content.entrySet()) {
            check(reader.entrySize(archive, file.getKey()) == file.getValue().length, label + " entry size");
            check(Arrays.equals(reader.readEntry(archive, file.getKey()), file.getValue()), label + " byte read");
            try (var input = reader.openEntry(archive, file.getKey())) {
                check(Arrays.equals(input.readAllBytes(), file.getValue()), label + " streaming read");
            }
        }
        for (String missing : List.of(DIRECTORY, DIRECTORY + "missing.wav")) {
            expectIOException(() -> reader.entrySize(archive, missing));
            expectIOException(() -> reader.readEntry(archive, missing));
            expectIOException(() -> {
                try (var ignored = reader.openEntry(archive, missing)) { }
            });
        }
        Path extracted = Files.createDirectory(root.resolve(label));
        reader.extract(archive, extracted);
        for (var file : content.entrySet()) {
            check(Arrays.equals(Files.readAllBytes(extracted.resolve(file.getKey())), file.getValue()),
                    label + " extracted content");
        }
        // A closed entry stream must not leave the archive file locked on Windows.
        Files.delete(Files.move(archive, root.resolve(label + "-closed.zip")));

        Path unsafe = root.resolve(label + "-unsafe.zip");
        try (var output = new ZipOutputStream(Files.newOutputStream(unsafe), charset)) {
            output.putNextEntry(new ZipEntry(DIRECTORY + "../../escaped.bms"));
            output.write(1);
            output.closeEntry();
        }
        expectIOException(() -> reader.listEntries(unsafe));
        expectIOException(() -> reader.extract(unsafe, extracted));
        check(!Files.exists(root.resolve("escaped.bms")), label + " extraction containment");
    }

    private static void expectIOException(IoAction action) throws IOException {
        try {
            action.run();
            throw new AssertionError("Expected IOException");
        } catch (IOException expected) { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
