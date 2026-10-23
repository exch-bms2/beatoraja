package bms.player.beatoraja.input;

import java.util.concurrent.atomic.AtomicIntegerArray;

import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.Gdx;

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

    public PollingLwjgl3Input(Lwjgl3Window window) {
        super(window);
        this.window = window;
    }

    @Override public void update() {
        super.update();
        int count = 0;
        boolean focused = window.isFocused();
        for (int i = 0; i < keys.length(); i++) {
            int pressed = focused && super.isKeyPressed(i) ? 1 : 0;
            keys.set(i, pressed);
            count += pressed;
        }
        pressedCount = count;
        for (int i = 0; i < buttons.length(); i++) buttons.set(i, focused && super.isButtonPressed(i) ? 1 : 0);
        x = super.getX();
        y = super.getY();
        if (scratchEnabled && focused) {
            int centerX = Gdx.graphics.getWidth() / 2;
            int centerY = Gdx.graphics.getHeight() / 2;
            scratch.add(x - centerX, y - centerY);
            super.setCursorPosition(centerX, centerY);
            x = centerX;
            y = centerY;
        }
    }

    @Override public boolean isKeyPressed(int key) {
        if (Thread.currentThread() == owner) return super.isKeyPressed(key);
        return key == -1 ? pressedCount > 0 : key >= 0 && key < keys.length() && keys.get(key) != 0;
    }

    @Override public boolean isButtonPressed(int button) {
        if (Thread.currentThread() == owner) return super.isButtonPressed(button);
        return button >= 0 && button < buttons.length() && buttons.get(button) != 0;
    }

    @Override public int getX() { return Thread.currentThread() == owner ? super.getX() : x; }
    @Override public int getY() { return Thread.currentThread() == owner ? super.getY() : y; }

    public void setMouseScratchEnabled(boolean enabled) {
        scratchEnabled = enabled;
        scratch.drain();
    }

    public long consumeMouseScratchDelta() { return scratch.drain(); }

    /** Accumulates render-thread motion until consumed once by the input thread. */
    public static final class MouseDeltas {
        private int dx, dy;
        public synchronized void add(int x, int y) { dx += x; dy += y; }
        public synchronized long drain() {
            long result = ((long) dx << 32) | (dy & 0xffffffffL);
            dx = dy = 0;
            return result;
        }
    }
}
