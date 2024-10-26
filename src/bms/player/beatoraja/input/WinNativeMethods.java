package bms.player.beatoraja.input;

import java.lang.reflect.Method;

/**
 * lwjgl already has several JNI accessing windows APIs.
 * Rather than maintaining our own JNI classes, we can just "use" them.
 */
public class WinNativeMethods {
    /// HWND GetForegroundWindow();
    static Method lwjgl_getForegroundWindow = null;

    static long GetForegroundWindow() {
        if (lwjgl_getForegroundWindow == null) {
            try {
                Class<?> windowsKeyboardClass = Class.forName("org.lwjgl.opengl.WindowsDisplay");
                Method getForegroundWindow = windowsKeyboardClass.getDeclaredMethod("getForegroundWindow");
                getForegroundWindow.setAccessible(true);
                lwjgl_getForegroundWindow = getForegroundWindow;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        try {
            return (long) lwjgl_getForegroundWindow.invoke(null);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /// bool isKeyPressedAsync

    static Method lwjgl_isKeyPressedAsync = null;
    public static boolean isKeyPressedAsync(int vKey) {
        if (lwjgl_isKeyPressedAsync == null) {
            try {
                Class<?> windowsKeyboardClass = Class.forName("org.lwjgl.opengl.WindowsKeyboard");
                Method isKeyPressedAsync = windowsKeyboardClass.getDeclaredMethod("isKeyPressedAsync", int.class);
                isKeyPressedAsync.setAccessible(true);
                lwjgl_isKeyPressedAsync = isKeyPressedAsync;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            return (Boolean) lwjgl_isKeyPressedAsync.invoke(null, vKey);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
