package bms.player.beatoraja.input;

import java.util.logging.Logger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;

/** Opt-in, change-only observations from the GLFW owner thread. No cursor positions are logged. */
public final class WindowDiagnostics {
    private final int[] x = new int[1], y = new int[1];
    private final float[] scaleX = new float[1], scaleY = new float[1];
    private Snapshot previous;
    private long previousHandle;

    public static WindowDiagnostics createIfEnabled() {
        return Boolean.getBoolean("beatoraja.windowDiagnostics") ? new WindowDiagnostics() : null;
    }

    public void sample() {
        var graphics = (Lwjgl3Graphics) Gdx.graphics;
        var window = graphics.getWindow();
        long handle = window.getWindowHandle();
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetWindowContentScale(handle, scaleX, scaleY);
        var monitor = graphics.getMonitor();
        int width = graphics.getWidth(), height = graphics.getHeight();
        int bufferWidth = graphics.getBackBufferWidth(), bufferHeight = graphics.getBackBufferHeight();
        boolean fullscreen = graphics.isFullscreen(), focused = window.isFocused();
        boolean iconified = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) != 0;
        if (previous != null && previousHandle == handle && previous.matches(width, height, bufferWidth, bufferHeight,
                x[0], y[0], scaleX[0], scaleY[0], monitor.virtualX, monitor.virtualY,
                fullscreen, focused, iconified)) return;
        previous = new Snapshot(width, height, bufferWidth, bufferHeight, x[0], y[0],
                scaleX[0], scaleY[0], monitor.virtualX, monitor.virtualY, fullscreen, focused, iconified);
        previousHandle = handle;
        Logger.getGlobal().info("Window diagnostics [logicalInput=true, cursorPositionsLogged=false] " + previous
                + ", monitor=" + monitor.name + ", refreshRate=" + graphics.getDisplayMode().refreshRate
                + ", bufferPerLogical=" + ratio(bufferWidth, width) + "x" + ratio(bufferHeight, height));
    }

    public static double ratio(int pixels, int logical) { return logical > 0 ? (double) pixels / logical : 0; }

    public static boolean matchesSkinProjection(float xScale, float yScale, int width, int height) {
        return Math.abs(xScale * width - 2) < 0.0001f && Math.abs(yScale * height - 2) < 0.0001f;
    }

    public static void describeSkinProjection(com.badlogic.gdx.graphics.g2d.SpriteBatch batch, int width, int height) {
        if (!Boolean.getBoolean("beatoraja.windowDiagnostics")) return;
        float[] matrix = batch.getProjectionMatrix().val;
        float xScale = matrix[com.badlogic.gdx.math.Matrix4.M00], yScale = matrix[com.badlogic.gdx.math.Matrix4.M11];
        boolean matches = matchesSkinProjection(xScale, yScale, width, height);
        Logger.getGlobal().log(matches ? java.util.logging.Level.INFO : java.util.logging.Level.WARNING,
                "Window skin projection : configured=" + width + "x" + height
                + ", initialProjectionScale=" + xScale + "x" + yScale + ", matchesConfigured=" + matches);
    }

    /** Same integer rounding as the legacy click mapping; minimized windows are not interactive. */
    public static int skinX(int x, int skinWidth, int windowWidth) {
        return windowWidth > 0 ? (int) ((long) x * skinWidth / windowWidth) : 0;
    }

    public static int skinY(int y, int skinHeight, int windowHeight) {
        return windowHeight > 0 ? skinHeight - (int) ((long) y * skinHeight / windowHeight) : 0;
    }

    public record Snapshot(int width, int height, int bufferWidth, int bufferHeight, int x, int y,
            float scaleX, float scaleY, int monitorX, int monitorY,
            boolean fullscreen, boolean focused, boolean iconified) {
        boolean matches(int w, int h, int bw, int bh, int px, int py, float sx, float sy,
                int mx, int my, boolean fs, boolean focus, boolean icon) {
            return width == w && height == h && bufferWidth == bw && bufferHeight == bh
                    && x == px && y == py && scaleX == sx && scaleY == sy && monitorX == mx
                    && monitorY == my && fullscreen == fs && focused == focus && iconified == icon;
        }
    }
}
