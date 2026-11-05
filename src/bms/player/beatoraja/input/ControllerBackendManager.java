package bms.player.beatoraja.input;

import com.badlogic.gdx.utils.Disposable;
import java.util.function.BooleanSupplier;

/** Backend lifecycle shared by the input thread and the rendering application. */
interface ControllerBackendManager extends Disposable {
    void setPollingShutdown(BooleanSupplier shutdown);

    interface PollRunner extends Runnable { ControllerBackendManager owner(); }

    static ControllerBackendManager ownerOf(Runnable runner) {
        return runner instanceof PollRunner poll ? poll.owner() : null;
    }
}
