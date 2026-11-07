package bms.player.beatoraja.input.controller;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.LongConsumer;
import org.lwjgl.sdl.SDL_GUID;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.sdl.SDLJoystick.*;

/** Acquisition-thread-owned handles and stable slots across SDL instance ID changes. */
final class SdlDeviceRegistry {
    private final ArrayList<SdlRawSource.Device> devices = new ArrayList<>();
    private final ArrayList<Long> handles = new ArrayList<>();
    private final LongConsumer closeHandle;
    private long version;

    SdlDeviceRegistry(LongConsumer closeHandle) { this.closeHandle = closeHandle; }

    static String identity(long joystick, String name) {
        try (var stack = MemoryStack.stackPush()) {
            var guid = SDL_GetJoystickGUID(joystick, SDL_GUID.malloc(stack));
            var text = stack.malloc(33);
            org.lwjgl.sdl.SDLGUID.SDL_GUIDToString(guid, text);
            String model = MemoryUtil.memASCII(MemoryUtil.memAddress(text)) + ":" + name;
            String serial = SDL_GetJoystickSerial(joystick);
            if (serial != null && !serial.isBlank()) return model + ":serial:" + serial;
            String path = SDL_GetJoystickPath(joystick);
            return path != null && !path.isBlank() ? model + ":path:" + path : model;
        }
    }

    boolean contains(int id) {
        for (int i = 0; i < devices.size(); i++)
            if (handles.get(i) != 0 && devices.get(i).id() == id) return true;
        return false;
    }

    void add(SdlRawSource.Device device, long handle) {
        for (int i = 0; i < devices.size(); i++) {
            var old = devices.get(i);
            if (handles.get(i) == 0 && Objects.equals(old.identity(), device.identity())
                    && old.buttons() == device.buttons() && old.axes() == device.axes() && old.hats() == device.hats()) {
                devices.set(i, device);
                handles.set(i, handle);
                version++;
                return;
            }
        }
        devices.add(device);
        handles.add(handle);
        version++;
    }

    void remove(int id) {
        for (int i = 0; i < devices.size(); i++) if (devices.get(i).id() == id && handles.get(i) != 0) {
            long handle = handles.set(i, 0L);
            closeHandle.accept(handle);
            version++;
            return;
        }
    }

    SdlRawSource.Device[] devices() { return devices.toArray(SdlRawSource.Device[]::new); }
    long version() { return version; }
    long handle(int slot) { return handles.get(slot); }
    SdlRawSource.Device device(int slot) { return devices.get(slot); }

    void close() {
        for (int i = 0; i < handles.size(); i++) {
            long handle = handles.set(i, 0L);
            if (handle != 0) closeHandle.accept(handle);
        }
    }
}
