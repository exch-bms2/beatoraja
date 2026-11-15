package bms.player.beatoraja.input.keyboard;

import bms.player.beatoraja.MainState.MainStateType;
import bms.player.beatoraja.PlayerConfig;
import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

import com.badlogic.gdx.Input.Keys;

import static bms.player.beatoraja.input.keyboard.KeyboardShortcut.*;

/** One action per physical press, independent of the gameplay FIFO. */
final class KeyboardShortcutInput {
    private static final int KEY_COUNT = 256;
    private final KeyboardShortcut[] exact = new KeyboardShortcut[CHORD_COUNT];
    private final KeyboardShortcut[] legacy = new KeyboardShortcut[CHORD_COUNT];
    private final KeyboardShortcut[] active = new KeyboardShortcut[KEY_COUNT];
    private final boolean[] owns = new boolean[KEY_COUNT];
    private final boolean[] down = new boolean[KEY_COUNT];
    private final boolean[] pressed = new boolean[KeyboardShortcut.values().length];
    private final int[] keys = new int[KEY_COUNT];
    private int keyCount;

    void configure(PlayerConfig config, MainStateType screen, IntPredicate allowed, IntPredicate physicalState) {
        Arrays.fill(exact, null);
        Arrays.fill(legacy, null);
        Arrays.fill(owns, false);
        keyCount = 0;
        if (config != null && screen != null) for (var action : KeyboardShortcut.values()) {
            int binding = config.getShortcutBinding(action);
            if (binding == DISABLED || !action.activeIn(screen) || !allowed.test(key(binding))) continue;
            exact[binding] = action;
            if (binding == action.defaultBinding()) legacy[binding] = action;
            int key = key(binding);
            if (!owns[key]) keys[keyCount++] = key;
            owns[key] = true;
        }
        reset(physicalState);
    }

    boolean owns(int key) { return key >= 0 && key < KEY_COUNT && owns[key]; }

    void reset(IntPredicate physicalState) {
        Arrays.fill(active, null);
        Arrays.fill(pressed, false);
        for (int i = 0; i < keyCount; i++) down[keys[i]] = physicalState.test(keys[i]);
    }

    KeyboardShortcut match(int key, int mods) {
        if (!owns(key) || isRecoveryChord(key, mods)) return null;
        var action = exact[chord(key, mods)];
        return action != null ? action : legacy[legacyChord(key, mods)];
    }

    void poll(IntPredicate physicalState, IntSupplier modifiers, boolean legacyTextMode) {
        int mods = -1;
        for (int i = 0; i < keyCount; i++) {
            int key = keys[i];
            boolean state = physicalState.test(key);
            if (state == down[key]) continue;
            down[key] = state;
            if (state) {
                if (mods == -1) mods = modifiers.getAsInt();
                var action = match(key, mods);
                if (legacyTextMode && (key < Keys.F1 || key > Keys.F12 || action == null
                        || legacy[legacyChord(key, mods)] != action)) action = null;
                active[key] = action;
                if (action != null) pressed[action.ordinal()] = true;
            } else {
                var action = active[key];
                if (action != null) pressed[action.ordinal()] = false;
                active[key] = null;
            }
        }
    }

    boolean held(KeyboardShortcut action, int binding) {
        return binding >= 0 && active[key(binding)] == action;
    }

    boolean consume(KeyboardShortcut action) {
        int i = action.ordinal();
        boolean result = pressed[i];
        pressed[i] = false;
        return result;
    }
}
