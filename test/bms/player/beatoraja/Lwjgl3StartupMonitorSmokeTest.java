package bms.player.beatoraja;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.GL20;
import org.lwjgl.glfw.GLFW;

/** Isolated real-window test; defaults to the last connected monitor. Fullscreen is explicitly opt-in. */
public final class Lwjgl3StartupMonitorSmokeTest {
    public static void main(String[] args) {
        var monitors = Lwjgl3ApplicationConfiguration.getMonitors();
        var info = StartupMonitor.snapshot(monitors, GlfwWindowSupport.hasWindowPosition());
        int index = Integer.getInteger("beatoraja.monitorTest.index", monitors.length - 1);
        check(index >= 0 && index < monitors.length, "Valid test monitor index");
        var selected = (Lwjgl3Graphics.Lwjgl3Monitor) monitors[index];
        var config = new Config();
        config.setStartupMonitor(info[index].selection()); config.setResolution(Resolution.SD);
        config.setAudioConfig(new AudioConfig()); config.getAudioConfig().setDriver(AudioConfig.DriverType.PortAudio);
        boolean fullscreen = Boolean.getBoolean("beatoraja.monitorTest.fullscreen");
        if (Boolean.getBoolean("beatoraja.monitorTest.startFullscreen")) {
            config.setDisplaymode(Config.DisplayMode.FULLSCREEN);
            var current = info[index].desktop;
            for (var resolution : Resolution.values()) {
                if (resolution.width == current.width && resolution.height == current.height) config.setResolution(resolution);
            }
        }
        var target = MainLoader.createApplicationConfiguration(config);
        target.setTitle("beatoraja startup monitor verification"); target.setForegroundFPS(60);
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames;
            WindowModeController modes;
            int width, height;
            @Override public void create() {
                var graphics = (Lwjgl3Graphics) Gdx.graphics;
                verifyMonitor(graphics);
                modes = WindowModeController.create(graphics, config.getResolution(), config.getStartupMonitor());
                if (graphics.isFullscreen()) check(modes.toggle() && !graphics.isFullscreen(), "Restore startup fullscreen on selected monitor");
                width = graphics.getLogicalWidth(); height = graphics.getLogicalHeight();
            }
            @Override public void render() {
                if (modes != null && modes.isSwitching()) return;
                Gdx.gl.glClearColor(0.1f, 0.6f, 0.3f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                var graphics = (Lwjgl3Graphics) Gdx.graphics;
                if (frames == 10) verifyMonitor(graphics);
                if (frames == 20 && fullscreen) check(modes.toggle() && graphics.isFullscreen(), "Enter fullscreen");
                if (frames == 40 && fullscreen) {
                    verifyMonitor(graphics);
                    check(modes.toggle() && !graphics.isFullscreen(), "Exit fullscreen");
                }
                if (++frames == 60) {
                    verifyMonitor(graphics);
                    check(graphics.getLogicalWidth() == width && graphics.getLogicalHeight() == height, "Restore logical size");
                    check(graphics.getWidth() == graphics.getLogicalWidth(), "HiDPI logical input coordinates");
                    check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "No GL errors");
                    Gdx.app.exit();
                }
            }
            private void verifyMonitor(Lwjgl3Graphics graphics) {
                long actual = GLFW.glfwGetWindowMonitor(graphics.getWindow().getWindowHandle());
                if (!graphics.isFullscreen() && GlfwWindowSupport.hasWindowPosition())
                    actual = ((Lwjgl3Graphics.Lwjgl3Monitor) graphics.getMonitor()).getMonitorHandle();
                if (actual != 0) check(actual == selected.getMonitorHandle(), "Window remains on selected monitor");
            }
        }, target) {
            @Override public Lwjgl3Input createInput(Lwjgl3Window window) {
                return new bms.player.beatoraja.input.PollingLwjgl3Input(window);
            }
        };
        System.out.println("Lwjgl3StartupMonitorSmokeTest passed (monitor=" + info[index].name
                + ", connected=" + monitors.length + ", fullscreenToggle=" + fullscreen + ")");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
