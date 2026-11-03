package bms.player.beatoraja.build;

import java.nio.file.Files;
import java.nio.file.Path;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;

/** Real GL negative fixture: diagnostic capture must preserve the original assertion and cleanup. */
public final class GlTestDiagnosticsFailureTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("beatoraja.glTest.output"));
        var config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(160, 120); config.disableAudio(true); config.setForegroundFPS(60);
        boolean[] verified = {false};
        new Lwjgl3Application(new ApplicationAdapter() {
            int frames, disposed;
            @Override public void render() {
                if (++frames != 5) return;
                var expected = new AssertionError("Expected GL diagnostic failure fixture");
                var guarded = GlTestDiagnostics.wrap("diagnostic-failure-detector", new ApplicationAdapter() {
                    @Override public void render() {
                        Gdx.gl.glClearColor(1, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        throw expected;
                    }
                    @Override public void dispose() { disposed++; }
                });
                guarded.create();
                try {
                    try { guarded.render(); throw new AssertionError("Fixture must throw"); }
                    catch (AssertionError failure) {
                        if (failure != expected) throw new AssertionError("Capture replaced the original failure", failure);
                    }
                    Path output = root.resolve("diagnostic-failure-detector");
                    if (!Files.readString(output.resolve("failure.txt")).contains(expected.getMessage()))
                        throw new AssertionError("Missing failure trace");
                    var image = new Pixmap(Gdx.files.absolute(output.resolve("failure.png").toString()));
                    try {
                        if (image.getWidth() != Gdx.graphics.getBackBufferWidth()
                                || image.getHeight() != Gdx.graphics.getBackBufferHeight()
                                || image.getPixel(image.getWidth() / 2, image.getHeight() / 2) != 0xff0000ff)
                            throw new AssertionError("Failure image must contain the actual red framebuffer");
                    } finally { image.dispose(); }
                } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                finally { guarded.dispose(); }
                if (disposed != 1) throw new AssertionError("Capture must preserve delegate cleanup");
                verified[0] = true; Gdx.app.exit();
            }
        }, config);
        if (!verified[0]) throw new AssertionError("Failure detector did not execute");
        System.out.println("GlTestDiagnosticsFailureTest passed (expected assertion, real failure PNG/trace and cleanup; not an actual suite failure)");
    }
}
