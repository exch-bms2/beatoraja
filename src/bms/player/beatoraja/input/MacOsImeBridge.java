package bms.player.beatoraja.input;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import java.nio.file.Path;
import java.util.logging.Logger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFWNativeCocoa;

/** Opt-in AppKit editor hosted by the current GLFW window; no JavaFX/AWT initialization. */
public final class MacOsImeBridge implements ImeBridge {
    private static boolean loaded;
    private final long window;
    private final Thread owner = Thread.currentThread();
    private long handle, session;
    private Sink sink;

    private MacOsImeBridge(long window) { this.window = window; }

    public static ImeBridge create() {
        if (!Boolean.getBoolean("beatoraja.macosIme") || !System.getProperty("os.name", "").startsWith("Mac")
                || !(Gdx.graphics instanceof Lwjgl3Graphics graphics)) return NONE;
        try {
            synchronized (MacOsImeBridge.class) {
                if (!loaded) {
                    System.load(Path.of(System.getProperty("beatoraja.macosIme.library",
                            "natives/libbeatoraja-ime.dylib")).toAbsolutePath().toString());
                    loaded = true;
                }
            }
            return new MacOsImeBridge(GLFWNativeCocoa.glfwGetCocoaWindow(graphics.getWindow().getWindowHandle()));
        } catch (LinkageError | RuntimeException failure) {
            Logger.getGlobal().warning("macOS IME prototype unavailable; using GLFW text input: " + failure);
            return NONE;
        }
    }

    public boolean isAvailable() { return window != 0; }
    public void begin(long id, TextInputSession.EditorState initial, Sink sink) {
        checkThread();
        if (handle != 0) throw new IllegalStateException("IME already active");
        this.session = id; this.sink = sink;
        handle = open(window, id, initial.text(), initial.anchor(), initial.cursor());
        if (handle == 0) { this.sink = null; throw new IllegalStateException("Cannot attach macOS IME to GLFW window"); }
    }
    public void update(long id, TextInputSession.EditorState state) {
        checkThread();
        if (active(id)) updateNative(handle, state.text(), state.anchor(), state.cursor());
    }
    public void caret(long id, double x, double y, double w, double h, double cw, double ch) {
        checkThread();
        if (active(id) && Double.isFinite(x + y + w + h + cw + ch) && w >= 0 && h > 0 && cw > 0 && ch > 0)
            caretNative(handle, x, y, w, h, cw, ch);
    }
    public void confirmComposition(long id) { checkThread(); if (active(id)) confirmNative(handle); }
    public void cancelComposition(long id) { checkThread(); if (active(id)) cancelNative(handle); }
    public void finish(long id) {
        checkThread();
        if (active(id)) { confirmNative(handle); sink.finished(id); }
    }
    public void close(long id) {
        checkThread();
        if (!active(id)) return;
        long pointer = handle;
        handle = 0; sink = null;
        closeNative(pointer);
    }
    private boolean active(long id) { return handle != 0 && id == session && sink != null; }
    private void checkThread() { if (Thread.currentThread() != owner) throw new IllegalStateException("IME requires GLFW owner thread"); }

    // AppKit calls on the GLFW/main thread. Sink messages are still queued by TextInputSession.
    private void nativeEvent(long id, int kind, String text, int anchor, int cursor, boolean consumed) {
        if (sink == null || id != session) return;
        switch (kind) {
            case 0 -> sink.committed(id, new TextInputSession.EditorState(text, anchor, cursor));
            case 1 -> sink.preedit(id, text, cursor);
            case 2 -> sink.cancelled(id);
            case 3 -> sink.command(id, Command.SUBMIT, consumed);
            default -> throw new IllegalArgumentException("Unknown IME notification");
        }
    }
    long nativeHandle() { return handle; }
    private native long open(long window, long id, String text, int anchor, int cursor);
    private native void updateNative(long handle, String text, int anchor, int cursor);
    private native void caretNative(long handle, double x, double y, double w, double h, double cw, double ch);
    private native void confirmNative(long handle);
    private native void cancelNative(long handle);
    private native void closeNative(long handle);
}
