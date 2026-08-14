package bms.player.beatoraja.input;

import com.badlogic.gdx.Gdx;

import java.lang.reflect.Method;

/**
 * lwjgl already has several JNI accessing windows APIs.
 * Rather than maintaining our own JNI classes, we can just "use" them.
 */
public class WinNativeMethods {
    /// HWND GetForegroundWindow();
    private static Method lwjgl_getForegroundWindow = null;
    private static Long foregroundWindowCache = null;

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

        if (foregroundWindowCache == null) {
            try {
                foregroundWindowCache = (long) lwjgl_getForegroundWindow.invoke(null);
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        foregroundWindowCache = null;
                    }
                });
                return foregroundWindowCache;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        } else {
            return foregroundWindowCache;
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
