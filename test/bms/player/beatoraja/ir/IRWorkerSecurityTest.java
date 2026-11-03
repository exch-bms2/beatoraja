package bms.player.beatoraja.ir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.ToolProvider;

/** Harmless marker plugins and real child JVMs; never accesses player settings or an IR server. */
public final class IRWorkerSecurityTest {
    private static final String MARKER = "beatoraja.test.ir.constructor";

    public static final class ConstructorMarker {
        public ConstructorMarker() { System.setProperty(MARKER, "constructed"); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--json-worker")) {
            new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
            System.out.println("{\"class\":\"" + ConstructorMarker.class.getName()
                    + "\",\"succeeded\":true,\"text\":\"safe\"}");
            System.out.flush();
            return;
        }
        testFixedJsonTypes();
        testWireJson();
        Path root = Files.createTempDirectory("ir-worker-security-");
        try {
            Path plugin = compilePlugin(root);
            testClasspathPlan(root, plugin);
            Path host = compileHost(root);
            runHost(root, host, plugin, false);
            runHost(root, host, plugin, true);
            Path directoryRoot = root.resolve("directory-case");
            Files.createDirectories(directoryRoot);
            runHost(directoryRoot, host, root.resolve("plugin-classes"), false);
            Path defaultRoot = root.resolve("default-case");
            Files.createDirectories(defaultRoot.resolve("ir"));
            Files.copy(plugin, defaultRoot.resolve("ir/plugin.jar"));
            runHost(defaultRoot, host, null, false);
            testDiscoveryTimeout(root, plugin);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("IRWorkerSecurityTest passed (parent classpath, worker metadata/legacy APIs, strict JSON, timeout cleanup)");
    }

    private static void testFixedJsonTypes() {
        String type = ConstructorMarker.class.getName();
        System.clearProperty(MARKER);
        var json = IsolatedIRConnection.createJson();
        var response = json.fromJson(IRWorkerProtocol.Response.class, "{\"class\":\"" + type
                + "\",\"succeeded\":true,\"player\":{\"class\":\"" + type + "\",\"id\":\"fixture\"},"
                + "\"players\":[{\"class\":\"" + type + "\",\"id\":\"nested\"}]}");
        check(response.succeeded && response.player.id.equals("fixture") && response.players[0].id.equals("nested"), "Fixed DTO fields retained");
        var metadata = json.fromJson(IRWorkerProtocol.Discovery.class, "{\"class\":\"" + type
                + "\",\"connections\":[{\"class\":\"" + type + "\",\"name\":\"metadata\"}]}");
        check(metadata.connections[0].name.equals("metadata"), "Fixed metadata DTO");
        var request = json.fromJson(IRWorkerProtocol.Request.class, "{\"class\":\"" + type + "\",\"method\":\"login\"}");
        check(request.method.equals("login") && System.getProperty(MARKER) == null, "No dynamic constructor in requests or responses");
        check(!json.toJson(response).contains("\"class\""), "No outgoing polymorphic tags");
    }

    private static void testWireJson() throws Exception {
        List<Process> children = new ArrayList<>();
        System.clearProperty(MARKER);
        try (var connection = new IsolatedIRConnection("untrusted-json", 5_000, () -> {
            Process process = new ProcessBuilder(java(), "-cp", System.getProperty("java.class.path"),
                    IRWorkerSecurityTest.class.getName(), "--json-worker").redirectError(ProcessBuilder.Redirect.INHERIT).start();
            children.add(process);
            return process;
        })) {
            check("safe".equals(connection.getPlayerURL(null)), "Normal response works even with ignored class tag");
            check(System.getProperty(MARKER) == null, "Wire response cannot execute a parent constructor");
        } finally {
            for (Process child : children) {
                if (child.isAlive()) child.destroyForcibly();
                check(child.waitFor(5, TimeUnit.SECONDS), "JSON fixture leaked");
            }
        }
    }

    private static Path compilePlugin(Path root) throws Exception {
        Path classes = root.resolve("plugin-classes");
        Files.createDirectories(classes);
        String base = "bms/player/beatoraja/ir/TestIRConnectionBase.class";
        Files.createDirectories(classes.resolve(base).getParent());
        try (var input = IRWorkerSecurityTest.class.getClassLoader().getResourceAsStream(base)) {
            Files.copy(input, classes.resolve(base));
        }
        compile(root, classes, "bms/player/beatoraja/ir/security/MarkerIR.java", """
                package bms.player.beatoraja.ir.security;
                import bms.player.beatoraja.ir.*;
                public final class MarkerIR extends TestIRConnectionBase {
                    public static final String NAME = initialize();
                    public static final String HOME = "https://example.invalid/fixture";
                    public MarkerIR() { event("constructor"); }
                    private static String initialize() {
                        System.setProperty("fixture.plugin.initialized", "true");
                        event("init");
                        if (Boolean.getBoolean("fixture.metadata.hang")) {
                            try { Thread.sleep(60000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }
                        return "Isolated marker IR";
                    }
                    private static void event(String name) {
                        try { java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("fixture.events")),
                            name + ":" + ProcessHandle.current().pid() + "\\n",
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); }
                        catch (java.io.IOException e) { throw new IllegalStateException(e); }
                    }
                    private static <T> IRResponse<T> result(T value) {
                        return new IRResponse<T>() {
                            public boolean isSucceeded() { return true; }
                            public String getMessage() { return "fixture"; }
                            public T getData() { return value; }
                        };
                    }
                    public IRResponse<IRPlayerData> login(IRAccount account) {
                        if (!getClass().equals(IRConnectionManager.getIRConnectionClass(NAME))) throw new AssertionError("Legacy worker Class API");
                        if (IRConnectionManager.getAllAvailableIRConnectionName().length != 1) throw new AssertionError("Worker metadata recursion");
                        event("login"); return result(new IRPlayerData(account.id, account.name, ""));
                    }
                    public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) {
                        event("send"); return result(null);
                    }
                    public IRResponse<IRScoreData[]> getPlayData(IRPlayerData player, IRChartData chart) {
                        return result(new IRScoreData[0]);
                    }
                }
                """);
        compile(root, classes, "bms/player/beatoraja/ir/security/dto/Unused.java", """
                package bms.player.beatoraja.ir.security.dto;
                public class Unused { static { throwIfInitialized(); }
                    private static void throwIfInitialized() { throw new AssertionError("DTO must not initialize"); } }
                """);
        compile(root, classes, "bms/player/beatoraja/ir/security/NoName.java", """
                package bms.player.beatoraja.ir.security;
                public final class NoName extends bms.player.beatoraja.ir.TestIRConnectionBase { }
                """);
        compile(root, classes, "fixture/PluginDriver.java", """
                package fixture;
                public final class PluginDriver implements java.sql.Driver {
                    static { System.setProperty("fixture.service.initialized", "true"); }
                    public java.sql.Connection connect(String url, java.util.Properties info) { return null; }
                    public boolean acceptsURL(String url) { return false; }
                    public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) {
                        return new java.sql.DriverPropertyInfo[0];
                    }
                    public int getMajorVersion() { return 1; }
                    public int getMinorVersion() { return 0; }
                    public boolean jdbcCompliant() { return false; }
                    public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getGlobal(); }
                }
                """);
        Path service = classes.resolve("META-INF/services/java.sql.Driver");
        Files.createDirectories(service.getParent());
        Files.writeString(service, "fixture.PluginDriver\n");
        Path jar = root.resolve("marker-plugin.jar");
        jar(classes, jar, null); // Intentionally no package-directory entries in this JAR.
        return jar;
    }

    private static Path compileHost(Path root) throws Exception {
        Path classes = root.resolve("host-classes");
        compile(root, classes, "fixture/CleanHost.java", """
                package fixture;
                import bms.player.beatoraja.ir.*;
                public final class CleanHost {
                    public static void main(String[] args) throws Exception {
                        IRProcessSupport.ensureParentClasspath(CleanHost.class.getName(), args);
                        var bundle = java.util.ResourceBundle.getBundle("resources.UIResources", java.util.Locale.JAPAN);
                        if (!bundle.getLocale().equals(java.util.Locale.JAPAN) || bundle.getString("FULLSCREEN").isBlank())
                            throw new AssertionError("Japanese launcher resources missing");
                        if (CleanHost.class.getResource("/bms/player/beatoraja/launcher/PlayConfigurationView.fxml") == null)
                            throw new AssertionError("Launcher FXML missing");
                        try { Class.forName("bms.player.beatoraja.ir.security.MarkerIR");
                            throw new AssertionError("Plugin is visible to parent"); }
                        catch (ClassNotFoundException expected) { }
                        if (System.getProperty("fixture.plugin.initialized") != null) throw new AssertionError("Parent plugin init");
                        java.sql.DriverManager.getDrivers();
                        if (System.getProperty("fixture.service.initialized") != null) throw new AssertionError("Parent service init");
                        String[] names = IRConnectionManager.getAllAvailableIRConnectionName();
                        if (!java.util.Arrays.equals(names, new String[]{"Isolated marker IR"}))
                            throw new AssertionError("Metadata names: " + java.util.Arrays.toString(names));
                        if (!"https://example.invalid/fixture".equals(IRConnectionManager.getHomeURL(names[0])))
                            throw new AssertionError("Metadata HOME");
                        if (IRConnectionManager.hasIRConnection("missing")) throw new AssertionError("Invalid IR");
                        try { IRConnectionManager.getIRConnectionClass(names[0]); throw new AssertionError("Parent Class API"); }
                        catch (IllegalStateException expected) { }
                        try (var proxy = (IsolatedIRConnection) IRConnectionManager.getIRConnection(names[0])) {
                            if (!proxy.login(new IRAccount("fixture", "not-a-real-password", "fixture")).isSucceeded())
                                throw new AssertionError("Legacy login");
                            if (!proxy.sendPlayData(null, null).isSucceeded()) throw new AssertionError("Legacy send");
                            if (!proxy.getPlayDataSince(null, 0).isSucceeded()) throw new AssertionError("Legacy default API");
                        }
                        if (System.getProperty("fixture.plugin.initialized") != null) throw new AssertionError("Parent init after RPC");
                        for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of(System.getProperty("fixture.events")))) {
                            long pid = Long.parseLong(line.substring(line.indexOf(':') + 1));
                            if (pid == ProcessHandle.current().pid()) throw new AssertionError("Parent initialized metadata");
                        }
                        System.out.println("CLEAN-PARENT-OK");
                    }
                }
                """);
        Path jar = root.resolve("host.jar"); jar(classes, jar, null); return jar;
    }

    private static void runHost(Path root, Path host, Path plugin, boolean legacyClasspath) throws Exception {
        runHost(root, host, plugin, legacyClasspath,
                System.getProperty("fixture.application.classpath", IRProcessSupport.classpaths().applicationPath()));
    }

    private static void runHost(Path root, Path host, Path plugin, boolean legacyClasspath, String base) throws Exception {
        Path events = root.resolve(legacyClasspath ? "legacy-events.txt" : "events.txt");
        String cp = host + File.pathSeparator + base + (legacyClasspath ? File.pathSeparator + plugin : "");
        List<String> command = new ArrayList<>(List.of(java(), "-Dfixture.events=" + events));
        if (plugin != null) command.add("-D" + IRProcessSupport.PLUGIN_CLASSPATH + "=" + plugin);
        command.addAll(List.of("-cp", cp, "fixture.CleanHost"));
        Path log = root.resolve(legacyClasspath ? "legacy.log" : "host.log");
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            check(process.waitFor(30, TimeUnit.SECONDS), "Parent/worker integration timeout");
            String output = Files.readString(log);
            check(process.exitValue() == 0 && output.contains("CLEAN-PARENT-OK"), "Parent/worker integration: " + output);
            check(!output.contains("IRConnection読み込み失敗"), "Non-IR DTOs must not generate load warnings: " + output);
            List<String> entries = Files.readAllLines(events);
            check(entries.stream().filter(line -> line.startsWith("init:")).count() == 2, "Only metadata and RPC workers initialize the plugin");
            check(entries.stream().filter(line -> line.startsWith("constructor:")).count() == 1, "Metadata must not instantiate IRConnection");
            check(entries.stream().filter(line -> line.startsWith("send:")).count() == 1, "No duplicate score upload");
            for (String line : entries) {
                long pid = Long.parseLong(line.substring(line.indexOf(':') + 1));
                check(ProcessHandle.of(pid).map(p -> !p.isAlive()).orElse(true), "Worker process must exit: " + line);
            }
        } finally {
            process.descendants().forEach(child -> child.destroyForcibly());
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    /** Uses only the actual application/dependency JARs extracted from the generated ZIP in both parent and worker. */
    public static void verifyDistribution(Path zipPath) throws Exception {
        Path root = Files.createTempDirectory("ir-packaged-security-");
        try {
            Path lib = Files.createDirectories(root.resolve("lib"));
            List<String> jars = new ArrayList<>();
            try (var zip = new java.util.zip.ZipFile(zipPath.toFile())) {
                for (var entry : zip.stream().filter(value -> value.getName().matches("[^/]+/lib/[^/]+\\.jar")).toList()) {
                    Path destination = lib.resolve(entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
                    try (var input = zip.getInputStream(entry)) { Files.copy(input, destination); }
                    jars.add(destination.toString());
                }
            }
            check(!jars.isEmpty(), "Distribution libraries missing");
            Path plugin = compilePlugin(root);
            Path host = compileHost(root);
            runHost(root, host, plugin, false, String.join(File.pathSeparator, jars));
            runHost(root, host, plugin, true, String.join(File.pathSeparator, jars));
            System.out.println("Packaged IR security passed (clean parent and unchanged legacy IR JAR in real ZIP runtime)");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void testClasspathPlan(Path root, Path plugin) throws Exception {
        Path core = Path.of(IRProcessSupport.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path resources = root.resolve("resources-only");
        Files.createDirectories(resources.resolve("bms/player/beatoraja/ir"));
        Files.writeString(resources.resolve("bms/player/beatoraja/ir/README.txt"), "Not an IR implementation");
        for (String name : List.of("resources/UIResources.properties", "resources/UIResources_ja_JP.properties",
                "bms/player/beatoraja/launcher/PlayConfigurationView.fxml")) {
            Path destination = resources.resolve(name);
            Files.createDirectories(destination.getParent());
            try (var input = IRWorkerSecurityTest.class.getClassLoader().getResourceAsStream(name)) {
                if (input == null) throw new IOException("Missing test runtime resource: " + name);
                Files.copy(input, destination);
            }
        }
        var resourcePaths = IRProcessSupport.classpaths(core + File.pathSeparator + resources, null, root.resolve("ir"));
        check(resourcePaths.application().contains(resources.toRealPath()) && resourcePaths.plugins().isEmpty(),
                "Gradle resource output with an empty IR package must remain on the parent classpath");
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{ resources.toUri().toURL() }, null)) {
            var bundle = java.util.ResourceBundle.getBundle("resources.UIResources", java.util.Locale.JAPAN, loader);
            check(bundle.getLocale().equals(java.util.Locale.JAPAN), "Fixture contains Japanese bundle");
        }
        var paths = IRProcessSupport.classpaths(core + File.pathSeparator + plugin, null, root.resolve("ir"));
        check(paths.application().equals(List.of(core.toRealPath())) && paths.plugins().contains(plugin.toRealPath()), "Legacy plugin removed from parent");
        Path renamed = root.resolve("renamed-plugin.zip"); Files.copy(plugin, renamed);
        paths = IRProcessSupport.classpaths(core + File.pathSeparator + renamed, null, root.resolve("ir"));
        check(!paths.application().contains(renamed.toRealPath()), "A renamed JAR must not bypass classpath filtering");
        paths = IRProcessSupport.classpaths(core.toString(), root.resolve("plugin-classes").toString(), root.resolve("ir"));
        check(paths.plugins().contains(root.resolve("plugin-classes").toRealPath()), "Directory plugin support");
        paths = IRProcessSupport.classpaths(core + File.pathSeparator + root.resolve("plugin-classes"), null, root.resolve("ir"));
        check(paths.plugins().contains(root.resolve("plugin-classes").toRealPath())
                && !paths.application().contains(root.resolve("plugin-classes").toRealPath()), "Real IR class directory still excluded");
        Path linked = root.resolve("linked-class-output");
        Path link = linked.resolve("bms/player/beatoraja/ir");
        Files.createDirectories(link.getParent());
        boolean symlinkCreated = false;
        try {
            Files.createSymbolicLink(link, root.resolve("plugin-classes/bms/player/beatoraja/ir").toAbsolutePath());
            symlinkCreated = true;
        } catch (UnsupportedOperationException | IOException e) {
            System.out.println("SKIP IR class directory symlink: " + e.getMessage());
        }
        if (symlinkCreated) {
            paths = IRProcessSupport.classpaths(core + File.pathSeparator + linked, null, root.resolve("ir"));
            check(paths.plugins().contains(linked.toRealPath()) && !paths.application().contains(linked.toRealPath()),
                    "Symlinked IR classes must still be excluded from the parent");
        }
        Files.createDirectories(root.resolve("ir"));
        Path dependency = root.resolve("ir/dependency.jar");
        jar(root.resolve("host-empty"), dependency, null);
        paths = IRProcessSupport.classpaths(core + File.pathSeparator + dependency, null, root.resolve("ir"));
        check(!paths.application().contains(dependency.toRealPath()), "IR helper libraries also excluded");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, plugin.toUri().toString());
        Path unsafe = root.resolve("unsafe-manifest.jar"); jar(root.resolve("host-empty"), unsafe, manifest);
        try {
            IRProcessSupport.classpaths(core + File.pathSeparator + unsafe, null, root.resolve("ir"));
            throw new AssertionError("Manifest bypass accepted");
        } catch (IOException expected) { check(expected.getMessage().contains("manifest"), "Manifest failure diagnosis"); }
        Manifest chain = new Manifest();
        chain.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        chain.getMainAttributes().put(Attributes.Name.CLASS_PATH, unsafe.getFileName().toString());
        Path indirect = root.resolve("indirect-manifest.jar"); jar(root.resolve("host-empty"), indirect, chain);
        try {
            IRProcessSupport.classpaths(core + File.pathSeparator + indirect, null, root.resolve("ir"));
            throw new AssertionError("Indirect manifest bypass accepted");
        } catch (IOException expected) { check(expected.getMessage().contains("manifest"), "Indirect manifest failure diagnosis"); }
    }

    private static void testDiscoveryTimeout(Path root, Path plugin) throws Exception {
        String before = System.getProperty(IRProcessSupport.PLUGIN_CLASSPATH);
        System.setProperty(IRProcessSupport.PLUGIN_CLASSPATH, plugin.toString());
        // startWorker propagates VM arguments, not arbitrary runtime properties. A special child JVM supplies this fixture flag.
        Path events = root.resolve("timeout-events.txt");
        Path log = root.resolve("timeout.log");
        Process process = new ProcessBuilder(java(), "-Dfixture.events=" + events, "-Dfixture.metadata.hang=true",
                "-D" + IRProcessSupport.PLUGIN_CLASSPATH + "=" + plugin, "-cp", System.getProperty("java.class.path"),
                TimeoutProbe.class.getName()).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            check(process.waitFor(20, TimeUnit.SECONDS), "Discovery timeout cleanup hung");
            check(process.exitValue() == 0, "Discovery timeout: " + Files.readString(log));
            for (String line : Files.readAllLines(events)) {
                long pid = Long.parseLong(line.substring(line.indexOf(':') + 1));
                check(ProcessHandle.of(pid).map(p -> !p.isAlive()).orElse(true), "Timed out metadata worker leaked");
            }
        } finally {
            if (before == null) System.clearProperty(IRProcessSupport.PLUGIN_CLASSPATH); else System.setProperty(IRProcessSupport.PLUGIN_CLASSPATH, before);
            process.descendants().forEach(child -> child.destroyForcibly());
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    public static final class TimeoutProbe {
        public static void main(String[] args) throws Exception {
            try { IRConnectionManager.discover(5_000); throw new AssertionError("Missing discovery timeout"); }
            catch (IOException expected) { }
        }
    }

    private static void compile(Path root, Path classes, String name, String source) throws IOException {
        Path file = root.resolve("sources").resolve(name);
        Files.createDirectories(file.getParent()); Files.createDirectories(classes);
        Files.writeString(file, source);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-classpath",
                System.getProperty("java.class.path"), "-d", classes.toString(), file.toString());
        check(result == 0, "Fixture compilation: " + name);
    }

    private static void jar(Path classes, Path destination, Manifest manifest) throws IOException {
        try (var out = manifest == null ? new JarOutputStream(Files.newOutputStream(destination))
                : new JarOutputStream(Files.newOutputStream(destination), manifest)) {
            if (!Files.isDirectory(classes)) return;
            try (var files = Files.walk(classes)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    out.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                    Files.copy(file, out); out.closeEntry();
                }
            }
        }
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
