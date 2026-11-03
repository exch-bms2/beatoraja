package bms.player.beatoraja.build;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import org.lwjgl.glfw.GLFW;

/** Opt-in test-only evidence captured on the GL thread before a failing callback unwinds. */
public final class GlTestDiagnostics implements ApplicationListener {
    private final ApplicationListener delegate;
    private final Path output;
    private boolean firstFrame;

    private GlTestDiagnostics(ApplicationListener delegate, Path output) {
        this.delegate = delegate;
        this.output = output;
    }

    public static ApplicationListener wrap(String name, ApplicationListener delegate) {
        String directory = System.getProperty("beatoraja.glTest.output");
        if (directory == null) return delegate;
        if (!name.matches("[A-Za-z0-9-]+")) throw new IllegalArgumentException("Invalid GL test name");
        return new GlTestDiagnostics(delegate, Path.of(directory).resolve(name));
    }

    static void verifyEnvironment(String os, int backend, String renderer, boolean requireMesa) {
        if (requireMesa && (!os.startsWith("Linux") || backend != GLFW.GLFW_PLATFORM_X11
                || renderer == null || !renderer.toLowerCase(Locale.ROOT).contains("llvmpipe")))
            throw new AssertionError("Linux GL CI requires X11 and Mesa llvmpipe, not " + os + "/" + backend + "/" + renderer);
    }

    @Override public void create() {
        run(() -> {
            Files.createDirectories(output);
            String renderer = Gdx.gl.glGetString(GL20.GL_RENDERER);
            int backend = GLFW.glfwGetPlatform();
            String evidence = "os=" + System.getProperty("os.name") + "\njava=" + System.getProperty("java.version")
                    + "\narch=" + System.getProperty("os.arch") + "\nbackend=" + backend
                    + "\nvendor=" + Gdx.gl.glGetString(GL20.GL_VENDOR) + "\nrenderer=" + renderer
                    + "\nversion=" + Gdx.gl.glGetString(GL20.GL_VERSION)
                    + "\nlogical=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight()
                    + "\nframebuffer=" + Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight()
                    + "\nNOT TESTED: physical GPU, audio, IME server, compositor focus, mixed-monitor HiDPI\n";
            Files.writeString(output.resolve("environment.txt"), evidence);
            System.out.print(evidence);
            verifyEnvironment(System.getProperty("os.name"), backend, renderer,
                    Boolean.getBoolean("beatoraja.glTest.requireMesa"));
            delegate.create();
        });
    }

    @Override public void resize(int width, int height) { run(() -> delegate.resize(width, height)); }
    @Override public void pause() { run(delegate::pause); }
    @Override public void resume() { run(delegate::resume); }
    @Override public void render() {
        run(() -> {
            delegate.render();
            if (!firstFrame) { capture("first-frame.png"); firstFrame = true; }
        });
    }
    @Override public void dispose() {
        // A screenshot failure must not prevent the test's normal resource cleanup.
        Throwable captureFailure = null;
        try { capture("last-frame.png"); } catch (Throwable failure) { captureFailure = failure; }
        try { run(delegate::dispose); }
        catch (RuntimeException | Error failure) {
            if (captureFailure != null) failure.addSuppressed(captureFailure);
            throw failure;
        }
        if (captureFailure != null) throw new AssertionError("Final GL screenshot failed", captureFailure);
    }

    private void run(Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            try {
                Files.createDirectories(output);
                var trace = new StringWriter(); failure.printStackTrace(new PrintWriter(trace));
                Files.writeString(output.resolve("failure.txt"), trace.toString());
            } catch (Throwable extra) { failure.addSuppressed(extra); }
            try { capture("failure.png"); } catch (Throwable extra) { failure.addSuppressed(extra); }
            if (failure instanceof Error error) throw error;
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new AssertionError(failure);
        }
    }

    private void capture(String name) throws Exception {
        Files.createDirectories(output);
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(new FileHandle(output.resolve(name).toFile()), image, -1, true); }
        finally { image.dispose(); }
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
