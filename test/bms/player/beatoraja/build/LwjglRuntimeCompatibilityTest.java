package bms.player.beatoraja.build;

import java.nio.ByteBuffer;
import java.util.Base64;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.ALC10;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jemalloc.JEmalloc;

/** Java 17 JNI/native compatibility without a window, input device or audio device. */
public final class LwjglRuntimeCompatibilityTest {
    public static void main(String[] args) throws Exception {
        check(Version.getVersion().startsWith("3.4.3"), "Selected LWJGL Java version");
        ByteBuffer source = MemoryUtil.memAlloc(32);
        ByteBuffer copy = JEmalloc.je_malloc(32);
        try {
            check(copy != null, "jemalloc native allocation");
            for (int i = 0; i < source.capacity(); i++) source.put(i, (byte) (i * 7));
            MemoryUtil.memCopy(source, copy);
            for (int i = 0; i < source.capacity(); i++) check(copy.get(i) == source.get(i), "JNI memory copy");
        } finally {
            if (copy != null) JEmalloc.je_free(copy);
            MemoryUtil.memFree(source);
        }
        try (var stack = MemoryStack.stackPush()) {
            var major = stack.mallocInt(1);
            var minor = stack.mallocInt(1);
            var revision = stack.mallocInt(1);
            GLFW.glfwGetVersion(major, minor, revision);
            check(major.get(0) == 3 && minor.get(0) >= 4, "GLFW native version");
            byte[] png = Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+XdbsAAAAASUVORK5CYII=");
            var encoded = stack.malloc(png.length).put(png).flip();
            var channels = stack.mallocInt(1);
            ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, major, minor, channels, 4);
            try {
                check(decoded != null, "STB PNG decode: " + STBImage.stbi_failure_reason());
                check(major.get(0) == 1 && minor.get(0) == 1 && decoded.remaining() == 4, "STB image dimensions");
            } finally {
                if (decoded != null) STBImage.stbi_image_free(decoded);
            }
        }
        check(ALC10.alcGetCurrentContext() == 0, "Probe must not create an OpenAL context/device");
        bms.player.beatoraja.input.WindowsImeCallbackTest.main(new String[0]);
        System.out.println("LwjglRuntimeCompatibilityTest passed (" + Version.getVersion()
                + ", " + GLFW.glfwGetVersionString() + "; JNI memory/jemalloc/STB/OpenAL loader and callback ABI; NO window/device)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
