package bms.player.beatoraja.ir;

import bms.build.FatJarContent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;

/** Real child JVMs; synthetic Mocha performs local marker operations only. No account or server is used. */
public final class BundledIRIntegrationTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("bundled mocha regression with spaces-");
        try {
            Path classes = root.resolve("mocha-classes");
            compile(root, classes, "bms/player/beatoraja/ir/mocha/FixtureBase.java", """
                    package bms.player.beatoraja.ir.mocha;
                    import bms.player.beatoraja.ir.*;
                    public abstract class FixtureBase implements IRConnection {
                        public IRResponse<IRPlayerData> register(IRAccount a) { return null; }
                        public IRResponse<IRPlayerData> login(IRAccount a) { return null; }
                        public IRResponse<IRPlayerData[]> getRivals() { return null; }
                        public IRResponse<IRTableData[]> getTableDatas() { return null; }
                        public IRResponse<IRScoreData[]> getPlayData(IRPlayerData p, IRChartData c) { return null; }
                        public IRResponse<IRScoreData[]> getCoursePlayData(IRPlayerData p, IRCourseData c) { return null; }
                        public IRResponse<Object> sendPlayData(IRChartData c, IRScoreData s) { return null; }
                        public IRResponse<Object> sendCoursePlayData(IRCourseData c, IRScoreData s) { return null; }
                        public String getSongURL(IRChartData c) { return null; }
                        public String getCourseURL(IRCourseData c) { return null; }
                        public String getPlayerURL(IRPlayerData p) { return null; }
                    }
                    """);
            compile(root, classes, "bms/player/beatoraja/ir/MochaIRConnection.java", """
                    package bms.player.beatoraja.ir;
                    public final class MochaIRConnection extends bms.player.beatoraja.ir.mocha.FixtureBase {
                        public static final String NAME = initialize();
                        public static final String HOME = "https://example.invalid/mocha";
                        private static String initialize() {
                            System.setProperty("fixture.mocha.initialized", "true"); event("mocha-init"); return "mocha";
                        }
                        public MochaIRConnection() { event("mocha-constructor"); }
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
                            event("mocha-login"); return result(new IRPlayerData(account.id, account.name, ""));
                        }
                        public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) {
                            event("mocha-send"); return result(null);
                        }
                        public String getPlayerURL(IRPlayerData player) {
                            try (var input = getClass().getClassLoader().getResourceAsStream("private-worker-resource.txt")) {
                                if (input == null || !new String(input.readAllBytes()).equals("helper resource")) throw new AssertionError("Private resource missing");
                            } catch (java.io.IOException e) { throw new AssertionError(e); }
                            return "worker:" + ProcessHandle.current().pid();
                        }
                    }
                    """);
            compile(root, classes, "bms/player/beatoraja/ir/mocha/dto/Unused.java", """
                    package bms.player.beatoraja.ir.mocha.dto;
                    public class Unused { static { fail(); } private static void fail() { throw new AssertionError("DTO initialized"); } }
                    """);
            Path helperClasses = Files.createDirectory(root.resolve("helper-classes"));
            Path baseClass = helperClasses.resolve("bms/player/beatoraja/ir/mocha/FixtureBase.class");
            Files.createDirectories(baseClass.getParent());
            Files.copy(classes.resolve("bms/player/beatoraja/ir/mocha/FixtureBase.class"), baseClass);
            Files.writeString(helperClasses.resolve("private-worker-resource.txt"), "helper resource");
            compile(root, helperClasses, "privateapi/WorkerService.java", """
                    package privateapi;
                    public class WorkerService implements Runnable { public void run() {} }
                    """);
            Path service = helperClasses.resolve("META-INF/services/java.lang.Runnable"); Files.createDirectories(service.getParent());
            Files.writeString(service, "privateapi.WorkerService\n");
            Path helper = root.resolve("helper.jar"); jar(helperClasses, helper);
            Path secondClasses = root.resolve("second-classes");
            compile(root, secondClasses, "bms/player/beatoraja/ir/other/SecondIR.java", """
                    package bms.player.beatoraja.ir.other;
                    public final class SecondIR extends bms.player.beatoraja.ir.mocha.FixtureBase {
                        public static final String NAME = "second-bundled";
                        public String getPlayerURL(bms.player.beatoraja.ir.IRPlayerData player) { return "worker:" + ProcessHandle.current().pid(); }
                    }
                    """, classes);
            Path second = root.resolve("another-ir.jar"); jar(secondClasses, second);
            Path mocha = root.resolve("beatoraja-mocha.jar"); jar(classes, mocha);
            Path core = IRProcessSupport.applicationLocation();
            Path stage = root.resolve("core-stage");
            FatJarContent.prepare(stage, root.resolve("ir"), List.of(core), List.of(), List.of(mocha, second, helper));
            compile(root, stage, "bms/player/beatoraja/ir/BundledLifecycleProbe.java", """
                    package bms.player.beatoraja.ir;
                    public final class BundledLifecycleProbe {
                        public static void test() throws Exception {
                            Process normal = IRProcessSupport.startWorker("--metadata");
                            normal.getOutputStream().close(); normal.getInputStream().readAllBytes();
                            if (!normal.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) || normal.exitValue() != 0) throw new AssertionError("Metadata failed");
                            empty();
                            Process blocked = IRProcessSupport.startWorker("mocha");
                            blocked.destroyForcibly(); blocked.waitFor(10, java.util.concurrent.TimeUnit.SECONDS); empty();
                            String home = System.getProperty("java.home");
                            try {
                                System.setProperty("java.home", java.nio.file.Path.of("missing-jdk").toAbsolutePath().toString());
                                try { IRProcessSupport.startWorker("--metadata"); throw new AssertionError("Invalid Java home accepted"); }
                                catch (java.io.IOException expected) { }
                            } finally { System.setProperty("java.home", home); }
                            empty();
                        }
                        private static void empty() throws Exception {
                            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
                            do {
                                try (var paths = java.nio.file.Files.list(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")))) {
                                    if (paths.noneMatch(p -> p.getFileName().toString().startsWith("beatoraja-ir-"))) return;
                                }
                                Thread.sleep(20);
                            } while (System.nanoTime() < deadline);
                            throw new AssertionError("Worker extraction directory leaked");
                        }
                    }
                    """);
            Path bundled = root.resolve("bundled-core.jar"); jar(stage, bundled);
            Path plain = root.resolve("plain-core.jar"); jar(core, plain);
            Path host = host(root);
            Path externalClasses = root.resolve("external-classes");
            Path base = classes.resolve("bms/player/beatoraja/ir/mocha/FixtureBase.class");
            Path target = externalClasses.resolve("bms/player/beatoraja/ir/mocha/FixtureBase.class");
            Files.createDirectories(target.getParent()); Files.copy(base, target);
            compile(root, externalClasses, "bms/player/beatoraja/ir/ExternalIRConnection.java", """
                    package bms.player.beatoraja.ir;
                    public final class ExternalIRConnection extends bms.player.beatoraja.ir.mocha.FixtureBase {
                        public static final String NAME = "external-fixture";
                        public String getPlayerURL(IRPlayerData player) { return "worker:" + ProcessHandle.current().pid(); }
                    }
                    """);
            Path spoof = externalClasses.resolve(BundledIR.INDEX); Files.createDirectories(spoof.getParent());
            Files.copy(stage.resolve(BundledIR.INDEX), spoof);
            Path external = root.resolve("external.jar"); jar(externalClasses, external);
            var deps = IRProcessSupport.classpaths().application().stream().filter(Files::isRegularFile).toList();
            run(root.resolve("bundled-only"), host, bundled, deps, null, false, "fixture");
            run(root.resolve("coexist"), host, bundled, deps, external, false, "fixture");
            run(root.resolve("legacy"), host, bundled, deps, external, true, "fixture");
            run(root.resolve("lifecycle"), host, bundled, deps, null, false, "lifecycle");
            Path markers = Files.createDirectories(root.resolve("markers"));
            Path marker = markers.resolve(BundledIR.INDEX); Files.createDirectories(marker.getParent());
            Files.copy(spoof, marker);
            Path markerJar = root.resolve("marker-only.jar"); jar(markers, markerJar);
            var spoofedDeps = new ArrayList<Path>(deps); spoofedDeps.add(markerJar);
            run(root.resolve("spoof"), host, plain, spoofedDeps, external, true, "plain");
            System.out.println("BundledIRIntegrationTest passed (opaque JARs, worker-only metadata/login/single-send, external IR isolation/coexistence, index spoof rejection and process exit)");
        } finally { delete(root); }
    }

    /** Original JAR bytes and parent isolation, then metadata RPC. No authentication or score upload. */
    public static void verifyActual(Path fat, List<Path> sources, Path zip) throws Exception {
        var hidden = new java.util.TreeSet<String>();
        try (var jar = new java.util.jar.JarFile(fat.toFile()); var archive = new java.util.zip.ZipFile(zip.toFile())) {
            check((jar.getJarEntry(BundledIR.INDEX) != null) == !sources.isEmpty(), "Bundled index opt-in");
            for (Path source : sources) {
                String name = source.getFileName().toString();
                try (var nested = jar.getInputStream(jar.getJarEntry(BundledIR.ROOT + "jars/" + name))) {
                    check(java.util.Arrays.equals(nested.readAllBytes(), Files.readAllBytes(source)), "Nested JAR bytes changed: " + name);
                }
                try (var input = new java.util.jar.JarFile(source.toFile())) {
                    for (var entry : input.stream().filter(e -> e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")
                            && !e.getName().endsWith("module-info.class") && !e.getName().endsWith("package-info.class")).toList()) {
                        String type = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                        try { Class.forName(type, false, BundledIRIntegrationTest.class.getClassLoader()); }
                        catch (ClassNotFoundException expectedMissing) {
                            hidden.add(type);
                            check(jar.getJarEntry(entry.getName()) == null, "IR/helper classes escaped nested JAR: " + type);
                        }
                    }
                }
                var entry = archive.stream().filter(e -> e.getName().endsWith("/ir/" + name)).findFirst().orElseThrow();
                try (var input = archive.getInputStream(entry)) {
                    check(java.util.Arrays.equals(input.readAllBytes(), Files.readAllBytes(source)), "ZIP IR bytes changed");
                }
                check(archive.stream().noneMatch(e -> e.getName().endsWith("/lib/" + name)), "Thin ZIP put IR on parent lib classpath");
            }
        }
        if (sources.isEmpty()) return;
        Path root = Files.createTempDirectory("actual bundled IR with spaces-");
        try {
            Path list = root.resolve("hidden.txt"); Files.write(list, hidden);
            Path classes = root.resolve("host-classes");
            compile(root, classes, "fixture/ActualBundledIRHost.java", """
                    package fixture;
                    import bms.player.beatoraja.ir.*;
                    public final class ActualBundledIRHost {
                        public static void main(String[] args) throws Exception {
                            IRProcessSupport.ensureParentClasspath(ActualBundledIRHost.class.getName(), args);
                            for (String type : java.nio.file.Files.readAllLines(java.nio.file.Path.of(args[0]))) {
                                try { Class.forName(type, false, ActualBundledIRHost.class.getClassLoader()); throw new AssertionError("Parent loaded IR: " + type); }
                                catch (ClassNotFoundException expected) { }
                            }
                            var names = java.util.Set.of(IRConnectionManager.getAllAvailableIRConnectionName());
                            for (String name : names) {
                                if (!IRConnectionManager.hasIRConnection(name)) throw new AssertionError("Missing IR metadata: " + name);
                                try { IRConnectionManager.getIRConnectionClass(name); throw new AssertionError("Parent Class API exposed"); }
                                catch (IllegalStateException expected) { }
                            }
                            if (names.contains("mocha")) {
                                try (var proxy = (IsolatedIRConnection) IRConnectionManager.getIRConnection("mocha")) {
                                    String url = proxy.getPlayerURL(new IRPlayerData("123", "fixture", ""));
                                    if (url == null || !url.startsWith("https://") || !url.contains("123")) throw new AssertionError("Pure Mocha URL RPC");
                                }
                            }
                            System.out.println("ACTUAL-BUNDLED-IR-OK " + names);
                        }
                    }
                    """);
            Path host = root.resolve("host.jar"); jar(classes, host);
            Path log = root.resolve("host.log");
            Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                    "-cp", host + File.pathSeparator + fat.toAbsolutePath(), "fixture.ActualBundledIRHost", list.toString())
                    .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                check(process.waitFor(45, TimeUnit.SECONDS), "Actual bundled IR host timeout");
                String output = Files.readString(log);
                check(process.exitValue() == 0 && output.contains("ACTUAL-BUNDLED-IR-OK"), "Actual bundled IR failed: " + output);
            } finally {
                process.descendants().forEach(p -> p.destroyForcibly());
                if (process.isAlive()) process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS);
            }
        }
        finally { delete(root); }
        System.out.println("Actual bundled IR passed (original nested/ZIP bytes, no parent IR/helper classes, metadata and pure URL RPC; NO authentication/score upload)");
    }

    private static Path host(Path root) throws Exception {
        Path classes = root.resolve("host-classes");
        compile(root, classes, "fixture/TrustedMochaHost.java", """
                package fixture;
                import bms.player.beatoraja.ir.*;
                public final class TrustedMochaHost {
                    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
                    public static void main(String[] args) throws Exception {
                        IRProcessSupport.ensureParentClasspath(TrustedMochaHost.class.getName(), args);
                        if (args[0].equals("lifecycle")) {
                            Class.forName("bms.player.beatoraja.ir.BundledLifecycleProbe").getMethod("test").invoke(null);
                            System.out.println("MOCHA-HOST-OK"); return;
                        }
                        boolean plain = args[0].equals("plain"), actual = args[0].equals("actual");
                        try { Class.forName("bms.player.beatoraja.ir.ExternalIRConnection"); throw new AssertionError("External IR visible to parent"); }
                        catch (ClassNotFoundException expected) { }
                        try { Class.forName("bms.player.beatoraja.ir.MochaIRConnection", false, TrustedMochaHost.class.getClassLoader());
                            throw new AssertionError("Bundled IR visible to parent"); }
                        catch (ClassNotFoundException expectedMissing) { }
                        for (String type : new String[]{"bms.player.beatoraja.ir.other.SecondIR", "privateapi.WorkerService"}) {
                            try { Class.forName(type); throw new AssertionError("Private class visible to parent: " + type); }
                            catch (ClassNotFoundException expectedMissing) { }
                        }
                        check(TrustedMochaHost.class.getClassLoader().getResource("private-worker-resource.txt") == null, "Private resource leaked");
                        check(java.util.ServiceLoader.load(Runnable.class).stream().findAny().isEmpty(), "Private service provider leaked");
                        var names = java.util.Set.of(IRConnectionManager.getAllAvailableIRConnectionName());
                        var expected = new java.util.HashSet<String>();
                        if (!plain) { expected.add("mocha"); if (!actual) expected.add("second-bundled"); }
                        if (Boolean.getBoolean("fixture.external")) expected.add("external-fixture");
                        check(names.equals(expected), "IR names: " + names + " expected " + expected);
                        check(System.getProperty("fixture.mocha.initialized") == null, "Parent initialized Mocha metadata");
                        try { IRConnectionManager.getIRConnectionClass("mocha"); throw new AssertionError("Parent Class API exposed"); }
                        catch (IllegalStateException expectedFailure) { }
                        if (!plain) {
                            check(IRConnectionManager.getHomeURL("mocha").equals(actual ? "https://mocha-repository.info/" : "https://example.invalid/mocha"), "Mocha HOME");
                            try (var proxy = (IsolatedIRConnection) IRConnectionManager.getIRConnection("mocha")) {
                                if (!actual) {
                                    check(proxy.login(new IRAccount("fixture", "unused", "fixture")).isSucceeded(), "Fixture login");
                                    check(proxy.sendPlayData(null, null).isSucceeded(), "Fixture send");
                                }
                                String url = proxy.getPlayerURL(new IRPlayerData("123", "fixture", ""));
                                if (actual) check(url != null && url.startsWith("https://") && url.contains("123"), "Actual pure Mocha URL");
                                else check(Long.parseLong(url.substring(7)) != ProcessHandle.current().pid(), "Mocha executed in parent");
                            }
                        }
                        if (Boolean.getBoolean("fixture.external")) {
                            try (var proxy = (IsolatedIRConnection) IRConnectionManager.getIRConnection("external-fixture")) {
                                String url = proxy.getPlayerURL(null);
                                check(Long.parseLong(url.substring(7)) != ProcessHandle.current().pid(), "External IR executed in parent");
                            }
                        }
                        if (!plain && !actual) {
                            try (var proxy = (IsolatedIRConnection) IRConnectionManager.getIRConnection("second-bundled")) {
                                String url = proxy.getPlayerURL(null);
                                check(Long.parseLong(url.substring(7)) != ProcessHandle.current().pid(), "Second bundled IR executed in parent");
                            }
                        }
                        check(System.getProperty("fixture.mocha.initialized") == null, "Parent initialized Mocha during RPC");
                        System.out.println("MOCHA-HOST-OK");
                    }
                }
                """);
        Path jar = root.resolve("host.jar"); jar(classes, jar); return jar;
    }

    private static void run(Path root, Path host, Path core, List<Path> deps, Path external, boolean legacy, String mode) throws Exception {
        Files.createDirectories(root);
        Files.createDirectory(root.resolve("tmp"));
        var cp = new ArrayList<String>(List.of(host.toString(), core.toString())); deps.forEach(p -> cp.add(p.toString()));
        if (legacy && external != null) cp.add(external.toString());
        var command = new ArrayList<String>(List.of(Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                "-Dfixture.events=" + root.resolve("events.txt"), "-Dfixture.external=" + (external != null),
                "-Djava.io.tmpdir=" + root.resolve("tmp")));
        if (external != null && !legacy) command.add("-D" + IRProcessSupport.PLUGIN_CLASSPATH + "=" + external);
        command.addAll(List.of("-cp", String.join(File.pathSeparator, cp), "fixture.TrustedMochaHost", mode));
        Path log = root.resolve("host.log");
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            check(process.waitFor(40, TimeUnit.SECONDS), "Mocha host timed out: " + log);
            String output = Files.readString(log);
            check(process.exitValue() == 0 && output.contains("MOCHA-HOST-OK"), "Mocha integration failed: " + output);
            check(!output.contains("IRConnection\u8aad\u307f\u8fbc\u307f\u5931\u6557"), "Unexpected DTO/class loading warning: " + output);
            if (mode.equals("fixture")) {
                var events = Files.readAllLines(root.resolve("events.txt"));
                check(events.stream().filter(line -> line.startsWith("mocha-init:")).count() == (external == null ? 3 : 4),
                        "Only metadata and RPC workers initialize Mocha: " + events);
                check(events.stream().filter(line -> line.startsWith("mocha-constructor:")).count() == 1, "Metadata must not construct Mocha");
                check(events.stream().filter(line -> line.startsWith("mocha-send:")).count() == 1, "No duplicate score send");
                for (String event : events) {
                    long pid = Long.parseLong(event.substring(event.indexOf(':') + 1));
                    var handle = ProcessHandle.of(pid);
                    if (handle.isPresent() && handle.get().isAlive()) handle.get().onExit().get(5, TimeUnit.SECONDS);
                }
            }
        } finally {
            process.descendants().forEach(child -> child.destroyForcibly());
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static void compile(Path root, Path classes, String name, String source, Path... extra) throws Exception {
        Path file = root.resolve("sources").resolve(name); Files.createDirectories(file.getParent()); Files.createDirectories(classes);
        Files.writeString(file, source);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-cp",
                classes + File.pathSeparator + System.getProperty("java.class.path")
                + (extra.length == 0 ? "" : File.pathSeparator + java.util.Arrays.stream(extra).map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator))),
                "-d", classes.toString(), file.toString());
        check(result == 0, "Fixture compilation failed: " + name);
    }
    private static void jar(Path classes, Path path) throws Exception {
        try (var out = new JarOutputStream(Files.newOutputStream(path)); var files = Files.walk(classes)) {
            for (var file : files.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/'))); Files.copy(file, out); out.closeEntry();
            }
        }
    }
    private static void delete(Path root) throws Exception {
        try (var files = Files.walk(root)) { for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
