package bms.player.beatoraja.input.mouse;

/** Maps logical client coordinates to skin coordinates with legacy integer rounding. */
public final class MouseCoordinates {
    private MouseCoordinates() { }

    public static int skinX(int x, int skinWidth, int windowWidth) {
        return windowWidth > 0 ? (int) ((long) x * skinWidth / windowWidth) : 0;
    }

    public static int skinY(int y, int skinHeight, int windowHeight) {
        return windowHeight > 0 ? skinHeight - (int) ((long) y * skinHeight / windowHeight) : 0;
    }
}
