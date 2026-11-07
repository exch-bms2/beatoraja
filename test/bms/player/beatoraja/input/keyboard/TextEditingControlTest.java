package bms.player.beatoraja.input.keyboard;

import bms.player.beatoraja.input.KeyboardQueueTestSupport;

import java.lang.reflect.Proxy;
import com.badlogic.gdx.*;
import bms.model.Mode;
import bms.player.beatoraja.PlayModeConfig.KeyboardConfig;
import bms.player.beatoraja.input.keyboard.KeyBoardInputProcesseor.ControlKeys;

public final class TextEditingControlTest {
    public static void main(String[] args) throws Exception {
        var old = Gdx.input;
        boolean[] held = new boolean[256];
        Gdx.input = (Input)Proxy.newProxyInstance(TextEditingControlTest.class.getClassLoader(), new Class<?>[]{Input.class},
                (p, m, a) -> m.getName().equals("isKeyPressed") ? held[(int)a[0]]
                        : m.getReturnType() == boolean.class ? false : m.getReturnType() == int.class ? 0 : null);
        try {
            var config = new KeyboardConfig(Mode.BEAT_7K, true); config.getMouseScratchConfig().setMouseScratchEnabled(false);
            var input = KeyboardQueueTestSupport.input(config); var keyboard = input.getKeyBoardInputProcesseor();
            for (var key : new ControlKeys[]{ControlKeys.UP, ControlKeys.DOWN, ControlKeys.LEFT, ControlKeys.RIGHT,
                    ControlKeys.DEL, ControlKeys.ESCAPE, ControlKeys.ENTER, ControlKeys.NUM1}) {
                held[key.keycode] = true; keyboard.poll(1000);
                keyboard.setTextInputMode(true); keyboard.poll(2000);
                check(!input.getControlKeyState(key) && !input.isControlKeyPressed(key), "No editing control leakage: " + key);
                keyboard.setTextInputMode(false); keyboard.poll(3000);
                check(!input.getControlKeyState(key), "Held key remains suppressed on editor exit: " + key);
                held[key.keycode] = false; keyboard.poll(4000);
                held[key.keycode] = true; keyboard.poll(5000);
                check(input.getControlKeyState(key), "New press remains compatible: " + key);
                held[key.keycode] = false; keyboard.poll(6000);
            }
            keyboard.setTextInputMode(true); held[Input.Keys.F1] = true; keyboard.poll(7000);
            check(input.getControlKeyState(ControlKeys.F1), "Existing non-editing function shortcuts retained");
            System.out.println("TextEditingControlTest passed (editing controls, held-key suppression, existing function keys)");
        } finally { Gdx.input = old; }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
