package bms.player.beatoraja.obs;

import bms.player.beatoraja.Config;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;
import static bms.player.beatoraja.obs.ObsWebSocket.object;

/** Real JDK WebSocket handshake, authentication, request correlation and reconnect over loopback. */
public final class ObsWebSocketTest {
    private static final String EXPECTED_AUTH = "VBq4oJYHmPPiUPOgV7plUbnlg+g3GXDz09R0/ZksMN4=";
    public static void main(String[] args) throws Exception {
        check(ObsWebSocket.authentication("fixture-password", "salt", "challenge").equals(EXPECTED_AUTH), "OBS challenge hash");
        probe(); reconnect();
        System.out.println("ObsWebSocketTest passed (real local WebSocket, auth, fragmented Hello, probe catalog and reconnect)");
    }

    private static void probe() throws Exception {
        try (var server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
            var script = runServer(() -> {
                try (var wire = new Wire(server.accept())) {
                    wire.identify();
                    var version = wire.request("GetVersion"); wire.respond(version, object("obsVersion", "Fixture OBS"));
                    var list = wire.request("GetSceneList");
                    var scenes = new JsonValue(JsonValue.ValueType.array);
                    scenes.addChild(object("sceneName", "Play")); scenes.addChild(object("sceneName", "Result"));
                    wire.respond(list, object("scenes", scenes));
                    var one = wire.request("GetSceneItemList"); var two = wire.request("GetSceneItemList");
                    // Out-of-order replies must still reach their own scene's request.
                    respondItems(wire, two); respondItems(wire, one);
                }
            });
            var info = ObsController.inspect(config(server.getLocalPort())).get(8, TimeUnit.SECONDS);
            check(info.version().equals("Fixture OBS") && info.scenes().get("Play").equals(List.of("Play source"))
                    && info.scenes().get("Result").equals(List.of("Result source")), "Read-only probe catalog");
            script.get(3, TimeUnit.SECONDS);
        }
    }
    private static void respondItems(Wire wire, JsonValue request) throws Exception {
        String name = request.get("requestData").getString("sceneName");
        var items = new JsonValue(JsonValue.ValueType.array); items.addChild(object("sourceName", name + " source"));
        wire.respond(request, object("sceneItems", items));
    }

    private static void reconnect() throws Exception {
        try (var server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
            var offline = new CountDownLatch(1); var reapplied = new CountDownLatch(1);
            var script = runServer(() -> {
                try (var first = new Wire(server.accept())) {
                    first.identify();
                    var scene = first.request("SetCurrentProgramScene");
                    check(scene.get("requestData").getString("sceneName").equals("Play"), "Apply current state after identification");
                    first.respond(scene, object());
                }
                try (var second = new Wire(server.accept())) {
                    second.identify();
                    var scene = second.request("SetCurrentProgramScene");
                    check(scene.get("requestData").getString("sceneName").equals("Result"), "Reconnect applies latest state only");
                    second.respond(scene, object()); reapplied.countDown();
                    var exit = second.request("SetCurrentProgramScene");
                    check(exit.get("requestData").getString("sceneName").equals("Exit"), "Shutdown sends MAIN_END scene before closing");
                    second.respond(exit, object());
                }
            });
            var config = config(server.getLocalPort());
            config.getObsCommands().add(new ObsControlCommand("PLAY_START", "SetScene", "Play", ""));
            config.getObsCommands().add(new ObsControlCommand("RESULT_START", "SetScene", "Result", ""));
            config.getObsCommands().add(new ObsControlCommand("MAIN_END", "SetScene", "Exit", ""));
            try (var controller = new ObsController(config)) {
                controller.setStatusListener(status -> { if (status.startsWith("Disconnected")) offline.countDown(); });
                controller.stateChanged("PLAY"); controller.start();
                check(offline.await(5, TimeUnit.SECONDS), "Connection loss reported");
                controller.stateChanged("RESULT");
                check(reapplied.await(6, TimeUnit.SECONDS), "Bounded retry reconnects without a render loop");
            }
            script.get(3, TimeUnit.SECONDS);
        }
    }
    private static Config config(int port) {
        var config = new Config(); config.setObsWsHost("127.0.0.1"); config.setObsWsPort(port); config.setObsWsPass("fixture-password");
        return config;
    }
    private static CompletableFuture<Void> runServer(CallableScript task) {
        var done = new CompletableFuture<Void>();
        var thread = new Thread(() -> {
            try { task.run(); done.complete(null); } catch (Throwable failure) { done.completeExceptionally(failure); }
        }, "OBS fixture server");
        thread.setDaemon(true); thread.start(); return done;
    }
    @FunctionalInterface private interface CallableScript { void run() throws Exception; }

    private static final class Wire implements AutoCloseable {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        Wire(Socket socket) throws Exception {
            this.socket = socket; socket.setSoTimeout(5000);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String key = null;
            for (String line = readLine(); !line.isEmpty(); line = readLine()) {
                if (line.regionMatches(true, 0, "Sec-WebSocket-Key:", 0, 18)) key = line.substring(18).strip();
            }
            check(key != null, "WebSocket upgrade key");
            String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(
                    (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
            out.writeBytes("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n"); out.flush();
        }
        String readLine() throws IOException {
            var line = new StringBuilder();
            int b;
            while ((b = in.read()) >= 0 && b != '\n') if (b != '\r') line.append((char) b);
            if (b < 0) throw new EOFException("WebSocket header");
            return line.toString();
        }
        void identify() throws Exception {
            byte[] hello = object("op", 0, "d", object("rpcVersion", 1, "authentication",
                    object("salt", "salt", "challenge", "challenge"))).toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8);
            int half = hello.length / 2;
            frame(1, java.util.Arrays.copyOfRange(hello, 0, half));
            frame(128, java.util.Arrays.copyOfRange(hello, half, hello.length));
            JsonValue identify = receive();
            check(identify.getInt("op") == 1 && identify.get("d").getString("authentication").equals(EXPECTED_AUTH), "Wire Identify authentication");
            send(object("op", 2, "d", object("negotiatedRpcVersion", 1)));
        }
        JsonValue request(String type) throws Exception {
            var packet = receive();
            check(packet.getInt("op") == 6 && packet.get("d").getString("requestType").equals(type), "Expected request " + type);
            return packet.get("d");
        }
        void respond(JsonValue request, JsonValue data) throws Exception {
            send(object("op", 7, "d", object("requestType", request.getString("requestType"), "requestId", request.getString("requestId"),
                    "requestStatus", object("result", true, "code", 100), "responseData", data)));
        }
        JsonValue receive() throws IOException {
            int opcode = in.readUnsignedByte(); int header = in.readUnsignedByte();
            check(opcode == 129 && (header & 128) != 0, "Client sends a masked text frame");
            int length = header & 127;
            if (length == 126) length = in.readUnsignedShort();
            byte[] mask = in.readNBytes(4); byte[] payload = in.readNBytes(length);
            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
            return new JsonReader().parse(new String(payload, StandardCharsets.UTF_8));
        }
        void send(JsonValue packet) throws IOException { frame(129, packet.toJson(JsonWriter.OutputType.json).getBytes(StandardCharsets.UTF_8)); }
        void frame(int opcode, byte[] data) throws IOException {
            out.writeByte(opcode);
            if (data.length < 126) out.writeByte(data.length);
            else { out.writeByte(126); out.writeShort(data.length); }
            out.write(data); out.flush();
        }
        public void close() throws IOException { socket.close(); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
