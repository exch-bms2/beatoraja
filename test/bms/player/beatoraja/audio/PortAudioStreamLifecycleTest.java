package bms.player.beatoraja.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.portaudio.BlockingStream;
import com.portaudio.StreamInfo;

/** Native-free regression tests for PortAudio stream startup, failure, and shutdown. */
public final class PortAudioStreamLifecycleTest {
    public static void main(String[] args) throws Exception {
        assertAsioMinimumBufferSize();
        assertStartFailure();
        assertWriteFailure();
        assertCloseFailure();
        assertShutdownDuringWrite();
        assertStreamInfoLogging();
        assertAbortOnlyOnce();
        assertAlreadyStopped();
        assertWriteStatistics();
        assertStartupWait();
        assertAvailabilityTimeout();
        assertAbortDuringAvailabilityWait();
        assertInvalidAvailability();
        assertCloseDuringNativeCalls();
        System.out.println("PortAudioStreamLifecycleTest passed");
    }

    private static void assertCloseDuringNativeCalls() throws Exception {
        for (String operation : List.of("start", "write", "info", "abort")) {
            var entered = new CountDownLatch(1);
            var unblock = new CountDownLatch(1);
            var closes = new AtomicInteger();
            var releases = new AtomicInteger();
            var aborts = new AtomicInteger();
            var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            var nativeStream = new FakeStream() {
                private void block(String name) {
                    if (operation.equals(name)) { entered.countDown(); await(unblock); }
                }
                @Override public void start() { block("start"); }
                @Override public boolean write(float[] buffer, int frames) { block("write"); return false; }
                @Override public StreamInfo getInfo() { block("info"); return null; }
                @Override public void abort() {
                    aborts.incrementAndGet();
                    block("abort");
                    throw new IllegalStateException("Abort failed to unblock native call");
                }
                @Override public void close() { closes.incrementAndGet(); }
            };
            var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> {
                releases.incrementAndGet();
                throw new IllegalStateException("Runtime release failed");
            });
            Thread worker = new Thread(() -> {
                try {
                    switch (operation) {
                        case "start" -> stream.start();
                        case "write" -> stream.write(new float[32], 16);
                        case "info" -> stream.logInfo(1);
                        case "abort" -> stream.abort();
                    }
                } catch (Throwable e) { error.set(e); }
            });
            worker.start();
            try {
                await(entered);
                stream.close(); stream.close(); stream.abort();
                check(aborts.get() == 1 && closes.get() == 0 && releases.get() == 0,
                        "Failed abort cannot close/terminate during native " + operation);
                check(!stream.write(new float[32], 16), "No writes after close request");
                expectFailure(stream::start);
                stream.logInfo(1);
            } finally {
                unblock.countDown(); worker.join(5000);
            }
            check(!worker.isAlive() && error.get() == null, "Native call exits: " + error.get());
            stream.close();
            check(closes.get() == 1 && releases.get() == 1, "Deferred cleanup runs once after " + operation);
        }
    }

    private static void assertAsioMinimumBufferSize() {
        var messages = new ArrayList<String>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Logger logger = Logger.getGlobal();
        logger.addHandler(handler);
        try {
            var sizes = new PortAudioAsioDiagnostics.BufferSizes(64, 2048, 64, 8);
            check(PortAudioDriver.adjustAsioBufferSize(0, 16, sizes) == 64, "Below-minimum buffers must be raised");
            check(messages.size() == 1 && messages.get(0).contains("requestedFrames=16")
                    && messages.get(0).contains("effectiveFrames=64"), "Adjustment must log both sizes");
            messages.clear();
            check(PortAudioDriver.adjustAsioBufferSize(0, 64, sizes) == 64, "Minimum must remain unchanged");
            check(PortAudioDriver.adjustAsioBufferSize(0, 256, sizes) == 256, "Larger buffers must remain unchanged");
            check(PortAudioDriver.adjustAsioBufferSize(0, 16, null) == 16, "Unavailable queries must preserve the request");
            check(PortAudioDriver.adjustAsioBufferSize(0, 16,
                    new PortAudioAsioDiagnostics.BufferSizes(-1, 2048, 64, 8)) == 16,
                    "Invalid minimum must not change the request");
            check(messages.isEmpty(), "Unchanged buffers must not log an adjustment");
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static void assertAbortOnlyOnce() {
        var nativeStream = new FakeStream();
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        stream.abort();
        stream.abort();
        stream.close();
        check(nativeStream.events.equals(List.of("abort", "close")),
                "Explicit abort followed by close must abort only once");
    }

    private static void assertAlreadyStopped() {
        var nativeStream = new FakeStream() {
            @Override public boolean isStopped() { return true; }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        stream.close();
        check(nativeStream.events.equals(List.of("close")), "Stopped streams must not be aborted again");
    }

    private static void assertWriteStatistics() {
        var nativeStream = new FakeStream();
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        for (int i = 0; i < 5; i++) stream.write(new float[32], 16);
        nativeStream.failWrite = true;
        expectFailure(() -> stream.write(new float[32], 16));
        var statistics = stream.getWriteStatistics();
        check(statistics.attempts() == 6 && statistics.completed() == 5 && statistics.failed() == 1,
                "Write statistics must distinguish successful and failed attempts");
        check(statistics.totalNanos() >= statistics.maxNanos()
                && statistics.maxNanos() >= statistics.lastNanos(), "Write durations must be accumulated");
        check(nativeStream.availableQueries == 6, "Availability must be checked before every write");
        stream.close();
    }

    private static void assertStartupWait() {
        var nativeStream = new FakeStream() {
            private int writes;
            private long readyAt;
            private int lastAvailable;

            @Override
            public int getWriteAvailable() {
                check(writes < 2, "Running stream must not poll for write availability");
                if (writes == 1 && readyAt == 0) {
                    readyAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(40);
                }
                lastAvailable = writes == 0 || System.nanoTime() >= readyAt ? 128 : 0;
                return lastAvailable;
            }

            @Override
            public boolean write(float[] buffer, int frames) {
                if (writes < 2) {
                    check(lastAvailable >= frames, "Startup write must wait for the entire block to fit");
                }
                writes++;
                return super.write(buffer, frames);
            }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        stream.start();
        check(stream.write(new float[256], 128), "First block must be written immediately");
        long started = System.nanoTime();
        check(stream.write(new float[256], 128), "Startup wait longer than 23ms must be allowed");
        check(System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(30),
                "Test must exercise the startup grace period");
        check(stream.getWriteStatistics().completed() == 2, "No native write retries should occur");
        check(stream.write(new float[256], 128), "Running stream must use direct native write");
        nativeStream.failWrite = true;
        expectFailure(() -> stream.run(() -> stream.write(new float[256], 128)));
        var statistics = stream.getWriteStatistics();
        check(statistics.attempts() == 4 && statistics.completed() == 3 && statistics.failed() == 1,
                "Native write failures must stop the stream without a retry or returning to polling");
        check(nativeStream.events.equals(List.of("start", "write", "write", "write", "write", "abort", "close")),
                "Running stream failure must close and release the stream");
    }

    private static void assertAvailabilityTimeout() {
        var nativeStream = new FakeStream() {
            @Override public int getWriteAvailable() { return 0; }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        stream.start();
        expectFailure(() -> stream.run(() -> stream.write(new float[256], 128)));
        check(!nativeStream.events.contains("write"), "Unavailable stream must not receive a native write");
        check(stream.getWriteStatistics().failed() == 1, "Startup availability timeout must be counted");
        check(nativeStream.events.equals(List.of("start", "abort", "close")),
                "Availability timeout must still close the stream");
    }

    private static void assertAbortDuringAvailabilityWait() throws Exception {
        CountDownLatch polling = new CountDownLatch(1);
        var nativeStream = new FakeStream() {
            @Override
            public int getWriteAvailable() {
                polling.countDown();
                return 0;
            }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        AtomicInteger cancelled = new AtomicInteger();
        Thread worker = new Thread(() -> stream.run(() -> {
            if (!stream.write(new float[256], 128)) cancelled.incrementAndGet();
        }));
        worker.start();
        try {
            await(polling);
            stream.abort();
            worker.join(5000);
            check(!worker.isAlive() && cancelled.get() == 1, "Abort must cancel the availability wait");
            var statistics = stream.getWriteStatistics();
            check(statistics.cancelled() == 1 && statistics.failed() == 0,
                    "Shutdown cancellation must not be counted as failure");
            check(nativeStream.events.equals(List.of("abort", "close")),
                    "Cancelled wait must close once without any native write");
        } finally {
            stream.abort();
            worker.join(5000);
        }
    }

    private static void assertInvalidAvailability() {
        var nativeStream = new FakeStream() {
            @Override public int getWriteAvailable() { return -10000; }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
        expectFailure(() -> stream.run(() -> stream.write(new float[32], 16)));
        check(!nativeStream.events.contains("write"), "Availability errors must not cause a write or retry loop");
    }

    private static void assertStreamInfoLogging() {
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Logger logger = Logger.getGlobal();
        logger.addHandler(handler);
        try {
            var nativeStream = new FakeStream() {
                @Override
                public StreamInfo getInfo() {
                    var info = new StreamInfo();
                    info.outputLatency = 0.01;
                    info.sampleRate = 48000;
                    return info;
                }
            };
            var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> { });
            stream.logInfo(2);
            check(messages.stream().anyMatch(message -> message.contains("device=2")
                    && message.contains("outputLatencySeconds=0.01")
                    && message.contains("outputLatencyMs=10.0")
                    && message.contains("sampleRate=48000.0")), "Stream info must include values with units");
            stream.close();

            var failing = new FakeStream() {
                @Override public StreamInfo getInfo() { throw new IllegalStateException("info failed"); }
            };
            var failingStream = new PortAudioDriver.StreamLifecycle(failing, () -> { });
            failingStream.start();
            failingStream.logInfo(3);
            failingStream.write(new float[32], 16);
            check(failing.events.equals(List.of("start", "write")),
                    "Diagnostic failure must not stop or close the stream");
            failingStream.close();
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static void assertStartFailure() {
        var nativeStream = new FakeStream();
        nativeStream.failStart = true;
        var events = nativeStream.events;
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> events.add("release"));
        expectFailure(stream::start);
        stream.close();
        check(events.equals(List.of("start", "abort", "close", "release")),
                "Failed start must close and release exactly once");
    }

    private static void assertWriteFailure() {
        var nativeStream = new FakeStream();
        nativeStream.failWrite = true;
        var events = nativeStream.events;
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, () -> events.add("release"));
        stream.start();
        expectFailure(() -> stream.run(() -> stream.write(new float[32], 16)));
        stream.close();
        check(events.equals(List.of("start", "write", "abort", "close", "release")),
                "Write failure must close before releasing PortAudio");
    }

    private static void assertCloseFailure() {
        var nativeStream = new FakeStream();
        nativeStream.failAbort = true;
        nativeStream.failClose = true;
        AtomicInteger releases = new AtomicInteger();
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, releases::incrementAndGet);
        stream.close();
        stream.close();
        stream.abort();
        check(nativeStream.events.equals(List.of("abort", "close")),
                "Close must still be attempted after abort fails, without duplicate cleanup");
        check(releases.get() == 1, "Native cleanup failure must still release the runtime once");
    }

    private static void assertShutdownDuringWrite() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch aborted = new CountDownLatch(1);
        AtomicInteger closes = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        BlockingStream nativeStream = new FakeStream() {
            private volatile boolean inWrite;

            @Override
            public boolean write(float[] buffer, int frames) {
                inWrite = true;
                writing.countDown();
                await(aborted);
                inWrite = false;
                return false;
            }

            @Override
            public void abort() {
                aborted.countDown();
            }

            @Override
            public void close() {
                check(!inWrite, "Native stream must not be closed while writing");
                closes.incrementAndGet();
            }
        };
        var stream = new PortAudioDriver.StreamLifecycle(nativeStream, releases::incrementAndGet);
        Thread worker = new Thread(() -> stream.run(() -> stream.write(new float[32], 16)));
        worker.start();
        try {
            await(writing);
            stream.abort();
            worker.join(5000);
            check(!worker.isAlive(), "Aborted write must let the mixer exit");
            stream.close();
            check(closes.get() == 1 && releases.get() == 1, "Shutdown must clean up exactly once");
        } finally {
            aborted.countDown();
            worker.join(5000);
        }
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected stream failure");
        } catch (IllegalStateException expected) {
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            check(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for test stream");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class FakeStream extends BlockingStream {
        final List<String> events = new ArrayList<>();
        boolean failStart;
        boolean failWrite;
        boolean failAbort;
        boolean failClose;
        int availableQueries;

        @Override
        public boolean isStopped() { return false; }

        @Override
        public int getWriteAvailable() { availableQueries++; return 512; }

        @Override
        public void start() {
            events.add("start");
            if (failStart) throw new IllegalStateException("start failed");
        }

        @Override
        public boolean write(float[] buffer, int frames) {
            events.add("write");
            if (failWrite) throw new IllegalStateException("write failed");
            return false;
        }

        @Override
        public void abort() {
            events.add("abort");
            if (failAbort) throw new IllegalStateException("abort failed");
        }

        @Override
        public void close() {
            events.add("close");
            if (failClose) throw new IllegalStateException("close failed");
        }
    }
}
