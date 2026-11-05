package bms.player.beatoraja.input;

import com.badlogic.gdx.controllers.Controllers;

/** Selected once, before the first gdx-controllers manager is created. */
public enum ControllerBackend {
    JINPUT, SDL;

    public void configure() {
        Controllers.preferredManager = switch (this) {
            case JINPUT -> JInputControllerManager.class.getName();
            case SDL -> SdlControllerManager.class.getName();
        };
        java.util.logging.Logger.getGlobal().info("Controller backend selected: " + this + "; restart required to change; no automatic fallback");
    }
}
