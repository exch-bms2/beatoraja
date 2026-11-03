package bms.player.beatoraja.skin.lua;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.luaj.vm2.*;

/** Tests all Lua HTTP paths without sockets, external servers, or DNS lookups. */
public final class SkinHttpLimitsTest {
    private static final Map<String, byte[]> BODIES = new ConcurrentHashMap<>();
    private static final AtomicReference<FakeConnection> LAST = new AtomicReference<>();

    public static void main(String[] args) throws Exception {
        URL.setURLStreamHandlerFactory(protocol -> "http".equals(protocol) ? new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                FakeConnection connection = new FakeConnection(url, BODIES.getOrDefault(url.getPath(), new byte[0]));
                LAST.set(connection);
                return connection;
            }
            @Override protected URLConnection openConnection(URL url, Proxy proxy) { return openConnection(url); }
        } : null);
        testReader();
        testSynchronousAndLegacy();
        testAsyncLua();
        testCapacity();
        System.out.println("SkinHttpLimitsTest passed");
    }

    private static void testReader() throws Exception {
        check(SkinHttpBody.DEFAULT_MAX_BYTES == 1024 * 1024, "Default is 1 MiB");
        check(SkinHttpBody.MAX_BYTES == 8 * 1024 * 1024, "Hard limit is 8 MiB");
        for (long invalid : new long[] {0, -1, SkinHttpBody.MAX_BYTES + 1L, Long.MAX_VALUE}) {
            expectFailure(() -> SkinHttpBody.checkedLimit(invalid));
        }
        for (double invalid : new double[] {1.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            expectFailure(() -> SkinHttpBody.checkedLimit(invalid));
        }
        byte[] utf8 = "あ\n".getBytes(StandardCharsets.UTF_8);
        FakeConnection exact = fake(utf8);
        check(Arrays.equals(utf8, SkinHttpBody.read(exact, 4)), "UTF-8 and newlines count as bytes");
        check(exact.closed, "Input closed on success");
        FakeConnection overflow = fake(utf8);
        expectFailure(() -> SkinHttpBody.read(overflow, 3));
        check(overflow.closed && overflow.bytesRead == 4, "Unknown length checks limit + 1 and closes input");
        FakeConnection declared = fake(new byte[1024]);
        declared.declaredLength = 1024;
        expectFailure(() -> SkinHttpBody.read(declared, 32));
        check(!declared.opened, "Oversized Content-Length rejected before opening body");
        FakeConnection dishonest = fake(new byte[1024]);
        dishonest.declaredLength = 1;
        expectFailure(() -> SkinHttpBody.read(dishonest, 32));
        check(dishonest.bytesRead == 33, "Incorrect Content-Length cannot bypass streaming limit");
    }

    private static void testSynchronousAndLegacy() throws Exception {
        byte[] large = new byte[SkinHttpBody.DEFAULT_MAX_BYTES + 1];
        Arrays.fill(large, (byte) 'x');
        BODIES.put("/large", large);
        BODIES.put("/lines", "あ\n".repeat(4096).getBytes(StandardCharsets.UTF_8));
        BODIES.put("/maximum", new byte[SkinHttpBody.MAX_BYTES]);
        LuaTable sync = new LuaTable();
        new SkinHttpLuaApiExporter().export(sync);
        for (String name : new String[] {"http_get", "http_get_lines"}) {
            check(sync.get(name).call(LuaValue.valueOf("http://8.8.8.8/large")).isnil(), "Default limit: " + name);
            check(LAST.get().closed && LAST.get().disconnected, "Failed synchronous request closes resources");
            Varargs result = sync.get(name).invoke(LuaValue.varargsOf(new LuaValue[] {
                    LuaValue.valueOf("http://8.8.8.8/large"), LuaValue.NIL, LuaValue.valueOf(2 * 1024 * 1024)}));
            check(!result.arg1().isnil() && result.arg(2).toboolean(), "Explicit limit: " + name);
        }
        check(sync.get("http_get_lines").call(LuaValue.valueOf("http://8.8.8.8/lines")).length() == 4096,
                "No line limit for synchronous API");
        check(sync.get("http_get").call(LuaValue.valueOf("http://8.8.8.8/lines")).tojstring().startsWith("あ\n"), "UTF-8 retained");
        check(sync.get("http_get").call(LuaValue.valueOf("http://8.8.8.8/large"), LuaValue.valueOf(1000), LuaValue.valueOf(0)).isnil(),
                "Invalid requested capacity returns nil/error");

        var lua = new SkinLuaAccessor(false);
        lua.exec("local j=require('luajava'); local c=j.newInstance('java.net.URL','http://8.8.8.8/large'):openConnection(); "
                + "assert(not pcall(function() c:getInputStream() end)); "
                + "local d=j.newInstance('java.net.URL','http://8.8.8.8/large'):openConnection(); "
                + "d:setResponseLimitBytes(2*1024*1024); local r=d:getInputStream(); assert(#r:readLine()==1024*1024+1); "
                + "r:close(); assert(r:readLine()==nil); "
                + "local e=j.newInstance('java.net.URL','http://8.8.8.8/lines'):openConnection(); local l=e:getInputStream(); "
                + "local n=0; while l:readLine() do n=n+1 end; assert(n==4096); "
                + "assert(not pcall(function() e:setResponseLimitBytes(8*1024*1024+1) end))");
        lua.dispose();
        FakeConnection max = fake(BODIES.get("/maximum"));
        check(SkinHttpBody.read(max, SkinHttpBody.MAX_BYTES).length == SkinHttpBody.MAX_BYTES, "Exact hard limit accepted");
    }

    private static void testAsyncLua() throws Exception {
        var http = new SkinAsyncHttp();
        var runtime = new SkinRuntimeLuaApiExporter(true, http, () -> false, uri -> { });
        LuaTable api = new LuaTable();
        runtime.export(api);
        int id = api.get("http_get_async").call(LuaValue.valueOf("http://8.8.8.8/large")).checkint();
        check(await(http, id).status().equals("failed"), "Async default limit");
        id = api.get("http_get_async").call(LuaValue.valueOf("http://8.8.8.8/large"), LuaValue.NIL,
                LuaValue.valueOf(2 * 1024 * 1024)).checkint();
        check(await(http, id).body().length == SkinHttpBody.DEFAULT_MAX_BYTES + 1, "Async custom limit");
        id = api.get("http_get_async").call(LuaValue.valueOf("http://8.8.8.8/lines")).checkint();
        LuaValue result;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            result = api.get("http_poll").call(LuaValue.valueOf(id));
            if (!result.get("status").tojstring().equals("pending")) break;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        check(result.get("lines").length() == 4096, "Async Lua conversion has no line limit");
        check(api.get("http_get_async").call(LuaValue.valueOf("http://8.8.8.8/lines"), LuaValue.NIL,
                LuaValue.valueOf(SkinHttpBody.MAX_BYTES + 1)).isnil(), "Async hard limit enforced");
        runtime.dispose();
    }

    private static void testCapacity() throws Exception {
        int max = SkinHttpBody.MAX_BYTES;
        var big = new SkinAsyncHttp((url, timeout, job) -> new SkinAsyncHttp.Result("completed", 200, new byte[max], ""));
        int first = big.submit("http://8.8.8.8", 5000, max);
        int second = big.submit("http://8.8.8.8", 5000, max);
        check(big.reservedBytes() == SkinHttpBody.MAX_RETAINED_BYTES, "Running and retained bodies reserve 16 MiB");
        expectFailure(() -> big.submit("http://8.8.8.8", 5000, 1));
        check(await(big, first).body().length == max, "First result consumed");
        check(big.reservedBytes() == max, "Poll releases retained capacity");
        int third = big.submit("http://8.8.8.8", 5000, max);
        check(await(big, second).body().length == max, "Second result consumed");
        check(await(big, third).body().length == max, "Capacity can be reused");
        check(big.reservedBytes() == 0, "No retained bytes after polling");
        big.submit("http://8.8.8.8", 5000, max);
        big.dispose();
        awaitReleased(big);

        var release = new CountDownLatch(1);
        var entered = new CountDownLatch(1);
        var cancelled = new SkinAsyncHttp((url, timeout, job) -> {
            entered.countDown();
            // Deliberately ignore cancellation until released, like non-interruptible native I/O.
            while (true) { try { release.await(); break; } catch (InterruptedException ignored) { } }
            return new SkinAsyncHttp.Result("completed", 200, new byte[1], "");
        });
        first = cancelled.submit("http://8.8.8.8", 5000, max);
        check(entered.await(1, TimeUnit.SECONDS), "Worker entered");
        check(cancelled.cancel(first), "Cancel succeeds");
        check(cancelled.reservedBytes() == max, "Running cancelled job keeps its reservation until it stops");
        release.countDown();
        awaitReleased(cancelled);
        cancelled.dispose();

        var failed = new SkinAsyncHttp((url, timeout, job) -> { throw new IOException("test failure"); });
        first = failed.submit("http://8.8.8.8", 5000, max);
        check(await(failed, first).status().equals("failed"), "Failed job reported");
        awaitReleased(failed);
        failed.dispose();

        var small = new SkinAsyncHttp((url, timeout, job) -> new SkinAsyncHttp.Result("completed", 200, new byte[7], ""));
        first = small.submit("http://8.8.8.8", 5000, max);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (small.reservedBytes() != 7 && System.nanoTime() < deadline) Thread.sleep(1);
        check(small.reservedBytes() == 7, "Completed responses shrink reservations to their actual byte size");
        check(await(small, first).body().length == 7, "Small result consumed");
        awaitReleased(small);
        small.dispose();

        var expired = new SkinAsyncHttp((url, timeout, job) -> {
            new CountDownLatch(1).await();
            throw new AssertionError("Expected deadline interruption");
        });
        first = expired.submit("http://8.8.8.8", 30, max);
        check(await(expired, first).status().equals("failed"), "Deadline failure reported");
        awaitReleased(expired);
        expired.dispose();
    }

    private static SkinAsyncHttp.Result await(SkinAsyncHttp http, int id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        do {
            SkinAsyncHttp.Result result = http.poll(id);
            if (!result.status().equals("pending")) return result;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Request did not finish");
    }

    private static void awaitReleased(SkinAsyncHttp http) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (http.reservedBytes() != 0 && System.nanoTime() < deadline) Thread.sleep(1);
        check(http.reservedBytes() == 0, "Capacity released after worker exit");
    }

    private static FakeConnection fake(byte[] bytes) throws Exception { return new FakeConnection(new URI("http://8.8.8.8").toURL(), bytes); }
    private static final class FakeConnection extends HttpURLConnection {
        private final byte[] bytes;
        private long declaredLength = -1;
        private int bytesRead;
        private boolean opened, closed, disconnected;
        FakeConnection(URL url, byte[] bytes) { super(url); this.bytes = bytes; }
        @Override public long getContentLengthLong() { return declaredLength; }
        @Override public int getResponseCode() { return 200; }
        @Override public InputStream getInputStream() {
            opened = true;
            return new ByteArrayInputStream(bytes) {
                @Override public synchronized int read(byte[] b, int off, int len) {
                    int count = super.read(b, off, len);
                    if (count > 0) bytesRead += count;
                    return count;
                }
                @Override public void close() { closed = true; }
            };
        }
        @Override public void connect() { }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void expectFailure(Action action) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("Expected failure");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
