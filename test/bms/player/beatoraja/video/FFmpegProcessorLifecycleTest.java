package bms.player.beatoraja.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.channels.FileLockInterruptionException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;
import bms.player.beatoraja.song.SongResource;
import bms.player.beatoraja.song.SongResources;

/** Queue wakeup, interruptible resource I/O and XML diagnostics without loading FFmpeg natives. */
public final class FFmpegProcessorLifecycleTest {
    public static void main(String[] args) throws Exception {
        queueWakeup(false);
        queueWakeup(true);
        haltFullQueue();
        commandsDuringRead();
        failureLog();
        System.out.println("FFmpegProcessorLifecycleTest passed (FIFO/timed/idle wake, full-queue HALT, non-interrupted I/O, XML cause/exit logs)");
    }

    private static void queueWakeup(boolean timed) throws Exception {
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(SongResources.fromPath(Path.of("unused.mp4")));
        var queue = queue(worker);
        var result = new FutureTask<>(() -> timed ? queue.pollFirst(1, TimeUnit.MINUTES) : queue.takeFirst());
        var waiter = new Thread(result, "movie-command-test"); waiter.setDaemon(true); waiter.start();
        try {
            worker.exec(FFmpegProcessor.Command.PLAY);
            check(result.get(2, TimeUnit.SECONDS) == FFmpegProcessor.Command.PLAY, "Queue signals both timed/idle waits");
            check(!worker.isInterrupted(), "Playback commands never set worker interrupt status");
            worker.exec(FFmpegProcessor.Command.STOP); worker.exec(FFmpegProcessor.Command.LOOP);
            check(queue.pollFirst() == FFmpegProcessor.Command.STOP && queue.pollFirst() == FFmpegProcessor.Command.LOOP,
                    "Normal commands remain FIFO");
        } finally {
            queue.offerFirst(FFmpegProcessor.Command.HALT); waiter.join(2000);
            worker.exec(FFmpegProcessor.Command.HALT); worker.start(); join(worker);
            processor.dispose();
        }
    }

    private static void haltFullQueue() throws Exception {
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(SongResources.fromPath(Path.of("unused.mp4")));
        var queue = queue(worker);
        for (int i = 0; i < 4; i++) worker.exec(FFmpegProcessor.Command.STOP);
        check(queue.remainingCapacity() == 0, "Test queue must be full");
        worker.exec(FFmpegProcessor.Command.HALT);
        check((boolean) field(worker, "haltRequested") && queue.size() == 1
                && queue.peekFirst() == FFmpegProcessor.Command.HALT, "HALT clears backlog and supplies a wakeup marker");
        worker.exec(FFmpegProcessor.Command.PLAY);
        check(queue.size() == 1 && !worker.isInterrupted(), "Halted workers reject commands without interruption");
        worker.start(); join(worker);
        released(worker); check(queue.isEmpty(), "HALT marker cleared after exit");
        processor.dispose(); processor.dispose();
    }

    private static void commandsDuringRead() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var interrupted = new AtomicBoolean(); var closed = new AtomicBoolean();
        var input = new InputStream() {
            @Override public int read() throws IOException {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test read timeout");
                } catch (InterruptedException e) { interrupted.set(true); throw new IOException(e); }
                return -1;
            }
            @Override public void close() { closed.set(true); }
        };
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(resource(() -> input));
        var logs = new ByteArrayOutputStream(); var handler = capture(logs);
        try {
            worker.start(); check(entered.await(2, TimeUnit.SECONDS), "Read entered");
            worker.exec(FFmpegProcessor.Command.PLAY); worker.exec(FFmpegProcessor.Command.STOP);
            worker.exec(FFmpegProcessor.Command.LOOP); worker.exec(FFmpegProcessor.Command.HALT);
            check(!worker.isInterrupted(), "HALT does not interrupt active I/O");
            release.countDown(); join(worker);
            check(!interrupted.get() && closed.get(), "Resource I/O completes/closes cooperatively");
            released(worker);
        } finally {
            release.countDown(); worker.exec(FFmpegProcessor.Command.HALT); join(worker);
            Logger.getGlobal().removeHandler(handler); handler.close(); processor.dispose();
        }
        check(logs.toString(StandardCharsets.UTF_8).contains("reason=halt"), "Normal exit reason in XML");
    }

    private static void failureLog() throws Exception {
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(resource(() -> new InputStream() {
            @Override public int read() throws IOException {
                throw new IOException("Synthetic read failure", new FileLockInterruptionException());
            }
            @Override public void close() throws IOException { throw new IOException("Synthetic close failure"); }
        }));
        var logs = new ByteArrayOutputStream(); var handler = capture(logs);
        try { worker.start(); join(worker); released(worker); }
        finally { Logger.getGlobal().removeHandler(handler); handler.close(); processor.dispose(); }
        String xml = logs.toString(StandardCharsets.UTF_8);
        check(xml.contains("<exception>") && xml.contains("Synthetic read failure")
                && xml.contains("FileLockInterruptionException") && xml.contains("Synthetic close failure")
                && xml.contains("Suppressed:") && xml.contains("phase=resource-read")
                && xml.contains("reason=failure"), "XML contains source, phase, stack/cause and exit reason");
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(logs.toByteArray()));
        check(document.getElementsByTagName("record").getLength() >= 2, "Failure and final records are valid XML");
        worker.exec(FFmpegProcessor.Command.PLAY);
        check(queue(worker).isEmpty(), "Failed/finished workers reject new commands");
    }

    private static StreamHandler capture(ByteArrayOutputStream output) throws IOException {
        var handler = new StreamHandler(output, new XMLFormatter());
        handler.setEncoding(StandardCharsets.UTF_8.name());
        Logger.getGlobal().addHandler(handler); return handler;
    }

    @FunctionalInterface private interface Open { InputStream open() throws IOException; }
    private static SongResource resource(Open input) {
        return (SongResource) Proxy.newProxyInstance(SongResource.class.getClassLoader(), new Class<?>[]{SongResource.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "openStream" -> input.open();
                    case "displayPath", "name", "cacheKey" -> "synthetic & movie.mp4";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static LinkedBlockingDeque<FFmpegProcessor.Command> queue(Thread worker) throws Exception {
        return (LinkedBlockingDeque<FFmpegProcessor.Command>) field(worker, "commands");
    }
    static Object field(Thread worker, String name) throws Exception {
        var field = worker.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(worker);
    }
    private static void released(Thread worker) throws Exception {
        for (String name : new String[]{"grabber", "pixmap", "converter", "movieBytes", "frameRow"})
            check(field(worker, name) == null, "Released " + name);
    }
    private static void join(Thread worker) throws InterruptedException {
        worker.join(5000); check(!worker.isAlive(), "Worker terminates without an interrupt");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
