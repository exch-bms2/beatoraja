package bms.player.beatoraja;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.lwjgl.glfw.GLFW;

class Lwjgl3GameApplication extends Lwjgl3Application {
    Lwjgl3GameApplication(ApplicationListener listener, Lwjgl3ApplicationConfiguration config) {
        super(listener, config);
    }

    @Override
    protected void loop() {
        try {
            super.loop();
        } catch (Throwable failure) {
            logBeforeCleanup("LWJGL3 game loop failed before cleanup", failure);
            throw failure;
        }
    }

    @Override
    protected void cleanupWindows() {
        try {
            super.cleanupWindows();
        } catch (Throwable failure) {
            logBeforeCleanup("LWJGL3 window cleanup failed before GLFW termination", failure);
            throw failure;
        }
    }

    @Override
    protected void cleanup() {
        // libGDX owns the callback and frees it before glfwTerminate(). Only unregister it here.
        GLFW.glfwSetErrorCallback(null);
        super.cleanup();
    }

    private static void logBeforeCleanup(String message, Throwable failure) {
        try {
            Logger.getGlobal().log(new BeforeCleanupLogRecord(message, failure));
        } catch (RuntimeException loggingFailure) {
            // Still report the original failure if a logging handler failed.
            System.err.println(message);
            failure.printStackTrace(System.err);
        }
    }

    // Bypass the async queue so this reaches the console and XML even if native teardown crashes.
    static final class BeforeCleanupLogRecord extends LogRecord {
        private BeforeCleanupLogRecord(String message, Throwable failure) {
            super(Level.SEVERE, message);
            setLoggerName(Logger.GLOBAL_LOGGER_NAME);
            setThrown(failure);
        }
    }
}
