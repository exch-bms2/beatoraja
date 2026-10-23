package bms.player.beatoraja.external;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;

/** Desktop integration. Clipboard text operations must run on the game thread. */
public final class DesktopActions {
    private DesktopActions() {}

    // AppKit runs outside the first-thread GLFW JVM; paths are arguments, never script source.
    static final String MAC_IMAGE_SCRIPT = """
            ObjC.import('AppKit');
            function run(argv) {
                var data = $.NSData.dataWithContentsOfFile($(argv[0]));
                if (!data || data.isNil()) throw new Error('Cannot read screenshot');
                var image = $.NSImage.alloc.initWithData(data);
                if (!image || image.isNil()) throw new Error('Invalid screenshot');
                var tiff = image.TIFFRepresentation;
                if (!tiff || tiff.isNil()) throw new Error('Cannot convert screenshot');
                var board = $.NSPasteboard.generalPasteboard;
                board.clearContents;
                if (!board.setDataForType(data, $.NSPasteboardTypePNG)
                        || !board.setDataForType(tiff, $.NSPasteboardTypeTIFF))
                    throw new Error('Clipboard write failed');
            }
            """;

    public static boolean copyText(String text) {
        return attempt("Clipboard text copy", () -> {
            if (Gdx.app == null || text == null) throw new IOException("Clipboard unavailable");
            Gdx.app.getClipboard().setContents(text);
        });
    }

    public static boolean openUri(String uri) {
        return attempt("Browser launch", () -> {
            URI target = URI.create(uri);
            String scheme = target.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
                throw new IOException("Only HTTP(S) browser URLs are supported");
            if (Gdx.net != null) {
                if (!Gdx.net.openURI(uri)) throw new IOException("Browser unavailable or URI rejected");
            } else if (isMac()) {
                // The JavaFX Launcher has no libGDX application/backend.
                runCommand(macUriCommand(target));
            } else {
                AwtDesktopActions.openUri(target);
            }
        });
    }

    public static boolean openPath(Path path) {
        return attempt("File/folder launch", () -> {
            Path absolute = path.toAbsolutePath().normalize();
            if (!Files.exists(absolute)) throw new IOException("Path does not exist: " + absolute);
            if (isMac()) runCommand(macOpenCommand(absolute));
            else AwtDesktopActions.openPath(absolute);
        });
    }

    /** Copies a saved PNG; failure does not invalidate the saved screenshot. */
    public static boolean copyImage(Path path) {
        return attempt("Screenshot clipboard copy", () -> {
            Path absolute = path.toAbsolutePath().normalize();
            if (!Files.isRegularFile(absolute)) throw new IOException("Image does not exist: " + absolute);
            if (isMac()) runCommand(macImageCommand(absolute));
            else AwtDesktopActions.copyImage(absolute);
        });
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").startsWith("Mac");
    }

    static List<String> macOpenCommand(Path path) {
        return List.of("/usr/bin/open", path.toAbsolutePath().normalize().toString());
    }

    static List<String> macUriCommand(URI uri) {
        return List.of("/usr/bin/open", uri.toString());
    }

    static List<String> macImageCommand(Path path) {
        return List.of("/usr/bin/osascript", "-l", "JavaScript", "-e", MAC_IMAGE_SCRIPT,
                path.toAbsolutePath().normalize().toString());
    }

    static void runCommand(List<String> command) throws IOException, InterruptedException {
        runCommand(command, 10_000, args -> new ProcessBuilder(args)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start());
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(List<String> command) throws IOException;
    }

    static void runCommand(List<String> command, long timeoutMillis, ProcessStarter starter)
            throws IOException, InterruptedException {
        Process process = starter.start(command);
        try {
            process.getOutputStream().close();
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS))
                throw new IOException("Desktop helper timed out: " + command.get(0));
            if (process.exitValue() != 0)
                throw new IOException("Desktop helper exited with " + process.exitValue() + ": " + command.get(0));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static boolean attempt(String action, CheckedAction task) {
        try {
            task.run();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getGlobal().warning(action + " interrupted");
        } catch (Exception e) {
            Logger.getGlobal().warning(action + " failed: " + e);
        }
        return false;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
