package bms.player.beatoraja.input;

import com.badlogic.gdx.controllers.Controller;

/** Digital transitions use the same FIFO regardless of the physical input backend. */
interface QueuedController extends Controller {
    ControllerEventQueue events();
    long inputGeneration();
    void resetEvents(boolean active);
    ControllerInputDiagnostics diagnostics();
}
