package bms.player.beatoraja.input;

import bms.player.beatoraja.input.text.ime.X11ImePlatform;
import com.badlogic.gdx.Gdx;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.lwjgl.glfw.GLFW;

/** JNI attachment for the opt-in GLFW. Keep this binary name and callback signature for existing libraries. */
public final class X11ImeNative implements X11ImePlatform {
    private static boolean loaded;
    private final long window;
    private long handle;
    private Listener listener;
    private boolean positionWarned;

    private X11ImeNative(long window) { this.window = window; }

    public static X11ImePlatform forWindow(long window) { return new X11ImeNative(window); }

    public static synchronized void load() {
        if (loaded) return;
        String selected = System.getProperty("org.lwjgl.glfw.libname", "");
        Path library = Path.of(System.getProperty("beatoraja.x11Ime.library", "natives/libglfw-beatoraja-x11-ime.so")).toAbsolutePath().normalize();
        if (selected.isEmpty() || !Path.of(selected).toAbsolutePath().normalize().equals(library))
            throw new IllegalStateException("Select the same X11 IME library for GLFW and JNI before startup");
        // Do not load a second GLFW: both Java bindings must refer to this exact library.
        System.load(library.toString());
        if (versionNative() != 1) throw new IllegalStateException("Unsupported X11 IME bridge ABI");
        loaded = true;
    }
    public void attach(Listener listener) {
        this.listener = listener;
        handle = openNative(window);
        if (handle == 0) throw new IllegalStateException("XIM callback style unavailable or another editor is active");
        Logger.getGlobal().info("X11 IME prototype attached; candidate positioning is best effort with XIM callbacks");
    }
    public void reset() { if (handle != 0) resetNative(handle); }
    public void caret(Caret caret) {
        if (handle == 0) return;
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var width = stack.mallocInt(1); var height = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, width, height);
            if (width.get(0) > 0 && height.get(0) > 0) {
                int[] rect = caret.nativeRectangle(width.get(0), height.get(0));
                if (!caretNative(handle, rect[0], rect[1], rect[2], rect[3]) && !positionWarned) {
                    positionWarned = true;
                    Logger.getGlobal().warning("X11 IME prototype candidate position request refused by the input method");
                }
            }
        }
    }
    public void close() {
        long value = handle; handle = 0; listener = null;
        if (value != 0) closeNative(value);
    }
    public String clipboard() { return Gdx.app.getClipboard().getContents(); }
    public void clipboard(String value) { Gdx.app.getClipboard().setContents(value); }
    private void nativeEvent(int kind, byte[] utf8, int first, int second, boolean consumed) {
        Listener current = listener;
        if (current != null) current.event(kind, utf8, first, second, consumed);
    }
    private static native int versionNative();
    private native long openNative(long window);
    private static native void resetNative(long handle);
    private static native boolean caretNative(long handle, int x, int y, int right, int bottom);
    private static native void closeNative(long handle);
}
