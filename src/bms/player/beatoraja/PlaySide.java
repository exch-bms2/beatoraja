package bms.player.beatoraja;

import bms.model.Mode;
import bms.player.beatoraja.skin.SkinType;

/** Physical side for single play; it does not change the chart mode or logical lanes. */
public enum PlaySide {
    P1("1P"), P2("2P");

    private final String label;

    PlaySide(String label) { this.label = label; }

    public String label() { return label; }

    public PlaySide other() { return this == P1 ? P2 : P1; }

    public static boolean supports(Mode mode) { return index(mode) >= 0; }

    public static boolean supports(SkinType type) {
        return type == SkinType.PLAY_5KEYS || type == SkinType.PLAY_7KEYS || type == SkinType.PLAY_24KEYS;
    }

    static int index(Mode mode) {
        if (mode == Mode.BEAT_5K) return 0;
        if (mode == Mode.BEAT_7K) return 1;
        if (mode == Mode.KEYBOARD_24K) return 2;
        return -1;
    }
}
