package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicIntegerArray;

import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.Gdx;
import bms.player.beatoraja.FramePacingDiagnostics;
import bms.player.beatoraja.GlfwWindowSupport;
import org.lwjgl.glfw.GLFW;

/** Publishes GLFW input to the polling thread without calling GLFW from that thread. */
public final class PollingLwjgl3Input extends DefaultLwjgl3Input {
    private final Thread owner = Thread.currentThread();
    private final FramePacingDiagnostics frameDiagnostics;
    private final Lwjgl3Window window;
    private final AtomicIntegerArray keys = new AtomicIntegerArray(pressedKeys.length);
    private final AtomicIntegerArray buttons = new AtomicIntegerArray(5);
    private volatile int pressedCount;
    private volatile int x, y;
    private final MouseDeltas scratch = new MouseDeltas();
    private volatile boolean scratchEnabled;
    private volatile boolean windowInputActive = true;
    private boolean iconified;
    private final boolean[] suppressedKeys = new boolean[256];
    private volatile java.util.function.Consumer<Boolean> activityListener;
    private final InputDiagnostics diagnostics = InputDiagnostics.createIfEnabled();
    private Lwjgl3InputCallbacks callbacks;
    private final KeyboardEventQueue keyboardEvents = new KeyboardEventQueue(diagnostics != null);
    private boolean overflowReported;
    private boolean scratchOverflowReported;
    private final WindowDiagnostics windowDiagnostics = WindowDiagnostics.createIfEnabled();
    private final MouseScratchGeometry scratchGeometry = new MouseScratchGeometry();
    private final MouseScratchPosition scratchPosition = new MouseScratchPosition();
    private final boolean windowPositionSupported;
    private boolean captureUnavailable;
    private final int[] scratchWindowX = new int[1], scratchWindowY = new int[1];
    private final float[] scratchScaleX = new float[1], scratchScaleY = new float[1];

    public PollingLwjgl3Input(Lwjgl3Window window) {
        this(window, null);
    }

    public PollingLwjgl3Input(Lwjgl3Window window, FramePacingDiagnostics frameDiagnostics) {
        this(window, frameDiagnostics, GlfwWindowSupport.hasWindowPosition());
    }

    PollingLwjgl3Input(Lwjgl3Window window, FramePacingDiagnostics frameDiagnostics, boolean windowPositionSupported) {
        super(window);
        this.frameDiagnostics = frameDiagnostics;
        this.window = window;
        this.windowPositionSupported = windowPositionSupported;
        windowInputActive = window.isFocused();
        callbacks = new Lwjgl3InputCallbacks(window.getWindowHandle(), this, diagnostics);
    }

    @Override public void update() {
        long started = frameDiagnostics != null ? System.nanoTime() : 0;
        if (callbacks != null) callbacks.attachWindowCallbacks();
        super.update();
        // Never invoke the listener while holding diagnostics: polling takes keyboard -> diagnostics.
        windowActivity(window.isFocused(), iconified);
        if (windowDiagnostics != null) windowDiagnostics.sample();
        if (keyboardEvents.isOverflowed() && !overflowReported) {
            overflowReported = true;
            java.util.logging.Logger.getGlobal().severe("Keyboard event queue overflow; stopping to avoid lost releases or invalid scores");
            Gdx.app.exit();
        }
        if (diagnostics == null) publishState();
        else synchronized (diagnostics) {
            publishState();
            diagnostics.publish(keys, buttons, window.isFocused(), System.nanoTime());
        }
        if (frameDiagnostics != null) frameDiagnostics.inputUpdated(started, System.nanoTime());
    }

    private void publishState() {
        int count = 0;
        boolean focused = windowInputActive;
        for (int i = 0; i < keys.length(); i++) {
            if (!super.isKeyPressed(i)) suppressedKeys[i] = false;
            int pressed = focused && !suppressedKeys[i] && super.isKeyPressed(i) ? 1 : 0;
            keys.set(i, pressed);
            count += pressed;
        }
        pressedCount = count;
        for (int i = 0; i < buttons.length(); i++) buttons.set(i, focused && super.isButtonPressed(i) ? 1 : 0);
        keyboardEvents.publish(focused, keys);
        x = super.getX();
        y = super.getY();
        boolean deliver = false;
        boolean drawable = Gdx.graphics.getWidth() > 0 && Gdx.graphics.getHeight() > 0
                && Gdx.graphics.getBackBufferWidth() > 0 && Gdx.graphics.getBackBufferHeight() > 0;
        if (scratchEnabled) {
            long handle = window.getWindowHandle();
            if (windowPositionSupported) GLFW.glfwGetWindowPos(handle, scratchWindowX, scratchWindowY);
            GLFW.glfwGetWindowContentScale(handle, scratchScaleX, scratchScaleY);
            boolean iconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) != 0;
            drawable &= !iconified;
            deliver = scratchGeometry.sample(handle, GLFW.glfwGetWindowMonitor(handle),
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                    Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
                    scratchWindowX[0], scratchWindowY[0], scratchScaleX[0], scratchScaleY[0], focused, iconified);
        }
        boolean sample = scratchEnabled && focused && drawable;
        if (sample) scratchPosition.sample(windowPositionSupported || super.isCursorCatched(), deliver,
                x, y, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        else scratchPosition.invalidate();
        if (diagnostics != null) diagnostics.mouseScratch().sample(scratchEnabled, focused, x, y,
                ((Lwjgl3Graphics) Gdx.graphics).getLogicalWidth(), ((Lwjgl3Graphics) Gdx.graphics).getLogicalHeight(),
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Gdx.graphics.getBackBufferWidth(),
                Gdx.graphics.getBackBufferHeight(), System.nanoTime(), scratchPosition.delivered,
                scratchPosition.dx, scratchPosition.dy);
        if (sample) {
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            // Already queued samples are real pre-transition movement; do not clear or reinterpret them.
            if (scratchPosition.delivered) scratch.add(scratchPosition.dx, scratchPosition.dy);
            checkMouseScratchOverflow();
            if (scratchPosition.delivered && diagnostics != null) diagnostics.scratchPublished(scratchPosition.dx, scratchPosition.dy, System.nanoTime());
            if (scratchPosition.recenter) {
                if (diagnostics != null) diagnostics.mouseScratch().warp(centerX, centerY);
                // GLFW_CURSOR_DISABLED changes only virtual position, including on Wayland.
                super.setCursorPosition(centerX, centerY);
                x = centerX;
                y = centerY;
            }
        }
    }

    public boolean isWindowInputActive() { return windowInputActive; }
    /** MainController captures immediately for continuous Wayland scratch during play, not in text/config UIs. */
    public boolean requiresMouseScratchCapture() { return !windowPositionSupported && scratchEnabled && !captureUnavailable; }

    @Override public void setCursorCatched(boolean catched) {
        if ((!windowPositionSupported && captureUnavailable && catched) || super.isCursorCatched() == catched) return;
        invalidateMouseScratchCenter();
        try {
            if (windowPositionSupported || !catched) { super.setCursorCatched(catched); return; }
            // GLFW's requested mode is not proof that the compositor supports pointer locking.
            boolean[] refused = {false};
            var previous = GLFW.glfwSetErrorCallback(null);
            org.lwjgl.glfw.GLFWErrorCallback observer = null;
            try {
                observer = org.lwjgl.glfw.GLFWErrorCallback.create((error, message) -> {
                    if (error == GLFW.GLFW_FEATURE_UNAVAILABLE || error == GLFW.GLFW_FEATURE_UNIMPLEMENTED
                            || error == GLFW.GLFW_PLATFORM_ERROR) refused[0] = true;
                    if (previous != null) previous.invoke(error, message);
                });
                GLFW.glfwSetErrorCallback(observer);
                super.setCursorCatched(true);
            } finally {
                GLFW.glfwSetErrorCallback(previous);
                if (observer != null) observer.free();
            }
            if (refused[0]) {
                captureUnavailable = true;
                super.setCursorCatched(false);
                java.util.logging.Logger.getGlobal().warning("Wayland cursor capture unavailable; using bounded cursor movement without warping");
            }
        } finally { invalidateMouseScratchCenter(); }
    }

    synchronized void setActivityListener(java.util.function.Consumer<Boolean> listener) { activityListener = listener; }
    synchronized void clearActivityListener(java.util.function.Consumer<Boolean> listener) {
        if (activityListener == listener) activityListener = null;
    }

    /** Called on the GLFW owner thread, including loss/regain within one event-loop iteration. */
    void windowActivity(boolean focused, boolean minimized) {
        iconified = minimized;
        boolean active = focused && !minimized;
        if (active == windowInputActive) return;
        windowInputActive = active;
        for (int i = 0; i < keys.length(); i++) {
            suppressedKeys[i] = super.isKeyPressed(i);
            keys.set(i, 0);
        }
        for (int i = 0; i < buttons.length(); i++) buttons.set(i, 0);
        pressedCount = 0;
        keyboardEvents.reset(keys);
        resetMouseScratchDeltas();
        invalidateMouseScratchCenter();
        var listener = activityListener;
        if (listener != null) listener.accept(active);
    }

    void windowFocus(boolean focused) { windowActivity(focused, iconified); }
    void windowIconified(boolean minimized) { windowActivity(window.isFocused(), minimized); }

    void keyboardEdge(int key, boolean down, long time) {
        if (key < 0 || key >= suppressedKeys.length) return;
        if (!down) suppressedKeys[key] = false;
        if (windowInputActive && !suppressedKeys[key]) keyboardEvents.offer(key, down, time);
    }

    @Override public boolean isKeyPressed(int key) {
        if (Thread.currentThread() == owner) return super.isKeyPressed(key);
        if (diagnostics == null || key < 0 || key >= keys.length())
            return key == -1 ? pressedCount > 0 : key >= 0 && key < keys.length() && keys.get(key) != 0;
        synchronized (diagnostics) {
            boolean down = keys.get(key) != 0;
            diagnostics.read(key, down, System.nanoTime());
            return down;
        }
    }

    @Override public boolean isButtonPressed(int button) {
        if (Thread.currentThread() == owner) return super.isButtonPressed(button);
        if (diagnostics == null || button < 0 || button >= buttons.length())
            return button >= 0 && button < buttons.length() && buttons.get(button) != 0;
        synchronized (diagnostics) {
            boolean down = buttons.get(button) != 0;
            diagnostics.read(InputDiagnostics.KEY_COUNT + button, down, System.nanoTime());
            return down;
        }
    }

    @Override public int getX() { return Thread.currentThread() == owner ? super.getX() : x; }
    @Override public int getY() { return Thread.currentThread() == owner ? super.getY() : y; }

    public void setMouseScratchEnabled(boolean enabled) {
        scratchEnabled = enabled;
        invalidateMouseScratchCenter();
        resetMouseScratchDeltas();
    }

    /** No GLFW calls, queue clearing, algorithm reset or judgment events. Safe for configuration callers. */
    public void invalidateMouseScratchCenter() {
        if (scratchGeometry != null) scratchGeometry.invalidate();
    }

    public void resetMouseScratchDeltas() {
        if (diagnostics == null) scratch.clear();
        else synchronized (diagnostics) {
            scratch.clear();
            diagnostics.resetScratch();
            diagnostics.mouseScratch().reset();
        }
    }

    public void consumeMouseScratchDeltas(MouseMotionQueue.Batch batch) {
        if (diagnostics == null) scratch.drain(batch);
        else synchronized (diagnostics) {
            scratch.drain(batch);
            diagnostics.scratchConsumed(System.nanoTime());
            diagnostics.mouseScratch().consumedQueue(batch, System.nanoTime());
        }
        checkMouseScratchOverflow();
    }

    public boolean isMouseScratchBatchCurrent(MouseMotionQueue.Batch batch) { return scratch.isCurrent(batch); }

    private synchronized void checkMouseScratchOverflow() {
        if (scratch.isOverflowed() && !scratchOverflowReported) {
            scratchOverflowReported = true;
            java.util.logging.Logger.getGlobal().severe("Mouse scratch motion queue overflow; stopping to avoid invalid scores");
            if (Gdx.app != null) Gdx.app.exit();
        }
    }

    /** Legacy aggregate diagnostic probe; gameplay must consume the FIFO batch instead. */
    public long consumeMouseScratchDelta() {
        if (diagnostics == null) return scratch.drain();
        synchronized (diagnostics) {
            long delta = scratch.drain();
            diagnostics.scratchConsumed(System.nanoTime());
            diagnostics.mouseScratch().consumed((int) (delta >> 32), (int) delta, System.nanoTime());
            return delta;
        }
    }

    public InputDiagnostics getDiagnostics() { return diagnostics; }
    public FramePacingDiagnostics getFrameDiagnostics() { return frameDiagnostics; }
    public KeyboardEventQueue getKeyboardEvents() { return keyboardEvents; }
    public void configureKeyboardEvents(int[] assignments) { keyboardEvents.configure(assignments, keys); }
    public void resetKeyboardEvents() { keyboardEvents.reset(keys); }

    @Override public void windowHandleChanged(long handle) {
        // Called by the superclass constructor before our fields are initialized.
        if (callbacks != null) {
            callbacks.close(callbacks.handle == handle);
            callbacks = null;
        }
        super.windowHandleChanged(handle);
        captureUnavailable = false;
        if (scratch != null) resetMouseScratchDeltas();
        invalidateMouseScratchCenter();
        if (diagnostics != null) diagnostics.mouseScratch().resetCallbacks();
        if (keyboardEvents != null) callbacks = new Lwjgl3InputCallbacks(handle, this, diagnostics);
    }

    @Override public void dispose() {
        try {
            activityListener = null;
            resetMouseScratchDeltas();
            if (callbacks != null) {
                callbacks.close(true);
                callbacks = null;
            }
        } finally {
            if (diagnostics != null) diagnostics.close();
            super.dispose();
        }
    }

    /** Compatibility name for existing aggregate probes and test fixtures. */
    public static final class MouseDeltas extends MouseMotionQueue { }
}
