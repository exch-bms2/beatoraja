package bms.player.beatoraja.build;

import bms.build.FatJarContent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/** Synthetic dependency archives: no plugin code or native libraries are executed. */
public final class FatJarContentTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("fat-jar-content-");
        try {
            Path main = Files.createDirectories(root.resolve("main/example"));
            Files.writeString(main.resolve("Owner.class"), "application");
            Map<String, String> first = new LinkedHashMap<>();
            first.put("example/Owner.class", "dependency");
            first.put("example/Duplicate.class", "first");
            first.put("example/Versioned.class", "base");
            first.put("META-INF/versions/17/example/Versioned.class", "java17");
            first.put("META-INF/versions/21/example/Versioned.class", "java21");
            first.put("META-INF/services/example.Service", "first.Provider # comment\nshared.Provider\n");
            first.put("linux/x64/libfixture.so", "native");
            first.put("META-INF/LICENSE", "license one");
            first.put("META-INF/INDEX.LIST", "stale");
            first.put("META-INF/SIGNATURE.SF", "invalid");
            first.put("module-info.class", "module");
            first.put("META-INF/versions/9/module-info.class", "versioned module");
            Path one = jar(root.resolve("one.jar"), first, true, null);
            Path two = jar(root.resolve("two.jar"), Map.of(
                    "example/Duplicate.class", "second",
                    "META-INF/versions/17/example/Inactive.class", "must not activate",
                    "example/Inactive.class", "inactive base",
                    "META-INF/services/example.Service", "shared.Provider\nsecond.Provider\n",
                    "linux/x64/libfixture.so", "native",
                    "META-INF/LICENSE", "license two"), false, null);
            Path output = root.resolve("output");
            FatJarContent.prepare(output, root.resolve("ir"), List.of(main.getParent()), List.of(one, two));
            check(Files.readString(output.resolve("example/Owner.class")).equals("application"), "Application precedence");
            check(Files.readString(output.resolve("example/Duplicate.class")).equals("first"), "Dependency classpath order");
            check(Files.readString(output.resolve("example/Versioned.class")).equals("java17"), "Effective Java 17 entry");
            check(Files.readString(output.resolve("example/Inactive.class")).equals("inactive base"), "Inactive MR entries remain inactive");
            check(Files.readString(output.resolve("META-INF/services/example.Service"))
                    .equals("first.Provider\nshared.Provider\nsecond.Provider\n"), "Merged services/order/deduplication");
            for (String name : List.of("META-INF/MANIFEST.MF", "META-INF/INDEX.LIST", "META-INF/SIGNATURE.SF", "module-info.class", "META-INF/versions"))
                check(!Files.exists(output.resolve(name)), "Retired metadata: " + name);
            check(Files.readString(output.resolve("META-INF/beatoraja/dependencies/000-one.jar/META-INF/LICENSE")).equals("license one")
                    && Files.readString(output.resolve("META-INF/beatoraja/dependencies/001-two.jar/META-INF/LICENSE")).equals("license two"), "All dependency licenses retained");
            expectFailure(() -> FatJarContent.prepare(output, root.resolve("ir"), List.of(), List.of()), "existing");
            Path conflict = jar(root.resolve("conflict.jar"), Map.of("linux/x64/libfixture.so", "different native"), false, null);
            expectFailure(() -> FatJarContent.prepare(root.resolve("bad-native"), root.resolve("ir"), List.of(), List.of(one, conflict)), "Conflicting native");
            for (String path : List.of("bms/player/beatoraja/ir/plugin/Bad.class", "META-INF/versions/17/bms/player/beatoraja/ir/plugin/Bad.class")) {
                Path plugin = jar(root.resolve("plugin.jar"), Map.of(path, "not executable"), false, null);
                expectFailure(() -> FatJarContent.prepare(root.resolve("bad-plugin-" + path.length()), root.resolve("ir"), List.of(), List.of(plugin)), "IR implementation");
            }
            Path ir = Files.createDirectories(root.resolve("ir"));
            Path helper = jar(ir.resolve("helper.jar"), Map.of("helper.txt", "not a plugin class"), false, null);
            expectFailure(() -> FatJarContent.prepare(root.resolve("bad-helper"), ir, List.of(), List.of(helper)), "IR plugin directory");
            Path indirect = jar(root.resolve("indirect.jar"), Map.of(), false, "ir/helper.jar");
            Path indirectOutput = root.resolve("manifest-not-followed");
            FatJarContent.prepare(indirectOutput, ir, List.of(), List.of(indirect));
            check(!Files.exists(indirectOutput.resolve("helper.txt"))
                    && !Files.exists(indirectOutput.resolve("META-INF/MANIFEST.MF")), "Manifest cannot pull IR dependencies into Fat JAR");
            Path traversal = jar(root.resolve("traversal.jar"), Map.of("../escaped.txt", "bad"), false, null);
            expectFailure(() -> FatJarContent.prepare(root.resolve("bad-path"), ir, List.of(), List.of(traversal)), "Unsafe archive");
            check(!Files.exists(root.resolve("escaped.txt")), "Traversal rejected");
            System.out.println("FatJarContentTest passed (precedence, Java 17 MR view, services, licenses, signatures, native conflicts and IR exclusion)");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static Path jar(Path path, Map<String, String> entries, boolean multiRelease, String classpath) throws Exception {
        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (multiRelease) manifest.getMainAttributes().putValue("Multi-Release", "true");
        if (classpath != null) manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath);
        try (var output = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return path;
    }

    private static void expectFailure(Check check, String message) throws Exception {
        try { check.run(); }
        catch (java.io.IOException expected) {
            check(expected.getMessage().contains(message), "Wrong failure: " + expected);
            return;
        }
        throw new AssertionError("Unsafe input was accepted: " + message);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @FunctionalInterface private interface Check { void run() throws Exception; }
}
