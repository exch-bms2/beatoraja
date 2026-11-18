package bms.player.beatoraja;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class StartupDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            StartupDiagnostics.gameEntry();
            StartupDiagnostics.selectionFrame(false);
            StartupDiagnostics.selectionFrame(true);
            StartupDiagnostics.selectionFrame(true);
            return;
        }
        var events = new ArrayList<String>();
        var logger = Logger.getGlobal();
        var capture = new Handler() {
            public void publish(LogRecord record) { events.add(record.getMessage()); }
            public void flush() { }
            public void close() { }
        };
        String property = StartupDiagnostics.PROPERTY;
        String previous = System.getProperty(property);
        logger.addHandler(capture);
        try {
            System.clearProperty(property);
            var env = new HashMap<String, String>();
            StartupDiagnostics.start();
            StartupDiagnostics.configureGameEnvironment(env);
            try (var ignored = StartupDiagnostics.measure(StartupDiagnostics.Step.IR_METADATA)) { }
            check(events.isEmpty() && env.isEmpty(), "Default-off must not log or change the environment");
            System.setProperty(property, "true");
            StartupDiagnostics.start();
            try (var span = StartupDiagnostics.measure(StartupDiagnostics.Step.IR_LOGIN)) {
                span.outcome(StartupDiagnostics.Outcome.TIMEOUT);
            }
            check(events.get(1).contains("step=IR_LOGIN") && events.get(1).contains("outcome=TIMEOUT durationUs="), "Timed result");
            int before = events.size();
            try (var ignored = StartupDiagnostics.measure(StartupDiagnostics.Step.SELECT_SKIN_LOAD, false)) { }
            check(events.size() == before, "Later selection visits must not log startup spans");
            var child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-D" + property + "=true", "-cp", System.getProperty("java.class.path"),
                    StartupDiagnosticsTest.class.getName(), "child").redirectErrorStream(true);
            StartupDiagnostics.configureGameEnvironment(child.environment());
            var process = child.start();
            try {
                check(process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0, "Diagnostic child must exit");
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String trace = events.get(0).split("trace=")[1].split(" ")[0];
                check(output.contains("trace=" + trace + " sinceStartMs=") && output.contains("step=GAME_ENTRY"),
                        "Child correlates its timings with the parent's START");
                int frame = output.indexOf("step=SELECT_FIRST_FRAME");
                int input = output.indexOf("step=SELECT_INPUT_READY");
                check(frame >= 0 && input > frame && input == output.lastIndexOf("step=SELECT_INPUT_READY"),
                        "First frame and later input unlock recorded once and in order");
            } finally { if (process.isAlive()) process.destroyForcibly(); }
            check(StartupDiagnostics.readLaunch("untrusted text", "invalid") == null, "Invalid inherited context is ignored");
        } finally {
            logger.removeHandler(capture);
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
        }
        System.out.println("StartupDiagnosticsTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
