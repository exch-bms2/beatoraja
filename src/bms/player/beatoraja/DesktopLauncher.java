package bms.player.beatoraja;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Json;

/** Non-JavaFX entry point; keeps the GLFW event loop out of the Launcher JVM. */
public final class DesktopLauncher {
    static final String GAME_PROCESS = "beatoraja.gameProcess";
    private static final String NVIDIA_THREADED_OPTIMIZATIONS = "__GL_THREADED_OPTIMIZATIONS";

    private DesktopLauncher() { }

    public static void main(String[] args) {
        bms.player.beatoraja.ir.IRProcessSupport.ensureParentClasspath(DesktopLauncher.class.getName(), args);
        if (Arrays.asList(args).contains("--runtime-smoke")) {
            PackagedRuntimeProbe.main(args);
            return;
        }
        DistributionDiagnostics.start(Arrays.asList(args).contains("--display-modes") ? "display-probe"
                : Boolean.getBoolean(GAME_PROCESS) ? "game" : "launcher");
        if (Arrays.asList(args).contains("--display-modes")) {
            try {
                var info = new ScreenInfo();
                var monitors = Lwjgl3ApplicationConfiguration.getMonitors();
                info.windowPositionSupported = GlfwWindowSupport.hasWindowPosition();
                info.monitors = StartupMonitor.snapshot(monitors, info.windowPositionSupported);
                int primary = StartupMonitor.resolve(null, info.monitors);
                if (primary < 0) throw new IllegalStateException("No connected monitors");
                info.desktop = info.monitors[primary].desktop;
                info.available = info.monitors[primary].available;
                System.out.println(new Json().toJson(info));
            } finally {
                org.lwjgl.glfw.GLFW.glfwTerminate();
            }
            return;
        }
        MainLoader.main(args);
    }

    static ScreenInfo queryScreenInfo() throws IOException, InterruptedException {
        Path output = Files.createTempFile("beatoraja-display-modes-", ".json");
        Process process = null;
        try {
            process = new ProcessBuilder(gameCommand(System.getProperty("java.home"), System.getProperty("java.class.path"),
                    System.getProperty("os.name"), ManagementFactory.getRuntimeMXBean().getInputArguments(), "--display-modes"))
                    .redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            DistributionDiagnostics.observe(process, "display-probe");
            if (!process.waitFor(15, TimeUnit.SECONDS) || process.exitValue() != 0)
                throw new IOException("Display-mode probe did not complete successfully");
            return new Json().fromJson(ScreenInfo.class, Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) { process.destroyForcibly(); process.waitFor(); }
            Files.deleteIfExists(output);
        }
    }

    public static final class ScreenInfo {
        public ModeData desktop;
        public ModeData[] available;
        public StartupMonitor.Info[] monitors = new StartupMonitor.Info[0];
        public boolean windowPositionSupported = true;
    }

    public static final class ModeData {
        public int width, height, refreshRate, bitsPerPixel;
        public ModeData() { }
        ModeData(Graphics.DisplayMode mode) {
            width = mode.width;
            height = mode.height;
            refreshRate = mode.refreshRate;
            bitsPerPixel = mode.bitsPerPixel;
        }
        public Graphics.DisplayMode toDisplayMode() {
            return new Graphics.DisplayMode(width, height, refreshRate, bitsPerPixel) { };
        }
    }

    public static Process startGame(String... args) throws IOException {
        return startGame(Config.readSystemConfig(), args);
    }

    public static Process startGame(Config config, String... args) throws IOException {
        String os = System.getProperty("os.name");
        var child = new ProcessBuilder(gameCommand(System.getProperty("java.home"), System.getProperty("java.class.path"),
                os, ManagementFactory.getRuntimeMXBean().getInputArguments(), args))
                .directory(Path.of(System.getProperty("user.dir")).toFile()).inheritIO();
        if (configureGameEnvironment(child.environment(), os, config.isNvidiaWaylandWorkaroundEnabled())) {
            DistributionDiagnostics.event("NVIDIA Wayland workaround applied to game child: " + NVIDIA_THREADED_OPTIMIZATIONS + "=0");
        }
        return DistributionDiagnostics.observe(child.start(), "game");
    }

    static boolean configureGameEnvironment(Map<String, String> environment, String os, boolean workaroundEnabled) {
        if (!workaroundEnabled || !os.equals("Linux") || "x11".equals(environment.get("XDG_SESSION_TYPE"))
                || environment.getOrDefault("WAYLAND_DISPLAY", "").isBlank()) {
            return false;
        }
        // The NVIDIA driver reads this at load time; do not modify the launcher or display probe.
        return environment.putIfAbsent(NVIDIA_THREADED_OPTIMIZATIONS, "0") == null;
    }

    static List<String> gameCommand(String javaHome, String classpath, String os, List<String> vmArgs, String... args) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(javaHome, "bin", os.startsWith("Windows") ? "java.exe" : "java").toString());
        for (String arg : vmArgs) {
            // Do not duplicate debugger ports/agents or carry JavaFX's first-thread setup over.
            if ((arg.startsWith("-D") || arg.startsWith("-Xms") || arg.startsWith("-Xmx")
                    || arg.startsWith("-Xss") || arg.startsWith("-XX:") || arg.startsWith("--add-opens=")
                    || arg.startsWith("--add-exports=") || arg.startsWith("--add-modules="))
                    && !arg.startsWith("-D" + GAME_PROCESS + "=") && !arg.startsWith("-Djava.class.path=")) {
                command.add(arg);
            }
        }
        if (os.startsWith("Mac")) command.add("-XstartOnFirstThread");
        command.add("-D" + GAME_PROCESS + "=true");
        command.add("-cp");
        command.add(classpath);
        command.add(DesktopLauncher.class.getName());
        command.add("--game");
        command.addAll(Arrays.asList(args));
        return List.copyOf(command);
    }
}
