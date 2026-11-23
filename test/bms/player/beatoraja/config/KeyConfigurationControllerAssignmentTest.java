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
        keyboardAssignments();
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

    private static void keyboardAssignments() throws Exception {
        var player = new PlayerConfig();
        var layout = player.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P1).getKeyboardConfig();
        for (int target : new int[]{0, -1, -2}) {
            check(KeyConfiguration.keyboardAssignmentRejection(player, layout, target, Keys.NUM_2) == null,
                    "Numeric select shortcut can be reused in play for lanes, START and SELECT");
            check(KeyConfiguration.keyboardAssignmentRejection(player, layout, target, Keys.F1) != null,
                    "An active global shortcut conflicts with every target");
            check(KeyConfiguration.keyboardAssignmentRejection(player, layout, target, Keys.ESCAPE) != null,
                    "Fixed recovery control remains reserved");
        }
        check(player.setShortcutBinding(KeyboardShortcut.SHOW_FPS, KeyboardShortcut.DISABLED), "Disable F1");
        for (int target : new int[]{0, -1, -2})
            check(KeyConfiguration.keyboardAssignmentRejection(player, layout, target, Keys.F1) == null,
                    "Released function key can be assigned to any target");
        check(player.setShortcutBinding(KeyboardShortcut.SHOW_FPS, Keys.A), "Remap global shortcut");
        check(KeyConfiguration.keyboardAssignmentRejection(player, layout, -1, Keys.A).contains("shortcut"),
                "New shortcut binding is checked, not only default function keys");
        check(player.setShortcutBinding(KeyboardShortcut.SHOW_FPS,
                KeyboardShortcut.chord(Keys.A, KeyBoardInputProcesseor.MASK_CTRL)), "Modified shortcut");
        check(KeyConfiguration.keyboardAssignmentRejection(player, layout, -2, Keys.A) != null,
                "A modifier chord also conflicts with the underlying gameplay key");
        check(!player.setShortcutBinding(KeyboardShortcut.SHOW_FPS, Keys.NUM_2),
                "Existing validation disallows global shortcuts on numeric gameplay keys");
        check(player.setShortcutBinding(KeyboardShortcut.SHOW_FPS, Keys.A), "Restore remapped global shortcut");
        layout.setStart(Keys.NUM_2);
        check(KeyConfiguration.keyboardAssignmentRejection(player, layout, -1, Keys.NUM_2) == null
                && KeyConfiguration.keyboardAssignmentRejection(player, layout, -2, Keys.NUM_2) != null
                && KeyConfiguration.keyboardAssignmentRejection(player, layout, 0, Keys.NUM_2) != null,
                "Numeric START does not conflict with itself, but cannot overlap SELECT or a lane");
        var input = KeyboardQueueTestSupport.input(layout);
        var keyboard = input.getKeyBoardInputProcesseor();
        var view = new KeyConfiguration(KeyboardQueueTestSupport.allocate(MainController.class));
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "config", player);
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "keyboardConfig", layout);
        KeyboardQueueTestSupport.set(view, KeyConfiguration.class, "keyboard", keyboard);
        var assign = KeyConfiguration.class.getDeclaredMethod("setKeyboardKeyAssign", int.class);
        assign.setAccessible(true);
        keyboard.setLastPressedKey(Keys.F1);
        check((boolean) assign.invoke(view, -1) && layout.getStart() == Keys.F1, "Assign freed F1 to START");
        keyboard.setLastPressedKey(Keys.NUM_2);
        check((boolean) assign.invoke(view, -2) && layout.getSelect() == Keys.NUM_2, "Assign freed number to SELECT");
        var json = new Json();
        var restored = json.fromJson(PlayerConfig.class, json.toJson(player));
        restored.validate();
        var saved = restored.getPlayInputConfig(Mode.BEAT_7K, PlaySide.P1).getKeyboardConfig();
        check(saved.getStart() == Keys.F1 && saved.getSelect() == Keys.NUM_2,
                "Assignments survive the existing config format and validation");
        keyboard.setLastPressedKey(Keys.A);
        check(!(boolean) assign.invoke(view, -1) && layout.getStart() == Keys.F1,
                "Rejected assignment preserves the prior binding");
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
