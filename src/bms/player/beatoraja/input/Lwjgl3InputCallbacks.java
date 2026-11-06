package bms.player.beatoraja.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWCharCallback;

/** Captures keyboard edges and optional diagnostics, always forwarding the original libGDX callbacks. */
final class Lwjgl3InputCallbacks {
    final long handle;
    private GLFWKeyCallback originalKey;
    private GLFWMouseButtonCallback originalButton;
    private GLFWCursorPosCallback originalCursor;
    private final GLFWKeyCallback key;
    private final GLFWMouseButtonCallback button;
    private final GLFWCursorPosCallback cursor;
    private GLFWWindowFocusCallback originalFocus;
    private GLFWWindowIconifyCallback originalIconify;
    private final GLFWWindowFocusCallback focus;
    private final GLFWWindowIconifyCallback iconify;
    private boolean windowCallbacksAttached;
    private GLFWCharCallback originalCharacter;
    private final GLFWCharCallback character;
    private int lastSupplementary;
    private int fallbackPressed;
    private int knownAliasPressed;

    Lwjgl3InputCallbacks(long handle, PollingLwjgl3Input input, InputDiagnostics diagnostics) {
        this.handle = handle;
        key = GLFWKeyCallback.create((window, code, scancode, action, mods) -> {
            int resolved = input.resolveKeyboardKey(code, scancode);
            int mapped = input.getGdxKeyCode(resolved);
            if (input.isWindowsKeyboardAlias(mapped)) {
                // GLFW cannot track/repeat UNKNOWN keys itself. Mirror its known-key bookkeeping.
                int bit = mapped == WindowsKeyboardMapping.RO ? 1 : 2;
                boolean fallback = resolved != code;
                int held = fallback ? fallbackPressed : knownAliasPressed;
                boolean wasPressed = input.isKeyPressed(mapped);
                if (action == GLFW.GLFW_PRESS) {
                    if ((held & bit) != 0) action = GLFW.GLFW_REPEAT;
                    else held |= bit;
                } else if (action == GLFW.GLFW_RELEASE) {
                    if (fallback && (held & bit) == 0) return;
                    held &= ~bit;
                } else if (fallback && action == GLFW.GLFW_REPEAT && (held & bit) == 0) return;
                if (fallback) fallbackPressed = held;
                else knownAliasPressed = held;
                // Yen aliases the existing Backslash ID. One physical source cannot release the other.
                if (action == GLFW.GLFW_RELEASE && (!wasPressed || ((fallbackPressed | knownAliasPressed) & bit) != 0)) return;
                if ((action == GLFW.GLFW_PRESS && wasPressed) || (action == GLFW.GLFW_REPEAT && !wasPressed)) return;
            }
            long eventTime = diagnostics != null ? System.nanoTime() : 0;
            if (mapped != com.badlogic.gdx.Input.Keys.UNKNOWN
                    && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE))
                input.keyboardEdge(mapped, action == GLFW.GLFW_PRESS, eventTime);
            if (diagnostics != null) {
                if (mapped == com.badlogic.gdx.Input.Keys.UNKNOWN) diagnostics.unknownKey();
                else if (action == GLFW.GLFW_REPEAT) diagnostics.repeat();
                else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)
                    diagnostics.edge(mapped, action == GLFW.GLFW_PRESS, eventTime);
            }
            if (action == GLFW.GLFW_REPEAT && lastSupplementary != 0) {
                originalCharacter.invoke(window, Character.highSurrogate(lastSupplementary));
                originalCharacter.invoke(window, Character.lowSurrogate(lastSupplementary));
            } else if (originalKey != null) {
                if (action == GLFW.GLFW_PRESS) lastSupplementary = 0;
                // Feed the same alias to libGDX's held state and InputProcessor/key configuration.
                originalKey.invoke(window, resolved, scancode, action, mods);
            }
        });
        button = diagnostics == null ? null : GLFWMouseButtonCallback.create((window, code, action, mods) -> {
            if (code >= 0 && code < 5)
                diagnostics.edge(InputDiagnostics.KEY_COUNT + code, action == GLFW.GLFW_PRESS, System.nanoTime());
            if (originalButton != null) originalButton.invoke(window, code, action, mods);
        });
        cursor = diagnostics == null ? null : GLFWCursorPosCallback.create((window, x, y) -> {
            long now = System.nanoTime();
            diagnostics.cursorCallback(now);
            diagnostics.mouseScratch().cursor(x, y, now);
            if (originalCursor != null) originalCursor.invoke(window, x, y);
        });
        originalKey = GLFW.glfwSetKeyCallback(handle, key);
        character = GLFWCharCallback.create((window, codepoint) -> {
            if (originalCharacter == null || !Character.isValidCodePoint(codepoint)) return;
            lastSupplementary = Character.isSupplementaryCodePoint(codepoint) ? codepoint : 0;
            // libGDX 1.14.2 narrows UTF-32 directly to char; keep supplementary characters in its ordered queue.
            if (Character.isSupplementaryCodePoint(codepoint)) {
                originalCharacter.invoke(window, Character.highSurrogate(codepoint));
                originalCharacter.invoke(window, Character.lowSurrogate(codepoint));
            } else originalCharacter.invoke(window, codepoint);
        });
        originalCharacter = GLFW.glfwSetCharCallback(handle, character);
        focus = GLFWWindowFocusCallback.create((window, focused) -> {
            if (originalFocus != null) originalFocus.invoke(window, focused);
            input.windowFocus(focused);
            if (!focused) releaseFallbackKeys();
        });
        iconify = GLFWWindowIconifyCallback.create((window, minimized) -> {
            if (originalIconify != null) originalIconify.invoke(window, minimized);
            input.windowIconified(minimized);
            if (minimized) releaseFallbackKeys();
        });
        if (button != null) originalButton = GLFW.glfwSetMouseButtonCallback(handle, button);
        if (cursor != null) originalCursor = GLFW.glfwSetCursorPosCallback(handle, cursor);
    }

    private void releaseFallbackKeys() {
        // GLFW's focus-loss releases cover only keys in its own known-key table.
        if ((fallbackPressed & 1) != 0) key.invoke(handle, GLFW.GLFW_KEY_UNKNOWN, 0x73, GLFW.GLFW_RELEASE, 0);
        if ((fallbackPressed & 2) != 0) key.invoke(handle, GLFW.GLFW_KEY_UNKNOWN, 0x7d, GLFW.GLFW_RELEASE, 0);
    }

    void attachWindowCallbacks() {
        // Lwjgl3Window installs its callbacks after createInput(), so chain them on the first update.
        if (windowCallbacksAttached) return;
        originalFocus = GLFW.glfwSetWindowFocusCallback(handle, focus);
        originalIconify = GLFW.glfwSetWindowIconifyCallback(handle, iconify);
        windowCallbacksAttached = true;
    }

    void close(boolean restore) {
        releaseFallbackKeys();
        if (restore) {
            GLFW.glfwSetKeyCallback(handle, originalKey);
            GLFW.glfwSetCharCallback(handle, originalCharacter);
            if (windowCallbacksAttached) {
                GLFW.glfwSetWindowFocusCallback(handle, originalFocus);
                GLFW.glfwSetWindowIconifyCallback(handle, originalIconify);
            }
            if (button != null) GLFW.glfwSetMouseButtonCallback(handle, originalButton);
            if (cursor != null) GLFW.glfwSetCursorPosCallback(handle, originalCursor);
        }
        key.free();
        character.free();
        focus.free();
        iconify.free();
        if (button != null) button.free();
        if (cursor != null) cursor.free();
    }
}
