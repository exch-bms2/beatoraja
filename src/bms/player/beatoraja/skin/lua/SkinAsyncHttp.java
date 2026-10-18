package bms.player.beatoraja.skin.lua;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import com.badlogic.gdx.utils.Disposable;

/** Bounded HTTP jobs. Workers only produce Java data; Lua polls on its own thread. */
final class SkinAsyncHttp implements Disposable {
    static final int MAX_REQUESTS = 16;
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), runnable -> daemon(runnable, "Skin HTTP"));
    private static final ScheduledThreadPoolExecutor TIMER = timer();
    private static final ThreadPoolExecutor CLEANUP = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), runnable -> daemon(runnable, "Skin HTTP cleanup"),
            new ThreadPoolExecutor.DiscardPolicy());

    record Result(String status, int statusCode, byte[] body, String error) {
        static Result failed(String error) { return new Result("failed", 0, new byte[0], error); }
    }
    @FunctionalInterface interface Transport { Result get(String url, int timeout, Job job) throws Exception; }
    @FunctionalInterface interface ConnectionFactory { HttpURLConnection open(URI uri) throws Exception; }

    private final Map<Integer, Job> jobs = new HashMap<>();
    private final Transport transport;
    private final Capacity capacity = new Capacity();
    private int nextId;
    private boolean disposed;

    SkinAsyncHttp() { this(SkinAsyncHttp::download); }
    SkinAsyncHttp(Transport transport) { this.transport = transport; }

    synchronized int submit(String url, int timeout) {
        return submit(url, timeout, SkinHttpBody.DEFAULT_MAX_BYTES);
    }

    synchronized int submit(String url, int timeout, long requestedMaxBytes) {
        if (disposed || jobs.size() >= MAX_REQUESTS) throw new IllegalStateException("HTTP request limit reached or skin disposed");
        validateUrl(url);
        int maxBytes = SkinHttpBody.checkedLimit(requestedMaxBytes);
        capacity.reserve(maxBytes);
        int boundedTimeout = Math.max(1, Math.min(5000, timeout));
        do { nextId = nextId == Integer.MAX_VALUE ? 1 : nextId + 1; } while (jobs.containsKey(nextId));
        Job job = new Job(capacity, maxBytes);
        int id = nextId;
        jobs.put(id, job);
        try {
            synchronized (job) {
                job.task = WORKERS.submit(() -> {
                    if (!job.start()) return;
                    try { job.complete(transport.get(url, boundedTimeout, job)); }
                    catch (Exception e) { job.complete(Result.failed(e.getMessage() != null ? e.getMessage() : "HTTP request failed")); }
                    finally { job.complete(Result.failed("HTTP worker stopped")); }
                });
                job.deadline = TIMER.schedule(() -> job.cancel("HTTP request timed out"), boundedTimeout, TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException e) {
            jobs.remove(id);
            job.cancel("HTTP queue full");
            job.discard();
            throw new IllegalStateException("HTTP queue full", e);
        }
        return id;
    }

    synchronized Result poll(int id) {
        Job job = jobs.get(id);
        if (job == null) return Result.failed("Unknown or consumed HTTP request");
        Result result = job.result;
        if (result == null) return new Result("pending", 0, new byte[0], "");
        jobs.remove(id);
        job.discard();
        return result;
    }

    synchronized boolean cancel(int id) {
        Job job = jobs.remove(id);
        if (job == null) return false;
        job.cancel("HTTP request cancelled");
        job.discard();
        return true;
    }

    @Override public synchronized void dispose() {
        disposed = true;
        for (Job job : jobs.values()) {
            job.cancel("Skin disposed");
            job.discard();
        }
        jobs.clear();
    }

    int reservedBytes() { return capacity.used(); }

    static URI validateUrl(String text) {
        if (text == null || text.length() > 2048) throw new IllegalArgumentException("Invalid HTTP URL length");
        URI uri = URI.create(text);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("Only absolute HTTP/HTTPS URLs without credentials are allowed");
        }
        return uri;
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        int a = bytes[0] & 255, b = bytes[1] & 255;
        if (bytes.length == 4) {
            return a != 0 && a != 10 && a != 127 && a < 224
                    && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                    && !(a == 192 && (b == 0 || b == 168))
                    && !(a == 198 && (b == 18 || b == 19 || b == 51))
                    && !(a == 203 && b == 0 && (bytes[2] & 255) == 113);
        }
        // Only global unicast, excluding transition and documentation ranges.
        return (a & 0xe0) == 0x20 && !(a == 0x20 && b == 0x02)
                && !(a == 0x20 && b == 0x01 && ((bytes[2] & 255) < 2
                || ((bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8)));
    }

    private static Result download(String text, int timeout, Job job) throws Exception {
        return download(text, timeout, job, uri -> {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublicAddress(address)) throw new IOException("Non-public HTTP destination denied");
            }
            return (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        });
    }

    static Result download(String text, int timeout, Job job, ConnectionFactory connections) throws Exception {
        URI uri = validateUrl(text);
        for (int redirects = 0; redirects <= 3; redirects++) {
            HttpURLConnection connection = connections.open(uri);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            job.attach(connection);
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects == 3) throw new IOException("Invalid or excessive HTTP redirects");
                    URI next = validateUrl(uri.resolve(location).toString());
                    if ("https".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(next.getScheme())) {
                        throw new IOException("HTTPS downgrade denied");
                    }
                    uri = next;
                    continue;
                }
                if (status < 200 || status >= 300) return new Result("failed", status, new byte[0], "HTTP status " + status);
                return new Result("completed", status, SkinHttpBody.read(connection, job.maxBytes), "");
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many HTTP redirects");
    }

    static final class Job {
        private final Capacity capacity;
        private final int maxBytes;
        private int reserved;
        private boolean started;
        private boolean finished;
        private boolean discarded;
        private volatile Result result;
        private Future<?> task;
        private ScheduledFuture<?> deadline;
        private HttpURLConnection connection;

        Job() { this(null, SkinHttpBody.DEFAULT_MAX_BYTES); }
        private Job(Capacity capacity, int maxBytes) {
            this.capacity = capacity;
            this.maxBytes = maxBytes;
            this.reserved = capacity == null ? 0 : maxBytes;
        }
        synchronized boolean start() {
            if (result != null || discarded) return false;
            started = true;
            return true;
        }
        private void shrinkReservation(int bytes) {
            if (capacity != null) capacity.release(reserved - bytes);
            reserved = bytes;
        }
        synchronized void discard() {
            discarded = true;
            if (finished || !started) shrinkReservation(0);
            result = null;
        }

        synchronized void attach(HttpURLConnection connection) throws IOException {
            if (result != null || discarded || Thread.currentThread().isInterrupted()) {
                connection.disconnect();
                throw new IOException("HTTP request cancelled");
            }
            this.connection = connection;
        }
        synchronized void complete(Result value) {
            if (finished) return;
            finished = true;
            if (value.body().length > maxBytes) value = Result.failed("HTTP response too large");
            if (!discarded && result == null) {
                result = value;
                shrinkReservation(value.body().length);
            } else {
                shrinkReservation(0);
            }
            if (deadline != null) deadline.cancel(false);
            connection = null;
        }
        void cancel(String error) {
            HttpURLConnection active;
            synchronized (this) {
                if (result != null) return;
                result = Result.failed(error);
                if (!started) shrinkReservation(0);
                if (task != null) {
                    task.cancel(true);
                    if (task instanceof Runnable runnable) WORKERS.remove(runnable);
                }
                if (deadline != null) deadline.cancel(false);
                active = connection;
                connection = null;
            }
            // Some JDK connections synchronize disconnect with a pending read/connect.
            // Never block the render thread or the deadline scheduler on native I/O.
            // If cleanup is full, the worker still closes in finally after its socket timeout.
            if (active != null) CLEANUP.execute(active::disconnect);
        }
    }

    /** Raw-body budget, including reservations for running/queued requests. No job locks are acquired here. */
    private static final class Capacity {
        private int bytes;
        synchronized void reserve(int count) {
            if (bytes + count > SkinHttpBody.MAX_RETAINED_BYTES) {
                throw new IllegalStateException("HTTP retained response capacity exceeded: limit=" + SkinHttpBody.MAX_RETAINED_BYTES);
            }
            bytes += count;
        }
        synchronized void release(int count) { bytes -= count; }
        synchronized int used() { return bytes; }
    }

    private static Thread daemon(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
    private static ScheduledThreadPoolExecutor timer() {
        var executor = new ScheduledThreadPoolExecutor(1, runnable -> daemon(runnable, "Skin HTTP deadlines"));
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
