package bms.player.beatoraja.build;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import bms.player.beatoraja.skin.lua.SkinLuaAccessor;
import bms.player.beatoraja.skin.LibGdxCompatibilityTest;
import bms.player.beatoraja.song.archive.RarSongArchiveTest;
import bms.player.beatoraja.input.JInputControllerCompatibilityTest;
import bms.player.beatoraja.external.DesktopActionsTest;
import bms.player.beatoraja.input.InputDiagnosticsTest;
import bms.player.beatoraja.input.KeyboardEventQueueTest;
import bms.player.beatoraja.play.KeyboardJudgeCompatibilityTest;

/** Verify the thin-JAR distribution without opening windows or using player data. */
public final class DistributionSmokeTest {
    private static final Map<String, String> CONTROLLER_JARS = Map.of(
            "gdx-controllers-core-2.2.4.jar", "04da67627b1a3646ef8f214c91d296742a7c05597dbc2167aaab09cbdcc77470",
            "gdx-controllers-desktop-2.2.4.jar", "8e390e59c6211ed23e0a6fd4018cefa37f8b9865dee69995699053496b801d6c",
            "jamepad-2.26.5.0.jar", "b0ef910ba531cadfe0418496a30d87cbac46021b062547f5b5947e035435711a");
    private static final Map<String, String> JINPUT_JARS = Map.of(
            "jinput-2.0.5.jar", "79b5a4f5829e1a49a415711f7fb8eb5b9ad22defa72929fda2da96ec30d3f018",
            "jutils-1.0.0.jar", "c3334ff39cf0ab3b54925619101054c90098b7c733b1e7834c7b75e4c41e84a5",
            "jinput-platform-2.0.5-natives-osx.jar", "171024b00f9a743b92388b48733dc30fd3f6d3a7da4f66f9821531fbcf1aa074",
            "jinput-platform-2.0.5-natives-linux.jar", "1ab4d194277816b6105253d224c8c69f1b6d720690efe7c85f3ce884f0ea273d",
            "jinput-platform-2.0.5-natives-windows.jar", "24afbd5e1fab17da57d16a4d3f19d53f36155ef46a9976484201a4bb9722287f");
    private static final List<String> GRAPHICS_JARS = graphicsJars();

    private static List<String> graphicsJars() {
        var names = new java.util.ArrayList<>(List.of(
            "gdx-1.14.2.jar", "gdx-freetype-1.14.2.jar", "gdx-backend-lwjgl3-1.14.2.jar",
            "gdx-platform-1.14.2-natives-desktop.jar", "gdx-freetype-platform-1.14.2-natives-desktop.jar",
            "gdx-jnigen-loader-2.5.2.jar", "gdx-controllers-core-2.2.4.jar", "gdx-controllers-desktop-2.2.4.jar",
            "jamepad-2.26.5.0.jar"));
        for (String module : List.of("lwjgl", "lwjgl-glfw", "lwjgl-jemalloc", "lwjgl-openal", "lwjgl-opengl", "lwjgl-stb")) {
            names.add(module + "-3.3.3.jar");
            for (String platform : List.of("windows", "linux", "macos", "macos-arm64"))
                names.add(module + "-3.3.3-natives-" + platform + ".jar");
        }
        return List.copyOf(names);
    }
    // SHA-256 of the existing local binaries, independently matched to Central in the inventory.
    private static final Map<String, String> CENTRAL_JARS = Map.ofEntries(
            Map.entry("junrar-7.6.0.jar", "2fe2f89104f7fa7f4d525159718b9a52927e208c45ffdaaf55355264e207ed8b"),
            Map.entry("commons-compress-1.16.1.jar", "a20d8e45315abbe07faf952e095817b9925f38c4fc39e8a211c7d072702e97eb"),
            Map.entry("commons-dbutils-1.6.jar", "bd42e879e33afeacf6c59bdd5382a987eb017202857c509bbd1c88e15bc9a974"),
            Map.entry("sqlite-jdbc-3.46.1.0.jar", "6dc7464e3803648d3ff18a7359bab6adf079fcd8495b18991f6f5edcb8ac6e3b"),
            Map.entry("slf4j-api-1.7.36.jar", "d3ef575e3e4979678dc01bf1dcce51021493b4d11fb7f1be8ad982877c16a1c0"),
            Map.entry("javacpp-1.3.3.jar", "e800b74c7239bc651b747cf4936ae84d055d2fbb1b45bd2f51e32cfeaf2ec186"),
            Map.entry("javacv-1.3.3.jar", "aa14054a40320bd0dfc449ddd2ad9736cae24fc22aa6c374edfd06b3be5f4523"),
            Map.entry("javacv-platform-1.3.3.jar", "04c9d7fc3ec01c50c0f1bfd265479649fb12301367e120b4cd2394fa042f82f2"),
            Map.entry("ffmpeg-3.2.1-1.3.jar", "7cd071753ca3c7ed8b9f4d3e20e631986c6d3e8fc0daba7b1f3eb9fab073f5cc"),
            Map.entry("ffmpeg-platform-3.2.1-1.3.jar", "fb8ff4f7897c5b747bdf52917695d7c7765fdf758140684151623f9ebe0546a9"),
            Map.entry("ffmpeg-3.2.1-1.3-linux-x86.jar", "030fd7a87fbdbfdd1875f81f203f6877093eeea1e2e21297da5e18f3e7bf4e8d"),
            Map.entry("ffmpeg-3.2.1-1.3-linux-x86_64.jar", "ac6f3fc9eb5b4cefe3ca84c81cf82403bc9ab4df26ceda569ebbe7ecdc1769d6"),
            Map.entry("ffmpeg-3.2.1-1.3-macosx-x86_64.jar", "47e433b905e595c68d2f89cdc23bbcea3600b86ca2a56192e09ee2c0b1cc54b7"),
            Map.entry("ffmpeg-3.2.1-1.3-windows-x86.jar", "a5c1f415cee64e1cd1de17adfe6ce425479fde159fa92776b47de37dcac13878"),
            Map.entry("ffmpeg-3.2.1-1.3-windows-x86_64.jar", "d6b1284299c442c6b720660d21e8abf32640ef0283cc0b27c005153b302577ec"));

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--probe")) {
            probeRuntime();
            return;
        }
        Path install = Path.of(args[0]);
        bms.player.beatoraja.ir.IRWorkerSecurityTest.verifyDistribution(Path.of(args[3]));
        verifyCentralJars(install, Path.of(args[3]));
        verifyGraphicsJars(install, Path.of(args[3]));
        verifyJInputJars(install, Path.of(args[3]));
        Path appJar;
        try (var jars = Files.list(install.resolve("lib"))) {
            appJar = jars.filter(p -> p.getFileName().toString().startsWith("beatoraja-")).findFirst().orElseThrow();
        }
        try (JarFile jar = new JarFile(appJar.toFile())) {
            check(jar.getManifest().getMainAttributes().getValue("Main-Class")
                    .equals("bms.player.beatoraja.DesktopLauncher"), "Non-JavaFX desktop entry point");
            check(jar.getEntry("bms/player/beatoraja/MainLoader.class") != null, "Missing application class");
            check(jar.getEntry("bms/player/beatoraja/StartupMonitor.class") != null, "Missing startup monitor resolver");
            for (String resource : List.of("resources/UIResources.properties", "resources/UIResources_ja_JP.properties")) {
                String text;
                try (var input = jar.getInputStream(jar.getJarEntry(resource))) {
                    text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                check(text.contains("STARTUP_MONITOR=") && text.contains("MONITOR_AUTO="), "Missing monitor translations: " + resource);
            }
            check(jar.getEntry("org/lwjgl/glfw/GLFW.class") == null
                    && jar.getEntry("bms/build/FatJarContent.class") == null, "Ordinary distribution must keep the thin application JAR");
            check(jar.getEntry("bms/player/beatoraja/build/PackagedGameAgent.class") == null, "Never ship the test observer agent");
            for (String type : List.of("WindowsImeBridge", "WindowsImeSession", "WindowsImeNative", "WindowsImeCallback"))
                check(jar.getEntry("bms/player/beatoraja/input/" + type + ".class") != null, "Missing Windows IME component: " + type);
            check(jar.getEntry("bms/player/beatoraja/input/WindowsImeTestSupport.class") == null, "Never ship fake IMM transport");
            check(jar.getEntry("bms/player/beatoraja/audio/AudioDiagnostics.class") != null, "Missing audio diagnostics");
            check(jar.getEntry("bms/player/beatoraja/FramePacingDiagnostics.class") != null, "Missing frame pacing diagnostics");
            check(jar.getEntry("bms/player/beatoraja/input/JInputControllerManager.class") != null, "Missing JInput manager");
            check(jar.getEntry("bms/player/beatoraja/input/InputDiagnostics.class") != null, "Missing input diagnostics");
            check(jar.getEntry("bms/player/beatoraja/input/InputPollingThread.class") != null, "Missing input polling lifecycle");
            check(jar.getEntry("bms/player/beatoraja/skin/SkinRenderTarget.class") != null, "Missing preview render target scope");
            check(jar.getEntry("bms/player/beatoraja/skin/SkinTextClipboard.class") != null, "Missing guarded text clipboard");
            check(jar.getEntry("bms/player/beatoraja/input/X11ImeBridge.class") != null, "Missing opt-in X11 IME bridge");
            check(jar.getEntry("bms/player/beatoraja/input/X11ImeSession.class") != null, "Missing X11 IME session");
            check(jar.getEntry("bms/player/beatoraja/MemoryDiagnostics$Snapshot.class") != null, "Missing structured memory diagnostics");
            check(jar.getEntry("bms/player/beatoraja/video/BgrFrameConverter.class") != null, "Missing aligned movie conversion");
            check(jar.getEntry("bms/player/beatoraja/input/ControllerInputDiagnostics.class") != null,
                    "Missing controller diagnostics");
            check(jar.getEntry("bms/player/beatoraja/input/MouseScratchDiagnostics.class") != null,
                    "Missing mouse scratch diagnostics");
            check(jar.getEntry("bms/player/beatoraja/input/MouseMotionQueue.class") != null, "Missing sampled mouse FIFO");
            check(jar.getEntry("bms/player/beatoraja/input/WindowDiagnostics.class") != null, "Missing window diagnostics");
            check(jar.getEntry("bms/player/beatoraja/GlfwWindowSupport.class") != null, "Missing Wayland window capability policy");
            check(jar.getEntry("bms/player/beatoraja/input/MouseScratchPosition.class") != null, "Missing Wayland endpoint sampler");
            check(jar.getEntry("bms/player/beatoraja/input/Lwjgl3InputCallbacks.class") != null,
                    "Missing native callback diagnostics");
            check(jar.getEntry("bms/player/beatoraja/input/KeyboardEventQueue.class") != null
                    && jar.getEntry("bms/player/beatoraja/input/KeyboardJudgeQueue.class") != null, "Missing keyboard queues");
            check(jar.getEntry("bms/player/beatoraja/input/ControllerEventQueue.class") != null
                    && jar.getEntry("bms/player/beatoraja/input/JudgeEventQueue.class") != null, "Missing shared/controller queues");
            check(jar.getEntry("bms/player/beatoraja/input/Lwjgl2ControllerManager.class") == null, "Retired raw manager remains");
            for (var entry : jar.stream().filter(e -> e.getName().startsWith("bms/player/beatoraja/input/JInputControllerManager")
                    && e.getName().endsWith(".class")).toList()) {
                try (var in = jar.getInputStream(entry)) {
                    check(!new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains("org/lwjgl/"),
                            "JInput manager must not reference LWJGL: " + entry.getName());
                }
            }
            check(jar.getEntry("bms/player/beatoraja/external/ScreenShotTwitterExporter.class") == null,
                    "Do not ship retired Twitter exporter");
            check(jar.getEntry("bms/player/beatoraja/external/ScreenShotFileExporter.class") != null,
                    "Screenshot file export must remain available");
            check(jar.getEntry("bms/player/beatoraja/build/DistributionSmokeTest.class") == null, "Do not ship tests");
            check(jar.getEntry("bms/player/beatoraja/audio/AudioBackendTestAssets.class") == null
                    && jar.getEntry("bms/player/beatoraja/build/AudioTestDistributionTest.class") == null,
                    "Do not ship audio fixture generator or verification classes");
            try (InputStream in = jar.getInputStream(jar.getEntry("bms/player/beatoraja/MainLoader.class"))) {
                byte[] header = in.readNBytes(8);
                check(header.length == 8 && header[6] == 0 && header[7] == 61, "Application must target Java 17");
            }
        }
        for (String file : List.of("bin/beatoraja", "bin/beatoraja.bat", "skin/default/select.json",
                "docs/windows-ime.md",
                "docs/ci.md",
                "docs/ir-worker-recovery.md",
                "docs/ir-worker-isolation.md",
                "docs/wayland-compatibility.md",
                "docs/x11-ime.md", "native/linux/x11-ime.c", "scripts/prepare-x11-ime.py", "scripts/build-x11-ime.sh",
                "docs/macos-ime.md",
                "docs/audio-backend-measurement.md",
                "docs/distribution-smoke.md",
                "skin/default/play/play7.luaskin", "font/VL-Gothic-Regular.ttf", "folder/default.json",
                "defaultsound/guide-pg.wav", "natives/libjportaudio.dylib", "natives/libjportaudio.so", "LICENSE",
                "docs/input-diagnostics.md", "docs/input-shutdown.md", "docs/window-input-lifecycle.md", "docs/stage-framebuffer-hidpi.md", "docs/text-input-ime-clipboard.md", "docs/state-endurance.md", "docs/ffmpeg-decoder-alignment.md", "docs/keyboard-events.md", "docs/controller-events.md",
                "docs/mouse-scratch-diagnostics.md", "docs/mouse-scratch-events.md", "docs/window-hidpi-diagnostics.md",
                "docs/audio-diagnostics.md", "docs/frame-pacing-diagnostics.md", "docs/gl-bga-skin-integration.md")) {
            check(Files.isRegularFile(install.resolve(file)), "Missing distribution file: " + file);
        }
        for (String path : List.of("config_sys.json", "config.json", "songdata.db", "songinfo.db", "player", "test", "gl-integration", "build-gradle",
                "skin/default/select/background/Weaponhuman_BGA.wmv")) {
            check(!Files.exists(install.resolve(path)), "Private/local data in distribution: " + path);
        }
        check(Files.isDirectory(install.resolve("table")), "Initial launcher needs a table directory");
        if (System.getProperty("os.name").startsWith("Mac")) {
            byte[] nativeHeader = Files.readAllBytes(install.resolve("natives/libbeatoraja-ime.dylib"));
            int magic = java.nio.ByteBuffer.wrap(nativeHeader).getInt();
            check(magic == 0xcafebabe || magic == 0xcafebabf, "macOS IME must be a universal binary");
        }
        try (var zip = new ZipFile(args[3])) {
            check(zip.stream().noneMatch(e -> e.getName().matches(".*/beatoraja-[^/]+-all\\.jar")), "Fat JAR is a separate opt-in artifact");
            check(zip.stream().noneMatch(e -> e.getName().contains("packaged-game-test-agent")), "Never distribute the observer agent JAR");
            check(zip.stream().noneMatch(e -> e.getName().contains("ime-test.dylib")), "Never ship test-only AppKit JNI hooks");
            verifyAudioTestAssets(install, zip, args.length > 4 && Boolean.parseBoolean(args[4]));
            check(zip.stream().noneMatch(e -> e.getName().contains("/gl-integration/")
                    || e.getName().endsWith("/Lwjgl3BgaSkinIntegrationTest.class")), "Do not ship generated GL test media or tests");
        }
        try (var tables = Files.list(install.resolve("table"))) {
            check(tables.findAny().isEmpty(), "Do not ship local table data");
        }
        check(Files.readString(install.resolve("bin/beatoraja")).contains("cd \"$APP_HOME\""), "Unix working directory");
        check(Files.readString(install.resolve("bin/beatoraja.bat")).contains("cd /d \"%APP_HOME%\""), "Windows working directory");
        check(!Files.readString(install.resolve("bin/beatoraja")).contains("-XstartOnFirstThread"),
                "JavaFX parent must not inherit GLFW's Mac first-thread option");
        Process child = new ProcessBuilder(args[2], "-cp", args[1] + File.pathSeparator + install.resolve("lib/*"),
                DistributionSmokeTest.class.getName(), "--probe")
                .directory(install.toFile()).redirectErrorStream(true).start();
        try {
            if (!child.waitFor(30, TimeUnit.SECONDS)) throw new AssertionError("Packaged runtime probe timed out");
            String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.print(output);
            check(child.exitValue() == 0, "Packaged runtime probe failed");
        } finally {
            if (child.isAlive()) child.destroyForcibly().waitFor();
        }
        System.out.println("DistributionSmokeTest passed");
    }

    static void verifyAudioTestAssets(Path install, ZipFile zip, boolean included) throws Exception {
        Path root = install.resolve("audio-test");
        if (!included) {
            check(!Files.exists(root), "Audio fixtures must be opt-in in installDist");
            check(zip.stream().noneMatch(e -> e.getName().contains("/audio-test/")
                    || e.getName().endsWith("/audio-test")), "Audio fixtures must be opt-in in ZIP");
            return;
        }
        for (String name : List.of("README.txt", "measurements.csv", "SHA256SUMS")) {
            check(Files.isRegularFile(root.resolve(name)), "Missing audio fixture metadata: " + name);
            verifyAudioZipFile(zip, name, Files.readAllBytes(root.resolve(name)));
        }
        List<String> manifest = Files.readAllLines(root.resolve("SHA256SUMS"));
        check(manifest.size() == 12, "Audio fixture manifest must contain both sample rates");
        for (int rate : new int[] {44100, 48000}) {
            for (String name : List.of("timeline.wav", "silence.wav", "trigger.wav", "timeline.bms", "trigger.bms", "markers.csv")) {
                String path = rate + "/" + name;
                check(Files.isRegularFile(root.resolve(path)), "Missing audio fixture: " + path);
                byte[] installed = Files.readAllBytes(root.resolve(path));
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(installed));
                check(manifest.contains(hash + "  " + path), "Installed audio fixture hash: " + path);
                verifyAudioZipFile(zip, path, installed);
            }
        }
    }

    private static void verifyAudioZipFile(ZipFile zip, String path, byte[] installed) throws Exception {
        var entries = zip.stream().filter(e -> e.getName().endsWith("/audio-test/" + path)).toList();
        check(entries.size() == 1, "ZIP audio fixture: " + path);
        try (var input = zip.getInputStream(entries.get(0))) {
            check(java.util.Arrays.equals(installed, input.readAllBytes()), "ZIP audio fixture content: " + path);
        }
    }

    private static void probeRuntime() throws Exception {
        check(Runtime.version().feature() == 17, "Runtime probe must use Java 17");
        verifyClassOrigin("com.badlogic.gdx.Gdx", "gdx-1.14.2.jar");
        verifyClassOrigin("com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator", "gdx-freetype-1.14.2.jar");
        verifyClassOrigin("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application", "gdx-backend-lwjgl3-1.14.2.jar");
        verifyClassOrigin("org.lwjgl.glfw.GLFW", "lwjgl-glfw-3.3.3.jar");
        verifyClassOrigin("org.lwjgl.system.MemoryUtil", "lwjgl-3.3.3.jar");
        for (String retired : List.of("org.lwjgl.Sys", "org.lwjgl.input.Controllers", "com.badlogic.gdx.backends.lwjgl.LwjglApplication")) {
            try {
                Class.forName(retired, false, DistributionSmokeTest.class.getClassLoader());
                throw new AssertionError("LWJGL2 remains in installed runtime: " + retired);
            } catch (ClassNotFoundException expected) { }
        }
        verifyClassOrigin("com.badlogic.gdx.controllers.Controllers", "gdx-controllers-core-2.2.4.jar");
        verifyClassOrigin("com.badlogic.gdx.controllers.desktop.JamepadControllerManager", "gdx-controllers-desktop-2.2.4.jar");
        verifyClassOrigin("com.studiohartman.jamepad.ControllerManager", "jamepad-2.26.5.0.jar");
        verifyClassOrigin("net.java.games.input.ControllerEnvironment", "jinput-2.0.5.jar");
        try {
            Class.forName("com.badlogic.gdx.controllers.desktop.OisControllers");
            throw new AssertionError("Do not ship the old OIS backend");
        } catch (ClassNotFoundException expected) {
            // The raw adapter must not share a classpath with the retired backend.
        }
        JInputControllerCompatibilityTest.main(new String[]{"--packaged"});
        LibGdxCompatibilityTest.main(new String[0]);
        bms.player.beatoraja.SkinProjectionTest.main(new String[0]);
        bms.player.beatoraja.WindowModeControllerTest.main(new String[0]);
        bms.player.beatoraja.input.MouseScratchGeometryTest.main(new String[0]);
        DesktopActionsTest.main(new String[0]);
        InputDiagnosticsTest.main(new String[0]);
        bms.player.beatoraja.input.MouseScratchDiagnosticsTest.main(new String[0]);
        bms.player.beatoraja.input.MouseMotionQueueTest.main(new String[0]);
        KeyboardEventQueueTest.main(new String[0]);
        bms.player.beatoraja.input.ControllerEventQueueTest.main(new String[0]);
        KeyboardJudgeCompatibilityTest.main(new String[0]);
        for (String name : List.of("org.apache.commons.compress.archivers.zip.ZipFile",
                "org.apache.commons.dbutils.QueryRunner", "org.sqlite.JDBC", "org.slf4j.LoggerFactory",
                "org.bytedeco.javacpp.Pointer", "org.bytedeco.javacv.FFmpegFrameGrabber",
                "org.bytedeco.javacpp.avcodec", "com.github.junrar.Archive")) {
            Class<?> type = Class.forName(name, false, DistributionSmokeTest.class.getClassLoader());
            String resource = name.replace('.', '/') + ".class";
            var resources = type.getClassLoader().getResources(resource);
            check(resources.hasMoreElements(), "Missing runtime class: " + name);
            resources.nextElement();
            check(!resources.hasMoreElements(), "Duplicate runtime class: " + name);
            String jarName = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getFileName().toString();
            check(CENTRAL_JARS.containsKey(jarName), "Unexpected runtime JAR for " + name + ": " + jarName);
        }
        try {
            Class.forName("twitter4j.Twitter", false, DistributionSmokeTest.class.getClassLoader());
            throw new AssertionError("Do not ship Twitter4J");
        } catch (ClassNotFoundException expected) {
            // The retired dependency must not be present in the packaged runtime.
        }
        for (String name : List.of("bms.player.beatoraja.MainLoader", "bms.player.beatoraja.ir.IRWorkerMain",
                "javafx.fxml.FXMLLoader", "com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application",
                "org.bytedeco.javacv.FFmpegFrameGrabber")) {
            Class.forName(name, false, DistributionSmokeTest.class.getClassLoader()).getDeclaredMethods();
        }
        var mapper = Class.forName("com.fasterxml.jackson.databind.ObjectMapper").getConstructor().newInstance();
        RarSongArchiveTest.main(new String[0]);
        check(mapper != null, "Bundled Jackson initialization");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            check(connection.isValid(1), "Bundled SQLite runtime");
        }
        var lua = new SkinLuaAccessor(false);
        try {
            lua.setDirectory(Path.of("skin/default"));
            lua.exec("assert(1 + 2 == 3)");
        } finally {
            lua.dispose();
        }
        for (String resource : List.of("bms/player/beatoraja/launcher/PlayConfigurationView.fxml",
                "resources/UIResources.properties", "glsl/ffmpeg.frag")) {
            check(DistributionSmokeTest.class.getClassLoader().getResource(resource) != null, "Missing JAR resource: " + resource);
        }
        System.out.println("Packaged Java 17 runtime, JavaFX, LWJGL3, JInput, Jackson, SQLite, Lua and resources OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyCentralJars(Path install, Path archive) throws Exception {
        try (var files = Files.list(install.resolve("lib")); var zip = new ZipFile(archive.toFile())) {
            var names = files.map(p -> p.getFileName().toString()).toList();
            var entries = zip.stream().toList();
            var prefixes = List.of("commons-compress-", "commons-dbutils-", "sqlite-jdbc-", "slf4j-api-",
                    "javacpp-", "javacv-", "ffmpeg-", "junrar-");
            var expectedNames = CENTRAL_JARS.keySet().stream().sorted().toList();
            check(names.stream().filter(n -> prefixes.stream().anyMatch(n::startsWith)).sorted().toList()
                    .equals(expectedNames), "Installed Central dependency/native set changed");
            check(entries.stream().filter(e -> e.getName().contains("/lib/"))
                    .map(e -> e.getName().substring(e.getName().lastIndexOf('/') + 1))
                    .filter(n -> prefixes.stream().anyMatch(n::startsWith)).sorted().toList()
                    .equals(expectedNames), "ZIP Central dependency/native set changed");
            for (var expected : CENTRAL_JARS.entrySet()) {
                String name = expected.getKey();
                try (var in = Files.newInputStream(install.resolve("lib").resolve(name))) {
                    check(expected.getValue().equals(sha256(in)), "Installed dependency changed: " + name);
                }
                var matching = entries.stream().filter(e -> e.getName().endsWith("/lib/" + name)).toList();
                check(matching.size() == 1, "Missing or duplicate ZIP dependency: " + name);
                try (var in = zip.getInputStream(matching.get(0))) {
                    check(expected.getValue().equals(sha256(in)), "ZIP dependency changed: " + name);
                }
            }
            for (String obsolete : List.of("commons-dbutils.jar", "sqlite-jdbc.jar", "javacpp.jar",
                    "javacv.jar", "javacv-platform.jar", "ffmpeg.jar", "ffmpeg-platform.jar", "junrar.jar")) {
                check(!names.contains(obsolete), "Local duplicate in install: " + obsolete);
                check(entries.stream().noneMatch(e -> e.getName().endsWith("/lib/" + obsolete)),
                        "Local duplicate in ZIP: " + obsolete);
            }
        }
        System.out.println("Central dependencies match existing binaries in install and ZIP");
    }

    private static String sha256(InputStream in) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        for (int read; (read = in.read(buffer)) != -1;) digest.update(buffer, 0, read);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void verifyClassOrigin(String name, String expectedJar) throws Exception {
        ClassLoader loader = DistributionSmokeTest.class.getClassLoader();
        var type = Class.forName(name, false, loader);
        var resources = loader.getResources(name.replace('.', '/') + ".class");
        check(resources.hasMoreElements(), "Missing runtime class: " + name);
        resources.nextElement();
        check(!resources.hasMoreElements(), "Duplicate runtime class: " + name);
        check(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName()
                .toString().equals(expectedJar), "Wrong dependency version for " + name);
        type.getDeclaredMethods();
    }

    private static void verifyGraphicsJars(Path install, Path archive) throws Exception {
        try (var files = Files.list(install.resolve("lib")); var zip = new ZipFile(archive.toFile())) {
            var expected = GRAPHICS_JARS.stream().sorted().toList();
            var names = files.map(p -> p.getFileName().toString()).filter(DistributionSmokeTest::isGraphicsJar)
                    .sorted().toList();
            check(names.equals(expected), "Wrong/duplicate installed libGDX, controller or LWJGL JARs: " + names);
            var entries = zip.stream().filter(e -> e.getName().contains("/lib/"))
                    .filter(e -> {
                        String name = e.getName().substring(e.getName().lastIndexOf('/') + 1);
                        return isGraphicsJar(name);
                    }).toList();
            check(entries.stream().map(e -> e.getName().substring(e.getName().lastIndexOf('/') + 1))
                    .sorted().toList().equals(expected), "Wrong libGDX/LWJGL ZIP dependencies");
            for (var entry : entries) {
                String name = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                try (var installed = Files.newInputStream(install.resolve("lib").resolve(name));
                        var zipped = zip.getInputStream(entry)) {
                    String hash = sha256(installed);
                    check(hash.equals(sha256(zipped)), "ZIP graphics JAR differs: " + name);
                    if (CONTROLLER_JARS.containsKey(name)) {
                        check(hash.equals(CONTROLLER_JARS.get(name)), "Controller dependency changed: " + name);
                    }
                }
            }
        }
    }

    private static boolean isGraphicsJar(String name) {
        return name.startsWith("gdx") || name.startsWith("lwjgl") || name.startsWith("jamepad");
    }

    private static void verifyJInputJars(Path install, Path archive) throws Exception {
        try (var files = Files.list(install.resolve("lib")); var zip = new ZipFile(archive.toFile())) {
            var expected = JINPUT_JARS.keySet().stream().sorted().toList();
            check(files.map(p -> p.getFileName().toString()).filter(n -> n.startsWith("jinput") || n.startsWith("jutils"))
                    .sorted().toList().equals(expected), "JInput runtime set changed");
            var entries = zip.stream().filter(e -> e.getName().contains("/lib/"))
                    .filter(e -> {
                        String name = e.getName().substring(e.getName().lastIndexOf('/') + 1);
                        return name.startsWith("jinput") || name.startsWith("jutils");
                    }).toList();
            check(entries.stream().map(e -> e.getName().substring(e.getName().lastIndexOf('/') + 1))
                    .sorted().toList().equals(expected), "JInput ZIP set changed");
            for (var entry : entries) {
                String name = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                try (var installed = Files.newInputStream(install.resolve("lib").resolve(name));
                        var zipped = zip.getInputStream(entry)) {
                    check(JINPUT_JARS.get(name).equals(sha256(installed)), "JInput runtime binary changed: " + name);
                    check(JINPUT_JARS.get(name).equals(sha256(zipped)), "JInput ZIP binary changed: " + name);
                }
            }
        }
    }
}
