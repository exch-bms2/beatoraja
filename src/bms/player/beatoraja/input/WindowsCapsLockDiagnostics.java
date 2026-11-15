package bms.player.beatoraja.input;

import org.lwjgl.glfw.GLFW;
import java.util.logging.Logger;

/** Observe Caps Lock at the GLFW boundary without logging typed characters or changing key routing. */
final class WindowsCapsLockDiagnostics {
    private WindowsCapsLockDiagnostics() {}

    static boolean enable(boolean windows) {
        boolean enabled = windows && Boolean.getBoolean("beatoraja.windowsCapsLockDiagnostics");
        if (enabled) Logger.getGlobal().info("Windows Caps Lock diagnostics enabled: GLFW key events only");
        return enabled;
    }

    static void key(int code, int scancode, int mapped, int action, int mods) {
        if (code != GLFW.GLFW_KEY_CAPS_LOCK && (scancode & 0xff) != 0x3a) return;
        String edge = switch (action) {
            case GLFW.GLFW_PRESS -> "press";
            case GLFW.GLFW_RELEASE -> "release";
            case GLFW.GLFW_REPEAT -> "repeat";
            default -> Integer.toString(action);
        };
        Logger.getGlobal().info("Windows Caps Lock input: action=" + edge + ", shift="
                + ((mods & GLFW.GLFW_MOD_SHIFT) != 0) + ", glfwKey=" + code + ", scan=0x"
                + Integer.toHexString(scancode) + ", gdxKey=" + mapped + ", mods=0x" + Integer.toHexString(mods));
    }
}
