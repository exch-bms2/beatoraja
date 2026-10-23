package bms.player.beatoraja.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/** Captures keyboard edges and optional diagnostics, always forwarding the original libGDX callbacks. */
final class Lwjgl3InputCallbacks {
    final long handle;
    private GLFWKeyCallback originalKey;
    private GLFWMouseButtonCallback originalButton;
    private GLFWCursorPosCallback originalCursor;
    private final GLFWKeyCallback key;
    private final GLFWMouseButtonCallback button;
    private final GLFWCursorPosCallback cursor;

    Lwjgl3InputCallbacks(long handle, PollingLwjgl3Input input, InputDiagnostics diagnostics) {
        this.handle = handle;
        key = GLFWKeyCallback.create((window, code, scancode, action, mods) -> {
            int mapped = input.getGdxKeyCode(code);
            long eventTime = diagnostics != null ? System.nanoTime() : 0;
            if (mapped != com.badlogic.gdx.Input.Keys.UNKNOWN
                    && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE))
                input.getKeyboardEvents().offer(mapped, action == GLFW.GLFW_PRESS, eventTime);
            if (diagnostics != null) {
                if (mapped == com.badlogic.gdx.Input.Keys.UNKNOWN) diagnostics.unknownKey();
                else if (action == GLFW.GLFW_REPEAT) diagnostics.repeat();
                else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)
                    diagnostics.edge(mapped, action == GLFW.GLFW_PRESS, eventTime);
            }
            if (originalKey != null) originalKey.invoke(window, code, scancode, action, mods);
        });
        button = diagnostics == null ? null : GLFWMouseButtonCallback.create((window, code, action, mods) -> {
            if (code >= 0 && code < 5)
                diagnostics.edge(InputDiagnostics.KEY_COUNT + code, action == GLFW.GLFW_PRESS, System.nanoTime());
            if (originalButton != null) originalButton.invoke(window, code, action, mods);
        });
        cursor = diagnostics == null ? null : GLFWCursorPosCallback.create((window, x, y) -> {
            diagnostics.cursorCallback(System.nanoTime());
            if (originalCursor != null) originalCursor.invoke(window, x, y);
        });
        originalKey = GLFW.glfwSetKeyCallback(handle, key);
        if (button != null) originalButton = GLFW.glfwSetMouseButtonCallback(handle, button);
        if (cursor != null) originalCursor = GLFW.glfwSetCursorPosCallback(handle, cursor);
    }

    void close(boolean restore) {
        if (restore) {
            GLFW.glfwSetKeyCallback(handle, originalKey);
            if (button != null) GLFW.glfwSetMouseButtonCallback(handle, originalButton);
            if (cursor != null) GLFW.glfwSetCursorPosCallback(handle, originalCursor);
        }
        key.free();
        if (button != null) button.free();
        if (cursor != null) cursor.free();
    }
}
