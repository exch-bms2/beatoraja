package bms.player.beatoraja.input.controller;

/** SDL3 Gamepad acquisition on the existing input worker, exposing SDL/Jamepad logical IDs. */
public final class SdlAsyncControllerManager extends SdlRawControllerManager {
    public SdlAsyncControllerManager() {
        super(ControllerBackend.SDL_ASYNC);
    }
}
