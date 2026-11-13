package bms.player.beatoraja.input.controller;

import java.util.Arrays;

/** Separate, snapshot-based capture for choosing a device, never for binding keys. */
final class DeviceButtonCapture {
    private final boolean[] previous = new boolean[256];
    private final boolean[] current = new boolean[256];
    private boolean baseline = true;
    private int pressed = -1;

    void reset() {
        baseline = true;
        pressed = -1;
    }

    void sample(QueuedController controller) {
        Arrays.fill(current, false);
        controller.readSelectionButtons(current);
        for (int i = 0; i < current.length; i++) {
            if (!baseline && pressed < 0 && current[i] && !previous[i]) pressed = i;
            previous[i] = current[i];
        }
        baseline = false;
    }

    int take() {
        int result = pressed;
        pressed = -1;
        return result;
    }

    boolean anyDown() {
        for (boolean down : current) if (down) return true;
        return false;
    }
}
