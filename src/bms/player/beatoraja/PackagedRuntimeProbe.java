package bms.player.beatoraja;

/** Native loading only: no player configuration, window, audio device or IR initialization. */
public final class PackagedRuntimeProbe {
    private PackagedRuntimeProbe() { }

    public static void main(String[] args) {
        DistributionDiagnostics.start("native-probe");
        int failures = 0;
        failures += check("libGDX", () -> com.badlogic.gdx.utils.GdxNativesLoader.load());
        failures += check("FreeType", () -> {
            var font = new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(
                    new com.badlogic.gdx.files.FileHandle("font/VL-Gothic-Regular.ttf"));
            font.dispose();
        });
        failures += check("GLFW (no display initialization)", () -> {
            if (org.lwjgl.glfw.GLFW.glfwGetVersionString() == null) throw new IllegalStateException("No GLFW version");
        });
        if (java.util.Arrays.asList(args).contains("--x11-ime"))
            failures += check("X11 IME prototype (load/ABI only)",
                    bms.player.beatoraja.input.text.ime.X11ImeBridge::verifyNativeLibrary);
        failures += check("OpenAL (no device opened)", () -> {
            if (org.lwjgl.openal.ALC.getFunctionProvider() == null) throw new IllegalStateException("No OpenAL provider");
        });
        failures += check("FFmpeg", () -> org.bytedeco.javacv.FFmpegFrameGrabber.tryLoad());
        failures += check("SDL_RAW SDL3 (no joystick initialization)", () -> {
            if (org.lwjgl.sdl.SDLVersion.SDL_GetVersion() < 3_000_000) throw new IllegalStateException("No SDL3 version");
        });
        if (System.getProperty("os.name").startsWith("Mac")) {
            var ime = java.nio.file.Path.of("natives/libbeatoraja-ime.dylib");
            if (java.nio.file.Files.isRegularFile(ime))
                failures += check("AppKit IME prototype (load only)", () -> System.load(ime.toAbsolutePath().toString()));
            else System.out.println("SKIP AppKit IME prototype: optional native not included in this build");
        }
        failures += check("SQLite", () -> {
            Class.forName("org.sqlite.JDBC");
            try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")) {
                if (!connection.isValid(1)) throw new IllegalStateException("Invalid SQLite connection");
            }
        });
        System.out.println("Native probe failures=" + failures
                + "; NOT TESTED: window/GL, JavaFX, audio playback, PortAudio, physical controllers, IME");
        if (failures != 0) System.exit(1);
    }

    @FunctionalInterface interface Probe { void run() throws Exception; }

    static int check(String name, Probe probe) {
        try {
            probe.run();
            System.out.println("PASS " + name);
            return 0;
        } catch (Exception | LinkageError failure) {
            System.err.println("FAIL " + name + ": " + failure);
            failure.printStackTrace();
            return 1;
        }
    }
}
