package bms.player.beatoraja.input.keyboard;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.windows.Kernel32;
import org.lwjgl.system.windows.MSG;
import org.lwjgl.system.windows.User32;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.libffi.LibFFI.*;

/** Observes removed MSGs on the GLFW owner thread only; never modifies or consumes a message. */
public final class WindowsKeyboardMessageClock implements AutoCloseable {
    // Retain the trampoline for process lifetime if Win32 refuses to detach it.
    private static final Set<WindowsKeyboardMessageClock> RETAINED = new HashSet<>();
    private final Message last = new Message();
    private final long hwnd, tickCount, nextHook, unhook;
    private final Hook callback;
    private final long handle;
    private boolean closed;

    public static WindowsKeyboardMessageClock attach(long glfwWindow) {
        try {
            var clock = new WindowsKeyboardMessageClock(glfwWindow);
            Logger.getGlobal().info("Windows keyboard timing: thread-local MSG observer attached");
            return clock;
        } catch (RuntimeException | LinkageError error) {
            Logger.getGlobal().log(Level.WARNING, "Windows keyboard timing: OS timestamps unavailable", error);
            return null;
        }
    }

    private WindowsKeyboardMessageClock(long glfwWindow) {
        if (Pointer.POINTER_SIZE != 8) throw new IllegalStateException("64-bit Win32 ABI required");
        hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0) throw new IllegalStateException("No Win32 window");
        var user = User32.getLibrary();
        long install = address(user, "SetWindowsHookExW");
        unhook = address(user, "UnhookWindowsHookEx");
        nextHook = address(user, "CallNextHookEx");
        tickCount = address(Kernel32.getLibrary(), "GetTickCount");
        callback = new Hook(this::removed);
        long hook;
        try {
            hook = callPPP(3, callback.address(), 0, Kernel32.GetCurrentThreadId(), install); // WH_GETMESSAGE
            if (hook == 0) throw new IllegalStateException("SetWindowsHookExW failed");
        } catch (RuntimeException | LinkageError error) {
            callback.free();
            throw error;
        }
        handle = hook;
    }

    private static long address(SharedLibrary library, String function) {
        long address = library.getFunctionAddress(function);
        if (address == 0) throw new UnsatisfiedLinkError(function);
        return address;
    }

    private long removed(int code, long wParam, long lParam) {
        try {
            if (!closed && code == 0 && (wParam & 1) != 0 && lParam != 0) { // PM_REMOVE
                last.clear();
                if (MSG.nhwnd(lParam) == hwnd && Message.isKey(MSG.nmessage(lParam))) {
                    long peekNanos = System.nanoTime();
                    last.set(MSG.nmessage(lParam), MSG.nlParam(lParam), MSG.ntime(lParam), callI(tickCount), peekNanos);
                }
            }
        } catch (RuntimeException | LinkageError error) {
            last.clear();
        }
        return callPPPP(0L, code, wParam, lParam, nextHook);
    }

    public KeyboardTimingTrace.Edge edge(KeyboardTimingTrace trace, int scan, boolean down, long callbackNanos) {
        return last.match(trace, scan, down, callbackNanos);
    }

    public void clear() { last.clear(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try {
            if (callPI(handle, unhook) != 0) {
                callback.free();
                return;
            }
        } catch (RuntimeException | LinkageError error) {
            Logger.getGlobal().log(Level.WARNING, "Windows keyboard timing: native detach failed", error);
        }
        synchronized (RETAINED) { RETAINED.add(this); }
        Logger.getGlobal().warning("Windows keyboard timing: retaining MSG callback after failed detach");
    }

    static final class Message {
        private int scan, messageTick, ageMs;
        private boolean down;
        private long peekNanos;

        static boolean isKey(int message) {
            return message == 0x100 || message == 0x101 || message == 0x104 || message == 0x105;
        }
        void set(int message, long lParam, int messageTick, int nowTick, long peekNanos) {
            scan = (int) (lParam >>> 16) & 0x1ff;
            down = (message & 1) == 0;
            this.messageTick = messageTick;
            ageMs = nowTick - messageTick; // DWORD uptime, including wrap.
            this.peekNanos = peekNanos;
        }
        KeyboardTimingTrace.Edge match(KeyboardTimingTrace trace, int scan, boolean down, long callbackNanos) {
            long peek = peekNanos;
            clear();
            if (peek == 0 || this.scan != scan || this.down != down || scan == 0
                    || callbackNanos - peek < 0 || callbackNanos - peek > 100_000_000L) {
                return trace.unmatched(callbackNanos);
            }
            return new KeyboardTimingTrace.Edge(trace, messageTick, ageMs, peek, callbackNanos);
        }
        void clear() { peekNanos = 0; }
    }

    @FunctionalInterface
    private interface Proc extends CallbackI {
        Callback.Descriptor ABI = new Callback.Descriptor(Proc.class, MethodHandles.lookup(),
                APIUtil.apiCreateCIF(APIUtil.apiStdcall(), ffi_type_pointer,
                        ffi_type_sint32, ffi_type_pointer, ffi_type_pointer));
        @Override default Callback.Descriptor getDescriptor() { return ABI; }
        @Override default void callback(long result, long args) {
            APIUtil.apiClosureRetP(result, invoke(memGetInt(memGetAddress(args)),
                    memGetAddress(memGetAddress(args + Pointer.POINTER_SIZE)),
                    memGetAddress(memGetAddress(args + 2L * Pointer.POINTER_SIZE))));
        }
        long invoke(int code, long wParam, long lParam);
    }

    private static final class Hook extends Callback implements Proc {
        private final Proc handler;
        Hook(Proc handler) { super(ABI); this.handler = handler; }
        @Override public long invoke(int code, long wParam, long lParam) { return handler.invoke(code, wParam, lParam); }
    }
}
