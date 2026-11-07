package bms.player.beatoraja.input.keyboard;

import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.input.PollingLwjgl3Input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Json;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import org.lwjgl.glfw.GLFW;

/** Platform-gated fixed aliases, without a window or Windows DLLs. */
public final class WindowsKeyboardMappingTest {
    public static void main(String[] args) throws Exception {
        var input = KeyboardQueueTestSupport.allocate(PollingLwjgl3Input.class);
        check(WindowsKeyboardMapping.RO == 240 && WindowsKeyboardMapping.YEN == 73, "Stable IDs; keep legacy Backslash");
        int ro = WindowsKeyboardMapping.resolve(true, GLFW.GLFW_KEY_UNKNOWN, 0x73);
        int yen = WindowsKeyboardMapping.resolve(true, GLFW.GLFW_KEY_UNKNOWN, 0x7d);
        check(input.getGdxKeyCode(ro) == WindowsKeyboardMapping.RO
                && input.getGdxKeyCode(yen) == WindowsKeyboardMapping.YEN, "Same aliases for libGDX and FIFO");
        check(ro != yen && ro != GLFW.GLFW_KEY_WORLD_2, "Ro, Yen and existing ISO World 2 are distinct");
        for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
            check(WindowsKeyboardMapping.resolve(true, key, 0x73) == key
                    && WindowsKeyboardMapping.resolve(true, key, 0x7d) == key, "Never replace a known GLFW key");
        }
        for (int scan : new int[]{0x73, 0x7d}) check(
                WindowsKeyboardMapping.resolve(false, GLFW.GLFW_KEY_UNKNOWN, scan) == GLFW.GLFW_KEY_UNKNOWN,
                "Other platforms keep their existing key mapping");
        for (int scan : new int[]{-1, 0, 0x56, 0x173, 0x17d, 0x10073, Integer.MAX_VALUE}) check(
                WindowsKeyboardMapping.resolve(true, GLFW.GLFW_KEY_UNKNOWN, scan) == GLFW.GLFW_KEY_UNKNOWN,
                "Do not alias arbitrary/extended/invalid scancodes");
        check(WindowsKeyboardMapping.keyName(true, 240).equals("JIS Ro")
                && WindowsKeyboardMapping.keyName(true, 73).equals("Yen/\\"), "Distinct compact labels");
        check(java.util.Objects.equals(WindowsKeyboardMapping.keyName(false, 240), Keys.toString(240))
                && java.util.Objects.equals(WindowsKeyboardMapping.keyName(false, 73), Keys.toString(73)), "Other OS labels unchanged");
        var config = new KeyboardConfig(Mode.BEAT_7K, true);
        config.getKeyAssign()[0] = WindowsKeyboardMapping.RO;
        config.getKeyAssign()[1] = WindowsKeyboardMapping.YEN;
        config.getKeyAssign()[2] = Keys.Z;
        config.setStart(240);
        config.setSelect(73);
        var json = new Json();
        var loaded = json.fromJson(KeyboardConfig.class, json.toJson(config));
        check(java.util.Arrays.equals(config.getKeyAssign(), loaded.getKeyAssign())
                && loaded.getStart() == 240 && loaded.getSelect() == 73, "Config keeps numeric IDs without migration");
        System.out.println("WindowsKeyboardMappingTest passed (fixed aliases, known-key/other-OS isolation, names and config)");
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
