package bms.player.beatoraja.input.controller;

import com.badlogic.gdx.controllers.ControllerMapping;
import static org.lwjgl.sdl.SDLGamepad.*;

/** Explicit Jamepad 2.30 button/axis IDs; never persist SDL3 enum ordinals or new extra buttons. */
final class SdlGamepadCompatibility {
    static final int[] BUTTONS = {
        SDL_GAMEPAD_BUTTON_SOUTH, SDL_GAMEPAD_BUTTON_EAST, SDL_GAMEPAD_BUTTON_WEST, SDL_GAMEPAD_BUTTON_NORTH,
        SDL_GAMEPAD_BUTTON_BACK, SDL_GAMEPAD_BUTTON_GUIDE, SDL_GAMEPAD_BUTTON_START,
        SDL_GAMEPAD_BUTTON_LEFT_STICK, SDL_GAMEPAD_BUTTON_RIGHT_STICK,
        SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER,
        SDL_GAMEPAD_BUTTON_DPAD_UP, SDL_GAMEPAD_BUTTON_DPAD_DOWN, SDL_GAMEPAD_BUTTON_DPAD_LEFT, SDL_GAMEPAD_BUTTON_DPAD_RIGHT,
        SDL_GAMEPAD_BUTTON_MISC1, SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1, SDL_GAMEPAD_BUTTON_LEFT_PADDLE1,
        SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2, SDL_GAMEPAD_BUTTON_LEFT_PADDLE2, SDL_GAMEPAD_BUTTON_TOUCHPAD
    };
    static final int[] AXES = {SDL_GAMEPAD_AXIS_LEFTX, SDL_GAMEPAD_AXIS_LEFTY, SDL_GAMEPAD_AXIS_RIGHTX,
        SDL_GAMEPAD_AXIS_RIGHTY, SDL_GAMEPAD_AXIS_LEFT_TRIGGER, SDL_GAMEPAD_AXIS_RIGHT_TRIGGER};
    static final ControllerMapping MAPPING = new ControllerMapping(0, 1, 2, 3,
            0, 1, 2, 3, 4, 6, 9, -1, 10, -1, 7, 8, 11, 12, 13, 14) { };

    static int legacyButton(int nativeButton) {
        for (int i = 0; i < BUTTONS.length; i++) if (BUTTONS[i] == nativeButton) return i;
        return -1;
    }
    static int dpadButton(int alias) {
        return switch (alias) { case 28 -> 13; case 29 -> 11; case 30 -> 14; case 31 -> 12; default -> -1; };
    }
    static int dpadAlias(int button) {
        return switch (button) { case 13 -> 28; case 11 -> 29; case 14 -> 30; case 12 -> 31; default -> -1; };
    }
    static float normalized(short value) {
        // Jamepad divides both signs by 32767; the existing SDL adapter then clamps.
        return Math.max(-1, Math.min(1, value / 32767f));
    }
}
