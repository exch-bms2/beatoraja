package bms.player.beatoraja.input;

import java.util.ArrayList;
import org.lwjgl.sdl.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLJoystick.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLHints.*;

/** SDL3 joystick API, isolated from the existing SDL2/Jamepad GameController backend. */
final class SdlRawNative implements SdlRawSource {
    private static final int SUBSYSTEMS = SDL_INIT_JOYSTICK | SDL_INIT_EVENTS;
    private final Thread owner = Thread.currentThread();
    private final boolean async;
    private final String previousAutoUpdate;
    private final Device[] devices;
    private final long[] handles;
    private SDL_Event event;
    private boolean closed;

    SdlRawNative() {
        this(false);
    }

    SdlRawNative(boolean async) {
        this.async = async;
        previousAutoUpdate = async ? SDL_GetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS) : null;
        if (!SDL_InitSubSystem(SUBSYSTEMS)) throw new IllegalStateException("SDL_RAW init failed: " + SDL_GetError());
        var opened = new ArrayList<Long>();
        try {
            if (async && !SDL_SetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS, "0"))
                throw new IllegalStateException("SDL_RAW_ASYNC cannot disable automatic joystick updates");
            var found = new ArrayList<Device>();
            var ids = SDL_GetJoysticks();
            try {
                if (ids == null) throw new IllegalStateException("SDL_RAW enumeration failed: " + SDL_GetError());
                for (int i = 0; i < ids.remaining(); i++) {
                    long handle = SDL_OpenJoystick(ids.get(i));
                    if (handle == 0) throw new IllegalStateException("SDL_RAW open failed: " + SDL_GetError());
                    opened.add(handle);
                    int buttons = SDL_GetNumJoystickButtons(handle), axes = SDL_GetNumJoystickAxes(handle), hats = SDL_GetNumJoystickHats(handle);
                    if (buttons < 0 || buttons > 256 || axes < 0 || axes > 256 || hats < 0 || hats > 256)
                        throw new IllegalStateException("Invalid SDL_RAW component counts");
                    String name = SDL_GetJoystickName(handle);
                    found.add(new Device(ids.get(i), name != null ? name : "SDL raw joystick", buttons, axes, hats));
                }
            } finally { if (ids != null) SDLStdinc.SDL_free(ids); }
            handles = new long[opened.size()];
            for (int i = 0; i < handles.length; i++) handles[i] = opened.get(i);
            devices = found.toArray(Device[]::new);
            event = SDL_Event.calloc();
            SDL_SetJoystickEventsEnabled(true);
        } catch (RuntimeException | Error failure) {
            for (long handle : opened) SDL_CloseJoystick(handle);
            if (event != null) event.free();
            SDL_QuitSubSystem(SUBSYSTEMS);
            restoreAutoUpdate();
            throw failure;
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL_RAW requires its owner thread");
    }
    private void checkAcquisition() {
        if (!async) checkThread();
        if (closed) throw new IllegalStateException("SDL_RAW source is closed");
    }
    private void restoreAutoUpdate() {
        if (!async) return;
        if (previousAutoUpdate == null) SDL_ResetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS);
        else SDL_SetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS, previousAutoUpdate);
    }
    public Device[] devices() { return devices.clone(); }
    public void update() { checkAcquisition(); SDL_UpdateJoysticks(); }
    private boolean readEvent() {
        if (!async) return SDL_PollEvent(event);
        // Unlike PollEvent, PeepEvents never pumps the main-thread window event loop.
        int count = nSDL_PeepEvents(event.address(), 1, SDL_GETEVENT,
                SDL_EVENT_JOYSTICK_AXIS_MOTION, SDL_EVENT_JOYSTICK_UPDATE_COMPLETE);
        if (count < 0) throw new IllegalStateException("SDL_RAW_ASYNC event read failed: " + SDL_GetError());
        return count != 0;
    }
    public boolean next(Event target) {
        checkAcquisition();
        while (readEvent()) {
            long address = event.address();
            switch (event.type()) {
                case SDL_EVENT_JOYSTICK_BUTTON_DOWN, SDL_EVENT_JOYSTICK_BUTTON_UP -> {
                    target.device = SDL_JoyButtonEvent.nwhich(address); target.type = Event.BUTTON;
                    target.index = Byte.toUnsignedInt(SDL_JoyButtonEvent.nbutton(address));
                    target.value = SDL_JoyButtonEvent.ndown(address) ? 1 : 0;
                    return true;
                }
                case SDL_EVENT_JOYSTICK_HAT_MOTION -> {
                    target.device = SDL_JoyHatEvent.nwhich(address); target.type = Event.HAT;
                    target.index = Byte.toUnsignedInt(SDL_JoyHatEvent.nhat(address));
                    target.value = Byte.toUnsignedInt(SDL_JoyHatEvent.nvalue(address));
                    return true;
                }
                case SDL_EVENT_JOYSTICK_AXIS_MOTION -> {
                    target.device = SDL_JoyAxisEvent.nwhich(address); target.type = Event.AXIS;
                    target.index = Byte.toUnsignedInt(SDL_JoyAxisEvent.naxis(address));
                    target.value = normalized(SDL_JoyAxisEvent.nvalue(address));
                    return true;
                }
                default -> { }
            }
        }
        return false;
    }
    static float normalized(short value) { return value < 0 ? value / 32768f : value / 32767f; }
    public boolean connected(int slot) { checkAcquisition(); return SDL_JoystickConnected(handles[slot]); }
    public boolean button(int slot, int button) { checkAcquisition(); return SDL_GetJoystickButton(handles[slot], button); }
    public float axis(int slot, int axis) { checkAcquisition(); return normalized(SDL_GetJoystickAxis(handles[slot], axis)); }
    public int hat(int slot) { checkAcquisition(); return devices[slot].hats() == 0 ? 0 : Byte.toUnsignedInt(SDL_GetJoystickHat(handles[slot], 0)); }
    public void close() {
        checkThread();
        if (closed) return;
        closed = true;
        try { for (long handle : handles) SDL_CloseJoystick(handle); }
        finally {
            try { event.free(); SDL_QuitSubSystem(SUBSYSTEMS); }
            finally { restoreAutoUpdate(); }
        }
    }
}
