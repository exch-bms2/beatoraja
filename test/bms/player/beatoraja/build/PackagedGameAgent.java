package bms.player.beatoraja.build;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import bms.player.beatoraja.*;
import bms.player.beatoraja.MainState.MainStateType;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Explicit test agent, no bytecode changes. Drives packaged public UI/state paths only. */
public final class PackagedGameAgent {
    private static Path root, evidence;
    private static volatile MainController main;
    private static volatile Thread inputWorker;
    private static volatile Thread glThread;
    private static volatile boolean inputSeen, finished, playSeen, sequenceComplete;
    private static boolean pressed;
    private static int phase, frames;
    private static String previousState = "";

    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        root = PackagedGameSupport.verifyRoot(System.getProperty("beatoraja.packagedGameTest.root"));
        evidence = root.resolve("test-evidence");
        String command = System.getProperty("sun.java.command", "");
        if (command.contains("--display-modes") || command.contains("--runtime-smoke")) return;
        boolean game = Boolean.getBoolean("beatoraja.gameProcess");
        String role = game ? "game" : "launcher";
        Files.writeString(evidence.resolve(role + ".pid"), Long.toString(ProcessHandle.current().pid()));
        // No GitHub/IR network call is needed to validate the Launcher/game boundary.
        MainLoader.setVersionChecker(new MainLoader.VersionChecker() {
            public String getMessage() { return ""; }
            public String getDownloadURL() { return ""; }
        });
        if (game) Runtime.getRuntime().addShutdownHook(new Thread(PackagedGameAgent::shutdownEvidence, "packaged-game-shutdown-evidence"));
        Thread observer = new Thread(() -> observe(game), "packaged-game-observer");
        observer.setDaemon(true); observer.start();
    }

    private static void observe(boolean game) {
        var pending = new AtomicBoolean();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(100);
        while (!finished && System.nanoTime() < deadline) {
            if (pending.compareAndSet(false, true)) {
                Runnable tick = () -> {
                    try { if (game) gameTick(); else launcherTick(); }
                    catch (Throwable failure) { fail(failure, game); }
                    finally { pending.set(false); }
                };
                try {
                    if (game) {
                        if (Gdx.app != null) Gdx.app.postRunnable(tick); else pending.set(false);
                    } else Platform.runLater(tick);
                } catch (IllegalStateException notStarted) { pending.set(false); }
            }
            try { Thread.sleep(100); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); return; }
        }
        if (!finished) fail(new AssertionError("Packaged " + (game ? "game" : "launcher") + " observer timeout"), game);
    }

    private static void launcherTick() throws Exception {
        Stage stage = Window.getWindows().stream().filter(w -> w instanceof Stage && w.isShowing())
                .map(w -> (Stage)w).filter(s -> s.getTitle().endsWith(" configuration")).findFirst().orElse(null);
        if (stage == null || ++frames < 3) return;
        String label = java.util.ResourceBundle.getBundle("resources.UIResources").getString("START");
        captureLauncher(stage, "launcher.png");
        Button start = stage.getScene().getRoot().lookupAll(".button").stream().filter(n -> n instanceof Button)
                .map(n -> (Button)n).filter(b -> label.equals(b.getText())).findFirst().orElseThrow();
        Files.writeString(evidence.resolve("launcher-ready.txt"), "actual JavaFX FXML START button; packaged MainLoader=" + source(MainLoader.class) + "\n");
        finished = true;
        start.fire();
    }
    private static void captureLauncher(Stage stage, String name) {
        GdxNativesLoader.load();
        var snapshot = stage.getScene().snapshot(null);
        var image = new Pixmap((int)snapshot.getWidth(), (int)snapshot.getHeight(), Pixmap.Format.RGBA8888);
        try {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int argb = snapshot.getPixelReader().getArgb(x, y); image.drawPixel(x, y, (argb << 8) | (argb >>> 24));
            }
            saveImage(image, name);
        } finally { image.dispose(); }
    }

    private static void gameTick() throws Exception {
        glThread = Thread.currentThread();
        if (main == null) main = findMain(Gdx.app.getApplicationListener(), 0);
        if (main == null || main.getCurrentState() == null || main.getInputProcessor() == null) return;
        var input = main.getInputProcessor();
        Object polling = field(input, "polling");
        inputWorker = (Thread)field(polling, "thread");
        inputSeen |= inputWorker != null && inputWorker.isAlive();
        MainState state = main.getCurrentState();
        String name = state.type.name();
        if (!name.equals(previousState)) { previousState = name; frames = 0; }
        if (++frames < 3 || state.getSkin() == null) return;
        if (phase == 0 && state.type == MainStateType.MUSICSELECT) {
            if (Boolean.getBoolean("beatoraja.packagedGameTest.requireMesa"))
                PackagedGameSupport.check(System.getProperty("os.name").startsWith("Linux")
                        && org.lwjgl.glfw.GLFW.glfwGetPlatform() == org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
                        && Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER).toLowerCase(java.util.Locale.ROOT).contains("llvmpipe"),
                        "Packaged game must use Linux X11/Mesa llvmpipe");
            capture("select-before.png");
            Files.writeString(evidence.resolve("game-runtime.txt"), "MainController=" + source(MainController.class)
                    + "\nclasspath=" + System.getProperty("java.class.path") + "\nrenderer=" + Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER)
                    + "\nbackend=" + org.lwjgl.glfw.GLFW.glfwGetPlatform() + "\n");
            main.setPlayMode(BMSPlayerMode.PLAY);
            PackagedGameSupport.check(main.getPlayerResource().setBMSFile(root.resolve("test-song/smoke.bms"), BMSPlayerMode.PLAY), "Generated chart loads");
            main.changeState(MainStateType.DECIDE); phase = 1;
        } else if (phase == 1 && state.type == MainStateType.PLAY
                && main.getTimer().isTimerOn(bms.player.beatoraja.skin.SkinProperty.TIMER_PLAY)) {
            if (!playSeen) { capture("play.png"); playSeen = true; }
            // Exercise this window's real callback/FIFO/judge path. No global OS input
            // injection or fabricated ScoreData: a real judgment is required for RESULT.
            long handle = ((com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle();
            var focus = org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback(handle, null);
            org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback(handle, focus); focus.invoke(handle, true);
            pressed = !pressed;
            key(pressed);
        } else if (phase == 1 && state.type == MainStateType.RESULT) {
            PackagedGameSupport.check(playSeen, "Actual PLAY timer/pixels must precede RESULT");
            if (pressed) { key(false); pressed = false; }
            capture("result.png"); main.changeState(MainStateType.MUSICSELECT); phase = 2;
        } else if (phase == 2 && state.type == MainStateType.MUSICSELECT) {
            capture("select-after.png");
            PackagedGameSupport.check(inputSeen && inputWorker.isAlive(), "Real input polling thread observed before shutdown");
            Files.writeString(evidence.resolve("game-ready.txt"), "actual SELECT -> DECIDE -> PLAY -> RESULT -> SELECT; normal main.exit requested\n");
            sequenceComplete = true; finished = true; main.exit();
        }
    }

    private static void key(boolean value) {
        long handle = ((com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle();
        var callback = org.lwjgl.glfw.GLFW.glfwSetKeyCallback(handle, null);
        org.lwjgl.glfw.GLFW.glfwSetKeyCallback(handle, callback);
        callback.invoke(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_Z, 0,
                value ? org.lwjgl.glfw.GLFW.GLFW_PRESS : org.lwjgl.glfw.GLFW.GLFW_RELEASE, 0);
    }
    private static MainController findMain(Object object, int depth) throws Exception {
        if (object == null || depth > 3) return null;
        for (Field field : object.getClass().getDeclaredFields()) {
            if (field.getType() == MainController.class) { field.setAccessible(true); return (MainController)field.get(object); }
            if (ApplicationListener.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true); var found = findMain(field.get(object), depth + 1); if (found != null) return found;
            }
        }
        return null;
    }
    private static Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static String source(Class<?> type) throws Exception {
        Path location = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        PackagedGameSupport.check(location.startsWith(root.resolve("lib")) && location.toString().endsWith(".jar"), "Production class must come from extracted ZIP: " + type.getName());
        return location.toString();
    }

    private static void capture(String name) throws Exception {
        var image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { saveImage(image, name); } finally { image.dispose(); }
    }
    private static void saveImage(Pixmap image, String name) {
        int ink = 0;
        for (int y = 0; y < image.getHeight(); y += 8) for (int x = 0; x < image.getWidth(); x += 8)
            if ((image.getPixel(x, y) & 0xffffff00) != 0) ink++;
        PixmapIO.writePNG(new FileHandle(evidence.resolve(name).toFile()), image, -1, true);
        PackagedGameSupport.check(ink > 16, "Nonblank actual screen: " + name);
    }
    private static void shutdownEvidence() {
        try {
            var report = new Properties();
            report.setProperty("inputObservedRunning", Boolean.toString(inputSeen));
            report.setProperty("inputThreadAlive", Boolean.toString(inputWorker == null || inputWorker.isAlive()));
            boolean released = main != null && ((java.util.concurrent.atomic.AtomicBoolean)field(main.getInputProcessor(), "devicesReleased")).get();
            report.setProperty("devicesReleased", Boolean.toString(released));
            report.setProperty("gameSequenceComplete", Boolean.toString(sequenceComplete));
            try (var out = Files.newOutputStream(evidence.resolve("shutdown.properties"))) { report.store(out, "Observed only; does not stop/join input or prove process exit"); }
            dumpThreads();
        } catch (Throwable failure) { fail(failure, false); }
    }
    private static void dumpThreads() throws Exception {
        var text = new StringBuilder();
        for (var entry : Thread.getAllStackTraces().entrySet()) {
            text.append(entry.getKey().getName()).append(" alive=").append(entry.getKey().isAlive()).append('\n');
            for (var frame : entry.getValue()) text.append("  ").append(frame).append('\n');
        }
        Files.writeString(evidence.resolve("threads-" + ProcessHandle.current().pid() + ".txt"), text);
    }
    private static void fail(Throwable failure, boolean game) {
        finished = true; failure.printStackTrace();
        try {
            var trace = new java.io.StringWriter(); failure.printStackTrace(new java.io.PrintWriter(trace));
            Files.writeString(evidence.resolve("agent-failure-" + ProcessHandle.current().pid() + ".txt"), trace.toString()); dumpThreads();
            if (game && Gdx.gl != null && Thread.currentThread() == glThread) capture("failure.png");
            if (!game && Platform.isFxApplicationThread())
                Window.getWindows().stream().filter(w -> w instanceof Stage && w.isShowing()).findFirst()
                        .ifPresent(w -> captureLauncher((Stage)w, "failure-launcher.png"));
        } catch (Throwable extra) { failure.addSuppressed(extra); }
        if (game && Gdx.app != null) Gdx.app.exit();
        else if (!Thread.currentThread().getName().equals("packaged-game-shutdown-evidence")) System.exit(2);
    }
}
