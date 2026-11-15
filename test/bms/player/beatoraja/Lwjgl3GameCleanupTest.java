package bms.player.beatoraja;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

public final class Lwjgl3GameCleanupTest {
    public static void main(String[] args) {
        verify(false);
        verify(true);
    }

    private static void verify(boolean fail) {
        var expected = new IllegalStateException("Intentional first-frame failure");
        boolean[] logged = {false};
        boolean[] cleaned = {false};
        boolean[] disposed = {false};
        var handler = new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getThrown() == expected) {
                    check(!cleaned[0], "Original exception must be logged before cleanup");
                    check(record instanceof Lwjgl3GameApplication.BeforeCleanupLogRecord, "Log must bypass the async queue");
                    logged[0] = true;
                }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        var config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(160, 120);
        config.disableAudio(true);
        var logger = Logger.getGlobal();
        logger.addHandler(handler);
        // Keep this callback alive so the test can detect a stale registration without crashing.
        try (var callback = GLFWErrorCallback.createPrint(System.err)) {
            try {
                new Lwjgl3GameApplication(new ApplicationAdapter() {
                    @Override public void render() {
                        callback.set();
                        if (fail) throw expected;
                        Gdx.app.exit();
                    }
                    @Override public void dispose() { disposed[0] = true; }
                }, config) {
                    @Override protected void cleanup() {
                        cleaned[0] = true;
                        super.cleanup();
                        check(GLFW.glfwSetErrorCallback(null) == null, "GLFW still has an error callback after teardown");
                    }
                };
                check(!fail, "Expected the original failure to propagate");
            } catch (IllegalStateException failure) {
                if (!fail || failure != expected) throw failure;
            } finally {
                GLFW.glfwSetErrorCallback(null);
            }
        } finally {
            logger.removeHandler(handler);
        }
        check(cleaned[0], "Game cleanup was not called");
        check(logged[0] == fail, "Unexpected failure log state");
        check(fail || disposed[0], "Normal exit did not dispose the application listener");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
