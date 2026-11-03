package bms.player.beatoraja.input;

import com.badlogic.gdx.math.Rectangle;

/** Logical input is deliberately independent of framebuffer scale. */
public final class WindowDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        for (int width : new int[] {640, 1280, 1920, 2560}) {
            int height = width * 9 / 16;
            for (double scale : new double[] {1, 1.25, 1.5, 2}) {
                check(WindowDiagnostics.ratio((int) (width * scale), width) == scale, "buffer ratio");
                int x = WindowDiagnostics.skinX(width / 2, 1280, width);
                int y = WindowDiagnostics.skinY(height / 2, 720, height);
                check(x == 640 && y == 360, "logical center independent of DPI");
                check(new Rectangle(630, 350, 20, 20).contains(x, y), "skin click rectangle");
                check(WindowDiagnostics.skinX(width, 1280, width) == 1280, "right edge");
                check(WindowDiagnostics.skinY(height, 720, height) == 0, "bottom edge");
            }
        }
        check(WindowDiagnostics.skinX(20, 1280, 0) == 0, "zero width");
        check(WindowDiagnostics.skinY(20, 720, 0) == 0, "zero height");
        check(WindowDiagnostics.skinX(100000, 100000, 100000) == 100000, "multiplication overflow");
        check(WindowDiagnostics.matchesSkinProjection(2f / 1280, 2f / 720, 1280, 720), "Configured projection");
        check(!WindowDiagnostics.matchesSkinProjection(2f / 640, 2f / 360, 1280, 720),
                "Detect default SpriteBatch projection when initial logical size differs from Config");
        var s = new WindowDiagnostics.Snapshot(640, 360, 1280, 720, 0, 0, 2, 2, 0, 0, false, true, false);
        check(s.matches(640, 360, 1280, 720, 0, 0, 2, 2, 0, 0, false, true, false), "unchanged");
        check(!s.matches(640, 360, 640, 360, 0, 0, 1, 1, 1920, 0, false, true, false), "monitor/DPI change");
        check(!s.matches(640, 360, 1280, 720, 0, 0, 2, 2, 0, 0, true, true, false), "fullscreen change");
        testInputHandlers();
        System.out.println("WindowDiagnosticsTest passed");
    }

    private static void testInputHandlers() throws Exception {
        var old = com.badlogic.gdx.Gdx.graphics;
        int[] dimensions = {640, 360, 1280, 720};
        com.badlogic.gdx.Gdx.graphics = (com.badlogic.gdx.Graphics) java.lang.reflect.Proxy.newProxyInstance(
                WindowDiagnosticsTest.class.getClassLoader(), new Class<?>[] {com.badlogic.gdx.Graphics.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWidth" -> dimensions[0];
                    case "getHeight" -> dimensions[1];
                    case "getBackBufferWidth" -> dimensions[2];
                    case "getBackBufferHeight" -> dimensions[3];
                    default -> throw new AssertionError("Unexpected graphics call " + method);
                });
        try {
            var input = KeyboardQueueTestSupport.allocate(BMSPlayerInputProcessor.class);
            var keyboard = KeyboardQueueTestSupport.allocate(KeyBoardInputProcesseor.class);
            KeyboardQueueTestSupport.set(keyboard, BMSPlayerInputDevice.class, "bmsPlayerInputProcessor", input);
            KeyboardQueueTestSupport.set(keyboard, KeyBoardInputProcesseor.class, "resolution", bms.player.beatoraja.Resolution.HD);
            keyboard.mouseMoved(320, 180);
            check(input.mousex == 640 && input.mousey == 360, "Real move handler");
            dimensions[2] = 640; dimensions[3] = 360;
            keyboard.touchDown(320, 180, 0, 0);
            check(input.mousepressed && input.mousex == 640 && input.mousey == 360, "Real click handler ignores framebuffer DPI");
            dimensions[0] = 1280; dimensions[1] = 720;
            keyboard.touchDragged(640, 360, 0);
            check(input.mousedragged && input.mousex == 640 && input.mousey == 360, "Real drag after resize");
            input.mousepressed = false; input.mousedragged = false;
            dimensions[0] = 0; dimensions[1] = 0;
            keyboard.touchDown(10, 10, 0, 0);
            keyboard.mouseMoved(10, 10);
            keyboard.touchDragged(10, 10, 0);
            check(!input.mousepressed && !input.mousedragged && input.mousex == 640 && input.mousey == 360,
                    "No click or division by zero while minimized");
        } finally { com.badlogic.gdx.Gdx.graphics = old; }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
