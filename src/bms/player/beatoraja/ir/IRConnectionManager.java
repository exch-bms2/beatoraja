package bms.player.beatoraja.ir;

import bms.player.beatoraja.StartupDiagnostics;
import bms.player.beatoraja.StartupDiagnostics.Outcome;
import bms.player.beatoraja.StartupDiagnostics.Step;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/** Metadata-only parent facade. Plugin classes are discovered and initialized in a short-lived worker. */
public class IRConnectionManager {
    private static IRWorkerProtocol.Metadata[] connections;

    public static String[] getAllAvailableIRConnectionName() {
        if (IRWorkerDiscovery.isWorker()) return IRWorkerDiscovery.getAllAvailableIRConnectionName();
        return Arrays.stream(metadata()).map(value -> value.name).toArray(String[]::new);
    }

    public static boolean hasIRConnection(String name) {
        if (IRWorkerDiscovery.isWorker()) return IRWorkerDiscovery.getIRConnectionClass(name) != null;
        return name != null && Arrays.stream(metadata()).anyMatch(value -> name.equals(value.name));
    }

    public static IRConnection getIRConnection(String name) { return getIsolatedIRConnection(name); }

    public static IRConnection getIsolatedIRConnection(String name) {
        return hasIRConnection(name) ? new IsolatedIRConnection(name) : null;
    }

    static IRConnection getIRConnectionInProcess(String name) {
        return IRWorkerDiscovery.getIRConnectionInProcess(name);
    }

    /**
     * Retained for binary compatibility inside legacy IR workers. Parent callers must use
     * {@link #hasIRConnection(String)} instead; returning a plugin Class would break isolation.
     */
    @Deprecated
    public static Class<IRConnection> getIRConnectionClass(String name) {
        return IRWorkerDiscovery.getIRConnectionClass(name);
    }

    public static String getHomeURL(String name) {
        if (IRWorkerDiscovery.isWorker()) return IRWorkerDiscovery.getHomeURL(name);
        for (IRWorkerProtocol.Metadata metadata : metadata()) {
            if (metadata.name.equals(name)) return metadata.home;
        }
        return null;
    }

    private static synchronized IRWorkerProtocol.Metadata[] metadata() {
        if (connections == null) {
            try (var timing = StartupDiagnostics.measure(Step.IR_METADATA)) {
                if (IRProcessSupport.classpaths().plugins().isEmpty() && !BundledIR.isPresent()) {
                    connections = new IRWorkerProtocol.Metadata[0];
                    timing.outcome(Outcome.SKIPPED);
                } else {
                    connections = discover(30_000);
                    timing.outcome(Outcome.OK);
                }
            } catch (IOException | RuntimeException e) {
                Logger.getGlobal().warning("IR metadata discovery failed : " + e.getMessage());
                connections = new IRWorkerProtocol.Metadata[0];
            }
        }
        return connections;
    }

    static IRWorkerProtocol.Metadata[] discover(long timeoutMillis) throws IOException {
        Process process;
        try (var timing = StartupDiagnostics.measure(Step.IR_METADATA_WORKER_START)) {
            process = IRProcessSupport.startWorker("--metadata");
            timing.worker(process.pid());
            timing.outcome(Outcome.OK);
        }
        var reader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ir-metadata-reader");
            thread.setDaemon(true);
            return thread;
        });
        try {
            process.getOutputStream().close();
            var result = reader.submit(() -> {
                // Bound discovery independently of potentially large rival score responses.
                byte[] bytes = process.getInputStream().readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) throw new IOException("IR metadata exceeds 1 MiB");
                return new String(bytes, StandardCharsets.UTF_8);
            });
            String output;
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            try (var response = StartupDiagnostics.measure(Step.IR_METADATA_RESPONSE_WAIT, process.pid())) {
                try { output = result.get(timeoutMillis, TimeUnit.MILLISECONDS); }
                catch (TimeoutException e) { response.outcome(Outcome.TIMEOUT); throw e; }
                catch (InterruptedException e) { response.outcome(Outcome.INTERRUPTED); throw e; }
                response.outcome(Outcome.OK);
            }
            try (var exit = StartupDiagnostics.measure(Step.IR_METADATA_EXIT_WAIT, process.pid())) {
                boolean exited;
                try { exited = process.waitFor(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS); }
                catch (InterruptedException e) { exit.outcome(Outcome.INTERRUPTED); throw e; }
                if (!exited) exit.outcome(Outcome.TIMEOUT);
                if (!exited || process.exitValue() != 0)
                    throw new IOException("IR metadata worker failed");
                exit.outcome(Outcome.OK);
            }
            IRWorkerProtocol.Discovery discovery = IsolatedIRConnection.createJson().fromJson(IRWorkerProtocol.Discovery.class, output);
            if (discovery == null || discovery.connections == null) throw new IOException("Missing IR metadata");
            return Arrays.stream(discovery.connections).filter(value -> value != null && value.name != null && !value.name.isBlank())
                    .toArray(IRWorkerProtocol.Metadata[]::new);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("IR metadata discovery interrupted", e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new IOException("IR metadata discovery failed", e);
        } finally {
            try (var cleanup = StartupDiagnostics.measure(Step.IR_METADATA_CLEANUP, process.pid())) {
                process.descendants().forEach(child -> child.destroyForcibly());
                if (process.isAlive()) process.destroyForcibly();
                try { process.waitFor(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                reader.shutdownNow();
                try { process.getInputStream().close(); process.getErrorStream().close(); }
                catch (IOException ignored) { }
                try { reader.awaitTermination(1, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                cleanup.outcome(Thread.currentThread().isInterrupted() ? Outcome.INTERRUPTED
                        : process.isAlive() || !reader.isTerminated() ? Outcome.TIMEOUT : Outcome.OK);
            }
        }
    }
}
