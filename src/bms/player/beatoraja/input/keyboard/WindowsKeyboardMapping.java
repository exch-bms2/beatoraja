package bms.player.beatoraja.input.keyboard;

import com.badlogic.gdx.Input.Keys;
import org.lwjgl.glfw.GLFW;

/** Fixed aliases for JIS keys missing from GLFW's Windows scancode table. */
public final class WindowsKeyboardMapping {
    public static final int RO = Keys.WORLD_1;
    static final int YEN = Keys.BACKSLASH;
    public static final int NONCONVERT = 244;
    public static final int CONVERT = 245;
    private static final int RO_SCAN = 0x73, YEN_SCAN = 0x7d, NONCONVERT_SCAN = 0x7b, CONVERT_SCAN = 0x79;

    // Private callback codes, never sent to GLFW. They preserve libGDX's usual event queue and held state.
    private static final int NONCONVERT_CALLBACK = -2, CONVERT_CALLBACK = -3;

    private WindowsKeyboardMapping() { }

    public static int resolve(boolean windows, int key, int scancode) {
        if (!windows || key != GLFW.GLFW_KEY_UNKNOWN) return key;
        // Do not mask extended bits: unrelated extended keys must not become JIS keys.
        return switch (scancode) {
            case RO_SCAN -> GLFW.GLFW_KEY_WORLD_1;
            case YEN_SCAN -> GLFW.GLFW_KEY_BACKSLASH;
            case NONCONVERT_SCAN -> NONCONVERT_CALLBACK;
            case CONVERT_SCAN -> CONVERT_CALLBACK;
            default -> key;
        };
    }

    public static int conversionKey(int callbackCode) {
        return switch (callbackCode) {
            case NONCONVERT_CALLBACK -> NONCONVERT;
            case CONVERT_CALLBACK -> CONVERT;
            default -> Keys.UNKNOWN;
        };
    }

    public static boolean isConversionKey(int key) {
        return key == NONCONVERT || key == CONVERT;
    }

    public static int aliasBit(boolean windows, int key) {
        if (!windows) return 0;
        return switch (key) {
            case RO -> 1;
            case YEN -> 2;
            case NONCONVERT -> 4;
            case CONVERT -> 8;
            default -> 0;
        };
    }

    public static int aliasScancode(int bit) {
        return switch (bit) {
            case 1 -> RO_SCAN;
            case 2 -> YEN_SCAN;
            case 4 -> NONCONVERT_SCAN;
            case 8 -> CONVERT_SCAN;
            default -> throw new IllegalArgumentException("Invalid keyboard alias bit: " + bit);
        };
    }

    public static String keyName(boolean windows, int key) {
        if (key == NONCONVERT) return "Muhenkan";
        if (key == CONVERT) return "Henkan";
        if (windows) {
            if (key == RO) return "JIS Ro";
            if (key == YEN) return "Yen/\\";
        }
        return Keys.toString(key);
    }
}
