package bms.player.beatoraja.build;

import bms.player.beatoraja.ir.IRWorkerSecurityTest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/** Real Fat JAR, real worker processes and native probe; no user configuration or network. */
public final class FatJarSmokeTest {
    public static void main(String[] args) throws Exception {
        Path archive = Path.of(args[0]);
        verify(archive, Arrays.stream(args).skip(4).map(Path::of).toList());
        Path output = Files.createDirectories(Path.of(args[3]));
        Path run = Files.createTempDirectory(output, "fat jar with spaces-");
        Path root = PackagedGameSupport.extract(Path.of(args[1]), run.resolve("extracted"));
        Path fat = PackagedGameSupport.useFatJar(root, archive);
        Path log = run.resolve("native-probe.log");
        Process child = new ProcessBuilder(args[2], "-Dbeatoraja.distributionDiagnostics=true",
                "-Djava.library.path=./natives", "-jar", root.relativize(fat).toString(), "--runtime-smoke")
                .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            check(child.waitFor(90, TimeUnit.SECONDS), "Fat JAR native probe timed out: " + log);
            String text = Files.readString(log);
            System.out.print(text);
            check(child.exitValue() == 0 && text.contains("Native probe failures=0"), "Fat JAR native probe failed: " + log);
            check(!Files.exists(root.resolve("config_sys.json")) && !Files.exists(root.resolve("player")), "Probe touched player data");
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); }
        }
        IRWorkerSecurityTest.verifyFatJar(fat);
        System.out.println("FatJarSmokeTest passed (classes/resources/native hashes, merged services, java -jar, isolated legacy IR and process exit)");
    }

    private static void verify(Path archive, List<Path> dependencies) throws Exception {
        try (var fat = new JarFile(archive.toFile())) {
            check("bms.player.beatoraja.DesktopLauncher".equals(fat.getManifest().getMainAttributes().getValue("Main-Class")), "Desktop entry point");
            check(fat.getManifest().getMainAttributes().getValue("Class-Path") == null && !fat.isMultiRelease(), "Self-contained Java 17 view");
            var names = new HashSet<String>();
            for (var entry : fat.stream().toList()) {
                String name = entry.getName();
                check(names.add(name), "Duplicate Fat JAR entry: " + name);
                check(!name.startsWith("META-INF/versions/") && !name.equals("module-info.class")
                        && !name.equals("META-INF/INDEX.LIST") && !name.matches("META-INF/[^/]+\\.(?i:SF|RSA|DSA|EC)"), "Stale metadata: " + name);
                check(!name.startsWith("bms/build/") && !name.startsWith("bms/player/beatoraja/build/")
                        && !name.equals("org/lwjgl/Sys.class"), "Build/test-only class leaked: " + name);
            }
            check(fat.getEntry("resources/UIResources_ja_JP.properties") != null
                    && fat.getEntry("bms/player/beatoraja/launcher/PlayConfigurationView.fxml") != null, "Launcher resources");
            var seen = new HashSet<String>();
            var services = new LinkedHashMap<String, LinkedHashSet<String>>();
            int verified = 0;
            for (Path dependency : dependencies) {
                try (var jar = new JarFile(dependency.toFile(), false, ZipFile.OPEN_READ, Runtime.Version.parse("17"))) {
                    for (var entry : jar.versionedStream().filter(e -> !e.isDirectory()).toList()) {
                        String name = entry.getName();
                        if (name.startsWith("META-INF/services/")) {
                            var providers = services.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
                            try (var input = jar.getInputStream(entry)) {
                                for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                                    String provider = line.split("#", 2)[0].trim();
                                    if (!provider.isEmpty()) providers.add(provider);
                                }
                            }
                        } else if (!name.startsWith("META-INF/") && !name.equals("module-info.class") && seen.add(name)) {
                            var target = fat.getJarEntry(name);
                            check(target != null, "Missing dependency entry: " + dependency.getFileName() + " / " + name);
                            try (var original = jar.getInputStream(entry); var merged = fat.getInputStream(target)) {
                                check(Arrays.equals(hash(original), hash(merged)), "Dependency class/resource precedence changed: " + name);
                            }
                            verified++;
                        }
                    }
                }
            }
            for (var entry : services.entrySet()) {
                try (var input = fat.getInputStream(fat.getJarEntry(entry.getKey()))) {
                    check(new String(input.readAllBytes(), StandardCharsets.UTF_8).equals(String.join("\n", entry.getValue()) + "\n"),
                            "Service providers lost/reordered: " + entry.getKey());
                }
            }
            check(verified > 100 && !services.isEmpty(), "Dependency verification did not run");
            System.out.println("Fat JAR entries verified=" + verified + ", services=" + services.size());
        }
    }

    private static byte[] hash(InputStream input) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        for (int length; (length = input.read(buffer)) != -1;) digest.update(buffer, 0, length);
        return digest.digest();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
