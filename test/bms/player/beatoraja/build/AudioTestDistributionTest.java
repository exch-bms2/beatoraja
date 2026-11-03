package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Verify optional fixture contents, including stale and incomplete distributions. */
public final class AudioTestDistributionTest {
    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("audio-distribution-");
        try {
            Path install = Files.createDirectory(temporary.resolve("install"));
            Path archive = temporary.resolve("distribution.zip");
            zip(install, archive);
            verify(install, archive, false);
            expectFailure(() -> verify(install, archive, true));

            Path audio = Files.createDirectory(install.resolve("audio-test"));
            Files.writeString(audio.resolve("README.txt"), "fixture");
            Files.writeString(audio.resolve("measurements.csv"), "fixture");
            StringBuilder manifest = new StringBuilder();
            for (int rate : new int[] {44100, 48000}) {
                Path folder = Files.createDirectory(audio.resolve(Integer.toString(rate)));
                for (String name : List.of("timeline.wav", "silence.wav", "trigger.wav", "timeline.bms", "trigger.bms", "markers.csv")) {
                    byte[] bytes = (rate + "/" + name).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                    Files.write(folder.resolve(name), bytes);
                    manifest.append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
                            .append("  ").append(rate).append('/').append(name).append('\n');
                }
            }
            Files.writeString(audio.resolve("SHA256SUMS"), manifest);
            expectFailure(() -> verify(install, archive, false));
            expectFailure(() -> verify(install, archive, true));
            zip(install, archive);
            verify(install, archive, true);

            Path metadata = audio.resolve("README.txt");
            Files.delete(metadata);
            expectFailure(() -> verify(install, archive, true));
            Files.writeString(metadata, "fixture");
            Files.writeString(audio.resolve("44100/timeline.wav"), "corrupt");
            expectFailure(() -> verify(install, archive, true));
            Files.writeString(audio.resolve("44100/timeline.wav"), "44100/timeline.wav");
            verify(install, archive, true);

            Path empty = Files.createDirectory(temporary.resolve("empty"));
            expectFailure(() -> verify(empty, archive, false));
            Files.writeString(metadata, "ZIP mismatch");
            expectFailure(() -> verify(install, archive, true));
            System.out.println("AudioTestDistributionTest passed (opt-in, stale data, required metadata, both rates, hashes and ZIP contents)");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void verify(Path install, Path archive, boolean included) throws Exception {
        try (var zip = new ZipFile(archive.toFile())) {
            DistributionSmokeTest.verifyAudioTestAssets(install, zip, included);
        }
    }

    private static void zip(Path root, Path archive) throws Exception {
        try (var output = new ZipOutputStream(Files.newOutputStream(archive)); var files = Files.walk(root)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(new ZipEntry("beatoraja/"
                        + root.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
    }

    private static void expectFailure(Check check) throws Exception {
        try {
            check.run();
        } catch (AssertionError expected) {
            return;
        }
        throw new AssertionError("Invalid audio fixture distribution was accepted");
    }

    @FunctionalInterface
    private interface Check { void run() throws Exception; }
}
