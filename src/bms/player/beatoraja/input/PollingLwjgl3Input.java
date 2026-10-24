package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicIntegerArray;

import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.Gdx;
import org.lwjgl.glfw.GLFW;

/** Publishes GLFW input to the polling thread without calling GLFW from that thread. */
public final class PollingLwjgl3Input extends DefaultLwjgl3Input {
    private final Thread owner = Thread.currentThread();
    private final Lwjgl3Window window;
    private final AtomicIntegerArray keys = new AtomicIntegerArray(pressedKeys.length);
    private final AtomicIntegerArray buttons = new AtomicIntegerArray(5);
    private volatile int pressedCount;
    private volatile int x, y;
    private final MouseDeltas scratch = new MouseDeltas();
    private volatile boolean scratchEnabled;
    private final InputDiagnostics diagnostics = InputDiagnostics.createIfEnabled();
    private Lwjgl3InputCallbacks callbacks;
    private final KeyboardEventQueue keyboardEvents = new KeyboardEventQueue(diagnostics != null);
    private boolean overflowReported;
    private boolean scratchOverflowReported;
    private final WindowDiagnostics windowDiagnostics = WindowDiagnostics.createIfEnabled();
    private final MouseScratchGeometry scratchGeometry = new MouseScratchGeometry();
    private final int[] scratchWindowX = new int[1], scratchWindowY = new int[1];
    private final float[] scratchScaleX = new float[1], scratchScaleY = new float[1];

    public PollingLwjgl3Input(Lwjgl3Window window) {
        super(window);
        this.window = window;
        callbacks = new Lwjgl3InputCallbacks(window.getWindowHandle(), this, diagnostics);
    }

    @Override public void update() {
        super.update();
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
    }

    private void publishState() {
        int count = 0;
        boolean focused = window.isFocused();
        for (int i = 0; i < keys.length(); i++) {
            int pressed = focused && super.isKeyPressed(i) ? 1 : 0;
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
            GLFW.glfwGetWindowPos(handle, scratchWindowX, scratchWindowY);
            GLFW.glfwGetWindowContentScale(handle, scratchScaleX, scratchScaleY);
            boolean iconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) != 0;
            drawable &= !iconified;
            deliver = scratchGeometry.sample(handle, GLFW.glfwGetWindowMonitor(handle),
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                    Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(),
                    scratchWindowX[0], scratchWindowY[0], scratchScaleX[0], scratchScaleY[0], focused, iconified);
        }
        if (diagnostics != null) diagnostics.mouseScratch().sample(scratchEnabled, focused, x, y,
                ((Lwjgl3Graphics) Gdx.graphics).getLogicalWidth(), ((Lwjgl3Graphics) Gdx.graphics).getLogicalHeight(),
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Gdx.graphics.getBackBufferWidth(),
                Gdx.graphics.getBackBufferHeight(), System.nanoTime(), deliver);
        if (scratchEnabled && focused && drawable) {
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            // Already queued samples are real pre-transition movement; do not clear or reinterpret them.
            if (deliver) scratch.add(x - centerX, y - centerY);
            checkMouseScratchOverflow();
            if (deliver && diagnostics != null) diagnostics.scratchPublished(x - centerX, y - centerY, System.nanoTime());
            if (diagnostics != null) diagnostics.mouseScratch().warp(centerX, centerY);
            super.setCursorPosition(centerX, centerY);
            x = centerX;
            y = centerY;
        }
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
        if (scratch != null) resetMouseScratchDeltas();
        invalidateMouseScratchCenter();
        if (diagnostics != null) diagnostics.mouseScratch().resetCallbacks();
        if (keyboardEvents != null) callbacks = new Lwjgl3InputCallbacks(handle, this, diagnostics);
    }

    @Override public void dispose() {
        try {
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
