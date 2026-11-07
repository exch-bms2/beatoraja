package bms.player.beatoraja.video;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;
import bms.player.beatoraja.skin.MockTextureEnvironment;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.GdxNativesLoader;

/** Fresh worker JVM/native cache per case; fixture generation never warms the child's FFmpeg loader. */
public final class FFmpegColdStartTest {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("child")) { child(Path.of(args[1]), Path.of(args[2]), args[3]); return; }
        Path root = Files.createDirectories(Path.of(args[0]).toAbsolutePath());
        Path run = Files.createTempDirectory(root, "cold video ");
        Path fixture = run.resolve("colors.avi");
        Lwjgl3BgaSkinIntegrationTest.generateMovie(fixture, true);
        for (String mode : List.of("eof-wait", "frame-wait", "upload-failure")) {
            Path output = run.resolve(mode + ".log");
            Path xml = run.resolve(mode + ".xml");
            Path cache = Files.createDirectory(run.resolve(mode + "-native-cache"));
            var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(),
                    "-Xmx512m", "-XX:-CreateCoredumpOnCrash", "-XX:ErrorFile=" + run.resolve("hs_err_pid%p.log"),
                    "-Dorg.bytedeco.javacpp.cachedir=" + cache, "-cp", System.getProperty("java.class.path"),
                    FFmpegColdStartTest.class.getName(), "child", fixture.toString(), xml.toString(), mode)
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try {
                check(process.waitFor(60, TimeUnit.SECONDS), "Cold worker timeout: " + output);
                String text = new String(Files.readAllBytes(output), java.nio.charset.StandardCharsets.UTF_8);
                check(process.exitValue() == 0 && text.contains("COLD-VIDEO-PASS"), "Cold worker failure: " + output);
            } finally {
                if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
            }
            String log = Files.readString(xml);
            check(log.contains("phase=") && log.contains("lastCommand=") && log.contains("uploads="), "Lifecycle XML: " + xml);
            check(log.contains("reason=" + (mode.equals("upload-failure") ? "upload-failure" : "disposed")), "Exit reason: " + xml);
            if (mode.equals("upload-failure")) check(log.contains("Synthetic movie upload failure")
                    && log.contains("phase=texture-upload") && log.contains("<exception>"), "Upload failure XML");
            System.out.println("Cold video " + mode + " passed: " + output);
        }
        System.out.println("FFmpegColdStartTest passed (fresh JVM/cache, immediate PLAY, timed/EOF wake, restart/seek/loop, upload error, cleanup/XML)");
    }

    private static void child(Path fixture, Path xml, String mode) throws Exception {
        GdxNativesLoader.load();
        var uploads = new LinkedBlockingQueue<Runnable>();
        var previousApp = Gdx.app;
        var handler = new FileHandler(xml.toString()); handler.setFormatter(new XMLFormatter());
        handler.setEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        Logger.getGlobal().addHandler(handler);
        FFmpegProcessor processor = null; Thread worker = null;
        var gl = new MockTextureEnvironment();
        try {
            Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(), new Class<?>[]{Application.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("postRunnable")) { uploads.add((Runnable) args[0]); return null; }
                        throw new AssertionError("Unexpected app call: " + method.getName());
                    });
            if (mode.equals("upload-failure")) {
                GL20 delegate = Gdx.gl;
                Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[]{GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glTexImage2D")) throw new IllegalStateException("Synthetic movie upload failure");
                            try { return method.invoke(delegate, args); }
                            catch (InvocationTargetException e) { throw e.getCause(); }
                        });
            }
            processor = new FFmpegProcessor(1);
            processor.create(fixture.toString());
            var field = FFmpegProcessor.class.getDeclaredField("movieseek"); field.setAccessible(true);
            worker = (Thread) field.get(processor);
            // The old exec() interrupted native loading before its cache lock completed here.
            processor.play(0, false);
            check(!worker.isInterrupted(), "Immediate PLAY must not interrupt cold native initialization");
            if (mode.equals("upload-failure")) {
                Runnable upload = uploads.poll(30, TimeUnit.SECONDS);
                check(upload != null, "First decoded upload");
                try { upload.run(); throw new AssertionError("Expected upload failure"); }
                catch (com.badlogic.gdx.utils.GdxRuntimeException expected) {
                    check(expected.getCause() instanceof IllegalStateException, "Original upload error retained");
                }
                worker.join(5000); check(!worker.isAlive(), "Upload failure halts decoder");
            } else {
                final FFmpegProcessor movie = processor; final Thread thread = worker;
                await(() -> movie.getFrame(0) != null, uploads, thread);
                movie.getFrame(-60_000);
                await(() -> number(thread, "waitMillis") > 50_000 && thread.getState() == Thread.State.TIMED_WAITING,
                        uploads, thread);
                if (mode.equals("eof-wait")) {
                    movie.stop();
                    await(() -> number(thread, "waitMillis") == -1 && thread.getState() == Thread.State.WAITING, uploads, thread);
                    check(!thread.isInterrupted(), "STOP wakes queue without interruption");
                    long restarts = number(thread, "restartCount");
                    movie.play(0, false);
                    await(() -> number(thread, "restartCount") > restarts, uploads, thread);
                    movie.getFrame(2000);
                    await(() -> number(thread, "waitMillis") == -1 && thread.getState() == Thread.State.WAITING, uploads, thread);
                    long beforeLoop = number(thread, "restartCount");
                    movie.play(0, true);
                    await(() -> number(thread, "restartCount") > beforeLoop, uploads, thread);
                    movie.getFrame(2000);
                    await(() -> number(thread, "restartCount") > beforeLoop + 1, uploads, thread);
                    movie.stop();
                    await(() -> number(thread, "waitMillis") == -1 && thread.getState() == Thread.State.WAITING, uploads, thread);
                }
                long closing = System.nanoTime();
                movie.dispose(); thread.join(2000);
                check(!thread.isAlive() && System.nanoTime() - closing < TimeUnit.SECONDS.toNanos(2), "HALT wakes long waits promptly");
            }
            for (String name : new String[]{"grabber", "pixmap", "converter", "movieBytes", "frameRow"})
                check(FFmpegProcessorLifecycleTest.field(worker, name) == null, "Released " + name);
            processor.dispose();
            Runnable pending; while ((pending = uploads.poll()) != null) pending.run();
            check(processor.getFrame(0) == null, "No stale frame after disposal");
            System.out.println("COLD-VIDEO-PASS " + mode);
        } finally {
            try {
                if (processor != null) processor.dispose();
                if (worker != null) { worker.join(5000); check(!worker.isAlive(), "Decoder must not survive test exit"); }
            } finally {
                gl.close(); Gdx.app = previousApp;
                Logger.getGlobal().removeHandler(handler); handler.close();
            }
        }
    }

    private static long number(Thread worker, String name) {
        try { return (long) FFmpegProcessorLifecycleTest.field(worker, name); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static void await(BooleanSupplier ready, LinkedBlockingQueue<Runnable> uploads, Thread worker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!ready.getAsBoolean()) {
            check(worker.isAlive(), "Decoder exited early");
            check(System.nanoTime() < deadline, "Decoder/control timeout, phase=" + FFmpegProcessorLifecycleTest.field(worker, "phase"));
            Runnable runnable = uploads.poll(5, TimeUnit.MILLISECONDS);
            if (runnable != null) runnable.run();
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
