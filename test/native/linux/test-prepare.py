"""Guarded source-hook regressions. No downloads, compiler or X server required."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location("prepare_x11", ROOT / "scripts/prepare-x11-ime.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

FIXTURE = """// Process the specified X event
    filtered = XFilterEvent(event, None);
                        while (c - chars < count)
                            _glfwInputChar(window, decodeUTF8(&c), mods, plain);
            return;
        }

        case KeyRelease:
            _glfwInputKey(window, key, keycode, GLFW_RELEASE, mods);
            _glfwInputWindowFocus(window, GLFW_FALSE);
            _glfwInputWindowFocus(window, GLFW_TRUE);
void _glfwDestroyWindowX11(_GLFWwindow* window)
{
}
"""


class PrepareTest(unittest.TestCase):
    def fixture(self, source, text=FIXTURE):
        (source / "src").mkdir()
        (source / "src/x11_window.c").write_text(text)
        (source / "src/CMakeLists.txt").write_text("# upstream\n")
        bridge = source / "bridge.c"
        bridge.write_text("/* prototype */\n")
        return bridge

    def test_all_hooks_and_repeated_application(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            bridge = self.fixture(source)
            MODULE.prepare(source, bridge)
            text = (source / "src/x11_window.c").read_text()
            for hook in ("baBefore(event)", "baFiltered(filtered)", "baCommit(window", "baKey(window, key, mods, 0)",
                         "baKey(window, key, mods, 1)", "baFocusLost(window)", "baFocusGained(window)", "baWindowDestroyed(window)"):
                self.assertEqual(text.count(hook), 1)
            self.assertEqual((source / "src/beatoraja-x11-ime.c").read_bytes(), bridge.read_bytes())
            self.assertIn("${X11_X11_LIB}", (source / "src/CMakeLists.txt").read_text())
            with self.assertRaises(ValueError):
                MODULE.prepare(source, bridge)
            self.assertEqual((source / "src/x11_window.c").read_text(), text)

    def test_wrong_source_is_not_partially_modified(self):
        for text in (FIXTURE.replace("case KeyRelease:", "case Wrong:"), FIXTURE + FIXTURE):
            with self.subTest(), tempfile.TemporaryDirectory() as directory:
                source = Path(directory)
                bridge = self.fixture(source, text)
                with self.assertRaises(ValueError):
                    MODULE.prepare(source, bridge)
                self.assertEqual((source / "src/x11_window.c").read_text(), text)
                self.assertFalse((source / "src/beatoraja-x11-ime.c").exists())


if __name__ == "__main__":
    unittest.main()
