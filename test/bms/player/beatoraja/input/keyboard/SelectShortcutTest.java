package bms.player.beatoraja.input.keyboard;

import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.ReplayData;
import bms.player.beatoraja.input.KeyboardJudgeQueue;
import bms.player.beatoraja.input.KeyboardQueueTestSupport;
import bms.player.beatoraja.input.PollingLwjgl3Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Json;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.lang.reflect.Proxy;

import static bms.player.beatoraja.input.keyboard.KeyboardShortcut.*;
import static bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.*;
import static bms.player.beatoraja.MainState.MainStateType.CONFIG;
import static bms.player.beatoraja.MainState.MainStateType.COURSERESULT;
import static bms.player.beatoraja.MainState.MainStateType.MUSICSELECT;
import static bms.player.beatoraja.MainState.MainStateType.PLAY;
import static bms.player.beatoraja.MainState.MainStateType.RESULT;
import static bms.player.beatoraja.MainState.MainStateType.SKINCONFIG;

public final class SelectShortcutTest {
    public static void main(String[] args) throws Exception {
        configRoundTrip();
        functionChords();
        var previous = Gdx.input;
        try {
            routesAndReplay();
            overlayToggle();
        } finally {
            Gdx.input = previous;
        }
        System.out.println("SelectShortcutTest passed");
    }

    private static void configRoundTrip() {
        var json = new Json();
        var player = json.fromJson(PlayerConfig.class, "{}");
        for (var action : values()) check(player.getShortcutBinding(action) == action.defaultBinding(), "Legacy defaults");
        check(!player.setShortcutBinding(SORT, Keys.NUM_1), "Reject another shortcut's key");
        check(!player.setShortcutBinding(SORT, Keys.ENTER) && !player.setShortcutBinding(SORT, Keys.F2),
                "Reject fixed select shortcuts");
        check(player.setShortcutBinding(SORT, Keys.A) && player.setShortcutBinding(SEARCH, DISABLED),
                "Remap and disable independently");
        var loaded = json.fromJson(PlayerConfig.class, json.toJson(player));
        loaded.validate();
        check(loaded.getShortcutBinding(SORT) == Keys.A && loaded.getShortcutBinding(SEARCH) == DISABLED,
                "Remap and disable survive config reload");
        check(loaded.setShortcutBinding(SORT, SORT.defaultBinding()), "Restore a single action");
        loaded.resetShortcuts();
        for (var action : values()) check(loaded.getShortcutBinding(action) == action.defaultBinding(), "Restore all defaults");
        var invalid = json.fromJson(PlayerConfig.class,
                "{selectShortcuts:{SORT:" + Keys.NUM_1 + ",SEARCH:9999,OPEN_DOCUMENT:-1}}");
        invalid.validate();
        check(invalid.getShortcutBinding(SORT) == Keys.NUM_2 && invalid.getShortcutBinding(SEARCH) == Keys.NUM_0
                && invalid.getShortcutBinding(OPEN_DOCUMENT) == DISABLED, "Validate invalid and conflicting file entries");
    }

    private static void functionChords() {
        var player = new PlayerConfig();
        var held = new boolean[256];
        var shortcuts = new KeyboardShortcutInput();
        var state = (java.util.function.IntPredicate) key -> held[key];
        shortcuts.configure(player, MUSICSELECT, key -> true, state);
        int[] modifiers = {0, MASK_SHIFT, MASK_CTRL | MASK_ALT, MASK_CTRL | MASK_SHIFT | MASK_ALT};
        var expected = new KeyboardShortcut[]{OPEN_EXPLORER, OPEN_EXPLORER, COPY_SONG_MD5_HASH, COPY_SONG_SHA256_HASH};
        for (int i = 0; i < modifiers.length; i++) {
            check(shortcuts.match(Keys.F3, modifiers[i]) == expected[i], "Preserve F3's legacy modifier matching");
        }
        check(shortcuts.match(Keys.NUM_6, MASK_CTRL) == null && !player.setShortcutBinding(SEARCH, chord(Keys.NUM_6, MASK_CTRL)),
                "Ctrl+6 remains reserved for recovery");
        check(!player.setShortcutBinding(SHOW_FPS, Keys.F2), "Global conflicts with the select F2 action");
        check(player.setShortcutBinding(SWITCH_RESULT_SCORE, Keys.F2), "The same chord is allowed in disjoint screens");
        check(player.setShortcutBinding(COPY_SONG_MD5_HASH, chord(Keys.F2, MASK_CTRL)), "Ctrl+F2 can coexist with plain F2");
        check(player.setShortcutBinding(SAVE_SCREENSHOT, chord(Keys.F3, MASK_ALT)), "An exact chord may override a legacy fallback");
        check(!player.setShortcutBinding(OPEN_IR, chord(Keys.F3, MASK_ALT)), "Exact chords conflict across intersecting screens");
        check(player.setShortcutBinding(SWITCH_PLAY_SIDE, DISABLED), "Disable only the select side switch");
        var json = new Json();
        var read = json.fromJson(PlayerConfig.class, json.toJson(player));
        read.validate();
        check(read.getShortcutBinding(COPY_SONG_MD5_HASH) == chord(Keys.F2, MASK_CTRL)
                && read.getShortcutBinding(SWITCH_PLAY_SIDE) == DISABLED, "Modifier bindings and disabled actions survive reload");
        shortcuts.configure(read, MUSICSELECT, key -> true, state);
        check(shortcuts.match(Keys.F3, MASK_CTRL) == null && shortcuts.match(Keys.F3, MASK_ALT) == SAVE_SCREENSHOT,
                "Old chord is removed; exact chord takes priority without firing its fallback");
        held[Keys.F2] = true;
        shortcuts.poll(state, () -> MASK_CTRL, false);
        check(shortcuts.consume(COPY_SONG_MD5_HASH) && !shortcuts.consume(UPDATE_FOLDER), "Only Ctrl+F2 fires");
        shortcuts.poll(state, () -> 0, false);
        check(!shortcuts.consume(UPDATE_FOLDER), "Releasing Ctrl while F2 is held does not run F2 again");
        shortcuts.configure(read, RESULT, key -> true, state);
        shortcuts.poll(state, () -> 0, false);
        check(!shortcuts.consume(SWITCH_RESULT_SCORE), "A press is not carried across screens");
        check(shortcuts.match(Keys.F2, 0) == SWITCH_RESULT_SCORE && shortcuts.match(Keys.F11, 0) == OPEN_IR,
                "Result action and IR action resolve in the result context");
        shortcuts.configure(read, COURSERESULT, key -> true, state);
        check(shortcuts.match(Keys.F2, 0) == null && shortcuts.match(Keys.F11, 0) == OPEN_IR,
                "Course result keeps IR but not single-song score toggling");
        shortcuts.configure(read, SKINCONFIG, key -> true, state);
        check(shortcuts.match(Keys.F5, 0) == SWITCH_SKIN_SIDE, "Disabling select F5 leaves skin configuration F5");
        shortcuts.configure(read, PLAY, key -> key != Keys.F1, state);
        check(shortcuts.match(Keys.F12, 0) == ADJUST_SKIN && shortcuts.match(Keys.F1, 0) == null,
                "F12 has its play action and conflicting layout keys can be excluded");
        shortcuts.configure(read, MUSICSELECT, key -> true, state);
        check(shortcuts.match(Keys.F12, 0) == OPEN_SKIN_CONFIGURATION && shortcuts.match(Keys.F5, 0) == null,
                "F12 keeps its select action; disabled F5 has none");
        check(read.setShortcutBinding(COPY_SONG_MD5_HASH, COPY_SONG_MD5_HASH.defaultBinding())
                && read.getShortcutBinding(COPY_SONG_MD5_HASH) == chord(Keys.F3, MASK_CTRL), "Restore original modified chord");
    }

    private static void overlayToggle() throws Exception {
        var player = new PlayerConfig();
        var binding = chord(Keys.F8, MASK_CTRL);
        check(player.setShortcutBinding(ADJUST_SKIN, binding), "The overlay can use a modified shortcut");
        Gdx.input = null;
        var keyboard = KeyboardQueueTestSupport.input(new KeyboardConfig()).getKeyBoardInputProcesseor();
        var held = new boolean[256];
        var justPressed = new boolean[256];
        Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isKeyPressed" -> held[(int) args[0]];
                    case "isKeyJustPressed" -> justPressed[(int) args[0]];
                    default -> false;
                });
        keyboard.setShortcutContext(player, PLAY, true);
        justPressed[Keys.F12] = true;
        check(!keyboard.isOverlayTogglePressed(), "Old F12 no longer toggles the overlay");
        justPressed[Keys.F8] = true;
        check(!keyboard.isOverlayTogglePressed(), "A missing modifier does not toggle the overlay");
        held[Keys.CONTROL_LEFT] = true;
        check(keyboard.isOverlayTogglePressed(), "Remapped overlay key works even without the input polling thread");
        keyboard.setTextInputMode(true, true);
        check(!keyboard.isOverlayTogglePressed(), "IME suppresses the render-thread overlay shortcut too");
        keyboard.setTextInputMode(false);
        keyboard.setShortcutContext(player, MUSICSELECT, false);
        check(!keyboard.isOverlayTogglePressed(), "The overlay cannot toggle from Music Select");
    }

    private static void routesAndReplay() throws Exception {
        var glfw = KeyboardQueueTestSupport.allocate(PollingLwjgl3Input.class);
        var snapshot = new AtomicIntegerArray(256);
        var queue = new KeyboardEventQueue(8);
        KeyboardQueueTestSupport.set(glfw, PollingLwjgl3Input.class, "keys", snapshot);
        KeyboardQueueTestSupport.set(glfw, PollingLwjgl3Input.class, "keyboardEvents", queue);
        Gdx.input = null;
        var layout = new KeyboardConfig();
        layout.setKeyAssign(new int[]{Keys.NUM_1, Keys.A});
        layout.setDuration(16);
        var input = KeyboardQueueTestSupport.input(layout);
        var keyboard = input.getKeyBoardInputProcesseor();
        Gdx.input = glfw;
        var config = new PlayerConfig();
        check(config.setShortcutBinding(SORT, Keys.A), "Simulate a collision after editing the note layout");
        check(config.setShortcutBinding(SEARCH, DISABLED), "Disable search");
        keyboard.setShortcutContext(config, MUSICSELECT, false);
        queue.publish(true, snapshot);
        snapshot.set(Keys.NUM_0, 1);
        snapshot.set(Keys.NUM_1, 1);
        snapshot.set(Keys.A, 1);
        keyboard.poll(1_000);
        check(keyboard.consumeShortcut(MODE_FILTER) && keyboard.consumeShortcut(SORT)
                && !keyboard.consumeShortcut(SEARCH)
                && !input.getKeyState(0) && !input.getKeyState(1), "Select shortcuts never also send lane presses");
        keyboard.poll(2_000);
        check(!keyboard.consumeShortcut(MODE_FILTER), "Held shortcut does not repeat");
        snapshot.set(Keys.NUM_5, 1);
        keyboard.poll(3_000);
        check(keyboard.isShortcutHeld(OPTIONS), "Options keeps held semantics");
        keyboard.setTextInputMode(true, true);
        snapshot.set(Keys.F1, 1);
        keyboard.poll(4_000);
        check(!keyboard.isShortcutHeld(OPTIONS) && !keyboard.consumeShortcut(MODE_FILTER)
                && !input.isActivated(SHOW_FPS), "IME owns input, including function keys");
        keyboard.setTextInputMode(false);
        snapshot.set(Keys.A, 0);
        keyboard.poll(5_000);
        check(!keyboard.isShortcutHeld(OPTIONS) && !input.isActivated(SHOW_FPS), "IME exit does not carry over a held key");

        keyboard.setShortcutContext(config, PLAY, true);
        input.beginKeyboardJudge(null, false);
        keyboard.poll(6_000);
        check(!input.getKeyState(0), "Entering play does not turn the held shortcut into a note");
        queue.offer(Keys.NUM_1, false);
        queue.offer(Keys.NUM_1, true);
        queue.offer(Keys.NUM_1, false);
        snapshot.set(Keys.NUM_1, 0);
        queue.publish(true, snapshot);
        keyboard.poll(10_000);
        keyboard.poll(25_999);
        check(input.getKeyState(0), "Short numeric tap remains pending for duration");
        keyboard.poll(26_000);
        var batch = new KeyboardJudgeQueue.Batch();
        input.getKeyboardJudgeQueue().drain(batch);
        check(batch.size == 2 && batch.keys[0] == 0 && batch.keys[1] == 0 && batch.pressed[0] && !batch.pressed[1]
                && batch.times[0] == 10_000 && batch.times[1] == 26_000, "Numeric lanes keep FIFO order and judgment times");
        check(!keyboard.consumeShortcut(MODE_FILTER), "Play never dispatches the select shortcut");
        var replay = new ReplayData();
        replay.keyboardEventKeys = input.getKeyboardReplayKeys();
        replay.keylog = input.getKeyInputLog();
        replay.shrink();
        var json = new Json();
        ReplayData loaded = json.fromJson(ReplayData.class, json.toJson(replay));
        check(loaded.validate() && loaded.keylog.length == 2 && loaded.keylog[0].getKeycode() == 0
                && loaded.keylog[0].getTime() == 10_000 && loaded.keylog[1].getTime() == 26_000,
                "Replays retain logical keys and original timestamps");

        input.endKeyboardJudge();
        snapshot.set(Keys.NUM_1, 1);
        keyboard.setShortcutContext(config, MUSICSELECT, false);
        keyboard.poll(30_000);
        check(!keyboard.consumeShortcut(MODE_FILTER), "Returning to select ignores a held number");
        snapshot.set(Keys.NUM_1, 0);
        keyboard.poll(31_000);
        snapshot.set(Keys.NUM_1, 1);
        keyboard.poll(32_000);
        check(keyboard.consumeShortcut(MODE_FILTER), "New press after returning to select works");
        keyboard.setShortcutContext(config, CONFIG, false);
        keyboard.setShortcutEditorActive(true);
        snapshot.set(Keys.F1, 0);
        keyboard.poll(32_200);
        snapshot.set(Keys.F1, 1);
        snapshot.set(Keys.CONTROL_LEFT, 1);
        keyboard.keyDown(Keys.F1);
        keyboard.poll(32_500);
        check(keyboard.getLastPressedKey() == Keys.F1 && keyboard.getLastPressedModifiers() == MASK_CTRL
                && !input.isActivated(SHOW_FPS), "Capture stores modifiers without executing global shortcuts");
        keyboard.setShortcutEditorActive(false);
        snapshot.set(Keys.NUM_1, 0);
        keyboard.poll(33_000);
        snapshot.set(Keys.NUM_1, 1);
        keyboard.poll(34_000);
        check(input.isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.NUM1)
                && !input.getKeyState(0), "Outside live play fixed numeric controls still work without lane events");
        snapshot.set(Keys.NUM_7, 1);
        keyboard.poll(35_000);
        keyboard.suppressHeldControlKeys();
        keyboard.poll(36_000);
        check(!input.isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.NUM7),
                "Finishing assignment with 7 does not also reset the keyboard layout");
        snapshot.set(Keys.NUM_7, 0);
        keyboard.poll(37_000);
        snapshot.set(Keys.NUM_7, 1);
        keyboard.poll(38_000);
        check(input.isControlKeyPressed(KeyBoardInputProcesseor.ControlKeys.NUM7),
                "Explicit reset after release is still available");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
