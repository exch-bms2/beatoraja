package bms.player.beatoraja.obs;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.*;

/** OBS WebSocket 5.x with bounded requests, connection generations and serialized sends. */
final class ObsWebSocket implements ObsTransport {
    private final ScheduledExecutorService executor;
    private final URI uri;
    private final String password;
    private final HashMap<String, CompletableFuture<JsonValue>> pending = new HashMap<>();
    private final HashMap<String, ScheduledFuture<?>> deadlines = new HashMap<>();
    private final HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(3)).build();
    private Listener listener;
    private WebSocket socket;
    private CompletableFuture<?> connecting;
    private CompletableFuture<?> sends = CompletableFuture.completedFuture(null);
    private ScheduledFuture<?> reconnect, handshake;
    private long generation, sequence;
    private int retrySeconds = 1;
    private boolean ready, closed;

    ObsWebSocket(Config config, ScheduledExecutorService executor) {
        this.executor = executor;
        password = config.getObsWsPass();
        try { uri = new URI("ws", null, config.getObsWsHost(), config.getObsWsPort(), null, null, null); }
        catch (Exception invalid) { throw new IllegalArgumentException("Invalid OBS address"); }
    }
    public void setListener(Listener listener) { this.listener = listener; }
    public boolean isReady() { return ready && !closed; }
    public void connect() {
        if (closed) return;
        long session = ++generation;
        ready = false;
        listener.status("Connecting");
        handshake = executor.schedule(() -> lost(session), 5, TimeUnit.SECONDS);
        connecting = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(3))
                .buildAsync(uri, new WebSocket.Listener() {
                    private final StringBuilder text = new StringBuilder();
                    public void onOpen(WebSocket ws) {
                        if (!dispatch(() -> {
                            if (closed || session != generation) ws.abort();
                            else socket = ws;
                        })) ws.abort();
                        ws.request(1);
                    }
                    public CompletionStage<?> onText(WebSocket ws, CharSequence part, boolean last) {
                        if (text.length() + part.length() > 1024 * 1024) {
                            ws.abort(); dispatch(() -> lost(session)); return null;
                        }
                        text.append(part);
                        if (last) {
                            String message = text.toString(); text.setLength(0);
                            dispatch(() -> receive(session, message));
                        }
                        ws.request(1);
                        return null;
                    }
                    public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                        dispatch(() -> lost(session)); return null;
                    }
                    public void onError(WebSocket ws, Throwable error) { dispatch(() -> lost(session)); }
                });
        connecting.whenComplete((ws, error) -> { if (error != null) dispatch(() -> lost(session)); });
    }

    private boolean dispatch(Runnable task) {
        try { executor.execute(task); return true; }
        catch (RejectedExecutionException stopped) { return false; }
    }
    private void receive(long session, String message) {
        if (closed || session != generation) return;
        try {
            JsonValue packet = new JsonReader().parse(message);
            JsonValue data = packet.get("d");
            switch (packet.getInt("op")) {
                case 0 -> {
                    JsonValue identify = object("rpcVersion", 1, "eventSubscriptions", 65);
                    JsonValue authentication = data.get("authentication");
                    if (authentication != null) identify.addChild("authentication", new JsonValue(
                            authentication(password, authentication.getString("salt"), authentication.getString("challenge"))));
                    send(object("op", 1, "d", identify));
                }
                case 2 -> {
                    if (ready) break;
                    ready = true; retrySeconds = 1;
                    if (handshake != null) handshake.cancel(false);
                    listener.status("Connected"); listener.ready();
                }
                case 5 -> { if (ready) listener.event(data.getString("eventType"), data.get("eventData")); }
                case 7 -> {
                    String id = data.getString("requestId");
                    JsonValue status = data.get("requestStatus");
                    boolean success = status.getBoolean("result");
                    int code = status.getInt("code", 0);
                    var future = pending.remove(id);
                    var timeout = deadlines.remove(id);
                    if (timeout != null) timeout.cancel(false);
                    if (future != null) {
                        JsonValue response = data.get("responseData");
                        if (success) future.complete(response != null && response.isObject() ? response : object());
                        else future.completeExceptionally(new IllegalStateException("OBS request rejected (" + code + ")"));
                    }
                }
                default -> { }
            }
        } catch (Exception malformed) { lost(session); }
    }

    static String authentication(String password, String salt, String challenge) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        String secret = Base64.getEncoder().encodeToString(digest.digest((password + salt).getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(digest.digest((secret + challenge).getBytes(StandardCharsets.UTF_8)));
    }

    public CompletableFuture<JsonValue> request(String type, JsonValue data) {
        if (!isReady()) return CompletableFuture.failedFuture(new IllegalStateException("OBS disconnected"));
        if (pending.size() >= 128) return CompletableFuture.failedFuture(new IllegalStateException("Too many OBS requests"));
        String id = Long.toString(++sequence);
        var future = new CompletableFuture<JsonValue>();
        pending.put(id, future);
        deadlines.put(id, executor.schedule(() -> {
            pending.remove(id); deadlines.remove(id);
            future.completeExceptionally(new TimeoutException("OBS request timed out"));
        }, 5, TimeUnit.SECONDS));
        send(object("op", 6, "d", object("requestType", type, "requestId", id, "requestData", data == null ? object() : data)));
        return future;
    }

    private void send(JsonValue packet) {
        long session = generation;
        String id = packet.get("d").getString("requestId", null);
        String message = packet.toJson(JsonWriter.OutputType.json);
        WebSocket target = socket;
        sends = sends.thenComposeAsync(ignored -> {
                    if (closed || session != generation || target == null)
                        return CompletableFuture.failedFuture(new IllegalStateException("OBS connection changed"));
                    if (id != null && !pending.containsKey(id)) return CompletableFuture.completedFuture(target);
                    return target.sendText(message, true);
                }, executor)
                .whenComplete((ignored, error) -> { if (error != null) dispatch(() -> lost(session)); });
    }
    private void lost(long session) {
        if (closed || session != generation) return;
        generation++; ready = false;
        if (socket != null) socket.abort(); socket = null;
        if (connecting != null) connecting.cancel(true);
        if (handshake != null) handshake.cancel(false);
        sends = CompletableFuture.completedFuture(null);
        failPending();
        listener.disconnected(); listener.status("Disconnected; retrying");
        if (closed) return;
        if (reconnect != null) reconnect.cancel(false);
        reconnect = executor.schedule(this::connect, retrySeconds, TimeUnit.SECONDS);
        retrySeconds = Math.min(15, retrySeconds * 2);
    }
    private void failPending() {
        var requests = pending.values().toArray(CompletableFuture[]::new);
        pending.clear();
        for (var timeout : deadlines.values()) timeout.cancel(false);
        deadlines.clear();
        for (var future : requests) future.completeExceptionally(new IllegalStateException("OBS connection closed"));
    }
    public void close() {
        if (closed) return;
        closed = true; ready = false; generation++;
        if (reconnect != null) reconnect.cancel(false);
        if (handshake != null) handshake.cancel(false);
        if (connecting != null) connecting.cancel(true);
        if (socket != null) socket.abort(); socket = null;
        failPending();
    }

    static JsonValue object(Object... fields) {
        var result = new JsonValue(JsonValue.ValueType.object);
        for (int i = 0; i < fields.length; i += 2) {
            Object value = fields[i + 1];
            JsonValue json = value instanceof JsonValue nested ? nested
                    : value instanceof Boolean bool ? new JsonValue(bool)
                    : value instanceof Number number ? new JsonValue(number.longValue()) : new JsonValue((String) value);
            result.addChild((String) fields[i], json);
        }
        return result;
    }
}
