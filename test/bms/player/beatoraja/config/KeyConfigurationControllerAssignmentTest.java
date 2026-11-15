package bms.player.beatoraja.config;

import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import bms.model.Mode;
import bms.player.beatoraja.MainController;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlaySide;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.input.keyboard.KeyboardShortcut;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Json;

public final class KeyConfigurationControllerAssignmentTest {
    public static void main(String[] args) throws Exception {
        int[] independentOrder = {0, 7, 1, 8, 2, 3, 4, 5, 12, 6};
        for (int i = 0; i < independentOrder.length; i++)
            check(KeyConfiguration.modeIdAt(i, true) == independentOrder[i], "2P follows its 1P mode");
        for (int i = 0; i < 7; i++)
            check(KeyConfiguration.modeIdAt(i, false) == i, "Shared settings retain the original order");
        verifyEscape(false);
        verifyEscape(true);
        var layouts = new PlayerConfig();
        check(KeyConfiguration.conflictsWithLayout(layouts, KeyboardShortcut.SHOW_FPS, Keys.Z),
                "A global shortcut must not take a note key");
        check(KeyConfiguration.conflictsWithLayout(layouts, KeyboardShortcut.SORT, Keys.Q),
                "Select cannot bind over keyboard START");
        check(!KeyConfiguration.conflictsWithLayout(layouts, KeyboardShortcut.SWITCH_SKIN_SIDE, Keys.Z),
                "A skin configuration shortcut may reuse a key from a disjoint layout");
        var first = new ControllerConfig();
        first.setName("Controller");
        first.setKeyAssign(new int[]{7});
        var second = new ControllerConfig();
        second.setName("Controller-2");
        second.setKeyAssign(new int[]{9});
        var slots = new ControllerConfig[]{first, second};

        check(KeyConfiguration.assignController(slots, 0, "Other device", 30,
                ignored -> { throw new AssertionError("An unselected device must not clear a binding"); }) == -1,
                "Unselected device keeps assignment pending");
        check(first.getKeyAssign()[0] == 7 && second.getKeyAssign()[0] == 9, "Existing bindings stay intact");
        check(KeyConfiguration.assignController(slots, 0, "Controller-2", 39,
                index -> { first.getKeyAssign()[index] = -1; second.getKeyAssign()[index] = -1; }) == 1,
                "A selected device can bind after rejection; keep the disambiguated name");
        check(KeyConfiguration.assignController(slots, -1, "Controller-2", 2, ignored -> {}) == 1, "START");
        check(KeyConfiguration.assignController(slots, -2, "Controller-2", 3, ignored -> {}) == 1, "SELECT");
        var json = new Json();
        KeyConfiguration.selectController(slots, 1, "New device");
        check("Controller".equals(first.getName()) && "New device".equals(second.getName()),
                "Selection changes only the requested slot");
        var reloaded = json.fromJson(ControllerConfig.class, json.toJson(second));
        check("New device".equals(reloaded.getName()) && reloaded.getKeyAssign()[0] == 39
                && reloaded.getStart() == 2 && reloaded.getSelect() == 3, "Existing config format and logical IDs");
    }

    private static void verifyEscape(boolean independentSides) throws Exception {
        var player = new PlayerConfig();
        player.setIndependentPlaySides(independentSides);
        var side = independentSides ? PlaySide.P2 : PlaySide.P1;
        var bindings = player.getPlayInputConfig(Mode.BEAT_7K, side);
        var input = KeyboardQueueTestSupport.input(bindings.getKeyboardConfig());
        var keyboard = input.getKeyBoardInputProcesseor();
        var held = new boolean[256];
        held[ControlKeys.ESCAPE.keycode] = true;
        KeyboardQueueTestSupport.set(keyboard, KeyBoardInputProcesseor.class, "keystate", held);
        var times = new long[256];
        java.util.Arrays.fill(times, Long.MIN_VALUE);
        times[ControlKeys.ESCAPE.keycode] = 1;
        KeyboardQueueTestSupport.set(keyboard, KeyBoardInputProcesseor.class, "keytime", times);
        var main = KeyboardQueueTestSupport.allocate(MainController.class);
        var transitions = new int[1];
        var view = new KeyConfiguration(main) {
            @Override void returnToMusicSelect() {
                transitions[0]++;
                shutdown();
            }
        };
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "input", input);
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "config", player);
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "pc", bindings);
        // No GL or skin: any attempt to continue rendering after the transition fails.
        view.render();
        check(transitions[0] == 1, "Esc exits KeyConfig, independent sides=" + independentSides);
        check(!input.isControlKeyPressed(ControlKeys.ESCAPE), "Esc is consumed by the exit");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
