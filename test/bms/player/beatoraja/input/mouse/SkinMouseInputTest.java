package bms.player.beatoraja.input.mouse;

/** Non-consuming, frame-stable mouse press edges for Lua skins. */
public final class SkinMouseInputTest {

    public static void main(String[] args) {
        var input = new SkinMouseInput();
        input.beginFrame();
        check(!input.isJustPressed(0), "No initial press");
        check(!input.touchDown(0, 0, 0, 0), "Observer must not consume clicks");
        input.touchUp(0, 0, 0, 0);
        check(!input.isJustPressed(0), "Do not change the published frame mid-frame");
        input.beginFrame();
        check(input.isJustPressed(0), "Preserve clicks released before rendering");
        check(input.isJustPressed(0), "Queries must not consume presses");
        input.beginFrame();
        check(!input.isJustPressed(0), "Clear edges next frame");
        input.touchDown(0, 0, 0, 1);
        input.touchDown(0, 0, 0, 4);
        input.touchDown(0, 0, 0, -1);
        input.touchDown(0, 0, 0, 5);
        input.beginFrame();
        check(input.isJustPressed(1) && input.isJustPressed(4), "Independent mouse buttons");
        check(!input.isJustPressed(-1) && !input.isJustPressed(5), "Invalid buttons are safe");
        input.beginFrame();
        check(!input.isJustPressed(1), "Holding a button must not repeat its press edge");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
