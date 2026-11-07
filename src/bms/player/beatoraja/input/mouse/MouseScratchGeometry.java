package bms.player.beatoraja.input.mouse;

/** Detects coordinate-system boundaries without allocating per sample or changing movement units. */
public final class MouseScratchGeometry {
    private boolean valid, active;
    private long handle, fullscreenMonitor;
    private int width, height, bufferWidth, bufferHeight, windowX, windowY;
    private float scaleX, scaleY;

    public synchronized void invalidate() { valid = false; }

    public synchronized boolean sample(long handle, long fullscreenMonitor, int width, int height,
            int bufferWidth, int bufferHeight, int windowX, int windowY,
            float scaleX, float scaleY, boolean focused, boolean iconified) {
        boolean active = focused && !iconified && width > 0 && height > 0
                && bufferWidth > 0 && bufferHeight > 0;
        boolean same = valid && this.active && active && this.handle == handle
                && this.fullscreenMonitor == fullscreenMonitor && this.width == width && this.height == height
                && this.bufferWidth == bufferWidth && this.bufferHeight == bufferHeight
                && this.windowX == windowX && this.windowY == windowY
                && this.scaleX == scaleX && this.scaleY == scaleY;
        valid = true;
        this.active = active;
        this.handle = handle;
        this.fullscreenMonitor = fullscreenMonitor;
        this.width = width; this.height = height;
        this.bufferWidth = bufferWidth; this.bufferHeight = bufferHeight;
        this.windowX = windowX; this.windowY = windowY;
        this.scaleX = scaleX; this.scaleY = scaleY;
        return same;
    }
}
