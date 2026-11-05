package bms.player.beatoraja.ir;

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
            try {
                if (IRProcessSupport.classpaths().plugins().isEmpty() && !BundledIR.isPresent()) connections = new IRWorkerProtocol.Metadata[0];
                else connections = discover(30_000);
            } catch (IOException | RuntimeException e) {
                Logger.getGlobal().warning("IR metadata discovery failed : " + e.getMessage());
                connections = new IRWorkerProtocol.Metadata[0];
            }
        }
        return connections;
    }

    static IRWorkerProtocol.Metadata[] discover(long timeoutMillis) throws IOException {
        Process process = IRProcessSupport.startWorker("--metadata");
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
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            String output = result.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!process.waitFor(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS) || process.exitValue() != 0)
                throw new IOException("IR metadata worker failed");
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
            process.descendants().forEach(child -> child.destroyForcibly());
            if (process.isAlive()) process.destroyForcibly();
            try { process.waitFor(5, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            reader.shutdownNow();
            try { process.getInputStream().close(); process.getErrorStream().close(); }
            catch (IOException ignored) { }
            try { reader.awaitTermination(1, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
