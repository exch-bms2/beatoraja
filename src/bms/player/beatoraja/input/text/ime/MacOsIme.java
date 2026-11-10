package bms.player.beatoraja.input.text.ime;

import bms.player.beatoraja.input.MacOsImeBridge;
import bms.player.beatoraja.input.text.ImeBridge;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFWNativeCocoa;

import java.nio.file.Path;
import java.util.logging.Logger;

/** Selects the opt-in AppKit editor without initializing JavaFX or AWT. */
public final class MacOsIme {
    private static boolean loaded;

    private MacOsIme() { }

    public static ImeBridge create() {
        return create(true);
    }

    public static ImeBridge create(boolean configured) {
        if (!ImeBridge.isEnabled("beatoraja.macosIme", configured) || !System.getProperty("os.name", "").startsWith("Mac")
                || !(Gdx.graphics instanceof Lwjgl3Graphics graphics)) return ImeBridge.NONE;
        try {
            load();
            return new MacOsImeBridge(GLFWNativeCocoa.glfwGetCocoaWindow(graphics.getWindow().getWindowHandle()));
        } catch (LinkageError | RuntimeException failure) {
            Logger.getGlobal().warning("macOS IME prototype unavailable; using GLFW text input: " + failure);
            return ImeBridge.NONE;
        }
    }

    private static synchronized void load() {
        if (!loaded) {
            System.load(Path.of(System.getProperty("beatoraja.macosIme.library",
                    "natives/libbeatoraja-ime.dylib")).toAbsolutePath().toString());
            loaded = true;
        }
    }
}
