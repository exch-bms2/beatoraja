#!/usr/bin/env python3
"""Apply guarded mechanical hooks to the exact GLFW shipped by LWJGL 3.4.3."""
import argparse
from pathlib import Path

REVISION = "d9d6f0f1f967807ffade6598ea9a631ebaf37a56"


def prepare(source: Path, bridge: Path):
    target = source / "src/x11_window.c"
    text = target.read_text()
    hooks = [
        ("// Process the specified X event\n", '#include "beatoraja-x11-ime.c"\n\n// Process the specified X event\n'),
        ("    filtered = XFilterEvent(event, None);", "    baBefore(event);\n    filtered = XFilterEvent(event, None);\n    baFiltered(filtered);"),
        ("                        while (c - chars < count)\n                            _glfwInputChar(window, decodeUTF8(&c), mods, plain);",
         "                        if (!baCommit(window, chars, count))\n                            while (c - chars < count)\n                                _glfwInputChar(window, decodeUTF8(&c), mods, plain);"),
        ("            return;\n        }\n\n        case KeyRelease:", "            baKey(window, key, mods, 0);\n            return;\n        }\n\n        case KeyRelease:"),
        ("            _glfwInputKey(window, key, keycode, GLFW_RELEASE, mods);", "            _glfwInputKey(window, key, keycode, GLFW_RELEASE, mods);\n            baKey(window, key, mods, 1);"),
        ("            _glfwInputWindowFocus(window, GLFW_FALSE);", "            baFocusLost(window);\n            _glfwInputWindowFocus(window, GLFW_FALSE);"),
        ("            _glfwInputWindowFocus(window, GLFW_TRUE);", "            baFocusGained(window);\n            _glfwInputWindowFocus(window, GLFW_TRUE);"),
        ("void _glfwDestroyWindowX11(_GLFWwindow* window)\n{", "void _glfwDestroyWindowX11(_GLFWwindow* window)\n{\n    baWindowDestroyed(window);"),
    ]
    for before, after in hooks:
        if text.count(before) != 1:
            raise ValueError(f"Unexpected GLFW source; hook must match exactly once: {before[:70]}")
        text = text.replace(before, after)
    # All validation precedes writes; a wrong/repeated source is never partly patched.
    target.write_text(text)
    (source / "src/beatoraja-x11-ime.c").write_bytes(bridge.read_bytes())
    cmake = source / "src/CMakeLists.txt"
    # The new XIM entry points are linked explicitly; GLFW's existing dynamic calls remain intact.
    cmake.write_text(cmake.read_text() + "\n# Altered build: beatoraja opt-in X11 IME.\n"
                     "target_link_libraries(glfw PRIVATE ${X11_X11_LIB})\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("bridge", type=Path)
    args = parser.parse_args()
    prepare(args.source, args.bridge)
