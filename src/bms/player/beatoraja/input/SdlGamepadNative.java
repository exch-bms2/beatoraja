package bms.player.beatoraja.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLGamepad.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLHints.*;
import static org.lwjgl.sdl.SDLError.SDL_GetError;

/** SDL3 Gamepad worker API, returning only the legacy Jamepad button/axis namespace. */
final class SdlGamepadNative implements SdlRawSource {
    private static final int SUBSYSTEMS = SDL_INIT_GAMEPAD | SDL_INIT_EVENTS;
    private final Thread owner = Thread.currentThread();
    private final String previousAutoUpdate = SDL_GetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS);
    private final Device[] devices;
    private final long[] handles;
    private SDL_Event event;
    private boolean closed;

    SdlGamepadNative() {
        if (!SDL_InitSubSystem(SUBSYSTEMS)) throw new IllegalStateException("SDL_ASYNC init failed: " + SDL_GetError());
        var opened = new ArrayList<Long>();
        try {
            if (!SDL_SetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS, "0"))
                throw new IllegalStateException("SDL_ASYNC cannot disable automatic joystick updates");
            loadLegacyMappings();
            var found = new ArrayList<Device>();
            var ids = SDL_GetGamepads();
            try {
                if (ids == null) throw new IllegalStateException("SDL_ASYNC enumeration failed: " + SDL_GetError());
                for (int i = 0; i < ids.remaining(); i++) {
                    long handle = SDL_OpenGamepad(ids.get(i));
                    if (handle == 0) throw new IllegalStateException("SDL_ASYNC open failed: " + SDL_GetError());
                    opened.add(handle);
                    String name = SDL_GetGamepadName(handle);
                    int buttons = SdlGamepadCompatibility.BUTTONS.length;
                    while (buttons > 1 && !SDL_GamepadHasButton(handle, SdlGamepadCompatibility.BUTTONS[buttons - 1])) buttons--;
                    int axes = SdlGamepadCompatibility.AXES.length;
                    while (axes > 0 && !SDL_GamepadHasAxis(handle, SdlGamepadCompatibility.AXES[axes - 1])) axes--;
                    found.add(new Device(ids.get(i), name != null ? name : "Unknown", buttons, axes, 0));
                    Logger.getGlobal().info("SDL_ASYNC mapping: id=" + ids.get(i) + ", name=" + name
                            + ", joystickName=" + SDLJoystick.SDL_GetJoystickName(SDL_GetGamepadJoystick(handle))
                            + ", mapping=" + SDL_GetGamepadMapping(handle));
                }
            } finally { if (ids != null) SDLStdinc.SDL_free(ids); }
            handles = new long[opened.size()];
            for (int i = 0; i < handles.length; i++) handles[i] = opened.get(i);
            devices = found.toArray(Device[]::new);
            event = SDL_Event.calloc();
            SDLJoystick.SDL_SetJoystickEventsEnabled(true);
            SDL_SetGamepadEventsEnabled(true);
        } catch (RuntimeException | Error failure) {
            for (long handle : opened) SDL_CloseGamepad(handle);
            if (event != null) event.free();
            SDL_QuitSubSystem(SUBSYSTEMS);
            restoreHint();
            throw failure;
        }
    }

    private static void loadLegacyMappings() {
        // Same bundled database as the unchanged SDL2/Jamepad backend, not a downloaded replacement.
        try (var input = SdlGamepadNative.class.getResourceAsStream("/gamecontrollerdb.txt")) {
            if (input == null) throw new IllegalStateException("Missing bundled Jamepad gamecontrollerdb.txt");
            byte[] bytes = input.readAllBytes();
            var data = MemoryUtil.memAlloc(bytes.length);
            try {
                data.put(bytes).flip();
                long io = SDLIOStream.SDL_IOFromConstMem(data);
                if (io == 0) throw new IllegalStateException("SDL_ASYNC mapping stream failed: " + SDL_GetError());
                int count = SDL_AddGamepadMappingsFromIO(io, true);
                if (count < 0) throw new IllegalStateException("SDL_ASYNC mapping import failed: " + SDL_GetError());
                Logger.getGlobal().info("SDL_ASYNC imported bundled Jamepad mappings: " + count
                        + "; SDL2/SDL3 physical recognition may still differ; verify bindings");
            } finally { MemoryUtil.memFree(data); }
        } catch (IOException failure) { throw new IllegalStateException("SDL_ASYNC mapping read failed", failure); }
    }

    private void checkOpen() { if (closed) throw new IllegalStateException("SDL_ASYNC source is closed"); }
    private void restoreHint() {
        if (previousAutoUpdate == null) SDL_ResetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS);
        else SDL_SetHint(SDL_HINT_AUTO_UPDATE_JOYSTICKS, previousAutoUpdate);
    }
    public Device[] devices() { return devices.clone(); }
    public void update() { checkOpen(); SDL_UpdateGamepads(); }
    public boolean next(Event target) {
        checkOpen();
        while (true) {
            // Gamepad events are generated from joystick events by SDL's event watcher.
            // Drain both queues, forwarding only mapped buttons; otherwise raw events accumulate forever.
            int count = nSDL_PeepEvents(event.address(), 1, SDL_GETEVENT,
                    SDL_EVENT_JOYSTICK_AXIS_MOTION, SDL_EVENT_GAMEPAD_STEAM_HANDLE_UPDATED);
            if (count < 0) throw new IllegalStateException("SDL_ASYNC event read failed: " + SDL_GetError());
            if (count == 0) return false;
            if (event.type() == SDL_EVENT_GAMEPAD_BUTTON_DOWN || event.type() == SDL_EVENT_GAMEPAD_BUTTON_UP) {
                int legacy = SdlGamepadCompatibility.legacyButton(Byte.toUnsignedInt(SDL_GamepadButtonEvent.nbutton(event.address())));
                if (legacy >= 0) {
                    target.device = SDL_GamepadButtonEvent.nwhich(event.address()); target.type = Event.BUTTON;
                    target.index = legacy; target.value = SDL_GamepadButtonEvent.ndown(event.address()) ? 1 : 0;
                    return true;
                }
            }
            // Axes use the final mapped snapshot, not one scratch update per native event.
        }
    }
    public boolean connected(int slot) { checkOpen(); return SDL_GamepadConnected(handles[slot]); }
    public boolean button(int slot, int button) {
        checkOpen();
        return button >= 0 && button < SdlGamepadCompatibility.BUTTONS.length
                && SDL_GetGamepadButton(handles[slot], SdlGamepadCompatibility.BUTTONS[button]);
    }
    public float axis(int slot, int axis) {
        checkOpen();
        return axis >= 0 && axis < SdlGamepadCompatibility.AXES.length
                ? SdlGamepadCompatibility.normalized(SDL_GetGamepadAxis(handles[slot], SdlGamepadCompatibility.AXES[axis])) : 0;
    }
    public int hat(int slot) { checkOpen(); return 0; }
    public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("SDL_ASYNC shutdown requires its owner thread");
        if (closed) return;
        closed = true;
        try { for (long handle : handles) SDL_CloseGamepad(handle); }
        finally {
            try { event.free(); SDL_QuitSubSystem(SUBSYSTEMS); }
            finally { restoreHint(); }
        }
    }
}
