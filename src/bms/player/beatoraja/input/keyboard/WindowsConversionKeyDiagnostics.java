package bms.player.beatoraja.input.keyboard;

import java.util.logging.Logger;

/** Opt-in routing diagnostics for conversion keys, never text or composition contents. */
public final class WindowsConversionKeyDiagnostics {
    private static final boolean ENABLED = System.getProperty("os.name", "").startsWith("Windows")
            && Boolean.getBoolean("beatoraja.inputDiagnostics");

    private WindowsConversionKeyDiagnostics() { }

    public static boolean enabledFor(int key) {
        return ENABLED && WindowsKeyboardMapping.isConversionKey(key);
    }

    public static void callback(boolean windows, int code, int scan, int mapped, int action, int mods) {
        if (!ENABLED || !windows || !isRelevant(scan, mapped)) return;
        logEvent("callback", callbackDetails(code, scan, mapped, action, mods));
    }

    static String callbackDetails(int code, int scan, int mapped, int action, int mods) {
        return "glfwKey=" + code + ", scan=0x" + Integer.toHexString(scan)
                + ", logicalKey=" + mapped + ", action=" + action + ", mods=" + mods;
    }

    static boolean isRelevant(int scan, int mapped) {
        return scan == 0x79 || scan == 0x7b || WindowsKeyboardMapping.isConversionKey(mapped);
    }

    public static void log(String stage, int key, String details) {
        if (enabledFor(key))
            logEvent(stage, "logicalKey=" + key + ", " + details);
    }

    private static void logEvent(String stage, String details) {
        Logger.getGlobal().info("Windows conversion key: stage=" + stage + ", " + details);
    }
}
