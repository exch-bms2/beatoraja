package bms.player.beatoraja.input.text.ime;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.windows.WindowsLibrary;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Keeps the IME detached from the game window except while an in-game text field has focus.
 * <p>
 * While an IME is in a native-input mode (Korean Hangul, Japanese kana, ...), Windows replaces the
 * virtual key of every key the IME processes with VK_PROCESSKEY. GLFW drops those key events, so
 * letter keys bound to lanes never reach the game (#404). With no input context associated with the
 * window, the IME does not process keys and GLFW receives the physical keys.
 * <p>
 * Disable with {@code -Dbeatoraja.windowsGameplayIme=false}.
 */
public final class WindowsGameplayIme {
	static final String PROPERTY = "beatoraja.windowsGameplayIme";

	/** Win32 boundary: associate the window's default input context, or none. */
	interface Platform {
		boolean associate(boolean enabled);
		void close();
	}

	private static WindowsGameplayIme instance;

	private final Platform platform;
	private int textInputs;
	private boolean detached;
	private boolean warned;

	WindowsGameplayIme(Platform platform) {
		this.platform = platform;
		detach();
	}

	/** Called once on the GLFW owner thread after the game window exists. */
	public static synchronized void install() {
		if (instance != null || !Boolean.parseBoolean(System.getProperty(PROPERTY, "true"))
				|| !System.getProperty("os.name", "").startsWith("Windows")
				|| !(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
			return;
		}
		try {
			long hwnd = GLFWNativeWin32.glfwGetWin32Window(graphics.getWindow().getWindowHandle());
			if (hwnd == 0) return;
			instance = new WindowsGameplayIme(new Native(hwnd));
			Logger.getGlobal().info("Windows IME: detached from the game window outside text input");
		} catch (RuntimeException | LinkageError e) {
			Logger.getGlobal().log(Level.WARNING, "Windows IME: cannot detach from the game window", e);
		}
	}

	/** Re-attaches the IME before a text field starts its session. */
	public static synchronized void textInputBegin() {
		if (instance != null) instance.begin();
	}

	/** Detaches the IME again once the last text field has closed. */
	public static synchronized void textInputEnd() {
		if (instance != null) instance.end();
	}

	/** Restores the window's default input context and releases native libraries. */
	public static synchronized void uninstall() {
		if (instance == null) return;
		try { instance.close(); } finally { instance = null; }
	}

	void begin() {
		if (textInputs++ == 0 && detached) {
			if (associate(true)) detached = false;
		}
	}

	void end() {
		if (textInputs == 0) return;
		if (--textInputs == 0) detach();
	}

	void close() {
		try {
			if (detached && associate(true)) detached = false;
		} finally {
			platform.close();
		}
	}

	boolean isDetached() { return detached; }

	private void detach() {
		if (!detached && associate(false)) detached = true;
	}

	private boolean associate(boolean enabled) {
		try {
			if (platform.associate(enabled)) return true;
			warn("ImmAssociateContextEx refused", null);
		} catch (RuntimeException | LinkageError e) {
			warn("ImmAssociateContextEx failed", e);
		}
		return false;
	}

	private void warn(String message, Throwable error) {
		if (warned) return;
		warned = true;
		Logger.getGlobal().log(Level.WARNING, "Windows IME: " + message, error);
	}

	/** imm32 loaded from System32 (never cwd/PATH); every call runs on the window's thread. */
	private static final class Native implements Platform {
		private static final int IACE_DEFAULT = 0x10;
		private final long hwnd;
		private final SharedLibrary kernel, imm;
		private final long associateContextEx;

		Native(long hwnd) {
			this.hwnd = hwnd;
			kernel = new WindowsLibrary("kernel32.dll");
			SharedLibrary loaded = null;
			try {
				String directory;
				try (var stack = MemoryStack.stackPush()) {
					ByteBuffer buffer = stack.calloc(520);
					int count = callPI(memAddress(buffer), 260, function(kernel, "GetSystemDirectoryW"));
					if (count <= 0 || count >= 260) throw new IllegalStateException("Cannot resolve System32");
					directory = memUTF16(memAddress(buffer), count);
				}
				loaded = new WindowsLibrary(Path.of(directory, "imm32.dll").toString());
				associateContextEx = function(loaded, "ImmAssociateContextEx");
				imm = loaded;
			} catch (RuntimeException | Error e) {
				if (loaded != null) loaded.free();
				kernel.free();
				throw e;
			}
		}

		private static long function(SharedLibrary library, String name) {
			long address = library.getFunctionAddress(name);
			if (address == 0) throw new UnsatisfiedLinkError("Missing Win32 API: " + name);
			return address;
		}

		@Override
		public boolean associate(boolean enabled) {
			// IACE_DEFAULT restores the default context; flags 0 with a NULL HIMC removes it.
			return callPPI(hwnd, 0L, enabled ? IACE_DEFAULT : 0, associateContextEx) != 0;
		}

		@Override
		public void close() {
			imm.free();
			kernel.free();
		}
	}
}
