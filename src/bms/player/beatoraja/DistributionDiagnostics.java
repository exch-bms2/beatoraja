package bms.player.beatoraja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Opt-in process evidence; deliberately excludes arguments, configuration and credentials. */
public final class DistributionDiagnostics {
    private static boolean started;

    private DistributionDiagnostics() { }

    public static synchronized void start(String role) {
        if (!Boolean.getBoolean("beatoraja.distributionDiagnostics") || started) return;
        started = true;
        event("start role=" + role + " parent=" + ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(-1L)
                + " os=" + System.getProperty("os.name") + " arch=" + System.getProperty("os.arch")
                + " java=" + System.getProperty("java.version") + " home=" + System.getProperty("java.home")
                + " cwd=" + System.getProperty("user.dir"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> event("shutdown-hook (not proof of process exit)"),
                "distribution-diagnostics-shutdown"));
    }

    public static synchronized void event(String message) {
        if (!Boolean.getBoolean("beatoraja.distributionDiagnostics")) return;
        String line = Instant.now() + " pid=" + ProcessHandle.current().pid() + " " + message + System.lineSeparator();
        System.err.print(line);
        try {
            Path directory = Path.of("diagnostics");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("process-" + ProcessHandle.current().pid() + ".log"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            System.err.println("Distribution diagnostics unavailable: " + e);
        }
    }

    static Process observe(Process child, String role) {
        if (Boolean.getBoolean("beatoraja.distributionDiagnostics")) {
            event("child-start role=" + role + " child=" + child.pid());
            child.onExit().thenAccept(exited -> event("child-exit role=" + role + " child=" + exited.pid()
                    + " exit=" + exited.exitValue()));
        }
        return child;
    }
}
