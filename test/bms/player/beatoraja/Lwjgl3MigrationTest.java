package bms.player.beatoraja;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.Json;
import bms.player.beatoraja.input.PollingLwjgl3Input.MouseDeltas;

/** Headless coverage for process separation, configuration translation and mouse deltas. */
public final class Lwjgl3MigrationTest {
    public static void main(String[] args) throws Exception {
        var mac = DesktopLauncher.gameCommand("/jdk with spaces", "lib/*:app.jar", "Mac OS X",
                List.of("-Xmx4g", "-Xms1g", "-Djava.library.path=./natives", "-XstartOnFirstThread",
                        "-agentlib:jdwp=transport=dt_socket,address=5005", "-Dbeatoraja.gameProcess=false"),
                "-p", "/songs with spaces/chart.bms", "--song-updated");
        check(mac.get(0).equals("/jdk with spaces/bin/java"), "Same Java runtime");
        check(mac.stream().filter("-XstartOnFirstThread"::equals).count() == 1, "Mac first thread only once");
        check(mac.contains("-Xmx4g") && mac.contains("-Djava.library.path=./natives"), "Heap/native options retained");
        check(mac.stream().noneMatch(a -> a.contains("jdwp") || a.equals("-Dbeatoraja.gameProcess=false")), "No debug port collision");
        check(mac.contains("-Dbeatoraja.gameProcess=true") && mac.contains("--game")
                && mac.contains("/songs with spaces/chart.bms") && mac.contains("--song-updated"), "Arguments and recursion guard");
        for (String os : List.of("Linux", "Windows 11")) {
            var command = DesktopLauncher.gameCommand("/jdk", "classpath", os, List.of(), "-s");
            check(!command.contains("-XstartOnFirstThread"), "No Mac flag on " + os);
        }
        check(!javafx.application.Application.class.isAssignableFrom(DesktopLauncher.class), "Non-JavaFX entry point");
        verifyNvidiaWaylandWorkaround();
        verifyNvidiaWaylandConfig();

        Graphics.DisplayMode[] modes = {mode(1280,720,60), mode(1920,1080,60), mode(1920,1080,144)};
        check(MainLoader.selectDisplayMode(modes, 1920, 1080).refreshRate == 144, "Highest matching refresh rate");
        check(MainLoader.selectDisplayMode(modes, 640, 480) == null, "Missing fullscreen mode fallback");
        check(MainLoader.getAvailableDisplayMode().length > 0 && MainLoader.getDesktopDisplayMode().width > 0,
                "Launcher screen query without GLFW");
        var config = new Config();
        config.setAudioConfig(new AudioConfig());
        config.setDisplaymode(Config.DisplayMode.WINDOW);
        config.setVsync(false);
        config.setMaxFramePerSecond(240);
        config.getAudioConfig().setDriver(AudioConfig.DriverType.PortAudio);
        var translated = MainLoader.createApplicationConfiguration(config);
        check((boolean) field(translated, "disableAudio"), "PortAudio disables backend audio");
        check((int) field(translated, "foregroundFPS") == 240, "Frame limiter retained");
        check(field(translated, "hdpiMode") == HdpiMode.Logical, "Logical skin/input coordinates");
        config.getAudioConfig().setDriver(AudioConfig.DriverType.AudioDevice);
        config.getAudioConfig().setDeviceBufferSize(512);
        translated = MainLoader.createApplicationConfiguration(config);
        check(!(boolean) field(translated, "disableAudio") && (int) field(translated, "audioDeviceBufferSize") == 4096,
                "AudioDevice buffer compatibility");
        for (boolean vsync : new boolean[] {false, true}) {
            for (int fps : new int[] {0, 30, 60, 144, 240}) {
                config.setVsync(vsync); config.setMaxFramePerSecond(fps);
                translated = MainLoader.createApplicationConfiguration(config);
                check((boolean) field(translated, "vSyncEnabled") == vsync
                        && (int) field(translated, "foregroundFPS") == fps
                        && (int) field(translated, "idleFPS") == Math.max(1, fps)
                        && !(boolean) field(translated, "pauseWhenMinimized"), "VSync/FPS/minimization settings retained");
            }
        }
        var deltas = new MouseDeltas();
        deltas.add(12, -8);
        deltas.add(-5, 3);
        long packed = deltas.drain();
        check((int)(packed >> 32) == 7 && (int)packed == -5, "Mouse motion accumulated across frames");
        check(deltas.drain() == 0, "High-frequency polling must not repeat frame motion");
        System.out.println("Lwjgl3MigrationTest passed");
    }

    private static void verifyNvidiaWaylandConfig() {
        String property = "beatoraja.nvidiaWaylandWorkaround";
        String previous = System.getProperty(property);
        try {
            System.clearProperty(property);
            var json = new Json();
            var config = json.fromJson(Config.class, "{}");
            check(!new Config().isNvidiaWaylandWorkaroundEnabled() && !config.isNvidiaWaylandWorkaroundEnabled(),
                    "New and legacy configs default to off");
            System.setProperty(property, "true");
            check(config.isNvidiaWaylandWorkaroundEnabled() && !config.isNvidiaWaylandWorkaround(),
                    "Legacy VM option enables the workaround without changing the saved value");
            config.setNvidiaWaylandWorkaround(true);
            config = json.fromJson(Config.class, json.toJson(config));
            System.clearProperty(property);
            check(config.isNvidiaWaylandWorkaroundEnabled(), "Saved opt-in survives a reload");
            System.setProperty(property, "false");
            check(!config.isNvidiaWaylandWorkaroundEnabled() && config.isNvidiaWaylandWorkaround(),
                    "Explicit VM opt-out overrides the saved setting without clearing it");
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }

    private static void verifyNvidiaWaylandWorkaround() {
        String variable = "__GL_THREADED_OPTIMIZATIONS";
        var inherited = Map.of("WAYLAND_DISPLAY", "wayland-0", "XDG_SESSION_TYPE", "wayland", "DISPLAY", ":0");
        var child = new HashMap<>(inherited);
        check(!DesktopLauncher.configureGameEnvironment(child, "Linux", false) && child.equals(inherited),
                "Disabled workaround must not change the child environment");
        for (String os : List.of("Windows 11", "Mac OS X")) {
            check(!DesktopLauncher.configureGameEnvironment(child, os, true) && child.equals(inherited),
                    "Workaround must not change the environment on " + os);
        }
        child.put("XDG_SESSION_TYPE", "x11");
        var x11 = Map.copyOf(child);
        check(!DesktopLauncher.configureGameEnvironment(child, "Linux", true) && child.equals(x11),
                "Do not change explicitly selected X11, even when a Wayland socket is available");
        child.remove("WAYLAND_DISPLAY");
        child.put("XDG_SESSION_TYPE", "wayland");
        var noWaylandDisplay = Map.copyOf(child);
        check(!DesktopLauncher.configureGameEnvironment(child, "Linux", true) && child.equals(noWaylandDisplay),
                "A session label without a Wayland display is insufficient");
        child.putAll(inherited);
        child.put(variable, "1");
        check(!DesktopLauncher.configureGameEnvironment(child, "Linux", true) && "1".equals(child.get(variable)),
                "An explicit inherited value must be preserved");
        child.remove(variable);
        check(DesktopLauncher.configureGameEnvironment(child, "Linux", true) && "0".equals(child.remove(variable))
                        && child.equals(inherited), "Only the NVIDIA variable is added for the opted-in game child");
    }

    private static Object field(Object instance, String name) throws Exception {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Graphics.DisplayMode mode(int width, int height, int refresh) {
        return new Graphics.DisplayMode(width, height, refresh, 32) { };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
