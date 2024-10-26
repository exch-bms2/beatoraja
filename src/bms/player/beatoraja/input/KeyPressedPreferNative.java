// Copyright (c) 2024 Park Hyunwoo
//
// This software is provided 'as-is', without any express or implied
// warranty. In no event will the authors be held liable for any damages
// arising from the use of this software.
//
// Permission is granted to anyone to use this software for any purpose,
// including commercial applications, and to alter it and redistribute it
// freely, subject to the following restrictions:
//
// 1. The origin of this software must not be misrepresented; you must not
// claim that you wrote the original software. If you use this software
// in a product, an acknowledgment in the product documentation would be
// appreciated but is not required.
// 2. Altered source versions must be plainly marked as such, and must not be
// misrepresented as being the original software.
// 3. This notice may not be removed or altered from any source distribution.

package bms.player.beatoraja.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Method;

import static bms.player.beatoraja.input.WinNativeMethods.GetForegroundWindow;
import static bms.player.beatoraja.input.WinNativeMethods.isKeyPressedAsync;
import static bms.player.beatoraja.input.WinVKCode.*;

/**
 * Performant key getter for beatoraja
 * Gdx.input.isKeyPressed uses GLFW event system to process keys, so in case of
 * delay or CPU overuse, it might not be performant enough to process keys ASAP.
 * <p>
 * Windows has `User32.GetAsyncKeyState` as a faster alternative, so this library
 * wraps around the api for libgdx.
 */
public class KeyPressedPreferNative {
    private static long beatorajaHWND = 0;

    /**
     * Get HWND pointer value of beatoraja window.
     *
     * @return
     */
    private static long windowsGetBeatorajaHWND() {
        if (beatorajaHWND == 0) {
            // Reflection hack.
            try {
                Method getImplementationMethod = Display.class.getDeclaredMethod("getImplementation");
                getImplementationMethod.setAccessible(true);
                Object implementation = getImplementationMethod.invoke(null);
                Class<?> windowsDisplayClass = Class.forName("org.lwjgl.opengl.WindowsDisplay");

                if (!windowsDisplayClass.isInstance(implementation)) {
                    throw new Exception("The current platform must be Windows!");
                }

                Method getHwndMethod = windowsDisplayClass.getDeclaredMethod("getHwnd");
                getHwndMethod.setAccessible(true);

                beatorajaHWND = (long) getHwndMethod.invoke(implementation);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return beatorajaHWND;
    }

    private static boolean isKeyPressedAsyncVK(WinVKCode vk) {
        return isKeyPressedAsync(vk.code);
    }

    private static boolean windowsIsKeyPressed(int gdxKey) {
        // Note: GetAsyncKeyState checks if the key is pressed regardless of whether the application window
        // is in focus or not. This may severely interfere with user usability when beatoraja is NOT
        // in foreground. (e.g minimized). We check if the beatoraja is the focused window before
        // using GetAsyncKeyState.
        long foregroundWindow = GetForegroundWindow();
        if (foregroundWindow != windowsGetBeatorajaHWND()) {
            return Gdx.input.isKeyPressed(gdxKey);
        }

        // Key list reference: https://github.com/libgdx/libgdx/blob/1.8.0/backends/gdx-backend-lwjgl/src/com/badlogic/gdx/backends/lwjgl/LwjglInput.java
        // Vkey reference: https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/opengl/WindowsKeycodes.java
        switch (gdxKey) {
            case Input.Keys.LEFT_BRACKET:
                return isKeyPressedAsyncVK(VK_OEM_4);
            case Input.Keys.RIGHT_BRACKET:
                return isKeyPressedAsyncVK(VK_OEM_6);
            case Input.Keys.GRAVE:
                return isKeyPressedAsyncVK(VK_OEM_8);
            case Input.Keys.STAR:
                return isKeyPressedAsyncVK(VK_MULTIPLY);
            case Input.Keys.NUM:
                return isKeyPressedAsyncVK(VK_NUMLOCK);
            case Input.Keys.PERIOD:
                return isKeyPressedAsyncVK(VK_OEM_PERIOD);
            case Input.Keys.SLASH:
                return isKeyPressedAsyncVK(VK_OEM_2);
            case Input.Keys.SYM:
                return isKeyPressedAsyncVK(VK_RWIN);
            case Input.Keys.EQUALS:
                return isKeyPressedAsyncVK(VK_OEM_PLUS);
            case Input.Keys.COMMA:
                return isKeyPressedAsyncVK(VK_OEM_COMMA);
            case Input.Keys.ENTER:
                return isKeyPressedAsyncVK(VK_RETURN);
            case Input.Keys.NUM_0:
            case Input.Keys.NUM_1:
            case Input.Keys.NUM_2:
            case Input.Keys.NUM_3:
            case Input.Keys.NUM_4:
            case Input.Keys.NUM_5:
            case Input.Keys.NUM_6:
            case Input.Keys.NUM_7:
            case Input.Keys.NUM_8:
            case Input.Keys.NUM_9:
                return isKeyPressedAsync(0x30 + gdxKey - Input.Keys.NUM_0);
            case Input.Keys.A:
            case Input.Keys.B:
            case Input.Keys.C:
            case Input.Keys.D:
            case Input.Keys.E:
            case Input.Keys.F:
            case Input.Keys.G:
            case Input.Keys.H:
            case Input.Keys.I:
            case Input.Keys.J:
            case Input.Keys.K:
            case Input.Keys.L:
            case Input.Keys.M:
            case Input.Keys.N:
            case Input.Keys.O:
            case Input.Keys.P:
            case Input.Keys.Q:
            case Input.Keys.R:
            case Input.Keys.S:
            case Input.Keys.T:
            case Input.Keys.U:
            case Input.Keys.V:
            case Input.Keys.W:
            case Input.Keys.X:
            case Input.Keys.Y:
            case Input.Keys.Z:
                return isKeyPressedAsync(0x41 + gdxKey - Input.Keys.A);

            // Some people *might* want to use control keys for gameplays, so
            // unlike F1~F12 keys we just use GetAsyncKeyState here.
            case Input.Keys.ALT_LEFT:
                return isKeyPressedAsyncVK(VK_LMENU);
            case Input.Keys.ALT_RIGHT:
                return isKeyPressedAsyncVK(VK_RMENU);
            case Input.Keys.BACKSLASH:
                return isKeyPressedAsyncVK(VK_OEM_5);
            case Input.Keys.FORWARD_DEL:
                return isKeyPressedAsyncVK(VK_DELETE);
            case Input.Keys.DPAD_LEFT:
                return isKeyPressedAsyncVK(VK_LEFT);
            case Input.Keys.DPAD_RIGHT:
                return isKeyPressedAsyncVK(VK_RIGHT);
            case Input.Keys.DPAD_UP:
                return isKeyPressedAsyncVK(VK_UP);
            case Input.Keys.DPAD_DOWN:
                return isKeyPressedAsyncVK(VK_DOWN);
            case Input.Keys.HOME:
                return isKeyPressedAsyncVK(VK_HOME);
            case Input.Keys.MINUS:
                return isKeyPressedAsyncVK(VK_SUBTRACT);
            case Input.Keys.PLUS:
                return isKeyPressedAsyncVK(VK_ADD);
            case Input.Keys.SEMICOLON:
            case Input.Keys.COLON:
                return isKeyPressedAsyncVK(VK_OEM_1);
            case Input.Keys.SHIFT_LEFT:
                return isKeyPressedAsyncVK(VK_LSHIFT);
            case Input.Keys.SHIFT_RIGHT:
                return isKeyPressedAsyncVK(VK_RSHIFT);
            case Input.Keys.SPACE:
                return isKeyPressedAsyncVK(VK_SPACE);
            case Input.Keys.TAB:
                return isKeyPressedAsyncVK(VK_TAB);
            case Input.Keys.CONTROL_LEFT:
                return isKeyPressedAsyncVK(VK_LCONTROL);
            case Input.Keys.CONTROL_RIGHT:
                return isKeyPressedAsyncVK(VK_RCONTROL);
            case Input.Keys.PAGE_DOWN:
                return isKeyPressedAsyncVK(VK_NEXT);
            case Input.Keys.PAGE_UP:
                return isKeyPressedAsyncVK(VK_PRIOR);
            case Input.Keys.ESCAPE:
                return isKeyPressedAsyncVK(VK_ESCAPE);
            case Input.Keys.END:
                return isKeyPressedAsyncVK(VK_END);
            case Input.Keys.INSERT:
                return isKeyPressedAsyncVK(VK_INSERT);
            case Input.Keys.DEL:
                return isKeyPressedAsyncVK(VK_BACK);
            case Input.Keys.APOSTROPHE:
                return isKeyPressedAsyncVK(VK_OEM_7);
            case Input.Keys.F1:
            case Input.Keys.F2:
            case Input.Keys.F3:
            case Input.Keys.F4:
            case Input.Keys.F5:
            case Input.Keys.F6:
            case Input.Keys.F7:
            case Input.Keys.F8:
            case Input.Keys.F9:
            case Input.Keys.F10:
            case Input.Keys.F11:
            case Input.Keys.F12: {
                if (isKeyPressedAsync(VK_F1.code + (gdxKey - Input.Keys.F1))) {
                    // If we just use GetAsyncKeyState for polling F(\d+) keys,
                    // common keystrokes like Alt+F4 would not work.
                    // I suspect nobody would use function keys for gaming, so
                    // some delay in processing keys won't matter that much?
                    return Gdx.input.isKeyPressed(gdxKey);
                }
                return false;
            }
            case Input.Keys.NUMPAD_0:
            case Input.Keys.NUMPAD_1:
            case Input.Keys.NUMPAD_2:
            case Input.Keys.NUMPAD_3:
            case Input.Keys.NUMPAD_4:
            case Input.Keys.NUMPAD_5:
            case Input.Keys.NUMPAD_6:
            case Input.Keys.NUMPAD_7:
            case Input.Keys.NUMPAD_8:
            case Input.Keys.NUMPAD_9:
                return isKeyPressedAsync(gdxKey - Input.Keys.NUMPAD_0 + VK_NUMPAD0.code);
            default:  // Fallback
                // TODO: above list should be exhaustive. Show error
                return Gdx.input.isKeyJustPressed(gdxKey);
        }
    }

    public static boolean isKeyPressed(int gdxKey) {
        int platform = LWJGLUtil.getPlatform();
        if (platform == LWJGLUtil.PLATFORM_WINDOWS) {
            return windowsIsKeyPressed(gdxKey);
        } else {
            return Gdx.input.isKeyPressed(gdxKey);
        }
    }
}
