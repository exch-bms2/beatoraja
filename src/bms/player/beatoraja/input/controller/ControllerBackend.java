package bms.player.beatoraja.input.controller;

import com.badlogic.gdx.controllers.Controllers;

/** Selected once, before the first gdx-controllers manager is created. */
public enum ControllerBackend {
    JINPUT, SDL, SDL_RAW, SDL_RAW_ASYNC, SDL_ASYNC;

    /** Acquisition variants share one persistent binding bank. */
    public ControllerBackend bindingBackend() {
        return switch (this) {
            case SDL_RAW_ASYNC -> SDL_RAW;
            case SDL_ASYNC -> SDL;
            default -> this;
        };
    }

    public void configure() {
        Controllers.preferredManager = switch (this) {
            case JINPUT -> JInputControllerManager.class.getName();
            case SDL -> SdlControllerManager.class.getName();
            case SDL_RAW -> SdlRawControllerManager.class.getName();
            case SDL_RAW_ASYNC -> SdlRawAsyncControllerManager.class.getName();
            case SDL_ASYNC -> SdlAsyncControllerManager.class.getName();
        };
        java.util.logging.Logger.getGlobal().info("Controller backend selected: " + this + "; restart required to change; no automatic fallback");
    }
}
