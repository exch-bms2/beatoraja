package bms.player.beatoraja;

import java.util.Arrays;
import java.util.logging.Logger;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3Monitor;
import org.lwjgl.glfw.GLFW;

/** Resolves persisted monitor hints without retaining native handles across processes or launches. */
public final class StartupMonitor {
    private StartupMonitor() { }

    /** Launcher-safe snapshot. Positions and work areas are GLFW screen units, not framebuffer pixels. */
    public static final class Info {
        public String name;
        public int x, y, workX, workY, workWidth, workHeight;
        public boolean primary;
        public DesktopLauncher.ModeData desktop;
        public DesktopLauncher.ModeData[] available;

        public Config.MonitorSelection selection() { return new Config.MonitorSelection(name, x, y); }
    }

    static Info[] snapshot(Graphics.Monitor[] monitors, boolean positioned) {
        var result = new Info[monitors.length];
        long primary = GLFW.glfwGetPrimaryMonitor();
        for (int i = 0; i < monitors.length; i++) {
            var monitor = monitors[i];
            long handle = ((Lwjgl3Monitor) monitor).getMonitorHandle();
            var info = result[i] = new Info();
            info.name = monitor.name; info.x = monitor.virtualX; info.y = monitor.virtualY;
            info.primary = handle == primary;
            info.desktop = new DesktopLauncher.ModeData(Lwjgl3ApplicationConfiguration.getDisplayMode(monitor));
            info.available = Arrays.stream(Lwjgl3ApplicationConfiguration.getDisplayModes(monitor))
                    .map(DesktopLauncher.ModeData::new).toArray(DesktopLauncher.ModeData[]::new);
            if (positioned) {
                int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
                GLFW.glfwGetMonitorWorkarea(handle, x, y, w, h);
                info.workX = x[0]; info.workY = y[0]; info.workWidth = w[0]; info.workHeight = h[0];
            }
        }
        return result;
    }

    /** Returns -1 for an absent or ambiguous preference. A unique name survives desktop rearrangement. */
    public static int find(Config.MonitorSelection requested, Info[] monitors) {
        if (requested == null || !requested.isValid()) return -1;
        int match = -1, names = 0, exact = -1, exactCount = 0;
        for (int i = 0; i < monitors.length; i++) {
            var info = monitors[i];
            if (!requested.name.equals(info.name)) continue;
            match = i; names++;
            if (requested.x == info.x && requested.y == info.y) { exact = i; exactCount++; }
        }
        return names == 1 ? match : exactCount == 1 ? exact : -1;
    }

    public static int resolve(Config.MonitorSelection requested, Info[] monitors) {
        int found = find(requested, monitors);
        if (found >= 0) return found;
        for (int i = 0; i < monitors.length; i++) if (monitors[i].primary) return i;
        return monitors.length == 0 ? -1 : 0;
    }

    static void configure(Lwjgl3ApplicationConfiguration target, Config config) {
        // Preserve automatic window startup and headless configuration tests without initializing GLFW.
        if (config.getStartupMonitor() == null && config.getDisplaymode() != Config.DisplayMode.FULLSCREEN) return;
        var monitors = Lwjgl3ApplicationConfiguration.getMonitors();
        boolean positioned = GlfwWindowSupport.hasWindowPosition();
        var info = snapshot(monitors, positioned);
        int index = resolve(config.getStartupMonitor(), info);
        if (index < 0) {
            Logger.getGlobal().warning("No startup monitor available; using automatic window placement");
            return;
        }
        if (config.getStartupMonitor() != null && find(config.getStartupMonitor(), info) < 0)
            Logger.getGlobal().warning("Startup monitor unavailable or ambiguous: " + config.getStartupMonitor().name
                    + "; falling back to primary monitor: " + info[index].name);
        var modes = config.getDisplaymode() == Config.DisplayMode.FULLSCREEN
                ? Lwjgl3ApplicationConfiguration.getDisplayModes(monitors[index]) : new Graphics.DisplayMode[0];
        apply(target, config, info[index], modes, positioned);
        Logger.getGlobal().info("Startup monitor selected: name=" + info[index].name + ", primary=" + info[index].primary
                + ", position=" + info[index].x + "," + info[index].y + ", windowPositionSupported=" + positioned);
    }

    static void apply(Lwjgl3ApplicationConfiguration target, Config config, Info monitor,
            Graphics.DisplayMode[] modes, boolean positioned) {
        int width = config.getResolution().width, height = config.getResolution().height;
        if (positioned) {
            var position = centered(monitor, width, height);
            target.setWindowPosition(position.x(), position.y());
        } else if (config.getStartupMonitor() != null && config.getDisplaymode() != Config.DisplayMode.FULLSCREEN) {
            Logger.getGlobal().warning("Startup monitor placement is unavailable on Wayland; compositor chooses the window output");
        }
        if (config.getDisplaymode() == Config.DisplayMode.FULLSCREEN) {
            var mode = MainLoader.selectDisplayMode(modes, width, height);
            if (mode != null) target.setFullscreenMode(mode);
            else Logger.getGlobal().warning("Requested fullscreen resolution unavailable on " + monitor.name
                    + "; using windowed mode: " + width + "x" + height);
        }
    }

    static WindowModeController.Bounds centered(Info monitor, int width, int height) {
        int x = monitor.workWidth > 0 ? monitor.workX : monitor.x;
        int y = monitor.workHeight > 0 ? monitor.workY : monitor.y;
        int w = monitor.workWidth > 0 ? monitor.workWidth : monitor.desktop.width;
        int h = monitor.workHeight > 0 ? monitor.workHeight : monitor.desktop.height;
        return new WindowModeController.Bounds(x + Math.max(0, (w - width) / 2),
                y + Math.max(0, (h - height) / 2), width, height);
    }
}
