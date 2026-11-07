package bms.player.beatoraja.input.mouse;

/** Endpoint sampling only: keeps legacy centered deltas and avoids unsupported uncaptured Wayland warps. */
public final class MouseScratchPosition {
    private boolean valid, centered;
    private int previousX, previousY;
    int dx, dy;
    boolean delivered, recenter;

    public void sample(boolean centered, boolean deliver, int x, int y, int width, int height) {
        boolean sameMode = !valid || this.centered == centered;
        delivered = deliver && sameMode && (centered || valid);
        dx = delivered ? x - (centered ? width / 2 : previousX) : 0;
        dy = delivered ? y - (centered ? height / 2 : previousY) : 0;
        recenter = centered;
        previousX = x; previousY = y;
        valid = true; this.centered = centered;
    }

    public void invalidate() { valid = delivered = recenter = false; dx = dy = 0; }

    public int dx() { return dx; }
    public int dy() { return dy; }
    public boolean delivered() { return delivered; }
    public boolean recenter() { return recenter; }
}
