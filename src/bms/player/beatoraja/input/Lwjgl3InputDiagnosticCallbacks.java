package bms.player.beatoraja.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

/** Observes native callbacks before libGDX coalesces state, always forwarding to its original callbacks. */
final class Lwjgl3InputDiagnosticCallbacks {
    final long handle;
    private GLFWKeyCallback originalKey;
    private GLFWMouseButtonCallback originalButton;
    private GLFWCursorPosCallback originalCursor;
    private final GLFWKeyCallback key;
    private final GLFWMouseButtonCallback button;
    private final GLFWCursorPosCallback cursor;

    Lwjgl3InputDiagnosticCallbacks(long handle, PollingLwjgl3Input input, InputDiagnostics diagnostics) {
        this.handle = handle;
        key = GLFWKeyCallback.create((window, code, scancode, action, mods) -> {
            int mapped = input.getGdxKeyCode(code);
            if (mapped == com.badlogic.gdx.Input.Keys.UNKNOWN) diagnostics.unknownKey();
            else if (action == GLFW.GLFW_REPEAT) diagnostics.repeat();
            else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)
                diagnostics.edge(mapped, action == GLFW.GLFW_PRESS, System.nanoTime());
            if (originalKey != null) originalKey.invoke(window, code, scancode, action, mods);
        });
        button = GLFWMouseButtonCallback.create((window, code, action, mods) -> {
            if (code >= 0 && code < 5)
                diagnostics.edge(InputDiagnostics.KEY_COUNT + code, action == GLFW.GLFW_PRESS, System.nanoTime());
            if (originalButton != null) originalButton.invoke(window, code, action, mods);
        });
        cursor = GLFWCursorPosCallback.create((window, x, y) -> {
            diagnostics.cursorCallback(System.nanoTime());
            if (originalCursor != null) originalCursor.invoke(window, x, y);
        });
        originalKey = GLFW.glfwSetKeyCallback(handle, key);
        originalButton = GLFW.glfwSetMouseButtonCallback(handle, button);
        originalCursor = GLFW.glfwSetCursorPosCallback(handle, cursor);
    }

    void close(boolean restore) {
        if (restore) {
            GLFW.glfwSetKeyCallback(handle, originalKey);
            GLFW.glfwSetMouseButtonCallback(handle, originalButton);
            GLFW.glfwSetCursorPosCallback(handle, originalCursor);
        }
        key.free();
        button.free();
        cursor.free();
    }
}
