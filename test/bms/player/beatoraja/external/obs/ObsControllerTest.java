package bms.player.beatoraja.external.obs;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static bms.player.beatoraja.external.obs.ObsWebSocket.object;

/** Uses a deterministic transport and isolated files to test the game controller's ownership rules. */
public final class ObsControllerTest {
    public static void main(String[] args) throws Exception {
        configRoundTrip();
        delayedStopAndRestart();
        retainManualAndDisconnected();
        retainUnverifiedFiles();
        shutdownTimeout();
        System.out.println("ObsControllerTest passed (settings, delayed stop/restart, graceful shutdown, ownership, retention and disconnect)");
    }

    private static void configRoundTrip() {
        var json = new Json();
        var old = json.fromJson(Config.class, "{}"); old.validate();
        check(!old.isUseObsWs() && old.getObsWsPort() == 4455 && old.getObsWsRecMode() == 0, "Disabled and keep-all defaults");
        old.setUseObsWs(true); old.setObsWsHost("127.0.0.1"); old.setObsWsPort(4599); old.setObsWsPass("fixture-password");
        old.setObsWsRecStopWait(650); old.setObsWsRecMode(2);
        old.getObsCommands().add(new ObsControlCommand("PLAY_START", "HideSource", "Play", "Overlay"));
        old.getObsCommands().add(new ObsControlCommand("PLAY_START", "DeleteFile", "", ""));
        var restored = json.fromJson(Config.class, json.toJson(old)); restored.validate();
        check(restored.isUseObsWs() && restored.getObsWsPort() == 4599 && restored.getObsWsRecStopWait() == 650
                && restored.getObsWsRecMode() == 2 && restored.getObsWsPass().equals("fixture-password"), "OBS settings round trip");
        check(restored.getObsCommands().size() == 1 && restored.getObsCommands().get(0).getTargetSource().equals("Overlay"),
                "Valid commands survive; arbitrary request types are rejected");
    }

    private static void delayedStopAndRestart() throws Exception {
        try (var fixture = new Fixture("localhost", 1, 5000)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain();
            var first = fixture.transport.current;
            check(first != null && Files.exists(first) && fixture.transport.starts == 1, "Game starts an owned recording");
            fixture.controller.playEnded(); fixture.controller.stateChanged("RESULT"); fixture.drain();
            check(fixture.transport.stops == 0 && fixture.transport.requests.contains("SetSceneItemEnabled"),
                    "Unrelated scene change does not flush a delayed stop");
            fixture.controller.stateChanged("PLAY"); fixture.drain();
            check(fixture.transport.stops == 1 && fixture.transport.starts == 2 && !Files.exists(first),
                    "Retry flushes the old stop once and removes only the completed, unmarked recording: stops="
                            + fixture.transport.stops + ", starts=" + fixture.transport.starts + ", retained=" + Files.exists(first));
            var marked = fixture.transport.current;
            fixture.controller.keepRecording(true); fixture.controller.playEnded(); fixture.drain();
            fixture.controller.close();
            check(fixture.transport.stops == 2 && fixture.transport.closed && Files.exists(marked),
                    "Shutdown flushes the pending stop and retains the screenshot-marked file");
            try (var paths = Files.list(fixture.root)) {
                check(paths.noneMatch(Files::isDirectory), "Temporary identity links are cleaned up");
            }
        }
        try (var fixture = new Fixture("localhost", 2, 0)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var recording = fixture.transport.current;
            fixture.controller.playEnded(); fixture.drain();
            check(fixture.transport.stops == 1 && Files.exists(recording), "Stopped file remains eligible during results");
            fixture.controller.keepRecording(false); fixture.drain(); fixture.controller.close();
            check(Files.exists(recording), "Replay save after OBS stopped still retains the matching recording");
        }
    }

    private static void retainManualAndDisconnected() throws Exception {
        try (var fixture = new Fixture("localhost", 1, 0)) {
            Path manual = Files.writeString(fixture.root.resolve("manual.mkv"), "manual");
            fixture.onExecutor(() -> fixture.transport.current = manual);
            fixture.controller.stateChanged("PLAY"); fixture.controller.playEnded(); fixture.drain(); fixture.controller.close();
            check(fixture.transport.starts == 0 && fixture.transport.stops == 0 && Files.exists(manual), "Existing manual recording is untouched");
        }
        try (var fixture = new Fixture("localhost", 1, 5000)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var original = fixture.transport.current;
            fixture.controller.playEnded(); fixture.drain();
            fixture.onExecutor(fixture.transport::disconnect);
            fixture.controller.stateChanged("RESULT"); fixture.drain();
            int requestsBefore = fixture.transport.requests.size();
            fixture.onExecutor(fixture.transport::connect); fixture.drain();
            check(fixture.transport.requests.size() > requestsBefore && fixture.transport.requests.contains("SetSceneItemEnabled"),
                    "Reconnect reapplies the current state, including source visibility");
            fixture.controller.close();
            check(fixture.transport.stops == 0 && Files.exists(original), "Lost ownership never stops or deletes after reconnect");
        }
        try (var fixture = new Fixture("192.0.2.1", 1, 0)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var recording = fixture.transport.current;
            fixture.controller.close();
            check(fixture.transport.stops == 1 && Files.exists(recording), "Remote OBS recording paths never delete a local file");
        }
    }

    private static void retainUnverifiedFiles() throws Exception {
        try (var fixture = new Fixture("localhost", 1, 0)) {
            Path preexisting = Files.writeString(fixture.root.resolve("old.mkv"), "existing");
            fixture.onExecutor(() -> fixture.transport.reportPath = preexisting);
            fixture.controller.stateChanged("PLAY"); fixture.drain(); fixture.controller.close();
            check(Files.readString(preexisting).equals("existing"), "Even matching OBS paths cannot delete a preexisting file");
        }
        try (var fixture = new Fixture("localhost", 1, 0)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var recording = fixture.transport.current;
            fixture.controller.playEnded(); fixture.drain();
            Files.move(recording, fixture.root.resolve("renamed.mkv"));
            Files.writeString(recording, "game recording");
            fixture.controller.close();
            check(Files.exists(recording), "A replacement at the same path is not the owned file");
        }
        try (var fixture = new Fixture("localhost", 1, 0)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var recording = fixture.transport.current;
            fixture.onExecutor(() -> fixture.transport.mismatch = true);
            fixture.controller.close();
            check(Files.exists(recording), "Disagreement between stop response and event keeps the file");
        }
    }

    private static void shutdownTimeout() throws Exception {
        try (var fixture = new Fixture("localhost", 1, 0)) {
            fixture.controller.stateChanged("PLAY"); fixture.drain(); var recording = fixture.transport.current;
            fixture.onExecutor(() -> fixture.transport.ignoreStop = true);
            long start = System.nanoTime(); fixture.controller.close();
            check(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3), "Missing OBS response cannot hang game shutdown");
            check(fixture.executor.awaitTermination(2, TimeUnit.SECONDS) && fixture.transport.closed && Files.exists(recording),
                    "Timeout closes resources and keeps unconfirmed recording");
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory("beatoraja-obs-recording-test-");
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final Fake transport = new Fake(root, executor);
        final ObsController controller;
        Fixture(String host, int mode, int delay) throws Exception {
            var config = new Config(); config.setObsWsHost(host); config.setObsWsRecMode(mode); config.setObsWsRecStopWait(delay);
            config.getObsCommands().addAll(List.of(new ObsControlCommand("PLAY_START", "StartRecord", "", ""),
                    new ObsControlCommand("PLAY_END", "StopRecord", "", ""),
                    new ObsControlCommand("RESULT_START", "SetScene", "Result", ""),
                    new ObsControlCommand("RESULT_START", "ShowSource", "Result", "Summary")));
            controller = new ObsController(config, executor, transport);
            controller.setStatusListener(message -> { }); controller.start(); drain();
        }
        void onExecutor(Runnable task) throws Exception { executor.submit(task).get(2, TimeUnit.SECONDS); }
        void drain() throws Exception { for (int i = 0; i < 5; i++) onExecutor(() -> { }); }
        public void close() throws Exception {
            controller.close(); executor.awaitTermination(3, TimeUnit.SECONDS);
            try (var paths = Files.walk(root)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    private static final class Fake implements ObsTransport {
        final Path root;
        final ScheduledExecutorService executor;
        final List<String> requests = new ArrayList<>();
        Listener listener;
        boolean ready, closed, mismatch, ignoreStop;
        int starts, stops;
        Path current, reportPath;
        Fake(Path root, ScheduledExecutorService executor) { this.root = root; this.executor = executor; }
        public void setListener(Listener listener) { this.listener = listener; }
        public void connect() { ready = true; listener.ready(); }
        void disconnect() { ready = false; listener.disconnected(); }
        public boolean isReady() { return ready; }
        public CompletableFuture<JsonValue> request(String type, JsonValue data) {
            requests.add(type);
            try {
                return switch (type) {
                    case "GetRecordStatus" -> CompletableFuture.completedFuture(object("outputActive", current != null));
                    case "GetRecordDirectory" -> CompletableFuture.completedFuture(object("recordDirectory", root.toString()));
                    case "StartRecord" -> {
                        starts++;
                        current = Files.writeString(root.resolve("owned-" + starts + ".mkv"), "game recording");
                        if (starts % 2 == 0) recordEvent("STARTED", "");
                        else executor.execute(() -> recordEvent("STARTED", ""));
                        yield CompletableFuture.completedFuture(object());
                    }
                    case "StopRecord" -> {
                        stops++;
                        if (ignoreStop) yield new CompletableFuture<>();
                        Path path = reportPath != null ? reportPath : current;
                        current = null;
                        recordEvent("STOPPED", mismatch ? root.resolve("different.mkv").toString() : path.toString());
                        yield CompletableFuture.completedFuture(object("outputPath", path.toString()));
                    }
                    case "GetSceneItemId" -> CompletableFuture.completedFuture(object("sceneItemId", 42));
                    default -> CompletableFuture.completedFuture(object());
                };
            } catch (Exception failure) { return CompletableFuture.failedFuture(failure); }
        }
        private void recordEvent(String state, String path) {
            listener.event("RecordStateChanged", object("outputState", "OBS_WEBSOCKET_OUTPUT_" + state, "outputPath", path));
        }
        public void close() { closed = true; ready = false; }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
