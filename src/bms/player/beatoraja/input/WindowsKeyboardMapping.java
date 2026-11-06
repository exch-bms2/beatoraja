package bms.player.beatoraja.input;

import com.badlogic.gdx.Input.Keys;
import org.lwjgl.glfw.GLFW;

/** Fixed aliases for JIS keys missing from GLFW's Windows scancode table. */
final class WindowsKeyboardMapping {
    static final int RO = Keys.WORLD_1;
    static final int YEN = Keys.BACKSLASH;

    private WindowsKeyboardMapping() { }

    static int resolve(boolean windows, int key, int scancode) {
        if (!windows || key != GLFW.GLFW_KEY_UNKNOWN) return key;
        // Do not mask extended bits: unrelated extended keys must not become JIS keys.
        return switch (scancode) {
            case 0x73 -> GLFW.GLFW_KEY_WORLD_1;
            case 0x7d -> GLFW.GLFW_KEY_BACKSLASH;
            default -> key;
        };
    }

    static String keyName(boolean windows, int key) {
        if (windows) {
            if (key == RO) return "JIS Ro";
            if (key == YEN) return "Yen/\\";
        }
        return Keys.toString(key);
    }
}
