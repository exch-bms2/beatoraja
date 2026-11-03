package bms.player.beatoraja.skin.lua;

import java.io.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import org.luaj.vm2.*;

/** Native-, browser- and external-service-free Lua compatibility tests. */
public final class SkinCompatibilityApiTest {
    public static void main(String[] args) throws Exception {
        testFilesAndInput();
        testRuntime();
        testHttpTransport();
        testHttpLifecycle();
        System.out.println("SkinCompatibilityApiTest passed");
    }

    private static void testFilesAndInput() throws Exception {
        Path root = Files.createTempDirectory("skin-api");
        Path outside = Files.createTempDirectory("skin-outside");
        Input original = Gdx.input;
        try {
            Files.writeString(root.resolve("file.txt"), "test");
            Files.writeString(outside.resolve("keep.txt"), "keep");
            Files.createDirectory(root.resolve("directory"));
            var accessor = new SkinLuaAccessor(false);
            accessor.setDirectory(root);
            accessor.exec("local j=require('luajava'); local f=j.newInstance('java.io.File','file.txt'); "
                    + "assert(f:isFile()); assert(not f:isDirectory()); assert(f:getName()=='file.txt'); "
                    + "assert(tostring(f)==f:getPath()); local d=j.new(j.bindClass('java.io.File'),'.'); "
                    + "assert(d:isDirectory()); local found=false; for _,v in ipairs(d:listFiles()) do "
                    + "if v:getName()=='file.txt' then found=v:isFile() end end; assert(found); assert(d:mkdir()==false)");
            LuaTable files = new LuaTable();
            new SkinFileLuaApiExporter(new SkinLuaPathResolver(() -> root)).export(files);
            check(!files.get("file_delete").call(LuaValue.valueOf("directory")).toboolean(), "Must not delete directories");
            check(!files.get("file_delete").call(LuaValue.valueOf(outside.resolve("keep.txt").toString())).toboolean(), "Must reject outside files");
            boolean linksSupported = true;
            try {
                Files.createSymbolicLink(root.resolve("link"), outside);
                Files.createSymbolicLink(root.resolve("file-link"), outside.resolve("keep.txt"));
                Files.createSymbolicLink(root.resolve("dangling"), outside.resolve("missing"));
            } catch (IOException | UnsupportedOperationException e) {
                linksSupported = false;
                System.out.println("Symbolic link tests skipped: " + e.getMessage());
            }
            if (linksSupported) {
                for (String path : List.of("link/keep.txt", "file-link", "dangling", "../keep.txt")) {
                    check(!files.get("file_delete").call(LuaValue.valueOf(path)).toboolean(), "Must reject link/traversal: " + path);
                }
            }
            LuaTable preview = new LuaTable();
            new SkinFileLuaApiExporter(new SkinLuaPathResolver(() -> root), false).export(preview);
            check(!preview.get("file_delete").call(LuaValue.valueOf("file.txt")).toboolean(), "Preview must not delete");
            check(files.get("file_delete").call(LuaValue.valueOf("file.txt")).toboolean(), "Delete a regular file");
            check(!files.get("file_delete").call(LuaValue.valueOf("file.txt")).toboolean(), "Missing file returns false");
            check(Files.exists(outside.resolve("keep.txt")), "Outside file must remain");
            var previewAccessor = new SkinLuaAccessor(true, root);
            previewAccessor.exportMainStateAccessor(null);
            previewAccessor.exec("assert(not file_delete('file.txt')); assert(not open_url('https://example.com')); "
                    + "assert(http_get_async('https://example.com')==nil)");
            previewAccessor.dispose();

            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class}, (proxy, method, arguments) -> {
                return switch (method.getName()) {
                    case "isKeyPressed", "isKeyJustPressed", "isButtonPressed" -> true;
                    case "getX" -> 123;
                    case "getY" -> 456;
                    case "getDeltaX" -> -3;
                    case "getDeltaY" -> 4;
                    default -> throw new AssertionError("Unexpected input access: " + method.getName());
                };
            });
            accessor.exec("local j=require('luajava'); local g=j.bindClass('com.badlogic.gdx.Gdx'); local i=j.bindClass('com.badlogic.gdx.Input'); "
                    + "assert(g.input:isButtonPressed(i.Buttons.LEFT),'button'); assert(g.input:isKeyJustPressed(i.Keys.UP),'key'); "
                    + "assert(g.input.getX()==123 and g.input:getY()==456,'position'); assert(g.input:getDeltaX()==-3 and g.input:getDeltaY()==4,'delta'); "
                    + "assert(not g.input:isButtonPressed(999),'invalid button'); assert(not g.input:isKeyJustPressed(-1),'invalid key'); assert(j.createProxy==nil,'proxy')");
            Gdx.input = null;
            accessor.exec("local g=require('luajava').bindClass('com.badlogic.gdx.Gdx'); "
                    + "assert(not g.input:isKeyJustPressed(1)); assert(not g.input:isButtonPressed(0)); assert(g.input:getX()==0)");
            accessor.dispose();
        } finally {
            Gdx.input = original;
            deleteTree(root);
            deleteTree(outside);
        }
    }

    private static void testRuntime() throws Exception {
        var opened = new AtomicReference<URI>();
        var action = new AtomicBoolean(false);
        var http = new SkinAsyncHttp((url, timeout, job) -> new SkinAsyncHttp.Result("completed", 200,
                "one\ntwo".getBytes(java.nio.charset.StandardCharsets.UTF_8), ""));
        var runtime = new SkinRuntimeLuaApiExporter(true, http, action::get, opened::set);
        LuaTable api = new LuaTable();
        runtime.export(api);
        check(!api.get("open_url").call(LuaValue.valueOf("https://example.com")).toboolean(), "Browser requires a user action");
        action.set(true);
        check(!api.get("open_url").call(LuaValue.valueOf("file:///tmp/file")).toboolean(), "No file URLs");
        check(api.get("open_url").call(LuaValue.valueOf("https://example.com")).toboolean(), "Open HTTP URL");
        check(opened.get().getHost().equals("example.com"), "Correct browser URL");
        check(!api.get("open_url").call(LuaValue.valueOf("https://example.com")).toboolean(), "Browser launch rate limit");
        int id = api.get("http_get_async").call(LuaValue.valueOf("https://example.com")).checkint();
        LuaValue response;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        do {
            response = api.get("http_poll").call(LuaValue.valueOf(id));
            if (!response.get("status").tojstring().equals("pending")) break;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        check(response.get("status").tojstring().equals("completed") && response.get("lines").length() == 2, "Lua HTTP result");
        check(api.get("http_poll").call(LuaValue.valueOf(id)).get("status").tojstring().equals("failed"), "Result consumed once");
        runtime.dispose();
        check(api.get("http_get_async").call(LuaValue.valueOf("https://example.com")).isnil(), "Disposed API must reject jobs");
        var sandbox = new SkinRuntimeLuaApiExporter(false, new SkinAsyncHttp(), () -> true, uri -> { throw new AssertionError(); });
        sandbox.export(api);
        check(!api.get("open_url").call(LuaValue.valueOf("https://example.com")).toboolean(), "Sandbox browser disabled");
        check(api.get("http_get_async").call(LuaValue.valueOf("https://example.com")).isnil(), "Sandbox HTTP disabled");
        sandbox.dispose();
        var unavailableBrowser = new SkinRuntimeLuaApiExporter(true, new SkinAsyncHttp(), () -> true,
                uri -> { throw new IllegalStateException("Browser unavailable"); });
        unavailableBrowser.export(api);
        check(!api.get("open_url").call(LuaValue.valueOf("https://example.com")).toboolean(),
                "Browser failures return false without escaping into Lua");
        unavailableBrowser.dispose();
    }

    private static void testHttpTransport() throws Exception {
        for (String invalid : List.of("file:///etc/passwd", "jar:https://example.com/x", "http://user:pass@example.com", "/relative")) {
            expectFailure(() -> SkinAsyncHttp.validateUrl(invalid));
        }
        for (String privateIp : List.of("127.0.0.1", "10.0.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "::1", "fc00::1", "fe80::1", "::ffff:127.0.0.1")) {
            check(!SkinAsyncHttp.isPublicAddress(InetAddress.getByName(privateIp)), "Private address: " + privateIp);
        }
        check(SkinAsyncHttp.isPublicAddress(InetAddress.getByName("8.8.8.8")), "Public IPv4");
        check(SkinAsyncHttp.isPublicAddress(InetAddress.getByName("2606:4700:4700::1111")), "Public IPv6");
        var good = new FakeConnection(200, "one\ntwo");
        var response = SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> good);
        check(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8).equals("one\ntwo") && good.disconnected, "Read and disconnect");
        var large = new FakeConnection(200, "x".repeat(SkinHttpBody.DEFAULT_MAX_BYTES + 1));
        expectFailure(() -> SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> large));
        check(large.disconnected, "Oversized body disconnected");
        var lines = new FakeConnection(200, "x\n".repeat(4096));
        check(new String(SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> lines).body(),
                java.nio.charset.StandardCharsets.UTF_8).lines().count() == 4096,
                "Responses must not be limited by line count");
        var redirect = new FakeConnection(302, "");
        redirect.location = "file:///etc/passwd";
        expectFailure(() -> SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> redirect));
        redirect.location = "http://example.com";
        expectFailure(() -> SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> redirect));
        redirect.location = "/loop";
        expectFailure(() -> SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> redirect));
        var missing = new FakeConnection(404, "");
        check(SkinAsyncHttp.download("https://example.com", 1000, new SkinAsyncHttp.Job(), uri -> missing).statusCode() == 404, "HTTP status retained");
    }

    private static void testHttpLifecycle() throws Exception {
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        SkinAsyncHttp.Transport blocking = (url, timeout, job) -> {
            entered.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException e) { interrupted.countDown(); throw e; }
            throw new AssertionError();
        };
        var http = new SkinAsyncHttp(blocking);
        int id = http.submit("https://example.com", 5000);
        check(entered.await(1, TimeUnit.SECONDS), "Worker started");
        check(http.poll(id).status().equals("pending"), "Pending request");
        http.dispose();
        check(interrupted.await(1, TimeUnit.SECONDS), "Dispose interrupts the worker");
        check(http.poll(id).status().equals("failed"), "Dispose clears retained results");
        expectFailure(() -> http.submit("https://example.com", 1000));
        var timed = new SkinAsyncHttp(blocking);
        id = timed.submit("https://example.com", 40);
        check(await(timed, id).status().equals("failed"), "Overall deadline enforced");
        timed.dispose();
        var capped = new SkinAsyncHttp((url, timeout, job) -> new SkinAsyncHttp.Result("completed", 200, new byte[0], ""));
        for (int i = 0; i < SkinAsyncHttp.MAX_REQUESTS; i++) capped.submit("https://example.com", 5000);
        expectFailure(() -> capped.submit("https://example.com", 1000));
        check(capped.cancel(1), "Cancellation releases a slot");
        capped.submit("https://example.com", 1000);
        capped.dispose();
        var denied = new SkinAsyncHttp();
        check(await(denied, denied.submit("http://127.0.0.1:1", 1000)).error().contains("Non-public"), "Production transport blocks loopback");
        denied.dispose();
    }

    private static SkinAsyncHttp.Result await(SkinAsyncHttp http, int id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        SkinAsyncHttp.Result result;
        do {
            result = http.poll(id);
            if (!result.status().equals("pending")) return result;
            Thread.sleep(1);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Timed out waiting for test request");
    }

    private static final class FakeConnection extends HttpURLConnection {
        private final int status;
        private final byte[] body;
        private String location;
        private boolean disconnected;
        FakeConnection(int status, String body) throws Exception {
            super(URI.create("https://example.com").toURL());
            this.status = status;
            this.body = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        @Override public int getResponseCode() { return status; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body); }
        @Override public String getHeaderField(String name) { return "Location".equals(name) ? location : null; }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
    }

    private static void expectFailure(CheckedRunnable action) throws Exception {
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("Expected failure");
    }
    @FunctionalInterface private interface CheckedRunnable { void run() throws Exception; }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void deleteTree(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
