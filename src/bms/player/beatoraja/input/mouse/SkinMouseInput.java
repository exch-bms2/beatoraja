package bms.player.beatoraja.input.mouse;

import java.util.Arrays;

import com.badlogic.gdx.InputAdapter;

/** Observes presses without consuming events handled by the skin or Stage. */
public final class SkinMouseInput extends InputAdapter {
    private final boolean[] pending = new boolean[5];
    private final boolean[] justPressed = new boolean[5];

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (button >= 0 && button < pending.length) pending[button] = true;
        return false;
    }

    public void beginFrame() {
        System.arraycopy(pending, 0, justPressed, 0, pending.length);
        Arrays.fill(pending, false);
    }

    public boolean isJustPressed(int button) {
        return button >= 0 && button < justPressed.length && justPressed[button];
    }
}
