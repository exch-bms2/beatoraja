package bms.player.beatoraja;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3DisplayMode;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.Json;

/** Headless regression coverage; no GLFW initialization or user settings are needed. */
public final class StartupMonitorTest {
    public static void main(String[] args) throws Exception {
        var primary = monitor("Internal", 0, 0, 1920, 1080, true);
        var external = monitor("External", -2560, -200, 2560, 1440, false);
        var monitors = new StartupMonitor.Info[]{external, primary};
        check(StartupMonitor.resolve(null, monitors) == 1, "Automatic uses primary, not array order");
        check(StartupMonitor.resolve(external.selection(), monitors) == 0, "Explicit secondary");
        check(StartupMonitor.resolve(external.selection(), new StartupMonitor.Info[]{primary, external}) == 1,
                "Enumeration reorder preserves selection");
        var remembered = external.selection();
        external.x = 1920;
        check(StartupMonitor.find(remembered, monitors) == 0, "Unique name survives monitor movement");
        external.x = -2560;
        var duplicate = monitor("External", 1920, 0, 2560, 1440, false);
        check(StartupMonitor.find(remembered, new StartupMonitor.Info[]{duplicate, external}) == 1, "Duplicate name uses position");
        duplicate.x = external.x; duplicate.y = external.y;
        check(StartupMonitor.find(remembered, new StartupMonitor.Info[]{duplicate, external}) == -1, "Ambiguous identity");
        check(StartupMonitor.resolve(remembered, new StartupMonitor.Info[]{primary}) == 0, "Disconnected primary fallback");
        check(StartupMonitor.resolve(remembered, new StartupMonitor.Info[0]) == -1, "No monitor");

        var json = new Json();
        check(json.fromJson(Config.class, "{}").getStartupMonitor() == null, "Old config defaults to automatic");
        var config = new Config();
        config.setStartupMonitor(remembered);
        config = json.fromJson(Config.class, json.toJson(config));
        config.validate();
        check(StartupMonitor.find(config.getStartupMonitor(), monitors) == 0, "Config round trip");
        StartupMonitor.resolve(config.getStartupMonitor(), new StartupMonitor.Info[]{primary});
        check(config.getStartupMonitor().name.equals("External"), "Fallback must not erase saved preference");
        var invalid = new Config.MonitorSelection(null, 0, 0);
        config.setStartupMonitor(invalid); config.validate();
        check(config.getStartupMonitor() == null, "Invalid config is automatic");
        check(!new Config.MonitorSelection("x".repeat(1025), 0, 0).isValid(), "Bound name length");

        config.setStartupMonitor(remembered); config.setResolution(Resolution.HD);
        config.setAudioConfig(new AudioConfig());
        external.workX = -2560; external.workY = -180; external.workWidth = 2560; external.workHeight = 1400;
        for (Config.DisplayMode mode : new Config.DisplayMode[]{Config.DisplayMode.WINDOW, Config.DisplayMode.BORDERLESS}) {
            config.setDisplaymode(mode);
            var target = target(config);
            StartupMonitor.apply(target, config, external, new Graphics.DisplayMode[0], true);
            check((int) field(target, "windowX") == -1920 && (int) field(target, "windowY") == 160,
                    "Center on secondary logical work area including negative coordinates");
            check((int) field(target, "windowWidth") == 1280 && (int) field(target, "windowHeight") == 720,
                    "No HiDPI scaling of configured logical window size");
            check((boolean) field(target, "windowDecorated") == (mode != Config.DisplayMode.BORDERLESS), "Keep borderless semantics");
            check(field(target, "hdpiMode") == HdpiMode.Logical, "Preserve logical coordinates");
            check(field(target, "fullscreenMode") == null, "Window stays windowed");
        }
        var target = target(config);
        StartupMonitor.apply(target, config, external, new Graphics.DisplayMode[0], false);
        check((int) field(target, "windowX") == -1 && (int) field(target, "windowY") == -1, "Wayland does not position windows");

        config.setDisplaymode(Config.DisplayMode.FULLSCREEN);
        var ctor = Lwjgl3DisplayMode.class.getDeclaredConstructor(long.class, int.class, int.class, int.class, int.class);
        ctor.setAccessible(true);
        var slow = ctor.newInstance(42L, 1280, 720, 60, 24);
        var fast = ctor.newInstance(42L, 1280, 720, 144, 24);
        target = target(config);
        StartupMonitor.apply(target, config, external, new Graphics.DisplayMode[]{slow, fast}, true);
        check(field(target, "fullscreenMode") == fast && fast.getMonitor() == 42L, "Selected monitor and highest matching refresh");
        target = target(config);
        StartupMonitor.apply(target, config, external, new Graphics.DisplayMode[]{fast}, false);
        check(field(target, "fullscreenMode") == fast, "Wayland still supports explicit fullscreen output");
        target = target(config);
        StartupMonitor.apply(target, config, external, new Graphics.DisplayMode[0], true);
        check(field(target, "fullscreenMode") == null && (int) field(target, "windowX") == -1920,
                "Unsupported fullscreen falls back to selected monitor window");
        check(config.getResolution() == Resolution.HD, "Fallback preserves saved resolution");

        var screens = new DesktopLauncher.ScreenInfo(); screens.monitors = monitors;
        var restored = json.fromJson(DesktopLauncher.ScreenInfo.class, json.toJson(screens));
        check(StartupMonitor.find(remembered, restored.monitors) == 0, "Value-only probe DTO round trip");
        System.out.println("StartupMonitorTest passed (selection, persistence, fallback, logical placement and fullscreen policy)");
    }

    private static Lwjgl3ApplicationConfiguration target(Config config) {
        var target = new Lwjgl3ApplicationConfiguration();
        target.setWindowedMode(config.getResolution().width, config.getResolution().height);
        target.setDecorated(config.getDisplaymode() != Config.DisplayMode.BORDERLESS);
        return target;
    }

    private static StartupMonitor.Info monitor(String name, int x, int y, int width, int height, boolean primary) {
        var info = new StartupMonitor.Info();
        info.name = name; info.x = info.workX = x; info.y = info.workY = y;
        info.workWidth = width; info.workHeight = height; info.primary = primary;
        info.desktop = new DesktopLauncher.ModeData(new Graphics.DisplayMode(width, height, 60, 24) { });
        info.available = new DesktopLauncher.ModeData[]{info.desktop};
        return info;
    }

    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try { var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
