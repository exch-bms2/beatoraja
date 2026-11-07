package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Supervises the real extracted runtime. The observer agent is separate and contains no production classes. */
public final class PackagedGameIntegrationTest {
    public static void main(String[] args) throws Exception {
        PackagedGameSupport.check(args.length == 4 || args.length == 5,
                "Expected distribution ZIP, Java executable, test agent JAR, output directory and optional Fat JAR");
        Path output = Path.of(args[3]).toAbsolutePath(); Files.createDirectories(output);
        Path run = Files.createTempDirectory(output, "zip game runtime with spaces-");
        Path root = PackagedGameSupport.extract(Path.of(args[0]), run.resolve("extracted"));
        Path fat = args.length == 5 ? PackagedGameSupport.useFatJar(root, Path.of(args[4])) : null;
        verifyAgent(Path.of(args[2]));
        PackagedGameSupport.fixture(root);
        // Exercise persisted selection and the missing-monitor fallback through the actual Launcher and game JVMs.
        var json = new com.badlogic.gdx.utils.Json();
        var configPath = root.resolve("config_sys.json");
        var config = json.fromJson(bms.player.beatoraja.Config.class, Files.readString(configPath));
        String missingMonitor = "beatoraja-packaged-test-disconnected-monitor";
        config.setStartupMonitor(new bms.player.beatoraja.Config.MonitorSelection(missingMonitor, -50000, 0));
        Files.writeString(configPath, json.toJson(config));
        Path diagnostics = Files.createDirectories(output.resolve("diagnostics").resolve(run.getFileName()));
        Files.writeString(output.resolve("diagnostics/latest.txt"), run.getFileName().toString() + "\n");
        for (String name : new String[]{"home", "tmp"}) Files.createDirectory(root.resolve(name));
        var command = new java.util.ArrayList<String>();
        command.add(args[1]); command.add("-Xmx768m");
        command.add("-Dbeatoraja.distributionDiagnostics=true");
        command.add("-Dbeatoraja.distributionDiagnostics.awaitChildren=true");
        command.add("-Dbeatoraja.packagedGameTest.requireMesa=" + Boolean.getBoolean("beatoraja.glTest.requireMesa"));
        command.add("-Duser.home=" + root.resolve("home")); command.add("-Djava.io.tmpdir=" + root.resolve("tmp"));
        command.add("-Djava.library.path=./natives");
        command.add("-XX:ErrorFile=" + root.resolve("test-evidence/hs_err_pid%p.log"));
        if (fat == null) command.addAll(java.util.List.of("-cp", "lib/*", "bms.player.beatoraja.DesktopLauncher"));
        else command.addAll(java.util.List.of("-jar", root.relativize(fat).toString()));
        var builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(diagnostics.resolve("launcher-game.log").toFile());
        var environment = builder.environment();
        // Ignore developer JVM injection options; inherit only this explicit test agent.
        environment.remove("JDK_JAVA_OPTIONS"); environment.remove("_JAVA_OPTIONS");
        environment.put("JAVA_TOOL_OPTIONS", "-javaagent:" + quoted(Path.of(args[2]).toAbsolutePath())
                + " -Dbeatoraja.packagedGameTest.root=" + quoted(root));
        environment.put("ALSOFT_DRIVERS", "null");
        for (String name : new String[]{"HOME", "USERPROFILE"}) environment.put(name, root.resolve("home").toString());
        for (String name : new String[]{"TMPDIR", "TMP", "TEMP"}) environment.put(name, root.resolve("tmp").toString());
        Process launcher = null;
        Map<Long, ProcessHandle> observed = new LinkedHashMap<>();
        boolean passed = false;
        try {
            launcher = builder.start(); observed.put(launcher.pid(), launcher.toHandle());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(150);
            while (launcher.isAlive() && System.nanoTime() < deadline) {
                observe(launcher.toHandle(), observed); discover(root, observed); Thread.sleep(100);
            }
            PackagedGameSupport.check(!launcher.isAlive(), "Launcher/game timeout; no successful exit evidence");
            PackagedGameSupport.check(launcher.exitValue() == 0, "Actual Launcher exit=" + launcher.exitValue());
            discover(root, observed);
            validate(root, launcher.pid());
            if (config.getControllerBackend() == bms.player.beatoraja.input.controller.ControllerBackend.SDL) {
                String gameLog = Files.readString(diagnostics.resolve("launcher-game.log"));
                PackagedGameSupport.check(gameLog.contains("Controller backend=SDL/Jamepad, devices=")
                        && !gameLog.contains("SDL controller initialization failed")
                        && gameLog.contains("SDL controller native shutdown complete"), "SDL native initialization/update/shutdown was not verified");
                PackagedGameSupport.check(Files.readString(root.resolve("test-evidence/game-runtime.txt")).contains("controllerBackend=SDL"), "Game lost SDL selection");
            }
            if (config.getControllerBackend().bindingBackend() == bms.player.beatoraja.input.controller.ControllerBackend.SDL_RAW
                    || config.getControllerBackend() == bms.player.beatoraja.input.controller.ControllerBackend.SDL_ASYNC) {
                String backend = config.getControllerBackend().name();
                String gameLog = Files.readString(diagnostics.resolve("launcher-game.log"));
                PackagedGameSupport.check(gameLog.contains("Controller backend=" + backend + ", devices=")
                        && !gameLog.contains(backend + " initialization failed")
                        && !gameLog.contains(backend + " polling failed")
                        && gameLog.contains(backend + " controller native shutdown complete"), "SDL3 controller native initialization/update/shutdown was not verified");
                PackagedGameSupport.check(Files.readString(root.resolve("test-evidence/game-runtime.txt")).contains("controllerBackend=" + backend), "Game lost SDL_RAW selection");
                if (config.getControllerBackend() != bms.player.beatoraja.input.controller.ControllerBackend.SDL_RAW)
                    PackagedGameSupport.check(gameLog.contains(backend + " input polling started: thread=Input polling"),
                            "SDL3 async backend did not acquire on the real input worker");
            }
            PackagedGameSupport.check(Files.readString(diagnostics.resolve("launcher-game.log"))
                    .contains("Startup monitor unavailable or ambiguous: " + missingMonitor), "Missing monitor fallback was not exercised");
            var savedConfig = json.fromJson(bms.player.beatoraja.Config.class, Files.readString(configPath));
            PackagedGameSupport.check(savedConfig.getStartupMonitor() != null
                    && missingMonitor.equals(savedConfig.getStartupMonitor().name), "Launcher erased disconnected monitor preference");
            PackagedGameSupport.check(observed.values().stream().noneMatch(ProcessHandle::isAlive), "Descendant process still alive after normal exit");
            String fontWarnings = Files.readString(diagnostics.resolve("launcher-game.log")).lines()
                    .filter(line -> line.contains("Skin Font load failed")).collect(java.util.stream.Collectors.joining("\n"));
            if (!fontWarnings.isEmpty()) {
                Files.writeString(diagnostics.resolve("known-font-warnings.txt"), fontWarnings + "\n");
                System.out.println("WARNING: lifecycle passed, but bundled default-skin font paths failed; see known-font-warnings.txt. Text completeness is NOT certified.");
            }
            passed = true;
            System.out.println("PackagedGameIntegrationTest passed: actual packaged JavaFX START -> MainController SELECT/PLAY/RESULT/SELECT -> exit; input stopped/devices released; child OS exit code 0");
        } finally {
            if (launcher != null) { observe(launcher.toHandle(), observed); discover(root, observed); }
            if (!passed) for (var handle : observed.values()) if (handle.isAlive()) dump(args[1], handle, diagnostics);
            var ledger = new StringBuilder("status=" + (passed ? "PASS" : "FAIL") + "\n");
            for (var handle : observed.values()) ledger.append("pid=").append(handle.pid()).append(" aliveBeforeCleanup=").append(handle.isAlive()).append('\n');
            // Only terminate the processes belonging to this supervised run, never unrelated Java processes.
            for (var handle : observed.values()) if (handle.isAlive()) handle.destroy();
            for (var handle : observed.values()) if (handle.isAlive()) {
                try { handle.onExit().get(3, TimeUnit.SECONDS); } catch (Exception ignored) { handle.destroyForcibly(); }
            }
            for (var handle : observed.values()) if (handle.isAlive()) {
                try { handle.onExit().get(3, TimeUnit.SECONDS); } catch (Exception ignored) { }
            }
            for (var handle : observed.values()) ledger.append("pid=").append(handle.pid()).append(" aliveAfterCleanup=").append(handle.isAlive()).append('\n');
            Files.writeString(diagnostics.resolve("processes.txt"), ledger);
            copyEvidence(root, diagnostics);
        }
    }

    static String quoted(Path path) {
        String value = path.toString();
        PackagedGameSupport.check(!value.contains("\"") && !value.contains("\n") && !value.contains("\r"), "Unsafe JVM option path");
        return "\"" + value + "\"";
    }
    static void verifyAgent(Path file) throws Exception {
        try (var jar = new java.util.jar.JarFile(file.toFile())) {
            PackagedGameSupport.check(jar.getManifest() != null && "bms.player.beatoraja.build.PackagedGameAgent".equals(
                    jar.getManifest().getMainAttributes().getValue("Premain-Class")), "Wrong test agent entry point");
            PackagedGameSupport.check(jar.stream().filter(e -> e.getName().endsWith(".class")).allMatch(e ->
                    e.getName().startsWith("bms/player/beatoraja/build/PackagedGameAgent")
                    || e.getName().equals("bms/player/beatoraja/build/PackagedGameSupport.class")), "Agent must not contain production replacements or other test classes");
            PackagedGameSupport.check(jar.getEntry("bms/player/beatoraja/build/PackagedGameAgent.class") != null
                    && jar.getEntry("bms/player/beatoraja/build/PackagedGameSupport.class") != null, "Agent classes missing");
        }
    }
    private static void observe(ProcessHandle root, Map<Long, ProcessHandle> observed) {
        try (var descendants = root.descendants()) { descendants.forEach(p -> observed.putIfAbsent(p.pid(), p)); }
    }
    private static void discover(Path root, Map<Long, ProcessHandle> observed) throws Exception {
        Path file = root.resolve("test-evidence/game.pid");
        if (Files.exists(file)) {
            String text = Files.readString(file).trim();
            if (text.matches("[0-9]+")) ProcessHandle.of(Long.parseLong(text)).ifPresent(p -> observed.putIfAbsent(p.pid(), p));
        }
    }
    static void validate(Path root, long launcherPid) throws Exception {
        Path evidence = root.resolve("test-evidence");
        long gamePid = Long.parseLong(Files.readString(evidence.resolve("game.pid")).trim());
        PackagedGameSupport.check(gamePid != launcherPid, "Game must use a separate JVM");
        for (String file : new String[]{"launcher.png", "select-before.png", "play.png", "result.png", "select-after.png"}) {
            try (var in = Files.newInputStream(evidence.resolve(file))) {
                PackagedGameSupport.check(java.util.Arrays.equals(in.readNBytes(8), new byte[]{(byte)137,80,78,71,13,10,26,10}), "PNG missing/invalid: " + file);
            }
        }
        PackagedGameSupport.check(Files.isRegularFile(evidence.resolve("launcher-ready.txt")) && Files.isRegularFile(evidence.resolve("game-ready.txt")), "Real UI/state milestones missing");
        try (var files = Files.list(evidence)) {
            PackagedGameSupport.check(files.noneMatch(p -> p.getFileName().toString().startsWith("agent-failure-")
                    || p.getFileName().toString().startsWith("hs_err_pid")), "Agent failure/native crash evidence present");
        }
        var report = new Properties();
        try (var input = Files.newInputStream(evidence.resolve("shutdown.properties"))) { report.load(input); }
        PackagedGameSupport.check("true".equals(report.getProperty("inputObservedRunning"))
                && "false".equals(report.getProperty("inputThreadAlive")) && "true".equals(report.getProperty("devicesReleased"))
                && "true".equals(report.getProperty("gameSequenceComplete")), "Input cleanup or real sequence not verified: " + report);
        String parent = Files.readString(root.resolve("diagnostics/process-" + launcherPid + ".log"));
        PackagedGameSupport.check(parent.contains("child-wait-complete role=game child=" + gamePid + " exit=0"), "Actual child OS exit code 0 not observed by Launcher");
        String game = Files.readString(root.resolve("diagnostics/process-" + gamePid + ".log"));
        PackagedGameSupport.check(game.contains("game-create-complete") && game.contains("game-dispose-complete"), "Normal MainController create/dispose missing");
    }
    private static void dump(String java, ProcessHandle handle, Path output) {
        Process command = null;
        try {
            Path jcmd = Path.of(java).resolveSibling(System.getProperty("os.name").startsWith("Windows") ? "jcmd.exe" : "jcmd");
            command = new ProcessBuilder(jcmd.toString(), Long.toString(handle.pid()), "Thread.print")
                    .redirectErrorStream(true).redirectOutput(output.resolve("jcmd-" + handle.pid() + ".txt").toFile()).start();
            if (!command.waitFor(5, TimeUnit.SECONDS)) command.destroyForcibly();
        } catch (Exception error) { System.err.println("jcmd unavailable for test PID " + handle.pid() + ": " + error); }
        finally { if (command != null && command.isAlive()) try { command.destroyForcibly().waitFor(3, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }
    private static void copyEvidence(Path root, Path output) throws Exception {
        for (String folder : new String[]{"test-evidence", "diagnostics"}) {
            Path source = root.resolve(folder);
            if (!Files.isDirectory(source)) continue;
            try (var files = Files.list(source)) {
                for (Path file : files.filter(Files::isRegularFile).toList())
                    Files.copy(file, output.resolve(folder + "-" + file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        for (String file : new String[]{"beatoraja_log.xml", "beatoraja_launcher_log.xml"})
            if (Files.isRegularFile(root.resolve(file))) Files.copy(root.resolve(file), output.resolve(file), StandardCopyOption.REPLACE_EXISTING);
    }
}
