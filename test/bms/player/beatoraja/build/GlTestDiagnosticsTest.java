package bms.player.beatoraja.build;

import com.badlogic.gdx.ApplicationAdapter;
import org.lwjgl.glfw.GLFW;

/** Environment validation never substitutes for an actual Linux GL run. */
public final class GlTestDiagnosticsTest {
    public static void main(String[] args) throws Exception {
        GlTestDiagnostics.verifyEnvironment("Linux", GLFW.GLFW_PLATFORM_X11, "llvmpipe (LLVM 18, 256 bits)", true);
        rejects("Linux", GLFW.GLFW_PLATFORM_WAYLAND, "llvmpipe");
        rejects("Linux", GLFW.GLFW_PLATFORM_X11, "NVIDIA");
        rejects("Linux", GLFW.GLFW_PLATFORM_X11, null);
        rejects("Mac OS X", GLFW.GLFW_PLATFORM_COCOA, "llvmpipe");
        GlTestDiagnostics.verifyEnvironment("Mac OS X", GLFW.GLFW_PLATFORM_COCOA, "Apple", false);
        String previous = System.getProperty("beatoraja.glTest.output");
        try {
            System.clearProperty("beatoraja.glTest.output");
            var listener = new ApplicationAdapter() { };
            if (GlTestDiagnostics.wrap("fixture", listener) != listener) throw new AssertionError("Diagnostics must remain opt-in");
        } finally {
            if (previous != null) System.setProperty("beatoraja.glTest.output", previous);
        }
        verifyBrokenDiagnostics();
        System.out.println("GlTestDiagnosticsTest passed (policy only; NO GL/display)");
    }
    private static void verifyBrokenDiagnostics() throws Exception {
        var directory = java.nio.file.Files.createTempDirectory("broken GL diagnostics ");
        String previous = System.getProperty("beatoraja.glTest.output");
        try {
            var file = directory.resolve("not-a-directory"); java.nio.file.Files.writeString(file, "fixture");
            System.setProperty("beatoraja.glTest.output", file.toString());
            var expected = new AssertionError("Original render failure");
            int[] disposed = {0};
            var guarded = GlTestDiagnostics.wrap("fixture", new ApplicationAdapter() {
                @Override public void render() { throw expected; }
                @Override public void dispose() { disposed[0]++; }
            });
            try { guarded.render(); throw new AssertionError("Expected failure"); }
            catch (AssertionError failure) {
                if (failure != expected || failure.getSuppressed().length != 2)
                    throw new AssertionError("Broken diagnostics replaced original failure", failure);
            }
            try { guarded.dispose(); throw new AssertionError("Expected screenshot failure"); }
            catch (AssertionError failure) {
                if (!"Final GL screenshot failed".equals(failure.getMessage()) || disposed[0] != 1)
                    throw new AssertionError("Broken capture must not prevent delegate cleanup", failure);
            }
        } finally {
            if (previous == null) System.clearProperty("beatoraja.glTest.output");
            else System.setProperty("beatoraja.glTest.output", previous);
            try (var files = java.nio.file.Files.walk(directory)) {
                for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(file);
            }
        }
    }
    private static void rejects(String os, int backend, String renderer) {
        try { GlTestDiagnostics.verifyEnvironment(os, backend, renderer, true); }
        catch (AssertionError expected) { return; }
        throw new AssertionError("Wrong GL backend accepted");
    }
}
