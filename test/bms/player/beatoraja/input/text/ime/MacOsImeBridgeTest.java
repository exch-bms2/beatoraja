package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.text.ImeBridge;

/** The prototype must never load native code on an opt-out or non-GLFW editor path. */
public final class MacOsImeBridgeTest {
    public static void main(String[] args) throws Exception {
        String previous = System.getProperty("beatoraja.macosIme");
        try {
            System.clearProperty("beatoraja.macosIme");
            check(MacOsIme.create(false) == ImeBridge.NONE, "Config opt-out");
            System.setProperty("beatoraja.macosIme", "true");
            check(MacOsIme.create() == ImeBridge.NONE, "No GLFW context is a safe fallback");
            var loaded = MacOsIme.class.getDeclaredField("loaded"); loaded.setAccessible(true);
            check(!(boolean)loaded.get(null), "Headless/Launcher fallback must not load JNI");
            System.out.println("MacOsImeBridgeTest passed (opt-out, no GLFW fallback, no native load)");
        } finally {
            if (previous == null) System.clearProperty("beatoraja.macosIme"); else System.setProperty("beatoraja.macosIme", previous);
        }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
