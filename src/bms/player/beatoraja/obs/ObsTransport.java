package bms.player.beatoraja.obs;

import com.badlogic.gdx.utils.JsonValue;
import java.util.concurrent.CompletableFuture;

/** Callbacks and requests run on the OBS executor, never on rendering or input threads. */
interface ObsTransport extends AutoCloseable {
    interface Listener {
        void ready();
        void disconnected();
        void event(String type, JsonValue data);
        void status(String message);
    }
    void setListener(Listener listener);
    void connect();
    boolean isReady();
    CompletableFuture<JsonValue> request(String type, JsonValue data);
    @Override void close();
}
