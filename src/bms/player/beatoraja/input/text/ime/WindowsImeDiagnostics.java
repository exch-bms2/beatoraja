package bms.player.beatoraja.input.text.ime;

import java.util.logging.Logger;

/** Lifecycle-only diagnostics. Callers supply fixed event names, never editor or OS message payloads. */
public final class WindowsImeDiagnostics {
    private WindowsImeDiagnostics() {}

    static boolean enabled() {
        return Boolean.getBoolean("beatoraja.windowsImeDiagnostics");
    }

    static void event(long session, String event) {
        if (enabled()) log("session=" + session + " " + event);
    }

    public static void windowFocus(boolean focused, boolean finished) {
        if (enabled() && System.getProperty("os.name", "").startsWith("Windows")) {
            log("GLFW focus " + (focused ? "gained" : "lost") + (finished ? " completed" : " begin"));
        }
    }

    private static void log(String event) {
        Logger.getGlobal().info("Windows IME lifecycle: " + event + " thread=" + Thread.currentThread().getName());
    }
}
