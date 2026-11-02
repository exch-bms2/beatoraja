package bms.player.beatoraja.ir;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

/** JDK-only bootstrap: plugin files belong exclusively to the worker classpath. */
public final class IRProcessSupport {
    public static final String PLUGIN_CLASSPATH = "beatoraja.ir.classpath";
    private static final String RESOURCE = "bms/player/beatoraja/ir/";

    private IRProcessSupport() { }

    record Classpaths(List<Path> application, List<Path> plugins) {
        String applicationPath() { return join(application); }
        String workerPath() {
            Set<Path> paths = new LinkedHashSet<>(application);
            paths.addAll(plugins);
            return join(paths);
        }
    }

    static Classpaths classpaths() throws IOException {
        return classpaths(System.getProperty("java.class.path"),
                System.getProperty(PLUGIN_CLASSPATH), Path.of("ir"));
    }

    static Classpaths classpaths(String classpath, String configured, Path directory) throws IOException {
        Path core;
        try {
            core = Path.of(IRProcessSupport.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
        Set<Path> plugins = new LinkedHashSet<>();
        if (configured != null && !configured.isBlank()) {
            for (String entry : configured.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!entry.isEmpty()) plugins.add(canonical(Path.of(entry)));
            }
        }
        if (Files.isDirectory(directory)) {
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(IRProcessSupport::isJar).sorted().toList()) plugins.add(canonical(file));
            }
        }
        List<Path> application = new ArrayList<>();
        Path pluginDirectory = canonical(directory);
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator), -1)) {
            Path file = canonical(Path.of(entry));
            if (!file.equals(core) && (plugins.contains(file) || file.startsWith(pluginDirectory) || containsIR(file))) {
                plugins.add(file);
            } else {
                application.add(file);
            }
        }
        plugins.remove(core);
        // Manifest Class-Path entries bypass an explicit -cp filter. Refuse unsafe legacy manifests.
        Set<Path> checked = new LinkedHashSet<>();
        for (Path file : application) validateManifest(file, core, plugins, pluginDirectory, checked);
        return new Classpaths(List.copyOf(application), List.copyOf(plugins));
    }

    private static void validateManifest(Path file, Path core, Set<Path> plugins, Path directory, Set<Path> checked) throws IOException {
        if (!checked.add(file) || !Files.isRegularFile(file)) return;
        List<Path> references = new ArrayList<>();
        try (JarFile jar = new JarFile(file.toFile())) {
            var manifest = jar.getManifest();
            String value = manifest == null ? null : manifest.getMainAttributes().getValue("Class-Path");
            if (value == null) return;
            for (String reference : value.trim().split("\\s+")) {
                if (reference.isEmpty()) continue;
                Path target;
                try { target = canonical(Path.of(file.toUri().resolve(reference))); }
                catch (IllegalArgumentException e) { throw new IOException("Unsupported manifest Class-Path: " + file, e); }
                if (!target.equals(core) && (plugins.contains(target) || target.startsWith(directory) || containsIR(target)))
                    throw new IOException("IR JAR in manifest Class-Path; remove that entry before launching: " + file);
                references.add(target);
            }
        } catch (java.util.zip.ZipException ignored) {
            return; // The JVM also ignores non-archive files on a classpath.
        }
        for (Path target : references) validateManifest(target, core, plugins, directory, checked);
    }

    private static boolean containsIR(Path file) throws IOException {
        if (Files.isDirectory(file)) return Files.isDirectory(file.resolve(RESOURCE));
        if (!Files.isRegularFile(file)) return false;
        try (JarFile jar = new JarFile(file.toFile())) {
            return jar.stream().anyMatch(entry -> entry.getName().startsWith(RESOURCE) && entry.getName().endsWith(".class"));
        } catch (java.util.zip.ZipException ignored) {
            return false;
        }
    }

    private static boolean isJar(Path file) {
        return file.getFileName() != null && file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar");
    }

    private static Path canonical(Path path) throws IOException {
        return Files.exists(path) ? path.toRealPath() : path.toAbsolutePath().normalize();
    }

    private static String join(Iterable<Path> paths) {
        List<String> entries = new ArrayList<>();
        paths.forEach(path -> entries.add(path.toString()));
        return String.join(File.pathSeparator, entries);
    }

    /** Re-exec legacy classpath launches before any plugin discovery or application startup. */
    public static void ensureParentClasspath(String mainClass, String[] args) {
        try {
            Classpaths paths = classpaths();
            System.setProperty(PLUGIN_CLASSPATH, join(paths.plugins()));
            Set<Path> current = new LinkedHashSet<>();
            for (String entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator), -1))
                current.add(canonical(Path.of(entry)));
            if (current.equals(new LinkedHashSet<>(paths.application()))) return;
            List<String> command = javaCommand(false);
            command.add("-D" + PLUGIN_CLASSPATH + "=" + join(paths.plugins()));
            command.addAll(List.of("-cp", paths.applicationPath(), mainClass));
            command.addAll(Arrays.asList(args));
            Process child = new ProcessBuilder(command).inheritIO().start();
            Thread cleanup = new Thread(child::destroy, "ir-classpath-bootstrap-shutdown");
            Runtime.getRuntime().addShutdownHook(cleanup);
            int result = child.waitFor();
            Runtime.getRuntime().removeShutdownHook(cleanup);
            System.exit(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot isolate IR plugin classpath", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while launching isolated application", e);
        }
    }

    static Process startWorker(String argument) throws IOException {
        List<String> command = javaCommand(true);
        Classpaths paths = classpaths();
        command.add("-D" + PLUGIN_CLASSPATH + "=" + join(paths.plugins()));
        command.addAll(List.of("-cp", paths.workerPath(), IRWorkerMain.class.getName(), argument));
        return new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
    }

    private static List<String> javaCommand(boolean worker) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString());
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (int i = 0; i < arguments.size(); i++) {
            String arg = arguments.get(i);
            // Do not allocate the game's initial heap (usually 1 GiB) for each short-lived discovery worker.
            if (worker && (arg.startsWith("-Xms") || arg.startsWith("-Xmx") || arg.startsWith("-Xss")
                    || arg.startsWith("-XX:") || arg.equals("-XstartOnFirstThread"))) continue;
            if (List.of("--module-path", "-p", "--add-modules", "--add-opens", "--add-exports").contains(arg)) {
                command.add(arg);
                if (i + 1 < arguments.size()) command.add(arguments.get(++i));
            } else if ((arg.startsWith("-D") || arg.startsWith("-Xmx") || arg.startsWith("-Xms")
                    || arg.startsWith("-Xss") || arg.startsWith("--module-path=") || arg.startsWith("--add-modules=")
                    || arg.startsWith("--add-opens=") || arg.startsWith("--add-exports=") || arg.startsWith("-XX:")
                    || arg.equals("-XstartOnFirstThread"))
                    && !arg.startsWith("-Djava.class.path=") && !arg.startsWith("-D" + PLUGIN_CLASSPATH + "=")) {
                command.add(arg);
            }
        }
        return command;
    }
}
