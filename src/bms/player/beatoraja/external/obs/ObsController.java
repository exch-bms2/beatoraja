package bms.player.beatoraja.external.obs;

import bms.player.beatoraja.*;
import com.badlogic.gdx.utils.JsonValue;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import static bms.player.beatoraja.external.obs.ObsWebSocket.object;

/** Nonblocking game hooks; all OBS state and recording ownership live on one executor. */
public final class ObsController implements MainStateListener, AutoCloseable, ObsTransport.Listener {
    private final ScheduledExecutorService executor;
    private final ObsTransport transport;
    private final List<ObsControlCommand> commands;
    private final int stopDelay, recordingMode;
    private final boolean local;
    private final AtomicBoolean closing = new AtomicBoolean();
    private volatile Consumer<String> status = message -> Logger.getGlobal().info("OBS: " + message);
    private MainState lastState;
    private String currentState;
    private boolean mainStarted, playEnded;
    private ScheduledFuture<?> scheduledStop;
    private long stopGeneration;
    private Recording starting, active, stopping, completed;
    private CompletableFuture<Void> startFuture, stopFuture;
    private final Map<String, Long> sourceChanges = new HashMap<>();
    private long sourceSequence;

    public ObsController(Config config) {
        this(config, newExecutor(), null);
    }
    private static ScheduledExecutorService newExecutor() {
        var executor = new ScheduledThreadPoolExecutor(1, task -> {
            var thread = new Thread(task, "OBS control"); thread.setDaemon(true); return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }
    ObsController(Config config, ScheduledExecutorService executor, ObsTransport supplied) {
        this.executor = executor;
        commands = List.copyOf(config.getObsCommands());
        stopDelay = config.getObsWsRecStopWait(); recordingMode = config.getObsWsRecMode();
        String host = config.getObsWsHost();
        local = host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]");
        transport = supplied != null ? supplied : new ObsWebSocket(config, executor);
        transport.setListener(this);
    }
    public void start() { submit(transport::connect); }
    public void setStatusListener(Consumer<String> listener) { status = listener; }
    public void status(String message) { status.accept(message); }
    private void submit(Runnable task) {
        if (closing.get()) return;
        try { executor.execute(task); } catch (RejectedExecutionException stopped) { }
    }

    @Override public void update(MainState state, int status) {
        submit(() -> {
            if (lastState == state) return;
            transition(state.type.name()); lastState = state;
        });
    }
    /** State name entry point also used by tests without constructing a game or a skin. */
    void stateChanged(String state) { submit(() -> transition(state)); }
    private void transition(String state) {
        if (currentState != null && !(currentState.equals("PLAY") && playEnded)) runCommands(currentState + "_END");
        currentState = state; playEnded = false;
        runCommands(state + "_START");
    }
    public void playEnded() {
        submit(() -> {
            if (!playEnded && "PLAY".equals(currentState)) { playEnded = true; runCommands("PLAY_END"); }
        });
    }
    public void keepRecording(boolean screenshot) {
        submit(() -> {
            if (recordingMode != (screenshot ? 1 : 2)) return;
            Recording recording = active != null ? active : starting != null ? starting : stopping != null ? stopping : completed;
            if (recording != null) {
                recording.keep = true;
                recording.releaseIdentity();
            }
        });
    }
    @Override public void ready() {
        if (closing.get()) return;
        if (!mainStarted) { mainStarted = true; runCommands("MAIN_START"); }
        if (currentState != null) runCommands(currentState + (playEnded ? "_END" : "_START"));
    }
    @Override public void disconnected() {
        cancelStop(); sourceChanges.clear();
        for (Recording recording : new Recording[]{starting, active, stopping, completed})
            if (recording != null) { recording.invalid = true; recording.keep = true; }
        if (completed != null) completed.releaseIdentity();
        active = null; starting = null; stopping = null;
        if (startFuture != null && !startFuture.isDone()) startFuture.completeExceptionally(new IllegalStateException("OBS disconnected"));
        if (stopFuture != null && !stopFuture.isDone()) stopFuture.complete(null);
    }

    private CompletableFuture<Void> runCommands(String timing) {
        if (!transport.isReady()) return CompletableFuture.completedFuture(null);
        var requests = new ArrayList<CompletableFuture<?>>();
        for (var command : commands) if (command.isValid() && command.getTiming().equals(timing)) {
            switch (command.getAction()) {
                case "SetScene" -> requests.add(request("SetCurrentProgramScene", object("sceneName", command.getTransitionScene())));
                case "ShowSource", "HideSource" -> requests.add(visibility(command));
                case "StartRecord" -> startRecording();
                case "StopRecord" -> scheduleStop();
                default -> { }
            }
        }
        return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new));
    }
    private CompletableFuture<JsonValue> visibility(ObsControlCommand command) {
        String key = command.getTargetScene() + "\n" + command.getTargetSource();
        long change = ++sourceSequence; sourceChanges.put(key, change);
        return request("GetSceneItemId", object("sceneName", command.getTargetScene(), "sourceName", command.getTargetSource()))
                .thenCompose(data -> {
                    if (Objects.equals(sourceChanges.get(key), change)) return request("SetSceneItemEnabled",
                            object("sceneName", command.getTargetScene(), "sceneItemId", data.getInt("sceneItemId"),
                                    "sceneItemEnabled", command.getAction().equals("ShowSource")));
                    return CompletableFuture.completedFuture(null);
                });
    }
    private CompletableFuture<JsonValue> request(String type, JsonValue data) {
        var request = transport.request(type, data);
        request.whenComplete((result, error) -> { if (error != null) status("Request failed: " + type); });
        return request;
    }
    private void cancelStop() {
        stopGeneration++;
        if (scheduledStop != null) scheduledStop.cancel(false);
        scheduledStop = null;
    }
    private void scheduleStop() {
        cancelStop();
        long generation = stopGeneration;
        scheduledStop = executor.schedule(() -> {
            if (generation != stopGeneration) return;
            scheduledStop = null; stopRecording();
        }, stopDelay, TimeUnit.MILLISECONDS);
    }
    private void startRecording() {
        if (closing.get()) return;
        boolean flush = scheduledStop != null;
        cancelStop();
        if (flush || stopping != null) {
            stopRecording().thenRun(() -> { if (!closing.get()) startRecording(); });
            return;
        }
        if (active != null || starting != null || !transport.isReady()) return;
        var recording = new Recording(); starting = recording;
        startFuture = recording.startDone = new CompletableFuture<>();
        ScheduledFuture<?> deadline = executor.schedule(() -> {
            recording.invalid = true; recording.keep = true;
            if (starting == recording) starting = null;
            recording.startDone.completeExceptionally(new TimeoutException("OBS recording start timed out"));
        }, 5, TimeUnit.SECONDS);
        recording.startDone.whenComplete((ignored, error) -> deadline.cancel(false));
        request("GetRecordStatus", null).thenCompose(data -> {
            if (data.getBoolean("outputActive")) return CompletableFuture.<JsonValue>failedFuture(new IllegalStateException("OBS already recording"));
            return local && recordingMode != 0 ? request("GetRecordDirectory", null).exceptionally(error -> object())
                    : CompletableFuture.completedFuture(object());
        }).thenCompose(data -> {
            if (recording.invalid || !transport.isReady()) return CompletableFuture.<JsonValue>failedFuture(new IllegalStateException("OBS connection changed"));
            if (local) recording.captureDirectory(data.getString("recordDirectory", ""));
            // Some filesystems/JDKs expose creation time only to the second. The pre-start listing still excludes existing files.
            recording.requestedAt = System.currentTimeMillis() / 1000 * 1000;
            recording.startSent = true;
            return request("StartRecord", null);
        }).whenComplete((data, error) -> {
            if (error == null && !recording.invalid) {
                recording.acknowledged = true;
                completeStart(recording);
            } else {
                recording.invalid = true; recording.keep = true;
                recording.startDone.completeExceptionally(new IllegalStateException("OBS recording start failed"));
                if (starting == recording) starting = null;
            }
        });
    }
    private void completeStart(Recording recording) {
        if (recording.acknowledged && recording.started && !recording.invalid && starting == recording) {
            finishPrevious();
            active = recording; starting = null; recording.startDone.complete(null);
        }
    }
    private CompletableFuture<Void> stopRecording() {
        if (stopping != null) return stopFuture;
        if (starting != null) return startFuture.thenCompose(ignored -> stopRecording());
        if (active == null || active.invalid || !transport.isReady()) return CompletableFuture.completedFuture(null);
        Recording recording = active; stopping = recording;
        stopFuture = new CompletableFuture<>();
        var result = stopFuture;
        ScheduledFuture<?> timeout = executor.schedule(() -> {
            recording.invalid = true; recording.keep = true;
            if (stopping == recording) { stopping = null; active = null; }
            result.complete(null);
        }, 5, TimeUnit.SECONDS);
        result.whenComplete((ignored, error) -> timeout.cancel(false));
        request("StopRecord", null).whenComplete((data, error) -> {
            if (error == null) { recording.stopPath = data.getString("outputPath", ""); completeStop(recording); }
            else {
                recording.invalid = true; recording.keep = true;
                if (stopping == recording) { stopping = null; active = null; }
                result.complete(null);
            }
        });
        return result;
    }
    private void completeStop(Recording recording) {
        if (stopping != recording || recording.stopPath == null || recording.eventPath == null) return;
        recording.invalid |= !recording.stopPath.equals(recording.eventPath);
        if (recordingMode != 0 && !recording.keep && !recording.invalid) recording.captureStoppedFile();
        completed = recording; stopping = null;
        if (active == recording) active = null;
        stopFuture.complete(null);
    }
    @Override public void event(String type, JsonValue data) {
        if (type.equals("RecordFileChanged")) {
            if (active != null) active.keep = true; // Split files require more ownership information than 5.0 supplies.
            return;
        }
        if (!type.equals("RecordStateChanged") || data == null) return;
        switch (data.getString("outputState", "")) {
            case "OBS_WEBSOCKET_OUTPUT_STARTED" -> {
                if (starting != null && starting.startSent && !starting.started) {
                    starting.started = true;
                    completeStart(starting);
                } else {
                    if (active != null) { active.invalid = true; active.keep = true; }
                    active = null;
                    if (stopping != null) { stopping.invalid = true; stopping.keep = true; }
                }
            }
            case "OBS_WEBSOCKET_OUTPUT_STOPPED" -> {
                if (stopping != null && active == stopping) {
                    active = null;
                    stopping.eventPath = data.getString("outputPath", ""); completeStop(stopping);
                } else {
                    if (active != null) { active.invalid = true; active.keep = true; }
                    if (starting != null) { starting.invalid = true; starting.keep = true; }
                    active = null;
                }
            }
            default -> { }
        }
    }
    private void finishPrevious() {
        if (completed == null) return;
        Recording recording = completed;
        completed = null;
        try {
            if (recordingMode != 0 && !recording.keep && !recording.invalid) recording.deleteOwnedFile();
        } finally { recording.releaseIdentity(); }
    }

    public record ConnectionInfo(String version, Map<String, List<String>> scenes) { }

    /** Launcher probe performs only read requests and closes on success, failure, timeout or cancellation. */
    public static CompletableFuture<ConnectionInfo> inspect(Config config) {
        var executor = newExecutor();
        final ObsWebSocket connection;
        try { connection = new ObsWebSocket(config, executor); }
        catch (IllegalArgumentException invalid) { executor.shutdown(); return CompletableFuture.failedFuture(invalid); }
        var result = new CompletableFuture<ConnectionInfo>();
        var deadline = executor.schedule(() -> result.completeExceptionally(new TimeoutException("OBS probe timed out")), 8, TimeUnit.SECONDS);
        result.whenComplete((info, error) -> executor.execute(() -> {
            deadline.cancel(false); connection.close(); executor.shutdown();
        }));
        connection.setListener(new ObsTransport.Listener() {
            public void ready() {
                connection.request("GetVersion", null).thenCompose(version -> connection.request("GetSceneList", null)
                        .thenCompose(data -> {
                            Map<String, List<String>> scenes = new LinkedHashMap<>();
                            var requests = new ArrayList<CompletableFuture<Void>>();
                            JsonValue list = data.get("scenes");
                            if (list != null) for (JsonValue scene : list) {
                                if (scenes.size() >= 64) break;
                                String name = scene.getString("sceneName"); scenes.put(name, List.of());
                                requests.add(connection.request("GetSceneItemList", object("sceneName", name)).thenAccept(items -> {
                                    var sources = new LinkedHashSet<String>();
                                    JsonValue entries = items.get("sceneItems");
                                    if (entries != null) for (JsonValue item : entries) sources.add(item.getString("sourceName"));
                                    scenes.put(name, List.copyOf(sources));
                                }).exceptionally(error -> null));
                            }
                            return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).thenApply(ignored ->
                                    new ConnectionInfo(version.getString("obsVersion", ""), Collections.unmodifiableMap(scenes)));
                        })).whenComplete((info, error) -> {
                            if (error == null) result.complete(info); else result.completeExceptionally(error);
                        });
            }
            public void disconnected() { result.completeExceptionally(new IllegalStateException("OBS disconnected")); }
            public void event(String type, JsonValue data) { }
            public void status(String message) { }
        });
        executor.execute(connection::connect);
        return result;
    }

    /** Bounded shutdown flushes an owned stop before closing the connection and executor. */
    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        var finished = new CompletableFuture<Void>();
        try {
            executor.execute(() -> {
                cancelStop();
                var exitCommands = runCommands("MAIN_END");
                cancelStop();
                CompletableFuture.allOf(exitCommands, stopRecording()).whenComplete((ignored, error) -> {
                    finishPrevious(); transport.close(); finished.complete(null);
                });
            });
            finished.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        catch (ExecutionException | TimeoutException | RejectedExecutionException incomplete) { }
        finally {
            // Closing from the game never waits indefinitely for an unreachable OBS instance.
            try {
                executor.execute(() -> {
                    try { disconnected(); transport.close(); }
                    finally { executor.shutdown(); }
                });
            } catch (RejectedExecutionException stopped) { executor.shutdown(); }
        }
    }

    private static final class Recording {
        boolean startSent, started, acknowledged, keep, invalid;
        long requestedAt;
        Path directory;
        Set<Path> existing = Set.of();
        String stopPath, eventPath;
        CompletableFuture<Void> startDone;
        Path verifiedOutput;
        BasicFileAttributes outputIdentity;
        Path identityDirectory;
        void captureDirectory(String path) {
            try {
                Path root = Path.of(path);
                if (!root.isAbsolute()) return;
                directory = root.toRealPath();
                try (var entries = Files.list(directory)) {
                    var files = entries.limit(8193).toList();
                    if (files.size() <= 8192) existing = new HashSet<>(files);
                    else directory = null;
                }
            } catch (Exception unavailable) { directory = null; }
        }
        void captureStoppedFile() {
            if (!acknowledged || !started || directory == null || stopPath == null || stopPath.isBlank()) return;
            try {
                Path file = Path.of(stopPath);
                if (!file.isAbsolute()) return;
                file = file.normalize();
                if (!file.getParent().toRealPath().equals(directory)) return;
                file = directory.resolve(file.getFileName());
                if (existing.contains(file)) return;
                var attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!attributes.isRegularFile() || attributes.creationTime().toMillis() < requestedAt) return;
                // A temporary hard link gives Files.isSameFile an identity to compare on Windows too,
                // where BasicFileAttributes.fileKey() is unavailable. Unsupported volumes retain the file.
                identityDirectory = Files.createTempDirectory(directory, ".beatoraja-obs-");
                Files.createLink(identityDirectory.resolve("recording"), file);
                verifiedOutput = file; outputIdentity = attributes;
            } catch (Exception unavailable) { releaseIdentity(); }
        }
        void deleteOwnedFile() {
            if (verifiedOutput == null || identityDirectory == null) return;
            try {
                if (!verifiedOutput.getParent().toRealPath().equals(directory)) return;
                var now = Files.readAttributes(verifiedOutput, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!now.isRegularFile() || !Files.isSameFile(verifiedOutput, identityDirectory.resolve("recording"))
                        || !outputIdentity.creationTime().equals(now.creationTime())
                        || !outputIdentity.lastModifiedTime().equals(now.lastModifiedTime()) || outputIdentity.size() != now.size()) return;
                Files.delete(verifiedOutput);
                Logger.getGlobal().info("OBS: removed unselected game-owned recording: " + verifiedOutput);
            } catch (Exception unavailable) { Logger.getGlobal().warning("OBS: retaining recording; deletion could not be verified"); }
        }
        void releaseIdentity() {
            if (identityDirectory == null) return;
            try {
                Files.deleteIfExists(identityDirectory.resolve("recording"));
                Files.deleteIfExists(identityDirectory);
            } catch (Exception unavailable) { Logger.getGlobal().warning("OBS: could not remove temporary recording identity"); }
            finally { identityDirectory = null; }
        }
    }
}
