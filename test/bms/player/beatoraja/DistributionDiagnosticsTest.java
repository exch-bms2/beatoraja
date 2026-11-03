package bms.player.beatoraja;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Headless child-process regression; no desktop/native device initialization. */
public final class DistributionDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("leaf")) { System.exit(7); }
        if (args.length > 0 && args[0].equals("fixture")) {
            DistributionDiagnostics.start("fixture");
            DistributionDiagnostics.start("duplicate");
            var child = DistributionDiagnostics.observe(new ProcessBuilder(java(), "-cp",
                    System.getProperty("java.class.path"), DistributionDiagnosticsTest.class.getName(), "leaf").start(), "fixture-child");
            if (Boolean.getBoolean("beatoraja.distributionDiagnostics.awaitChildren")) System.exit(0);
            if (!child.waitFor(10, TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("Child timeout"); }
            if (Boolean.getBoolean("beatoraja.distributionDiagnostics")) {
                Path log = Path.of("diagnostics/process-" + ProcessHandle.current().pid() + ".log");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!Files.readString(log).contains("exit=7") && System.nanoTime() < deadline) Thread.sleep(10);
                check(Files.readString(log).contains("exit=7"), "Observe actual exit code");
            }
            return;
        }
        for (int mode = 0; mode < 3; mode++) {
            boolean enabled = mode > 0, await = mode == 2;
            Path root = Files.createTempDirectory("distribution-diagnostics-test-");
            Process process = new ProcessBuilder(java(), "-Dbeatoraja.distributionDiagnostics=" + enabled,
                    "-Dbeatoraja.distributionDiagnostics.awaitChildren=" + await,
                    "-cp", System.getProperty("java.class.path"), DistributionDiagnosticsTest.class.getName(), "fixture")
                    .directory(root.toFile()).redirectErrorStream(true).redirectOutput(root.resolve("console.log").toFile()).start();
            try {
                check(process.waitFor(20, TimeUnit.SECONDS), "Fixture timeout");
                check(process.exitValue() == 0 && !process.isAlive(), "Fixture exited");
                Path log = root.resolve("diagnostics/process-" + process.pid() + ".log");
                check(Files.exists(log) == enabled, "Opt-in only");
                if (enabled) {
                    String text = Files.readString(log);
                    check(text.contains("start role=fixture") && !text.contains("role=duplicate"), "Start once");
                    check(text.contains("child-start") && text.contains(await ? "child-wait-complete role=fixture-child" : "child-exit")
                            && text.contains("exit=7") && text.contains("shutdown-hook"), "Lifecycle evidence and actual child status");
                }
            } finally { if (process.isAlive()) process.destroyForcibly().waitFor(); }
        }
        check(PackagedRuntimeProbe.check("fake-success", () -> {}) == 0, "Successful probe");
        check(PackagedRuntimeProbe.check("fake-link-failure", () -> { throw new UnsatisfiedLinkError("injected"); }) == 1, "Native failure reported");
        var command = DesktopLauncher.gameCommand("/jdk", "classpath", "Mac OS X",
                List.of("-Dbeatoraja.distributionDiagnostics=true"));
        check(command.contains("-Dbeatoraja.distributionDiagnostics=true"), "Diagnostic opt-in inherited");
        System.out.println("DistributionDiagnosticsTest passed (opt-in, child exit, shutdown evidence, failed native probe)");
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
