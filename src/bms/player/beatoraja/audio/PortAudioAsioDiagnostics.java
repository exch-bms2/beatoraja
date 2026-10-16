package bms.player.beatoraja.audio;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;

/** Optional ASIO queries against the same Windows PortAudio DLL already used by JPortAudio. */
final class PortAudioAsioDiagnostics {
    record BufferSizes(int minimum, int maximum, int preferred, int granularity) {
    }

    private PortAudioAsioDiagnostics() {
    }

    static BufferSizes getBufferSizes(int deviceId) {
        NativeLibrary kernel = NativeLibrary.getInstance("kernel32");
        Function getModuleHandle = kernel.getFunction("GetModuleHandleW", Function.ALT_CONVENTION);
        Pointer module = null;
        for (String name : new String[] { "portaudio_x64.dll", "portaudio_x86.dll", "portaudio.dll" }) {
            module = getModuleHandle.invokePointer(new Object[] { new WString(name) });
            if (module != null) break;
        }
        if (module == null) {
            throw new IllegalStateException("Loaded PortAudio DLL not found; no additional DLL will be loaded");
        }
        Pointer address = kernel.getFunction("GetProcAddress", Function.ALT_CONVENTION)
                .invokePointer(new Object[] { module, "PaAsio_GetAvailableBufferSizes" });
        if (address == null) {
            throw new IllegalStateException("Loaded PortAudio DLL does not export PaAsio_GetAvailableBufferSizes");
        }
        // Windows C long is 32-bit, including in an x64 process.
        var minimum = new IntByReference();
        var maximum = new IntByReference();
        var preferred = new IntByReference();
        var granularity = new IntByReference();
        int result = Function.getFunction(address).invokeInt(
                new Object[] { deviceId, minimum, maximum, preferred, granularity });
        if (result != 0) {
            throw new IllegalStateException("PaAsio_GetAvailableBufferSizes returned " + result);
        }
        return new BufferSizes(minimum.getValue(), maximum.getValue(), preferred.getValue(), granularity.getValue());
    }
}
