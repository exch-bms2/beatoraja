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
        for (String mode : List.of("eof-wait", "frame-wait", "coalesced-upload", "upload-failure")) {
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
        System.out.println("FFmpegColdStartTest passed (fresh JVM/cache, immediate PLAY, frame-clock/EOF wake, restart/seek/loop, coalesced uploads, upload error, cleanup/XML)");
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
            } else if (mode.equals("coalesced-upload")) {
                verifyCoalescedUpload(processor, worker, uploads);
            } else {
                final FFmpegProcessor movie = processor; final Thread thread = worker;
                await(() -> movie.getFrame(0) != null, uploads, thread);
                await(() -> frameWaiting(thread), uploads, thread);
                long nextFrame = number(thread, "wakeAtMicros");
                movie.getFrame((nextFrame - 1) / 1000);
                long decoded = number(thread, "framecount");
                Thread.sleep(100);
                check(frameWaiting(thread) && number(thread, "framecount") == decoded,
                        "Worker must remain parked just before the next frame timestamp");
                movie.getFrame((nextFrame + 999) / 1000);
                await(() -> number(thread, "framecount") > decoded, uploads, thread);
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
                check(!thread.isAlive() && System.nanoTime() - closing < TimeUnit.SECONDS.toNanos(2), "HALT wakes playback waits promptly");
            }
            for (String name : new String[]{"grabber", "pixmap", "converter", "movieBytes"})
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

    private static boolean frameWaiting(Thread worker) {
        return worker.getState() == Thread.State.WAITING && number(worker, "waitMillis") >= 0;
    }

    private static void verifyCoalescedUpload(FFmpegProcessor processor, Thread worker,
                                             LinkedBlockingQueue<Runnable> uploads) throws Exception {
        Runnable first = uploads.poll(30, TimeUnit.SECONDS);
        check(first != null, "First decoded frame was queued");
        long decoded = number(worker, "framecount");
        for (long time : new long[]{300, 600}) {
            processor.getFrame(time);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (number(worker, "framecount") <= decoded || !waitingForPlayback(worker)) {
                check(worker.isAlive() && System.nanoTime() < deadline, "Decoder must advance while upload is held");
                Thread.sleep(5);
            }
            decoded = number(worker, "framecount");
        }
        System.out.println("Queued uploads after three decoded batches: " + (1 + uploads.size()));
        check(uploads.isEmpty(), "Only one callback may be pending while newer frames overwrite its Pixmap");
        first.run();
        check(number(worker, "uploadCount") == 1 && processor.getFrame(600) != null, "Held callback uploads the current Pixmap once");
        processor.getFrame(850);
        Runnable next = uploads.poll(5, TimeUnit.SECONDS);
        check(next != null, "A new frame queues after the previous upload");
        next.run();
        check(number(worker, "uploadCount") == 2, "New upload completes");
        processor.dispose();
        worker.join(2000);
        check(!worker.isAlive(), "Disposal wakes the decoder after coalesced uploads");
    }

    private static boolean waitingForPlayback(Thread worker) {
        Thread.State state = worker.getState();
        return (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING)
                && number(worker, "waitMillis") >= 0;
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
