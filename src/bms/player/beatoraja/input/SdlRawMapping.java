package bms.player.beatoraja.input;

import java.util.Arrays;
import bms.player.beatoraja.PlayModeConfig.ControllerConfig;

/** Startup/config-change lookup tables. The polling path only indexes fixed primitive arrays. */
final class SdlRawMapping {
    final int[] buttons = new int[32], axes = new int[8];
    final boolean[] inverted = new boolean[8];
    final long[] buttonTargets;

    SdlRawMapping(ControllerConfig config, int buttonCount, int axisCount) {
        if (buttonCount < 0 || buttonCount > 256 || axisCount < 0 || axisCount > 256)
            throw new IllegalArgumentException("Invalid SDL joystick component count");
        buttonTargets = new long[buttonCount];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = mapped(config.getSdlRawButtonMap(), i, buttonCount);
            if (buttons[i] >= 0) buttonTargets[buttons[i]] |= 1L << i;
        }
        for (int i = 0; i < axes.length; i++) {
            axes[i] = mapped(config.getSdlRawAxisMap(), i, axisCount);
            var signs = config.getSdlRawAxisInverted();
            inverted[i] = signs != null && i < signs.length && signs[i];
        }
    }

    private static int mapped(int[] configured, int logical, int count) {
        int physical = configured != null && logical < configured.length ? configured[logical] : logical;
        return physical >= 0 && physical < count ? physical : -1;
    }

    boolean same(SdlRawMapping other) {
        return Arrays.equals(buttons, other.buttons) && Arrays.equals(axes, other.axes)
                && Arrays.equals(inverted, other.inverted);
    }

    static boolean hat(int logical, int value) {
        return switch (logical) {
            case 28 -> (value & 8) != 0;
            case 29 -> (value & 1) != 0;
            case 30 -> (value & 2) != 0;
            case 31 -> (value & 4) != 0;
            default -> false;
        };
    }
}
