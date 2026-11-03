package bms.player.beatoraja.external;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;

/** No user clipboard, browser or document is modified by these tests. */
public final class DesktopActionsTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--mac-image-smoke")) {
            macImageSmoke();
            return;
        }
        testGdxActions();
        testMacArguments();
        testProcessLifecycle();
        testNoDirectAwtReferences();
        check(!DesktopActions.openPath(null), "Null path returns failure");
        check(!DesktopActions.copyImage(Path.of("missing-screenshot.png")), "Missing image returns failure");
        System.out.println("DesktopActionsTest passed");
    }

    private static void testGdxActions() {
        var oldApp = Gdx.app;
        var oldNet = Gdx.net;
        var copied = new AtomicReference<String>();
        var opened = new AtomicReference<String>();
        try {
            Clipboard clipboard = new Clipboard() {
                @Override public String getContents() { return copied.get(); }
                @Override public boolean hasContents() { return copied.get() != null; }
                @Override public void setContents(String value) { copied.set(value); }
            };
            Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
                    new Class<?>[] {Application.class}, (p, method, values) -> {
                        if (method.getName().equals("getClipboard")) return clipboard;
                        throw new AssertionError(method.getName());
                    });
            Gdx.net = (Net) Proxy.newProxyInstance(Net.class.getClassLoader(), new Class<?>[] {Net.class},
                    (p, method, values) -> {
                        check(method.getName().equals("openURI"), "Use libGDX browser API");
                        opened.set((String) values[0]);
                        return !values[0].toString().contains("failure");
                    });
            String text = "MD5 abc\n\u697d\u66f2 \"quoted\"";
            check(DesktopActions.copyText(text) && text.equals(copied.get()), "Unicode text is unchanged");
            String url = "https://example.com/music?q=%E6%9B%B2&value=1";
            check(DesktopActions.openUri(url) && url.equals(opened.get()), "URL passed unchanged");
            String[][] bracketUrls = {
                {"https://example.com/[event]/song[ANOTHER].zip", "https://example.com/%5Bevent%5D/song%5BANOTHER%5D.zip"},
                {"http://example.com/song].zip", "http://example.com/song%5D.zip"},
                {"https://example.com/[song.zip", "https://example.com/%5Bsong.zip"},
                {"https://[2001:db8::1]:443/[diff].zip", "https://[2001:db8::1]:443/%5Bdiff%5D.zip"},
                {"https://[2001:db8::1]/song.zip", "https://[2001:db8::1]/song.zip"},
                {"HTTPS://example.com:8443/[song].zip?signature=ab%2Fcd%3D&x[]=1#part[1]", "HTTPS://example.com:8443/%5Bsong%5D.zip?signature=ab%2Fcd%3D&x[]=1#part[1]"},
                {"https://example.com?next=/[song].zip", "https://example.com?next=/[song].zip"},
                {"https://example.com#/[song].zip", "https://example.com#/[song].zip"},
                {"https://example.com/song%5bANOTHER%5d.zip?x[]=1", "https://example.com/song%5bANOTHER%5d.zip?x[]=1"},
                {"https://example.com/[song]%5Bv2%5D.zip", "https://example.com/%5Bsong%5D%5Bv2%5D.zip"}
            };
            for (String[] pair : bracketUrls) {
                var target = DesktopActions.browserUri(pair[0]);
                check(pair[1].equals(target.toString()), "Encode path brackets only: " + pair[0]);
                check(DesktopActions.browserUri(target.toString()).equals(target), "Browser encoding is idempotent");
                check(DesktopActions.openUri(pair[0]) && pair[1].equals(opened.get()), "libGDX receives browser-safe URL");
                check(DesktopActions.macUriCommand(target).equals(List.of("/usr/bin/open", pair[1])), "macOS receives the same browser-safe URL");
            }
            check(!DesktopActions.openUri("https://failure.example.com"), "Backend rejection returns false");
            opened.set(null);
            for (String invalid : new String[] {"-aCalculator", "file:///tmp/test", "javascript:alert(1)", "http://[", "",
                    "https://ex[ample].com/[song].zip", "https://example.com/[song] name.zip",
                    "https://example.com/[song]\r\nInjected.zip", "https://example.com/[song]%ZZ.zip"})
                check(!DesktopActions.openUri(invalid), "Reject unsafe/invalid browser URL: " + invalid);
            check(opened.get() == null, "Rejected URLs never reach native launcher");
            Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
                    new Class<?>[] {Application.class}, (p, method, values) -> { throw new IllegalStateException("Unavailable"); });
            check(!DesktopActions.copyText("test"), "Clipboard exception is contained");
            Gdx.app = null;
            Gdx.net = null;
            check(!DesktopActions.copyText("test") && !DesktopActions.copyText(null), "Missing clipboard is contained");
            check(!DesktopActions.openUri(null), "Missing URL is contained without launching a helper");
        } finally {
            Gdx.app = oldApp;
            Gdx.net = oldNet;
        }
    }

    private static void testMacArguments() {
        Path path = Path.of("-file \u697d\u66f2 \"quoted\" 'single' ; $(touch unexpected)\n.png");
        String absolute = path.toAbsolutePath().normalize().toString();
        var open = DesktopActions.macOpenCommand(path);
        check(open.equals(List.of("/usr/bin/open", absolute)), "Absolute path is one argument, not an option/shell command");
        var url = URI.create("https://example.com/?a=1&b=%22quoted%22");
        check(DesktopActions.macUriCommand(url).equals(List.of("/usr/bin/open", url.toString())),
                "Launcher URL passed as a single argument without a shell");
        var image = DesktopActions.macImageCommand(path);
        check(image.size() == 6 && image.get(0).equals("/usr/bin/osascript")
                && image.get(5).equals(absolute), "Image path passed separately");
        check(image.get(4).equals(DesktopActions.MAC_IMAGE_SCRIPT) && !image.get(4).contains(absolute),
                "No script interpolation");
        check(image.get(4).contains("NSPasteboardTypePNG") && image.get(4).contains("NSPasteboardTypeTIFF"),
                "Publish image data, not the filename");
    }

    private static void testProcessLifecycle() throws Exception {
        var command = List.of("test-helper", "one argument");
        var success = new StubProcess(0, true, false);
        DesktopActions.runCommand(command, 20, args -> {
            check(args.equals(command), "Preserve command arguments");
            return success;
        });
        check(success.stdinClosed && !success.destroyed, "Close stdin and leave exited process alone");
        expectFailure(new StubProcess(1, true, false), false);
        expectFailure(new StubProcess(0, false, false), true);
        var interrupted = new StubProcess(0, false, true);
        try {
            DesktopActions.runCommand(command, 20, args -> interrupted);
            throw new AssertionError("Expected interruption");
        } catch (InterruptedException expected) {
            check(interrupted.destroyed, "Interrupted helper is terminated");
        }
        try {
            DesktopActions.runCommand(command, 20, args -> { throw new IOException("No executable"); });
            throw new AssertionError("Expected start failure");
        } catch (IOException expected) {
            check(expected.getMessage().equals("No executable"), "Start failure propagated to action boundary");
        }
    }

    private static void expectFailure(StubProcess process, boolean destroyed) throws Exception {
        try {
            DesktopActions.runCommand(List.of("test-helper"), 20, args -> process);
            throw new AssertionError("Expected helper failure");
        } catch (IOException expected) {
            check(process.destroyed == destroyed, "Timeout terminates live helper; exited helper needs no termination");
        }
    }

    private static void testNoDirectAwtReferences() throws Exception {
        for (String name : List.of("external/DesktopActions", "external/ScreenShotFileExporter",
                "select/MusicSelectCommand", "skin/property/EventFactory", "skin/lua/SkinRuntimeLuaApiExporter",
                "launcher/PlayConfigurationView", "launcher/IRConfigurationView", "launcher/TableEditorView")) {
            try (var in = DesktopActions.class.getResourceAsStream("/bms/player/beatoraja/" + name + ".class")) {
                check(in != null && !new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains("java/awt/"),
                        "No direct AWT reference: " + name);
            }
        }
    }

    /** Real AppKit image roundtrip on a private pasteboard, not the user's general pasteboard. */
    public static void macImageSmoke() throws Exception {
        check(System.getProperty("os.name").startsWith("Mac"), "macOS only");
        GdxNativesLoader.load();
        Path directory = Files.createTempDirectory("clipboard-image-");
        Path png = directory.resolve("image \u697d\u66f2 \"quote\" ' ; $test.png");
        Pixmap pixels = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        try {
            pixels.setColor(0, 1, 0, 1);
            pixels.fill();
            PixmapIO.writePNG(new FileHandle(png.toFile()), pixels);
            String script = DesktopActions.MAC_IMAGE_SCRIPT.replace("$.NSPasteboard.generalPasteboard",
                    "$.NSPasteboard.pasteboardWithName($('beatoraja.test.' + $.NSUUID.UUID.UUIDString.js))")
                    .replace("throw new Error('Clipboard write failed');", """
                            throw new Error('Clipboard write failed');
                        if (!board.dataForType($.NSPasteboardTypePNG).isEqualToData(data))
                            throw new Error('PNG roundtrip mismatch');
                        var decoded = $.NSBitmapImageRep.imageRepWithData(board.dataForType($.NSPasteboardTypeTIFF));
                        if (decoded.isNil() || decoded.pixelsWide != 2 || decoded.pixelsHigh != 2)
                            throw new Error('TIFF dimensions mismatch');
                        board.releaseGlobally;
                        """);
            var command = new ArrayList<>(DesktopActions.macImageCommand(png));
            command.set(4, script);
            DesktopActions.runCommand(command, 10_000, args -> new ProcessBuilder(args).inheritIO().start());
        } finally {
            pixels.dispose();
            Files.deleteIfExists(png);
            Files.delete(directory);
        }
        System.out.println("Desktop image smoke passed (AppKit PNG/TIFF roundtrip; user clipboard unchanged)");
    }

    private static final class StubProcess extends Process {
        private final int exit;
        private final boolean completes;
        private final boolean interrupts;
        private boolean alive = true;
        boolean destroyed;
        boolean stdinClosed;
        StubProcess(int exit, boolean completes, boolean interrupts) {
            this.exit = exit;
            this.completes = completes;
            this.interrupts = interrupts;
        }
        @Override public OutputStream getOutputStream() {
            return new ByteArrayOutputStream() {
                @Override public void close() { stdinClosed = true; }
            };
        }
        @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() { alive = false; return exit; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (interrupts) throw new InterruptedException();
            if (completes) alive = false;
            return completes;
        }
        @Override public int exitValue() { return exit; }
        @Override public void destroy() { destroyed = true; alive = false; }
        @Override public Process destroyForcibly() { destroy(); return this; }
        @Override public boolean isAlive() { return alive; }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
