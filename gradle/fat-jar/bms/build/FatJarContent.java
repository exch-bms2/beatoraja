package bms.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/** Build-only merger. Keeps Java 17 classpath precedence without loading dependency classes. */
public final class FatJarContent {
    private static final String SERVICES = "META-INF/services/";
    private FatJarContent() { }

    public static void main(String[] args) throws Exception {
        int separator = List.of(args).indexOf("--dependencies");
        if (separator < 3) throw new IllegalArgumentException("Expected output, plugin root, application outputs, --dependencies, JARs");
        List<Path> application = new ArrayList<>(), dependencies = new ArrayList<>();
        for (int i = 2; i < separator; i++) application.add(Path.of(args[i]));
        for (int i = separator + 1; i < args.length; i++) dependencies.add(Path.of(args[i]));
        prepare(Path.of(args[0]), Path.of(args[1]), application, dependencies);
    }

    public static void prepare(Path output, Path pluginRoot, List<Path> application, List<Path> dependencies) throws IOException {
        if (Files.exists(output)) throw new IOException("Refuse existing staging directory: " + output);
        Path root = Files.createDirectories(output).toAbsolutePath().normalize();
        Path forbidden = Files.exists(pluginRoot) ? pluginRoot.toRealPath() : pluginRoot.toAbsolutePath().normalize();
        Map<String, LinkedHashSet<String>> services = new LinkedHashMap<>();
        for (Path directory : application) {
            if (!Files.isDirectory(directory)) continue;
            try (var paths = Files.walk(directory)) {
                for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String name = directory.relativize(file).toString().replace('\\', '/');
                    try (var input = Files.newInputStream(file)) { copy(root, name, input, services); }
                }
            }
        }
        StringBuilder inventory = new StringBuilder("Java 17 effective entries; first runtime classpath entry wins.\n");
        for (int index = 0; index < dependencies.size(); index++) {
            Path file = dependencies.get(index).toRealPath();
            if (file.startsWith(forbidden)) throw new IOException("IR plugin directory is not a Fat JAR input: " + file);
            String provenance = "META-INF/beatoraja/dependencies/" + String.format(Locale.ROOT, "%03d-", index)
                    + file.getFileName() + "/";
            inventory.append(index).append(' ').append(file.getFileName()).append('\n');
            try (var jar = new JarFile(file.toFile(), false, ZipFile.OPEN_READ, Runtime.Version.parse("17"))) {
                // Scan raw entries too: an inactive version must not conceal a plugin class.
                if (jar.stream().anyMatch(entry -> entry.getName().endsWith(".class")
                        && (entry.getName().startsWith("bms/player/beatoraja/ir/")
                        || entry.getName().matches("META-INF/versions/[0-9]+/bms/player/beatoraja/ir/.*")))) {
                    throw new IOException("IR implementation/API classes in dependency: " + file);
                }
                // Do not follow or inherit manifest Class-Path (the parser JARs use '.').
                // Only Gradle's explicit runtime inputs may contribute code or services.
                // Flatten only effective Java 17 entries. Inactive versions in repacked parser
                // JARs must not become active merely because LWJGL uses Multi-Release: true.
                for (JarEntry entry : jar.versionedStream().filter(e -> !e.isDirectory()).sorted(Comparator.comparing(JarEntry::getName)).toList()) {
                    String name = entry.getName();
                    if (name.startsWith("META-INF/versions/")) continue;
                    try (var input = jar.getInputStream(entry)) { copy(root, name, input, services); }
                }
                for (JarEntry entry : jar.stream().filter(e -> !e.isDirectory() && legalNotice(e.getName())).toList()) {
                    Path target = target(root, provenance + entry.getName());
                    if (!Files.exists(target)) {
                        Files.createDirectories(target.getParent());
                        try (var input = jar.getInputStream(entry)) { Files.copy(input, target); }
                    }
                }
            }
        }
        for (var entry : services.entrySet()) {
            Path target = target(root, entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, String.join("\n", entry.getValue()) + "\n", StandardCharsets.UTF_8);
        }
        Path inventoryFile = target(root, "META-INF/beatoraja/dependencies.txt");
        Files.createDirectories(inventoryFile.getParent());
        Files.writeString(inventoryFile, inventory, StandardCharsets.UTF_8);
    }

    private static void copy(Path root, String name, InputStream input,
            Map<String, LinkedHashSet<String>> services) throws IOException {
        Path target = target(root, name);
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("META-INF/MANIFEST.MF") || upper.equals("META-INF/INDEX.LIST")
                || name.equals("module-info.class") || name.endsWith("/module-info.class")
                || upper.matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC)") || upper.startsWith("META-INF/SIG-")) return;
        if (name.startsWith(SERVICES)) {
            var providers = services.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                String provider = line.split("#", 2)[0].trim();
                if (!provider.isEmpty()) providers.add(provider);
            }
            return;
        }
        if (Files.exists(target)) {
            if (upper.endsWith(".DLL") || upper.endsWith(".SO") || upper.contains(".SO.")
                    || upper.endsWith(".DYLIB") || upper.endsWith(".JNILIB")) {
                Path comparison = Files.createTempFile(root, "native-compare-", ".tmp");
                try {
                    Files.copy(input, comparison, StandardCopyOption.REPLACE_EXISTING);
                    if (Files.mismatch(target, comparison) != -1) throw new IOException("Conflicting native resource: " + name);
                } finally { Files.deleteIfExists(comparison); }
            }
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(input, target);
    }

    private static Path target(Path root, String name) throws IOException {
        Path target = root.resolve(name).normalize();
        if (name.contains("\\") || name.startsWith("/") || name.contains(":") || !target.startsWith(root)
                || !root.relativize(target).toString().replace('\\', '/').equals(name))
            throw new IOException("Unsafe archive entry: " + name);
        return target;
    }

    private static boolean legalNotice(String name) {
        String leaf = name.substring(name.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
        return name.equalsIgnoreCase("META-INF/MANIFEST.MF") || leaf.startsWith("LICENSE")
                || leaf.startsWith("NOTICE") || leaf.startsWith("COPYING") || leaf.startsWith("COPYRIGHT");
    }
}
