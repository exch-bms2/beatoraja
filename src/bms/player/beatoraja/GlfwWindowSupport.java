package bms.player.beatoraja;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;

/** GLFW-owner-thread capabilities, based on the actual backend rather than desktop environment variables. */
public final class GlfwWindowSupport {
    private GlfwWindowSupport() { }

    public static boolean hasWindowPosition() { return hasWindowPosition(GLFW.glfwGetPlatform()); }
    public static boolean hasWindowPosition(int platform) { return platform != GLFW.GLFW_PLATFORM_WAYLAND; }

    /** A windowed Wayland surface's current output is not exposed by this GLFW version. */
    public static int refreshRate(Lwjgl3Graphics graphics) {
        if (hasWindowPosition()) return graphics.getDisplayMode().refreshRate;
        long monitor = GLFW.glfwGetWindowMonitor(graphics.getWindow().getWindowHandle());
        var mode = monitor != 0 ? GLFW.glfwGetVideoMode(monitor) : null;
        return mode != null ? mode.refreshRate() : 0;
    }
}
