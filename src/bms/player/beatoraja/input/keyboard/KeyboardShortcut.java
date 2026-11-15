package bms.player.beatoraja.input.keyboard;

import bms.player.beatoraja.MainState.MainStateType;
import com.badlogic.gdx.Input.Keys;

import java.util.EnumSet;
import java.util.Arrays;

import static bms.player.beatoraja.MainState.MainStateType.*;
import static bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.*;

/** Action names and unmodified key codes are compatible with existing player profiles. */
public enum KeyboardShortcut {
    SEARCH(Keys.NUM_0, "Search", MUSICSELECT),
    MODE_FILTER(Keys.NUM_1, "Change mode filter", MUSICSELECT),
    SORT(Keys.NUM_2, "Change sort order", MUSICSELECT),
    LN_MODE(Keys.NUM_3, "Change LN mode", MUSICSELECT),
    NEXT_REPLAY(Keys.NUM_4, "Select next replay", MUSICSELECT),
    OPTIONS(Keys.NUM_5, "Open options (hold)", MUSICSELECT),
    KEY_CONFIG(Keys.NUM_6, "Open KeyConfig", MUSICSELECT),
    RIVAL(Keys.NUM_7, "Change rival", MUSICSELECT),
    SAME_FOLDER(Keys.NUM_8, "Show songs in the same folder", MUSICSELECT),
    OPEN_DOCUMENT(Keys.NUM_9, "Open song document", MUSICSELECT),
    UPDATE_FOLDER(Keys.F2, "Update folder / local table", MUSICSELECT),
    OPEN_EXPLORER(Keys.F3, "Open song folder", MUSICSELECT),
    COPY_SONG_MD5_HASH(chord(Keys.F3, MASK_CTRL), "Copy MD5", MUSICSELECT),
    COPY_SONG_SHA256_HASH(chord(Keys.F3, MASK_CTRL | MASK_SHIFT), "Copy SHA-256", MUSICSELECT),
    SWITCH_PLAY_SIDE(Keys.F5, "Switch play side", MUSICSELECT),
    ADD_FAVORITE_SONG(Keys.F8, "Toggle favorite song", MUSICSELECT),
    ADD_FAVORITE_CHART(Keys.F9, "Toggle favorite chart", MUSICSELECT),
    AUTOPLAY_FOLDER(Keys.F10, "Autoplay folder", MUSICSELECT),
    OPEN_SKIN_CONFIGURATION(Keys.F12, "Open Skin Configuration", MUSICSELECT),
    OPEN_IR(Keys.F11, "Open IR page", MUSICSELECT, RESULT, COURSERESULT),
    SWITCH_RESULT_SCORE(Keys.F7, "Switch result score rule", RESULT),
    SWITCH_SKIN_SIDE(Keys.F5, "Switch skin configuration side", SKINCONFIG),
    ADJUST_SKIN(Keys.F12, "Toggle skin adjustment overlay", PLAY),
    SHOW_FPS(Keys.F1, "Toggle FPS display"),
    SWITCH_SCREEN_MODE(Keys.F4, "Toggle fullscreen"),
    SAVE_SCREENSHOT(Keys.F6, "Save screenshot");

    public static final int DISABLED = -1;
    static final int CHORD_COUNT = 256 * 8;
    private final int defaultBinding;
    private final String label;
    private final EnumSet<MainStateType> screens;

    KeyboardShortcut(int defaultBinding, String label, MainStateType... screens) {
        this.defaultBinding = defaultBinding;
        this.label = label;
        this.screens = screens.length == 0 ? EnumSet.allOf(MainStateType.class)
                : EnumSet.copyOf(Arrays.asList(screens));
    }

    public int defaultBinding() { return defaultBinding; }
    public String label() { return label; }
    public boolean activeIn(MainStateType screen) { return screens.contains(screen); }
    public boolean overlaps(KeyboardShortcut other) {
        for (var screen : screens) if (other.activeIn(screen)) return true;
        return false;
    }

    public String scopeLabel() {
        if (screens.size() == MainStateType.values().length) return "Global";
        if (this == OPEN_IR) return "Select / Result";
        return switch (screens.iterator().next()) {
            case MUSICSELECT -> "Music Select";
            case RESULT -> "Result";
            case SKINCONFIG -> "Skin Configuration";
            case PLAY -> "Practice / Autoplay";
            default -> "";
        };
    }

    public static int chord(int key, int modifiers) { return key | (modifiers << 8); }
    public static int key(int chord) { return chord & 255; }
    public static int modifiers(int chord) { return chord >>> 8; }
    public static boolean isDigit(int key) { return key >= Keys.NUM_0 && key <= Keys.NUM_9; }
    public static boolean isModifier(int key) {
        return switch (key) {
            case Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT, Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT,
                    Keys.ALT_LEFT, Keys.ALT_RIGHT, Keys.SYM -> true;
            default -> false;
        };
    }

    public boolean isAssignable(int binding) {
        if (binding == DISABLED) return true;
        if (binding < 0 || binding >= CHORD_COUNT) return false;
        int key = key(binding);
        if (key <= Keys.UNKNOWN || isModifier(key) || key == Keys.TAB
                || (Keys.toString(key) == null && !WindowsKeyboardMapping.isConversionKey(key))) return false;
        if (isRecoveryChord(key, modifiers(binding))) return false;
        if (isDigit(key) && (activeIn(PLAY) || activeIn(RESULT) || activeIn(COURSERESULT) || activeIn(CONFIG))) return false;
        for (var fixed : ControlKeys.values())
            if (fixed.keycode == key && !isDigit(key) && !(key >= Keys.F1 && key <= Keys.F12)) return false;
        return true;
    }

    static boolean isRecoveryChord(int key, int modifiers) {
        return key == Keys.NUM_6 && (modifiers & MASK_CTRL) != 0;
    }

    // Until a binding is changed, preserve the old modifier tolerance. Exact chords always win.
    static int legacyChord(int key, int modifiers) {
        return chord(key, key == Keys.F3 && (modifiers & MASK_CTRL) != 0
                ? modifiers & (MASK_CTRL | MASK_SHIFT) : 0);
    }
}
