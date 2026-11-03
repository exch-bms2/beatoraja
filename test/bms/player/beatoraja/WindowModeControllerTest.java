package bms.player.beatoraja;

import bms.player.beatoraja.WindowModeController.*;

/** Synthetic desktop topology and transition failures; no GLFW context or user configuration. */
public final class WindowModeControllerTest {
    public static void main(String[] args) {
        testRestoreAndResize();
        testFullscreenStartup();
        testFailures();
        testReentrantResize();
        testPlacement();
        testCompositorPlacement();
        System.out.println("WindowModeControllerTest passed (logical restoration, startup fallback, monitor changes and failures)");
    }

    private static void testRestoreAndResize() {
        var b = new Fake();
        var c = new WindowModeController(b, 640, 480);
        Bounds original = b.bounds;
        check(c.toggle() && b.fullscreen, "Enter fullscreen");
        check(c.toggle() && !b.fullscreen && b.bounds.equals(original), "Restore original logical bounds, not video/framebuffer dimensions");
        b.bounds = new Bounds(-1700, 130, 900, 650);
        b.areas = new Area[] {new Area(-1920, 0, 1920, 1050), new Area(0, 0, 1440, 875)};
        check(c.toggle(), "Enter from resized window on negative-origin monitor");
        check(c.toggle() && b.bounds.equals(new Bounds(-1700, 130, 900, 650)), "Refresh saved size/position each entry");
        check(c.toggle(), "Enter again");
        b.areas = new Area[] {new Area(0, 0, 1440, 875)};
        check(c.toggle() && b.bounds.width() == 900 && b.bounds.height() == 650
                && b.bounds.x() >= 0 && b.bounds.y() >= 24, "Disconnected monitor: preserve size and move onscreen");
    }

    private static void testFullscreenStartup() {
        var b = new Fake(); b.fullscreen = true;
        var c = new WindowModeController(b, 640, 480);
        check(c.toggle() && b.bounds.width() == 640 && b.bounds.height() == 480, "Startup fullscreen uses Config logical resolution");
        Bounds restored = b.bounds;
        check(c.toggle() && c.toggle() && b.bounds.equals(restored), "Subsequent transitions use actual window history");
        b = new Fake(); b.fullscreen = true;
        c = new WindowModeController(b, 3840, 2160);
        check(c.toggle() && b.bounds.width() == 1440 && b.bounds.height() == 851,
                "Fit history-free fallback into work area including title bar");
        check(b.lastRequestedWidth == 1440 && b.lastRequestedHeight == 851, "Request fitted size in logical units");
    }

    private static void testFailures() {
        var b = new Fake(); var c = new WindowModeController(b, 640, 480);
        b.failEnter = true;
        check(!c.toggle() && !b.fullscreen, "Failed entry remains windowed");
        b.failEnter = false; b.bounds = new Bounds(100, 80, 800, 600);
        check(c.toggle(), "Retry entry captures latest window");
        b.failExit = true;
        check(!c.toggle() && b.fullscreen, "Failed exit remains fullscreen");
        b.failExit = false;
        check(c.toggle() && b.bounds.equals(new Bounds(100, 80, 800, 600)), "Exit retry retains original size");
        b.throwEnter = true;
        check(!c.toggle() && !b.fullscreen, "Entry exception contained");
        b.throwEnter = false;
        check(c.toggle(), "Entry after exception");
        b.throwPosition = true;
        check(c.toggle() && !b.fullscreen, "Actual mode still reported if position adjustment fails after exit");
        b.throwPosition = false;
        b.bounds = new Bounds(0, 0, 0, 0);
        check(!c.toggle(), "Do not save zero-size window");
    }

    private static void testPlacement() {
        Insets frame = new Insets(0, 24, 0, 0);
        Area[] mixed = {new Area(-1920, 0, 1920, 1050), new Area(0, 0, 1440, 875)};
        Bounds left = new Bounds(-1800, 100, 1000, 700);
        check(WindowModeController.place(left, mixed, frame, false).equals(left), "Negative origins and different logical work areas");
        Bounds spanning = new Bounds(-300, 100, 2000, 800);
        Bounds moved = WindowModeController.place(spanning, mixed, frame, false);
        check(moved.width() == 2000 && moved.height() == 800, "Do not shrink saved windows spanning monitors");
        check(WindowModeController.place(left, new Area[0], frame, false).equals(left), "No topology: retain requested bounds");
        check(WindowModeController.place(left, new Area[] {new Area(0, 0, 0, 0)}, frame, false).equals(left), "Ignore invalid work areas");
        Bounds fallback = WindowModeController.place(new Bounds(0, 0, 640, 480), mixed, frame, true);
        check(fallback.equals(new Bounds(400, 209, 640, 480)), "Center history-free fallback on selected monitor");
        Bounds borderless = WindowModeController.place(new Bounds(0, 0, 1440, 875), mixed, new Insets(0, 0, 0, 0), false);
        check(borderless.equals(new Bounds(0, 0, 1440, 875)), "Borderless window dimensions unchanged");
    }

    private static void testCompositorPlacement() {
        var b = new Fake(); b.positioned = false;
        var c = new WindowModeController(b,640,480);
        b.bounds = new Bounds(0,0,900,650);
        check(c.toggle() && c.toggle() && b.bounds.width()==900 && b.bounds.height()==650,
                "Wayland restores saved logical size without desktop placement/work-area queries");
        b.bounds = new Bounds(0,0,1200,800);
        check(c.toggle() && c.toggle() && b.bounds.width()==1200 && b.bounds.height()==800,
                "Compositor resize replaces saved size on next fullscreen entry");
        b.fullscreen = true; b.bounds = new Bounds(0,0,1280,720);
        c = new WindowModeController(b,3840,2160);
        check(c.toggle() && b.bounds.width()==1280 && b.bounds.height()==720,
                "History-free compositor fallback uses logical fullscreen bounds, not framebuffer DPI");
        check(b.positionCalls==0,"Never request a desktop position on Wayland");
        b.failEnter = true;
        check(!c.toggle() && !b.fullscreen,"Wayland entry failure contained");
        b.failEnter = false; check(c.toggle(),"Wayland entry retry");
        b.failExit = true; check(!c.toggle() && b.fullscreen,"Wayland exit failure retains history");
        b.failExit = false; check(c.toggle() && b.bounds.width()==1280,"Wayland exit retry preserves size");
    }

    private static void testReentrantResize() {
        var b = new Fake(); var c = new WindowModeController(b, 640, 480);
        int[] callbacks = {0};
        b.onChange = () -> {
            callbacks[0]++;
            check(c.isSwitching() && !c.toggle(), "Ignore recursive toggles from resize callbacks");
        };
        check(c.toggle() && c.toggle() && callbacks[0] == 2 && !c.isSwitching(),
                "Complete outer transitions and release reentrancy guard");
    }

    private static final class Fake implements Backend {
        boolean fullscreen, failEnter, failExit, throwEnter, throwPosition;
        boolean positioned = true;
        int positionCalls;
        Bounds bounds = new Bounds(100, 80, 1000, 700);
        Area[] areas = {new Area(0, 0, 1440, 875)};
        int lastRequestedWidth, lastRequestedHeight;
        Runnable onChange = () -> { };
        public boolean fullscreen() { return fullscreen; }
        public boolean hasWindowPosition() { return positioned; }
        public Bounds bounds() { return bounds; }
        public boolean enterFullscreen() {
            if (throwEnter) throw new IllegalStateException("Expected entry failure");
            if (failEnter) return false;
            fullscreen = true;
            bounds = new Bounds(0, 0, 2880, 1800);
            onChange.run();
            return true;
        }
        public boolean windowed(int width, int height) {
            if (failExit) return false;
            fullscreen = false;
            lastRequestedWidth = width; lastRequestedHeight = height;
            bounds = new Bounds(0, 0, width, height);
            onChange.run();
            return true;
        }
        public Area[] workAreas() {
            check(positioned,"Wayland must not query global work areas for placement");
            check(!fullscreen, "Read work area after desktop video mode restored");
            return areas;
        }
        public Insets insets() { check(positioned,"Wayland frame insets are not used for placement"); return new Insets(0, 24, 0, 0); }
        public void position(int x, int y) {
            positionCalls++;
            check(positioned,"Wayland must not request a desktop position");
            if (throwPosition) throw new IllegalStateException("Expected positioning failure");
            bounds = new Bounds(x, y, bounds.width(), bounds.height());
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
