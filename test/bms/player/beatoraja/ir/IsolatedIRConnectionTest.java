package bms.player.beatoraja.ir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Actual child JVM/pipe regressions, no network, IR accounts, GUI or production data. */
public final class IsolatedIRConnectionTest {
    public static void main(String[] args) throws Exception {
        for (String fault : List.of("crash", "timeout")) {
            try (var f = new Fixture(fault)) {
                f.login();
                check(!f.connection.getRivals().isSucceeded(), "Fault must fail, not replay");
                check(f.connection.getTableDatas().isSucceeded(), "Fresh worker must reauthenticate");
                check(f.count("login:user-a") == 2 && f.count("rivals") == 1 && f.count("tables") == 1, "Login before API, no RPC replay");
                var scores = f.connection.getPlayDataSince(null, 0);
                check(scores.isSucceeded() && scores.getScoreRevision() == -1, "Legacy optional-API fallback");
            }
        }
        try (var f = new Fixture("normal")) {
            f.login(); f.processes.get(0).destroyForcibly().waitFor();
            check(f.connection.sendPlayData(null, null).isSucceeded(), "Idle worker death also restores login");
            check(f.count("login:user-a") == 2 && f.count("send") == 1, "Submission sent once after authentication");
            check(f.count("plugin-jar") == 2, "Worker loads the legacy-method fixture from a real plugin JAR");
        }
        try (var f = new Fixture("normal")) {
            f.login();
            check(f.connection.login(new IRAccount("user-b", "replacement-secret", "")).isSucceeded(), "Explicit account change");
            f.processes.get(0).destroyForcibly().waitFor();
            check(f.connection.getTableDatas().isSucceeded() && f.count("login:user-b") == 2 && f.count("login:user-a") == 1,
                    "Restore only the latest successful account");
        }
        try (var f = new Fixture("fail-reauth")) {
            f.login(); f.connection.getRivals();
            var result = f.connection.sendPlayData(null, null);
            check(!result.isSucceeded() && !IRSubmissionResponse.isOutcomeUnknown(result), "Failed re-login must not submit");
            check(f.count("send") == 0, "No unauthenticated upload");
            check(!f.connection.getTableDatas().isSucceeded() && f.count("tables") == 0, "Rejected authentication remains required");
        }
        try (var f = new Fixture("accept-hang")) {
            f.login();
            var result = f.connection.sendPlayData(null, null);
            check(!result.isSucceeded() && IRSubmissionResponse.isOutcomeUnknown(result), "Accepted then timed out is ambiguous");
            check(f.connection.getTableDatas().isSucceeded(), "Next unrelated call can recover");
            check(f.count("send") == 1 && f.count("login:user-a") == 2, "Never automatically resend the timed-out score");
        }
        try (var f = new Fixture("negative-response")) {
            f.login();
            check(IRSubmissionResponse.isOutcomeUnknown(f.connection.sendPlayData(null, null)), "Legacy false can hide HTTP timeout");
            check(IRSubmissionResponse.isOutcomeUnknown(f.connection.sendCoursePlayData(null, null)), "Course submissions use the same policy");
        }
        try (var f = new Fixture("timeout")) {
            f.login(); var executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> waiting = executor.submit(f.connection::getRivals);
                await(() -> f.count("rivals") > 0);
                long start = System.nanoTime(); f.connection.close();
                check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 4000, "Close cannot wait for the request monitor or live read lock");
                waiting.get(4, TimeUnit.SECONDS);
                check(!f.connection.login(new IRAccount("other", "unused", "")).isSucceeded() && f.processes.size() == 1, "Closed connection never reopens");
            } finally { executor.shutdownNow(); executor.awaitTermination(4, TimeUnit.SECONDS); }
        }
        try (var f = new Fixture("no-read")) {
            long start = System.nanoTime();
            check(!f.connection.getVersionInfo("x".repeat(4 * 1024 * 1024)).isSucceeded(), "Blocked pipe write times out too");
            check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 7000, "Write must be inside the timeout");
        }
        try (var f = new Fixture("malformed")) {
            check(IRSubmissionResponse.isOutcomeUnknown(f.connection.sendPlayData(null, null)), "Broken response after send must not be replayed");
        }
        try (var f = new Fixture("normal")) {
            f.login(); var executor = Executors.newFixedThreadPool(4);
            try {
                var calls = new ArrayList<Future<Boolean>>();
                for (int i = 0; i < 12; i++) {
                    boolean rivals = i % 2 == 0;
                    calls.add(executor.submit(() -> {
                        var response = rivals ? f.connection.getRivals() : f.connection.getTableDatas();
                        return response.isSucceeded() && response.getMessage().equals(rivals ? "rivals" : "tables");
                    }));
                }
                for (var call : calls) check(call.get(5, TimeUnit.SECONDS), "Concurrent calls must not cross responses");
                check(f.count("login:user-a") == 1, "Do not log in on every API call");
            } finally { executor.shutdownNow(); executor.awaitTermination(4, TimeUnit.SECONDS); }
        }
        var starting = new CountDownLatch(1); var allowStart = new CountDownLatch(1);
        try (var f = new Fixture("normal", starting, allowStart)) {
            var executor = Executors.newSingleThreadExecutor();
            try {
                var login = executor.submit(() -> f.connection.login(new IRAccount("user-a", "unused", "")));
                check(starting.await(3, TimeUnit.SECONDS), "Worker startup entered");
                f.connection.close(); allowStart.countDown();
                check(!login.get(4, TimeUnit.SECONDS).isSucceeded(), "Close during startup must discard the new generation");
            } finally { allowStart.countDown(); executor.shutdownNow(); executor.awaitTermination(4, TimeUnit.SECONDS); }
        }
        System.out.println("IsolatedIRConnectionTest passed (real legacy-method worker, reauthentication, crash/read/write timeout, concurrent close/requests, unknown submission outcome)");
    }
    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory("isolated-ir-recovery-");
        final Path events = root.resolve("events.txt");
        final List<Process> processes = new java.util.concurrent.CopyOnWriteArrayList<>();
        final IsolatedIRConnection connection;
        Fixture(String mode) throws Exception {
            this(mode, null, null);
        }
        Fixture(String mode, CountDownLatch entered, CountDownLatch release) throws Exception {
            Path plugin = root.resolve("legacy-plugin.jar");
            try (var jar = new java.util.jar.JarOutputStream(Files.newOutputStream(plugin))) {
                jar.putNextEntry(new java.util.jar.JarEntry("bms/player/beatoraja/ir/")); jar.closeEntry();
                for (String name : List.of("RecoveryIRConnection", "RecoveryIRConnection$1", "TestIRConnectionBase")) {
                    String resource = "bms/player/beatoraja/ir/" + name + ".class";
                    jar.putNextEntry(new java.util.jar.JarEntry(resource));
                    try (var input = IsolatedIRConnectionTest.class.getClassLoader().getResourceAsStream(resource)) { input.transferTo(jar); }
                    jar.closeEntry();
                }
            }
            connection = new IsolatedIRConnection(RecoveryIRConnection.NAME, 3000, () -> {
                if (entered != null) {
                    entered.countDown();
                    try { if (!release.await(5, TimeUnit.SECONDS)) throw new java.io.IOException("Fixture startup timeout"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new java.io.IOException(e); }
                }
                Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin",
                        System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                        "-cp", plugin + java.io.File.pathSeparator + System.getProperty("java.class.path"), RecoveryIRConnection.class.getName(), events.toString(), mode)
                        .directory(root.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                processes.add(process); return process;
            });
        }
        void login() { check(connection.login(new IRAccount("user-a", "fixture-secret-not-for-logs", "fixture")).isSucceeded(), "Fixture login"); }
        long count(String name) {
            try { return Files.exists(events) ? Files.readAllLines(events).stream().filter(name::equals).count() : 0; }
            catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        @Override public void close() throws Exception {
            connection.close(); connection.close();
            try {
                var account = IsolatedIRConnection.class.getDeclaredField("lastLogin"); account.setAccessible(true);
                check(account.get(connection) == null, "Do not retain credentials after close");
                for (Process process : processes) check(process.waitFor(4, TimeUnit.SECONDS), "Worker leaked");
                await(() -> Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.isAlive() && t.getName().startsWith("isolated-ir-") && t.getName().contains(RecoveryIRConnection.NAME)));
            } finally {
                for (Process process : processes) if (process.isAlive()) process.destroyForcibly().waitFor(3, TimeUnit.SECONDS);
                try (var files = Files.walk(root)) { for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
            }
        }
    }
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        check(condition.getAsBoolean(), "Condition timed out");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
