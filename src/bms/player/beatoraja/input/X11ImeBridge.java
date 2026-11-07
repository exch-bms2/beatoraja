package bms.player.beatoraja.input;

import bms.player.beatoraja.input.text.ImeBridge;
import bms.player.beatoraja.input.text.TextInputSession;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Opt-in X11 adapter for the explicitly selected, patched GLFW library. */
public final class X11ImeBridge implements ImeBridge {
    private final Supplier<X11ImePlatform> platforms;
    private final Thread owner = Thread.currentThread();
    private X11ImeSession worker;
    private long session;

    X11ImeBridge(Supplier<X11ImePlatform> platforms) { this.platforms = platforms; }

    public static ImeBridge create() {
        if (!Boolean.getBoolean("beatoraja.x11Ime") || !System.getProperty("os.name", "").startsWith("Linux")
                || !(Gdx.graphics instanceof Lwjgl3Graphics graphics)) return NONE;
        try {
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_X11) {
                warn("requires the X11 backend; using GLFW text input"); return NONE;
            }
            X11ImeNative.load();
            long window = graphics.getWindow().getWindowHandle();
            return new X11ImeBridge(() -> new X11ImeNative(window));
        } catch (RuntimeException | LinkageError failure) {
            warn("unavailable; using GLFW text input (" + failure.getClass().getSimpleName() + ")"); return NONE;
        }
    }
    /** Distribution diagnostics only: checks the explicitly selected JNI ABI without opening a display or enabling editing. */
    public static void verifyNativeLibrary() {
        if (!System.getProperty("os.name", "").startsWith("Linux"))
            throw new IllegalStateException("X11 IME native verification requires Linux");
        X11ImeNative.load();
    }
    static void warn(String value) { Logger.getGlobal().warning("X11 IME prototype " + value); }
    public boolean isAvailable() { return true; }
    public void begin(long id, TextInputSession.EditorState initial, Sink sink) {
        checkThread();
        if (worker != null) throw new IllegalStateException("IME already active");
        session = id;
        worker = new X11ImeSession(platforms.get());
        worker.begin(id, initial, sink);
    }
    public void update(long id, TextInputSession.EditorState state) { checkThread(); if (worker != null) worker.update(id, state); }
    public void caret(long id, double x, double y, double w, double h, double cw, double ch) {
        checkThread(); if (worker != null) worker.caret(id, x, y, w, h, cw, ch);
    }
    public void confirmComposition(long id) { checkThread(); if (worker != null) worker.confirmComposition(id); }
    public void cancelComposition(long id) { checkThread(); if (worker != null) worker.cancelComposition(id); }
    public void finish(long id) { checkThread(); if (worker != null) worker.finish(id); }
    public void close(long id) {
        checkThread();
        if (worker == null || session != id) return;
        try { worker.close(id); } finally { worker = null; }
    }
    private void checkThread() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("IME requires GLFW owner thread");
    }
}
