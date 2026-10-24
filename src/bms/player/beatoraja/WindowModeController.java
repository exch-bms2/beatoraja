package bms.player.beatoraja;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;

/** Game-thread fullscreen transitions. Saved sizes and desktop positions use GLFW screen units, not framebuffer pixels. */
final class WindowModeController {
    interface Backend {
        boolean fullscreen();
        Bounds bounds();
        boolean enterFullscreen();
        boolean windowed(int width, int height);
        Area[] workAreas();
        Insets insets();
        void position(int x, int y);
    }

    record Bounds(int x, int y, int width, int height) { }
    record Area(int x, int y, int width, int height) { }
    record Insets(int left, int top, int right, int bottom) { }

    private final Backend backend;
    private final int fallbackWidth, fallbackHeight;
    private Bounds saved;
    private boolean switching;

    WindowModeController(Backend backend, int fallbackWidth, int fallbackHeight) {
        this.backend = backend;
        this.fallbackWidth = Math.max(1, fallbackWidth);
        this.fallbackHeight = Math.max(1, fallbackHeight);
    }

    boolean toggle() {
        if (switching) return false;
        boolean wasFullscreen = backend.fullscreen();
        Bounds requested = saved;
        switching = true;
        try {
            boolean switched;
            if (!wasFullscreen) {
                Bounds current = backend.bounds();
                if (current.width <= 0 || current.height <= 0) return false;
                saved = current;
                switched = backend.enterFullscreen();
            } else {
                requested = saved != null ? saved : new Bounds(0, 0, fallbackWidth, fallbackHeight);
                int width = requested.width, height = requested.height;
                if (saved == null) {
                    // Avoid a giant transient framebuffer before the restored desktop work area can be queried.
                    Bounds fullscreenBounds = backend.bounds();
                    width = Math.min(width, Math.max(1, fullscreenBounds.width));
                    height = Math.min(height, Math.max(1, fullscreenBounds.height));
                }
                switched = backend.windowed(width, height);
                if (switched && !backend.fullscreen()) {
                    // Query after leaving fullscreen: its video mode may have changed the desktop work area.
                    Bounds source = saved != null ? saved : backend.bounds();
                    Bounds placed = place(new Bounds(source.x, source.y, requested.width, requested.height),
                            backend.workAreas(), backend.insets(), saved == null);
                    if (placed.width != width || placed.height != height)
                        switched = backend.windowed(placed.width, placed.height);
                    if (switched && !backend.fullscreen()) backend.position(placed.x, placed.y);
                }
            }
            boolean changed = backend.fullscreen() != wasFullscreen;
            boolean success = switched && changed;
            if (Boolean.getBoolean("beatoraja.windowDiagnostics") || !success) {
                Logger.getGlobal().log(success ? Level.INFO : Level.WARNING,
                        "Window mode switch : requestedFullscreen=" + !wasFullscreen + ", success=" + success
                        + ", actualFullscreen=" + backend.fullscreen() + ", restore=" + requested
                        + ", actualBounds=" + backend.bounds());
            }
            return changed;
        } catch (RuntimeException e) {
            Logger.getGlobal().log(Level.WARNING, "Window mode switch failed; saved window bounds retained", e);
            return backend.fullscreen() != wasFullscreen;
        } finally {
            switching = false;
        }
    }

    boolean isSwitching() { return switching; }

    /** Preserve saved client sizes, including windows spanning monitors; only fallback sizes are fitted. */
    static Bounds place(Bounds requested, Area[] areas, Insets frame, boolean fallback) {
        Area best = null;
        long bestOverlap = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Area area : areas) {
            if (area.width <= 0 || area.height <= 0) continue;
            long overlapX = Math.max(0L, Math.min((long) requested.x + requested.width, (long) area.x + area.width)
                    - Math.max(requested.x, area.x));
            long overlapY = Math.max(0L, Math.min((long) requested.y + requested.height, (long) area.y + area.height)
                    - Math.max(requested.y, area.y));
            long overlap = overlapX * overlapY;
            double dx = (double) requested.x + requested.width / 2.0 - (area.x + area.width / 2.0);
            double dy = (double) requested.y + requested.height / 2.0 - (area.y + area.height / 2.0);
            double distance = dx * dx + dy * dy;
            if (overlap > bestOverlap || overlap == bestOverlap && distance < bestDistance) {
                best = area; bestOverlap = overlap; bestDistance = distance;
            }
        }
        if (best == null) return requested;
        int availableWidth = Math.max(1, best.width - frame.left - frame.right);
        int availableHeight = Math.max(1, best.height - frame.top - frame.bottom);
        int width = fallback ? Math.min(requested.width, availableWidth) : requested.width;
        int height = fallback ? Math.min(requested.height, availableHeight) : requested.height;
        int minX = best.x + frame.left, minY = best.y + frame.top;
        int maxX = minX + Math.max(0, availableWidth - width);
        int maxY = minY + Math.max(0, availableHeight - height);
        int x = fallback ? minX + (maxX - minX) / 2 : Math.max(minX, Math.min(requested.x, maxX));
        int y = fallback ? minY + (maxY - minY) / 2 : Math.max(minY, Math.min(requested.y, maxY));
        return new Bounds(x, y, width, height);
    }

    static WindowModeController create(Lwjgl3Graphics graphics, Resolution fallback) {
        return new WindowModeController(new Backend() {
            public boolean fullscreen() { return graphics.isFullscreen(); }
            public Bounds bounds() {
                var window = graphics.getWindow();
                return new Bounds(window.getPositionX(), window.getPositionY(),
                        graphics.getLogicalWidth(), graphics.getLogicalHeight());
            }
            public boolean enterFullscreen() { return graphics.setFullscreenMode(graphics.getDisplayMode()); }
            public boolean windowed(int width, int height) { return graphics.setWindowedMode(width, height); }
            public void position(int x, int y) { graphics.getWindow().setPosition(x, y); }
            public Area[] workAreas() {
                var monitors = GLFW.glfwGetMonitors();
                if (monitors == null) return new Area[0];
                var areas = new Area[monitors.remaining()];
                int[] x = new int[1], y = new int[1], width = new int[1], height = new int[1];
                for (int i = 0; i < areas.length; i++) {
                    GLFW.glfwGetMonitorWorkarea(monitors.get(monitors.position() + i), x, y, width, height);
                    areas[i] = new Area(x[0], y[0], width[0], height[0]);
                }
                return areas;
            }
            public Insets insets() {
                int[] left = new int[1], top = new int[1], right = new int[1], bottom = new int[1];
                GLFW.glfwGetWindowFrameSize(graphics.getWindow().getWindowHandle(), left, top, right, bottom);
                return new Insets(left[0], top[0], right[0], bottom[0]);
            }
        }, fallback.width, fallback.height);
    }
}
