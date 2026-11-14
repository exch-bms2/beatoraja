package bms.player.beatoraja.input.controller;

import com.badlogic.gdx.controllers.Controller;

/** Digital transitions use the same FIFO regardless of the physical input backend. */
interface QueuedController extends Controller {
    ControllerEventQueue events();
    long inputGeneration();
    default long connectionGeneration() { return 0; }
    void resetEvents(boolean active);
    ControllerInputDiagnostics diagnostics();
    default void configure(bms.player.beatoraja.PlayModeConfig.ControllerConfig config) { }
    default void windowActivityChanged() { }
    default boolean windowBaselineReady() { return true; }
    default void readSelectionButtons(boolean[] state) { }
}
