package bms.player.beatoraja.input;

/** SDL3 raw acquisition on the existing input polling thread, with the SDL_RAW binding bank. */
public final class SdlRawAsyncControllerManager extends SdlRawControllerManager {
    public SdlRawAsyncControllerManager() {
        super(true);
    }
}
