package bms.player.beatoraja.input;

/** Endpoint sampling only: keeps legacy centered deltas and avoids unsupported uncaptured Wayland warps. */
final class MouseScratchPosition {
    private boolean valid, centered;
    private int previousX, previousY;
    int dx, dy;
    boolean delivered, recenter;

    void sample(boolean centered, boolean deliver, int x, int y, int width, int height) {
        boolean sameMode = !valid || this.centered == centered;
        delivered = deliver && sameMode && (centered || valid);
        dx = delivered ? x - (centered ? width / 2 : previousX) : 0;
        dy = delivered ? y - (centered ? height / 2 : previousY) : 0;
        recenter = centered;
        previousX = x; previousY = y;
        valid = true; this.centered = centered;
    }

    void invalidate() { valid = delivered = recenter = false; dx = dy = 0; }
}
